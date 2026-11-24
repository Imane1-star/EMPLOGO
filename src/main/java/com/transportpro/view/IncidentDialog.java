package com.transportpro.view;

import com.transportpro.controller.MainController;
import com.transportpro.dao.Option;
import com.transportpro.model.Incident;
import com.transportpro.model.enums.TypeIncident;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Fenêtre « Enregistrer / Modifier un incident » (BF20).
 * En mode chauffeur (BF21) : statut OUVERT et mode « Signalement chauffeur » imposés ; seuls le trajet,
 * le type, la gravité, la date et la description sont demandés.
 */
public final class IncidentDialog {

    private IncidentDialog() {}

    /** Valeur d'ENUM de la base affichée en français. */
    private record Code(String code, String label) {
        @Override
        public String toString() { return label; }
    }

    public static Optional<Incident> showAdd(MainController controller, boolean driverMode) {
        return show(controller, null, driverMode);
    }

    public static Optional<Incident> showEdit(MainController controller, Incident existing) {
        return show(controller, existing, false);
    }

    private static Optional<Incident> show(MainController controller, Incident existing, boolean driverMode) {
        boolean edit = existing != null;
        List<Option> trajets = controller.trajetsPourIncident(edit ? existing.getIdAffectation() : 0);
        List<Code> gravites = codes(controller.gravitesIncident(), Incident::graviteLabel);
        List<Code> statuts = codes(controller.statutsIncident(), Incident::statutLabel);
        List<Code> modes = codes(controller.modesDeclaration(), Incident::modeLabel);

        Dialog<Incident> d = DialogKit.dialog(edit ? "Modifier l'incident INC" + existing.getIdIncident()
                : driverMode ? "Signaler un incident" : "Enregistrer un incident", driverMode ? "Signaler" : "Enregistrer");

        ComboBox<Option> trajet = DialogKit.optionBox(trajets, edit ? existing.getIdAffectation() : 0);
        trajet.setPromptText("Choisir le trajet concerné");
        ComboBox<TypeIncident> type = new ComboBox<>();
        type.getItems().addAll(TypeIncident.values());
        type.setMaxWidth(Double.MAX_VALUE);
        type.setValue(edit && existing.getType() != null ? existing.getType() : TypeIncident.RETARD);
        ComboBox<Code> gravite = combo(gravites, edit ? existing.getGravite() : "MINEURE");
        ComboBox<Code> statut = combo(statuts, edit ? existing.getStatut() : "OUVERT");
        ComboBox<Code> mode = combo(modes, edit ? existing.getModeDeclaration()
                : driverMode ? "SIGNALEMENT_CHAUFFEUR" : "APPLICATION");

        LocalDateTime when = edit && existing.getDateHeure() != null
                ? existing.getDateHeure() : LocalDateTime.now().withSecond(0).withNano(0);
        DatePicker date = new DatePicker(when.toLocalDate());
        date.setPromptText("jj/mm/aaaa");
        TextField heure = new TextField(when.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        heure.setPromptText("07:40");
        TextArea description = new TextArea(edit ? existing.getDescription() : "");
        description.setPromptText("Décrivez précisément l’incident...");
        description.setPrefRowCount(4);
        description.setWrapText(true);

        Label error = DialogKit.errorLabel();
        if (trajets.isEmpty()) error.setText("Aucun trajet n'existe dans la base : créez d'abord un trajet.");

        GridPane g = DialogKit.grid(560);
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
        DialogKit.content(d, g);

        java.util.function.Supplier<Incident> build = () -> {
            LocalDate day = date.getValue();
            LocalTime t = DialogKit.time(heure);
            Incident i = new Incident(edit ? existing.getIdIncident() : 0, type.getValue(),
                    description.getText() == null ? "" : description.getText().trim(),
                    day == null || t == null ? null : LocalDateTime.of(day, t));
            if (edit) i.setIdCompte(existing.getIdCompte());   // le déclarant d'origine est conservé
            i.setIdAffectation(DialogKit.idOf(trajet));
            i.setGravite(code(gravite));
            i.setStatut(code(statut));
            i.setModeDeclaration(code(mode));
            return i;
        };
        DialogKit.guard(d, error, () -> {
            if (DialogKit.time(heure) == null) return "L'heure doit avoir le format HH:mm (exemple : 07:40).";
            if (date.getValue() == null) return "La date est obligatoire.";
            return build.get().validate();
        });
        d.setResultConverter(bt -> bt == DialogKit.saveType(d) ? build.get() : null);
        return d.showAndWait();
    }

    private static List<Code> codes(List<String> values, Function<String, String> label) {
        List<Code> list = new ArrayList<>();
        for (String v : values) list.add(new Code(v, label.apply(v)));
        return list;
    }

    private static ComboBox<Code> combo(List<Code> items, String selected) {
        ComboBox<Code> c = new ComboBox<>();
        c.getItems().addAll(items);
        c.setMaxWidth(Double.MAX_VALUE);
        items.stream().filter(x -> x.code().equals(selected)).findFirst()
                .ifPresentOrElse(c::setValue, () -> { if (!items.isEmpty()) c.setValue(items.get(0)); });
        return c;
    }

    private static String code(ComboBox<Code> c) {
        return c.getValue() == null ? null : c.getValue().code();
    }
}
