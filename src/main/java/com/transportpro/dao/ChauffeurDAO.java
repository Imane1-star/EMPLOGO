package com.transportpro.dao;

import com.transportpro.model.Chauffeur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChauffeurDAO {

    public List<Chauffeur> findAll() throws SQLException {
        List<Chauffeur> list = new ArrayList<>();
        String sql = """
            SELECT id_chauffeur, nom, telephone, numero_permis, disponible
            FROM chauffeur ORDER BY id_chauffeur
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Chauffeur(
                        rs.getInt("id_chauffeur"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("numero_permis"),
                        rs.getBoolean("disponible")
                ));
            }
        }
        return list;
    }
}
