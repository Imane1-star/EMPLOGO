package com.transportpro.model;

import com.transportpro.model.enums.TypeIncident;

import java.time.LocalDateTime;

public class Incident {

    private int idIncident;
    private TypeIncident type;
    private String description;
    private LocalDateTime dateHeure;

    public Incident() {
    }

    public Incident(int idIncident, TypeIncident type, String description, LocalDateTime dateHeure) {
        this.idIncident = idIncident;
        this.type = type;
        this.description = description;
        this.dateHeure = dateHeure;
    }

    public void creer() {
        if (this.dateHeure == null) {
            this.dateHeure = LocalDateTime.now();
        }
        // TODO : persistance via IncidentDAO
    }

    // ---- Getters / Setters ----
    public int getIdIncident() { return idIncident; }
    public void setIdIncident(int idIncident) { this.idIncident = idIncident; }

    public TypeIncident getType() { return type; }
    public void setType(TypeIncident type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
}
