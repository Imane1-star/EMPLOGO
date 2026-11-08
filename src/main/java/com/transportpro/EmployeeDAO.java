
package com.transportpro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public List<String[]> getAllEmployees() throws SQLException {

        List<String[]> employees = new ArrayList<>();

        String sql = """
            SELECT id_employe, matricule, nom, prenom,
                   email, telephone, adresse, departement,
                   date_embauche, actif
            FROM employe
            ORDER BY id_employe
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                employees.add(new String[]{
                    String.valueOf(rs.getInt("id_employe")),
                    rs.getString("matricule"),
                    rs.getString("nom") + " " + rs.getString("prenom"),
                    rs.getString("telephone"),
                    rs.getString("adresse"),
                    rs.getBoolean("actif") ? "Actif" : "Inactif"
                });
            }
        }

        return employees;
    }
}
