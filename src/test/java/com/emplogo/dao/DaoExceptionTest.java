package com.emplogo.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.emplogo.dao.DaoException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de DaoException.
 * Aucune base de données n'est nécessaire : on fabrique de fausses SQLException
 * (message, SQLState, code d'erreur MySQL) et on vérifie le message traduit.
 */
class DaoExceptionTest {

    // ---------- Constructeurs ----------

    @Test
    void constructeurAvecMessage_conserveLeMessage() {
        // Arrange + Act
        DaoException e = new DaoException("Erreur test");

        // Assert
        assertEquals("Erreur test", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void constructeurAvecCause_copieSqlStateCodeEtCause() {
        // Arrange
        SQLException cause = new SQLException("boom", "23000", 1062);

        // Act
        DaoException e = new DaoException("Doublon", cause);

        // Assert
        assertEquals("Doublon", e.getMessage());
        assertEquals("23000", e.getSQLState());
        assertEquals(1062, e.getErrorCode());
        assertSame(cause, e.getCause());
    }

    @Test
    void constructeurAvecCauseNulle_neLevePasDeNullPointerException() {
        // Act
        DaoException e = new DaoException("Sans cause", null);

        // Assert
        assertEquals("Sans cause", e.getMessage());
        assertNull(e.getSQLState());
        assertEquals(0, e.getErrorCode());
        assertNull(e.getCause());
    }

    @Test
    void daoException_estUneSqlException() {
        // Les DAO déclarent "throws SQLException" : DaoException doit en hériter.
        assertTrue(new DaoException("x") instanceof SQLException);
    }

    // ---------- translate() : cas particuliers ----------

    @Test
    void translate_renvoieLaMemeInstanceSiDejaUneDaoException() {
        // Arrange
        DaoException deja = new DaoException("Déjà traduite");

        // Act
        DaoException resultat = DaoException.translate(deja, "a", "b", "c");

        // Assert
        assertSame(deja, resultat);
    }

    @Test
    void translate_sqlStateCommencantPar08_donneUnMessageDeConnexion() {
        // Arrange
        SQLException e = new SQLException("Communications link failure", "08S01", 0);

        // Act
        DaoException resultat = DaoException.translate(e, null, null, "défaut");

        // Assert
        assertTrue(resultat.getMessage().startsWith("Connexion à la base impossible"));
        assertSame(e, resultat.getCause());
    }

    @Test
    void translate_sqlState45000_utiliseLeMessageDuDeclencheurNettoye() {
        // Arrange : message d'un trigger, avec préfixe de connexion et texte de requête
        SQLException e = new SQLException(
                "(conn=12) Capacité du bus dépassée\nQuery: INSERT INTO ...", "45000", 1644);

        // Act
        DaoException resultat = DaoException.translate(e, null, null, "défaut");

        // Assert
        assertEquals("Capacité du bus dépassée", resultat.getMessage());
    }

    // ---------- translate() : codes d'erreur MySQL ----------

    @Test
    void translate_1062_utiliseLeMessageDoublonFourni() {
        SQLException e = new SQLException("Duplicate entry", "23000", 1062);

        DaoException resultat = DaoException.translate(e, "Ce matricule existe déjà.", null, "défaut");

        assertEquals("Ce matricule existe déjà.", resultat.getMessage());
    }

    @Test
    void translate_1062_sansMessageFourni_utiliseLeMessageParDefaut() {
        SQLException e = new SQLException("Duplicate entry", "23000", 1062);

        DaoException resultat = DaoException.translate(e, null, null, "défaut");

        assertEquals("Cette valeur existe déjà.", resultat.getMessage());
    }

    @Test
    void translate_1451_utiliseLeMessageEnUtilisationFourni() {
        SQLException e = new SQLException("FK constraint", "23000", 1451);

        DaoException resultat = DaoException.translate(e, null, "Ce bus a des trajets.", "défaut");

        assertEquals("Ce bus a des trajets.", resultat.getMessage());
    }

    @Test
    void translate_1451_sansMessageFourni_utiliseLeMessageParDefaut() {
        SQLException e = new SQLException("FK constraint", "23000", 1451);

        DaoException resultat = DaoException.translate(e, null, null, "défaut");

        assertEquals(
                "Cette ligne est utilisée par d'autres données : suppression impossible.",
                resultat.getMessage());
    }

    @ParameterizedTest(name = "code {0} -> message contenant \"{1}\"")
    @CsvSource({
            "1452, n'existe pas dans la base",
            "1406, trop longue",
            "1265, n'est pas acceptée",
            "1366, n'est pas acceptée",
            "1292, date ou l'heure saisie"
    })
    void translate_codesConnus_donnentUnMessageClair(int code, String extrait) {
        SQLException e = new SQLException("détail technique", "HY000", code);

        DaoException resultat = DaoException.translate(e, null, null, "défaut");

        assertTrue(resultat.getMessage().contains(extrait),
                "Message obtenu : " + resultat.getMessage());
    }

    @ParameterizedTest(name = "code {0} -> base pas à jour")
    @CsvSource({"1054", "1146"})
    void translate_colonneOuTableManquante_indiqueDeReimporterLeScriptSql(int code) {
        SQLException e = new SQLException("Unknown column 'x'", "42S22", code);

        DaoException resultat = DaoException.translate(e, null, null, "défaut");

        assertTrue(resultat.getMessage().contains("dataBase.sql"));
        assertTrue(resultat.getMessage().contains("Unknown column 'x'"));
    }

    @Test
    void translate_codeInconnu_utiliseLeDebutDeMessageEtLeDetail() {
        SQLException e = new SQLException("oups", "HY000", 9999);

        DaoException resultat = DaoException.translate(e, null, null, "Échec de l'insertion");

        assertEquals("Échec de l'insertion (oups)", resultat.getMessage());
    }

    @Test
    void translate_sansSqlStateNiCode_utiliseLeCasParDefaut() {
        SQLException e = new SQLException("oups");

        DaoException resultat = DaoException.translate(e, null, null, "Échec");

        assertEquals("Échec (oups)", resultat.getMessage());
    }

    // ---------- cleanTriggerMessage() ----------

    @Test
    void cleanTriggerMessage_enleveLePrefixeDeConnexion() {
        assertEquals("Message simple",
                DaoException.cleanTriggerMessage("(conn=7) Message simple"));
    }

    @Test
    void cleanTriggerMessage_gardeSeulementLaPremiereLigne() {
        assertEquals("Première ligne",
                DaoException.cleanTriggerMessage("Première ligne\nQuery: SELECT 1"));
    }

    @Test
    void cleanTriggerMessage_laisseUnMessageDejaPropre() {
        assertEquals("Message simple", DaoException.cleanTriggerMessage("Message simple"));
    }

    @Test
    void cleanTriggerMessage_nul_donneUnMessageParDefaut() {
        assertEquals("Opération refusée par la base de données.",
                DaoException.cleanTriggerMessage(null));
    }

    @Test
    void cleanTriggerMessage_videApresNettoyage_donneUnMessageParDefaut() {
        assertEquals("Opération refusée par la base de données.",
                DaoException.cleanTriggerMessage("(conn=7)   "));
    }
}