package com.transportpro;

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
    private final TrajetDAO dao = new TrajetDAO();

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
    private final TableView<Trajet> table = new TableView<>();
    private final Label info = Ui.label("", "card-sub");
    private final Label pageLabel = Ui.label("", "card-sub");
    private final Button prev;
    private final Button next;

    private List<Trajet> filtered = new ArrayList<>();
    private int page = 0;

    public static Node build(Ctx ctx) {
        return new AssignmentsPage(ctx).root;
    }

    private AssignmentsPage(Ctx ctx) {
        this.ctx = ctx;

        // --- statuts du filtre : lus dans l'ENUM de la base
        try {
            for (String s : dao.findStatuts()) statusBox.getItems().add(Trajet.statutLabel(s));
        } catch (TrajetDAO.TrajetException e) {
            showError(e.getMessage());
        }
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
        table.getColumns().add(col("Trajet", 260, Trajet::getNomLigne));
        table.getColumns().add(col("Départ", 75, t -> t.getHeureDepart() == null ? "" : t.getHeureDepart().format(TIME_FMT)));
        table.getColumns().add(col("Arrivée", 75, t -> t.getHeureArrivee() == null ? "" : t.getHeureArrivee().format(TIME_FMT)));
        table.getColumns().add(col("Bus", 105, Trajet::getImmatriculation));
        table.getColumns().add(col("Chauffeur", 160, Trajet::getChauffeurNom));

        TableColumn<Trajet, String> statut = col("Statut", 115, t -> Trajet.statutLabel(t.getStatut()));
        statut.setCellFactory(c -> new TableCell<Trajet, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null || item.isEmpty() ? null : Ui.badge(item));
            }
        });
        table.getColumns().add(statut);

        TableColumn<Trajet, Void> actions = new TableColumn<>("ACTIONS");
        actions.setSortable(false);
        actions.setPrefWidth(175);
        actions.setCellFactory(c -> new TableCell<Trajet, Void>() {
            private final Button edit = new Button("Modifier");
            private final Button cancel = new Button("Annuler");
            private final HBox box = new HBox(6, edit, cancel);

            {
                edit.getStyleClass().add("table-action");
                cancel.getStyleClass().add("table-action");
                edit.setOnAction(e -> {
                    Trajet t = current();
                    if (t != null) openEdit(t);
                });
                cancel.setOnAction(e -> {
                    Trajet t = current();
                    if (t != null) openCancel(t);
                });
            }

            private Trajet current() {
                int i = getIndex();
                return i >= 0 && i < getTableView().getItems().size() ? getTableView().getItems().get(i) : null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                Trajet t = empty ? null : current();
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

    private TableColumn<Trajet, String> col(String title, double width, Function<Trajet, String> value) {
        TableColumn<Trajet, String> c = new TableColumn<>(title.toUpperCase());
        c.setSortable(false);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    /** Une affectation n'est modifiable / annulable que si elle est PLANIFIEE ou EN_COURS. */
    private static boolean isOpen(Trajet t) {
        String s = t.getStatut() == null ? "" : t.getStatut();
        return s.equals("PLANIFIEE") || s.equals("EN_COURS");
    }

    // =================================================================== chargement / pagination
    /** Relit la base (recherche + filtre de statut) puis réaffiche la page courante. */
    private void reload(boolean resetPage) {
        List<Trajet> all;
        try {
            all = dao.search(searchField.getText());
        } catch (TrajetDAO.TrajetException e) {
            showError(e.getMessage());
            all = new ArrayList<>();
        }
        String sel = statusBox.getValue();
        List<Trajet> result = new ArrayList<>();
        for (Trajet t : all) {
            if (sel == null || ALL.equals(sel) || sel.equals(Trajet.statutLabel(t.getStatut()))) result.add(t);
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
        List<Trajet> slice = new ArrayList<>(filtered.subList(from, to));
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
    private void openEdit(Trajet t) {
        List<TrajetDAO.Option> buses;
        List<TrajetDAO.Option> drivers;
        try {
            buses = dao.findAvailableBuses(t.getIdBus());
            drivers = dao.findAvailableDrivers(t.getIdChauffeur());
        } catch (TrajetDAO.TrajetException e) {
            showError(e.getMessage());
            return;
        }

        ComboBox<TrajetDAO.Option> busBox = new ComboBox<>();
        busBox.getItems().addAll(buses);
        ComboBox<TrajetDAO.Option> driverBox = new ComboBox<>();
        driverBox.getItems().addAll(drivers);
        for (TrajetDAO.Option o : buses) if (o.id() == t.getIdBus()) busBox.setValue(o);
        for (TrajetDAO.Option o : drivers) if (o.id() == t.getIdChauffeur()) driverBox.setValue(o);

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
                TrajetDAO.Option b = busBox.getValue();
                TrajetDAO.Option d = driverBox.getValue();
                if (b == null || d == null) {
                    show(mErr, mErrText, "Veuillez choisir un bus et un chauffeur.");
                    return;
                }
                if (b.id() == t.getIdBus() && d.id() == t.getIdChauffeur()) {
                    show(mErr, mErrText, "Aucune modification : le bus et le chauffeur sont inchangés.");
                    return;
                }
                try {
                    Trajet fresh = dao.findById(t.getIdAffectation());
                    if (!isOpen(fresh)) {
                        throw new TrajetDAO.TrajetException(
                                "Cette affectation n'est plus modifiable (statut : " + Trajet.statutLabel(fresh.getStatut()) + ").", null);
                    }
                    fresh.setIdBus(b.id());
                    fresh.setIdChauffeur(d.id());
                    dao.update(fresh); // les déclencheurs MySQL vérifient bus, chauffeur, permis, chevauchements, capacité
                    close.run();
                    hideError();
                    ctx.toast("Affectation T" + t.getIdAffectation() + " mise à jour");
                    reload(false);
                } catch (TrajetDAO.TrajetException ex) {
                    show(mErr, mErrText, ex.getMessage());
                }
            });
            return List.of(Ui.button("Annuler", "secondary", null, close), save);
        });
    }

    // =================================================================== annulation (statut ANNULEE)
    private void openCancel(Trajet t) {
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
                try {
                    Trajet fresh = dao.findById(t.getIdAffectation());
                    if (!isOpen(fresh)) {
                        throw new TrajetDAO.TrajetException(
                                "Cette affectation ne peut plus être annulée (statut : " + Trajet.statutLabel(fresh.getStatut()) + ").", null);
                    }
                    fresh.setStatut("ANNULEE");
                    dao.update(fresh); // UPDATE du statut uniquement : aucune suppression
                    close.run();
                    hideError();
                    ctx.toast("Affectation T" + t.getIdAffectation() + " annulée");
                    reload(false);
                } catch (TrajetDAO.TrajetException ex) {
                    show(mErr, mErrText, ex.getMessage());
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