package com.emplogo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.emplogo.model.enums.TypeIncident;

public class Incident {

    private int idIncident;
    private TypeIncident type;
    private String description;
    private LocalDateTime dateHeure;

    public Incident() {
    }

    public Incident(int idIncident, TypeIncident type, String description, LocalDateTime dateHeure) {
        this.idIncident = idIncident;
        this.type = type;
        this.description = description;
        this.dateHeure = dateHeure;
    }

    /** Prépare l'incident avant enregistrement ; la persistance est faite par IncidentDAO.insert(). */
    public void creer() {
        if (this.dateHeure == null) {
            this.dateHeure = LocalDateTime.now();
        }
        if (this.statut == null) this.statut = "OUVERT";
    }

    // ---- autres colonnes de la table incident ----
    private int idAffectation;          // trajet planifié concerné
    private Integer idCompte;           // compte déclarant (peut être null)
    private String gravite;             // MINEURE, MODEREE, GRAVE, CRITIQUE
    private String statut;              // OUVERT, EN_COURS, RESOLU, CLOTURE
    private String modeDeclaration;     // APPLICATION, TELEPHONE, RAPPORT_PAPIER, SIGNALEMENT_CHAUFFEUR

    // ---- affichage (jointures, lecture seule) ----
    private String nomLigne = "";
    private LocalDate dateTrajet;
    private int idBus;
    private String immatriculation = "";
    private String chauffeurNom = "";
    private String declarant = "";

    public static String graviteLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "MINEURE" -> "Mineure";
            case "MODEREE" -> "Modérée";
            case "GRAVE" -> "Grave";
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
            case "CLOTURE" -> "Clôturé";
            default -> code;
        };
    }

    public static String modeLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "APPLICATION" -> "Application";
            case "TELEPHONE" -> "Téléphone";
            case "RAPPORT_PAPIER" -> "Rapport papier";
            case "SIGNALEMENT_CHAUFFEUR" -> "Signalement chauffeur";
            default -> code;
        };
    }

    /** Retourne un message d'erreur, ou null si l'incident est valide (BF20). */
    public String validate() {
        if (idAffectation <= 0) return "Veuillez choisir le trajet concerné.";
        if (type == null) return "Veuillez choisir le type d'incident.";
        if (gravite == null || gravite.isBlank()) return "Veuillez choisir la gravité.";
        if (dateHeure == null) return "La date et l'heure de l'incident sont obligatoires.";
        if (dateHeure.isAfter(LocalDateTime.now().plusMinutes(5)))
            return "La date de l'incident ne peut pas être dans le futur.";
        if (modeDeclaration == null || modeDeclaration.isBlank()) return "Veuillez choisir le mode de déclaration.";
        if (statut == null || statut.isBlank()) return "Veuillez choisir le statut.";
        if (description == null || description.isBlank()) return "La description est obligatoire.";
        if (description.length() > 2000) return "La description ne doit pas dépasser 2000 caractères.";
        return null;
    }

    public int getIdAffectation() { return idAffectation; }
    public void setIdAffectation(int v) { this.idAffectation = v; }
    public Integer getIdCompte() { return idCompte; }
    public void setIdCompte(Integer v) { this.idCompte = v; }
    public String getGravite() { return gravite; }
    public void setGravite(String v) { this.gravite = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
    public String getModeDeclaration() { return modeDeclaration; }
    public void setModeDeclaration(String v) { this.modeDeclaration = v; }
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

    // ---- Getters / Setters ----
    public int getIdIncident() { return idIncident; }
    public void setIdIncident(int idIncident) { this.idIncident = idIncident; }

    public TypeIncident getType() { return type; }
    public void setType(TypeIncident type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
}
