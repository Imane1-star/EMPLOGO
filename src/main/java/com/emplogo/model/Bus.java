package com.emplogo.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.emplogo.model.enums.StatutBus;

public class Bus {

    private int idBus;
    private String immatriculation;
    private String marque;   // colonnes facultatives de la table bus
    private String modele;
    private int capacite;
    private StatutBus statut;

    public Bus() {
    }

    public Bus(int idBus, String immatriculation, String marque, String modele, int capacite, StatutBus statut) {
        this(idBus, immatriculation, capacite, statut);
        this.marque = marque;
        this.modele = modele;
    }

    /** Retourne un message d'erreur, ou null si le bus est valide (appelé avant INSERT / UPDATE). */
    public String validate() {
        if (immatriculation == null || immatriculation.isBlank()) return "L'immatriculation est obligatoire.";
        if (immatriculation.trim().length() > 20) return "L'immatriculation ne doit pas dépasser 20 caractères.";
        if (marque != null && marque.trim().length() > 50) return "La marque ne doit pas dépasser 50 caractères.";
        if (modele != null && modele.trim().length() > 50) return "Le modèle ne doit pas dépasser 50 caractères.";
        if (capacite <= 0 || capacite > 65535) return "La capacité doit être un entier entre 1 et 65535.";
        if (statut == null) return "Le statut est obligatoire.";
        return null;
    }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }

    public Bus(int idBus, String immatriculation, int capacite, StatutBus statut) {
        this.idBus = idBus;
        this.immatriculation = immatriculation;
        this.capacite = capacite;
        this.statut = statut;
    }

    /**
     * Un bus est disponible s'il est DISPONIBLE et n'a pas de trajet chevauchant.
     * (Vérification fine déléguée au service ; ici on expose le statut.)
     */
    public boolean estDisponible(LocalDate date, LocalTime heure) {
        return statut == StatutBus.DISPONIBLE;
    }

    public boolean peutAccueillir(int nbInscrits) {
        return nbInscrits < capacite;
    }

    // ---- Getters / Setters ----
    public int getIdBus() { return idBus; }
    public void setIdBus(int idBus) { this.idBus = idBus; }

    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String immatriculation) { this.immatriculation = immatriculation; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public StatutBus getStatut() { return statut; }
    public void setStatut(StatutBus statut) { this.statut = statut; }
}
