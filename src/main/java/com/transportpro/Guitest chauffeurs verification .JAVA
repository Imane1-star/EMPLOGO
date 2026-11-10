import com.transportpro.*;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.*;

public class GuiTest3 extends Application {
    static int fails = 0, passes = 0;
    static void check(String n, boolean ok, String d) { System.out.println((ok ? "PASS " : "FAIL ") + n + (d.isEmpty() ? "" : " -> " + d)); if (ok) passes++; else fails++; }
    static void after(int ms, Runnable r) { PauseTransition p = new PauseTransition(Duration.millis(ms)); p.setOnFinished(e -> r.run()); p.play(); }
    static void pump(int ms) { Object k = new Object(); after(ms, () -> Platform.exitNestedEventLoop(k, null)); Platform.enterNestedEventLoop(k); }
    static DialogPane dp() { for (Window w : Window.getWindows()) if (w.isShowing() && w.getScene() != null && w.getScene().getRoot() instanceof DialogPane d) return d; return null; }
    static String q(String sql) throws Exception { try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) { return r.next() ? r.getString(1) : null; } }
    AppShell shell; Scene scene;
    @SuppressWarnings("unchecked") TableView<String[]> table() { return (TableView<String[]>) scene.getRoot().lookup(".data-table"); }
    TextField search() { return (TextField) scene.getRoot().lookup(".main-card .search .text-field"); }
    String info() { return scene.getRoot().lookupAll(".muted").stream().filter(n -> n instanceof Label l && (l.getText().startsWith("Affichage") || l.getText().startsWith("Aucun"))).map(n -> ((Label) n).getText()).findFirst().orElse("?"); }
    List<Button> btns(String t) { return scene.getRoot().lookupAll(".button").stream().map(n -> (Button) n).filter(b -> t.equals(b.getText())).collect(Collectors.toList()); }
    @SuppressWarnings("unchecked") List<ComboBox<Object>> combos(DialogPane d) { return d.lookupAll(".combo-box").stream().filter(n -> n instanceof ComboBox).map(n -> (ComboBox<Object>) n).collect(Collectors.toList()); }
    void save(DialogPane d) { ((Button) d.lookupButton(d.getButtonTypes().get(0))).fire(); }
    void find(String text) { search().setText(text); pump(700); }
    ContextMenu menu() { return Window.getWindows().stream().filter(w -> w instanceof ContextMenu && w.isShowing()).map(w -> (ContextMenu) w).findFirst().orElse(null); }
    DatePicker datePicker(DialogPane d) { return (DatePicker) d.lookupAll(".date-picker").stream().findFirst().get(); }

    @Override public void start(Stage st) {
        shell = new AppShell("Administrateur", () -> {});
        scene = new Scene(shell, 1366, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        st.setScene(scene); st.show();
        Platform.runLater(() -> { try { run(); } catch (Throwable t) { t.printStackTrace(); fails++; } System.out.println("PASSES=" + passes + " FAILS=" + fails); Platform.exit(); });
    }

    void run() throws Exception {
        // statuts lus dans la base
        ChauffeurDAO dao = new ChauffeurDAO();
        check("Statuts lus depuis l'ENUM de la base", dao.findStatuts().equals(List.of("ACTIF", "INACTIF", "SUSPENDU")), dao.findStatuts().toString());
        shell.navigate(Page.DRIVERS); pump(500);
        check("Chauffeurs: lignes lues en base (3)", table().getItems().size() == 3 && info().endsWith("sur 3"), info());
        String[] r0 = table().getItems().get(0);
        check("Nom et telephone viennent de 'employe' (jointure)", r0[0].equals("Alaoui Éric") && r0[1].equals("0612345678") && r0[2].equals("D-123456") && r0[3].equals("Actif"), Arrays.toString(r0));
        check("Donnees fictives (Data) absentes", table().getItems().stream().noneMatch(r -> r[0].contains("Ouazzani")), "");
        // recherche SQL
        find("alaoui"); check("Recherche nom", table().getItems().size() == 1 && table().getItems().get(0)[2].equals("D-123456"), info());
        find("sara"); check("Recherche prenom", table().getItems().size() == 1 && table().getItems().get(0)[2].equals("D-234567"), info());
        find("0634"); check("Recherche telephone", table().getItems().size() == 1 && table().getItems().get(0)[2].equals("D-345678"), info());
        find("d-2345"); check("Recherche numero de permis", table().getItems().size() == 1, info());
        find("e003"); check("Recherche matricule employe", table().getItems().size() == 1, info());
        find("zzz"); check("Recherche sans resultat", info().equals("Aucun résultat"), info());
        find(""); check("Champ vide = tous", info().endsWith("sur 3"), info());
        // AJOUTER
        after(600, () -> { DialogPane d = dp(); check("Ajouter: fenetre ouverte", d != null, "");
            List<ComboBox<Object>> cb = combos(d);
            String choix = cb.get(0).getValue().toString();
            check("Ajouter: liste = employes pas encore chauffeurs (Dahmani en premier)", choix.startsWith("Dahmani"), choix);
            check("Ajouter: 8 employes disponibles (11 - 3)", cb.get(0).getItems().size() == 8, "" + cb.get(0).getItems().size());
            check("Ajouter: statuts proposes", cb.get(1).getItems().size() == 3, cb.get(1).getItems().toString());
            d.lookupAll(".text-field").stream().map(n -> (TextField) n).findFirst().get().setText("D-999999");
            datePicker(d).setValue(LocalDate.of(2030, 1, 1));
            save(d); });
        btns("Ajouter un chauffeur").get(0).fire(); pump(600);
        check("Ajouter: insere en BASE", "4".equals(q("SELECT COUNT(*) FROM chauffeur")) && "D-999999".equals(q("SELECT numero_permis FROM chauffeur WHERE id_employe=4")), "");
        check("Ajouter: date et statut enregistres", "2030-01-01".equals(q("SELECT date_validite_permis FROM chauffeur WHERE id_employe=4")) && "ACTIF".equals(q("SELECT statut FROM chauffeur WHERE id_employe=4")), "");
        check("Ajouter: tableau rafraichi", info().endsWith("sur 4"), info());
        String idNew = q("SELECT id_chauffeur FROM chauffeur WHERE id_employe=4");
        // validation
        after(600, () -> { DialogPane d = dp(); save(d);
            Label err = (Label) d.lookupAll(".label").stream().filter(n -> n instanceof Label l && l.getText().contains("obligatoire")).findFirst().orElse(null);
            check("Ajouter: permis vide refuse (fenetre reste ouverte)", err != null && d.getScene().getWindow().isShowing(), "");
            ((Button) d.lookupButton(ButtonType.CANCEL)).fire(); });
        btns("Ajouter un chauffeur").get(0).fire(); pump(600);
        check("Ajouter annule: rien insere", "4".equals(q("SELECT COUNT(*) FROM chauffeur")), "");
        // doublon permis
        after(600, () -> { DialogPane d = dp();
            d.lookupAll(".text-field").stream().map(n -> (TextField) n).findFirst().get().setText("D-999999");
            datePicker(d).setValue(LocalDate.of(2031, 2, 2)); save(d);
            after(500, () -> { DialogPane a = dp(); check("Doublon de permis: message clair", a != null && a.getContentText().contains("déjà enregistré"), a == null ? "" : a.getContentText()); if (a != null) ((Button) a.lookupButton(ButtonType.OK)).fire();
                after(300, () -> { DialogPane l = dp(); if (l != null) ((Button) l.lookupButton(ButtonType.CANCEL)).fire(); }); }); });
        btns("Ajouter un chauffeur").get(0).fire(); pump(1800);
        // MODIFIER
        find("999999");
        after(600, () -> { DialogPane d = dp(); List<ComboBox<Object>> cb = combos(d);
            check("Modifier: pre-rempli (employe fixe et grise)", cb.get(0).getValue().toString().startsWith("Dahmani") && cb.get(0).isDisabled(), cb.get(0).getValue().toString());
            check("Modifier: permis et statut pre-remplis", d.lookupAll(".text-field").stream().map(n -> (TextField) n).findFirst().get().getText().equals("D-999999") && cb.get(1).getValue().equals("ACTIF"), "");
            d.lookupAll(".text-field").stream().map(n -> (TextField) n).findFirst().get().setText("D-888888");
            cb.get(1).setValue("SUSPENDU");
            datePicker(d).setValue(LocalDate.of(2032, 3, 3));
            save(d); });
        btns("Modifier").get(0).fire(); pump(700);
        check("Modifier: enregistre en BASE", "D-888888".equals(q("SELECT numero_permis FROM chauffeur WHERE id_chauffeur=" + idNew)) && "SUSPENDU".equals(q("SELECT statut FROM chauffeur WHERE id_chauffeur=" + idNew)) && "2032-03-03".equals(q("SELECT date_validite_permis FROM chauffeur WHERE id_chauffeur=" + idNew)), "");
        check("Modifier: id_chauffeur et id_employe conserves", "4".equals(q("SELECT id_employe FROM chauffeur WHERE id_chauffeur=" + idNew)) && "4".equals(q("SELECT COUNT(*) FROM chauffeur")), "");
        check("Modifier: autres chauffeurs intacts", "D-123456".equals(q("SELECT numero_permis FROM chauffeur WHERE id_employe=1")) && "ACTIF".equals(q("SELECT statut FROM chauffeur WHERE id_employe=1")), "");
        check("Modifier: recherche conservee, ancienne ligne ne correspond plus", search().getText().equals("999999") && table().getItems().isEmpty(), info());
        find("888888");
        check("Modifier: tableau mis a jour (nouveau permis + statut Suspendu)", table().getItems().size() == 1 && table().getItems().get(0)[2].equals("D-888888") && table().getItems().get(0)[3].equals("Suspendu"), table().getItems().isEmpty() ? "vide" : Arrays.toString(table().getItems().get(0)));
        // MENU •••
        btns("•••").get(0).fire(); pump(300);
        ContextMenu cm = menu();
        check("••• : menu Modifier + Supprimer", cm != null && cm.getItems().stream().map(MenuItem::getText).collect(Collectors.toList()).equals(List.of("Modifier", "Supprimer")), "");
        after(400, () -> { DialogPane a = dp(); check("Supprimer: confirmation nomme le chauffeur", a != null && a.getContentText().contains("D-888888") && a.getContentText().contains("Dahmani"), a == null ? "" : a.getContentText()); ((Button) a.lookupButton(ButtonType.NO)).fire(); });
        cm.getItems().get(1).fire(); pump(300);
        check("Supprimer + NON: rien supprime", "4".equals(q("SELECT COUNT(*) FROM chauffeur")), "");
        btns("•••").get(0).fire(); pump(300);
        ContextMenu cm2 = menu();
        after(400, () -> ((Button) dp().lookupButton(ButtonType.YES)).fire());
        cm2.getItems().get(1).fire(); pump(600);
        check("Supprimer + OUI: supprime en BASE", "0".equals(q("SELECT COUNT(*) FROM chauffeur WHERE id_chauffeur=" + idNew)) && "3".equals(q("SELECT COUNT(*) FROM chauffeur")), "");
        check("Supprimer: l'employe reste dans 'employe'", "1".equals(q("SELECT COUNT(*) FROM employe WHERE id_employe=4")), "");
        find(""); check("Supprimer: tableau rafraichi (3)", info().endsWith("sur 3"), info());
        // SUPPRIMER chauffeur reference par une autre table (FK)
        find("alaoui");
        btns("•••").get(0).fire(); pump(300);
        ContextMenu cm3 = menu();
        after(400, () -> ((Button) dp().lookupButton(ButtonType.YES)).fire());
        after(1200, () -> { DialogPane a = dp(); check("Chauffeur reference: message FK clair", a != null && a.getContentText().contains("utilisé"), a == null ? "pas d'alerte" : a.getContentText()); if (a != null) ((Button) a.lookupButton(ButtonType.OK)).fire(); });
        cm3.getItems().get(1).fire(); pump(1800);
        check("Chauffeur reference toujours en base", "1".equals(q("SELECT COUNT(*) FROM chauffeur WHERE id_employe=1")), "");
        // autres modules
        shell.navigate(Page.EMPLOYEES); pump(500);
        check("Employes: toujours depuis la base", info().endsWith("sur 11"), info());
        shell.navigate(Page.BUSES); pump(500);
        check("Bus: toujours depuis la base", info().startsWith("Affichage"), info());
        shell.navigate(Page.TRIPS); pump(400);
        check("Trajets (Data): toujours affiches", table().getItems().size() > 0, "");
    }
    public static void main(String[] a) { launch(a); }
}