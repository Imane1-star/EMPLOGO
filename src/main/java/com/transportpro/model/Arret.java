package com.transportpro.model;

import java.time.LocalTime;

public class Arret {

    private int idArret;
    private String nom;
    private int ordre;
    private LocalTime heurePassage;

    public Arret() {
    }

    public Arret(int idArret, String nom, int ordre, LocalTime heurePassage) {
        this.idArret = idArret;
        this.nom = nom;
        this.ordre = ordre;
        this.heurePassage = heurePassage;
    }

    // ---- Getters / Setters ----
    public int getIdArret() { return idArret; }
    public void setIdArret(int idArret) { this.idArret = idArret; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public LocalTime getHeurePassage() { return heurePassage; }
    public void setHeurePassage(LocalTime heurePassage) { this.heurePassage = heurePassage; }
}
