package com.transportpro.model;

import com.transportpro.model.enums.StatutPresence;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class Rapport {

    private int idRapport;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    private String type;

    private List<Trajet> trajets;

    public Rapport() {
    }

    public Rapport(int idRapport, LocalDate periodeDebut, LocalDate periodeFin,
                   String type, List<Trajet> trajets) {
        this.idRapport = idRapport;
        this.periodeDebut = periodeDebut;
        this.periodeFin = periodeFin;
        this.type = type;
        this.trajets = trajets;
    }

    public int calculerNbTrajets() {
        return trajets == null ? 0 : trajets.size();
    }

    public double calculerTauxPresence() {
        if (trajets == null || trajets.isEmpty()) return 0.0;
        long total = 0, presents = 0;
        for (Trajet t : trajets) {
            for (Inscription i : t.getInscriptions()) {
                total++;
                if (i.getStatutPresence() == StatutPresence.PRESENT) {
                    presents++;
                }
            }
        }
        return total == 0 ? 0.0 : (presents * 100.0) / total;
    }

    public double estimerCouts() {
        // Exemple simplifié : 1.5 unité par trajet
        return calculerNbTrajets() * 1.5;
    }

    public File exporter(String format) {
        // TODO : export réel (CSV/PDF) via une classe utilitaire
        File f = new File("rapport_" + idRapport + "." + format);
        System.out.println("[Rapport] Export simulé vers " + f.getAbsolutePath());
        return f;
    }

    // ---- Getters / Setters ----
    public int getIdRapport() { return idRapport; }
    public void setIdRapport(int idRapport) { this.idRapport = idRapport; }

    public LocalDate getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(LocalDate periodeDebut) { this.periodeDebut = periodeDebut; }

    public LocalDate getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(LocalDate periodeFin) { this.periodeFin = periodeFin; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<Trajet> getTrajets() { return trajets; }
    public void setTrajets(List<Trajet> trajets) { this.trajets = trajets; }
}