package com.emplogo.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.emplogo.model.Employe;

/** Accès à la table « employe » (BF1 à BF4). */
public class EmployeDAO {

    private static final String COLS =
            "id_employe, matricule, nom, prenom, email, telephone, adresse, departement, date_embauche, actif";
    private static final String DUPLICATE = "Ce matricule (ou cet e-mail) existe déjà.";
    private static final String IN_USE = "Cet employé est utilisé par d'autres données (inscriptions, compte, chauffeur...).";

    public List<Employe> findAll() throws SQLException {
        List<Employe> employees = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM employe ORDER BY id_employe";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) employees.add(map(rs));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger les employés.");
        }
        return employees;
    }

    /** Retourne l'employé, ou null s'il n'existe pas. */
    public Employe findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM employe WHERE id_employe = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de lire l'employé.");
        }
    }

    /**
     * BF3 : recherche par matricule, nom, prénom ou téléphone (SQL LIKE).
     * Chaque mot saisi doit se retrouver dans l'un de ces champs ; texte vide = tous les employés.
     */
    public List<Employe> search(String text) throws SQLException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();
        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM employe WHERE ");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(matricule LIKE ? ESCAPE '!' OR nom LIKE ? ESCAPE '!' ")
               .append("OR prenom LIKE ? ESCAPE '!' OR telephone LIKE ? ESCAPE '!')");
        }
        sql.append(" ORDER BY id_employe");
        List<Employe> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = like(tok);
                for (int k = 0; k < 4; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de rechercher les employés.");
        }
        return list;
    }

    public void insert(Employe e) throws SQLException {
        check(e);
        String sql = """
            INSERT INTO employe
            (matricule, nom, prenom, email, telephone, adresse, departement, date_embauche, actif)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setIdEmploye(keys.getInt(1));
            }
        } catch (SQLException ex) {
            throw DaoException.translate(ex, DUPLICATE, IN_USE, "Impossible d'ajouter l'employé.");
        }
    }

    public void update(Employe e) throws SQLException {
        check(e);
        String sql = """
            UPDATE employe SET matricule = ?, nom = ?, prenom = ?, email = ?, telephone = ?,
                   adresse = ?, departement = ?, date_embauche = ?, actif = ?
            WHERE id_employe = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.setInt(10, e.getIdEmploye());
            if (ps.executeUpdate() == 0) throw new DaoException("Cet employé n'existe plus dans la base.");
        } catch (SQLException ex) {
            throw DaoException.translate(ex, DUPLICATE, IN_USE, "Impossible de modifier l'employé.");
        }
    }

    /** Suppression simple (refusée par la base si l'employé a des données liées). */
    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            DatabaseConnection.exec(c, "DELETE FROM employe WHERE id_employe = ?", id);
        } catch (SQLException ex) {
            throw DaoException.translate(ex, DUPLICATE, IN_USE, "Impossible de supprimer l'employé.");
        }
    }

    // ------------------------------------------------------------------ désactivation / suppression complète

    /** Données liées à un employé (elles empêchent une suppression simple). */
    public record Usage(int chauffeur, int trajetsConduits, int inscriptions, int presences, int comptes) {
        public boolean any() { return chauffeur + inscriptions + presences + comptes > 0; }
        /** Un chauffeur qui a déjà des trajets ne peut pas être effacé sans toucher aux autres employés. */
        public boolean canDeleteWithHistory() { return trajetsConduits == 0; }
    }

    private static final String SQL_TRAJETS_CONDUITS = "SELECT COUNT(*) FROM affectation a JOIN chauffeur ch "
            + "ON ch.id_chauffeur = a.id_chauffeur WHERE ch.id_employe = ?";

    public Usage usage(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return new Usage(
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM chauffeur WHERE id_employe = ?", id),
                    DatabaseConnection.count(c, SQL_TRAJETS_CONDUITS, id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM inscription WHERE id_employe = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM presence p JOIN inscription i "
                            + "ON i.id_inscription = p.id_inscription WHERE i.id_employe = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM compte_utilisateur WHERE id_employe = ?", id));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de vérifier les données liées à l'employé.");
        }
    }

    /** BF4 / RG7 : l'employé ne peut plus être inscrit, mais tout son historique est conservé. */
    public void desactiver(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "UPDATE employe SET actif = 0 WHERE id_employe = ?", id) == 0)
                throw new DaoException("Cet employé n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de désactiver l'employé.");
        }
    }

    /**
     * Supprime l'employé et tout ce qui lui appartient, en une seule transaction :
     * présences, inscriptions, compte utilisateur (ses incidents déclarés restent, sans auteur)
     * et fiche chauffeur. Refusé si l'employé a déjà conduit des trajets.
     */
    public void deleteWithHistory(int id) throws SQLException {
        try {
            DatabaseConnection.inTransaction(c -> {
                if (DatabaseConnection.count(c, SQL_TRAJETS_CONDUITS, id) > 0)
                    throw new DaoException("Cet employé est chauffeur et a déjà des trajets : il ne peut pas être "
                            + "supprimé. Désactivez-le à la place.");
                DatabaseConnection.exec(c, "DELETE p FROM presence p JOIN inscription i "
                        + "ON i.id_inscription = p.id_inscription WHERE i.id_employe = ?", id);
                DatabaseConnection.exec(c, "DELETE FROM inscription WHERE id_employe = ?", id);
                DatabaseConnection.exec(c, "UPDATE incident SET id_compte = NULL WHERE id_compte IN "
                        + "(SELECT id_compte FROM compte_utilisateur WHERE id_employe = ?)", id);
                DatabaseConnection.exec(c, "DELETE FROM compte_utilisateur WHERE id_employe = ?", id);
                DatabaseConnection.exec(c, "DELETE FROM chauffeur WHERE id_employe = ?", id);
                if (DatabaseConnection.exec(c, "DELETE FROM employe WHERE id_employe = ?", id) == 0)
                    throw new DaoException("Cet employé n'existe plus dans la base.");
                return null;
            });
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible de supprimer l'employé.");
        }
    }

    // ------------------------------------------------------------------ outils internes
    static String like(String token) {
        return "%" + token.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static void check(Employe e) throws DaoException {
        String err = e.validate();
        if (err != null) throw new DaoException(err);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static void bind(PreparedStatement ps, Employe e) throws SQLException {
        ps.setString(1, e.getMatricule().trim());
        ps.setString(2, e.getNom().trim());
        ps.setString(3, e.getPrenom().trim());
        ps.setString(4, e.getEmail().trim());
        ps.setString(5, trimOrNull(e.getTelephone()));
        ps.setString(6, trimOrNull(e.getAdresse()));
        ps.setString(7, trimOrNull(e.getDepartement()));
        if (e.getDateEmbauche() == null) ps.setNull(8, Types.DATE);
        else ps.setDate(8, Date.valueOf(e.getDateEmbauche()));
        ps.setBoolean(9, e.isActif());
    }

    private static Employe map(ResultSet rs) throws SQLException {
        Date d = rs.getDate("date_embauche");
        LocalDate dateEmb = d == null ? null : d.toLocalDate();
        return new Employe(
                rs.getInt("id_employe"),
                rs.getString("matricule"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("telephone"),
                rs.getString("adresse"),
                rs.getString("departement"),
                dateEmb,
                rs.getBoolean("actif"));
    }
}
