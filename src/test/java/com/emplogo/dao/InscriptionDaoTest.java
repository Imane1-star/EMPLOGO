package com.emplogo.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.emplogo.dao.DaoException;
import com.emplogo.dao.InscriptionDAO;
import com.emplogo.dao.Option;
import com.emplogo.model.enums.StatutPresence;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de InscriptionDAO, SANS MySQL (faux pilote JDBC : voir FakeJdbcDriver).
 *
 * Parties couvertes : findAll / search, findById (aucun résultat), delete, enregistrerPresence,
 * countPresences, annuler, deleteWithHistory (transaction), findEmployes, findAffectations
 * et la traduction des erreurs SQL.
 */
class InscriptionDAOTest {

    private static final String INSCRIPTION_INEXISTANTE = "Cette inscription n'existe plus dans la base.";
    private static final String PRESENCE_EXISTANTE =
            "Cette inscription a une présence enregistrée : annulez-la plutôt.";

    private static FakeJdbcDriver driver;

    private final InscriptionDAO dao = new InscriptionDAO();

    @BeforeAll
    static void enregistrerLeFauxPilote() throws SQLException {
        driver = new FakeJdbcDriver();
        DriverManager.registerDriver(driver);
    }

    @AfterAll
    static void retirerLeFauxPilote() throws SQLException {
        DriverManager.deregisterDriver(driver);
    }

    @BeforeEach
    void preparer() {
        driver.reset();
        System.setProperty("employgo.db.url", "jdbc:fake:test");
        System.setProperty("employgo.db.user", "testUser");
        System.setProperty("employgo.db.password", "testPwd");
    }

    @AfterEach
    void nettoyer() {
        System.clearProperty("employgo.db.url");
        System.clearProperty("employgo.db.user");
        System.clearProperty("employgo.db.password");
    }

    // ---------- Outils ----------

    /** La requête SQL envoyée au faux pilote (la première). */
    private String requetePreparee() {
        return driver.events.stream()
                .filter(e -> e.startsWith("prepare:"))
                .findFirst()
                .orElseThrow()
                .substring("prepare:".length());
    }

    private int nombreDeParametresTexte() {
        return (int) driver.events.stream().filter(e -> e.startsWith("setString:")).count();
    }

    private static int occurrences(String texte, String morceau) {
        int n = 0;
        int i = texte.indexOf(morceau);
        while (i >= 0) {
            n++;
            i = texte.indexOf(morceau, i + morceau.length());
        }
        return n;
    }

    // ---------- findAll() / search() ----------

    @Test
    void findAll_sansInscription_renvoieUneListeVideEtNeFiltrePas() throws SQLException {
        List<?> liste = dao.findAll();

        assertTrue(liste.isEmpty());
        assertFalse(requetePreparee().contains(" WHERE "));
        assertTrue(requetePreparee().endsWith(" ORDER BY i.date_inscription DESC, i.id_inscription DESC"));
        assertEquals(0, nombreDeParametresTexte());
    }

    @Test
    void search_texteNul_secomporteCommeUneRechercheVide() throws SQLException {
        dao.search(null);

        assertFalse(requetePreparee().contains(" WHERE "));
        assertEquals(0, nombreDeParametresTexte());
    }

    @Test
    void search_texteBlanc_secomporteCommeUneRechercheVide() throws SQLException {
        dao.search("    ");

        assertFalse(requetePreparee().contains(" WHERE "));
        assertEquals(0, nombreDeParametresTexte());
    }

    @Test
    void search_unMot_ajouteUneConditionEtSeptParametres() throws SQLException {
        dao.search("benali");

        String sql = requetePreparee();
        assertEquals(1, occurrences(sql, " WHERE ("));
        assertEquals(0, occurrences(sql, " AND ("));
        assertEquals(7, nombreDeParametresTexte());
    }

    @Test
    void search_deuxMots_lesRelieParAndEtDonneQuatorzeParametres() throws SQLException {
        dao.search("benali I12");

        String sql = requetePreparee();
        assertEquals(1, occurrences(sql, " WHERE ("));
        assertEquals(1, occurrences(sql, " AND ("));
        assertEquals(14, nombreDeParametresTexte());
        assertTrue(driver.events.stream().anyMatch(e -> e.startsWith("setString:14=")));
    }

    @Test
    void search_espacesMultiples_nEnCompteQueLesVraisMots() throws SQLException {
        dao.search("  ali    T5  ");

        assertEquals(1, occurrences(requetePreparee(), " AND ("));
        assertEquals(14, nombreDeParametresTexte());
    }

    @Test
    void search_fermeLesRessources() throws SQLException {
        dao.search("");

        assertEquals(
                List.of("connect", "prepare:" + requetePreparee(), "executeQuery",
                        "closeResultSet", "closeStatement", "close"),
                driver.events);
    }

    @Test
    void search_tableManquante_donneUnMessageQuiIndiqueDeReimporterLeScript() {
        driver.failOnExecute = new SQLException("Table 'inscription' doesn't exist", "42S02", 1146);

        SQLException e = assertThrows(DaoException.class, () -> dao.search("x"));

        assertTrue(e.getMessage().contains("dataBase.sql"));
    }

    // ---------- findById() ----------

    @Test
    void findById_aucuneLigne_renvoieNull() throws SQLException {
        driver.rows = 0;

        Object inscription = dao.findById(5);

        assertNull(inscription);
        assertTrue(requetePreparee().endsWith(" WHERE i.id_inscription = ?"));
        assertTrue(driver.events.contains("setInt:1=5"));
    }

    @Test
    void findById_erreurSql_estTraduiteEnDaoException() {
        driver.failOnExecute = new SQLException("Unknown column", "42S22", 1054);

        SQLException e = assertThrows(DaoException.class, () -> dao.findById(5));

        assertTrue(e.getMessage().contains("dataBase.sql"));
    }

    // ---------- delete() ----------

    @Test
    void delete_inscriptionExistante_executeLaSuppressionEtFermeLaConnexion() throws SQLException {
        driver.updateResult = 1;

        dao.delete(4);

        assertEquals(
                List.of("connect",
                        "prepare:DELETE FROM inscription WHERE id_inscription = ?",
                        "setInt:1=4", "executeUpdate", "closeStatement", "close"),
                driver.events);
    }

    @Test
    void delete_inscriptionInexistante_leveUneDaoExceptionClaire() {
        driver.updateResult = 0;

        SQLException e = assertThrows(DaoException.class, () -> dao.delete(99));

        assertEquals(INSCRIPTION_INEXISTANTE, e.getMessage());
    }

    @Test
    void delete_avecPresenceEnregistree_conseilleDAnnuler() {
        // Erreur 1451 : clé étrangère (la présence référence l'inscription)
        driver.failOnExecute = new SQLException("FK constraint", "23000", 1451);

        SQLException e = assertThrows(DaoException.class, () -> dao.delete(1));

        assertEquals(PRESENCE_EXISTANTE, e.getMessage());
    }

    @Test
    void delete_erreurDeDoublon_donneLeMessageDeDoublonDeInscription() {
        driver.failOnExecute = new SQLException("Duplicate entry", "23000", 1062);

        SQLException e = assertThrows(DaoException.class, () -> dao.delete(1));

        assertTrue(e.getMessage().startsWith("Cet employé a déjà une inscription pour ce trajet."));
    }

    // ---------- enregistrerPresence() ----------

    @Test
    void enregistrerPresence_statutNul_refuseSansOuvrirDeConnexion() {
        SQLException e = assertThrows(DaoException.class, () -> dao.enregistrerPresence(8, null));

        assertEquals("Le statut de présence ne peut pas être vide.", e.getMessage());
        assertTrue(driver.events.isEmpty());
    }

    @Test
    void enregistrerPresence_nonRenseigne_supprimeLaLigneDePresence() throws SQLException {
        dao.enregistrerPresence(8, StatutPresence.NON_RENSEIGNE);

        assertEquals(
                List.of("connect",
                        "prepare:DELETE FROM presence WHERE id_inscription = ?",
                        "setInt:1=8", "executeUpdate", "closeStatement", "close"),
                driver.events);
    }

    @Test
    void enregistrerPresence_present_inserePuisMetAJourSiDejaExistante() throws SQLException {
        dao.enregistrerPresence(8, StatutPresence.PRESENT);

        assertEquals(
                List.of("connect",
                        "prepare:INSERT INTO presence (id_inscription, statut) VALUES (?, ?) "
                                + "ON DUPLICATE KEY UPDATE statut = VALUES(statut)",
                        "setInt:1=8", "setString:2=PRESENT", "executeUpdate", "closeStatement", "close"),
                driver.events);
    }

    @Test
    void enregistrerPresence_absent_ecritLeNomDuStatut() throws SQLException {
        dao.enregistrerPresence(3, StatutPresence.ABSENT);

        assertTrue(driver.events.contains("setInt:1=3"));
        assertTrue(driver.events.contains("setString:2=ABSENT"));
    }

    @Test
    void enregistrerPresence_inscriptionInconnue_donneUnMessageClair() {
        // Erreur 1452 : l'inscription indiquée n'existe pas
        driver.failOnExecute = new SQLException("FK constraint fails", "23000", 1452);

        SQLException e = assertThrows(DaoException.class,
                () -> dao.enregistrerPresence(8, StatutPresence.PRESENT));

        assertTrue(e.getMessage().contains("n'existe pas dans la base"));
    }

    // ---------- countPresences() ----------

    @Test
    void countPresences_renvoieLeNombreDePresences() throws SQLException {
        driver.rows = 1;
        driver.intValue = 1;

        int nombre = dao.countPresences(3);

        assertEquals(1, nombre);
        assertEquals(
                List.of("connect",
                        "prepare:SELECT COUNT(*) FROM presence WHERE id_inscription = ?",
                        "setInt:1=3", "executeQuery", "closeResultSet", "closeStatement", "close"),
                driver.events);
    }

    @Test
    void countPresences_sansLigne_renvoieZero() throws SQLException {
        driver.rows = 0;
        driver.intValue = 99;

        assertEquals(0, dao.countPresences(3));
    }

    @Test
    void countPresences_erreurSql_estTraduiteEnDaoException() {
        driver.failOnExecute = new SQLException("Table 'presence' doesn't exist", "42S02", 1146);

        SQLException e = assertThrows(DaoException.class, () -> dao.countPresences(3));

        assertTrue(e.getMessage().contains("dataBase.sql"));
    }

    // ---------- annuler() ----------

    @Test
    void annuler_inscriptionExistante_passeLeStatutAAnnulee() throws SQLException {
        driver.updateResult = 1;

        dao.annuler(6);

        assertEquals(
                List.of("connect",
                        "prepare:UPDATE inscription SET statut = 'ANNULEE' WHERE id_inscription = ?",
                        "setInt:1=6", "executeUpdate", "closeStatement", "close"),
                driver.events);
    }

    @Test
    void annuler_inscriptionInexistante_leveUneDaoExceptionClaire() {
        driver.updateResult = 0;

        SQLException e = assertThrows(DaoException.class, () -> dao.annuler(6));

        assertEquals(INSCRIPTION_INEXISTANTE, e.getMessage());
    }

    // ---------- deleteWithHistory() ----------

    @Test
    void deleteWithHistory_succes_supprimePresenceEtInscriptionPuisValide() throws SQLException {
        driver.updateResult = 1;

        dao.deleteWithHistory(9);

        assertEquals(
                List.of("connect", "setAutoCommit:false",
                        "prepare:DELETE FROM presence WHERE id_inscription = ?",
                        "setInt:1=9", "executeUpdate", "closeStatement",
                        "prepare:DELETE FROM inscription WHERE id_inscription = ?",
                        "setInt:1=9", "executeUpdate", "closeStatement",
                        "commit", "close"),
                driver.events);
    }

    @Test
    void deleteWithHistory_inscriptionInexistante_annuleLaTransaction() {
        driver.updateResult = 0;

        SQLException e = assertThrows(DaoException.class, () -> dao.deleteWithHistory(9));

        assertEquals(INSCRIPTION_INEXISTANTE, e.getMessage());
        assertTrue(driver.events.contains("rollback"));
        assertFalse(driver.events.contains("commit"));
    }

    @Test
    void deleteWithHistory_erreurDeCleEtrangere_annuleEtDonneLeMessageEnUtilisation() {
        driver.failOnExecute = new SQLException("FK constraint", "23000", 1451);

        SQLException e = assertThrows(DaoException.class, () -> dao.deleteWithHistory(9));

        assertEquals(PRESENCE_EXISTANTE, e.getMessage());
        assertTrue(driver.events.contains("rollback"));
        assertFalse(driver.events.contains("commit"));
    }

    // ---------- findEmployes() ----------

    @Test
    void findEmployes_construitUneOptionParEmploye() throws SQLException {
        // Arrange : deux lignes, avec les noms de colonnes de la requête
        driver.rowData = List.of(
                Map.of("id_employe", 1, "matricule", "M01", "nom", "Ali", "prenom", "Amine"),
                Map.of("id_employe", 2, "matricule", "M02", "nom", "Benali", "prenom", "Sara"));

        // Act
        List<Option> options = dao.findEmployes(0);

        // Assert
        assertEquals(
                List.of(new Option(1, "Ali Amine (M01)"), new Option(2, "Benali Sara (M02)")),
                options);
        assertTrue(requetePreparee().contains("WHERE actif = 1 OR id_employe = ?"));
        assertTrue(driver.events.contains("setInt:1=0"));
    }

    @Test
    void findEmployes_inclutLEmployeIndiquePourUneModification() throws SQLException {
        dao.findEmployes(5);

        assertTrue(driver.events.contains("setInt:1=5"));
    }

    @Test
    void findEmployes_aucunEmploye_renvoieUneListeVide() throws SQLException {
        assertTrue(dao.findEmployes(0).isEmpty());
    }

    @Test
    void findEmployes_erreurSql_estTraduiteEnDaoException() {
        driver.failOnExecute = new SQLException("Unknown column", "42S22", 1054);

        SQLException e = assertThrows(DaoException.class, () -> dao.findEmployes(0));

        assertTrue(e.getMessage().contains("dataBase.sql"));
    }

    // ---------- findAffectations() ----------

    @Test
    void findAffectations_formateLeLibelleAvecDateEtHeure() throws SQLException {
        driver.rowData = List.of(Map.of(
                "id_affectation", 12,
                "nom", "Agadir-Inezgane",
                "date_trajet", java.sql.Date.valueOf("2026-05-12"),
                "heure_depart", java.sql.Time.valueOf("08:30:00")));

        List<Option> options = dao.findAffectations(0);

        assertEquals(List.of(new Option(12, "T12 • Agadir-Inezgane • 12/05/2026 08:30")), options);
        assertTrue(requetePreparee().contains("IN ('PLANIFIEE', 'EN_COURS')"));
        assertTrue(driver.events.contains("setInt:1=0"));
    }

    @Test
    void findAffectations_dateEtHeureAbsentes_neProvoquentPasDErreur() throws SQLException {
        driver.rowData = List.of(Map.of("id_affectation", 3, "nom", "Ligne"));

        List<Option> options = dao.findAffectations(0);

        assertEquals(1, options.size());
        assertEquals(3, options.get(0).id());
        assertEquals("T3 • Ligne •  ", options.get(0).label());
    }

    @Test
    void findAffectations_inclutLAffectationIndiqueePourUneModification() throws SQLException {
        dao.findAffectations(7);

        assertTrue(driver.events.contains("setInt:1=7"));
    }

    @Test
    void findAffectations_erreurSql_estTraduiteEnDaoException() {
        driver.failOnExecute = new SQLException("Table 'affectation' doesn't exist", "42S02", 1146);

        SQLException e = assertThrows(DaoException.class, () -> dao.findAffectations(0));

        assertTrue(e.getMessage().contains("dataBase.sql"));
    }
}