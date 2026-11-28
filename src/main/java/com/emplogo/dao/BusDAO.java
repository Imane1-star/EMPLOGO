package com.emplogo.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.emplogo.model.Bus;
import com.emplogo.model.enums.StatutBus;

/** Accès à la table « bus » (BF5, BF6). */
public class BusDAO {

    private static final String COLS = "id_bus, immatriculation, marque, modele, capacite, statut";
    private static final String DUPLICATE = "Cette immatriculation existe déjà.";
    private static final String IN_USE = "Ce bus est utilisé par d'autres données (maintenance, affectation...).";

    public List<Bus> findAll() throws SQLException {
        List<Bus> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM bus ORDER BY id_bus";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger la liste des bus.");
        }
        return list;
    }

    /** Retourne le bus, ou null s'il n'existe pas. */
    public Bus findById(int idBus) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM bus WHERE id_bus = ?")) {
            ps.setInt(1, idBus);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de lire le bus.");
        }
    }

    public void insert(Bus b) throws SQLException {
        check(b);
        String sql = "INSERT INTO bus (immatriculation, marque, modele, capacite, statut) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, b);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setIdBus(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, DUPLICATE, IN_USE, "Impossible d'ajouter le bus.");
        }
    }

    public void update(Bus b) throws SQLException {
        check(b);
        String sql = "UPDATE bus SET immatriculation = ?, marque = ?, modele = ?, capacite = ?, statut = ? WHERE id_bus = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, b);
            ps.setInt(6, b.getIdBus());
            if (ps.executeUpdate() == 0) throw new DaoException("Ce bus n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, DUPLICATE, IN_USE, "Impossible de modifier le bus.");
        }
    }

    public void delete(int idBus) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "DELETE FROM bus WHERE id_bus = ?", idBus) == 0)
                throw new DaoException("Ce bus n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, DUPLICATE, IN_USE, "Impossible de supprimer le bus.");
        }
    }

    // ------------------------------------------------------------------ mise hors service / suppression complète

    /** Données liées à un bus. */
    public record Usage(int affectations, int maintenances) {
        public boolean any() { return affectations + maintenances > 0; }
        /** Un bus qui a déjà roulé garde son historique de trajets : pas de suppression. */
        public boolean canDeleteWithHistory() { return affectations == 0; }
    }

    public Usage usage(int idBus) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return new Usage(
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM affectation WHERE id_bus = ?", idBus),
                    DatabaseConnection.count(c, "SELECT COUNT(*) FROM maintenance WHERE id_bus = ?", idBus));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de vérifier les données liées au bus.");
        }
    }

    /** RG6 : un bus hors service ne peut plus être affecté ; son historique est conservé. */
    public void mettreHorsService(int idBus) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (DatabaseConnection.exec(c, "UPDATE bus SET statut = 'HORS_SERVICE' WHERE id_bus = ?", idBus) == 0)
                throw new DaoException("Ce bus n'existe plus dans la base.");
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de mettre le bus hors service.");
        }
    }

    /** Supprime le bus et son historique de maintenance (refusé s'il a des trajets). */
    public void deleteWithHistory(int idBus) throws SQLException {
        try {
            DatabaseConnection.inTransaction(c -> {
                if (DatabaseConnection.count(c, "SELECT COUNT(*) FROM affectation WHERE id_bus = ?", idBus) > 0)
                    throw new DaoException("Ce bus a déjà des trajets : il ne peut pas être supprimé. "
                            + "Mettez-le hors service à la place.");
                DatabaseConnection.exec(c, "DELETE FROM maintenance WHERE id_bus = ?", idBus);
                if (DatabaseConnection.exec(c, "DELETE FROM bus WHERE id_bus = ?", idBus) == 0)
                    throw new DaoException("Ce bus n'existe plus dans la base.");
                return null;
            });
        } catch (SQLException e) {
            throw DaoException.translate(e, null, IN_USE, "Impossible de supprimer le bus.");
        }
    }

    // ------------------------------------------------------------------ outils internes
    private static Bus map(ResultSet rs) throws SQLException {
        return new Bus(
                rs.getInt("id_bus"),
                rs.getString("immatriculation"),
                rs.getString("marque"),
                rs.getString("modele"),
                rs.getInt("capacite"),
                parseStatut(rs.getString("statut")));
    }

    private static StatutBus parseStatut(String s) {
        if (s == null) return StatutBus.DISPONIBLE;
        try {
            return StatutBus.valueOf(s.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return StatutBus.DISPONIBLE;
        }
    }

    private static void check(Bus b) throws DaoException {
        String err = b.validate();
        if (err != null) throw new DaoException(err);
    }

    private static void bind(PreparedStatement ps, Bus b) throws SQLException {
        ps.setString(1, b.getImmatriculation().trim());
        ps.setString(2, b.getMarque() == null || b.getMarque().isBlank() ? null : b.getMarque().trim());
        ps.setString(3, b.getModele() == null || b.getModele().isBlank() ? null : b.getModele().trim());
        ps.setInt(4, b.getCapacite());
        ps.setString(5, b.getStatut().name());
    }
}
