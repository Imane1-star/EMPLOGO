package com.transportpro.incident;

import com.transportpro.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accès à la table « incident » (BF20 à BF22), jointe à l'affectation (trajet + date), au bus,
 * au chauffeur (nom via « employe ») et au compte qui a déclaré l'incident.
 */
public class IncidentDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class IncidentException extends Exception {
        public IncidentException(String message, Throwable cause) { super(message, cause); }
    }

    /** Élément d'une liste déroulante (trajet, bus). */
    public record Option(int id, String label) {
        @Override
        public String toString() { return label; }
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SELECT_JOIN =
            "SELECT i.id_incident, i.id_affectation, i.id_compte, i.type_incident, i.gravite, i.description, "
                    + "i.date_incident, i.mode_declaration, i.statut_traitement, "
                    + "t.nom AS ligne, a.date_trajet, b.id_bus, b.immatriculation, "
                    + "e.nom AS ch_nom, e.prenom AS ch_prenom, u.login "
                    + "FROM incident i "
                    + "JOIN affectation a ON a.id_affectation = i.id_affectation "
                    + "JOIN trajet t ON t.id_trajet = a.id_trajet "
                    + "JOIN bus b ON b.id_bus = a.id_bus "
                    + "JOIN chauffeur c ON c.id_chauffeur = a.id_chauffeur "
                    + "JOIN employe e ON e.id_employe = c.id_employe "
                    + "LEFT JOIN compte_utilisateur u ON u.id_compte = i.id_compte";
    private static final String ORDER = " ORDER BY i.date_incident DESC, i.id_incident DESC";
    private static final String INSERT =
            "INSERT INTO incident (id_affectation, id_compte, type_incident, gravite, description, "
                    + "date_incident, mode_declaration, statut_traitement) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE incident SET id_affectation = ?, id_compte = ?, type_incident = ?, gravite = ?, "
                    + "description = ?, date_incident = ?, mode_declaration = ?, statut_traitement = ? "
                    + "WHERE id_incident = ?";
    private static final String DELETE = "DELETE FROM incident WHERE id_incident = ?";

    private static final int SEARCH_FIELDS = 8;

    public List<Incident> findAll() throws IncidentException {
        List<Incident> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + ORDER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger les incidents.");
        }
        return list;
    }

    /**
     * Recherche (SQL LIKE) dans la description, le type, la ligne, la plaque du bus, le nom du chauffeur,
     * la date de l'incident (jj/mm/aaaa) et le numéro (INC12, T5).
     * Chaque mot saisi doit se retrouver dans l'un de ces champs ; texte vide = tous les incidents.
     */
    public List<Incident> search(String text) throws IncidentException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();

        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE ");
        for (int k = 0; k < tokens.length; k++) {
            if (k > 0) sql.append(" AND ");
            sql.append("(i.description LIKE ? ESCAPE '!' OR i.type_incident LIKE ? ESCAPE '!' ")
               .append("OR t.nom LIKE ? ESCAPE '!' OR b.immatriculation LIKE ? ESCAPE '!' ")
               .append("OR e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' ")
               .append("OR DATE_FORMAT(i.date_incident, '%d/%m/%Y') LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('INC', i.id_incident, ' T', a.id_affectation) LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);

        List<Incident> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = "%" + tok.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                for (int k = 0; k < SEARCH_FIELDS; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de rechercher les incidents.");
        }
        return list;
    }

    public Incident findById(int id) throws IncidentException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE i.id_incident = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire l'incident.");
        }
        throw new IncidentException("Cet incident n'existe plus dans la base.", null);
    }

    public void insert(Incident i) throws IncidentException {
        check(i);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, i);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) i.setIdIncident(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible d'enregistrer l'incident.");
        }
    }

    public void update(Incident i) throws IncidentException {
        check(i);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, i);
            ps.setInt(9, i.getIdIncident());
            if (ps.executeUpdate() == 0)
                throw new IncidentException("Cet incident n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier l'incident.");
        }
    }

    /** Un incident n'a aucune donnée dépendante : suppression simple. */
    public void delete(int id) throws IncidentException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, DELETE, id) == 0)
                throw new IncidentException("Cet incident n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer l'incident.");
        }
    }

    // ------------------------------------------------------------------ listes du formulaire

    /** Valeurs d'une colonne ENUM de la table incident (lues dans la base). */
    public List<String> findEnum(String column) throws IncidentException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incident' AND COLUMN_NAME = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString(1);
                    if (type != null && type.toLowerCase().startsWith("enum(")) {
                        Matcher m = Pattern.compile("'((?:[^']|'')*)'").matcher(type);
                        while (m.find()) values.add(m.group(1).replace("''", "'"));
                    }
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire les valeurs autorisées.");
        }
        if (values.isEmpty())
            throw new IncidentException("La colonne incident." + column + " est introuvable. "
                    + "Appliquez database/migrations/002_align_incident_maintenance.sql.", null);
        return values;
    }

    public List<String> findTypes() throws IncidentException { return findEnum("type_incident"); }
    public List<String> findGravites() throws IncidentException { return findEnum("gravite"); }
    public List<String> findStatuts() throws IncidentException { return findEnum("statut_traitement"); }
    public List<String> findModes() throws IncidentException { return findEnum("mode_declaration"); }

    /** Trajets (affectations) non annulés, plus celui indiqué (pour la modification ; 0 = aucun). */
    public List<Option> findAffectations(int includeId) throws IncidentException {
        String sql = "SELECT a.id_affectation, t.nom, a.date_trajet, a.heure_depart, b.immatriculation "
                + "FROM affectation a JOIN trajet t ON t.id_trajet = a.id_trajet "
                + "JOIN bus b ON b.id_bus = a.id_bus "
                + "WHERE a.statut <> 'ANNULEE' OR a.id_affectation = ? "
                + "ORDER BY a.date_trajet DESC, a.heure_depart, a.id_affectation";
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("date_trajet");
                    Time h = rs.getTime("heure_depart");
                    String label = "T" + rs.getInt("id_affectation") + " • " + nz(rs.getString("nom"))
                            + " • " + (d == null ? "" : d.toLocalDate().format(DATE_FMT))
                            + " " + (h == null ? "" : h.toLocalTime().format(TIME_FMT))
                            + " • " + nz(rs.getString("immatriculation"));
                    list.add(new Option(rs.getInt("id_affectation"), label));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des trajets.");
        }
        return list;
    }

    /** Tous les bus (filtre BF22). */
    public List<Option> findBuses() throws IncidentException {
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id_bus, immatriculation FROM bus ORDER BY immatriculation");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Option(rs.getInt("id_bus"), nz(rs.getString("immatriculation"))));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des bus.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static String nz(String s) { return s == null ? "" : s; }

    private static void bind(PreparedStatement ps, Incident i) throws SQLException {
        ps.setInt(1, i.getIdAffectation());
        if (i.getIdCompte() == null) ps.setNull(2, Types.INTEGER);
        else ps.setInt(2, i.getIdCompte());
        ps.setString(3, i.getTypeIncident());
        ps.setString(4, i.getGravite());
        ps.setString(5, i.getDescription().trim());
        ps.setTimestamp(6, Timestamp.valueOf(i.getDateIncident()));
        ps.setString(7, i.getModeDeclaration());
        ps.setString(8, i.getStatutTraitement());
    }

    private Incident map(ResultSet rs) throws SQLException {
        Incident i = new Incident();
        i.setIdIncident(rs.getInt("id_incident"));
        i.setIdAffectation(rs.getInt("id_affectation"));
        int compte = rs.getInt("id_compte");
        i.setIdCompte(rs.wasNull() ? null : compte);
        i.setTypeIncident(rs.getString("type_incident"));
        i.setGravite(rs.getString("gravite"));
        i.setDescription(nz(rs.getString("description")));
        Timestamp ts = rs.getTimestamp("date_incident");
        i.setDateIncident(ts == null ? null : ts.toLocalDateTime());
        i.setModeDeclaration(rs.getString("mode_declaration"));
        i.setStatutTraitement(rs.getString("statut_traitement"));
        i.setNomLigne(nz(rs.getString("ligne")));
        Date d = rs.getDate("date_trajet");
        i.setDateTrajet(d == null ? null : d.toLocalDate());
        i.setIdBus(rs.getInt("id_bus"));
        i.setImmatriculation(nz(rs.getString("immatriculation")));
        i.setChauffeurNom((nz(rs.getString("ch_nom")) + " " + nz(rs.getString("ch_prenom"))).trim());
        i.setDeclarant(nz(rs.getString("login")));
        return i;
    }

    private void check(Incident i) throws IncidentException {
        String err = i.validate();
        if (err != null) throw new IncidentException(err, null);
    }

    private IncidentException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new IncidentException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        switch (e.getErrorCode()) {
            case 1054:
                return new IncidentException("La table incident n'est pas à jour. "
                        + "Appliquez database/migrations/002_align_incident_maintenance.sql.", e);
            case 1452:
                return new IncidentException("Le trajet ou le compte choisi n'existe pas dans la base.", e);
            case 1265:
            case 1366:
                return new IncidentException("Une valeur choisie n'est pas acceptée par la base.", e);
            case 1292:
                return new IncidentException("La date ou l'heure saisie n'est pas valide.", e);
            default:
                return new IncidentException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}
