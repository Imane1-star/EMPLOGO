package com.transportpro;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Fenêtre « Créer / Modifier un trajet » (même présentation que la fenêtre Chauffeurs). */
public final class TrajetDialog {

    private TrajetDialog() {}

    /** Statut affiché en français, code de la base conservé. */
    private record StatutOption(String code, String label) {
        @Override
        public String toString() { return label; }
    }

    public static Optional<Trajet> showAdd() {
        return show(null);
    }

    public static Optional<Trajet> showEdit(Trajet existing) {
        return show(existing);
    }

    private static Optional<Trajet> show(Trajet existing) {
        boolean edit = existing != null;

        // Listes lues dans la base (lignes, bus disponibles, chauffeurs actifs, statuts)
        List<TrajetDAO.Option> lignes;
        List<TrajetDAO.Option> bus;
        List<TrajetDAO.Option> chauffeurs;
        List<StatutOption> statuts = new ArrayList<>();
        try {
            TrajetDAO dao = new TrajetDAO();
            lignes = dao.findLignes();
            bus = dao.findAvailableBuses(edit ? existing.getIdBus() : 0);
            chauffeurs = dao.findAvailableDrivers(edit ? existing.getIdChauffeur() : 0);
            for (String s : dao.findStatuts()) statuts.add(new StatutOption(s, Trajet.statutLabel(s)));
        } catch (TrajetDAO.TrajetException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return Optional.empty();
        }

        Dialog<Trajet> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier un trajet" : "Créer un trajet");
        dialog.setHeaderText(null);

        var css = TrajetDialog.class.getResource("/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<TrajetDAO.Option> ligne = new ComboBox<>();
        ligne.getItems().addAll(lignes);
        ComboBox<TrajetDAO.Option> busBox = new ComboBox<>();
        busBox.getItems().addAll(bus);
        ComboBox<TrajetDAO.Option> chauffeurBox = new ComboBox<>();
        chauffeurBox.getItems().addAll(chauffeurs);
        ComboBox<StatutOption> statut = new ComboBox<>();
        statut.getItems().addAll(statuts);

        DatePicker date = new DatePicker(edit ? existing.getDateTrajet() : null);
        date.setPromptText("jj/mm/aaaa");
        TextField depart = new TextField(edit && existing.getHeureDepart() != null ? existing.getHeureDepart().toString() : "");
        depart.setPromptText("07:00");
        TextField arrivee = new TextField(edit && existing.getHeureArrivee() != null ? existing.getHeureArrivee().toString() : "");
        arrivee.setPromptText("07:45");

        if (edit) {
            select(ligne, existing.getIdTrajet());
            select(busBox, existing.getIdBus());
            select(chauffeurBox, existing.getIdChauffeur());
            statuts.stream().filter(s -> s.code().equals(existing.getStatut()))
                    .findFirst().ifPresent(statut::setValue);
        } else {
            if (!lignes.isEmpty()) ligne.setValue(lignes.get(0));
            statuts.stream().filter(s -> s.code().equals("PLANIFIEE"))
                    .findFirst().ifPresentOrElse(statut::setValue,
                            () -> { if (!statuts.isEmpty()) statut.setValue(statuts.get(0)); });
        }

        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);
        if (lignes.isEmpty()) error.setText("Aucune ligne de transport n'existe dans la base.");
        else if (!edit && bus.isEmpty()) error.setText("Aucun bus disponible pour le moment.");
        else if (!edit && chauffeurs.isEmpty()) error.setText("Aucun chauffeur actif disponible.");

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        g.setPadding(new Insets(18));
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);
        g.add(Ui.field("Ligne *", ligne), 0, 0, 2, 1);
        g.add(Ui.field("Date *", date), 0, 1);
        g.add(Ui.field("Statut *", statut), 1, 1);
        g.add(Ui.field("Heure de départ *", depart), 0, 2);
        g.add(Ui.field("Heure d'arrivée *", arrivee), 1, 2);
        g.add(Ui.field("Bus *", busBox), 0, 3);
        g.add(Ui.field("Chauffeur *", chauffeurBox), 1, 3);
        g.add(error, 0, 4, 2, 1);
        g.setPrefWidth(520);
        dialog.getDialogPane().setContent(g);

        // On empêche la fermeture tant que la saisie est invalide
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = timeError(depart, arrivee);
            if (err == null) err = build(existing, ligne, date, depart, arrivee, busBox, chauffeurBox, statut).validate();
            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });

        dialog.setResultConverter(bt ->
                bt == saveType ? build(existing, ligne, date, depart, arrivee, busBox, chauffeurBox, statut) : null);

        return dialog.showAndWait();
    }

    private static void select(ComboBox<TrajetDAO.Option> box, int id) {
        box.getItems().stream().filter(o -> o.id() == id).findFirst().ifPresent(box::setValue);
    }

    /** Lit « HH:mm » ; retourne null si le champ est vide ou invalide. */
    private static LocalTime parseTime(TextField f) {
        String s = f.getText() == null ? "" : f.getText().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalTime.parse(s.length() == 4 && s.charAt(1) == ':' ? "0" + s : s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Message si une heure est saisie dans un format incorrect (champ vide : géré par validate()). */
    private static String timeError(TextField depart, TextField arrivee) {
        if (parseTime(depart) == null && !depart.getText().isBlank())
            return "L'heure de départ doit avoir le format HH:mm (exemple : 07:00).";
        if (parseTime(arrivee) == null && !arrivee.getText().isBlank())
            return "L'heure d'arrivée doit avoir le format HH:mm (exemple : 07:45).";
        return null;
    }

    private static Trajet build(Trajet existing, ComboBox<TrajetDAO.Option> ligne, DatePicker date,
                                TextField depart, TextField arrivee, ComboBox<TrajetDAO.Option> bus,
                                ComboBox<TrajetDAO.Option> chauffeur, ComboBox<StatutOption> statut) {
        Trajet t = new Trajet();
        if (existing != null) t.setIdAffectation(existing.getIdAffectation());
        t.setIdTrajet(ligne.getValue() == null ? 0 : ligne.getValue().id());
        t.setDateTrajet(date.getValue());
        t.setHeureDepart(parseTime(depart));
        t.setHeureArrivee(parseTime(arrivee));
        t.setIdBus(bus.getValue() == null ? 0 : bus.getValue().id());
        t.setIdChauffeur(chauffeur.getValue() == null ? 0 : chauffeur.getValue().id());
        t.setStatut(statut.getValue() == null ? null : statut.getValue().code());
        return t;
    }
}