
package com.transportpro;

import java.time.LocalDate;

/** Modèle du module Employés : une ligne de la table « employe ». */
public class Employee {

    private int idEmploye;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private String departement;
    private LocalDate dateEmbauche;
    private boolean actif = true;

    public Employee() {}

    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int v) { this.idEmploye = v; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String v) { this.matricule = v; }

    public String getNom() { return nom; }
    public void setNom(String v) { this.nom = v; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String v) { this.prenom = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { this.telephone = v; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { this.adresse = v; }

    public String getDepartement() { return departement; }
    public void setDepartement(String v) { this.departement = v; }

    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate v) { this.dateEmbauche = v; }

    public boolean isActif() { return actif; }
    public void setActif(boolean v) { this.actif = v; }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Retourne un message d'erreur, ou null si l'employé est valide. */
    public String validate() {

        if (blank(matricule))
            return "Le matricule est obligatoire.";

        if (blank(nom))
            return "Le nom est obligatoire.";

        if (blank(prenom))
            return "Le prénom est obligatoire.";

        if (blank(email))
            return "L'adresse e-mail est obligatoire.";

        if (!email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return "L'adresse e-mail n'est pas valide.";

        if (!blank(telephone)
                && !telephone.trim().matches("^[+0-9][0-9 .\\-]{5,19}$"))
            return "Le téléphone n'est pas valide.";

        return null;
    }

    @Override
    public String toString() {
        return matricule + " - " + nom + " " + prenom;
    }
}
