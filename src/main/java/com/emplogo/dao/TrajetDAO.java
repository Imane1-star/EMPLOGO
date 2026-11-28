package com.emplogo.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.emplogo.model.Bus;
import com.emplogo.model.Chauffeur;
import com.emplogo.model.Trajet;
import com.emplogo.model.enums.StatutTrajet;

/**
 * Lecture des trajets datés (objets métier {@link Trajet}).
 * Dans la base, la date, l'heure, le bus et le chauffeur d'un trajet sont dans la table « affectation » ;
 * la table « trajet » ne décrit que la ligne (nom, départ, arrivée). Un Trajet est donc une affectation
 * jointe à sa ligne : idTrajet = id_affectation, destination = lieu_arrivee de la ligne.
 * Pour créer / modifier / annuler un trajet planifié, voir {@link AffectationDAO}.
 */
public class TrajetDAO {

    private static final String SELECT = """
            SELECT a.id_affectation, a.date_trajet, a.heure_depart, a.heure_arrivee, a.statut,
                   t.nom AS ligne, t.lieu_arrivee,
                   b.id_bus, b.immatriculation, b.capacite,
                   c.id_chauffeur, c.numero_permis, e.nom, e.prenom, e.telephone
            FROM affectation a
            JOIN trajet t ON t.id_trajet = a.id_trajet
            JOIN bus b ON b.id_bus = a.id_bus
            JOIN chauffeur c ON c.id_chauffeur = a.id_chauffeur
            JOIN employe e ON e.id_employe = c.id_employe
            """;

    public List<Trajet> findAll() throws SQLException {
        List<Trajet> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY a.date_trajet, a.heure_depart");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger les trajets.");
        }
        return list;
    }

    /** Trajets non annulés d'un chauffeur (planning, BF « consulter son planning »). */
    public List<Trajet> findByChauffeur(int idChauffeur) throws SQLException {
        List<Trajet> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT
                     + " WHERE a.id_chauffeur = ? AND a.statut <> 'ANNULEE' ORDER BY a.date_trajet, a.heure_depart")) {
            ps.setInt(1, idChauffeur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw DaoException.translate(e, null, null, "Impossible de charger le planning.");
        }
        return list;
    }

    private static Trajet map(ResultSet rs) throws SQLException {
        Date d = rs.getDate("date_trajet");
        Time h = rs.getTime("heure_depart");
        Time ha = rs.getTime("heure_arrivee");
        StatutTrajet statut = StatutTrajet.fromCode(rs.getString("statut"));
        Trajet t = new Trajet(
                rs.getInt("id_affectation"),
                d != null ? d.toLocalDate() : null,
                h != null ? h.toLocalTime() : null,
                rs.getString("lieu_arrivee"),
                statut != null ? statut : StatutTrajet.PLANIFIE);
        t.setNomLigne(rs.getString("ligne"));
        t.setHeureArrivee(ha != null ? ha.toLocalTime() : null);
        Bus bus = new Bus();
        bus.setIdBus(rs.getInt("id_bus"));
        bus.setImmatriculation(rs.getString("immatriculation"));
        bus.setCapacite(rs.getInt("capacite"));
        t.setBus(bus);
        Chauffeur ch = new Chauffeur(rs.getInt("id_chauffeur"), rs.getString("nom"),
                rs.getString("telephone"), rs.getString("numero_permis"), true);
        ch.setPrenom(rs.getString("prenom"));
        t.setChauffeur(ch);
        return t;
    }
}
