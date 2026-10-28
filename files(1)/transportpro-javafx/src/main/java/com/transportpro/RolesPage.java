package com.transportpro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Matrice des autorisations : module × rôle × (Voir / Créer / Modifier / Supprimer). */
public final class RolesPage {
    private RolesPage() {}

    public static Node build(Ctx ctx) {
        String[] modules = {"Tableau de bord", "Employés", "Bus et chauffeurs", "Trajets", "Inscriptions",
                "Présences", "Incidents", "Rapports", "Comptes"};
        String[] roles = {"Administrateur", "Responsable", "Chauffeur"};
        String[] actions = {"Voir", "Créer", "Modifier", "Supprimer"};

        GridPane grid = new GridPane();
        double[] widths = {31, 23, 23, 23};
        for (double w : widths) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(w);
            grid.getColumnConstraints().add(cc);
        }

        grid.add(cell(Ui.label("MODULE", "th"), "perm-head"), 0, 0);
        for (int r = 0; r < roles.length; r++) {
            grid.add(cell(Ui.label(roles[r].toUpperCase(), "th"), "perm-head"), r + 1, 0);
        }

        for (int i = 0; i < modules.length; i++) {
            grid.add(cell(Ui.label(modules[i], "person-name"), "perm-row"), 0, i + 1);
            for (int r = 0; r < roles.length; r++) {
                HBox boxes = new HBox(12);
                boxes.setAlignment(Pos.CENTER_LEFT);
                for (int j = 0; j < actions.length; j++) {
                    boolean on = r == 0
                            || (r == 1 && i < 8)
                            || (r == 2 && ((i == 3 && j == 0) || (i == 6 && j < 2)));
                    CheckBox cb = new CheckBox(actions[j].substring(0, 1));
                    cb.setSelected(on);
                    Tooltip.install(cb, new Tooltip(actions[j]));
                    boxes.getChildren().add(cb);
                }
                grid.add(cell(boxes, "perm-row"), r + 1, i + 1);
            }
        }

        HBox head = new HBox(
                new VBox(2, Ui.label("Matrice des autorisations", "card-h3"),
                        Ui.label("Définissez les droits pour chaque rôle utilisateur.", "card-sub")),
                Ui.spacer(),
                Ui.button("Enregistrer les droits", "primary", null, () -> ctx.toast("Droits enregistrés")));
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(0, 0, 14, 0));

        VBox card = Ui.card(head, grid);
        card.getStyleClass().add("main-card");
        return Ui.page(card);
    }

    private static HBox cell(Node content, String styleClass) {
        HBox h = new HBox(content);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add(styleClass);
        return h;
    }
}
