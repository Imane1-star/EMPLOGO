package com.emplogo.view;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import static com.emplogo.view.DialogKit.nz;
import static com.emplogo.view.DialogKit.text;

import java.util.List;
import java.util.Optional;

import com.emplogo.controller.MainController;
import com.emplogo.dao.Option;
import com.emplogo.model.Chauffeur;

/** Fenêtre « Ajouter / Modifier un chauffeur » (BF7, BF8) : un chauffeur est un employé avec un permis. */
public final class ChauffeurDialog {

    private ChauffeurDialog() {}

    public static Optional<Chauffeur> showAdd(MainController controller) {
        return show(controller, null);
    }

    public static Optional<Chauffeur> showEdit(MainController controller, Chauffeur existing) {
        return show(controller, existing);
    }

    private static Optional<Chauffeur> show(MainController controller, Chauffeur existing) {
        boolean edit = existing != null;
        List<Option> employes = controller.employesPourChauffeur(edit ? existing.getIdEmploye() : 0);
        Dialog<Chauffeur> d = DialogKit.dialog(edit ? "Modifier un chauffeur" : "Ajouter un chauffeur", "Enregistrer");

        ComboBox<Option> employe = DialogKit.optionBox(employes, edit ? existing.getIdEmploye() : 0);
        if (edit) employe.setDisable(true);             // l'employé lié ne change pas en modification
        else if (!employes.isEmpty()) employe.setValue(employes.get(0));

        TextField permis = new TextField(edit ? nz(existing.getNumeroPermis()) : "");
        permis.setPromptText("D-123456");
        DatePicker validite = new DatePicker(edit ? existing.getValiditePermis() : null);
        validite.setPromptText("jj/mm/aaaa");
        ComboBox<String> statut = new ComboBox<>();
        statut.getItems().addAll(Chauffeur.STATUTS);
        statut.setMaxWidth(Double.MAX_VALUE);
        statut.setCellFactory(l -> labelCell());
        statut.setButtonCell(labelCell());
        statut.setValue(edit && existing.getStatut() != null ? existing.getStatut() : "ACTIF");

        Label error = DialogKit.errorLabel();
        if (!edit && employes.isEmpty())
            error.setText("Aucun employé disponible : tous les employés actifs sont déjà chauffeurs.");

        GridPane g = DialogKit.grid(520);
        g.add(Ui.field("Employé *", employe), 0, 0);
        g.add(Ui.field("Statut *", statut), 1, 0);
        g.add(Ui.field("N° de permis *", permis), 0, 1);
        g.add(Ui.field("Validité du permis *", validite), 1, 1);
        g.add(error, 0, 2, 2, 1);
        DialogKit.content(d, g);

        java.util.function.Supplier<Chauffeur> build = () -> {
            Chauffeur c = new Chauffeur();
            if (edit) {
                c.setIdChauffeur(existing.getIdChauffeur());
                c.setNom(existing.getNom());
                c.setPrenom(existing.getPrenom());
            }
            c.setIdEmploye(DialogKit.idOf(employe));
            c.setNumeroPermis(text(permis));
            c.setValiditePermis(validite.getValue());
            c.setStatut(statut.getValue());
            return c;
        };
        DialogKit.guard(d, error, () -> build.get().validate());
        d.setResultConverter(bt -> bt == DialogKit.saveType(d) ? build.get() : null);
        return d.showAndWait();
    }

    /** Affiche « En congé » au lieu de EN_CONGE dans la liste. */
    private static ListCell<String> labelCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : Chauffeur.statutLabel(item));
            }
        };
    }
}
