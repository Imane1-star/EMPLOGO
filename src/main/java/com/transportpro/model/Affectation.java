package com.transportpro.model;

import com.transportpro.model.enums.StatutTrajet;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Une ligne de la table « affectation » : une ligne de transport (trajet) planifiée à une date,
 * avec un bus et un chauffeur. C'est l'objet manipulé par les pages Trajets et Affectations.
 * Les champs « affichage » sont remplis par AffectationDAO grâce aux jointures (lecture seule).
 */
public class Affectation {

    private int idAffectation;
    private int idTrajet;
    private int idBus;
    private int idChauffeur;
    private LocalDate dateTrajet;
    private LocalTime heureDepart;
    private LocalTime heureArrivee;
    private String statut;              // valeur de la base : PLANIFIEE, EN_COURS, TERMINEE, ANNULEE

    // ---- affichage (jointures trajet / bus / chauffeur + employe / inscription)
    private String nomLigne = "";
    private String lieuDepart = "";
    private String lieuArrivee = "";
    private String immatriculation = "";
    private int capacite;
    private String chauffeurNom = "";
    private int inscrits;

    public Affectation() {
    }

    public Affectation(int idTrajet, int idBus, int idChauffeur,
                       LocalDate dateTrajet, LocalTime heureDepart,
                       LocalTime heureArrivee, String statut) {
        this.idTrajet = idTrajet;
        this.idBus = idBus;
        this.idChauffeur = idChauffeur;
        this.dateTrajet = dateTrajet;
        this.heureDepart = heureDepart;
        this.heureArrivee = heureArrivee;
        this.statut = statut;
    }

    /** Une affectation n'est modifiable / annulable que si elle est PLANIFIEE ou EN_COURS. */
    public boolean estOuverte() {
        return "PLANIFIEE".equals(statut) || "EN_COURS".equals(statut);
    }

    public StatutTrajet getStatutTrajet() {
        return StatutTrajet.fromCode(statut);
    }

    /** Retourne un message d'erreur, ou null si l'affectation est valide. */
    public String validate() {
        if (idTrajet <= 0) return "Veuillez choisir la ligne.";
        if (dateTrajet == null) return "La date est obligatoire.";
        if (heureDepart == null) return "L'heure de départ est obligatoire (format HH:mm).";
        if (heureArrivee == null) return "L'heure d'arrivée est obligatoire (format HH:mm).";
        if (!heureArrivee.isAfter(heureDepart)) return "L'heure d'arrivée doit être après l'heure de départ.";
        if (idBus <= 0) return "Veuillez choisir un bus.";
        if (idChauffeur <= 0) return "Veuillez choisir un chauffeur.";
        if (StatutTrajet.fromCode(statut) == null) return "Veuillez choisir un statut.";
        return null;
    }

    // ---- getters / setters ----
    public int getIdAffectation() { return idAffectation; }
    public void setIdAffectation(int idAffectation) { this.idAffectation = idAffectation; }

    public int getIdTrajet() { return idTrajet; }
    public void setIdTrajet(int idTrajet) { this.idTrajet = idTrajet; }

    public int getIdBus() { return idBus; }
    public void setIdBus(int idBus) { this.idBus = idBus; }

    public int getIdChauffeur() { return idChauffeur; }
    public void setIdChauffeur(int idChauffeur) { this.idChauffeur = idChauffeur; }

    public LocalDate getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDate dateTrajet) { this.dateTrajet = dateTrajet; }

    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime heureDepart) { this.heureDepart = heureDepart; }

    public LocalTime getHeureArrivee() { return heureArrivee; }
    public void setHeureArrivee(LocalTime heureArrivee) { this.heureArrivee = heureArrivee; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

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

    @Override
    public String toString() {
        return "T" + idAffectation + " (" + nomLigne + ", " + dateTrajet + ")";
    }
}
