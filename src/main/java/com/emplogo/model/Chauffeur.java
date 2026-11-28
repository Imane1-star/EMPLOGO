package com.emplogo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.emplogo.model.enums.Role;
import com.emplogo.model.enums.TypeIncident;

public class Chauffeur extends Utilisateur{
    private int idChauffeur;            // clé de la table chauffeur (distincte du compte utilisateur)
    private String nom;
    private String telephone;
    private String numeroPermis;
    private boolean disponible;
    private List<Trajet> trajets = new ArrayList<>();

    // colonnes de la table chauffeur (le nom et le téléphone viennent de l'employé lié)
    private int idEmploye;
    private String prenom;
    private LocalDate validitePermis;   // chauffeur.validite_permis
    private String statut;              // chauffeur.statut : ACTIF, EN_CONGE, SUSPENDU

    /** Valeurs de la colonne chauffeur.statut. */
    public static final String[] STATUTS = {"ACTIF", "EN_CONGE", "SUSPENDU"};

    public Chauffeur() {
        super();
        setRole(Role.CHAUFFEUR);
    }

    public Chauffeur(int idUtilisateur,String login , String motDePasseHache, boolean actif, Role role, String nom, String telephone,
                     String numeroPermis, boolean disponible) {
        super(idUtilisateur, login, motDePasseHache, actif, role);
        this.nom = nom;
        this.telephone = telephone;
        this.numeroPermis = numeroPermis;
        this.disponible = disponible;
    }

    /** Constructeur utilisé par ChauffeurDAO (ligne de la table chauffeur, sans compte de connexion). */
    public Chauffeur(int idChauffeur, String nom, String telephone, String numeroPermis, boolean disponible) {
        this();
        this.idChauffeur = idChauffeur;
        this.nom = nom;
        this.telephone = telephone;
        this.numeroPermis = numeroPermis;
        this.disponible = disponible;
    }

    public List<Trajet> consulterPlanning() {
        return new ArrayList<>(trajets);
    }

    public Incident signalerIncident(TypeIncident type, String description) {
        Incident i = new Incident();
        i.setType(type);
        i.setDescription(description);
        i.setDateHeure(LocalDateTime.now());
        i.creer();
        return i;
    }

    /** « Nom Prénom » (le prénom peut être vide). */
    public String getNomComplet() {
        return ((nom == null ? "" : nom) + " " + (prenom == null ? "" : prenom)).trim();
    }

    /** Libellé affiché d'un statut de la base. */
    public static String statutLabel(String statut) {
        if (statut == null) return "";
        return switch (statut) {
            case "ACTIF" -> "Actif";
            case "EN_CONGE" -> "En congé";
            case "SUSPENDU" -> "Suspendu";
            default -> statut;
        };
    }

    /** Retourne un message d'erreur, ou null si la fiche chauffeur est valide. */
    public String validate() {
        if (idEmploye <= 0) return "Veuillez choisir l'employé qui devient chauffeur.";
        if (numeroPermis == null || numeroPermis.isBlank()) return "Le numéro de permis est obligatoire.";
        if (numeroPermis.trim().length() > 30) return "Le numéro de permis ne doit pas dépasser 30 caractères.";
        if (validitePermis == null) return "La date de validité du permis est obligatoire.";
        if (statut == null || statut.isBlank()) return "Le statut est obligatoire.";
        return null;
    }

    // ---- Getters / Setters ----
    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int idEmploye) { this.idEmploye = idEmploye; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getValiditePermis() { return validitePermis; }
    public void setValiditePermis(LocalDate validitePermis) { this.validitePermis = validitePermis; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) {
        this.statut = statut;
        this.disponible = "ACTIF".equals(statut);
    }

    public int getIdChauffeur() { return idChauffeur; }
    public void setIdChauffeur(int idChauffeur) { this.idChauffeur = idChauffeur; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getNumeroPermis() { return numeroPermis; }
    public void setNumeroPermis(String numeroPermis) { this.numeroPermis = numeroPermis; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public List<Trajet> getTrajets() { return trajets; }
    public void setTrajets(List<Trajet> trajets) { this.trajets = trajets; }
}
