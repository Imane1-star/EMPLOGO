package com.transportpro;

import java.time.LocalDate;

/**
 * Modèle du module Chauffeurs : une ligne de la table « chauffeur ».
 * Un chauffeur est un employé (id_employe) : son nom et son téléphone viennent de la table « employe »
 * et ne sont modifiés que dans le module Employés.
 */
public class Chauffeur {

    private int idChauffeur;
    private int idEmploye;
    private String numeroPermis;
    private LocalDate dateValiditePermis;
    private String statut;

    // Lecture seule (issus de la jointure avec « employe »)
    private String nomComplet;
    private String telephone;

    public Chauffeur() {}

    public int getIdChauffeur() { return idChauffeur; }
    public void setIdChauffeur(int v) { this.idChauffeur = v; }
    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int v) { this.idEmploye = v; }
    public String getNumeroPermis() { return numeroPermis; }
    public void setNumeroPermis(String v) { this.numeroPermis = v; }
    public LocalDate getDateValiditePermis() { return dateValiditePermis; }
    public void setDateValiditePermis(LocalDate v) { this.dateValiditePermis = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String v) { this.nomComplet = v; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { this.telephone = v; }

    /** Texte affiché dans le tableau : ACTIF -> « Actif », EN_CONGE -> « En conge ». */
    public static String statutLabel(String statut) {
        if (statut == null || statut.isBlank()) return "";
        String s = statut.trim().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    /** Retourne un message d'erreur, ou null si le chauffeur est valide. */
    public String validate() {
        if (idEmploye <= 0) return "Veuillez choisir un employé.";
        if (blank(numeroPermis)) return "Le numéro de permis est obligatoire.";
        if (numeroPermis.trim().length() > 30) return "Le numéro de permis est trop long (30 caractères maximum).";
        if (dateValiditePermis == null) return "La date de validité du permis est obligatoire.";
        if (blank(statut)) return "Le statut est obligatoire.";
        return null;
    }

    @Override
    public String toString() {
        return nomComplet + " (" + numeroPermis + ")";
    }
}