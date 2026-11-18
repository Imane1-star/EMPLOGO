package com.transportpro.incident;

import com.transportpro.ui.Ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Fenêtre « Enregistrer / Modifier un incident » (BF20).
 * En mode chauffeur (BF21), le statut est fixé à « Ouvert » et le mode de déclaration à
 * « Signalement chauffeur » (ou « Application ») : seuls le trajet, le type, la gravité,
 * la date et la description sont demandés.
 */
public final class IncidentDialog {

    private IncidentDialog() {}

    /** Valeur d'ENUM affichée en français, code de la base conservé. */
    private record Code(String code, String label) {
        @Override
        public String toString() { return label; }
    }

    public static Optional<Incident> showAdd(boolean driverMode) {
        return show(null, driverMode);
    }

    public static Optional<Incident> showEdit(Incident existing) {
        return show(existing, false);
    }

    private static Optional<Incident> show(Incident existing, boolean driverMode) {
        boolean edit = existing != null;

        List<IncidentDAO.Option> trajets;
        List<Code> types, gravites, statuts, modes;
        try {
            IncidentDAO dao = new IncidentDAO();
            trajets = dao.findAffectations(edit ? existing.getIdAffectation() : 0);
            types = codes(dao.findTypes(), Incident::typeLabel);
            gravites = codes(dao.findGravites(), Incident::graviteLabel);
            statuts = codes(dao.findStatuts(), Incident::statutLabel);
            modes = codes(dao.findModes(), Incident::modeLabel);
        } catch (IncidentDAO.IncidentException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return Optional.empty();
        }

        Dialog<Incident> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier l'incident INC" + existing.getIdIncident()
                : driverMode ? "Signaler un incident" : "Enregistrer un incident");
        dialog.setHeaderText(null);
        var css = IncidentDialog.class.getResource("/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        ButtonType saveType = new ButtonType(driverMode ? "Signaler" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<IncidentDAO.Option> trajet = new ComboBox<>();
        trajet.getItems().addAll(trajets);
        trajet.setPromptText("Choisir le trajet concerné");
        trajet.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Code> type = combo(types);
        ComboBox<Code> gravite = combo(gravites);
        ComboBox<Code> statut = combo(statuts);
        ComboBox<Code> mode = combo(modes);

        LocalDateTime when = edit && existing.getDateIncident() != null
                ? existing.getDateIncident() : LocalDateTime.now().withSecond(0).withNano(0);
        DatePicker date = new DatePicker(when.toLocalDate());
        date.setPromptText("jj/mm/aaaa");
        TextField heure = new TextField(when.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        heure.setPromptText("07:40");
        TextArea description = new TextArea(edit ? existing.getDescription() : "");
        description.setPromptText("Décrivez précisément l’incident...");
        description.setPrefRowCount(4);
        description.setWrapText(true);

        if (edit) {
            trajets.stream().filter(o -> o.id() == existing.getIdAffectation()).findFirst().ifPresent(trajet::setValue);
            select(type, existing.getTypeIncident());
            select(gravite, existing.getGravite());
            select(statut, existing.getStatutTraitement());
            select(mode, existing.getModeDeclaration());
        } else {
            select(type, "RETARD");
            select(gravite, "FAIBLE");
            select(statut, "OUVERT");
            if (driverMode && !select(mode, "SIGNALEMENT_CHAUFFEUR")) select(mode, "APPLICATION");
            else if (!driverMode) select(mode, "APPLICATION");
        }

        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);
        if (trajets.isEmpty()) error.setText("Aucun trajet n'existe dans la base : créez d'abord un trajet.");

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        g.setPadding(new Insets(18));
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);
        int row = 0;
        g.add(Ui.field("Trajet concerné *", trajet), 0, row++, 2, 1);
        g.add(Ui.field("Type *", type), 0, row);
        g.add(Ui.field("Gravité *", gravite), 1, row++);
        g.add(Ui.field("Date *", date), 0, row);
        g.add(Ui.field("Heure *", heure), 1, row++);
        if (!driverMode) {
            g.add(Ui.field("Statut du traitement *", statut), 0, row);
            g.add(Ui.field("Mode de déclaration *", mode), 1, row++);
        }
        g.add(Ui.field("Description *", description), 0, row++, 2, 1);
        g.add(error, 0, row, 2, 1);
        g.setPrefWidth(560);
        dialog.getDialogPane().setContent(g);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = parseTime(heure) == null ? "L'heure doit avoir le format HH:mm (exemple : 07:40)." : null;
            if (err == null && date.getValue() == null) err = "La date est obligatoire.";
            if (err == null) err = build(existing, trajet, type, gravite, date, heure, statut, mode, description).validate();
            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == saveType
                ? build(existing, trajet, type, gravite, date, heure, statut, mode, description) : null);
        return dialog.showAndWait();
    }

    private static List<Code> codes(List<String> values, Function<String, String> label) {
        List<Code> list = new ArrayList<>();
        for (String v : values) list.add(new Code(v, label.apply(v)));
        return list;
    }

    private static ComboBox<Code> combo(List<Code> items) {
        ComboBox<Code> c = new ComboBox<>();
        c.getItems().addAll(items);
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    /** Sélectionne la valeur de code donné ; retourne false si elle n'existe pas dans la base. */
    private static boolean select(ComboBox<Code> box, String code) {
        for (Code c : box.getItems()) {
            if (c.code().equals(code)) {
                box.setValue(c);
                return true;
            }
        }
        if (box.getValue() == null && !box.getItems().isEmpty()) box.setValue(box.getItems().get(0));
        return false;
    }

    private static LocalTime parseTime(TextField f) {
        String s = f.getText() == null ? "" : f.getText().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalTime.parse(s.length() == 4 && s.charAt(1) == ':' ? "0" + s : s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Incident build(Incident existing, ComboBox<IncidentDAO.Option> trajet,
                                  ComboBox<Code> type, ComboBox<Code> gravite, DatePicker date,
                                  TextField heure, ComboBox<Code> statut, ComboBox<Code> mode,
                                  TextArea description) {
        Incident i = new Incident();
        if (existing != null) {
            i.setIdIncident(existing.getIdIncident());
            i.setIdCompte(existing.getIdCompte());   // le déclarant d'origine est conservé
        }
        i.setIdAffectation(trajet.getValue() == null ? 0 : trajet.getValue().id());
        i.setTypeIncident(type.getValue() == null ? null : type.getValue().code());
        i.setGravite(gravite.getValue() == null ? null : gravite.getValue().code());
        LocalDate d = date.getValue();
        LocalTime t = parseTime(heure);
        i.setDateIncident(d == null || t == null ? null : LocalDateTime.of(d, t));
        i.setStatutTraitement(statut.getValue() == null ? null : statut.getValue().code());
        i.setModeDeclaration(mode.getValue() == null ? null : mode.getValue().code());
        i.setDescription(description.getText() == null ? "" : description.getText().trim());
        return i;
    }
}
