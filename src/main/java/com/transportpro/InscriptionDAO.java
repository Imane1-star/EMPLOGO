package com.transportpro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accès aux inscriptions : table « inscription » jointe à « employe », « affectation » et « trajet ».
 * Les règles métier (employé actif, affectation non annulée, capacité du bus, pas de double inscription
 * au même trajet le même jour) sont appliquées par les déclencheurs de la base ; leurs messages sont
 * repris tels quels.
 */
public class InscriptionDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class InscriptionException extends Exception {
        public InscriptionException(String message, Throwable cause) { super(message, cause); }
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SELECT_JOIN =
            "SELECT i.id_inscription, i.id_employe, i.id_affectation, i.date_inscription, i.statut, "
                    + "e.matricule, e.nom, e.prenom, t.nom AS ligne, "
                    + "a.date_trajet, a.heure_depart, a.heure_arrivee "
                    + "FROM inscription i "
                    + "JOIN employe e ON e.id_employe = i.id_employe "
                    + "JOIN affectation a ON a.id_affectation = i.id_affectation "
                    + "JOIN trajet t ON t.id_trajet = a.id_trajet";
    private static final String ORDER = " ORDER BY i.date_inscription DESC, i.id_inscription DESC";
    // date_inscription n'est pas fournie : la base applique DEFAULT current_timestamp()
    private static final String INSERT =
            "INSERT INTO inscription (id_employe, id_affectation, statut) VALUES (?, ?, ?)";
    private static final String UPDATE =
            "UPDATE inscription SET id_employe = ?, id_affectation = ?, statut = ? WHERE id_inscription = ?";
    private static final String DELETE = "DELETE FROM inscription WHERE id_inscription = ?";

    private static final int SEARCH_FIELDS = 7;

    public List<Inscription> findAll() throws InscriptionException {
        List<Inscription> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + ORDER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger les inscriptions.");
        }
        return list;
    }

    /**
     * Recherche (SQL LIKE) dans le nom, le prénom et le matricule de l'employé, le nom de la ligne,
     * la date du trajet (jj/mm/aaaa), le numéro d'inscription (I12) et d'affectation (T5).
     * Chaque mot saisi doit se retrouver dans l'un de ces champs ; texte vide = toutes les inscriptions.
     */
    public List<Inscription> search(String text) throws InscriptionException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();

        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE ");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' ")
               .append("OR e.matricule LIKE ? ESCAPE '!' OR t.nom LIKE ? ESCAPE '!' ")
               .append("OR DATE_FORMAT(a.date_trajet, '%d/%m/%Y') LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('I', i.id_inscription) LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('T', a.id_affectation) LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);

        List<Inscription> list = new ArrayList<>();
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
            throw translate(e, "Impossible de rechercher les inscriptions.");
        }
        return list;
    }

    public Inscription findById(int id) throws InscriptionException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE i.id_inscription = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire l'inscription.");
        }
        throw new InscriptionException("Cette inscription n'existe plus dans la base.", null);
    }

    public void insert(Inscription i) throws InscriptionException {
        check(i);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, i.getIdEmploye());
            ps.setInt(2, i.getIdAffectation());
            ps.setString(3, i.getStatut());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) i.setIdInscription(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible d'ajouter l'inscription.");
        }
    }

    public void update(Inscription i) throws InscriptionException {
        check(i);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setInt(1, i.getIdEmploye());
            ps.setInt(2, i.getIdAffectation());
            ps.setString(3, i.getStatut());
            ps.setInt(4, i.getIdInscription());
            if (ps.executeUpdate() == 0)
                throw new InscriptionException("Cette inscription n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier l'inscription.");
        }
    }

    public void delete(int id) throws InscriptionException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0)
                throw new InscriptionException("Cette inscription n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer l'inscription.");
        }
    }

    // ------------------------------------------------------------------ listes du formulaire

    /** Valeurs autorisées pour le statut, lues dans l'ENUM de la colonne inscription.statut. */
    public List<String> findStatuts() throws InscriptionException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inscription' AND COLUMN_NAME = 'statut'";
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
        if (values.isEmpty()) values.add("CONFIRMEE");
        return values;
    }

    /** Employés actifs, plus l'employé indiqué (pour la modification ; 0 = aucun). */
    public List<TrajetDAO.Option> findEmployes(int includeId) throws InscriptionException {
        String sql = "SELECT id_employe, matricule, nom, prenom FROM employe "
                + "WHERE actif = 1 OR id_employe = ? ORDER BY nom, prenom";
        List<TrajetDAO.Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = (nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))).trim()
                            + " (" + nz(rs.getString("matricule")) + ")";
                    list.add(new TrajetDAO.Option(rs.getInt("id_employe"), label));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des employés.");
        }
        return list;
    }

    /** Affectations PLANIFIEE ou EN_COURS, plus l'affectation indiquée (0 = aucune). */
    public List<TrajetDAO.Option> findAffectations(int includeId) throws InscriptionException {
        String sql = "SELECT a.id_affectation, t.nom, a.date_trajet, a.heure_depart "
                + "FROM affectation a JOIN trajet t ON t.id_trajet = a.id_trajet "
                + "WHERE a.statut IN ('PLANIFIEE', 'EN_COURS') OR a.id_affectation = ? "
                + "ORDER BY a.date_trajet DESC, a.heure_depart, a.id_affectation";
        List<TrajetDAO.Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("date_trajet");
                    Time h = rs.getTime("heure_depart");
                    String label = "T" + rs.getInt("id_affectation") + " • " + nz(rs.getString("nom"))
                            + " • " + (d == null ? "" : d.toLocalDate().format(DATE_FMT))
                            + " " + (h == null ? "" : h.toLocalTime().format(TIME_FMT));
                    list.add(new TrajetDAO.Option(rs.getInt("id_affectation"), label));
                }
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des affectations.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static String nz(String s) { return s == null ? "" : s; }

    private Inscription map(ResultSet rs) throws SQLException {
        Inscription i = new Inscription();
        i.setIdInscription(rs.getInt("id_inscription"));
        i.setIdEmploye(rs.getInt("id_employe"));
        i.setIdAffectation(rs.getInt("id_affectation"));
        Timestamp ts = rs.getTimestamp("date_inscription");
        i.setDateInscription(ts == null ? null : ts.toLocalDateTime());
        i.setStatut(rs.getString("statut"));
        i.setMatricule(nz(rs.getString("matricule")));
        i.setEmployeNom((nz(rs.getString("nom")) + " " + nz(rs.getString("prenom"))).trim());
        i.setNomLigne(nz(rs.getString("ligne")));
        Date d = rs.getDate("date_trajet");
        i.setDateTrajet(d == null ? null : d.toLocalDate());
        Time dep = rs.getTime("heure_depart");
        i.setHeureDepart(dep == null ? null : dep.toLocalTime());
        Time arr = rs.getTime("heure_arrivee");
        i.setHeureArrivee(arr == null ? null : arr.toLocalTime());
        return i;
    }

    private void check(Inscription i) throws InscriptionException {
        String err = i.validate();
        if (err != null) throw new InscriptionException(err, null);
    }

    /** Message d'un déclencheur de la base : sans préfixe de connexion ni requête. */
    private static String cleanTriggerMessage(String raw) {
        String m = raw == null ? "" : raw;
        int nl = m.indexOf('\n');
        if (nl >= 0) m = m.substring(0, nl);
        m = m.replaceFirst("^\\(conn=\\d+\\)\\s*", "").trim();
        return m.isEmpty() ? "Opération refusée par la base de données." : m;
    }

    private InscriptionException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new InscriptionException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        // règles métier des déclencheurs (employé inactif, capacité atteinte, double inscription...)
        if ("45000".equals(state))
            return new InscriptionException(cleanTriggerMessage(e.getMessage()), e);
        switch (e.getErrorCode()) {
            case 1062:
                return new InscriptionException(
                        "Cet employé a déjà une inscription pour cette affectation. "
                                + "Si elle est annulée, modifiez-la pour la repasser au statut « Confirmée ».", e);
            case 1451:
                return new InscriptionException(
                        "Cette inscription a déjà une présence enregistrée : suppression impossible. "
                                + "Passez-la plutôt au statut « Annulée ».", e);
            case 1452:
                return new InscriptionException("L'employé ou l'affectation choisi n'existe pas dans la base.", e);
            case 1265:
            case 1366:
                return new InscriptionException("Le statut choisi n'est pas accepté par la base.", e);
            default:
                return new InscriptionException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}
