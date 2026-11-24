package com.transportpro.dao;

import com.transportpro.model.Affectation;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table « affectation » : une ligne (trajet) planifiée à une date, avec un bus et un chauffeur
 * (BF9, BF10, BF12 à BF14). Les règles métier (bus disponible, chauffeur actif, permis valide,
 * pas de chevauchement, capacité) sont vérifiées par les déclencheurs de la base (sp_verifier_affectation) ;
 * leurs messages sont repris tels quels.
 */
public class AffectationDAO {

    private static final String SELECT_JOIN = """
            SELECT a.id_affectation, a.id_trajet, a.id_bus, a.id_chauffeur, a.date_trajet,
                   a.heure_depart, a.heure_arrivee, a.statut,
                   t.nom AS ligne, t.lieu_depart, t.lieu_arrivee,
                   b.immatriculation, b.capacite, e.nom, e.prenom,
                   (SELECT COUNT(*) FROM inscription i WHERE i.id_affectation = a.id_affectation
                       AND i.statut = 'CONFIRMEE') AS inscrits
            FROM affectation a
            JOIN trajet t ON t.id_trajet = a.id_trajet
            JOIN bus b ON b.id_bus = a.id_bus
            JOIN chauffeur c ON c.id_chauffeur = a.id_chauffeur
            JOIN employe e ON e.id_employe = c.id_employe
            """;
    private static final String ORDER = " ORDER BY a.date_trajet DESC, a.heure_depart, a.id_affectation";
    private static final String IN_USE =
            "Ce trajet a déjà des inscriptions ou des incidents : suppression impossible. Annulez-le à la place.";

    /** Récupérer toutes les affectations. */
    public List<Affectation> findAll() throws SQLException {
        List<Affectation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + ORDER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger les trajets.");
        }
        return list;
    }

    /**
     * Recherche dans la ligne, le départ, l'arrivée, la plaque, le chauffeur, la date (jj/mm/aaaa),
     * l'heure (hh:mm) et le numéro (T12) ; texte vide = toutes les affectations.
     */
    public List<Affectation> search(String text) throws SQLException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return findAll();
        String[] tokens = t.split("\\s+");
        StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE ");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(t.nom LIKE ? ESCAPE '!' OR t.lieu_depart LIKE ? ESCAPE '!' OR t.lieu_arrivee LIKE ? ESCAPE '!' ")
               .append("OR b.immatriculation LIKE ? ESCAPE '!' OR e.nom LIKE ? ESCAPE '!' OR e.prenom LIKE ? ESCAPE '!' ")
               .append("OR DATE_FORMAT(a.date_trajet, '%d/%m/%Y') LIKE ? ESCAPE '!' ")
               .append("OR TIME_FORMAT(a.heure_depart, '%H:%i') LIKE ? ESCAPE '!' ")
               .append("OR CONCAT('T', a.id_affectation) LIKE ? ESCAPE '!')");
        }
        sql.append(ORDER);
        List<Affectation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String tok : tokens) {
                String like = EmployeDAO.like(tok);
                for (int k = 0; k < 9; k++) ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de rechercher les trajets.");
        }
        return list;
    }

    /** Retourne l'affectation, ou null si elle n'existe pas. */
    public Affectation findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE a.id_affectation = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de lire le trajet.");
        }
    }

    /** Ajouter une affectation ; retourne le nouvel id (aussi placé dans a). */
    public int insert(Affectation a) throws SQLException {
        check(a);
        String sql = """
            INSERT INTO affectation
            (id_trajet, id_bus, id_chauffeur, date_trajet, heure_depart, heure_arrivee, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, a);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    a.setIdAffectation(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible d'ajouter le trajet.");
        }
        return -1;
    }

    public void update(Affectation a) throws SQLException {
        check(a);
        String sql = """
            UPDATE affectation SET id_trajet = ?, id_bus = ?, id_chauffeur = ?, date_trajet = ?,
                   heure_depart = ?, heure_arrivee = ?, statut = ?
            WHERE id_affectation = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, a);
            ps.setInt(8, a.getIdAffectation());
            if (ps.executeUpdate() == 0) throw new DaoException("Ce trajet n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible de modifier le trajet.");
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "DELETE FROM affectation WHERE id_affectation = ?", id) == 0)
                throw new DaoException("Ce trajet n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible de supprimer le trajet.");
        }
    }

    // ------------------------------------------------------------------ contrôles (BF13, BF14)

    /** Compter les inscriptions confirmées d'une ligne de trajet (toutes dates). */
    public int countConfirmedInscriptions(int idTrajet) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM inscription i
            JOIN affectation a ON i.id_affectation = a.id_affectation
            WHERE a.id_trajet = ? AND i.statut = 'CONFIRMEE'
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTrajet);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Vérifier la disponibilité et la capacité du bus (BF14). */
    public boolean busDisponibleEtSuffisant(int idBus, int nombreEmployes) throws SQLException {
        String sql = "SELECT capacite FROM bus WHERE id_bus = ? AND statut = 'DISPONIBLE'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idBus);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("capacite") >= nombreEmployes;
            }
        }
    }

    /** Vérifier si le chauffeur est ACTIF avec un permis encore valide (BF13). */
    public boolean chauffeurDisponible(int idChauffeur) throws SQLException {
        String sql = "SELECT statut, validite_permis FROM chauffeur WHERE id_chauffeur = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idChauffeur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date validite = rs.getDate("validite_permis");
                    return "ACTIF".equals(rs.getString("statut"))
                            && validite != null
                            && !validite.toLocalDate().isBefore(LocalDate.now());
                }
            }
        }
        return false;
    }

    /** Vérifier les conflits d'horaire pour le bus ou le chauffeur (RG1, RG2). */
    public boolean hasConflit(int idBus, int idChauffeur, LocalDate date,
                              LocalTime heureDepart, LocalTime heureArrivee) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM affectation
            WHERE date_trajet = ?
              AND statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE')
              AND (id_bus = ? OR id_chauffeur = ?)
              AND heure_depart < ?
              AND heure_arrivee > ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, idBus);
            ps.setInt(3, idChauffeur);
            ps.setTime(4, Time.valueOf(heureArrivee));
            ps.setTime(5, Time.valueOf(heureDepart));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ------------------------------------------------------------------ annulation / suppression complète

    /** Données liées à une affectation. */
    public record Usage(int inscriptions, int presences, int incidents) {
        public boolean any() { return inscriptions + presences + incidents > 0; }
    }

    public Usage usage(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return new Usage(
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM inscription WHERE id_affectation = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM presence p JOIN inscription i "
                            + "ON i.id_inscription = p.id_inscription WHERE i.id_affectation = ?", id),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM incident WHERE id_affectation = ?", id));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de vérifier les données liées au trajet.");
        }
    }

    /** BF10 : passe le trajet au statut ANNULEE (inscriptions, présences et incidents conservés). */
    public void annuler(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "UPDATE affectation SET statut = 'ANNULEE' WHERE id_affectation = ?", id) == 0)
                throw new DaoException("Ce trajet n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible d'annuler le trajet.");
        }
    }

    /** Supprime le trajet avec ses présences, inscriptions et incidents, en une seule transaction. */
    public void deleteWithHistory(int id) throws SQLException {
        try {
            DatabaseConnection.inTransaction(c -> {
                DatabaseConnection.exec(c, "DELETE p FROM presence p JOIN inscription i "
                        + "ON i.id_inscription = p.id_inscription WHERE i.id_affectation = ?", id);
                DatabaseConnection.exec(c, "DELETE FROM inscription WHERE id_affectation = ?", id);
                DatabaseConnection.exec(c, "DELETE FROM incident WHERE id_affectation = ?", id);
                if (DatabaseConnection.exec(c, "DELETE FROM affectation WHERE id_affectation = ?", id) == 0)
                    throw new DaoException("Ce trajet n'existe plus dans la base.");
                return null;
            });
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible de supprimer le trajet.");
        }
    }

    // ------------------------------------------------------------------ listes du formulaire

    /** Lignes de transport (table trajet). */
    public List<Option> findLignes() throws SQLException {
        return options("SELECT id_trajet, nom FROM trajet ORDER BY nom", 0, rs -> rs.getString("nom"),
                "Impossible de charger les lignes.");
    }

    /** Bus DISPONIBLES, plus le bus indiqué (modification ; 0 = aucun). */
    public List<Option> findAvailableBuses(int includeBusId) throws SQLException {
        return options("SELECT id_bus, immatriculation, capacite FROM bus "
                        + "WHERE statut = 'DISPONIBLE' OR id_bus = ? ORDER BY immatriculation", includeBusId,
                rs -> rs.getString("immatriculation") + " (" + rs.getInt("capacite") + " places)",
                "Impossible de charger la liste des bus.");
    }

    /** Chauffeurs ACTIFS dont l'employé est actif, plus le chauffeur indiqué (0 = aucun). */
    public List<Option> findAvailableDrivers(int includeDriverId) throws SQLException {
        return options("SELECT c.id_chauffeur, c.numero_permis, e.nom, e.prenom "
                        + "FROM chauffeur c JOIN employe e ON e.id_employe = c.id_employe "
                        + "WHERE (c.statut = 'ACTIF' AND e.actif = 1) OR c.id_chauffeur = ? ORDER BY e.nom, e.prenom",
                includeDriverId,
                rs -> (rs.getString("nom") + " " + rs.getString("prenom")).trim()
                        + " (" + rs.getString("numero_permis") + ")",
                "Impossible de charger la liste des chauffeurs.");
    }

    @FunctionalInterface
    private interface Label { String of(ResultSet rs) throws SQLException; }

    /** Requête « id + texte » ; un éventuel paramètre ? reçoit param. */
    private static List<Option> options(String sql, int param, Label label, String err) throws SQLException {
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (sql.contains("?")) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Option(rs.getInt(1), label.of(rs)));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, err);
        }
        return list;
    }

    // ------------------------------------------------------------------ outils internes
    private static void check(Affectation a) throws DaoException {
        String err = a.validate();
        if (err != null) throw new DaoException(err);
    }

    private static void bind(PreparedStatement ps, Affectation a) throws SQLException {
        ps.setInt(1, a.getIdTrajet());
        ps.setInt(2, a.getIdBus());
        ps.setInt(3, a.getIdChauffeur());
        ps.setDate(4, Date.valueOf(a.getDateTrajet()));
        ps.setTime(5, Time.valueOf(a.getHeureDepart()));
        ps.setTime(6, Time.valueOf(a.getHeureArrivee()));
        ps.setString(7, a.getStatut());
    }

    private static Affectation map(ResultSet rs) throws SQLException {
        Date d = rs.getDate("date_trajet");
        Time dep = rs.getTime("heure_depart");
        Time arr = rs.getTime("heure_arrivee");
        Affectation a = new Affectation(
                rs.getInt("id_trajet"), rs.getInt("id_bus"), rs.getInt("id_chauffeur"),
                d == null ? null : d.toLocalDate(),
                dep == null ? null : dep.toLocalTime(),
                arr == null ? null : arr.toLocalTime(),
                rs.getString("statut"));
        a.setIdAffectation(rs.getInt("id_affectation"));
        a.setNomLigne(rs.getString("ligne"));
        a.setLieuDepart(rs.getString("lieu_depart"));
        a.setLieuArrivee(rs.getString("lieu_arrivee"));
        a.setImmatriculation(rs.getString("immatriculation"));
        a.setCapacite(rs.getInt("capacite"));
        a.setChauffeurNom((rs.getString("nom") + " " + rs.getString("prenom")).trim());
        a.setInscrits(rs.getInt("inscrits"));
        return a;
    }
}
