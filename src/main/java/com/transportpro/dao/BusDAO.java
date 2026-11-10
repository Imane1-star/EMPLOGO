package com.transportpro.dao;

import com.transportpro.model.Bus;
import com.transportpro.model.enums.StatutBus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BusDAO {

    public List<Bus> findAll() throws SQLException {
        List<Bus> list = new ArrayList<>();
        String sql = "SELECT id_bus, immatriculation, capacite, statut FROM bus ORDER BY id_bus";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Bus(
                        rs.getInt("id_bus"),
                        rs.getString("immatriculation"),
                        rs.getInt("capacite"),
                        parseStatut(rs.getString("statut"))
                ));
            }
        }
        return list;
    }

    private StatutBus parseStatut(String s) {
        if (s == null) return StatutBus.DISPONIBLE;
        try {
            return StatutBus.valueOf(s.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return StatutBus.DISPONIBLE;
        }
    }
}
