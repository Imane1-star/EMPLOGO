package com.transportpro.controller;

import com.transportpro.Ctx;
import com.transportpro.dao.BusDAO;
import com.transportpro.dao.ChauffeurDAO;
import com.transportpro.dao.EmployeDAO;
import com.transportpro.dao.TrajetDAO;
import com.transportpro.model.Bus;
import com.transportpro.model.Chauffeur;
import com.transportpro.model.Employe;
import com.transportpro.model.Trajet;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller applicatif principal.
 * Fait le lien entre les pages (View) et les DAO.
 * Toute exception SQL est transformée en toast lisible.
 */
public class MainController {

    private final Ctx ctx;
    private final EmployeDAO employeDAO = new EmployeDAO();
    private final BusDAO busDAO = new BusDAO();
    private final ChauffeurDAO chauffeurDAO = new ChauffeurDAO();
    private final TrajetDAO trajetDAO = new TrajetDAO();

    public MainController(Ctx ctx) {
        this.ctx = ctx;
    }

    // ---------------- Employés ----------------

    public List<Employe> chargerEmployes() {
        try {
            return employeDAO.findAll();
        } catch (SQLException e) {
            ctx.toast("Erreur employés : " + e.getMessage());
            return List.of();
        }
    }

    public void ajouterEmploye(Employe e) {
        try {
            employeDAO.insert(e);
            ctx.toast("Employé ajouté : " + e.getNomComplet());
        } catch (SQLException ex) {
            ctx.toast("Échec ajout : " + ex.getMessage());
        }
    }

    public void supprimerEmploye(int id) {
        try {
            employeDAO.delete(id);
            ctx.toast("Employé supprimé");
        } catch (SQLException ex) {
            ctx.toast("Échec suppression : " + ex.getMessage());
        }
    }

    // ---------------- Bus ----------------

    public List<Bus> chargerBus() {
        try {
            return busDAO.findAll();
        } catch (SQLException e) {
            ctx.toast("Erreur bus : " + e.getMessage());
            return List.of();
        }
    }

    // ---------------- Chauffeurs ----------------

    public List<Chauffeur> chargerChauffeurs() {
        try {
            return chauffeurDAO.findAll();
        } catch (SQLException e) {
            ctx.toast("Erreur chauffeurs : " + e.getMessage());
            return List.of();
        }
    }

    // ---------------- Trajets ----------------

    public List<Trajet> chargerTrajets() {
        try {
            return trajetDAO.findAll();
        } catch (SQLException e) {
            ctx.toast("Erreur trajets : " + e.getMessage());
            return List.of();
        }
    }
}