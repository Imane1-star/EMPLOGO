package com.emplogo.model.enums;

/** Mêmes valeurs que la colonne incident.type. */
public enum TypeIncident {
    RETARD("Retard"),
    PANNE("Panne"),
    ACCIDENT("Accident"),
    COMPORTEMENT("Comportement"),
    AUTRE("Autre");

    private final String libelle;

    TypeIncident(String libelle) { this.libelle = libelle; }

    public String libelle() { return libelle; }

    @Override
    public String toString() { return libelle; }
}
