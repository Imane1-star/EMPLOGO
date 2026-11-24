package com.transportpro.view;

import com.transportpro.controller.MainController;
import com.transportpro.dao.Option;
import com.transportpro.model.Affectation;
import com.transportpro.model.enums.StatutTrajet;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

/** Fenêtre « Créer / Modifier un trajet » : une ligne planifiée à une date avec un bus et un chauffeur (BF9, BF12). */
public final class AffectationDialog {

    private AffectationDialog() {}

    public static Optional<Affectation> showAdd(MainController controller) {
        return show(controller, null);
    }

    public static Optional<Affectation> showEdit(MainController controller, Affectation existing) {
        return show(controller, existing);
    }

    private static Optional<Affectation> show(MainController controller, Affectation existing) {
        boolean edit = existing != null;
        List<Option> lignes = controller.lignes();
        List<Option> bus = controller.busDisponibles(edit ? existing.getIdBus() : 0);
        List<Option> chauffeurs = controller.chauffeursDisponibles(edit ? existing.getIdChauffeur() : 0);

        Dialog<Affectation> d = DialogKit.dialog(edit ? "Modifier un trajet" : "Créer un trajet", "Enregistrer");

        ComboBox<Option> ligne = DialogKit.optionBox(lignes, edit ? existing.getIdTrajet() : 0);
        if (!edit && !lignes.isEmpty()) ligne.setValue(lignes.get(0));
        ComboBox<Option> busBox = DialogKit.optionBox(bus, edit ? existing.getIdBus() : 0);
        ComboBox<Option> chauffeurBox = DialogKit.optionBox(chauffeurs, edit ? existing.getIdChauffeur() : 0);
        ComboBox<StatutTrajet> statut = new ComboBox<>();
        statut.getItems().addAll(StatutTrajet.values());
        statut.setMaxWidth(Double.MAX_VALUE);
        StatutTrajet current = edit ? StatutTrajet.fromCode(existing.getStatut()) : null;
        statut.setValue(current != null ? current : StatutTrajet.PLANIFIE);

        DatePicker date = new DatePicker(edit ? existing.getDateTrajet() : null);
        date.setPromptText("jj/mm/aaaa");
        TextField depart = new TextField(edit && existing.getHeureDepart() != null ? existing.getHeureDepart().toString() : "");
        depart.setPromptText("07:00");
        TextField arrivee = new TextField(edit && existing.getHeureArrivee() != null ? existing.getHeureArrivee().toString() : "");
        arrivee.setPromptText("07:45");

        Label error = DialogKit.errorLabel();
        if (lignes.isEmpty()) error.setText("Aucune ligne de transport n'existe dans la base.");
        else if (!edit && bus.isEmpty()) error.setText("Aucun bus disponible pour le moment.");
        else if (!edit && chauffeurs.isEmpty()) error.setText("Aucun chauffeur actif disponible.");

        GridPane g = DialogKit.grid(520);
        g.add(Ui.field("Ligne *", ligne), 0, 0, 2, 1);
        g.add(Ui.field("Date *", date), 0, 1);
        g.add(Ui.field("Statut *", statut), 1, 1);
        g.add(Ui.field("Heure de départ *", depart), 0, 2);
        g.add(Ui.field("Heure d'arrivée *", arrivee), 1, 2);
        g.add(Ui.field("Bus *", busBox), 0, 3);
        g.add(Ui.field("Chauffeur *", chauffeurBox), 1, 3);
        g.add(error, 0, 4, 2, 1);
        DialogKit.content(d, g);

        java.util.function.Supplier<Affectation> build = () -> {
            Affectation a = new Affectation(DialogKit.idOf(ligne), DialogKit.idOf(busBox), DialogKit.idOf(chauffeurBox),
                    date.getValue(), DialogKit.time(depart), DialogKit.time(arrivee),
                    statut.getValue() == null ? null : statut.getValue().code());
            if (edit) a.setIdAffectation(existing.getIdAffectation());
            return a;
        };
        DialogKit.guard(d, error, () -> {
            String err = DialogKit.timeError(depart, "de départ");
            if (err == null) err = DialogKit.timeError(arrivee, "d'arrivée");
            return err != null ? err : build.get().validate();
        });
        d.setResultConverter(bt -> bt == DialogKit.saveType(d) ? build.get() : null);
        return d.showAndWait();
    }
}
