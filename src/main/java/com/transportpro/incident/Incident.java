package com.transportpro.incident;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modèle du module Incidents : une ligne de la table « incident », enrichie (lecture seule)
 * par jointure avec l'affectation (trajet, date), le bus, le chauffeur et le compte déclarant.
 */
public class Incident {

    // ---- colonnes de la table incident
    private int idIncident;
    private int idAffectation;
    private Integer idCompte;            // peut être null (déclaré par téléphone, compte supprimé...)
    private String typeIncident;         // PANNE, ACCIDENT, RETARD, COMPORTEMENT, AUTRE
    private String gravite;              // FAIBLE, MOYENNE, ELEVEE, CRITIQUE
    private String description;
    private LocalDateTime dateIncident;
    private String modeDeclaration;      // APPLICATION, TELEPHONE, ...
    private String statutTraitement;     // OUVERT, EN_COURS, RESOLU, CLOS

    // ---- informations liées (jointures)
    private String nomLigne;
    private LocalDate dateTrajet;
    private int idBus;
    private String immatriculation;
    private String chauffeurNom;
    private String declarant;            // login du compte, ou vide

    public int getIdIncident() { return idIncident; }
    public void setIdIncident(int v) { this.idIncident = v; }
    public int getIdAffectation() { return idAffectation; }
    public void setIdAffectation(int v) { this.idAffectation = v; }
    public Integer getIdCompte() { return idCompte; }
    public void setIdCompte(Integer v) { this.idCompte = v; }
    public String getTypeIncident() { return typeIncident; }
    public void setTypeIncident(String v) { this.typeIncident = v; }
    public String getGravite() { return gravite; }
    public void setGravite(String v) { this.gravite = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public LocalDateTime getDateIncident() { return dateIncident; }
    public void setDateIncident(LocalDateTime v) { this.dateIncident = v; }
    public String getModeDeclaration() { return modeDeclaration; }
    public void setModeDeclaration(String v) { this.modeDeclaration = v; }
    public String getStatutTraitement() { return statutTraitement; }
    public void setStatutTraitement(String v) { this.statutTraitement = v; }

    public String getNomLigne() { return nomLigne; }
    public void setNomLigne(String v) { this.nomLigne = v; }
    public LocalDate getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDate v) { this.dateTrajet = v; }
    public int getIdBus() { return idBus; }
    public void setIdBus(int v) { this.idBus = v; }
    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String v) { this.immatriculation = v; }
    public String getChauffeurNom() { return chauffeurNom; }
    public void setChauffeurNom(String v) { this.chauffeurNom = v; }
    public String getDeclarant() { return declarant; }
    public void setDeclarant(String v) { this.declarant = v; }

    // ---------------------------------------------------------------- libellés affichés
    public static String typeLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "PANNE" -> "Panne";
            case "ACCIDENT" -> "Accident";
            case "RETARD" -> "Retard";
            case "COMPORTEMENT" -> "Comportement";
            case "AUTRE" -> "Autre";
            default -> code;
        };
    }

    public static String graviteLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "FAIBLE" -> "Faible";
            case "MOYENNE" -> "Moyenne";
            case "ELEVEE" -> "Élevée";
            case "CRITIQUE" -> "Critique";
            default -> code;
        };
    }

    public static String statutLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "OUVERT" -> "Ouvert";
            case "EN_COURS" -> "En cours";
            case "RESOLU" -> "Résolu";
            case "CLOS" -> "Clos";
            default -> code;
        };
    }

    public static String modeLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "APPLICATION" -> "Application";
            case "TELEPHONE" -> "Téléphone";
            case "EMAIL" -> "E-mail";
            case "SUR_PLACE" -> "Sur place";
            case "RAPPORT_PAPIER" -> "Rapport papier";
            case "SIGNALEMENT_CHAUFFEUR" -> "Signalement chauffeur";
            default -> code;
        };
    }

    /** Retourne un message d'erreur, ou null si l'incident est valide. */
    public String validate() {
        if (idAffectation <= 0) return "Veuillez choisir le trajet concerné.";
        if (typeIncident == null || typeIncident.isBlank()) return "Veuillez choisir le type d'incident.";
        if (gravite == null || gravite.isBlank()) return "Veuillez choisir la gravité.";
        if (dateIncident == null) return "La date et l'heure de l'incident sont obligatoires.";
        if (dateIncident.isAfter(LocalDateTime.now().plusMinutes(5)))
            return "La date de l'incident ne peut pas être dans le futur.";
        if (modeDeclaration == null || modeDeclaration.isBlank()) return "Veuillez choisir le mode de déclaration.";
        if (statutTraitement == null || statutTraitement.isBlank()) return "Veuillez choisir le statut.";
        if (description == null || description.isBlank()) return "La description est obligatoire.";
        if (description.length() > 2000) return "La description ne doit pas dépasser 2000 caractères.";
        return null;
    }

    @Override
    public String toString() {
        return "Incident " + idIncident + " (" + typeIncident + ")";
    }
}
