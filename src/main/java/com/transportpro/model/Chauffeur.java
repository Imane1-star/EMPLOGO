package com.transportpro.model;

import com.transportpro.model.enums.TypeIncident;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.transportpro.model.enums.Role;

public class Chauffeur extends Utilisateur{
    private String nom;
    private String telephone;
    private String numeroPermis;
    private boolean disponible;
    private List<Trajet> trajets = new ArrayList<>();

    public Chauffeur() {
        super();
        setRole(Role.CHAUFFEUR);
    }

    public Chauffeur(int idUtilisateur,String login , String motDePasseHache, boolean actif, Role role, String nom, String telephone,
                     String numeroPermis, boolean disponible) {
        super(idUtilisateur, login, motDePasseHache, actif, role);
        this.nom = nom;
        this.telephone = telephone;
        this.numeroPermis = numeroPermis;
        this.disponible = disponible;
    }

    public List<Trajet> consulterPlanning() {
        return new ArrayList<>(trajets);
    }

    public Incident signalerIncident(TypeIncident type, String description) {
        Incident i = new Incident();
        i.setType(type);
        i.setDescription(description);
        i.setDateHeure(LocalDateTime.now());
        i.creer();
        return i;
    }

    // ---- Getters / Setters ----

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getNumeroPermis() { return numeroPermis; }
    public void setNumeroPermis(String numeroPermis) { this.numeroPermis = numeroPermis; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public List<Trajet> getTrajets() { return trajets; }
    public void setTrajets(List<Trajet> trajets) { this.trajets = trajets; }
}
