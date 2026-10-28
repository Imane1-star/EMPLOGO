package com.transportpro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AttendancePage {
    private AttendancePage() {}

    public static Node build(Ctx ctx) {
        List<ToggleGroup> groups = new ArrayList<>();
        List<ToggleButton> presentButtons = new ArrayList<>();
        Label counter = Ui.label("", "muted");
        int total = Data.EMPLOYEES.length;
        Runnable update = () -> {
            long n = groups.stream().filter(g -> g.getSelectedToggle() != null).count();
            counter.setText(n + " sur " + total + " présences renseignées");
        };

        // --- barre du haut
        DatePicker date = new DatePicker(LocalDate.of(2025, 6, 12));
        HBox filters = new HBox(10,
                Ui.field("Trajet", Ui.combo("T1 • Zone industrielle • 07:00")),
                Ui.field("Date", date));
        filters.setAlignment(Pos.BOTTOM_LEFT);
        HBox top = new HBox(filters, Ui.spacer(),
                Ui.button("Tout marquer présent", "secondary", null, () -> presentButtons.forEach(b -> b.setSelected(true))));
        top.setAlignment(Pos.BOTTOM_CENTER);
        top.setPadding(new Insets(0, 0, 15, 0));

        // --- liste
        VBox list = new VBox();
        list.getChildren().add(row(Ui.label("EMPLOYÉ", "th"), Ui.label("POINT D’EMBARQUEMENT", "th"), Ui.label("STATUT DE PRÉSENCE", "th"), "presence-head"));
        for (String[] e : Data.EMPLOYEES) {
            String[] parts = e[1].split(" ");
            String initials = "" + parts[0].charAt(0) + parts[1].charAt(0);
            HBox person = new HBox(10, Ui.avatar(initials), Ui.label(e[1], "person-name"));
            person.setAlignment(Pos.CENTER_LEFT);

            ToggleGroup g = new ToggleGroup();
            groups.add(g);
            g.selectedToggleProperty().addListener((obs, a, b) -> update.run());
            HBox seg = new HBox(-1);
            for (String s : new String[]{"Présent", "Absent", "Non renseigné"}) {
                ToggleButton tb = new ToggleButton(s);
                tb.setToggleGroup(g);
                tb.getStyleClass().add("seg");
                if (s.equals("Présent")) presentButtons.add(tb);
                seg.getChildren().add(tb);
            }
            list.getChildren().add(row(person, Ui.label(e[3], "cell-text"), seg, "presence-row"));
        }
        update.run();

        // --- barre d'enregistrement
        HBox save = new HBox(counter, Ui.spacer(),
                Ui.button("Enregistrer les présences", "primary", null, () -> ctx.toast("Présences enregistrées")));
        save.setAlignment(Pos.CENTER_LEFT);
        save.getStyleClass().add("save-bar");

        VBox card = Ui.card(top, list, save);
        card.getStyleClass().add("main-card");
        return Ui.page(card);
    }

    private static GridPane row(Node a, Node b, Node c, String styleClass) {
        GridPane g = new GridPane();
        g.getStyleClass().add(styleClass);
        double[] w = {40, 28, 32};
        for (double p : w) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(p);
            g.getColumnConstraints().add(cc);
        }
        g.add(a, 0, 0);
        g.add(b, 1, 0);
        g.add(c, 2, 0);
        GridPane.setValignment(a, javafx.geometry.VPos.CENTER);
        return g;
    }
}
