package com.emplogo.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connexion JDBC à la base « employgo » (script dao/dataBase.sql).
 * Priorité des identifiants :
 * 1) Options JVM (-Demploygo.db.url=..., -Demploygo.db.user=..., -Demploygo.db.password=...)
 * 2) Fichier local « db.properties » à la racine du projet (ignoré par Git)
 * 3) Valeurs par défaut : localhost:3306, utilisateur root sans mot de passe (XAMPP)
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/employgo"
          + "?useSSL=false"
          + "&allowPublicKeyRetrieval=true"
          + "&serverTimezone=UTC"
          + "&characterEncoding=UTF-8";

    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private DatabaseConnection() {}

    private static Properties loadLocalProperties() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
        } catch (IOException ignored) {
            // Si db.properties n'existe pas (ex. machine de votre camarade), on utilise les valeurs par défaut
        }
        return props;
    }

    public static Connection getConnection() throws SQLException {
        Properties props = loadLocalProperties();

        String url = System.getProperty("employgo.db.url",
                props.getProperty("db.url", DEFAULT_URL));
        String user = System.getProperty("employgo.db.user",
                props.getProperty("db.user", DEFAULT_USER));
        String password = System.getProperty("employgo.db.password",
                props.getProperty("db.password", DEFAULT_PASSWORD));

        return DriverManager.getConnection(url, user, password);
    }

    /** Travail exécuté dans une transaction (voir {@link #inTransaction}). */
    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /**
     * Exécute plusieurs requêtes dans une seule transaction : tout est validé, ou tout est annulé
     * en cas d'erreur (utilisé par les suppressions qui touchent plusieurs tables).
     */
    public static <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Exécute « SELECT COUNT(*) ... WHERE ... = ? » et retourne le nombre. */
    public static int count(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Exécute une requête de modification avec un seul paramètre entier ; retourne le nombre de lignes. */
    public static int exec(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }
}