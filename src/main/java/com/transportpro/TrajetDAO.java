package com.transportpro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accès aux trajets planifiés : table « affectation » jointe à « trajet » (ligne), « bus »,
 * « chauffeur » + « employe » (nom du chauffeur) et « inscription » (nombre d'inscrits).
 * Les règles métier (bus disponible, chauffeur actif, permis valide, pas de chevauchement...)
 * sont appliquées par les déclencheurs de la base ; leurs messages sont repris tels quels.
 */
public class TrajetDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class TrajetException extends Exception {
        public TrajetException(String message, Throwable cause) { super(message, cause); }
    }

    /** Élément d'une liste déroulante (ligne, bus, chauffeur). */
    public record Option(int id, String label) {
        @Override
        public String toString() { return label; }
    }

    private static final String SELECT_JOIN =
            "SELECT a.id_affectation, a.id_trajet, a.id_bus, a.id_chauffeur, a.date_trajet, "
                    + "a.heure_depart, a.heure_arrivee, a.statut, "
                    + "t.nom AS ligne, t.lieu_depart, t.lieu_arrivee, "
                    + "b.immatriculation, b.capacite, e.nom, e.prenom, "
                    + "(SELECT COUNT(*) FROM inscription i WHERE i.id_affectation = a.id_affectation "
                    + "AND i.statut = 'CONFIRMEE') AS inscrits "
                    + "FROM affectation a "
                    + "JOIN trajet t ON t.id_trajet = a.id_trajet "
                    + "JOIN bus b ON b.id_bus = a.id_bus "
                    + "JOIN chauffeur c ON c.id_chauffeur = a.id_chauffeur "
                    + "JOIN employe e ON e.id_employe = c.id_employe";
    private static final String ORDER = " ORDER BY a.date_trajet DESC, a.heure_depart, a.id_affectation";
    private static final String INSERT =
            "INSERT INTO affectation (id_trajet, id_bus, id_chauffeur, date_trajet, heure_depart, "
                    + "heure_arrivee, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE affectation SET id_trajet = ?, id_bus = ?, id_chauffeur = ?, date_trajet = ?, "
                    + "heure_depart = ?, heure_arrivee = ?, statut = ? WHERE id_affectation = ?";
    private static final String DELETE = "DELETE FROM affectation WHERE id_affectation = ?";

    private static final int SEARCH_FIELDS = 9;

    public List<Trajet> findAll() throws TrajetException {
        List<Trajet> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + ORDER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger les trajets.");
        }
        return list;
    }

    /**
     * Recherche (SQL LIKE) dans le nom de la ligne, le départ, l'arrivée, la plaque du bus,
     * le nom et le prénom du chauffeur, la date (jj/mm/aaaa), l'heure de départ (hh:mm) et le numéro (T12).
     * Chaque mot saisi doit se retrouver dans l'un de ces champs ; texte vide = tous les trajets.
     */
    public List<Trajet> search(String text) throws TrajetException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();

        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE ");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(t.nom LIKE ? ESCAPE '!' OR t.lieu_depart LIKE ? ESCAPE '!' ")
               .append("OR t.lieu_arrivee LIKE ? ESCAPE '!' OR b.immatriculation LIKE ? ESCAPE '!' ")
               .append("OR e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' ")
               .append("OR DATE_FORMAT(a.date_trajet, '%d/%m/%Y') LIKE ? ESCAPE '!' ")
               .append("OR TIME_FORMAT(a.heure_depart, '%H:%i') LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('T', a.id_affectation) LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);

        List<Trajet> list = new ArrayList<>();
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
            throw translate(e, "Impossible de rechercher les trajets.");
        }
        return list;
    }

    public Trajet findById(int id) throws TrajetException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE a.id_affectation = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire le trajet.");
        }
        throw new TrajetException("Ce trajet n'existe plus dans la base.", null);
    }

    public void insert(Trajet t) throws TrajetException {
        check(t);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, t);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) t.setIdAffectation(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible d'ajouter le trajet.");
        }
    }

    public void update(Trajet t) throws TrajetException {
        check(t);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, t);
            ps.setInt(8, t.getIdAffectation());
            if (ps.executeUpdate() == 0)
                throw new TrajetException("Ce trajet n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier le trajet.");
        }
    }

    public void delete(int id) throws TrajetException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0)
                throw new TrajetException("Ce trajet n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer le trajet.");
        }
    }

    // ------------------------------------------------------------------ listes du formulaire

    /** Valeurs autorisées pour le statut, lues dans l'ENUM de la colonne affectation.statut. */
    public List<String> findStatuts() throws TrajetException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'affectation' AND COLUMN_NAME = 'statut'";
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
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire les statuts.");
        }
        if (values.isEmpty()) values.add("PLANIFIEE");
        return values;
    }

    /** Lignes de transport (table trajet). */
    public List<Option> findLignes() throws TrajetException {
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id_trajet, nom FROM trajet ORDER BY nom");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Option(rs.getInt("id_trajet"), nz(rs.getString("nom"))));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger les lignes.");
        }
        return list;
    }

    /** Bus DISPONIBLES, plus le bus indiqué (pour la modification ; 0 = aucun). */
    public List<Option> findAvailableBuses(int includeBusId) throws TrajetException {
        String sql = "SELECT id_bus, immatriculation, capacite FROM bus "
                + "WHERE statut = 'DISPONIBLE' OR id_bus = ? ORDER BY immatriculation";
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeBusId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Option(rs.getInt("id_bus"),
                            nz(rs.getString("immatriculation")) + " (" + rs.getInt("capacite") + " places)"));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des bus.");
        }
        return list;
    }

    /** Chauffeurs ACTIFS dont l'employé est actif, plus le chauffeur indiqué (0 = aucun). */
    public List<Option> findAvailableDrivers(int includeDriverId) throws TrajetException {
        String sql = "SELECT c.id_chauffeur, c.numero_permis, e.nom, e.prenom "
                + "FROM chauffeur c JOIN employe e ON e.id_employe = c.id_employe "
                + "WHERE (c.statut = 'ACTIF' AND e.actif = 1) OR c.id_chauffeur = ? "
                + "ORDER BY e.nom, e.prenom";
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeDriverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = (nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))).trim()
                            + " (" + nz(rs.getString("numero_permis")) + ")";
                    list.add(new Option(rs.getInt("id_chauffeur"), label));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des chauffeurs.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static String nz(String s) { return s == null ? "" : s; }

    private static void bind(PreparedStatement ps, Trajet t) throws SQLException {
        ps.setInt(1, t.getIdTrajet());
        ps.setInt(2, t.getIdBus());
        ps.setInt(3, t.getIdChauffeur());
        ps.setDate(4, Date.valueOf(t.getDateTrajet()));
        ps.setTime(5, Time.valueOf(t.getHeureDepart()));
        ps.setTime(6, Time.valueOf(t.getHeureArrivee()));
        ps.setString(7, t.getStatut());
    }

    private Trajet map(ResultSet rs) throws SQLException {
        Trajet t = new Trajet();
        t.setIdAffectation(rs.getInt("id_affectation"));
        t.setIdTrajet(rs.getInt("id_trajet"));
        t.setIdBus(rs.getInt("id_bus"));
        t.setIdChauffeur(rs.getInt("id_chauffeur"));
        Date d = rs.getDate("date_trajet");
        t.setDateTrajet(d == null ? null : d.toLocalDate());
        Time dep = rs.getTime("heure_depart");
        t.setHeureDepart(dep == null ? null : dep.toLocalTime());
        Time arr = rs.getTime("heure_arrivee");
        t.setHeureArrivee(arr == null ? null : arr.toLocalTime());
        t.setStatut(rs.getString("statut"));
        t.setNomLigne(nz(rs.getString("ligne")));
        t.setLieuDepart(nz(rs.getString("lieu_depart")));
        t.setLieuArrivee(nz(rs.getString("lieu_arrivee")));
        t.setImmatriculation(nz(rs.getString("immatriculation")));
        t.setCapacite(rs.getInt("capacite"));
        t.setChauffeurNom((nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))).trim());
        t.setInscrits(rs.getInt("inscrits"));
        return t;
    }

    private void check(Trajet t) throws TrajetException {
        String err = t.validate();
        if (err != null) throw new TrajetException(err, null);
    }

    /** Message d'un déclencheur de la base : sans préfixe de connexion ni requête. */
    private static String cleanTriggerMessage(String raw) {
        String m = raw == null ? "" : raw;
        int nl = m.indexOf('\n');
        if (nl >= 0) m = m.substring(0, nl);
        m = m.replaceFirst("^\\(conn=\\d+\\)\\s*", "").trim();
        return m.isEmpty() ? "Opération refusée par la base de données." : m;
    }

    private TrajetException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new TrajetException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        // règles métier des déclencheurs (bus indisponible, permis expiré, chevauchement...)
        if ("45000".equals(state))
            return new TrajetException(cleanTriggerMessage(e.getMessage()), e);
        switch (e.getErrorCode()) {
            case 1451:
                return new TrajetException(
                        "Ce trajet a déjà des inscriptions ou des incidents : suppression impossible. "
                                + "Passez-le plutôt au statut « Annulé ».", e);
            case 1452:
                return new TrajetException("La ligne, le bus ou le chauffeur choisi n'existe pas dans la base.", e);
            case 1265:
            case 1366:
                return new TrajetException("Le statut choisi n'est pas accepté par la base.", e);
            case 1292:
                return new TrajetException("La date ou l'heure saisie n'est pas valide.", e);
            default:
                return new TrajetException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}