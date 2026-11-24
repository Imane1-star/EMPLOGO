package com.transportpro.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation de suppression commune à tous les modules.
 * <ul>
 *   <li>Rien de lié : « Supprimer » / « Annuler ».</li>
 *   <li>Données liées : explique ce qui est lié et propose l'action qui conserve l'historique
 *       (désactiver, mettre hors service, annuler...) et, si c'est possible sans toucher aux
 *       données des autres, la suppression complète.</li>
 * </ul>
 */
public final class DeleteDialog {

    private DeleteDialog() {}

    public enum Choice { DELETE, SOFT, CANCEL }

    /**
     * @param what        ex. « l'employé Benali Karim (EMP001) »
     * @param linked      lignes décrivant les données liées (vide = rien de lié)
     * @param softLabel   texte du bouton qui conserve l'historique (null = pas d'action douce)
     * @param canHardDelete suppression complète autorisée quand il y a des données liées
     * @param blockedReason explication quand la suppression complète est refusée
     */
    public static Choice ask(String what, List<String> linked, String softLabel,
                             boolean canHardDelete, String blockedReason) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.getDialogPane().setPrefWidth(520);
        var css = DeleteDialog.class.getResource("/styles.css");
        if (css != null) a.getDialogPane().getStylesheets().add(css.toExternalForm());

        ButtonType delete = new ButtonType(linked.isEmpty() ? "Supprimer" : "Supprimer tout",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType soft = softLabel == null ? null : new ButtonType(softLabel, ButtonBar.ButtonData.YES);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        List<ButtonType> buttons = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        if (linked.isEmpty()) {
            a.setHeaderText("Supprimer " + what + " ?");
            text.append("Cette action est définitive.");
            buttons.add(delete);
        } else {
            a.setHeaderText(capitalize(what) + " est lié(e) à d'autres données :");
            for (String l : linked) text.append("• ").append(l).append('\n');
            text.append('\n');
            if (soft != null) {
                text.append("« ").append(softLabel).append(" » conserve tout l'historique (recommandé).\n");
                buttons.add(soft);
            }
            if (canHardDelete) {
                text.append("« Supprimer tout » efface aussi ces données liées, définitivement.");
                buttons.add(delete);
            } else if (blockedReason != null) {
                text.append(blockedReason);
            }
        }
        buttons.add(cancel);
        a.setContentText(text.toString().trim());
        a.getButtonTypes().setAll(buttons);

        ButtonType r = a.showAndWait().orElse(cancel);
        if (r == delete) return Choice.DELETE;
        if (r == soft) return Choice.SOFT;
        return Choice.CANCEL;
    }

    /** « 3 inscription(s) » ; null si n == 0 (pratique pour construire la liste « linked »). */
    public static void addIf(List<String> list, int n, String label) {
        if (n > 0) list.add(n + " " + label);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
