package com.transportpro.dao;

import com.transportpro.model.Incident;
import com.transportpro.model.enums.TypeIncident;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accès à la table « incident » (BF20 à BF22), jointe à l'affectation (ligne + date), au bus,
 * au chauffeur (nom via « employe ») et au compte qui a déclaré l'incident.
 */
public class IncidentDAO {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SELECT_JOIN = """
            SELECT i.id_incident, i.id_affectation, i.id_compte, i.type, i.gravite, i.description,
                   i.date_incident, i.statut, i.mode_declaration,
                   t.nom AS ligne, a.date_trajet, b.id_bus, b.immatriculation,
                   e.nom AS ch_nom, e.prenom AS ch_prenom, u.login
            FROM incident i
            JOIN affectation a ON a.id_affectation = i.id_affectation
            JOIN trajet t ON t.id_trajet = a.id_trajet
            JOIN bus b ON b.id_bus = a.id_bus
            JOIN chauffeur c ON c.id_chauffeur = a.id_chauffeur
            JOIN employe e ON e.id_employe = c.id_employe
            LEFT JOIN compte_utilisateur u ON u.id_compte = i.id_compte
            """;
    private static final String ORDER = " ORDER BY i.date_incident DESC, i.id_incident DESC";

    public List<Incident> findAll() throws SQLException {
        return search("");
    }

    /**
     * Recherche dans la description, le type, la ligne, la plaque, le chauffeur, la date (jj/mm/aaaa)
     * et les numéros (INC12, T5) ; texte vide = tous les incidents.
     */
    public List<Incident> search(String text) throws SQLException {
        String t = text == null ? "" : text.trim();
        String[] tokens = t.isEmpty() ? new String[0] : t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN);
        for (int k = 0; k < tokens.length; k++) {
            sql.append(k == 0 ? " WHERE " : " AND ");
            sql.append("(i.description LIKE ? ESCAPE '!' OR i.type LIKE ? ESCAPE '!' OR t.nom LIKE ? ESCAPE '!' ")
               .append("OR b.immatriculation LIKE ? ESCAPE '!' OR e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' ")
               .append("OR DATE_FORMAT(i.date_incident, '%d/%m/%Y') LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('INC', i.id_incident, ' T', a.id_affectation) LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);
        List<Incident> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = EmployeDAO.like(tok);
                for (int k = 0; k < 8; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger les incidents.");
        }
        return list;
    }

    /** Retourne l'incident, ou null s'il n'existe pas. */
    public Incident findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE i.id_incident = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de lire l'incident.");
        }
    }

    public void insert(Incident i) throws SQLException {
        i.creer();
        check(i);
        String sql = "INSERT INTO incident (id_affectation, id_compte, type, gravite, description, date_incident, "
                + "statut, mode_declaration) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, i);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) i.setIdIncident(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible d'enregistrer l'incident.");
        }
    }

    public void update(Incident i) throws SQLException {
        check(i);
        String sql = "UPDATE incident SET id_affectation = ?, id_compte = ?, type = ?, gravite = ?, description = ?, "
                + "date_incident = ?, statut = ?, mode_declaration = ? WHERE id_incident = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, i);
            ps.setInt(9, i.getIdIncident());
            if (ps.executeUpdate() == 0) throw new DaoException("Cet incident n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de modifier l'incident.");
        }
    }

    /** Un incident n'a aucune donnée dépendante : suppression simple. */
    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "DELETE FROM incident WHERE id_incident = ?", id) == 0)
                throw new DaoException("Cet incident n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de supprimer l'incident.");
        }
    }

    // ------------------------------------------------------------------ listes du formulaire

    /** Valeurs d'une colonne ENUM de la table incident (lues dans la base). */
    public List<String> findEnum(String column) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incident' AND COLUMN_NAME = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null && rs.getString(1).toLowerCase().startsWith("enum(")) {
                    Matcher m = Pattern.compile("'((?:[^']|'')*)'").matcher(rs.getString(1));
                    while (m.find()) values.add(m.group(1).replace("''", "'"));
                }
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de lire les valeurs autorisées.");
        }
        if (values.isEmpty())
            throw new DaoException("La colonne incident." + column + " est introuvable : réimportez dataBase.sql.");
        return values;
    }

    public List<String> findGravites() throws SQLException { return findEnum("gravite"); }
    public List<String> findStatuts() throws SQLException { return findEnum("statut"); }
    public List<String> findModes() throws SQLException { return findEnum("mode_declaration"); }

    /** Trajets (affectations) non annulés, plus celui indiqué (modification ; 0 = aucun). */
    public List<Option> findAffectations(int includeId) throws SQLException {
        String sql = "SELECT a.id_affectation, t.nom, a.date_trajet, a.heure_depart, b.immatriculation "
                + "FROM affectation a JOIN trajet t ON t.id_trajet = a.id_trajet JOIN bus b ON b.id_bus = a.id_bus "
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
                    list.add(new Option(rs.getInt("id_affectation"), "T" + rs.getInt("id_affectation") + " • "
                            + rs.getString("nom") + " • " + (d == null ? "" : d.toLocalDate().format(DATE_FMT))
                            + " " + (h == null ? "" : h.toLocalTime().format(TIME_FMT))
                            + " • " + rs.getString("immatriculation")));
                }
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger la liste des trajets.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static void check(Incident i) throws DaoException {
        String err = i.validate();
        if (err != null) throw new DaoException(err);
    }

    private static void bind(PreparedStatement ps, Incident i) throws SQLException {
        ps.setInt(1, i.getIdAffectation());
        if (i.getIdCompte() == null) ps.setNull(2, Types.INTEGER);
        else ps.setInt(2, i.getIdCompte());
        ps.setString(3, i.getType().name());
        ps.setString(4, i.getGravite());
        ps.setString(5, i.getDescription().trim());
        ps.setTimestamp(6, Timestamp.valueOf(i.getDateHeure()));
        ps.setString(7, i.getStatut());
        ps.setString(8, i.getModeDeclaration());
    }

    private static Incident map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("date_incident");
        Incident i = new Incident(rs.getInt("id_incident"), TypeIncident.valueOf(rs.getString("type")),
                rs.getString("description") == null ? "" : rs.getString("description"),
                ts == null ? null : ts.toLocalDateTime());
        i.setIdAffectation(rs.getInt("id_affectation"));
        int compte = rs.getInt("id_compte");
        i.setIdCompte(rs.wasNull() ? null : compte);
        i.setGravite(rs.getString("gravite"));
        i.setStatut(rs.getString("statut"));
        i.setModeDeclaration(rs.getString("mode_declaration"));
        i.setNomLigne(rs.getString("ligne"));
        Date d = rs.getDate("date_trajet");
        i.setDateTrajet(d == null ? null : d.toLocalDate());
        i.setIdBus(rs.getInt("id_bus"));
        i.setImmatriculation(rs.getString("immatriculation"));
        i.setChauffeurNom((rs.getString("ch_nom") + " " + rs.getString("ch_prenom")).trim());
        i.setDeclarant(rs.getString("login") == null ? "" : rs.getString("login"));
        return i;
    }
}
