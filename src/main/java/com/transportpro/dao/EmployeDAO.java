package com.transportpro.dao;

import com.transportpro.model.Employe;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeDAO {

    public List<Employe> findAll() throws SQLException {

        List<Employe> employees = new ArrayList<>();

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
                LocalDate dateEmb = rs.getDate("date_embauche") != null
                        ? rs.getDate("date_embauche").toLocalDate()
                        : null;
                employees.add(new Employe(
                        rs.getInt("id_employe"),
                        rs.getString("matricule"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("telephone"),
                        rs.getString("adresse"),
                        rs.getString("departement"),
                        dateEmb,
                        rs.getBoolean("actif")
                ));
            }
        }

        return employees;
    }
    public Employe findById(int id) throws SQLException {
        String sql = "SELECT * FROM employe WHERE id_employe = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDate dateEmb = rs.getDate("date_embauche") != null
                            ? rs.getDate("date_embauche").toLocalDate()
                            : null;
                    return new Employe(
                            rs.getInt("id_employe"),
                            rs.getString("matricule"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("telephone"),
                            rs.getString("adresse"),
                            rs.getString("departement"),
                            dateEmb,
                            rs.getBoolean("actif")
                    );
                }
            }
        }
        return null;
    }
    public void insert(Employe e) throws SQLException {
        String sql = """
            INSERT INTO employe (matricule, nom, prenom, telephone,
                                 adresse, departement, date_embauche, actif)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getMatricule());
            ps.setString(2, e.getNom());
            ps.setString(3, e.getPrenom());
            ps.setString(4, e.getTelephone());
            ps.setString(5, e.getAdresse());
            ps.setString(6, e.getDepartement());
            ps.setDate(7, e.getDateEmbauche() != null
                    ? Date.valueOf(e.getDateEmbauche()) : null);
            ps.setBoolean(8, e.isActif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setIdEmploye(keys.getInt(1));
            }
        }
    }
    public void update(Employe e) throws SQLException {
        String sql = """
            UPDATE employe SET matricule=?, nom=?, prenom=?, telephone=?,
                   adresse=?, departement=?, date_embauche=?, actif=?
            WHERE id_employe=?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getMatricule());
            ps.setString(2, e.getNom());
            ps.setString(3, e.getPrenom());
            ps.setString(4, e.getTelephone());
            ps.setString(5, e.getAdresse());
            ps.setString(6, e.getDepartement());
            ps.setDate(7, e.getDateEmbauche() != null
                    ? Date.valueOf(e.getDateEmbauche()) : null);
            ps.setBoolean(8, e.isActif());
            ps.setInt(9, e.getIdEmploye());
            ps.executeUpdate();
        }
    }
    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM employe WHERE id_employe=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
