package com.transportpro.dao;

import com.transportpro.model.Trajet;
import com.transportpro.model.enums.StatutTrajet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrajetDAO {

    public List<Trajet> findAll() throws SQLException {
        List<Trajet> list = new ArrayList<>();
        String sql = """
            SELECT id_trajet, date, heure_depart, destination, statut
            FROM trajet ORDER BY date, heure_depart
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date d = rs.getDate("date");
                java.sql.Time h = rs.getTime("heure_depart");
                list.add(new Trajet(
                        rs.getInt("id_trajet"),
                        d != null ? d.toLocalDate() : null,
                        h != null ? h.toLocalTime() : null,
                        rs.getString("destination"),
                        parseStatut(rs.getString("statut"))
                ));
            }
        }
        return list;
    }

    private StatutTrajet parseStatut(String s) {
        if (s == null) return StatutTrajet.PLANIFIE;
        try {
            return StatutTrajet.valueOf(s.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return StatutTrajet.PLANIFIE;
        }
    }
}