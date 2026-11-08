package com.transportpro;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class RegistrationsPage {
    private RegistrationsPage() {}

    public static Node build(Ctx ctx) {
        // --- résumé du trajet sélectionné
        VBox info = new VBox(4,
                Ui.label("TRAJET SÉLECTIONNÉ", "eyebrow"),
                Ui.label("T1 • Taroudant Centre → Zone industrielle", "summary-h2"),
                Ui.label("12/06/2025 à 07:00 • Bus B", "muted"));

        StackPane track = new StackPane();
        track.getStyleClass().add("cap-track");
        Ui.fixHeight(track, 8);
        track.setMinWidth(230);
        track.setPrefWidth(230);
        track.setMaxWidth(230);
        Region fill = new Region();
        fill.getStyleClass().add("cap-fill");
        fill.setMinSize(161, 8);
        fill.setPrefSize(161, 8);
        fill.setMaxSize(161, 8); // 70 % de 230
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        VBox capacity = new VBox(6, Ui.label("35 / 50 places", "cap-text"), track, Ui.label("15 places restantes", "muted"));

        HBox summary = new HBox(30, info, Ui.spacer(), capacity,
                Ui.button("Inscrire un employé", "primary", "plus", () -> ctx.openModal("Inscrire un employé")));
        summary.setAlignment(Pos.CENTER_LEFT);
        VBox summaryCard = Ui.card(summary);
        summaryCard.getStyleClass().add("trip-summary");

        // --- liste des inscrits
        String[][] rows = new String[5][];
        for (int i = 0; i < 5; i++) {
            String[] e = Data.EMPLOYEES[i];
            rows[i] = new String[]{e[1], i < 3 ? "11/06/2025" : "12/06/2025", e[3], i == 0 ? "Présent" : "Non renseigné"};
        }
        HBox head = new HBox(Ui.label("Employés inscrits", "card-h3"), Ui.spacer(), Ui.search("Rechercher un employé", 210));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox listCard = Ui.card(head,
                Ui.table(new String[]{"Employé", "Date d’inscription", "Point d’embarquement", "Présence"}, rows, 3,
                        () -> ctx.openModal("Détail — Inscriptions")));
        listCard.getStyleClass().add("main-card");
        head.setSpacing(10);
        VBox.setMargin(head, new javafx.geometry.Insets(0, 0, 14, 0));

        return Ui.page(summaryCard, listCard);
    }
}
