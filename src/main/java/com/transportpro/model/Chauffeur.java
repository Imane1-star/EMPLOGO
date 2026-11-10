package com.transportpro.model;

import com.transportpro.model.enums.TypeIncident;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Chauffeur {

    private int idChauffeur;
    private String nom;
    private String telephone;
    private String numeroPermis;
    private boolean disponible;
    private List<Trajet> trajets = new ArrayList<>();

    public Chauffeur() {
    }

    public Chauffeur(int idChauffeur, String nom, String telephone,
                     String numeroPermis, boolean disponible) {
        this.idChauffeur = idChauffeur;
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
    public int getIdChauffeur() { return idChauffeur; }
    public void setIdChauffeur(int idChauffeur) { this.idChauffeur = idChauffeur; }

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
