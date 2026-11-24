package com.transportpro.view;

import com.transportpro.model.Bus;
import com.transportpro.model.enums.StatutBus;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

import static com.transportpro.view.DialogKit.nz;
import static com.transportpro.view.DialogKit.text;

/** Fenêtre « Ajouter / Modifier un bus » (BF5, BF6). */
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
        Dialog<Bus> d = DialogKit.dialog(edit ? "Modifier un bus" : "Ajouter un bus", "Enregistrer");

        TextField immat = new TextField(edit ? nz(existing.getImmatriculation()) : "");
        immat.setPromptText("12345-A-45");
        TextField marque = new TextField(edit ? nz(existing.getMarque()) : "");
        TextField modele = new TextField(edit ? nz(existing.getModele()) : "");
        TextField capacite = new TextField(edit ? String.valueOf(existing.getCapacite()) : "");
        capacite.setPromptText("40");
        ComboBox<StatutBus> statut = new ComboBox<>();
        statut.getItems().addAll(StatutBus.values());
        statut.setMaxWidth(Double.MAX_VALUE);
        statut.setValue(edit && existing.getStatut() != null ? existing.getStatut() : StatutBus.DISPONIBLE);

        Label error = DialogKit.errorLabel();
        GridPane g = DialogKit.grid(480);
        g.add(Ui.field("Immatriculation *", immat), 0, 0);
        g.add(Ui.field("Capacité *", capacite), 1, 0);
        g.add(Ui.field("Marque", marque), 0, 1);
        g.add(Ui.field("Modèle", modele), 1, 1);
        g.add(Ui.field("Statut *", statut), 0, 2);
        g.add(error, 0, 3, 2, 1);
        DialogKit.content(d, g);

        java.util.function.Supplier<Bus> build = () -> {
            int cap;
            try {
                cap = Integer.parseInt(text(capacite));
            } catch (NumberFormatException e) {
                cap = -1;
            }
            return new Bus(edit ? existing.getIdBus() : 0, text(immat), text(marque), text(modele), cap,
                    statut.getValue());
        };
        DialogKit.guard(d, error, () -> {
            if (!text(capacite).matches("\\d+")) return "La capacité doit être un nombre entier.";
            return build.get().validate();
        });
        d.setResultConverter(bt -> bt == DialogKit.saveType(d) ? build.get() : null);
        return d.showAndWait();
    }
}
