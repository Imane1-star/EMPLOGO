package com.transportpro;

public enum Page {
    DASHBOARD("Tableau de bord", "dashboard"),
    EMPLOYEES("Employés", "employees"),
    BUSES("Bus", "buses"),
    DRIVERS("Chauffeurs", "drivers"),
    TRIPS("Trajets", "trips"),
    ASSIGNMENTS("Affectations", "assignments"),
    REGISTRATIONS("Inscriptions", "registrations"),
    ATTENDANCE("Présences", "attendance"),
    INCIDENTS("Incidents", "incidents"),
    REPORTS("Statistiques et rapports", "reports"),
    ACCOUNTS("Gestion des comptes", "accounts"),
    ROLES("Rôles et autorisations", "roles"),
    PLANNING("Mon planning", "planning");

    public final String title;
    public final String icon;

    Page(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }
}
