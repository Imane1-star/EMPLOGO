package com.transportpro;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** « Mon planning » : vue du chauffeur. */
public final class PlanningPage {
    private PlanningPage() {}

    public static Node build(Ctx ctx) {
        HBox hero = new HBox(
                new VBox(4,
                        Ui.label("Bonjour Hassan,", "hero-small"),
                        Ui.label("Votre planning du jeudi 12 juin", "hero-h2"),
                        Ui.label("2 trajets prévus aujourd’hui • Bus B", "hero-small")),
                Ui.spacer(),
                Ui.button("Signaler un incident", "soft", null, () -> ctx.openModal("Signaler un incident")));
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("driver-hero");

        String[][] routes = {
            {"06:30", "07:00", "Zone industrielle", "35 employés"},
            {"17:00", "17:30", "Taroudant Centre", "42 employés"},
        };
        GridPane grid = new GridPane();
        grid.setHgap(14);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().add(cc);
            grid.add(routeCard(i, routes[i]), i, 0);
        }
        return Ui.page(hero, grid);
    }

    private static VBox routeCard(int index, String[] x) {
        HBox top = new HBox(
                new VBox(3, Ui.label(index == 0 ? "TRAJET ALLER" : "TRAJET RETOUR", "eyebrow"), Ui.label(x[2], "card-h3")),
                Ui.spacer(), Ui.badge("Planifié"));
        top.setAlignment(Pos.TOP_LEFT);

        VBox meta = new VBox(3, Ui.label(x[1], "route-time"), Ui.label("Bus B • 23456-B-45", "muted"), Ui.label(x[3], "muted"));

        VBox stops = new VBox(10);
        String[] names = {"Oulad Teima", "Ait Iaazza", "Taroudant Centre"};
        String[] times = {x[0], "06:40", "06:50"};
        for (int j = 0; j < names.length; j++) {
            Circle dot = new Circle(4.5);
            dot.getStyleClass().add("stop-dot");
            HBox s = new HBox(10, dot, Ui.label(times[j], "stop-time"), Ui.label(names[j], "cell-text"));
            s.setAlignment(Pos.CENTER_LEFT);
            stops.getChildren().add(s);
        }

        VBox card = Ui.card(top, meta, stops, Ui.button("Voir la liste des passagers", "secondary", null, null));
        card.setSpacing(14);
        card.getStyleClass().add("route-card");
        return card;
    }
}
