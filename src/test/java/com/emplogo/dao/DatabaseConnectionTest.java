package com.emplogo.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.emplogo.dao.DatabaseConnection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de DatabaseConnection, SANS MySQL.
 *
 * Principe : on enregistre un faux pilote JDBC ("jdbc:fake:...") dans DriverManager.
 * Ce faux pilote renvoie de fausses connexions qui notent tout ce qu'on leur demande
 * (commit, rollback, close, requêtes...). On peut ainsi vérifier le comportement
 * de la classe sans base de données.
 *
 * Les options JVM (-Demploygo.db.*) sont prioritaires sur db.properties :
 * les tests restent donc identiques même si un fichier db.properties existe.
 */
class DatabaseConnectionTest {

    private static FakeDriver driver;

    @BeforeAll
    static void enregistrerLeFauxPilote() throws SQLException {
        driver = new FakeDriver();
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

    // ---------- Constructeur ----------

    @Test
    void constructeur_estPrive_laClasseNePeutPasEtreInstanciee() throws Exception {
        Constructor<?> constructeur = DatabaseConnection.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructeur.getModifiers()));
    }

    // ---------- getConnection() ----------

    @Test
    void getConnection_utiliseLesOptionsJvm() throws SQLException {
        // Act
        Connection connexion = DatabaseConnection.getConnection();

        // Assert
        assertNotNull(connexion);
        assertEquals("jdbc:fake:test", driver.lastUrl);
        assertEquals("testUser", driver.lastUser);
        assertEquals("testPwd", driver.lastPassword);
    }

    @Test
    void getConnection_urlSansPilote_leveUneSqlException() {
        // Arrange
        System.setProperty("employgo.db.url", "jdbc:inconnu:test");

        // Act + Assert
        assertThrows(SQLException.class, DatabaseConnection::getConnection);
    }

    // ---------- inTransaction() ----------

    @Test
    void inTransaction_succes_renvoieLeResultatPuisValideEtFerme() throws SQLException {
        // Act
        String resultat = DatabaseConnection.inTransaction(c -> "ok");

        // Assert
        assertEquals("ok", resultat);
        assertEquals(
                List.of("connect", "setAutoCommit:false", "commit", "close"),
                driver.events);
    }

    @Test
    void inTransaction_sqlException_annuleRelanceEtFerme() {
        // Arrange
        SQLException erreur = new SQLException("boom");

        // Act
        SQLException levee = assertThrows(SQLException.class,
                () -> DatabaseConnection.inTransaction(c -> {
                    throw erreur;
                }));

        // Assert : même exception, rollback (et jamais commit), connexion fermée
        assertSame(erreur, levee);
        assertEquals(
                List.of("connect", "setAutoCommit:false", "rollback", "close"),
                driver.events);
    }

    @Test
    void inTransaction_runtimeException_annuleRelanceEtFerme() {
        // Arrange
        IllegalStateException erreur = new IllegalStateException("bug");

        // Act
        IllegalStateException levee = assertThrows(IllegalStateException.class,
                () -> DatabaseConnection.inTransaction(c -> {
                    throw erreur;
                }));

        // Assert
        assertSame(erreur, levee);
        assertEquals(
                List.of("connect", "setAutoCommit:false", "rollback", "close"),
                driver.events);
    }

    @Test
    void inTransaction_avecExec_valideApresLaRequete() throws SQLException {
        // Arrange
        driver.updateResult = 3;

        // Act
        int lignes = DatabaseConnection.inTransaction(
                c -> DatabaseConnection.exec(c, "DELETE FROM bus WHERE id_bus = ?", 5));

        // Assert : la requête est exécutée AVANT le commit
        assertEquals(3, lignes);
        assertEquals(
                List.of("connect", "setAutoCommit:false",
                        "prepare:DELETE FROM bus WHERE id_bus = ?",
                        "setInt:1=5", "executeUpdate", "closeStatement",
                        "commit", "close"),
                driver.events);
    }

    // ---------- count() ----------

    @Test
    void count_renvoieLaValeurDeLaPremiereColonne() throws SQLException {
        // Arrange
        driver.rowAvailable = true;
        driver.intValue = 4;
        Connection connexion = DatabaseConnection.getConnection();

        // Act
        int nombre = DatabaseConnection.count(connexion, "SELECT COUNT(*) FROM trajet WHERE id_bus = ?", 7);

        // Assert : paramètre transmis, ressources fermées
        assertEquals(4, nombre);
        assertEquals(
                List.of("connect",
                        "prepare:SELECT COUNT(*) FROM trajet WHERE id_bus = ?",
                        "setInt:1=7", "executeQuery", "next",
                        "closeResultSet", "closeStatement"),
                driver.events);
    }

    @Test
    void count_sansLigneDeResultat_renvoieZero() throws SQLException {
        // Arrange : pas de ligne, même si une valeur serait disponible
        driver.rowAvailable = false;
        driver.intValue = 99;
        Connection connexion = DatabaseConnection.getConnection();

        // Act
        int nombre = DatabaseConnection.count(connexion, "SELECT COUNT(*) FROM trajet WHERE id_bus = ?", 7);

        // Assert
        assertEquals(0, nombre);
    }

    // ---------- exec() ----------

    @Test
    void exec_renvoieLeNombreDeLignesModifiees() throws SQLException {
        // Arrange
        driver.updateResult = 2;
        Connection connexion = DatabaseConnection.getConnection();

        // Act
        int lignes = DatabaseConnection.exec(connexion, "DELETE FROM bus WHERE id_bus = ?", 9);

        // Assert
        assertEquals(2, lignes);
        assertEquals(
                List.of("connect",
                        "prepare:DELETE FROM bus WHERE id_bus = ?",
                        "setInt:1=9", "executeUpdate", "closeStatement"),
                driver.events);
    }

    // =====================================================================
    //  Faux pilote JDBC (outil de test, sans base de données)
    // =====================================================================

    /** Réaction d'un faux objet JDBC à l'appel d'une méthode. */
    @FunctionalInterface
    private interface Handler {
        Object handle(String methode, Object[] args) throws Throwable;
    }

    private static class FakeDriver implements Driver {

        List<String> events = new ArrayList<>();
        String lastUrl;
        String lastUser;
        String lastPassword;
        boolean rowAvailable = true;
        int intValue = 0;
        int updateResult = 0;

        void reset() {
            events = new ArrayList<>();
            lastUrl = null;
            lastUser = null;
            lastPassword = null;
            rowAvailable = true;
            intValue = 0;
            updateResult = 0;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:fake:");
        }

        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) {
                return null;
            }
            lastUrl = url;
            lastUser = info.getProperty("user");
            lastPassword = info.getProperty("password");
            events.add("connect");

            return proxy(Connection.class, (m, args) -> switch (m) {
                case "prepareStatement" -> {
                    events.add("prepare:" + args[0]);
                    yield fakeStatement();
                }
                case "setAutoCommit" -> {
                    events.add("setAutoCommit:" + args[0]);
                    yield null;
                }
                case "commit", "rollback", "close" -> {
                    events.add(m);
                    yield null;
                }
                default -> null;
            });
        }

        private PreparedStatement fakeStatement() {
            return proxy(PreparedStatement.class, (m, args) -> switch (m) {
                case "setInt" -> {
                    events.add("setInt:" + args[0] + "=" + args[1]);
                    yield null;
                }
                case "executeQuery" -> {
                    events.add("executeQuery");
                    yield fakeResultSet();
                }
                case "executeUpdate" -> {
                    events.add("executeUpdate");
                    yield updateResult;
                }
                case "close" -> {
                    events.add("closeStatement");
                    yield null;
                }
                default -> null;
            });
        }

        private ResultSet fakeResultSet() {
            return proxy(ResultSet.class, (m, args) -> switch (m) {
                case "next" -> {
                    events.add("next");
                    yield rowAvailable;
                }
                case "getInt" -> intValue;
                case "close" -> {
                    events.add("closeResultSet");
                    yield null;
                }
                default -> null;
            });
        }

        /** Fabrique un objet qui implémente l'interface JDBC demandée. */
        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, Handler handler) {
            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[] { type },
                    (p, method, args) -> {
                        Object resultat = handler.handle(method.getName(), args);
                        Class<?> retour = method.getReturnType();
                        if (resultat == null && retour.isPrimitive() && retour != void.class) {
                            // valeur par défaut (0 / false) pour les méthodes non simulées
                            return Array.get(Array.newInstance(retour, 1), 0);
                        }
                        return resultat;
                    });
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }
}