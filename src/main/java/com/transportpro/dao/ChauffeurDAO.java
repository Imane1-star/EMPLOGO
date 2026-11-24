package com.transportpro.dao;

import com.transportpro.model.Chauffeur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table « chauffeur » (BF7, BF8). Le nom et le téléphone viennent de l'employé lié
 * (jointure chauffeur.id_employe -> employe).
 */
public class ChauffeurDAO {

    private static final String SELECT_JOIN = """
            SELECT c.id_chauffeur, c.id_employe, c.numero_permis, c.validite_permis, c.statut,
                   e.nom, e.prenom, e.telephone
            FROM chauffeur c
            INNER JOIN employe e ON c.id_employe = e.id_employe
            """;
    private static final String ORDER = " ORDER BY c.id_chauffeur";

    public List<Chauffeur> findAll() throws SQLException {
        List<Chauffeur> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + ORDER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger les chauffeurs.");
        }
        return list;
    }

    /** Recherche par nom, prénom, téléphone, matricule ou numéro de permis ; texte vide = tous. */
    public List<Chauffeur> search(String text) throws SQLException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();
        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE ");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' OR e.telephone LIKE ? ESCAPE '!' ")
               .append("OR e.matricule LIKE ? ESCAPE '!' OR c.numero_permis LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);
        List<Chauffeur> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = EmployeDAO.like(tok);
                for (int k = 0; k < 5; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de rechercher les chauffeurs.");
        }
        return list;
    }

    /** Retourne le chauffeur, ou null s'il n'existe pas. */
    public Chauffeur findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE c.id_chauffeur = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire le chauffeur.");
        }
    }

    public void insert(Chauffeur ch) throws SQLException {
        check(ch);
        String sql = "INSERT INTO chauffeur (id_employe, numero_permis, validite_permis, statut) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ch.getIdEmploye());
            ps.setString(2, ch.getNumeroPermis().trim());
            ps.setDate(3, Date.valueOf(ch.getValiditePermis()));
            ps.setString(4, ch.getStatut());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ch.setIdChauffeur(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible d'ajouter le chauffeur.");
        }
    }

    /** Modifie le permis et le statut ; l'employé lié ne change pas. */
    public void update(Chauffeur ch) throws SQLException {
        check(ch);
        String sql = "UPDATE chauffeur SET numero_permis = ?, validite_permis = ?, statut = ? WHERE id_chauffeur = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ch.getNumeroPermis().trim());
            ps.setDate(2, Date.valueOf(ch.getValiditePermis()));
            ps.setString(3, ch.getStatut());
            ps.setInt(4, ch.getIdChauffeur());
            if (ps.executeUpdate() == 0) throw new DaoException("Ce chauffeur n'existe plus dans la base.");
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier le chauffeur.");
        }
    }

    /** Supprime uniquement la fiche chauffeur : l'employé reste dans la table « employe ». */
    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "DELETE FROM chauffeur WHERE id_chauffeur = ?", id) == 0)
                throw new DaoException("Ce chauffeur n'existe plus dans la base.");
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer le chauffeur.");
        }
    }

    /** Nombre de trajets (affectations) conduits ou prévus pour ce chauffeur. */
    public int countTrajets(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return DatabaseConnection.count(c, "SELECT COUNT(*) FROM affectation WHERE id_chauffeur = ?", id);
        } catch (SQLException e) {
            throw translate(e, "Impossible de vérifier les trajets du chauffeur.");
        }
    }

    /** BF8 : le chauffeur n'est plus proposé pour les trajets (statut SUSPENDU) ; son historique est conservé. */
    public void desactiver(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "UPDATE chauffeur SET statut = 'SUSPENDU' WHERE id_chauffeur = ?", id) == 0)
                throw new DaoException("Ce chauffeur n'existe plus dans la base.");
        } catch (SQLException e) {
            throw translate(e, "Impossible de désactiver le chauffeur.");
        }
    }

    /** Employés actifs qui ne sont pas encore chauffeurs, plus l'employé indiqué (modification ; 0 = aucun). */
    public List<Option> findAvailableEmployees(int includeEmployeId) throws SQLException {
        String sql = "SELECT e.id_employe, e.matricule, e.nom, e.prenom FROM employe e "
                + "WHERE e.id_employe = ? "
                + "OR (e.actif = 1 AND NOT EXISTS (SELECT 1 FROM chauffeur c WHERE c.id_employe = e.id_employe)) "
                + "ORDER BY e.nom, e.prenom";
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeEmployeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Option(rs.getInt("id_employe"),
                            (nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))).trim()
                                    + " (" + nz(rs.getString("matricule")) + ")"));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des employés.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static String nz(String s) { return s == null ? "" : s; }

    private static Chauffeur map(ResultSet rs) throws SQLException {
        Chauffeur ch = new Chauffeur(
                rs.getInt("id_chauffeur"),
                rs.getString("nom"),
                rs.getString("telephone"),
                rs.getString("numero_permis"),
                "ACTIF".equals(rs.getString("statut")));
        ch.setIdEmploye(rs.getInt("id_employe"));
        ch.setPrenom(rs.getString("prenom"));
        Date d = rs.getDate("validite_permis");
        ch.setValiditePermis(d == null ? null : d.toLocalDate());
        ch.setStatut(rs.getString("statut"));
        return ch;
    }

    private static void check(Chauffeur ch) throws DaoException {
        String err = ch.validate();
        if (err != null) throw new DaoException(err);
    }

    private static DaoException translate(SQLException e, String defaut) {
        String dup = String.valueOf(e.getMessage()).contains("uq_chauffeur_employe")
                ? "Cet employé est déjà enregistré comme chauffeur."
                : "Ce numéro de permis est déjà enregistré pour un chauffeur.";
        return DaoException.translate(e, dup,
                "Ce chauffeur a des trajets : suppression impossible. Désactivez-le à la place.", defaut);
    }
}
