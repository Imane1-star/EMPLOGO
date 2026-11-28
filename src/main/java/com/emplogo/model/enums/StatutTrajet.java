package com.emplogo.model.enums;

/**
 * Statut d'un trajet planifié. La base (colonne affectation.statut) écrit les valeurs au féminin
 * (PLANIFIEE, TERMINEE, ANNULEE) : {@link #code()} et {@link #fromCode(String)} font la conversion.
 */
public enum StatutTrajet {
    PLANIFIE("PLANIFIEE", "Planifié"),
    EN_COURS("EN_COURS", "En cours"),
    TERMINE("TERMINEE", "Terminé"),
    ANNULE("ANNULEE", "Annulé");

    private final String code;
    private final String libelle;

    StatutTrajet(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /** Valeur stockée dans la base. */
    public String code() { return code; }

    public String libelle() { return libelle; }

    /** Convertit une valeur de la base (PLANIFIEE...) ; null ou inconnue -> null. */
    public static StatutTrajet fromCode(String code) {
        if (code == null) return null;
        for (StatutTrajet s : values()) {
            if (s.code.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) return s;
        }
        return null;
    }

    /** Libellé d'une valeur de la base (PLANIFIEE -> « Planifié »). */
    public static String libelleOf(String code) {
        StatutTrajet s = fromCode(code);
        return s == null ? (code == null ? "" : code) : s.libelle;
    }

    @Override
    public String toString() { return libelle; }
}
