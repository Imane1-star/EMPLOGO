package com.transportpro;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/** Fenêtre de saisie Ajouter / Modifier un bus (AppShell n'est pas modifié). */
public final class BusDialog {

    private BusDialog() {}

    public static Optional<Bus> showAdd() {
        return show(null);
    }

    public static Optional<Bus> showEdit(Bus existing) {
        return show(existing);
    }

    private static Optional<Bus> show(Bus existing) {
        boolean edit = existing != null;

        Dialog<Bus> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier un bus" : "Ajouter un bus");
        dialog.setHeaderText(null);

        // Même feuille de style que le reste de l'application
        var css = BusDialog.class.getResource("/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField immat = new TextField();
        immat.setPromptText("12345-A-45");
        TextField marque = new TextField();
        TextField modele = new TextField();
        TextField capacite = new TextField();
        capacite.setPromptText("40");
        ComboBox<String> statut = new ComboBox<>();
        statut.getItems().addAll(Bus.STATUTS);
        statut.setValue("DISPONIBLE");

        if (edit) {
            immat.setText(existing.getImmatriculation());
            marque.setText(existing.getMarque() == null ? "" : existing.getMarque());
            modele.setText(existing.getModele() == null ? "" : existing.getModele());
            capacite.setText(String.valueOf(existing.getCapacite()));
            statut.setValue(existing.getStatut());
        }

        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        g.setPadding(new Insets(18));
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);
        g.add(Ui.field("Immatriculation *", immat), 0, 0);
        g.add(Ui.field("Capacité *", capacite), 1, 0);
        g.add(Ui.field("Marque", marque), 0, 1);
        g.add(Ui.field("Modèle", modele), 1, 1);
        g.add(Ui.field("Statut *", statut), 0, 2);
        g.add(error, 0, 3, 2, 1);
        g.setPrefWidth(480);
        dialog.getDialogPane().setContent(g);

        // On empêche la fermeture tant que la saisie est invalide
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            Built r = build(existing, immat, marque, modele, capacite, statut);
            String err = r.parseError != null ? r.parseError : r.bus.validate();
            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });

        dialog.setResultConverter(bt ->
                bt == saveType ? build(existing, immat, marque, modele, capacite, statut).bus : null);

        return dialog.showAndWait();
    }

    private static final class Built {
        final Bus bus = new Bus();
        String parseError;
    }

    private static Built build(Bus existing, TextField immat, TextField marque,
                               TextField modele, TextField capacite, ComboBox<String> statut) {
        Built r = new Built();
        if (existing != null) r.bus.setIdBus(existing.getIdBus());
        r.bus.setImmatriculation(immat.getText() == null ? "" : immat.getText().trim());
        r.bus.setMarque(marque.getText() == null ? "" : marque.getText().trim());
        r.bus.setModele(modele.getText() == null ? "" : modele.getText().trim());
        r.bus.setStatut(statut.getValue());
        try {
            r.bus.setCapacite(Integer.parseInt(capacite.getText().trim()));
        } catch (NumberFormatException e) {
            r.parseError = "La capacité doit être un nombre entier.";
        }
        return r;
    }
}
