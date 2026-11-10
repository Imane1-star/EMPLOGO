package com.transportpro;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
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

import java.util.Optional;

/** Fenêtre « Ajouter / Modifier un employé » (AppShell n'est pas modifié). */
public final class EmployeeDialog {

    private EmployeeDialog() {}

    public static Optional<Employee> showAdd() {
        return show(new Employee(), false);
    }

    public static Optional<Employee> showEdit(Employee existing) {
        return show(existing, true);
    }

    private static Optional<Employee> show(Employee existing, boolean edit) {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier un employé" : "Ajouter un employé");
        dialog.setHeaderText(null);

        var css = EmployeeDialog.class.getResource("/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField matricule = new TextField(nz(existing.getMatricule()));
        TextField nom = new TextField(nz(existing.getNom()));
        TextField prenom = new TextField(nz(existing.getPrenom()));
        TextField email = new TextField(nz(existing.getEmail()));
        TextField telephone = new TextField(nz(existing.getTelephone()));
        TextField adresse = new TextField(nz(existing.getAdresse()));
        TextField departement = new TextField(nz(existing.getDepartement()));
        DatePicker date = new DatePicker(existing.getDateEmbauche());
        ComboBox<String> statut = new ComboBox<>();
        statut.getItems().addAll("Actif", "Inactif");
        statut.setValue(existing.isActif() ? "Actif" : "Inactif");

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
        g.add(Ui.field("Matricule *", matricule), 0, 0);
        g.add(Ui.field("Statut *", statut), 1, 0);
        g.add(Ui.field("Nom *", nom), 0, 1);
        g.add(Ui.field("Prénom *", prenom), 1, 1);
        g.add(Ui.field("Téléphone", telephone), 0, 2);
        g.add(Ui.field("E-mail", email), 1, 2);
        g.add(Ui.field("Adresse / point d’embarquement", adresse), 0, 3);
        g.add(Ui.field("Département", departement), 1, 3);
        g.add(Ui.field("Date d’embauche", date), 0, 4);
        g.add(error, 0, 5, 2, 1);
        g.setPrefWidth(520);
        dialog.getDialogPane().setContent(g);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = build(existing, matricule, nom, prenom, email, telephone,
                    adresse, departement, date, statut).validate();
            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == saveType
                ? build(existing, matricule, nom, prenom, email, telephone,
                        adresse, departement, date, statut)
                : null);

        return dialog.showAndWait();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static Employee build(Employee existing, TextField matricule, TextField nom,
                                  TextField prenom, TextField email, TextField telephone,
                                  TextField adresse, TextField departement,
                                  DatePicker date, ComboBox<String> statut) {
        Employee e = new Employee();
        e.setIdEmploye(existing.getIdEmploye());
        e.setMatricule(matricule.getText().trim());
        e.setNom(nom.getText().trim());
        e.setPrenom(prenom.getText().trim());
        e.setEmail(email.getText().trim());
        e.setTelephone(telephone.getText().trim());
        e.setAdresse(adresse.getText().trim());
        e.setDepartement(departement.getText().trim());
        e.setDateEmbauche(date.getValue());
        e.setActif("Actif".equals(statut.getValue()));
        return e;
    }
}