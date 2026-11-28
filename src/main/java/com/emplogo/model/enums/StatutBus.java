package com.emplogo.model.enums;

/** Mêmes valeurs que la colonne bus.statut. */
public enum StatutBus {
    DISPONIBLE("Disponible"),
    EN_MAINTENANCE("En maintenance"),
    HORS_SERVICE("Hors service");

    private final String libelle;

    StatutBus(String libelle) { this.libelle = libelle; }

    /** Texte affiché dans l'interface. */
    public String libelle() { return libelle; }

    @Override
    public String toString() { return libelle; }
}
