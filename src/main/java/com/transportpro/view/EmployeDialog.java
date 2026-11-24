package com.transportpro.view;

import com.transportpro.model.Employe;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

import static com.transportpro.view.DialogKit.nz;
import static com.transportpro.view.DialogKit.text;

/** Fenêtre « Ajouter / Modifier un employé » (BF1, BF2). */
public final class EmployeDialog {

    private EmployeDialog() {}

    public static Optional<Employe> showAdd() {
        Employe e = new Employe();
        e.setActif(true);
        return show(e, false);
    }

    public static Optional<Employe> showEdit(Employe existing) {
        return show(existing, true);
    }

    private static Optional<Employe> show(Employe existing, boolean edit) {
        Dialog<Employe> d = DialogKit.dialog(edit ? "Modifier un employé" : "Ajouter un employé", "Enregistrer");

        TextField matricule = new TextField(nz(existing.getMatricule()));
        TextField nom = new TextField(nz(existing.getNom()));
        TextField prenom = new TextField(nz(existing.getPrenom()));
        TextField email = new TextField(nz(existing.getEmail()));
        TextField telephone = new TextField(nz(existing.getTelephone()));
        TextField adresse = new TextField(nz(existing.getAdresse()));
        TextField departement = new TextField(nz(existing.getDepartement()));
        DatePicker date = new DatePicker(existing.getDateEmbauche());
        ComboBox<String> statut = Ui.combo("Actif", "Inactif");
        statut.setValue(existing.isActif() ? "Actif" : "Inactif");

        Label error = DialogKit.errorLabel();
        GridPane g = DialogKit.grid(520);
        g.add(Ui.field("Matricule *", matricule), 0, 0);
        g.add(Ui.field("Statut *", statut), 1, 0);
        g.add(Ui.field("Nom *", nom), 0, 1);
        g.add(Ui.field("Prénom *", prenom), 1, 1);
        g.add(Ui.field("Téléphone", telephone), 0, 2);
        g.add(Ui.field("E-mail *", email), 1, 2);
        g.add(Ui.field("Adresse / point d’embarquement", adresse), 0, 3);
        g.add(Ui.field("Département", departement), 1, 3);
        g.add(Ui.field("Date d’embauche", date), 0, 4);
        g.add(error, 0, 5, 2, 1);
        DialogKit.content(d, g);

        java.util.function.Supplier<Employe> build = () -> {
            Employe e = new Employe();
            e.setIdEmploye(existing.getIdEmploye());
            e.setMatricule(text(matricule));
            e.setNom(text(nom));
            e.setPrenom(text(prenom));
            e.setEmail(text(email));
            e.setTelephone(text(telephone));
            e.setAdresse(text(adresse));
            e.setDepartement(text(departement));
            e.setDateEmbauche(date.getValue());
            e.setActif("Actif".equals(statut.getValue()));
            return e;
        };
        DialogKit.guard(d, error, () -> build.get().validate());
        d.setResultConverter(bt -> bt == DialogKit.saveType(d) ? build.get() : null);
        return d.showAndWait();
    }
}
