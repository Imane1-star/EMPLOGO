package com.transportpro;

/** Modèle du module Bus : une ligne de la table MySQL/MariaDB « bus ». */
public class Bus {

    public static final String[] STATUTS = {"DISPONIBLE", "EN_MAINTENANCE", "HORS_SERVICE"};

    private int idBus;
    private String immatriculation;
    private String marque;
    private String modele;
    private int capacite;
    private String statut;

    public Bus() {}

    public Bus(int idBus, String immatriculation, String marque, String modele,
               int capacite, String statut) {
        this.idBus = idBus;
        this.immatriculation = immatriculation;
        this.marque = marque;
        this.modele = modele;
        this.capacite = capacite;
        this.statut = statut;
    }

    public int getIdBus() { return idBus; }
    public void setIdBus(int idBus) { this.idBus = idBus; }
    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String v) { this.immatriculation = v; }
    public String getMarque() { return marque; }
    public void setMarque(String v) { this.marque = v; }
    public String getModele() { return modele; }
    public void setModele(String v) { this.modele = v; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int v) { this.capacite = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }

    /** Texte affiché dans le tableau (même présentation que l'ancienne maquette). */
    public static String statutLabel(String statut) {
        if (statut == null) return "";
        return switch (statut) {
            case "DISPONIBLE" -> "Disponible";
            case "EN_MAINTENANCE" -> "En maintenance";
            case "HORS_SERVICE" -> "Hors service";
            default -> statut;
        };
    }

    /** Retourne un message d'erreur, ou null si le bus est valide. */
    public String validate() {
        if (immatriculation == null || immatriculation.isBlank())
            return "L'immatriculation est obligatoire.";
        if (immatriculation.trim().length() > 20)
            return "L'immatriculation ne doit pas dépasser 20 caractères.";
        if (marque != null && marque.trim().length() > 50)
            return "La marque ne doit pas dépasser 50 caractères.";
        if (modele != null && modele.trim().length() > 50)
            return "Le modèle ne doit pas dépasser 50 caractères.";
        if (capacite <= 0 || capacite > 65535)
            return "La capacité doit être un entier entre 1 et 65535.";
        boolean ok = false;
        for (String s : STATUTS) if (s.equals(statut)) ok = true;
        if (!ok)
            return "Le statut doit être DISPONIBLE, EN_MAINTENANCE ou HORS_SERVICE.";
        return null;
    }

    @Override
    public String toString() {
        return immatriculation + " (" + statut + ")";
    }
}