package com.transportpro;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Modèle du module Présence : une ligne de la table « presence »
 * enrichie (lecture seule) avec les informations de l'employé, de l'affectation et du trajet
 * obtenues par jointure : presence -> inscription -> employe / affectation -> trajet.
 *
 * Remarque : la table « presence » n'a pas de colonne date. La date d'une présence est
 * la date du trajet (affectation.date_trajet).
 */
public class Presence {

    public static final String PRESENT = "PRESENT";
    public static final String ABSENT = "ABSENT";
    public static final String RETARD = "RETARD";

    /** Valeurs exactes de l'ENUM MySQL presence.statut. */
    public static final String[] STATUTS = {PRESENT, ABSENT, RETARD};

    // ---- colonnes de la table presence
    private int idPresence;          // presence.id_presence (0 = pas encore enregistrée)
    private int idInscription;       // presence.id_inscription
    private String statut;           // presence.statut
    private LocalTime heureMontee;   // presence.heure_montee (peut être null)
    private String remarque;         // presence.remarque (peut être null)

    // ---- informations liées (lecture seule, venant des jointures)
    private int idEmploye;           // employe.id_employe
    private String matricule;        // employe.matricule
    private String nomEmploye;       // employe.nom
    private String prenomEmploye;    // employe.prenom
    private String departement;      // employe.departement
    private int idAffectation;       // affectation.id_affectation
    private LocalDate dateTrajet;    // affectation.date_trajet
    private LocalTime heureDepart;   // affectation.heure_depart
    private String trajetNom;        // trajet.nom
    private String lieuDepart;       // trajet.lieu_depart
    private String lieuArrivee;      // trajet.lieu_arrivee

    public Presence() {}

    // ------------------------------------------------------------------ getters / setters
    public int getIdPresence() { return idPresence; }
    public void setIdPresence(int v) { this.idPresence = v; }
    public int getIdInscription() { return idInscription; }
    public void setIdInscription(int v) { this.idInscription = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
    public LocalTime getHeureMontee() { return heureMontee; }
    public void setHeureMontee(LocalTime v) { this.heureMontee = v; }
    public String getRemarque() { return remarque; }
    public void setRemarque(String v) { this.remarque = v; }

    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int v) { this.idEmploye = v; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String v) { this.matricule = v; }
    public String getNomEmploye() { return nomEmploye; }
    public void setNomEmploye(String v) { this.nomEmploye = v; }
    public String getPrenomEmploye() { return prenomEmploye; }
    public void setPrenomEmploye(String v) { this.prenomEmploye = v; }
    public String getDepartement() { return departement; }
    public void setDepartement(String v) { this.departement = v; }
    public int getIdAffectation() { return idAffectation; }
    public void setIdAffectation(int v) { this.idAffectation = v; }
    public LocalDate getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDate v) { this.dateTrajet = v; }
    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime v) { this.heureDepart = v; }
    public String getTrajetNom() { return trajetNom; }
    public void setTrajetNom(String v) { this.trajetNom = v; }
    public String getLieuDepart() { return lieuDepart; }
    public void setLieuDepart(String v) { this.lieuDepart = v; }
    public String getLieuArrivee() { return lieuArrivee; }
    public void setLieuArrivee(String v) { this.lieuArrivee = v; }

    // ------------------------------------------------------------------ outils d'affichage
    /** « Nom Prénom » de l'employé (même présentation que le module Employés). */
    public String getNomComplet() {
        String n = nomEmploye == null ? "" : nomEmploye;
        String p = prenomEmploye == null ? "" : prenomEmploye;
        return (n + " " + p).trim();
    }

    /** Texte affiché dans l'interface : PRESENT -> « Présent », etc. */
    public static String statutLabel(String statut) {
        if (statut == null) return "";
        return switch (statut) {
            case PRESENT -> "Présent";
            case ABSENT -> "Absent";
            case RETARD -> "Retard";
            default -> statut;
        };
    }

    /** Inverse de statutLabel : « Présent » -> PRESENT (retourne null si inconnu). */
    public static String statutFromLabel(String label) {
        if (label == null) return null;
        return switch (label.trim().toLowerCase()) {
            case "présent", "present" -> PRESENT;
            case "absent" -> ABSENT;
            case "retard" -> RETARD;
            default -> null;
        };
    }

    public static boolean isStatutValide(String statut) {
        for (String s : STATUTS) if (s.equals(statut)) return true;
        return false;
    }

    /** Retourne un message d'erreur, ou null si la présence est valide. */
    public String validate() {
        if (idInscription <= 0)
            return "Veuillez choisir une inscription (employé et trajet).";
        if (!isStatutValide(statut))
            return "Le statut doit être Présent, Absent ou Retard.";
        if (remarque != null && remarque.trim().length() > 255)
            return "La remarque ne doit pas dépasser 255 caractères.";
        return null;
    }

    @Override
    public String toString() {
        return getNomComplet() + " - " + dateTrajet + " - " + statutLabel(statut);
    }
}