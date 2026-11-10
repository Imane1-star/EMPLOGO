package com.transportpro.model;

import com.transportpro.model.enums.StatutPresence;

import java.time.LocalDate;

public class Inscription {

    private int idInscription;
    private LocalDate dateInscription;
    private StatutPresence statutPresence;

    // Relations
    private Employe employe;
    private Trajet trajet;

    public Inscription() {
        this.statutPresence = StatutPresence.NON_RENSEIGNE;
    }

    public Inscription(int idInscription, LocalDate dateInscription,
                       StatutPresence statutPresence, Employe employe, Trajet trajet) {
        this.idInscription = idInscription;
        this.dateInscription = dateInscription;
        this.statutPresence = statutPresence;
        this.employe = employe;
        this.trajet = trajet;
    }

    public void enregistrerPresence(StatutPresence statut) {
        if (statut == null) {
            throw new IllegalArgumentException("Le statut de présence ne peut pas être null.");
        }
        this.statutPresence = statut;
    }

    // ---- Getters / Setters ----
    public int getIdInscription() { return idInscription; }
    public void setIdInscription(int idInscription) { this.idInscription = idInscription; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public StatutPresence getStatutPresence() { return statutPresence; }
    public void setStatutPresence(StatutPresence statutPresence) { this.statutPresence = statutPresence; }

    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }

    public Trajet getTrajet() { return trajet; }
    public void setTrajet(Trajet trajet) { this.trajet = trajet; }
}
