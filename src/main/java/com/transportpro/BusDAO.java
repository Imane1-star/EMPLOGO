package com.transportpro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Accès à la table « bus » (réutilise DatabaseConnection). */
public class BusDAO {

    /** Erreur avec un message lisible par l'utilisateur. */
    public static class BusException extends Exception {
        public BusException(String message, Throwable cause) { super(message, cause); }
    }

    private static final String COLS = "id_bus, immatriculation, marque, modele, capacite, statut";
    private static final String SELECT_ALL = "SELECT " + COLS + " FROM bus ORDER BY id_bus";
    private static final String SELECT_ONE = "SELECT " + COLS + " FROM bus WHERE id_bus = ?";
    private static final String INSERT =
            "INSERT INTO bus (immatriculation, marque, modele, capacite, statut) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE bus SET immatriculation = ?, marque = ?, modele = ?, capacite = ?, statut = ? WHERE id_bus = ?";
    private static final String DELETE = "DELETE FROM bus WHERE id_bus = ?";

    public List<Bus> findAll() throws BusException {
        List<Bus> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw translate(e, "Impossible de charger la liste des bus.");
        }
        return list;
    }

    public Bus findById(int idBus) throws BusException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ONE)) {
            ps.setInt(1, idBus);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible de lire le bus.");
        }
        throw new BusException("Ce bus n'existe plus dans la base.", null);
    }

    public void insert(Bus b) throws BusException {
        check(b);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, b);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setIdBus(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw translate(e, "Impossible d'ajouter le bus.");
        }
    }

    public void update(Bus b) throws BusException {
        check(b);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, b);
            ps.setInt(6, b.getIdBus());
            if (ps.executeUpdate() == 0)
                throw new BusException("Ce bus n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de modifier le bus.");
        }
    }

    public void delete(int idBus) throws BusException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, idBus);
            if (ps.executeUpdate() == 0)
                throw new BusException("Ce bus n'existe plus dans la base.", null);
        } catch (SQLException e) {
            throw translate(e, "Impossible de supprimer le bus.");
        }
    }

    // ------------------------------------------------------------------ outils internes
    private Bus map(ResultSet rs) throws SQLException {
        return new Bus(
                rs.getInt("id_bus"),
                rs.getString("immatriculation"),
                rs.getString("marque"),
                rs.getString("modele"),
                rs.getInt("capacite"),
                rs.getString("statut"));
    }

    private void check(Bus b) throws BusException {
        String err = b.validate();
        if (err != null) throw new BusException(err, null);
    }

    private void bind(PreparedStatement ps, Bus b) throws SQLException {
        ps.setString(1, b.getImmatriculation().trim());
        setNullable(ps, 2, b.getMarque());
        setNullable(ps, 3, b.getModele());
        ps.setInt(4, b.getCapacite());
        ps.setString(5, b.getStatut());
    }

    private void setNullable(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v.trim());
    }

    private BusException translate(SQLException e, String defaut) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new BusException(
                    "Connexion à la base impossible. Vérifiez que MariaDB/MySQL est démarré.", e);
        switch (e.getErrorCode()) {
            case 1062:
                return new BusException("Cette immatriculation existe déjà.", e);
            case 1451:
                return new BusException(
                        "Ce bus est utilisé par d'autres données (maintenance, affectation...). "
                                + "Suppression impossible.", e);
            case 1406:
                return new BusException("Une valeur est trop longue pour la base de données.", e);
            default:
                return new BusException(defaut + " (" + e.getMessage() + ")", e);
        }
    }
}