package com.transportpro.model;

import com.transportpro.model.enums.StatutBus;

import java.time.LocalDate;
import java.time.LocalTime;

public class Bus {

    private int idBus;
    private String immatriculation;
    private int capacite;
    private StatutBus statut;

    public Bus() {
    }

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
