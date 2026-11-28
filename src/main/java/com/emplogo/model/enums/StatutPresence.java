package com.emplogo.model.enums;

/**
 * Statut de présence d'une inscription.
 * PRESENT, ABSENT, RETARD = valeurs de la colonne presence.statut ;
 * NON_RENSEIGNE = aucune ligne dans la table presence pour cette inscription.
 */
public enum StatutPresence {
    PRESENT("Présent"),
    ABSENT("Absent"),
    RETARD("En retard"),
    NON_RENSEIGNE("Non renseigné");

    private final String libelle;

    StatutPresence(String libelle) { this.libelle = libelle; }

    public String libelle() { return libelle; }

    @Override
    public String toString() { return libelle; }
}
