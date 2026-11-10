package com.transportpro;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Modèle du module Trajets : un trajet planifié, c'est-à-dire une ligne de la table « affectation »
 * (ligne de transport + date + horaires + bus + chauffeur + statut).
 * La ligne (nom, départ, arrivée) vient de « trajet », la plaque et la capacité de « bus »,
 * le nom du chauffeur de « chauffeur » + « employe », le nombre d'inscrits de « inscription ».
 */
public class Trajet {

    private int idAffectation;
    private int idTrajet;        // ligne de transport (table trajet)
    private int idBus;
    private int idChauffeur;
    private LocalDate dateTrajet;
    private LocalTime heureDepart;
    private LocalTime heureArrivee;
    private String statut;

    // Lecture seule (issus des jointures)
    private String nomLigne;
    private String lieuDepart;
    private String lieuArrivee;
    private String immatriculation;
    private int capacite;
    private String chauffeurNom;
    private int inscrits;

    public Trajet() {}

    public int getIdAffectation() { return idAffectation; }
    public void setIdAffectation(int v) { this.idAffectation = v; }
    public int getIdTrajet() { return idTrajet; }
    public void setIdTrajet(int v) { this.idTrajet = v; }
    public int getIdBus() { return idBus; }
    public void setIdBus(int v) { this.idBus = v; }
    public int getIdChauffeur() { return idChauffeur; }
    public void setIdChauffeur(int v) { this.idChauffeur = v; }
    public LocalDate getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDate v) { this.dateTrajet = v; }
    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime v) { this.heureDepart = v; }
    public LocalTime getHeureArrivee() { return heureArrivee; }
    public void setHeureArrivee(LocalTime v) { this.heureArrivee = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
    public String getNomLigne() { return nomLigne; }
    public void setNomLigne(String v) { this.nomLigne = v; }
    public String getLieuDepart() { return lieuDepart; }
    public void setLieuDepart(String v) { this.lieuDepart = v; }
    public String getLieuArrivee() { return lieuArrivee; }
    public void setLieuArrivee(String v) { this.lieuArrivee = v; }
    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String v) { this.immatriculation = v; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int v) { this.capacite = v; }
    public String getChauffeurNom() { return chauffeurNom; }
    public void setChauffeurNom(String v) { this.chauffeurNom = v; }
    public int getInscrits() { return inscrits; }
    public void setInscrits(int v) { this.inscrits = v; }

    /** Texte affiché dans le tableau : PLANIFIEE -> « Planifié », TERMINEE -> « Terminé »... */
    public static String statutLabel(String statut) {
        if (statut == null || statut.isBlank()) return "";
        return switch (statut.trim().toUpperCase()) {
            case "PLANIFIEE" -> "Planifié";
            case "EN_COURS" -> "En cours";
            case "TERMINEE" -> "Terminé";
            case "ANNULEE" -> "Annulé";
            default -> {
                String s = statut.trim().replace('_', ' ').toLowerCase();
                yield Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }
        };
    }

    /** Retourne un message d'erreur, ou null si le trajet est valide. */
    public String validate() {
        if (idTrajet <= 0) return "Veuillez choisir une ligne.";
        if (dateTrajet == null) return "La date du trajet est obligatoire.";
        if (heureDepart == null) return "L'heure de départ est obligatoire.";
        if (heureArrivee == null) return "L'heure d'arrivée est obligatoire.";
        if (!heureArrivee.isAfter(heureDepart)) return "L'heure d'arrivée doit être après l'heure de départ.";
        if (idBus <= 0) return "Veuillez choisir un bus.";
        if (idChauffeur <= 0) return "Veuillez choisir un chauffeur.";
        if (statut == null || statut.isBlank()) return "Le statut est obligatoire.";
        return null;
    }

    @Override
    public String toString() {
        return "T" + idAffectation + " - " + nomLigne + " (" + dateTrajet + " " + heureDepart + ")";
    }
}