package com.transportpro.model;

import com.transportpro.model.enums.Role;

public class ResponsableTransport extends Utilisateur {

    private String nom;
    private String telephone;

    public ResponsableTransport() {
        super();
        setRole(Role.RESPONSABLE);
    }

    public ResponsableTransport(int idUtilisateur, String login, String motDePasseHache,
                                boolean actif, String nom, String telephone) {
        super(idUtilisateur, login, motDePasseHache, actif, Role.RESPONSABLE);
        this.nom = nom;
        this.telephone = telephone;
    }

    public void planifierTrajet() {/* délégué au service */}

    public void affecterBusChauffeur() {/* délégué au service */}

    // ---- Getters / Setters ----
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}