package com.transportpro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Affectation d'un bus et d'un chauffeur à un trajet, avec vérifications automatiques. */
public final class AssignmentsPage {

    private final VBox root = Ui.page();
    private final Ctx ctx;
    private String bus = "A";

    public static Node build(Ctx ctx) {
        return new AssignmentsPage(ctx).root;
    }

    private AssignmentsPage(Ctx ctx) {
        this.ctx = ctx;
        render();
    }

    private void render() {
        boolean ok = bus.equals("B");

        // --- bandeau résultat
        HBox banner = new HBox(12,
                Ui.label(ok ? "Affectation acceptée" : "Affectation refusée", "banner-title"),
                Ui.label(ok ? "Toutes les vérifications sont validées. Vous pouvez confirmer."
                            : "Capacité du bus (30) inférieure au nombre d’inscrits (35).", "banner-text"));
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().addAll("assign-banner", ok ? "ok" : "error");

        // --- 1) trajet
        VBox step1 = stepCard("1", "Sélectionner le trajet", "Trajets à affecter",
                tripChoice("T1", "Zone industrielle", "12/06/2025 • 07:00", "35 inscrits", true),
                tripChoice("T3", "Oulad Teima", "12/06/2025 • 08:15", "22 inscrits", false));

        // --- 2) bus
        String[][] buses = {{"A", "30 places", "Disponible"}, {"B", "50 places", "Disponible"}, {"C", "40 places", "En maintenance"}};
        VBox step2 = stepCard("2", "Choisir un bus", "Capacité et disponibilité");
        for (String[] b : buses) {
            step2.getChildren().add(option("Bus " + b[0], b[1] + " • 12345-" + b[0] + "-45", b[2],
                    bus.equals(b[0]), b[0].equals("C"), () -> {
                        bus = b[0];
                        render();
                    }));
        }

        // --- 3) chauffeur
        VBox step3 = stepCard("3", "Choisir un chauffeur", "Disponibles sur ce créneau");
        for (int i = 0; i < 2; i++) {
            String[] d = Data.DRIVERS[i];
            step3.getChildren().add(option(d[0], d[2], d[3], i == 0, false, null));
        }

        GridPane steps = new GridPane();
        steps.setHgap(12);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.3);
            steps.getColumnConstraints().add(cc);
        }
        steps.add(step1, 0, 0);
        steps.add(step2, 1, 0);
        steps.add(step3, 2, 0);

        // --- vérifications automatiques
        FlowPane checks = new FlowPane(24, 8);
        Object[][] rules = {
            {"Bus disponible à cette date et heure", true},
            {"Chauffeur disponible", true},
            {"Capacité suffisante (capacité ≥ inscrits)", ok},
            {"Aucun chevauchement d’horaire", true},
        };
        for (Object[] r : rules) {
            boolean pass = (Boolean) r[1];
            HBox line = new HBox(6, Ui.label(pass ? "✓" : "×", pass ? "check-pass" : "check-fail"), Ui.label((String) r[0], "check-text"));
            line.setAlignment(Pos.CENTER_LEFT);
            checks.getChildren().add(line);
        }
        Button confirm = Ui.button("Confirmer l’affectation", "primary", null, () -> ctx.toast("Affectation confirmée"));
        confirm.setDisable(!ok);
        VBox checksCard = Ui.card(Ui.label("Vérifications automatiques", "card-h3"), checks, confirm);
        checksCard.setSpacing(12);
        checksCard.setPadding(new Insets(16));

        root.getChildren().setAll(banner, steps, checksCard);
    }

    private VBox stepCard(String number, String title, String subtitle, Node... extra) {
        HBox head = new HBox(10, Ui.label(number, "step"), Ui.cardTitle(title, subtitle, null, null));
        head.setAlignment(Pos.TOP_LEFT);
        VBox card = Ui.card(head);
        card.setSpacing(8);
        card.setPadding(new Insets(16));
        card.getChildren().addAll(extra);
        return card;
    }

    private HBox tripChoice(String id, String dest, String when, String count, boolean active) {
        HBox h = new HBox(12, Ui.label(id, "trip-id"),
                new VBox(2, Ui.label(dest, "opt-title"), Ui.label(when, "opt-sub")),
                Ui.spacer(), Ui.label(count, "trip-count"));
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("trip-choice");
        if (active) h.getStyleClass().add("active");
        return h;
    }

    private Button option(String title, String sub, String badgeText, boolean selected, boolean disabled, Runnable onClick) {
        Circle radio = new Circle(7);
        radio.getStyleClass().add(selected ? "radio-on" : "radio-off");
        HBox row = new HBox(10, radio, new VBox(2, Ui.label(title, "opt-title"), Ui.label(sub, "opt-sub")),
                Ui.spacer(), Ui.badge(badgeText));
        row.setAlignment(Pos.CENTER_LEFT);

        Button b = new Button();
        b.setGraphic(row);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("option-row");
        if (selected) b.getStyleClass().add("selected");
        row.prefWidthProperty().bind(b.widthProperty().subtract(30));
        b.setDisable(disabled);
        if (onClick != null) b.setOnAction(e -> onClick.run());
        return b;
    }
}
