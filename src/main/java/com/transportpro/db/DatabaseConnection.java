package com.transportpro.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC à la base MariaDB/MySQL « emplogo ».
 * Valeurs par défaut : localhost:3306, utilisateur root sans mot de passe (XAMPP).
 * Elles peuvent être remplacées au lancement avec -Demplogo.db.url=..., -Demplogo.db.user=...,
 * -Demplogo.db.password=... (utile pour les tests sur une copie de la base).
 */
public final class DatabaseConnection {

    private DatabaseConnection() {}

    private static final String URL = System.getProperty("emplogo.db.url",
            "jdbc:mysql://localhost:3306/emplogo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
    private static final String USER = System.getProperty("emplogo.db.user", "root");
    private static final String PASSWORD = System.getProperty("emplogo.db.password", "");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
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

    /** Exécute une requête « SELECT COUNT(*) ... WHERE ... = ? » et retourne le nombre. */
    public static int count(Connection c, String sql, int id) throws SQLException {
        try (java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Exécute une requête de modification avec un seul paramètre entier ; retourne le nombre de lignes. */
    public static int exec(Connection c, String sql, int id) throws SQLException {
        try (java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    public static void main(String[] args) {
        try (Connection connection = getConnection()) {
            System.out.println("Connexion à la base réussie : " + connection.getMetaData().getURL());
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
