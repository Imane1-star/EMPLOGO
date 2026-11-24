package com.transportpro.model;

import com.transportpro.model.enums.StatutPresence;

import java.time.LocalDate;

public class Inscription {

    private int idInscription;
    private LocalDate dateInscription;
    private StatutPresence statutPresence;

    // Relations
    private Employe employe;
    private Trajet trajet;

    public Inscription() {
        this.statutPresence = StatutPresence.NON_RENSEIGNE;
    }

    public Inscription(int idInscription, LocalDate dateInscription,
                       StatutPresence statutPresence, Employe employe, Trajet trajet) {
        this.idInscription = idInscription;
        this.dateInscription = dateInscription;
        this.statutPresence = statutPresence;
        this.employe = employe;
        this.trajet = trajet;
    }

    public void enregistrerPresence(StatutPresence statut) {
        if (statut == null) {
            throw new IllegalArgumentException("Le statut de présence ne peut pas être null.");
        }
        this.statutPresence = statut;
    }

    // ---- statut de l'inscription (colonne inscription.statut) ----
    private String statut = "CONFIRMEE";   // CONFIRMEE ou ANNULEE

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public static String statutLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "CONFIRMEE" -> "Confirmée";
            case "ANNULEE" -> "Annulée";
            default -> code;
        };
    }

    /** Id de l'employé inscrit (0 si aucun). */
    public int getIdEmploye() { return employe == null ? 0 : employe.getIdEmploye(); }

    /** Id du trajet planifié (= id_affectation), 0 si aucun. */
    public int getIdAffectation() { return trajet == null ? 0 : trajet.getIdTrajet(); }

    /** Retourne un message d'erreur, ou null si l'inscription est valide. */
    public String validate() {
        if (getIdEmploye() <= 0) return "Veuillez choisir un employé.";
        if (getIdAffectation() <= 0) return "Veuillez choisir un trajet.";
        if (!"CONFIRMEE".equals(statut) && !"ANNULEE".equals(statut)) return "Veuillez choisir un statut.";
        return null;
    }

    // ---- Getters / Setters ----
    public int getIdInscription() { return idInscription; }
    public void setIdInscription(int idInscription) { this.idInscription = idInscription; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public StatutPresence getStatutPresence() { return statutPresence; }
    public void setStatutPresence(StatutPresence statutPresence) { this.statutPresence = statutPresence; }

    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }

    public Trajet getTrajet() { return trajet; }
    public void setTrajet(Trajet trajet) { this.trajet = trajet; }
}
