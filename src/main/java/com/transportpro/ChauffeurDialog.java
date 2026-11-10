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

import java.util.List;
import java.util.Optional;

/** Fenêtre « Ajouter / Modifier un chauffeur » (AppShell n'est pas modifié). */
public final class ChauffeurDialog {

    private ChauffeurDialog() {}

    public static Optional<Chauffeur> showAdd() {
        return show(null);
    }

    public static Optional<Chauffeur> showEdit(Chauffeur existing) {
        return show(existing);
    }

    private static Optional<Chauffeur> show(Chauffeur existing) {
        boolean edit = existing != null;

        // Listes lues dans la base (employés disponibles, statuts autorisés)
        List<ChauffeurDAO.EmployeeOption> employes;
        List<String> statuts;
        try {
            ChauffeurDAO dao = new ChauffeurDAO();
            employes = dao.findAvailableEmployees(edit ? existing.getIdEmploye() : 0);
            statuts = dao.findStatuts();
        } catch (ChauffeurDAO.ChauffeurException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return Optional.empty();
        }

        Dialog<Chauffeur> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier un chauffeur" : "Ajouter un chauffeur");
        dialog.setHeaderText(null);

        var css = ChauffeurDialog.class.getResource("/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<ChauffeurDAO.EmployeeOption> employe = new ComboBox<>();
        employe.getItems().addAll(employes);
        if (edit) {
            employes.stream().filter(o -> o.id() == existing.getIdEmploye())
                    .findFirst().ifPresent(employe::setValue);
            employe.setDisable(true);          // l'employé lié ne change pas en modification
        } else if (!employes.isEmpty()) {
            employe.setValue(employes.get(0));
        }

        TextField permis = new TextField(edit ? nz(existing.getNumeroPermis()) : "");
        permis.setPromptText("D-123456");
        DatePicker validite = new DatePicker(edit ? existing.getDateValiditePermis() : null);
        validite.setPromptText("jj/mm/aaaa");

        ComboBox<String> statut = new ComboBox<>();
        statut.getItems().addAll(statuts);
        if (edit && existing.getStatut() != null && !statuts.contains(existing.getStatut()))
            statut.getItems().add(existing.getStatut());
        statut.setValue(edit && existing.getStatut() != null ? existing.getStatut() : statuts.get(0));

        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);
        if (!edit && employes.isEmpty())
            error.setText("Aucun employé disponible : tous les employés sont déjà chauffeurs.");

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        g.setPadding(new Insets(18));
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);
        g.add(Ui.field("Employé *", employe), 0, 0);
        g.add(Ui.field("Statut *", statut), 1, 0);
        g.add(Ui.field("N° de permis *", permis), 0, 1);
        g.add(Ui.field("Validité du permis *", validite), 1, 1);
        g.add(error, 0, 2, 2, 1);
        g.setPrefWidth(520);
        dialog.getDialogPane().setContent(g);

        // On empêche la fermeture tant que la saisie est invalide
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = build(existing, employe, permis, validite, statut).validate();
            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });

        dialog.setResultConverter(bt ->
                bt == saveType ? build(existing, employe, permis, validite, statut) : null);

        return dialog.showAndWait();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static Chauffeur build(Chauffeur existing, ComboBox<ChauffeurDAO.EmployeeOption> employe,
                                   TextField permis, DatePicker validite, ComboBox<String> statut) {
        Chauffeur ch = new Chauffeur();
        if (existing != null) ch.setIdChauffeur(existing.getIdChauffeur());
        ch.setIdEmploye(employe.getValue() == null ? 0 : employe.getValue().id());
        ch.setNumeroPermis(permis.getText() == null ? "" : permis.getText().trim());
        ch.setDateValiditePermis(validite.getValue());
        ch.setStatut(statut.getValue());
        return ch;
    }
}