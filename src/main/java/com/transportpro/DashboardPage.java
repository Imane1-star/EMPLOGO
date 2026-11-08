package com.transportpro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;

public final class DashboardPage {
    private DashboardPage() {}

    public static Node build(Ctx ctx) {
        // --- actions rapides
        HBox quick = new HBox(8, Ui.spacer(),
                Ui.button("Nouveau trajet", "primary", "plus", () -> ctx.openModal("Créer un trajet")),
                Ui.button("Nouvel employé", "secondary", "plus", () -> ctx.openModal("Ajouter un employé")),
                Ui.button("Enregistrer un incident", "soft", null, () -> ctx.openModal("Enregistrer un incident")));

        // --- bandeau « points d'attention »
        HBox alert = new HBox(22,
                Ui.label("Points d’attention", "alert-title"),
                chip("2", " bus en maintenance"),
                chip("1", " trajet sans affectation"),
                chip("2", " trajets presque complets"));
        alert.getStyleClass().add("alert-strip");
        alert.setAlignment(Pos.CENTER_LEFT);

        // --- cartes statistiques
        String[][] stats = {
            {"Trajets aujourd’hui", "12", "+8,2 %", "trips", "TRIPS"},
            {"Employés inscrits", "138", "+4,1 %", "employees", "EMPLOYEES"},
            {"Bus disponibles", "7 / 9", "2 indisponibles", "buses", "BUSES"},
            {"Incidents du mois", "5", "−12,5 %", "incidents", "INCIDENTS"},
        };
        GridPane statGrid = grid(25, 25, 25, 25);
        for (int i = 0; i < stats.length; i++) {
            final Page target = Page.valueOf(stats[i][4]);
            statGrid.add(Ui.statCard(stats[i][0], stats[i][1], stats[i][2], stats[i][3], () -> ctx.navigate(target)), i, 0);
        }

        // --- graphiques
        VBox lineCard = Ui.card(
                Ui.cardTitle("Nombre de trajets par semaine", "6 dernières semaines", null, null),
                Ui.areaChart(new String[]{"S1", "S2", "S3", "S4", "S5", "S6"}, new double[]{12, 15, 14, 21, 19, 27}));
        VBox donutCard = Ui.card(
                Ui.cardTitle("Taux de présence global", "Ce mois", null, null),
                donut(),
                legend());
        VBox barCard = Ui.card(
                Ui.cardTitle("Coût du transport", "En milliers MAD", null, null),
                Ui.barChart(new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Juin"}, new double[]{42, 58, 46, 72, 64, 84}));
        for (VBox c : new VBox[]{lineCard, donutCard, barCard}) {
            c.getStyleClass().add("chart-card");
            Ui.fixHeight(c, 250);
        }
        GridPane charts = grid(41, 24, 35);
        charts.add(lineCard, 0, 0);
        charts.add(donutCard, 1, 0);
        charts.add(barCard, 2, 0);

        // --- trajets du jour + derniers incidents
        String[][] rows = new String[3][];
        for (int i = 0; i < 3; i++) {
            String[] t = Data.TRIPS[i];
            rows[i] = new String[]{t[2], t[3], t[4], t[5], t[6], t[7]};
        }
        VBox tripsCard = Ui.card(
                Ui.cardTitle("Trajets du jour", "12 juin 2025", "Voir tous", () -> ctx.navigate(Page.TRIPS)),
                Ui.table(new String[]{"Heure", "Destination", "Bus", "Chauffeur", "Occupation", "Statut"}, rows, 5,
                        () -> ctx.openModal("Détail — Trajets")));
        VBox incidentsCard = Ui.card(
                Ui.cardTitle("Derniers incidents", "Mise à jour en direct", null, null),
                incidentList(ctx));
        GridPane bottom = grid(66.6, 33.4);
        bottom.add(tripsCard, 0, 0);
        bottom.add(incidentsCard, 1, 0);
        for (VBox c : new VBox[]{tripsCard, incidentsCard}) {
            c.setPadding(new Insets(15));
        }

        return Ui.page(quick, alert, statGrid, charts, bottom);
    }

    private static GridPane grid(double... percents) {
        GridPane g = new GridPane();
        g.setHgap(12);
        for (double p : percents) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(p);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    private static HBox chip(String number, String text) {
        HBox h = new HBox(Ui.label(number, "alert-num"), Ui.label(text, "alert-text"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Node donut() {
        Circle base = new Circle(52, Color.web("#DFB6B2"));
        Arc arc = new Arc(0, 0, 52, 52, 90, -327.6); // 91 % de 360°
        arc.setType(ArcType.ROUND);
        arc.setFill(Color.web("#16A34A"));
        Circle hole = new Circle(36, Color.WHITE);
        VBox text = new VBox(Ui.label("91%", "donut-value"), Ui.label("Présence", "donut-sub"));
        text.setAlignment(Pos.CENTER);
        text.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane s = new StackPane(new Group(base, arc, hole), text);
        s.setPadding(new Insets(4, 0, 8, 0));
        return s;
    }

    private static Node legend() {
        HBox h = new HBox(16, legendItem("#16A34A", "Présents 91%"), legendItem("#DFB6B2", "Absents 9%"));
        h.setAlignment(Pos.CENTER);
        return h;
    }

    private static Node legendItem(String color, String text) {
        HBox h = new HBox(6, new Circle(4, Color.web(color)), Ui.label(text, "muted"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Node incidentList(Ctx ctx) {
        String[][] items = {
            {"Retard", "T2 • 10/06/2025", "Retard de 18 minutes"},
            {"Panne", "Bus C • 08/06/2025", "Problème de démarrage"},
            {"Autre", "T1 • 04/06/2025", "Objet oublié à bord"},
        };
        VBox list = new VBox(8);
        for (String[] it : items) {
            HBox row = new HBox(10, Ui.badge(it[0]),
                    new VBox(2, Ui.label(it[2], "incident-title"), Ui.label(it[1], "muted")),
                    Ui.spacer(), Ui.label("›", "chevron"));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("incident-row");
            row.setOnMouseClicked(e -> ctx.navigate(Page.INCIDENTS));
            list.getChildren().add(row);
        }
        return list;
    }
}
