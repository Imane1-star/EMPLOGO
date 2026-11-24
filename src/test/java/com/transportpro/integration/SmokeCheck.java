package com.transportpro.integration;

import com.transportpro.Page;
import com.transportpro.view.AppShell;
import com.transportpro.view.LoginView;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Test de fumée : ouvre l'écran de connexion puis chaque page pour chaque profil et signale
 * toute exception ou fenêtre d'erreur. Les fenêtres d'erreur sont fermées automatiquement.
 *
 * Lancer sur une copie de la base :
 *   java -Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test -cp ... com.transportpro.integration.SmokeCheck
 */
public class SmokeCheck extends Application {

    private static final List<String> problems = new ArrayList<>();
    private static boolean expectingDialog = false;
    private Scene scene;

    static void pump(int ms) {
        Object key = new Object();
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> Platform.exitNestedEventLoop(key, null));
        p.play();
        Platform.enterNestedEventLoop(key);
    }

    /** Ferme les boîtes de dialogue ouvertes et les note comme problème. */
    static void closeDialogs(String where) {
        for (Window w : new ArrayList<>(Window.getWindows())) {
            if (w.isShowing() && w.getScene() != null && w.getScene().getRoot() instanceof DialogPane d) {
                problems.add(where + " : fenêtre « " + d.getHeaderText() + " » " + d.getContentText());
                ((Stage) w).close();
            }
        }
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            problems.add("Exception : " + e);
            e.printStackTrace();
        });
        // ferme régulièrement les dialogues (sinon showAndWait bloquerait le test)
        PauseTransition watcher = new PauseTransition(Duration.millis(150));
        watcher.setOnFinished(e -> { if (!expectingDialog) closeDialogs("popup"); watcher.playFromStart(); });
        watcher.play();

        scene = new Scene(new LoginView(r -> {}), 1366, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> {
            try {
                run();
            } catch (Throwable t) {
                problems.add("Exception : " + t);
                t.printStackTrace();
            }
            System.out.println("\n===== RÉSULTAT =====");
            if (problems.isEmpty()) System.out.println("OK : aucune erreur");
            problems.forEach(p -> System.out.println("PROBLÈME " + p));
            Platform.exit();
        });
    }

    /** Ouvre la boîte de suppression et retourne le texte de ses boutons (puis la ferme). */
    private static String deleteDialogButtons(List<String> linked, String soft, boolean hard) {
        StringBuilder seen = new StringBuilder();
        PauseTransition grab = new PauseTransition(Duration.millis(100));
        grab.setOnFinished(e -> {
            for (Window w : new ArrayList<>(Window.getWindows())) {
                if (w.isShowing() && w.getScene().getRoot() instanceof DialogPane d) {
                    d.getButtonTypes().forEach(bt -> seen.append('[').append(bt.getText()).append(']'));
                    ((Stage) w).close();
                }
            }
        });
        grab.play();
        expectingDialog = true;
        com.transportpro.view.DeleteDialog.ask("l'employé Test", linked, soft, hard, "bloqué");
        expectingDialog = false;
        return seen.toString();
    }

    private void run() {
        pump(300);
        String free = deleteDialogButtons(List.of(), "Désactiver", true);
        String linked = deleteDialogButtons(List.of("3 inscription(s)"), "Désactiver", true);
        String blocked = deleteDialogButtons(List.of("2 trajet(s)"), "Désactiver", false);
        System.out.println("dialogue suppression : libre=" + free + " lié=" + linked + " bloqué=" + blocked);
        if (!free.equals("[Supprimer][Annuler]") || !linked.equals("[Désactiver][Supprimer tout][Annuler]")
                || !blocked.equals("[Désactiver][Annuler]"))
            problems.add("Boutons de la boîte de suppression inattendus");
        for (String role : List.of("Responsable", "Administrateur", "Chauffeur")) {
            AppShell shell = new AppShell(role, () -> {});
            scene.setRoot(shell);
            pump(300);
            for (Page p : Page.values()) {
                int before = problems.size();
                try {
                    shell.navigate(p);
                    pump(350);
                    closeDialogs(role + " / " + p.title);
                } catch (Throwable t) {
                    problems.add(role + " / " + p.title + " : " + t);
                    t.printStackTrace();
                }
                TableView<?> tv = (TableView<?>) scene.getRoot().lookup(".data-table");
                System.out.println((problems.size() == before ? "ok   " : "FAIL ") + role + " / " + p.title
                        + (tv == null ? "" : " (" + tv.getItems().size() + " lignes)"));
            }
            incidentChecks(shell, role);
        }
    }

    private static int dbCount(String sql) {
        try (var c = com.transportpro.dao.DatabaseConnection.getConnection();
             var s = c.createStatement(); var r = s.executeQuery(sql)) {
            return r.next() ? r.getInt(1) : -1;
        } catch (Exception e) {
            problems.add("SQL : " + e.getMessage());
            return -1;
        }
    }

    private static void expect(String name, boolean ok) {
        System.out.println((ok ? "ok   " : "FAIL ") + name);
        if (!ok) problems.add(name);
    }

    /** Page Incidents : lignes lues en base, filtres BF22, vue chauffeur BF21. */
    @SuppressWarnings("unchecked")
    private void incidentChecks(AppShell shell, String role) {
        shell.navigate(Page.INCIDENTS);
        pump(400);
        TableView<String[]> tv = (TableView<String[]>) scene.getRoot().lookup(".data-table");
        int total = dbCount("SELECT COUNT(*) FROM incident");
        expect(role + " / Incidents : lignes de la base (" + total + ")",
                tv.getItems().size() == Math.min(total, 8)
                        && tv.getItems().stream().noneMatch(r -> r[4].contains("Ouazzani")));

        List<javafx.scene.control.ComboBox<String>> combos = scene.getRoot().lookupAll(".main-card .combo-box")
                .stream().map(n -> (javafx.scene.control.ComboBox<String>) n).toList();
        javafx.scene.control.ComboBox<String> type = combos.get(0);
        type.setValue("Retard");
        type.getOnAction().handle(null);
        pump(200);
        int retards = dbCount("SELECT COUNT(*) FROM incident WHERE type='RETARD'");
        expect(role + " / Incidents : filtre type « Retard » (" + retards + ")",
                tv.getItems().size() == retards && tv.getItems().stream().allMatch(r -> r[1].equals("Retard")));
        type.setValue(type.getItems().get(0));
        type.getOnAction().handle(null);

        javafx.scene.control.ComboBox<String> bus = combos.get(1);
        String plate = bus.getItems().size() > 1 ? bus.getItems().get(1) : null;
        if (plate != null) {
            bus.setValue(plate);
            bus.getOnAction().handle(null);
            pump(200);
            int n = dbCount("SELECT COUNT(*) FROM incident i JOIN affectation a ON a.id_affectation=i.id_affectation "
                    + "JOIN bus b ON b.id_bus=a.id_bus WHERE b.immatriculation='" + plate + "'");
            expect(role + " / Incidents : filtre bus " + plate + " (" + n + ")", tv.getItems().size() == n);
            bus.setValue(bus.getItems().get(0));
            bus.getOnAction().handle(null);
        }

        boolean driver = role.equals("Chauffeur");
        boolean addLabel = scene.getRoot().lookupAll(".button").stream()
                .anyMatch(b -> ((javafx.scene.control.Button) b).getText()
                        .equals(driver ? "Signaler un incident" : "Enregistrer un incident"));
        pump(200);
        boolean rowLabel = scene.getRoot().lookupAll(".table-action").stream()
                .anyMatch(b -> ((javafx.scene.control.Button) b).getText().equals(driver ? "Voir" : "Modifier"));
        expect(role + " / Incidents : boutons « " + (driver ? "Signaler / Voir" : "Enregistrer / Modifier") + " »",
                addLabel && rowLabel);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
