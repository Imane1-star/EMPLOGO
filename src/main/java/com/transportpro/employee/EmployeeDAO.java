
package com.transportpro.employee;

import com.transportpro.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class EmployeeException extends Exception {
        public EmployeeException(String message, Throwable cause) { super(message, cause); }
    }

    // ------------------------------------------------------------------ existant (inchangé)
    public List<String[]> getAllEmployees() throws SQLException {

        List<String[]> employees = new ArrayList<>();

        String sql = """
            SELECT id_employe, matricule, nom, prenom,
                   email, telephone, adresse, departement,
                   date_embauche, actif
            FROM employe
            ORDER BY id_employe
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                employees.add(new String[]{
                    String.valueOf(rs.getInt("id_employe")),
                    rs.getString("matricule"),
                    rs.getString("nom") + " " + rs.getString("prenom"),
                    rs.getString("telephone"),
                    rs.getString("adresse"),
                    rs.getBoolean("actif") ? "Actif" : "Inactif"
                });
            }
        }

        return employees;
    }

    // ------------------------------------------------------------------ nouveau
    private static final String COLS =
            "id_employe, matricule, nom, prenom, email, telephone, adresse, departement, date_embauche, actif";
    private static final String SELECT_ALL = "SELECT " + COLS + " FROM employe ORDER BY id_employe";
    private static final String SELECT_ONE = "SELECT " + COLS + " FROM employe WHERE id_employe = ?";
    private static final String INSERT =
            "INSERT INTO employe (matricule, nom, prenom, email, telephone, adresse, departement, "
                    + "date_embauche, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE = "DELETE FROM employe WHERE id_employe = ?";
    private static final String UPDATE =
            "UPDATE employe SET matricule = ?, nom = ?, prenom = ?, email = ?, telephone = ?, "
                    + "adresse = ?, departement = ?, date_embauche = ?, actif = ? WHERE id_employe = ?";

    public List<Employee> findAll() throws EmployeeException {
        List<Employee> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger les employés.");
        }
        return list;
    }

    /**
     * Recherche par matricule, nom, prénom ou téléphone (SQL LIKE, insensible à la casse).
     * Chaque mot saisi doit se retrouver dans l'un de ces quatre champs ; texte vide = tous les employés.
     */
    public List<Employee> search(String text) throws EmployeeException {
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

        List<Employee> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = "%" + tok.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                for (int k = 0; k < 4; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de rechercher les employés.");
        }
        return list;
    }

    public void insert(Employee e) throws EmployeeException {
        String err = e.validate();
        if (err != null) throw new EmployeeException(err, null);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setIdEmploye(keys.getInt(1));
            }
        } catch (SQLException ex) {
            throw translate(ex, "Impossible d'ajouter l'employé.");
        }
    }

    public void delete(int id) throws EmployeeException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0)
                throw new EmployeeException("Cet employé n'existe plus dans la base.", null);
        } catch (SQLException ex) {
            throw translate(ex, "Impossible de supprimer l'employé.");
        }
    }

    // ------------------------------------------------------------------ suppression / désactivation

    /** Données liées à un employé (elles empêchent une suppression simple). */
    public record Usage(int chauffeur, int trajetsConduits, int inscriptions, int presences, int comptes) {
        public boolean any() { return chauffeur + inscriptions + presences + comptes > 0; }
        /** Un chauffeur qui a déjà des trajets ne peut pas être effacé sans toucher aux autres employés. */
        public boolean canDeleteWithHistory() { return trajetsConduits == 0; }
    }

    public Usage usage(int id) throws EmployeeException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return new Usage(
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM chauffeur WHERE id_employe = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM affectation a JOIN chauffeur ch "
                            + "ON ch.id_chauffeur = a.id_chauffeur WHERE ch.id_employe = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM inscription WHERE id_employe = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM presence p JOIN inscription i "
                            + "ON i.id_inscription = p.id_inscription WHERE i.id_employe = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM compte_utilisateur WHERE id_employe = ?", id));
        } catch (SQLException e) {
            throw translate(e, "Impossible de vérifier les données liées à l'employé.");
        }
    }

    /** BF4 / RG7 : l'employé ne peut plus être inscrit, mais tout son historique est conservé. */
    public void deactivate(int id) throws EmployeeException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "UPDATE employe SET actif = 0 WHERE id_employe = ?", id) == 0)
                throw new EmployeeException("Cet employé n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de désactiver l'employé.");
        }
    }

    /**
     * Supprime l'employé et tout ce qui lui appartient, en une seule transaction :
     * présences, inscriptions, compte utilisateur (les incidents déclarés sont conservés, sans auteur)
     * et fiche chauffeur. Refusé si l'employé a déjà conduit des trajets.
     */
    public void deleteWithHistory(int id) throws EmployeeException {
        try {
            DatabaseConnection.inTransaction(c -> {
                if (DatabaseConnection.count(c, "SELECT COUNT(*) FROM affectation a JOIN chauffeur ch "
                        + "ON ch.id_chauffeur = a.id_chauffeur WHERE ch.id_employe = ?", id) > 0)
                    throw new SQLException("DRIVER_HAS_TRIPS");
                DatabaseConnection.exec(c, "DELETE p FROM presence p JOIN inscription i "
                        + "ON i.id_inscription = p.id_inscription WHERE i.id_employe = ?", id);
                DatabaseConnection.exec(c, "DELETE FROM inscription WHERE id_employe = ?", id);
                DatabaseConnection.exec(c, "UPDATE incident SET id_compte = NULL WHERE id_compte IN "
                        + "(SELECT id_compte FROM compte_utilisateur WHERE id_employe = ?)", id);
                DatabaseConnection.exec(c, "DELETE FROM compte_utilisateur WHERE id_employe = ?", id);
                DatabaseConnection.exec(c, "DELETE FROM chauffeur WHERE id_employe = ?", id);
                if (DatabaseConnection.exec(c, DELETE, id) == 0)
                    throw new SQLException("NOT_FOUND");
                return null;
            });
        } catch (SQLException e) {
            if ("DRIVER_HAS_TRIPS".equals(e.getMessage()))
                throw new EmployeeException("Cet employé est chauffeur et a déjà des trajets : "
                        + "il ne peut pas être supprimé. Désactivez-le à la place.", e);
            if ("NOT_FOUND".equals(e.getMessage()))
                throw new EmployeeException("Cet employé n'existe plus dans la base.", e);
            throw translate(e, "Impossible de supprimer l'employé.");
        }
    }

    public Employee findById(int id) throws EmployeeException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ONE)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire l'employé.");
        }
        throw new EmployeeException("Cet employé n'existe plus dans la base.", null);
    }

    public void update(Employee e) throws EmployeeException {
        String err = e.validate();
        if (err != null) throw new EmployeeException(err, null);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, e);
            ps.setInt(10, e.getIdEmploye());
            if (ps.executeUpdate() == 0)
                throw new EmployeeException("Cet employé n'existe plus dans la base.", null);
        } catch (SQLException ex) {
            throw translate(ex, "Impossible de modifier l'employé.");
        }
    }

    /** Paramètres 1 à 9 communs à INSERT et UPDATE. */
    private void bind(PreparedStatement ps, Employee e) throws SQLException {
        ps.setString(1, e.getMatricule().trim());
        ps.setString(2, e.getNom().trim());
        ps.setString(3, e.getPrenom().trim());
        setNullable(ps, 4, e.getEmail());
        setNullable(ps, 5, e.getTelephone());
        setNullable(ps, 6, e.getAdresse());
        setNullable(ps, 7, e.getDepartement());
        if (e.getDateEmbauche() == null) ps.setNull(8, Types.DATE);
        else ps.setDate(8, Date.valueOf(e.getDateEmbauche()));
        ps.setBoolean(9, e.isActif());
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setIdEmploye(rs.getInt("id_employe"));
        e.setMatricule(rs.getString("matricule"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setAdresse(rs.getString("adresse"));
        e.setDepartement(rs.getString("departement"));
        Date d = rs.getDate("date_embauche");
        e.setDateEmbauche(d == null ? null : d.toLocalDate());
        e.setActif(rs.getBoolean("actif"));
        return e;
    }

    private void setNullable(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v.trim());
    }

    private EmployeeException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new EmployeeException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        switch (e.getErrorCode()) {
            case 1062:
                return new EmployeeException("Ce matricule (ou cet e-mail) existe déjà.", e);
            case 1451:
                return new EmployeeException(
                        "Cet employé est utilisé par d'autres données (affectations, inscriptions...). "
                                + "Suppression impossible.", e);
            case 1406:
                return new EmployeeException("Une valeur est trop longue pour la base de données.", e);
            default:
                return new EmployeeException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}