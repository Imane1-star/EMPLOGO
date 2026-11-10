package com.transportpro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Accès à la table « chauffeur » (jointure avec « employe » pour le nom et le téléphone). */
public class ChauffeurDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class ChauffeurException extends Exception {
        public ChauffeurException(String message, Throwable cause) { super(message, cause); }
    }

    /** Employé proposé dans la liste « Employé » du formulaire. */
    public record EmployeeOption(int id, String label) {
        @Override
        public String toString() { return label; }
    }

    private static final String SELECT_JOIN =
            "SELECT c.id_chauffeur, c.id_employe, c.numero_permis, c.date_validite_permis, c.statut, "
                    + "e.nom, e.prenom, e.telephone "
                    + "FROM chauffeur c JOIN employe e ON e.id_employe = c.id_employe";
    private static final String ORDER = " ORDER BY c.id_chauffeur";
    private static final String INSERT =
            "INSERT INTO chauffeur (id_employe, numero_permis, date_validite_permis, statut) VALUES (?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE chauffeur SET numero_permis = ?, date_validite_permis = ?, statut = ? WHERE id_chauffeur = ?";
    private static final String DELETE = "DELETE FROM chauffeur WHERE id_chauffeur = ?";

    public List<Chauffeur> findAll() throws ChauffeurException {
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

    /**
     * Recherche par nom, prénom, téléphone, matricule de l'employé ou numéro de permis (SQL LIKE).
     * Chaque mot saisi doit se retrouver dans l'un de ces champs ; texte vide = tous les chauffeurs.
     */
    public List<Chauffeur> search(String text) throws ChauffeurException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();

        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE ");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' ")
               .append("OR e.telephone LIKE ? ESCAPE '!' OR e.matricule LIKE ? ESCAPE '!' ")
               .append("OR c.numero_permis LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);

        List<Chauffeur> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = "%" + tok.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
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

    public Chauffeur findById(int id) throws ChauffeurException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE c.id_chauffeur = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire le chauffeur.");
        }
        throw new ChauffeurException("Ce chauffeur n'existe plus dans la base.", null);
    }

    public void insert(Chauffeur ch) throws ChauffeurException {
        check(ch);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ch.getIdEmploye());
            ps.setString(2, ch.getNumeroPermis().trim());
            ps.setDate(3, Date.valueOf(ch.getDateValiditePermis()));
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
    public void update(Chauffeur ch) throws ChauffeurException {
        check(ch);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, ch.getNumeroPermis().trim());
            ps.setDate(2, Date.valueOf(ch.getDateValiditePermis()));
            ps.setString(3, ch.getStatut());
            ps.setInt(4, ch.getIdChauffeur());
            if (ps.executeUpdate() == 0)
                throw new ChauffeurException("Ce chauffeur n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier le chauffeur.");
        }
    }

    /** Supprime uniquement la ligne « chauffeur » : l'employé reste dans la table « employe ». */
    public void delete(int id) throws ChauffeurException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0)
                throw new ChauffeurException("Ce chauffeur n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer le chauffeur.");
        }
    }

    /**
     * Valeurs autorisées pour le statut, lues dans la base : liste de l'ENUM si la colonne en est un,
     * sinon valeurs déjà utilisées (ou ACTIF si la table est vide).
     */
    public List<String> findStatuts() throws ChauffeurException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chauffeur' AND COLUMN_NAME = 'statut'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String type = rs.getString(1);
                if (type != null && type.toLowerCase().startsWith("enum(")) {
                    Matcher m = Pattern.compile("'((?:[^']|'')*)'").matcher(type);
                    while (m.find()) values.add(m.group(1).replace("''", "'"));
                }
            }
            if (values.isEmpty()) {
                try (PreparedStatement ps2 = c.prepareStatement(
                        "SELECT DISTINCT statut FROM chauffeur WHERE statut IS NOT NULL ORDER BY statut");
                     ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) values.add(rs2.getString(1));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire les statuts.");
        }
        if (values.isEmpty()) values.add("ACTIF");
        return values;
    }

    /**
     * Employés actifs qui ne sont pas encore chauffeurs, plus l'employé indiqué (pour la modification ; 0 = aucun).
     */
    public List<EmployeeOption> findAvailableEmployees(int includeEmployeId) throws ChauffeurException {
        String sql = "SELECT e.id_employe, e.matricule, e.nom, e.prenom FROM employe e "
                + "WHERE e.id_employe = ? "
                + "OR (e.actif = 1 AND NOT EXISTS (SELECT 1 FROM chauffeur c WHERE c.id_employe = e.id_employe)) "
                + "ORDER BY e.nom, e.prenom";
        List<EmployeeOption> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeEmployeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String matricule = rs.getString("matricule");
                    String label = nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))
                            + (matricule == null || matricule.isBlank() ? "" : " (" + matricule + ")");
                    list.add(new EmployeeOption(rs.getInt("id_employe"), label.trim()));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des employés.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static String nz(String s) { return s == null ? "" : s; }

    private Chauffeur map(ResultSet rs) throws SQLException {
        Chauffeur ch = new Chauffeur();
        ch.setIdChauffeur(rs.getInt("id_chauffeur"));
        ch.setIdEmploye(rs.getInt("id_employe"));
        ch.setNumeroPermis(rs.getString("numero_permis"));
        Date d = rs.getDate("date_validite_permis");
        ch.setDateValiditePermis(d == null ? null : d.toLocalDate());
        ch.setStatut(rs.getString("statut"));
        ch.setNomComplet((nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))).trim());
        ch.setTelephone(nz(rs.getString("telephone")));
        return ch;
    }

    private void check(Chauffeur ch) throws ChauffeurException {
        String err = ch.validate();
        if (err != null) throw new ChauffeurException(err, null);
    }

    private ChauffeurException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new ChauffeurException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        switch (e.getErrorCode()) {
            case 1062: {
                String m = String.valueOf(e.getMessage());
                if (m.contains("uq_chauffeur_permis"))
                    return new ChauffeurException("Ce numéro de permis est déjà enregistré pour un chauffeur.", e);
                if (m.contains("uq_chauffeur_employe"))
                    return new ChauffeurException("Cet employé est déjà enregistré comme chauffeur.", e);
                return new ChauffeurException(
                        "Ce numéro de permis, ou cet employé, est déjà enregistré pour un chauffeur.", e);
            }
            case 1451:
                return new ChauffeurException(
                        "Ce chauffeur est utilisé par d'autres données (trajets, affectations...). "
                                + "Suppression impossible.", e);
            case 1452:
                return new ChauffeurException("L'employé choisi n'existe pas dans la base.", e);
            case 1265:
            case 1366:
                return new ChauffeurException("Le statut choisi n'est pas accepté par la base.", e);
            case 1406:
                return new ChauffeurException("Une valeur est trop longue pour la base de données.", e);
            default:
                return new ChauffeurException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}