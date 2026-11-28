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
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.emplogo.Ctx;
import com.emplogo.controller.MainController;
import com.emplogo.dao.Option;
import com.emplogo.model.Employe;
import com.emplogo.model.Inscription;
import com.emplogo.model.Trajet;
import com.emplogo.model.enums.StatutPresence;

/**
 * Module Inscriptions : CRUD complet sur la table « inscription » (ajout, modification, suppression avec
 * confirmation), recherche, filtre par statut et pagination. Les règles métier (employé actif, affectation
 * non annulée, capacité du bus, double inscription) sont contrôlées par les déclencheurs MySQL ; leurs
 * messages sont affichés tels quels.
 */
public final class RegistrationsPage {

    private static final String ALL = "Tous les statuts";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] CODES = {"CONFIRMEE", "ANNULEE"};

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
    private final TableView<Inscription> table = new TableView<>();
    private final Label info = Ui.label("", "card-sub");
    private final Label pageLabel = Ui.label("", "card-sub");
    private final Button prev;
    private final Button next;

    private List<Inscription> filtered = new ArrayList<>();
    private int page = 0;

    public static Node build(Ctx ctx, MainController controller) {
        return new RegistrationsPage(ctx, controller).root;
    }

    // ---- accès aux objets liés (le modèle Inscription contient un Employe et un Trajet)
    private static String nomEmploye(Inscription i) { return i.getEmploye() == null ? "" : i.getEmploye().getNomComplet(); }
    private static String matricule(Inscription i) { return i.getEmploye() == null ? "" : i.getEmploye().getMatricule(); }
    private static String ligne(Inscription i) { return i.getTrajet() == null ? "" : i.getTrajet().getNomLigne(); }
    private static java.time.LocalDate dateTrajet(Inscription i) { return i.getTrajet() == null ? null : i.getTrajet().getDate(); }
    private static java.time.LocalTime depart(Inscription i) { return i.getTrajet() == null ? null : i.getTrajet().getHeureDepart(); }

    private RegistrationsPage(Ctx ctx, MainController controller) {
        this.ctx = ctx;
        this.controller = controller;

        // --- statuts du filtre (colonne inscription.statut)
        for (String s : CODES) statusBox.getItems().add(Inscription.statutLabel(s));
        statusBox.setPrefWidth(190);
        statusBox.setMaxWidth(190);
        sizeBox.setPrefWidth(80);
        sizeBox.setMaxWidth(80);

        // --- recherche (même structure que Ui.search)
        searchField.setPromptText("Rechercher par employé, matricule, trajet ou ID…");
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
        Button add = Ui.button("Inscrire un employé", "primary", "plus", () -> openForm(null));

        HBox toolbar = new HBox(12, search, statusBox, Ui.spacer(),
                Ui.label("Par page", "card-sub"), sizeBox, refresh, add);
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
                Ui.cardTitle("Inscriptions", "Employés inscrits aux trajets planifiés", null, null),
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
        table.setPlaceholder(Ui.label("Aucune inscription trouvée.", "card-sub"));

        table.getColumns().add(col("ID", 55, i -> "I" + i.getIdInscription()));
        table.getColumns().add(col("Employé", 170, RegistrationsPage::nomEmploye));
        table.getColumns().add(col("Matricule", 80, RegistrationsPage::matricule));
        table.getColumns().add(col("Trajet", 230, i -> "T" + i.getIdAffectation() + " • " + ligne(i)));
        table.getColumns().add(col("Date trajet", 95, i -> dateTrajet(i) == null ? "" : dateTrajet(i).format(DATE_FMT)));
        table.getColumns().add(col("Départ", 70, i -> depart(i) == null ? "" : depart(i).format(TIME_FMT)));
        table.getColumns().add(col("Inscrit le", 95, i -> i.getDateInscription() == null ? "" : i.getDateInscription().format(DATE_FMT)));
        table.getColumns().add(col("Présence", 110, i -> i.getStatutPresence() == null ? "" : i.getStatutPresence().libelle()));

        TableColumn<Inscription, String> statut = col("Statut", 115, Inscription::getStatut);
        statut.setCellFactory(c -> new TableCell<Inscription, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null || item.isEmpty() ? null : badge(item));
            }
        });
        table.getColumns().add(statut);

        TableColumn<Inscription, Void> actions = new TableColumn<>("ACTIONS");
        actions.setSortable(false);
        actions.setPrefWidth(185);
        actions.setCellFactory(c -> new TableCell<Inscription, Void>() {
            private final Button edit = new Button("Modifier");
            private final Button delete = new Button("Supprimer");
            private final HBox box = new HBox(6, edit, delete);

            {
                edit.getStyleClass().add("table-action");
                delete.getStyleClass().add("table-action");
                edit.setOnAction(e -> {
                    Inscription i = current();
                    if (i != null) openForm(i);
                });
                delete.setOnAction(e -> {
                    Inscription i = current();
                    if (i != null) openDelete(i);
                });
            }

            private Inscription current() {
                int idx = getIndex();
                return idx >= 0 && idx < getTableView().getItems().size() ? getTableView().getItems().get(idx) : null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || current() == null ? null : box);
            }
        });
        table.getColumns().add(actions);
    }

    private TableColumn<Inscription, String> col(String title, double width, Function<Inscription, String> value) {
        TableColumn<Inscription, String> c = new TableColumn<>(title.toUpperCase());
        c.setSortable(false);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    /** Badge de statut (même structure que Ui.badge) : Confirmée = vert, Annulée = rouge. */
    private static HBox badge(String code) {
        Circle dot = new Circle(3);
        dot.getStyleClass().add("badge-dot");
        HBox b = new HBox(5, dot, new Label(Inscription.statutLabel(code)));
        b.setAlignment(Pos.CENTER);
        b.getStyleClass().addAll("badge", "CONFIRMEE".equals(code) ? "success" : "danger");
        b.setMaxWidth(Region.USE_PREF_SIZE);
        return b;
    }

    // =================================================================== chargement / pagination
    /** Relit la base (recherche + filtre de statut) puis réaffiche la page courante. */
    private void reload(boolean resetPage) {
        List<Inscription> all = controller.rechercherInscriptions(searchField.getText());
        String sel = statusBox.getValue();
        List<Inscription> result = new ArrayList<>();
        for (Inscription i : all) {
            if (sel == null || ALL.equals(sel) || sel.equals(Inscription.statutLabel(i.getStatut()))) result.add(i);
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
        List<Inscription> slice = new ArrayList<>(filtered.subList(from, to));
        table.getItems().setAll(slice);

        double h = 40 + Math.max(slice.size(), 3) * 46 + 4;
        table.setMinHeight(h);
        table.setPrefHeight(h);
        table.setMaxHeight(h);

        info.setText(total == 0 ? "Aucune inscription" : (from + 1) + "–" + to + " sur " + total + " inscription(s)");
        pageLabel.setText("Page " + (page + 1) + " / " + pages);
        prev.setDisable(page <= 0);
        next.setDisable(page >= pages - 1);
    }

    // =================================================================== ajout / modification
    /** existing == null : ajout ; sinon modification de l'inscription donnée. */
    private void openForm(Inscription existing) {
        boolean add = existing == null;
        List<Option> employes = controller.employesActifs(add ? 0 : existing.getIdEmploye());
        List<Option> affectations = controller.trajetsOuverts(add ? 0 : existing.getIdAffectation());
        List<String> codes = List.of(CODES);

        ComboBox<Option> empBox = new ComboBox<>();
        empBox.getItems().addAll(employes);
        empBox.setPromptText("Choisir un employé");
        ComboBox<Option> affBox = new ComboBox<>();
        affBox.getItems().addAll(affectations);
        affBox.setPromptText("Choisir une affectation");
        ComboBox<String> statBox = new ComboBox<>();
        for (String c : codes) statBox.getItems().add(Inscription.statutLabel(c));

        String initialStatut = add ? "CONFIRMEE" : existing.getStatut();
        statBox.getSelectionModel().select(Math.max(0, codes.indexOf(initialStatut)));
        if (!add) {
            for (Option o : employes) if (o.id() == existing.getIdEmploye()) empBox.setValue(o);
            for (Option o : affectations) if (o.id() == existing.getIdAffectation()) affBox.setValue(o);
        }

        Label mErrTitle = Ui.label(add ? "Inscription refusée" : "Modification refusée", "banner-title");
        Label mErrText = Ui.label("", "banner-text");
        mErrText.setWrapText(true);
        mErrText.setMaxWidth(380);
        HBox mErr = bannerBox(mErrTitle, mErrText);

        // BF18 : présence de l'employé pour ce trajet (modification uniquement)
        ComboBox<StatutPresence> presBox = new ComboBox<>();
        presBox.getItems().addAll(StatutPresence.values());
        presBox.setValue(add || existing.getStatutPresence() == null ? StatutPresence.NON_RENSEIGNE
                : existing.getStatutPresence());

        VBox content = new VBox(14, mErr,
                Ui.field("Employé", empBox),
                Ui.field("Affectation (trajet, date et heure)", affBox),
                Ui.field("Statut", statBox));
        if (!add) content.getChildren().add(Ui.field("Présence", presBox));

        String title = add ? "Inscrire un employé" : "Modifier l’inscription I" + existing.getIdInscription();
        showModal(title, content, close -> {
            Button save = Ui.button("Enregistrer", "primary", null, () -> {
                Option e = empBox.getValue();
                Option a = affBox.getValue();
                int si = statBox.getSelectionModel().getSelectedIndex();
                if (e == null || a == null || si < 0) {
                    show(mErr, mErrText, "Veuillez choisir un employé, une affectation et un statut.");
                    return;
                }
                String newStatut = codes.get(si);
                if (add) {
                    Inscription n = new Inscription();
                    n.setEmploye(employeRef(e.id()));
                    n.setTrajet(trajetRef(a.id()));
                    n.setStatut(newStatut);
                    // les déclencheurs MySQL contrôlent employé actif, capacité, doublon...
                    if (!controller.ajouterInscription(n)) return;
                } else {
                    Inscription fresh = controller.trouverInscription(existing.getIdInscription());
                    if (fresh == null) return;
                    boolean sameRow = fresh.getIdEmploye() == e.id() && fresh.getIdAffectation() == a.id()
                            && newStatut.equals(fresh.getStatut());
                    boolean samePresence = presBox.getValue() == fresh.getStatutPresence();
                    if (sameRow && samePresence) {
                        show(mErr, mErrText, "Aucune modification : les informations sont inchangées.");
                        return;
                    }
                    if (!sameRow) {
                        fresh.setEmploye(employeRef(e.id()));
                        fresh.setTrajet(trajetRef(a.id()));
                        fresh.setStatut(newStatut);
                        if (!controller.modifierInscription(fresh)) return;
                    }
                    if (!samePresence && !controller.enregistrerPresence(fresh, presBox.getValue())) return;
                }
                close.run();
                hideError();
                reload(add);
            });
            return List.of(Ui.button("Annuler", "secondary", null, close), save);
        });
    }

    // =================================================================== suppression (avec confirmation)
    private static Employe employeRef(int id) {
        Employe e = new Employe();
        e.setIdEmploye(id);
        return e;
    }

    /** Un Trajet daté = une affectation : son id est id_affectation. */
    private static Trajet trajetRef(int idAffectation) {
        Trajet t = new Trajet();
        t.setIdTrajet(idAffectation);
        return t;
    }

    private void openDelete(Inscription i) {
        int id = i.getIdInscription();
        int presences = controller.nombrePresences(id);
        if (presences < 0) return;
        List<String> linked = new ArrayList<>();
        DeleteDialog.addIf(linked, presences, "présence enregistrée");
        boolean alreadyCancelled = "ANNULEE".equals(i.getStatut());

        DeleteDialog.Choice choice = DeleteDialog.ask(
                "l’inscription I" + id + " de " + nomEmploye(i) + " (" + ligne(i) + ", "
                        + (dateTrajet(i) == null ? "" : dateTrajet(i).format(DATE_FMT)) + ")",
                linked, alreadyCancelled ? null : "Annuler l’inscription", true, null);
        boolean ok = switch (choice) {
            case SOFT -> controller.annulerInscription(id);
            case DELETE -> linked.isEmpty() ? controller.supprimerInscription(id)
                    : controller.supprimerInscriptionEtHistorique(id);
            case CANCEL -> false;
        };
        if (ok) {
            hideError();
            reload(false);
        }
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

    // =================================================================== bandeaux d'erreur
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