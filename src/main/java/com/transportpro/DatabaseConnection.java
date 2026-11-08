
package com.transportpro;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mariadb://localhost:3306/emplogo";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection connection = getConnection()) {
            System.out.println("Connexion MariaDB reussie !");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }
}


