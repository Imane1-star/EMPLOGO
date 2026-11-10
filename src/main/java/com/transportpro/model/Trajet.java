package com.transportpro.model;

import com.transportpro.model.enums.StatutTrajet;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Trajet {

    private int idTrajet;
    private LocalDate date;
    private LocalTime heureDepart;
    private String destination;
    private StatutTrajet statut;

    // Relations
    private List<Arret> arrets = new ArrayList<>();          // composition 1..*
    private List<Inscription> inscriptions = new ArrayList<>(); // 0..*
    private List<Incident> incidents = new ArrayList<>();    // 0..*
    private Bus bus;                                         // 0..1
    private Chauffeur chauffeur;                             // 0..1

    public Trajet() {
    }

    public Trajet(int idTrajet, LocalDate date, LocalTime heureDepart,
                  String destination, StatutTrajet statut) {
        this.idTrajet = idTrajet;
        this.date = date;
        this.heureDepart = heureDepart;
        this.destination = destination;
        this.statut = statut;
    }

    public void ajouterArret(Arret arret) {
        if (arret != null) {
            this.arrets.add(arret);
        }
    }

    public int nombreInscrits() {
        return inscriptions.size();
    }

    public boolean estComplet() {
        if (bus == null) return false;
        return inscriptions.size() >= bus.getCapacite();
    }

    /**
     * Deux trajets se chevauchent s'ils ont la même date et que leurs
     * intervalles horaires (départ + durée estimée) se superposent.
     * Durée par défaut : 2h (à affiner selon les besoins).
     */
    public boolean chevauche(Trajet autre) {
        if (autre == null || !this.date.equals(autre.date)) return false;
        LocalTime finA = this.heureDepart.plusHours(2);
        LocalTime finB = autre.heureDepart.plusHours(2);
        return this.heureDepart.isBefore(finB) && autre.heureDepart.isBefore(finA);
    }

    // ---- Getters / Setters ----
    public int getIdTrajet() { return idTrajet; }
    public void setIdTrajet(int idTrajet) { this.idTrajet = idTrajet; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime heureDepart) { this.heureDepart = heureDepart; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public StatutTrajet getStatut() { return statut; }
    public void setStatut(StatutTrajet statut) { this.statut = statut; }

    public List<Arret> getArrets() { return arrets; }
    public void setArrets(List<Arret> arrets) { this.arrets = arrets; }

    public List<Inscription> getInscriptions() { return inscriptions; }
    public void setInscriptions(List<Inscription> inscriptions) { this.inscriptions = inscriptions; }

    public List<Incident> getIncidents() { return incidents; }
    public void setIncidents(List<Incident> incidents) { this.incidents = incidents; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public Chauffeur getChauffeur() { return chauffeur; }
    public void setChauffeur(Chauffeur chauffeur) { this.chauffeur = chauffeur; }
}
