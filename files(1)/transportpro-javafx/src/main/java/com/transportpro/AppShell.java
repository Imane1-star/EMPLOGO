package com.transportpro;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/** Fenêtre principale : barre latérale + en-tête + zone de contenu, avec modale et notifications. */
public class AppShell extends StackPane implements Ctx {

    private String role;
    private Page page;
    private final Runnable onLogout;

    private final VBox navBox = new VBox(2);
    private final Label crumb = Ui.label("", "crumb");
    private final Label title = Ui.label("", "page-title");
    private final ScrollPane scroll = new ScrollPane();

    private final Label sideInitials = Ui.label("", "avatar-text");
    private final Label sideName = Ui.label("", "side-name");
    private final Label sideRole = Ui.label("", "side-role");

    public AppShell(String role, Runnable onLogout) {
        this.role = role;
        this.onLogout = onLogout;

        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(buildWorkspace());
        getChildren().add(root);

        navigate(role.equals("Chauffeur") ? Page.PLANNING : Page.DASHBOARD);
    }

    // ------------------------------------------------------------------ barre latérale
    private Node buildSidebar() {
        StackPane logo = new StackPane(Ui.icon("buses", 20, "#190019"));
        logo.getStyleClass().add("logo-mark");
        logo.setMinSize(38, 38);
        logo.setPrefSize(38, 38);
        logo.setMaxSize(38, 38);

        HBox brand = new HBox(12, logo, new VBox(1, Ui.label("TransportPro", "brand-name"), Ui.label("Gestion des transports", "brand-sub")));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 20, 0, 20));
        Ui.fixHeight(brand, 84);

        VBox nav = new VBox(6, Ui.label("NAVIGATION", "nav-section"), navBox);
        nav.setPadding(new Insets(8, 10, 8, 10));
        VBox.setVgrow(nav, Priority.ALWAYS);

        StackPane avatar = new StackPane(sideInitials);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(32, 32);
        avatar.setPrefSize(32, 32);
        avatar.setMaxSize(32, 32);
        HBox me = new HBox(9, avatar, new VBox(1, sideName, sideRole));
        me.setAlignment(Pos.CENTER_LEFT);

        Button out = new Button("Se déconnecter", Ui.icon("logout", 18, "#E5CFD9"));
        out.getStyleClass().add("logout-btn");
        out.setGraphicTextGap(8);
        out.setMaxWidth(Double.MAX_VALUE);
        out.setOnAction(e -> onLogout.run());

        VBox user = new VBox(11, me, out);
        user.getStyleClass().add("sidebar-user");

        VBox sidebar = new VBox(brand, nav, user);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setMinWidth(245);
        sidebar.setPrefWidth(245);
        sidebar.setMaxWidth(245);
        return sidebar;
    }

    private void refreshNav() {
        boolean driver = role.equals("Chauffeur");
        List<Page> items = new ArrayList<>();
        if (driver) {
            items.add(Page.PLANNING);
            items.add(Page.INCIDENTS);
        } else {
            for (Page p : Page.values()) {
                if (p == Page.PLANNING) continue;
                if (role.equals("Administrateur") || (p != Page.ACCOUNTS && p != Page.ROLES)) items.add(p);
            }
        }

        navBox.getChildren().clear();
        for (Page p : items) {
            boolean active = p == page;
            String text = (driver && p == Page.INCIDENTS) ? "Signaler un incident" : p.title;
            Button b = new Button(text, Ui.icon(p.icon, 20, active ? "#DFB6B2" : "#E5CFD9"));
            b.setGraphicTextGap(11);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setMaxWidth(Double.MAX_VALUE);
            b.getStyleClass().add("nav-item");
            if (active) b.getStyleClass().add("active");
            b.setOnAction(e -> navigate(p));
            navBox.getChildren().add(b);
        }

        sideInitials.setText(driver ? "HO" : "AE");
        sideName.setText(driver ? "Hassan Ouazzani" : "Amine El Mansouri");
        sideRole.setText(role);
    }

    // ------------------------------------------------------------------ en-tête + contenu
    private Node buildWorkspace() {
        VBox heading = new VBox(3, crumb, title);

        HBox search = Ui.search("Rechercher dans TransportPro…", 255);

        VBox today = new VBox(1, Ui.label("Aujourd’hui", "today-sm"), Ui.label("12 juin 2025", "today-b"));
        today.getStyleClass().add("today");

        Label count = Ui.label("3", "notify-count");
        count.setTranslateX(5);
        count.setTranslateY(-5);
        StackPane bell = new StackPane(Ui.icon("bell", 20, "#522B5B"), count);
        StackPane.setAlignment(count, Pos.TOP_RIGHT);
        bell.getStyleClass().add("notify");
        bell.setMinSize(34, 34);
        bell.setPrefSize(34, 34);
        bell.setMaxSize(34, 34);

        ComboBox<String> roleBox = Ui.combo("Responsable", "Administrateur", "Chauffeur");
        roleBox.setValue(role);
        roleBox.setPrefWidth(150);
        roleBox.setMaxWidth(150);
        roleBox.setOnAction(e -> {
            role = roleBox.getValue();
            navigate(role.equals("Chauffeur") ? Page.PLANNING : Page.DASHBOARD);
        });

        HBox tools = new HBox(14, search, today, bell, roleBox);
        tools.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(heading, Ui.spacer(), tools);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("workspace-header");
        Ui.fixHeight(header, 76);

        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new VBox(header, scroll);
    }

    @Override
    public void navigate(Page p) {
        page = p;
        refreshNav();
        crumb.setText("TransportPro / " + p.title);
        title.setText(p.title);

        Node content = switch (p) {
            case DASHBOARD -> DashboardPage.build(this);
            case EMPLOYEES, BUSES, DRIVERS, TRIPS, INCIDENTS, ACCOUNTS -> ListPages.build(p, this);
            case ASSIGNMENTS -> AssignmentsPage.build(this);
            case REGISTRATIONS -> RegistrationsPage.build(this);
            case ATTENDANCE -> AttendancePage.build(this);
            case REPORTS -> ReportsPage.build(this);
            case ROLES -> RolesPage.build(this);
            case PLANNING -> PlanningPage.build(this);
        };
        scroll.setContent(content);
        scroll.setVvalue(0);
    }

    // ------------------------------------------------------------------ modale
    @Override
    public void openModal(String titleText) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-backdrop");
        Runnable close = () -> getChildren().remove(overlay);

        Button x = new Button();
        x.setGraphic(Ui.icon("close", 18, "#522B5B"));
        x.getStyleClass().add("icon-btn");
        x.setOnAction(e -> close.run());
        HBox head = new HBox(new VBox(3, Ui.label("TRANSPORTPRO", "eyebrow"), Ui.label(titleText, "modal-title")), Ui.spacer(), x);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("modal-head");

        VBox body = new VBox(formBody(titleText));
        body.getStyleClass().add("modal-body");

        HBox foot = new HBox(10,
                Ui.button("Annuler", "secondary", null, close),
                Ui.button("Enregistrer", "primary", null, () -> {
                    close.run();
                    toast(titleText + " — enregistré avec succès");
                }));
        foot.setAlignment(Pos.CENTER_RIGHT);
        foot.getStyleClass().add("modal-foot");

        VBox modal = new VBox(head, body, foot);
        modal.getStyleClass().add("modal");
        modal.setMinWidth(520);
        modal.setPrefWidth(520);
        modal.setMaxWidth(520);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setOnMouseClicked(e -> e.consume());

        overlay.getChildren().add(modal);
        overlay.setOnMouseClicked(e -> close.run());
        getChildren().add(overlay);
    }

    private Node formBody(String t) {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);

        if (t.toLowerCase().contains("incident")) {
            g.add(Ui.field("Trajet", Ui.combo("T1 • Zone industrielle", "T2 • Taroudant Centre")), 0, 0);
            g.add(Ui.field("Type", Ui.combo("Retard", "Panne", "Autre")), 1, 0);
            TextArea d = new TextArea();
            d.setPromptText("Décrivez précisément l’incident...");
            d.setPrefRowCount(4);
            d.setWrapText(true);
            g.add(Ui.field("Description", d), 0, 1, 2, 1);
        } else {
            TextField nom = new TextField();
            nom.setPromptText("Saisir le nom");
            TextField tel = new TextField();
            tel.setPromptText("06 00 00 00 00");
            g.add(Ui.field("Nom complet", nom), 0, 0);
            g.add(Ui.field("Téléphone", tel), 1, 0);
            g.add(Ui.field("Point d’embarquement", Ui.combo("Taroudant Centre", "Oulad Teima", "Ait Iaazza")), 0, 1);
            g.add(Ui.field("Statut", Ui.combo("Actif", "Inactif")), 1, 1);
        }
        return g;
    }

    // ------------------------------------------------------------------ notification
    @Override
    public void toast(String message) {
        HBox t = new HBox(12,
                Ui.label("✓", "toast-icon"),
                new VBox(2, Ui.label("Opération réussie", "toast-title"), Ui.label(message, "toast-msg")));
        t.getStyleClass().add("toast");
        t.setAlignment(Pos.CENTER_LEFT);
        t.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(t, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(t, new Insets(0, 24, 24, 0));
        getChildren().add(t);

        PauseTransition pause = new PauseTransition(Duration.seconds(2.8));
        pause.setOnFinished(e -> getChildren().remove(t));
        pause.play();
    }
}
