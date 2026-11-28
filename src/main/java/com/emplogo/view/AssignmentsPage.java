package com.emplogo.view;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.emplogo.Ctx;
import com.emplogo.controller.MainController;
import com.emplogo.dao.Option;
import com.emplogo.model.Affectation;
import com.emplogo.model.enums.StatutTrajet;

/**
 * Module Affectations : tableau des affectations (table « affectation »), recherche, filtre par statut,
 * pagination, modification du bus / chauffeur et annulation (statut ANNULEE, sans suppression).
 * Les contrôles de disponibilité (bus, chauffeur, permis, chevauchement, capacité) sont exécutés par
 * les déclencheurs MySQL au moment de l'UPDATE ; leurs messages sont affichés tels quels.
 */
public final class AssignmentsPage {

    private static final String ALL = "Tous les statuts";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final VBox root = Ui.page();
    private final Ctx ctx;
    private final MainController controller;

    // bandeau d'erreur de la page
    private final Label errorTitle = Ui.label("Erreur", "banner-title");
    private final Label errorText = Ui.label("", "banner-text");
    private final HBox errorBanner = bannerBox(errorTitle, errorText);

    // barre d'outils
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusBox = Ui.combo(ALL);
    private final ComboBox<String> sizeBox = Ui.combo("10", "20", "50");
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    // tableau + pagination
    private final TableView<Affectation> table = new TableView<>();
    private final Label info = Ui.label("", "card-sub");
    private final Label pageLabel = Ui.label("", "card-sub");
    private final Button prev;
    private final Button next;

    private List<Affectation> filtered = new ArrayList<>();
    private int page = 0;

    public static Node build(Ctx ctx, MainController controller) {
        return new AssignmentsPage(ctx, controller).root;
    }

    private AssignmentsPage(Ctx ctx, MainController controller) {
        this.ctx = ctx;
        this.controller = controller;

        // --- statuts du filtre (enum StatutTrajet)
        for (StatutTrajet s : StatutTrajet.values()) statusBox.getItems().add(s.libelle());
        statusBox.setPrefWidth(190);
        statusBox.setMaxWidth(190);
        sizeBox.setPrefWidth(80);
        sizeBox.setMaxWidth(80);

        // --- recherche (même structure que Ui.search)
        searchField.setPromptText("Rechercher par ID, trajet, bus ou chauffeur…");
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox search = new HBox(8, Ui.icon("search", 16, "#854F6C"), searchField);
        search.setAlignment(Pos.CENTER_LEFT);
        search.getStyleClass().add("search");
        search.setMinWidth(340);
        search.setPrefWidth(340);
        search.setMaxWidth(340);

        debounce.setOnFinished(e -> reload(true));
        searchField.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        statusBox.setOnAction(e -> reload(true));
        sizeBox.setOnAction(e -> render(true));

        Button refresh = Ui.button("Actualiser", "secondary", null, () -> {
            hideError();
            reload(false);
        });

        HBox toolbar = new HBox(12, search, statusBox, Ui.spacer(),
                Ui.label("Par page", "card-sub"), sizeBox, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        buildTable();

        prev = Ui.button("‹ Précédent", "secondary", null, () -> {
            page--;
            render(false);
        });
        next = Ui.button("Suivant ›", "secondary", null, () -> {
            page++;
            render(false);
        });
        HBox footer = new HBox(12, info, Ui.spacer(), prev, pageLabel, next);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox card = Ui.card(
                Ui.cardTitle("Affectations", "Trajets, bus et chauffeurs assignés", null, null),
                toolbar, table, footer);
        card.setSpacing(12);
        card.setPadding(new Insets(16));

        root.getChildren().setAll(errorBanner, card);
        reload(true);
    }

    // =================================================================== tableau
    private void buildTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(46);
        table.setPlaceholder(Ui.label("Aucune affectation trouvée.", "card-sub"));

        table.getColumns().add(col("ID", 60, t -> "T" + t.getIdAffectation()));
        table.getColumns().add(col("Date", 95, t -> t.getDateTrajet() == null ? "" : t.getDateTrajet().format(DATE_FMT)));
        table.getColumns().add(col("Affectation", 260, Affectation::getNomLigne));
        table.getColumns().add(col("Départ", 75, t -> t.getHeureDepart() == null ? "" : t.getHeureDepart().format(TIME_FMT)));
        table.getColumns().add(col("Arrivée", 75, t -> t.getHeureArrivee() == null ? "" : t.getHeureArrivee().format(TIME_FMT)));
        table.getColumns().add(col("Bus", 105, Affectation::getImmatriculation));
        table.getColumns().add(col("Chauffeur", 160, Affectation::getChauffeurNom));

        TableColumn<Affectation, String> statut = col("Statut", 115, t -> StatutTrajet.libelleOf(t.getStatut()));
        statut.setCellFactory(c -> new TableCell<Affectation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null || item.isEmpty() ? null : Ui.badge(item));
            }
        });
        table.getColumns().add(statut);

        TableColumn<Affectation, Void> actions = new TableColumn<>("ACTIONS");
        actions.setSortable(false);
        actions.setPrefWidth(175);
        actions.setCellFactory(c -> new TableCell<Affectation, Void>() {
            private final Button edit = new Button("Modifier");
            private final Button cancel = new Button("Annuler");
            private final HBox box = new HBox(6, edit, cancel);

            {
                edit.getStyleClass().add("table-action");
                cancel.getStyleClass().add("table-action");
                edit.setOnAction(e -> {
                    Affectation t = current();
                    if (t != null) openEdit(t);
                });
                cancel.setOnAction(e -> {
                    Affectation t = current();
                    if (t != null) openCancel(t);
                });
            }

            private Affectation current() {
                int i = getIndex();
                return i >= 0 && i < getTableView().getItems().size() ? getTableView().getItems().get(i) : null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                Affectation t = empty ? null : current();
                if (t == null) {
                    setGraphic(null);
                    return;
                }
                boolean open = isOpen(t);
                edit.setDisable(!open);
                cancel.setDisable(!open);
                setGraphic(box);
            }
        });
        table.getColumns().add(actions);
    }

    private TableColumn<Affectation, String> col(String title, double width, Function<Affectation, String> value) {
        TableColumn<Affectation, String> c = new TableColumn<>(title.toUpperCase());
        c.setSortable(false);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    /** Une affectation n'est modifiable / annulable que si elle est PLANIFIEE ou EN_COURS. */
    private static boolean isOpen(Affectation t) {
        return t.estOuverte();
    }

    // =================================================================== chargement / pagination
    /** Relit la base (recherche + filtre de statut) puis réaffiche la page courante. */
    private void reload(boolean resetPage) {
        List<Affectation> all = controller.rechercherAffectations(searchField.getText());
        String sel = statusBox.getValue();
        List<Affectation> result = new ArrayList<>();
        for (Affectation t : all) {
            if (sel == null || ALL.equals(sel) || sel.equals(StatutTrajet.libelleOf(t.getStatut()))) result.add(t);
        }
        filtered = result;
        render(resetPage);
    }

    private void render(boolean resetPage) {
        int size = Integer.parseInt(sizeBox.getValue());
        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) size));
        page = resetPage ? 0 : Math.max(0, Math.min(page, pages - 1));

        int from = page * size;
        int to = Math.min(total, from + size);
        List<Affectation> slice = new ArrayList<>(filtered.subList(from, to));
        table.getItems().setAll(slice);

        double h = 40 + Math.max(slice.size(), 3) * 46 + 4;
        table.setMinHeight(h);
        table.setPrefHeight(h);
        table.setMaxHeight(h);

        info.setText(total == 0 ? "Aucune affectation" : (from + 1) + "–" + to + " sur " + total + " affectation(s)");
        pageLabel.setText("Page " + (page + 1) + " / " + pages);
        prev.setDisable(page <= 0);
        next.setDisable(page >= pages - 1);
    }

    // =================================================================== modification bus / chauffeur
    private void openEdit(Affectation t) {
        List<Option> buses = controller.busDisponibles(t.getIdBus());
        List<Option> drivers = controller.chauffeursDisponibles(t.getIdChauffeur());

        ComboBox<Option> busBox = new ComboBox<>();
        busBox.getItems().addAll(buses);
        ComboBox<Option> driverBox = new ComboBox<>();
        driverBox.getItems().addAll(drivers);
        for (Option o : buses) if (o.id() == t.getIdBus()) busBox.setValue(o);
        for (Option o : drivers) if (o.id() == t.getIdChauffeur()) driverBox.setValue(o);

        Label mErrTitle = Ui.label("Affectation refusée", "banner-title");
        Label mErrText = Ui.label("", "banner-text");
        mErrText.setWrapText(true);
        mErrText.setMaxWidth(380);
        HBox mErr = bannerBox(mErrTitle, mErrText);

        String horaire = (t.getHeureDepart() == null ? "" : t.getHeureDepart().format(TIME_FMT))
                + " → " + (t.getHeureArrivee() == null ? "" : t.getHeureArrivee().format(TIME_FMT));
        VBox content = new VBox(14,
                mErr,
                Ui.field("Affectation", Ui.label("T" + t.getIdAffectation() + " • " + t.getNomLigne(), "opt-title")),
                Ui.field("Date et horaire", Ui.label(
                        (t.getDateTrajet() == null ? "" : t.getDateTrajet().format(DATE_FMT)) + " • " + horaire, "opt-title")),
                Ui.field("Bus", busBox),
                Ui.field("Chauffeur", driverBox));

        showModal("Modifier l’affectation T" + t.getIdAffectation(), content, close -> {
            Button save = Ui.button("Enregistrer", "primary", null, () -> {
                Option b = busBox.getValue();
                Option d = driverBox.getValue();
                if (b == null || d == null) {
                    show(mErr, mErrText, "Veuillez choisir un bus et un chauffeur.");
                    return;
                }
                if (b.id() == t.getIdBus() && d.id() == t.getIdChauffeur()) {
                    show(mErr, mErrText, "Aucune modification : le bus et le chauffeur sont inchangés.");
                    return;
                }
                Affectation fresh = controller.trouverAffectation(t.getIdAffectation());
                if (fresh == null) return;
                if (!isOpen(fresh)) {
                    show(mErr, mErrText, "Cette affectation n'est plus modifiable (statut : "
                            + StatutTrajet.libelleOf(fresh.getStatut()) + ").");
                    return;
                }
                fresh.setIdBus(b.id());
                fresh.setIdChauffeur(d.id());
                // les déclencheurs MySQL vérifient bus, chauffeur, permis, chevauchements, capacité
                if (controller.modifierAffectation(fresh)) {
                    close.run();
                    hideError();
                    reload(false);
                }
            });
            return List.of(Ui.button("Annuler", "secondary", null, close), save);
        });
    }

    // =================================================================== annulation (statut ANNULEE)
    private void openCancel(Affectation t) {
        Label mErrTitle = Ui.label("Annulation impossible", "banner-title");
        Label mErrText = Ui.label("", "banner-text");
        mErrText.setWrapText(true);
        mErrText.setMaxWidth(380);
        HBox mErr = bannerBox(mErrTitle, mErrText);

        Label msg = Ui.label("Annuler l’affectation T" + t.getIdAffectation() + " (" + t.getNomLigne() + ", "
                + (t.getDateTrajet() == null ? "" : t.getDateTrajet().format(DATE_FMT)) + " "
                + (t.getHeureDepart() == null ? "" : t.getHeureDepart().format(TIME_FMT))
                + ") ? L’enregistrement est conservé avec le statut « Annulé ».", "opt-sub");
        msg.setWrapText(true);
        msg.setMaxWidth(470);
        VBox content = new VBox(14, mErr, msg);

        showModal("Annuler l’affectation", content, close -> {
            Button confirm = Ui.button("Confirmer l’annulation", "primary", null, () -> {
                Affectation fresh = controller.trouverAffectation(t.getIdAffectation());
                if (fresh == null) return;
                if (!isOpen(fresh)) {
                    show(mErr, mErrText, "Cette affectation ne peut plus être annulée (statut : "
                            + StatutTrajet.libelleOf(fresh.getStatut()) + ").");
                    return;
                }
                // UPDATE du statut uniquement : aucune suppression
                if (controller.annulerAffectation(t.getIdAffectation())) {
                    close.run();
                    hideError();
                    reload(false);
                }
            });
            return List.of(Ui.button("Retour", "secondary", null, close), confirm);
        });
    }

    // =================================================================== fenêtre modale (même structure que AppShell.openModal)
    private void showModal(String titleText, Node content, Function<Runnable, List<Node>> footerButtons) {
        if (!(ctx instanceof Pane host)) {
            showError("Impossible d'ouvrir la fenêtre : conteneur principal introuvable.");
            return;
        }
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-backdrop");
        Runnable close = () -> host.getChildren().remove(overlay);

        Button x = new Button();
        x.setGraphic(Ui.icon("close", 18, "#522B5B"));
        x.getStyleClass().add("icon-btn");
        x.setOnAction(e -> close.run());
        HBox head = new HBox(new VBox(3, Ui.label("TRANSPORTPRO", "eyebrow"), Ui.label(titleText, "modal-title")), Ui.spacer(), x);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("modal-head");

        VBox body = new VBox(content);
        body.getStyleClass().add("modal-body");

        HBox foot = new HBox(10);
        foot.getChildren().addAll(footerButtons.apply(close));
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
        host.getChildren().add(overlay);
    }

    // =================================================================== bandeaux d'erreur (classes CSS déjà utilisées par l'ancienne page)
    private static HBox bannerBox(Label title, Label text) {
        HBox b = new HBox(12, title, text);
        b.setAlignment(Pos.CENTER_LEFT);
        b.getStyleClass().addAll("assign-banner", "error");
        b.setVisible(false);
        b.setManaged(false);
        return b;
    }

    private static void show(HBox banner, Label text, String message) {
        text.setText(message);
        banner.setVisible(true);
        banner.setManaged(true);
    }

    private void showError(String message) {
        show(errorBanner, errorText, message);
    }

    private void hideError() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }
}