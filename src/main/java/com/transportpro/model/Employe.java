package com.transportpro.model;

import java.time.LocalDate;
import java.util.Objects;

public class Employe {

    private int idEmploye;
    private String matricule;
    private String nom;
    private String prenom;
    private String telephone;
    private String adresse;             // joue le rôle de "point d'embarquement"
    private String departement;
    private LocalDate dateEmbauche;
    private boolean actif;

    public Employe() {
    }

    public Employe(int idEmploye, String matricule, String nom, String prenom,
                   String telephone, String adresse, String departement,
                   LocalDate dateEmbauche, boolean actif) {
        this.idEmploye = idEmploye;
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.adresse = adresse;
        this.departement = departement;
        this.dateEmbauche = dateEmbauche;
        this.actif = actif;
    }

    public void desactiver() {
        this.actif = false;
    }

    public String getNomComplet() {
        return nom + " " + prenom;
    }

    // ---- getters / setters ----
    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int idEmploye) { this.idEmploye = idEmploye; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employe e)) return false;
        return idEmploye == e.idEmploye;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEmploye);
    }

    @Override
    public String toString() {
        return "Employe{" + idEmploye + ", " + getNomComplet() + "}";
    }
}