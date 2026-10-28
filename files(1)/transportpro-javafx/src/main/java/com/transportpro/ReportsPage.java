package com.transportpro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public final class ReportsPage {
    private ReportsPage() {}

    public static Node build(Ctx ctx) {
        // --- filtres
        ToggleGroup g = new ToggleGroup();
        ToggleButton b7 = period("7 jours", g);
        ToggleButton b30 = period("30 jours", g);
        ToggleButton bq = period("Trimestre", g);
        b30.setSelected(true);

        HBox left = new HBox(10,
                Ui.field("Du", new DatePicker(LocalDate.of(2025, 5, 12))),
                Ui.field("Au", new DatePicker(LocalDate.of(2025, 6, 12))),
                new HBox(-1, b7, b30, bq));
        left.setAlignment(Pos.BOTTOM_LEFT);
        HBox filter = new HBox(left, Ui.spacer(),
                Ui.button("Exporter le rapport", "primary", null, () -> ctx.toast("Rapport PDF exporté avec succès")));
        filter.setAlignment(Pos.BOTTOM_CENTER);
        VBox filterCard = Ui.card(filter);
        filterCard.setPadding(new Insets(14));

        // --- indicateurs
        String[][] kpis = {
            {"Nombre de trajets", "248", "+6,4 %"},
            {"Taux de présence moyen", "91,2 %", "+2,1 %"},
            {"Coût total estimé", "42 680 MAD", "−3,8 %"},
            {"Nombre d’incidents", "5", "−12,5 %"},
        };
        GridPane stats = grid(25, 25, 25, 25);
        for (int i = 0; i < kpis.length; i++) {
            stats.add(Ui.statCard(kpis[i][0], kpis[i][1], kpis[i][2], null, null), i, 0);
        }

        // --- graphiques
        VBox c1 = Ui.card(Ui.cardTitle("Trajets par période", "Mai — Juin 2025", null, null),
                Ui.barChart(new String[]{"S1", "S2", "S3", "S4", "S5", "S6"}, new double[]{38, 52, 44, 61, 55, 70}));
        VBox c2 = Ui.card(Ui.cardTitle("Évolution des coûts", "Total mensuel en MAD", null, null),
                Ui.areaChart(new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Juin"}, new double[]{38, 41, 40, 46, 44, 43}));
        for (VBox c : new VBox[]{c1, c2}) {
            c.getStyleClass().add("chart-card");
            Ui.fixHeight(c, 270);
        }
        GridPane charts = grid(50, 50);
        charts.add(c1, 0, 0);
        charts.add(c2, 1, 0);

        return Ui.page(filterCard, stats, charts);
    }

    private static ToggleButton period(String text, ToggleGroup g) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g);
        b.getStyleClass().addAll("seg", "period");
        return b;
    }

    private static GridPane grid(double... percents) {
        GridPane gp = new GridPane();
        gp.setHgap(12);
        for (double p : percents) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(p);
            gp.getColumnConstraints().add(c);
        }
        return gp;
    }
}
