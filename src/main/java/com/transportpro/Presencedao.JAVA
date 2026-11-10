package com.transportpro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table « presence » (réutilise DatabaseConnection).
 * Relations : presence -> inscription -> employe
 *                                     -> affectation -> trajet
 */
public class PresenceDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class PresenceException extends Exception {
        public PresenceException(String message, Throwable cause) { super(message, cause); }
    }

    // ------------------------------------------------------------------ requêtes SQL
    private static final String SELECT_PRESENCE =
            "SELECT p.id_presence, p.id_inscription, p.statut, p.heure_montee, p.remarque, "
          + "       e.id_employe, e.matricule, e.nom AS employe_nom, e.prenom AS employe_prenom, "
          + "       e.departement, "
          + "       a.id_affectation, a.date_trajet, a.heure_depart, "
          + "       t.nom AS trajet_nom, t.lieu_depart, t.lieu_arrivee "
          + "FROM presence p "
          + "JOIN inscription i ON i.id_inscription = p.id_inscription "
          + "JOIN employe e ON e.id_employe = i.id_employe "
          + "JOIN affectation a ON a.id_affectation = i.id_affectation "
          + "JOIN trajet t ON t.id_trajet = a.id_trajet ";

    private static final String ORDER_PRESENCE =
            " ORDER BY a.date_trajet DESC, a.heure_depart, e.nom, e.prenom";

    /** Inscriptions confirmées, sur une affectation non annulée, qui n'ont pas encore de présence. */
    private static final String SELECT_AVAILABLE =
            "SELECT i.id_inscription, "
          + "       e.id_employe, e.matricule, e.nom AS employe_nom, e.prenom AS employe_prenom, "
          + "       e.departement, "
          + "       a.id_affectation, a.date_trajet, a.heure_depart, "
          + "       t.nom AS trajet_nom, t.lieu_depart, t.lieu_arrivee "
          + "FROM inscription i "
          + "JOIN employe e ON e.id_employe = i.id_employe "
          + "JOIN affectation a ON a.id_affectation = i.id_affectation "
          + "JOIN trajet t ON t.id_trajet = a.id_trajet "
          + "WHERE i.statut = 'CONFIRMEE' "
          + "  AND a.statut <> 'ANNULEE' "
          + "  AND NOT EXISTS (SELECT 1 FROM presence p WHERE p.id_inscription = i.id_inscription) "
          + "ORDER BY a.date_trajet DESC, a.heure_depart, e.nom, e.prenom";

    private static final String SELECT_INSCRIPTION_STATUT =
            "SELECT statut FROM inscription WHERE id_inscription = ?";

    private static final String INSERT =
            "INSERT INTO presence (id_inscription, statut, heure_montee, remarque) VALUES (?, ?, ?, ?)";

    /** id_inscription n'est volontairement pas modifiable : une présence reste liée à son inscription. */
    private static final String UPDATE =
            "UPDATE presence SET statut = ?, heure_montee = ?, remarque = ? WHERE id_presence = ?";

    private static final String DELETE = "DELETE FROM presence WHERE id_presence = ?";

    // ------------------------------------------------------------------ lecture
    /** Toutes les présences, de la plus récente à la plus ancienne. */
    public List<Presence> findAll() throws PresenceException {
        List<Presence> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_PRESENCE + ORDER_PRESENCE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des présences.");
        }
        return list;
    }

    public Presence findById(int id) throws PresenceException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_PRESENCE + "WHERE p.id_presence = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire la présence.");
        }
        throw new PresenceException("Cette présence n'existe plus dans la base.", null);
    }

    /**
     * Inscriptions pour lesquelles on peut encore saisir une présence (liste du formulaire d'ajout).
     * Chaque objet retourné contient idInscription + infos employé/trajet/date ;
     * idPresence = 0 et statut = null.
     */
    public List<Presence> findAvailableInscriptions() throws PresenceException {
        List<Presence> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_AVAILABLE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Presence p = new Presence();
                p.setIdInscription(rs.getInt("id_inscription"));
                fillLinkedInfo(p, rs);
                list.add(p);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des inscriptions disponibles.");
        }
        return list;
    }

    // ------------------------------------------------------------------ écriture
    public void insert(Presence presence) throws PresenceException {
        check(presence);
        try (Connection c = DatabaseConnection.getConnection()) {
            // Contrôle : l'inscription doit exister et être confirmée.
            try (PreparedStatement ps = c.prepareStatement(SELECT_INSCRIPTION_STATUT)) {
                ps.setInt(1, presence.getIdInscription());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        throw new PresenceException("Cette inscription n'existe pas.", null);
                    if (!"CONFIRMEE".equals(rs.getString("statut")))
                        throw new PresenceException(
                                "Cette inscription est annulée : impossible d'enregistrer une présence.", null);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, presence.getIdInscription());
                ps.setString(2, presence.getStatut());
                setTime(ps, 3, presence);
                setNullable(ps, 4, presence.getRemarque());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) presence.setIdPresence(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible d'ajouter la présence.");
        }
    }

    public void update(Presence presence) throws PresenceException {
        check(presence);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, presence.getStatut());
            setTime(ps, 2, presence);
            setNullable(ps, 3, presence.getRemarque());
            ps.setInt(4, presence.getIdPresence());
            if (ps.executeUpdate() == 0)
                throw new PresenceException("Cette présence n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier la présence.");
        }
    }

    public void delete(int id) throws PresenceException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0)
                throw new PresenceException("Cette présence n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer la présence.");
        }
    }

    // ------------------------------------------------------------------ recherche / filtres
    /**
     * Recherche par matricule, nom, prénom, département, nom du trajet ou remarque
     * (SQL LIKE, insensible à la casse). Chaque mot saisi doit se retrouver dans l'un de ces champs ;
     * texte vide = toutes les présences.
     */
    public List<Presence> search(String keyword) throws PresenceException {
        return searchAndFilter(keyword, null, null);
    }

    /**
     * Filtre par date du trajet (affectation.date_trajet) et/ou par statut
     * (PRESENT, ABSENT, RETARD). Un paramètre null (ou statut vide / « TOUS ») est ignoré.
     */
    public List<Presence> filterByDateAndStatus(LocalDate date, String statut) throws PresenceException {
        return searchAndFilter(null, date, statut);
    }

    /** Recherche + filtres combinés (utile pour la vue : champ de recherche + date + statut). */
    public List<Presence> searchAndFilter(String keyword, LocalDate date, String statut)
            throws PresenceException {

        String st = normalizeStatut(statut);

        String kw = keyword == null ? "" : keyword.trim();
        String[] tokens = kw.isEmpty() ? new String[0] : kw.split("\\s+");

        StringBuilder sql = new StringBuilder(SELECT_PRESENCE);
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        for (String tok : tokens) {
            conditions.add("(e.matricule LIKE ? ESCAPE '!' OR e.nom LIKE ? ESCAPE '!' "
                    + "OR e.prenom LIKE ? ESCAPE '!' OR e.departement LIKE ? ESCAPE '!' "
                    + "OR t.nom LIKE ? ESCAPE '!' OR p.remarque LIKE ? ESCAPE '!')");
            String like = "%" + tok.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            for (int k = 0; k < 6; k++) params.add(like);
        }
        if (date != null) {
            conditions.add("a.date_trajet = ?");
            params.add(Date.valueOf(date));
        }
        if (!st.isEmpty()) {
            conditions.add("p.statut = ?");
            params.add(st);
        }
        if (!conditions.isEmpty())
            sql.append("WHERE ").append(String.join(" AND ", conditions));
        sql.append(ORDER_PRESENCE);

        List<Presence> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Date d) ps.setDate(i + 1, d);
                else ps.setString(i + 1, (String) v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de rechercher les présences.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    /**
     * Transforme ce que la vue envoie (code SQL « PRESENT » ou libellé affiché « Présent »,
     * « Tous », « Tous les statuts », null, vide...) en code SQL valide,
     * ou en chaîne vide quand il ne faut pas filtrer par statut.
     */
    private String normalizeStatut(String statut) throws PresenceException {
        if (statut == null) return "";
        String s = statut.trim();
        if (s.isEmpty()) return "";
        String lower = s.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("tous") || lower.startsWith("toutes") || lower.equals("all")) return "";
        String upper = s.toUpperCase(java.util.Locale.ROOT);
        if (Presence.isStatutValide(upper)) return upper;
        String fromLabel = Presence.statutFromLabel(s);
        if (fromLabel != null) return fromLabel;
        throw new PresenceException("Le statut doit être Présent, Absent ou Retard.", null);
    }

    private Presence map(ResultSet rs) throws SQLException {
        Presence p = new Presence();
        p.setIdPresence(rs.getInt("id_presence"));
        p.setIdInscription(rs.getInt("id_inscription"));
        p.setStatut(rs.getString("statut"));
        Time hm = rs.getTime("heure_montee");
        p.setHeureMontee(hm == null ? null : hm.toLocalTime());
        p.setRemarque(rs.getString("remarque"));
        fillLinkedInfo(p, rs);
        return p;
    }

    /** Colonnes communes aux requêtes de présences et d'inscriptions disponibles. */
    private void fillLinkedInfo(Presence p, ResultSet rs) throws SQLException {
        p.setIdEmploye(rs.getInt("id_employe"));
        p.setMatricule(rs.getString("matricule"));
        p.setNomEmploye(rs.getString("employe_nom"));
        p.setPrenomEmploye(rs.getString("employe_prenom"));
        p.setDepartement(rs.getString("departement"));
        p.setIdAffectation(rs.getInt("id_affectation"));
        Date d = rs.getDate("date_trajet");
        p.setDateTrajet(d == null ? null : d.toLocalDate());
        Time hd = rs.getTime("heure_depart");
        p.setHeureDepart(hd == null ? null : hd.toLocalTime());
        p.setTrajetNom(rs.getString("trajet_nom"));
        p.setLieuDepart(rs.getString("lieu_depart"));
        p.setLieuArrivee(rs.getString("lieu_arrivee"));
    }

    private void check(Presence p) throws PresenceException {
        String err = p.validate();
        if (err != null) throw new PresenceException(err, null);
    }

    /** Un absent n'est jamais monté dans le bus : l'heure de montée est alors enregistrée à NULL. */
    private void setTime(PreparedStatement ps, int idx, Presence p) throws SQLException {
        if (p.getHeureMontee() == null || Presence.ABSENT.equals(p.getStatut()))
            ps.setNull(idx, Types.TIME);
        else
            ps.setTime(idx, Time.valueOf(p.getHeureMontee()));
    }

    private void setNullable(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v.trim());
    }

    private PresenceException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new PresenceException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        switch (e.getErrorCode()) {
            case 1062:
                return new PresenceException(
                        "Une présence est déjà enregistrée pour cette inscription.", e);
            case 1452:
                return new PresenceException("L'inscription choisie n'existe pas.", e);
            case 1451:
                return new PresenceException(
                        "Cette présence est utilisée par d'autres données. Suppression impossible.", e);
            case 1406:
                return new PresenceException("Une valeur est trop longue pour la base de données.", e);
            default:
                return new PresenceException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}