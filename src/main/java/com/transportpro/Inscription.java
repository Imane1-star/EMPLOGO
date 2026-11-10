package com.transportpro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Modèle du module Inscriptions : une ligne de la table « inscription »
 * (id_inscription, id_employe, id_affectation, date_inscription, statut).
 * Les champs « lecture seule » viennent des jointures avec employe, affectation et trajet.
 */
public class Inscription {

    private int idInscription;
    private int idEmploye;
    private int idAffectation;
    private LocalDateTime dateInscription;
    private String statut;

    // Lecture seule (issus des jointures)
    private String matricule;
    private String employeNom;
    private String nomLigne;
    private LocalDate dateTrajet;
    private LocalTime heureDepart;
    private LocalTime heureArrivee;

    public Inscription() {}

    public int getIdInscription() { return idInscription; }
    public void setIdInscription(int v) { this.idInscription = v; }
    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int v) { this.idEmploye = v; }
    public int getIdAffectation() { return idAffectation; }
    public void setIdAffectation(int v) { this.idAffectation = v; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime v) { this.dateInscription = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String v) { this.matricule = v; }
    public String getEmployeNom() { return employeNom; }
    public void setEmployeNom(String v) { this.employeNom = v; }
    public String getNomLigne() { return nomLigne; }
    public void setNomLigne(String v) { this.nomLigne = v; }
    public LocalDate getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDate v) { this.dateTrajet = v; }
    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime v) { this.heureDepart = v; }
    public LocalTime getHeureArrivee() { return heureArrivee; }
    public void setHeureArrivee(LocalTime v) { this.heureArrivee = v; }

    /** CONFIRMEE -> « Confirmée », ANNULEE -> « Annulée ». */
    public static String statutLabel(String statut) {
        if (statut == null || statut.isBlank()) return "";
        return switch (statut.trim().toUpperCase()) {
            case "CONFIRMEE" -> "Confirmée";
            case "ANNULEE" -> "Annulée";
            default -> {
                String s = statut.trim().replace('_', ' ').toLowerCase();
                yield Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }
        };
    }

    /** Retourne un message d'erreur, ou null si l'inscription est valide. */
    public String validate() {
        if (idEmploye <= 0) return "Veuillez choisir un employé.";
        if (idAffectation <= 0) return "Veuillez choisir une affectation.";
        if (statut == null || statut.isBlank()) return "Le statut est obligatoire.";
        return null;
    }

    @Override
    public String toString() {
        return "I" + idInscription + " - " + employeNom + " (" + nomLigne + " " + dateTrajet + ")";
    }
}
