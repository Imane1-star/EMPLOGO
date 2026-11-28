package com.emplogo.view;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Supplier;

import com.emplogo.dao.Option;

/** Éléments communs aux fenêtres d'ajout / modification (EmployeDialog, BusDialog...). */
final class DialogKit {

    private DialogKit() {}

    /** Fenêtre avec les boutons « Enregistrer » (ou saveLabel) et « Annuler », et la feuille de style. */
    static <T> Dialog<T> dialog(String title, String saveLabel) {
        Dialog<T> d = new Dialog<>();
        d.setTitle(title);
        d.setHeaderText(null);
        var css = DialogKit.class.getResource("/styles.css");
        if (css != null) d.getDialogPane().getStylesheets().add(css.toExternalForm());
        d.getDialogPane().getButtonTypes().addAll(
                new ButtonType(saveLabel, ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        return d;
    }

    static ButtonType saveType(Dialog<?> d) {
        return d.getDialogPane().getButtonTypes().get(0);
    }

    /** Grille à deux colonnes égales. */
    static GridPane grid(double width) {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        g.setPadding(new Insets(18));
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            g.getColumnConstraints().add(c);
        }
        g.setPrefWidth(width);
        return g;
    }

    static Label errorLabel() {
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);
        return error;
    }

    /** Empêche la fermeture tant que validation() retourne un message (affiché dans error). */
    static void guard(Dialog<?> d, Label error, Supplier<String> validation) {
        Button save = (Button) d.getDialogPane().lookupButton(saveType(d));
        save.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = validation.get();
            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });
    }

    static void content(Dialog<?> d, Node n) {
        d.getDialogPane().setContent(n);
    }

    static ComboBox<Option> optionBox(List<Option> items, int selectedId) {
        ComboBox<Option> box = new ComboBox<>();
        box.getItems().addAll(items);
        box.setMaxWidth(Double.MAX_VALUE);
        items.stream().filter(o -> o.id() == selectedId).findFirst().ifPresent(box::setValue);
        return box;
    }

    static int idOf(ComboBox<Option> box) {
        return box.getValue() == null ? 0 : box.getValue().id();
    }

    static String nz(String s) { return s == null ? "" : s; }

    static String text(TextField f) { return f.getText() == null ? "" : f.getText().trim(); }

    /** Lit « HH:mm » (ou « H:mm ») ; null si vide ou invalide. */
    static LocalTime time(TextField f) {
        String s = text(f);
        if (s.isEmpty()) return null;
        try {
            return LocalTime.parse(s.length() == 4 && s.charAt(1) == ':' ? "0" + s : s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Message si l'heure est saisie mais mal écrite (un champ vide est contrôlé par validate()). */
    static String timeError(TextField f, String what) {
        return time(f) == null && !text(f).isEmpty()
                ? "L'heure " + what + " doit avoir le format HH:mm (exemple : 07:00)." : null;
    }
}
