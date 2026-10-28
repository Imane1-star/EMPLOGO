package com.transportpro;

/** Données de démonstration (les mêmes que la maquette Figma). */
public final class Data {
    private Data() {}

    public static final String[][] EMPLOYEES = {
        {"EMP-001", "Ahmed El Idrissi", "06 12 34 56 78", "Taroudant Centre", "Actif"},
        {"EMP-002", "Fatima Zahra Bennani", "06 23 45 67 89", "Oulad Teima", "Actif"},
        {"EMP-003", "Youssef Amrani", "06 34 56 78 90", "Ait Iaazza", "Actif"},
        {"EMP-004", "Khadija Lahlou", "06 45 67 89 01", "Tiout", "Inactif"},
        {"EMP-005", "Omar Berrada", "06 56 78 90 12", "Taroudant Centre", "Actif"},
        {"EMP-006", "Salma Tazi", "06 67 89 01 23", "Oulad Teima", "Actif"},
    };

    public static final String[][] BUSES = {
        {"12345-A-45", "30 places", "Disponible"},
        {"23456-B-45", "50 places", "Disponible"},
        {"34567-A-45", "40 places", "En maintenance"},
        {"45678-C-45", "45 places", "Hors service"},
    };

    public static final String[][] DRIVERS = {
        {"Hassan Ouazzani", "06 18 44 32 10", "P-458921", "Disponible"},
        {"Rachid Benali", "06 27 19 65 44", "P-650124", "Disponible"},
        {"Mohamed Alaoui", "06 49 08 36 52", "P-782340", "Indisponible"},
    };

    public static final String[][] TRIPS = {
        {"T1", "12/06/2025", "07:00", "Zone industrielle", "Bus B", "H. Ouazzani", "35 / 50", "Planifié"},
        {"T2", "12/06/2025", "17:30", "Taroudant Centre", "Bus A", "R. Benali", "28 / 30", "Planifié"},
        {"T3", "12/06/2025", "08:15", "Oulad Teima", "—", "—", "22 / 40", "À affecter"},
        {"T4", "11/06/2025", "17:30", "Taroudant Centre", "Bus B", "H. Ouazzani", "42 / 50", "Terminé"},
    };

    public static final String[][] INCIDENTS = {
        {"10/06/2025 • 17:48", "Retard", "T2", "Bus A", "Rachid Benali", "Circulation dense"},
        {"08/06/2025 • 06:42", "Panne", "T1", "Bus C", "Mohamed Alaoui", "Problème de démarrage"},
        {"04/06/2025 • 18:10", "Autre", "T2", "Bus B", "Hassan Ouazzani", "Objet oublié à bord"},
    };

    public static final String[][] ACCOUNTS = {
        {"a.elmansouri", "Administrateur", "12/06/2025 • 08:12", "Actif"},
        {"transport.resp", "Responsable", "12/06/2025 • 07:42", "Actif"},
        {"h.ouazzani", "Chauffeur", "11/06/2025 • 17:58", "Actif"},
        {"m.alaoui", "Chauffeur", "02/06/2025 • 06:51", "Inactif"},
    };
}
