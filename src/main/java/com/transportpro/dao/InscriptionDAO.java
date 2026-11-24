package com.transportpro.dao;

import com.transportpro.model.Employe;
import com.transportpro.model.Inscription;
import com.transportpro.model.Trajet;
import com.transportpro.model.enums.StatutPresence;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table « inscription » (BF15 à BF19), jointe à l'employé, à l'affectation (trajet daté),
 * à la ligne et à la présence. Les règles (employé actif, capacité, pas de double inscription) sont
 * vérifiées par les déclencheurs de la base (sp_verifier_inscription) ; leurs messages sont repris tels quels.
 */
public class InscriptionDAO {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DUPLICATE = "Cet employé a déjà une inscription pour ce trajet. "
            + "Si elle est annulée, modifiez-la pour la repasser au statut « Confirmée ».";
    private static final String IN_USE = "Cette inscription a une présence enregistrée : annulez-la plutôt.";

    private static final String SELECT_JOIN = """
            SELECT i.id_inscription, i.date_inscription, i.statut,
                   e.id_employe, e.matricule, e.nom, e.prenom,
                   a.id_affectation, a.date_trajet, a.heure_depart, a.heure_arrivee,
                   t.nom AS ligne, t.lieu_arrivee,
                   p.statut AS presence
            FROM inscription i
            JOIN employe e ON e.id_employe = i.id_employe
            JOIN affectation a ON a.id_affectation = i.id_affectation
            JOIN trajet t ON t.id_trajet = a.id_trajet
            LEFT JOIN presence p ON p.id_inscription = i.id_inscription
            """;
    private static final String ORDER = " ORDER BY i.date_inscription DESC, i.id_inscription DESC";

    public List<Inscription> findAll() throws SQLException {
        return search("");
    }

    /**
     * Recherche dans le nom, le prénom et le matricule de l'employé, la ligne, la date du trajet (jj/mm/aaaa)
     * et les numéros (I12, T5) ; texte vide = toutes les inscriptions.
     */
    public List<Inscription> search(String text) throws SQLException {
        String t = text == null ? "" : text.trim();
        String[] tokens = t.isEmpty() ? new String[0] : t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN);
        for (int i = 0; i < tokens.length; i++) {
            sql.append(i == 0 ? " WHERE " : " AND ");
            sql.append("(e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' OR e.matricule LIKE ? ESCAPE '!' ")
               .append("OR t.nom LIKE ? ESCAPE '!' OR DATE_FORMAT(a.date_trajet, '%d/%m/%Y') LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('I', i.id_inscription) LIKE ? ESCAPE '!' OR CONCAT('T', a.id_affectation) LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);
        List<Inscription> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = EmployeDAO.like(tok);
                for (int k = 0; k < 7; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger les inscriptions.");
        }
        return list;
    }

    /** Retourne l'inscription, ou null si elle n'existe pas. */
    public Inscription findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE i.id_inscription = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de lire l'inscription.");
        }
    }

    /** BF15 / BF17 : la base vérifie employé actif, places restantes et absence de doublon. */
    public void insert(Inscription i) throws SQLException {
        check(i);
        String sql = "INSERT INTO inscription (id_employe, id_affectation, statut) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, i.getIdEmploye());
            ps.setInt(2, i.getIdAffectation());
            ps.setString(3, i.getStatut());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) i.setIdInscription(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, DUPLICATE, IN_USE, "Impossible d'ajouter l'inscription.");
        }
    }

    public void update(Inscription i) throws SQLException {
        check(i);
        String sql = "UPDATE inscription SET id_employe = ?, id_affectation = ?, statut = ? WHERE id_inscription = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, i.getIdEmploye());
            ps.setInt(2, i.getIdAffectation());
            ps.setString(3, i.getStatut());
            ps.setInt(4, i.getIdInscription());
            if (ps.executeUpdate() == 0) throw new DaoException("Cette inscription n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, DUPLICATE, IN_USE, "Impossible de modifier l'inscription.");
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "DELETE FROM inscription WHERE id_inscription = ?", id) == 0)
                throw new DaoException("Cette inscription n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, DUPLICATE, IN_USE, "Impossible de supprimer l'inscription.");
        }
    }

    // ------------------------------------------------------------------ présence (BF18)

    /**
     * Enregistre la présence d'une inscription : PRESENT / ABSENT / RETARD écrit dans la table presence,
     * NON_RENSEIGNE supprime la ligne de présence.
     */
    public void enregistrerPresence(int idInscription, StatutPresence statut) throws SQLException {
        if (statut == null) throw new DaoException("Le statut de présence ne peut pas être vide.");
        try (Connection c = DatabaseConnection.getConnection()) {
            if (statut == StatutPresence.NON_RENSEIGNE) {
                DatabaseConnection.exec(c, "DELETE FROM presence WHERE id_inscription = ?", idInscription);
                return;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO presence (id_inscription, statut) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE statut = VALUES(statut)")) {
                ps.setInt(1, idInscription);
                ps.setString(2, statut.name());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible d'enregistrer la présence.");
        }
    }

    // ------------------------------------------------------------------ annulation / suppression complète

    /** Nombre de présences enregistrées pour cette inscription (0 ou 1). */
    public int countPresences(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return DatabaseConnection.count(c, "SELECT COUNT(*) FROM presence WHERE id_inscription = ?", id);
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de vérifier les présences.");
        }
    }

    /** BF16 : annule l'inscription (statut ANNULEE), la présence éventuelle est conservée. */
    public void annuler(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "UPDATE inscription SET statut = 'ANNULEE' WHERE id_inscription = ?", id) == 0)
                throw new DaoException("Cette inscription n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible d'annuler l'inscription.");
        }
    }

    /** Supprime l'inscription et sa présence éventuelle, en une seule transaction. */
    public void deleteWithHistory(int id) throws SQLException {
        try {
            DatabaseConnection.inTransaction(c -> {
                DatabaseConnection.exec(c, "DELETE FROM presence WHERE id_inscription = ?", id);
                if (DatabaseConnection.exec(c, "DELETE FROM inscription WHERE id_inscription = ?", id) == 0)
                    throw new DaoException("Cette inscription n'existe plus dans la base.");
                return null;
            });
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible de supprimer l'inscription.");
        }
    }

    // ------------------------------------------------------------------ listes du formulaire

    /** Employés actifs, plus l'employé indiqué (modification ; 0 = aucun). */
    public List<Option> findEmployes(int includeId) throws SQLException {
        String sql = "SELECT id_employe, matricule, nom, prenom FROM employe WHERE actif = 1 OR id_employe = ? "
                + "ORDER BY nom, prenom";
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, includeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Option(rs.getInt("id_employe"), (rs.getString("nom") + " " + rs.getString("prenom")).trim()
                            + " (" + rs.getString("matricule") + ")"));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger la liste des employés.");
        }
        return list;
    }

    /** Trajets PLANIFIEE ou EN_COURS, plus celui indiqué (0 = aucun). */
    public List<Option> findAffectations(int includeId) throws SQLException {
        String sql = "SELECT a.id_affectation, t.nom, a.date_trajet, a.heure_depart "
                + "FROM affectation a JOIN trajet t ON t.id_trajet = a.id_trajet "
                + "WHERE a.statut IN ('PLANIFIEE', 'EN_COURS') OR a.id_affectation = ? "
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
                            + " " + (h == null ? "" : h.toLocalTime().format(TIME_FMT))));
                }
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger la liste des trajets.");
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static void check(Inscription i) throws DaoException {
        String err = i.validate();
        if (err != null) throw new DaoException(err);
    }

    private static Inscription map(ResultSet rs) throws SQLException {
        Employe e = new Employe();
        e.setIdEmploye(rs.getInt("id_employe"));
        e.setMatricule(rs.getString("matricule"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));

        Date d = rs.getDate("date_trajet");
        Time dep = rs.getTime("heure_depart");
        Time arr = rs.getTime("heure_arrivee");
        Trajet t = new Trajet();
        t.setIdTrajet(rs.getInt("id_affectation"));   // un Trajet daté = une affectation
        t.setDate(d == null ? null : d.toLocalDate());
        t.setHeureDepart(dep == null ? null : dep.toLocalTime());
        t.setHeureArrivee(arr == null ? null : arr.toLocalTime());
        t.setNomLigne(rs.getString("ligne"));
        t.setDestination(rs.getString("lieu_arrivee"));

        Timestamp ts = rs.getTimestamp("date_inscription");
        String p = rs.getString("presence");
        StatutPresence presence = p == null ? StatutPresence.NON_RENSEIGNE : StatutPresence.valueOf(p);

        Inscription i = new Inscription(rs.getInt("id_inscription"),
                ts == null ? null : ts.toLocalDateTime().toLocalDate(), presence, e, t);
        i.setStatut(rs.getString("statut"));
        return i;
    }
}
