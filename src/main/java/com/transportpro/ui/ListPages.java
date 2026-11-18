package com.transportpro.ui;

import com.transportpro.bus.Bus;
import com.transportpro.bus.BusDAO;
import com.transportpro.bus.BusDialog;
import com.transportpro.chauffeur.Chauffeur;
import com.transportpro.chauffeur.ChauffeurDAO;
import com.transportpro.chauffeur.ChauffeurDialog;
import com.transportpro.employee.Employee;
import com.transportpro.employee.EmployeeDAO;
import com.transportpro.employee.EmployeeDialog;
import com.transportpro.incident.Incident;
import com.transportpro.incident.IncidentDAO;
import com.transportpro.incident.IncidentDialog;
import com.transportpro.trajet.Trajet;
import com.transportpro.trajet.TrajetDAO;
import com.transportpro.trajet.TrajetDialog;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.util.Duration;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Écrans « liste » : Employés, Bus, Chauffeurs, Trajets, Incidents, Comptes. */
public final class ListPages {
    private ListPages() {}

    /** Nombre de lignes affichées par page. */
    private static final int PAGE_SIZE = 8;

    private static final BusDAO BUS_DAO = new BusDAO();
    private static final EmployeeDAO EMP_DAO = new EmployeeDAO();
    private static final ChauffeurDAO DRIVER_DAO = new ChauffeurDAO();
    private static final TrajetDAO TRIP_DAO = new TrajetDAO();
    private static final IncidentDAO INCIDENT_DAO = new IncidentDAO();
    private static final DateTimeFormatter DATETIME_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm");
    private static final String ALL_TYPES = "Tous les types";
    private static final String ALL_BUSES = "Tous les bus";
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ALL_STATUS = "Tous les statuts";

    /**
     * loader     : fournit les lignes (base de données ou données de démonstration).
     * dbSearch   : si non null, la recherche est faite par la base (SQL) et non en mémoire.
     * searchCols : colonnes (index dans la ligne) utilisées par la recherche ; null = toutes les colonnes visibles.
     */
    private record Cfg(String action, String[] headers, Supplier<String[][]> loader,
                       Function<String, String[][]> dbSearch,
                       int badgeCol, String note, int[] searchCols) {}

    public static Node build(Page p, Ctx ctx) {
        Cfg c = switch (p) {

            // Employés : ligne = [id, matricule, nom + prénom, téléphone, adresse, statut] (recherche SQL)
            case EMPLOYEES -> new Cfg("Ajouter un employé",
                    new String[]{"ID", "Matricule", "Nom", "Téléphone",
                            "Point d’embarquement", "Statut"},
                    () -> searchEmployees(""), ListPages::searchEmployees, 5, null,
                    null);

            // Bus : ligne = [immatriculation, capacité, trajet, statut, id_bus, marque, modèle]
            case BUSES -> new Cfg("Ajouter un bus",
                    new String[]{"Immatriculation", "Capacité",
                            "Prochain trajet", "Statut"},
                    ListPages::loadBuses, null, 3,
                    "Un bus en maintenance ou hors service ne peut pas être affecté.",
                    new int[]{0, 1, 3, 5, 6});

            // Chauffeurs : ligne = [nom + prénom, téléphone, n° de permis, statut, id_chauffeur] (recherche SQL)
            case DRIVERS -> new Cfg("Ajouter un chauffeur",
                    new String[]{"Nom", "Téléphone", "N° de permis",
                            "Disponibilité"},
                    () -> searchDrivers(""), ListPages::searchDrivers, 3, null, null);

            // Trajets : ligne = [T + n°, date, heure de départ, destination, bus, chauffeur, inscrits / capacité,
            //                    statut, id_affectation] (recherche SQL)
            case TRIPS -> new Cfg("Créer un trajet",
                    new String[]{"ID", "Date", "Départ", "Destination",
                            "Bus", "Chauffeur", "Inscrits", "Statut"},
                    () -> searchTrips(""), ListPages::searchTrips, 7, null, null);

            // Incidents : ligne = [date/heure, type, trajet, bus, chauffeur, gravité, statut, description,
            //                     id_incident, date (jj/mm/aaaa), immatriculation] (recherche SQL)
            case INCIDENTS -> new Cfg(ctx.isDriver() ? "Signaler un incident" : "Enregistrer un incident",
                    new String[]{"Date / heure", "Type", "Trajet", "Bus",
                            "Chauffeur", "Gravité", "Statut", "Description"},
                    () -> searchIncidents(""), ListPages::searchIncidents, 6, null, null);

            case ACCOUNTS -> new Cfg("Créer un compte",
                    new String[]{"Identifiant", "Rôle",
                            "Dernière connexion", "Statut"},
                    () -> Data.ACCOUNTS, null, 3, null, null);

            default -> throw new IllegalArgumentException(
                    "Page non gérée : " + p);
        };

        VBox card = Ui.card();
        card.getStyleClass().add("main-card");

        if (c.note() != null) {
            HBox banner = new HBox(
                    8,
                    Ui.icon("incidents", 17, "#8A5A00"),
                    Ui.label(c.note(), "info-text")
            );
            banner.getStyleClass().add("info-banner");
            banner.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(banner);
        }

        HBox searchBox = Ui.search("Rechercher...", 210);
        TextField searchField = (TextField) searchBox.getChildren().get(1);

        // Chauffeurs : les statuts du filtre viennent de la base ; autres pages : liste inchangée
        ComboBox<String> statusBox = p == Page.DRIVERS
                ? Ui.combo(driverStatusItems())
                : p == Page.TRIPS
                        ? Ui.combo(tripStatusItems())
                        : p == Page.INCIDENTS
                                ? Ui.combo(incidentTypeItems())
                                : Ui.combo(ALL_STATUS, "Actif");

        HBox filters = new HBox(
                8,
                searchBox,
                statusBox
        );
        filters.setAlignment(Pos.CENTER_LEFT);

        DatePicker dateFilter = null;
        if (p == Page.TRIPS || p == Page.INCIDENTS) {
            dateFilter = new DatePicker();
            dateFilter.setPromptText("jj/mm/aaaa");
            filters.getChildren().add(dateFilter);
        }
        final DatePicker tripDate = dateFilter;

        // Incidents (BF22) : filtre supplémentaire par bus
        ComboBox<String> busFilter = p == Page.INCIDENTS ? Ui.combo(incidentBusItems()) : null;
        if (busFilter != null) filters.getChildren().add(busFilter);

        // Tableau + pagination pilotés par un même objet (recherche, pages, rafraîchissement)
        Listing listing = new Listing(c, ctx, p);

        Runnable onAdd = p == Page.BUSES
                ? () -> addBus(listing, ctx)
                : p == Page.EMPLOYEES
                        ? () -> addEmployee(listing, ctx)
                        : p == Page.DRIVERS
                                ? () -> addDriver(listing, ctx)
                                : p == Page.TRIPS
                                        ? () -> addTrip(listing, ctx)
                                        : p == Page.INCIDENTS
                                                ? () -> addIncident(listing, ctx)
                                                : () -> ctx.openModal(c.action());

        HBox toolbar = new HBox(
                filters,
                Ui.spacer(),
                Ui.button(c.action(), "primary", "plus", onAdd)
        );
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 15, 0));

        // Recherche pendant la saisie (Employés, Chauffeurs : requête SQL après une courte pause de frappe)
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(e -> listing.setQuery(searchField.getText()));
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (c.dbSearch() != null) debounce.playFromStart();
            else listing.setQuery(newV);
        });

        if (p == Page.EMPLOYEES) {
            wireActions(listing.table(),
                    row -> editEmployee(row, listing, ctx),
                    row -> deleteEmployee(row, listing, ctx));
        } else if (p == Page.BUSES) {
            wireActions(listing.table(),
                    row -> editBus(row, listing, ctx),
                    row -> deleteBus(row, listing, ctx));
        } else if (p == Page.DRIVERS) {
            wireActions(listing.table(),
                    row -> editDriver(row, listing, ctx),
                    row -> deleteDriver(row, listing, ctx));
            // filtre « statut » : s'applique sur la colonne Disponibilité (index 3)
            statusBox.setOnAction(e ->
                    listing.setExtraFilter(rowFilter(statusBox.getValue(), 3, null)));
        } else if (p == Page.TRIPS) {
            wireActions(listing.table(),
                    row -> editTrip(row, listing, ctx),
                    row -> deleteTrip(row, listing, ctx));
            // filtres « statut » (colonne 7) et « date » (colonne 1)
            Runnable applyFilters = () ->
                    listing.setExtraFilter(rowFilter(statusBox.getValue(), 7, tripDate.getValue()));
            statusBox.setOnAction(e -> applyFilters.run());
            tripDate.valueProperty().addListener((obs, o, n) -> applyFilters.run());
        } else if (p == Page.INCIDENTS) {
            if (ctx.isDriver()) {
                // BF21 : le chauffeur signale et consulte, sans modifier ni supprimer
                wireActions(listing.table(), ListPages::showIncident, null);
            } else {
                wireActions(listing.table(),
                        row -> editIncident(row, listing, ctx),
                        row -> deleteIncident(row, listing, ctx));
            }
            // BF22 : filtres type (colonne 1), date (colonne 9) et bus (colonne 10)
            Runnable applyFilters = () -> listing.setExtraFilter(
                    incidentFilter(statusBox.getValue(), tripDate.getValue(), busFilter.getValue()));
            statusBox.setOnAction(e -> applyFilters.run());
            busFilter.setOnAction(e -> applyFilters.run());
            tripDate.valueProperty().addListener((obs, o, n) -> applyFilters.run());
        }

        card.getChildren().addAll(toolbar, listing.table(), listing.footer());

        return Ui.page(card);
    }

    // ================================================================== tableau filtrable + pages
    private static final class Listing {
        private final Cfg cfg;
        private final TableView<String[]> table;
        private final Label info = Ui.label("", "muted");
        private final HBox pages = new HBox(4);
        private final HBox footer;

        private List<String[]> all = new ArrayList<>();
        private List<String[]> filtered = new ArrayList<>();
        private String query = "";
        private int page = 0;
        private Predicate<String[]> extra = null;   // filtre supplémentaire (ex. statut des chauffeurs)

        Listing(Cfg cfg, Ctx ctx, Page p) {
            this.cfg = cfg;
            this.table = Ui.table(cfg.headers(), new String[0][], cfg.badgeCol(),
                    () -> ctx.openModal("Détail — " + p.title));

            footer = new HBox(info, Ui.spacer(), pages);
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setPadding(new Insets(14, 0, 0, 0));

            reload();
        }

        TableView<String[]> table() { return table; }
        HBox footer() { return footer; }

        /** Relit les données (base de données) en gardant la recherche et la page courantes. */
        void reload() {
            String[][] rows = cfg.dbSearch() != null
                    ? cfg.dbSearch().apply(query.trim())
                    : cfg.loader().get();
            all = new ArrayList<>(Arrays.asList(rows));
            applyFilter();
        }

        void setQuery(String q) {
            query = q == null ? "" : q;
            page = 0;
            if (cfg.dbSearch() != null) reload();   // la base filtre
            else applyFilter();                     // filtre en mémoire
        }

        void setExtraFilter(Predicate<String[]> f) {
            extra = f;
            page = 0;
            applyFilter();
        }

        void goTo(int newPage) {
            page = newPage;
            applyFilter();
        }

        private void applyFilter() {
            // si la base a déjà filtré (Employés, Chauffeurs), on n'applique pas de second filtre texte
            String q = cfg.dbSearch() != null ? "" : normalize(query).trim();
            String[] tokens = q.isEmpty() ? new String[0] : q.split("\\s+");

            filtered = new ArrayList<>();
            for (String[] row : all) {
                if (matches(row, tokens) && (extra == null || extra.test(row))) filtered.add(row);
            }

            if (page > pageCount() - 1) page = pageCount() - 1;
            if (page < 0) page = 0;
            render();
        }

        private boolean matches(String[] row, String[] tokens) {
            if (tokens.length == 0) return true;
            StringBuilder sb = new StringBuilder();
            if (cfg.searchCols() != null) {
                for (int i : cfg.searchCols()) {
                    if (i < row.length && row[i] != null) sb.append(normalize(row[i])).append(' ');
                }
            } else {
                for (int i = 0; i < cfg.headers().length && i < row.length; i++) {
                    if (row[i] != null) sb.append(normalize(row[i])).append(' ');
                }
            }
            String hay = sb.toString();
            for (String t : tokens) {
                if (!hay.contains(t)) return false;
            }
            return true;
        }

        private int pageCount() {
            return Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        }

        private void render() {
            int from = page * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, filtered.size());
            List<String[]> slice = from < to ? filtered.subList(from, to) : List.of();

            table.getItems().setAll(slice);
            double h = 40 + slice.size() * 46 + 4;
            Ui.fixHeight(table, h);

            info.setText(filtered.isEmpty()
                    ? "Aucun résultat"
                    : "Affichage de " + (from + 1) + " à " + to + " sur " + filtered.size());

            renderPages();
        }

        private void renderPages() {
            pages.getChildren().clear();
            int total = pageCount();

            pages.getChildren().add(pageButton("‹", false, page > 0, page - 1));

            int start = Math.max(0, Math.min(page - 2, total - 5));
            int end = Math.min(total, start + 5);
            for (int i = start; i < end; i++) {
                pages.getChildren().add(pageButton(String.valueOf(i + 1), i == page, true, i));
            }

            pages.getChildren().add(pageButton("›", false, page < total - 1, page + 1));
        }

        private Button pageButton(String text, boolean active, boolean enabled, int target) {
            Button b = new Button(text);
            b.getStyleClass().add("page-btn");
            if (active) b.getStyleClass().add("active");
            b.setDisable(!enabled);
            b.setOnAction(e -> goTo(target));
            return b;
        }
    }

    /** Minuscules et sans accents : « Éric » est trouvé en tapant « eric ». */
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    // ================================================================== Employés (MariaDB)
    private static String nz(String s) { return s == null ? "" : s; }

    /** Lignes : [id, matricule, nom + prénom, téléphone, adresse, statut] ; texte vide = tous. */
    private static String[][] searchEmployees(String text) {
        try {
            List<Employee> list = EMP_DAO.search(text);
            String[][] rows = new String[list.size()][];
            for (int i = 0; i < rows.length; i++) {
                Employee e = list.get(i);
                rows[i] = new String[]{
                        String.valueOf(e.getIdEmploye()),
                        nz(e.getMatricule()),
                        (nz(e.getNom()) + " " + nz(e.getPrenom())).trim(),
                        nz(e.getTelephone()),
                        nz(e.getAdresse()),
                        e.isActif() ? "Actif" : "Inactif"
                };
            }
            return rows;
        } catch (EmployeeDAO.EmployeeException e) {
            e.printStackTrace();
            showError("Impossible de charger les employés", e.getMessage());
            return new String[0][];
        }
    }

    private static void addEmployee(Listing listing, Ctx ctx) {
        EmployeeDialog.showAdd().ifPresent(e -> {
            try {
                EMP_DAO.insert(e);
                listing.reload();
                ctx.toast("Employé " + e.getNom() + " " + e.getPrenom() + " ajouté");
            } catch (EmployeeDAO.EmployeeException ex) {
                showError("Ajout impossible", ex.getMessage());
            }
        });
    }

    private static void deleteEmployee(String[] row, Listing listing, Ctx ctx) {
        int id = Integer.parseInt(row[0]);
        try {
            EmployeeDAO.Usage u = EMP_DAO.usage(id);
            List<String> linked = new ArrayList<>();
            if (u.chauffeur() > 0) linked.add("une fiche chauffeur"
                    + (u.trajetsConduits() > 0 ? " avec " + u.trajetsConduits() + " trajet(s)" : ""));
            DeleteDialog.addIf(linked, u.inscriptions(), "inscription(s) à des trajets");
            DeleteDialog.addIf(linked, u.presences(), "présence(s) enregistrée(s)");
            DeleteDialog.addIf(linked, u.comptes(), "compte utilisateur");
            boolean alreadyInactive = "Inactif".equals(row[5]);

            DeleteDialog.Choice choice = DeleteDialog.ask(
                    "l'employé " + row[2] + " (" + row[1] + ")", linked,
                    alreadyInactive ? null : "Désactiver",
                    u.canDeleteWithHistory(),
                    "Suppression impossible : ses trajets de chauffeur concernent d'autres employés."
                            + (alreadyInactive ? " Il est déjà désactivé." : ""));
            switch (choice) {
                case SOFT -> {
                    EMP_DAO.deactivate(id);
                    ctx.toast("Employé " + row[2] + " désactivé (historique conservé)");
                }
                case DELETE -> {
                    if (u.any()) EMP_DAO.deleteWithHistory(id);
                    else EMP_DAO.delete(id);
                    ctx.toast("Employé " + row[2] + " supprimé");
                }
                case CANCEL -> { return; }
            }
            listing.reload();
        } catch (EmployeeDAO.EmployeeException ex) {
            showError("Suppression impossible", ex.getMessage());
        }
    }

    private static void editEmployee(String[] row, Listing listing, Ctx ctx) {
        try {
            Employee current = EMP_DAO.findById(Integer.parseInt(row[0]));
            EmployeeDialog.showEdit(current).ifPresent(e -> {
                try {
                    EMP_DAO.update(e);
                    listing.reload();
                    ctx.toast("Employé " + e.getNom() + " " + e.getPrenom() + " modifié");
                } catch (EmployeeDAO.EmployeeException ex) {
                    showError("Modification impossible", ex.getMessage());
                }
            });
        } catch (EmployeeDAO.EmployeeException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    // ================================================================== Bus (MariaDB)
    /** Ligne : [immatriculation, capacité, prochain trajet, statut, id_bus, marque, modèle]. */
    private static String[][] loadBuses() {
        try {
            List<Bus> buses = BUS_DAO.findAll();
            String[][] rows = new String[buses.size()][];
            for (int i = 0; i < rows.length; i++) {
                Bus b = buses.get(i);
                rows[i] = new String[]{
                        b.getImmatriculation(),
                        b.getCapacite() + " places",
                        "—",
                        Bus.statutLabel(b.getStatut()),
                        String.valueOf(b.getIdBus()),
                        nz(b.getMarque()),
                        nz(b.getModele())
                };
            }
            return rows;
        } catch (BusDAO.BusException e) {
            showError("Impossible de charger les bus", e.getMessage());
            return new String[0][];
        }
    }

    private static void addBus(Listing listing, Ctx ctx) {
        BusDialog.showAdd().ifPresent(b -> {
            try {
                BUS_DAO.insert(b);
                listing.reload();
                ctx.toast("Bus " + b.getImmatriculation() + " ajouté");
            } catch (BusDAO.BusException e) {
                showError("Ajout impossible", e.getMessage());
            }
        });
    }

    private static void editBus(String[] row, Listing listing, Ctx ctx) {
        try {
            Bus current = BUS_DAO.findById(Integer.parseInt(row[4]));
            BusDialog.showEdit(current).ifPresent(b -> {
                try {
                    BUS_DAO.update(b);
                    listing.reload();
                    ctx.toast("Bus " + b.getImmatriculation() + " modifié");
                } catch (BusDAO.BusException e) {
                    showError("Modification impossible", e.getMessage());
                }
            });
        } catch (BusDAO.BusException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private static void deleteBus(String[] row, Listing listing, Ctx ctx) {
        int id = Integer.parseInt(row[4]);
        try {
            BusDAO.Usage u = BUS_DAO.usage(id);
            List<String> linked = new ArrayList<>();
            DeleteDialog.addIf(linked, u.affectations(), "trajet(s) planifié(s) ou effectué(s)");
            DeleteDialog.addIf(linked, u.maintenances(), "maintenance(s)");
            boolean alreadyOut = "Hors service".equals(row[3]);

            DeleteDialog.Choice choice = DeleteDialog.ask("le bus " + row[0], linked,
                    alreadyOut ? null : "Mettre hors service",
                    u.canDeleteWithHistory(),
                    "Suppression impossible : l'historique des trajets doit être conservé."
                            + (alreadyOut ? " Le bus est déjà hors service." : ""));
            switch (choice) {
                case SOFT -> {
                    BUS_DAO.retire(id);
                    ctx.toast("Bus " + row[0] + " mis hors service");
                }
                case DELETE -> {
                    if (u.any()) BUS_DAO.deleteWithHistory(id);
                    else BUS_DAO.delete(id);
                    ctx.toast("Bus " + row[0] + " supprimé");
                }
                case CANCEL -> { return; }
            }
            listing.reload();
        } catch (BusDAO.BusException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    // ================================================================== Chauffeurs (MariaDB)
    /** Éléments du filtre « statut » : « Tous les statuts » + les statuts de la base (ENUM). */
    private static String[] driverStatusItems() {
        List<String> items = new ArrayList<>();
        items.add(ALL_STATUS);
        try {
            for (String s : DRIVER_DAO.findStatuts()) items.add(Chauffeur.statutLabel(s));
        } catch (ChauffeurDAO.ChauffeurException e) {
            e.printStackTrace();
            items.add("Actif");
        }
        return items.toArray(new String[0]);
    }

    /**
     * Lignes : [nom + prénom, téléphone, n° de permis, statut, id_chauffeur] ; texte vide = tous.
     * La recherche est faite en SQL par ChauffeurDAO.search() (nom, prénom, téléphone, matricule, permis).
     */
    private static String[][] searchDrivers(String text) {
        try {
            List<Chauffeur> list = DRIVER_DAO.search(text);
            String[][] rows = new String[list.size()][];
            for (int i = 0; i < rows.length; i++) {
                Chauffeur ch = list.get(i);
                rows[i] = new String[]{
                        nz(ch.getNomComplet()),
                        nz(ch.getTelephone()),
                        nz(ch.getNumeroPermis()),
                        Chauffeur.statutLabel(ch.getStatut()),
                        String.valueOf(ch.getIdChauffeur())
                };
            }
            return rows;
        } catch (ChauffeurDAO.ChauffeurException e) {
            e.printStackTrace();
            showError("Impossible de charger les chauffeurs", e.getMessage());
            return new String[0][];
        }
    }

    private static void addDriver(Listing listing, Ctx ctx) {
        ChauffeurDialog.showAdd().ifPresent(ch -> {
            try {
                DRIVER_DAO.insert(ch);
                String nom = DRIVER_DAO.findById(ch.getIdChauffeur()).getNomComplet();
                listing.reload();
                ctx.toast("Chauffeur " + nom + " ajouté");
            } catch (ChauffeurDAO.ChauffeurException ex) {
                showError("Ajout impossible", ex.getMessage());
            }
        });
    }

    private static void editDriver(String[] row, Listing listing, Ctx ctx) {
        try {
            Chauffeur current = DRIVER_DAO.findById(Integer.parseInt(row[4]));
            ChauffeurDialog.showEdit(current).ifPresent(ch -> {
                try {
                    DRIVER_DAO.update(ch);
                    listing.reload();
                    ctx.toast("Chauffeur " + current.getNomComplet() + " modifié");
                } catch (ChauffeurDAO.ChauffeurException ex) {
                    showError("Modification impossible", ex.getMessage());
                }
            });
        } catch (ChauffeurDAO.ChauffeurException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private static void deleteDriver(String[] row, Listing listing, Ctx ctx) {
        int id = Integer.parseInt(row[4]);
        try {
            int trajets = DRIVER_DAO.countTrajets(id);
            List<String> linked = new ArrayList<>();
            DeleteDialog.addIf(linked, trajets, "trajet(s) planifié(s) ou effectué(s)");
            boolean alreadyInactive = row[3].equals(Chauffeur.statutLabel("INACTIF"))
                    || row[3].equals(Chauffeur.statutLabel("SUSPENDU"));

            DeleteDialog.Choice choice = DeleteDialog.ask(
                    "le chauffeur " + row[0] + " (permis " + row[2] + ")", linked,
                    alreadyInactive ? null : "Désactiver",
                    false,
                    "Suppression impossible : l'historique des trajets doit être conservé."
                            + (alreadyInactive ? " Le chauffeur est déjà désactivé." : ""));
            switch (choice) {
                case SOFT -> {
                    DRIVER_DAO.deactivate(id);
                    ctx.toast("Chauffeur " + row[0] + " désactivé");
                }
                case DELETE -> {
                    DRIVER_DAO.delete(id);   // seulement possible sans trajet
                    ctx.toast("Chauffeur " + row[0] + " supprimé (l'employé est conservé)");
                }
                case CANCEL -> { return; }
            }
            listing.reload();
        } catch (ChauffeurDAO.ChauffeurException ex) {
            showError("Suppression impossible", ex.getMessage());
        }
    }

    // ================================================================== Trajets (MariaDB)
    /** Éléments du filtre « statut » : « Tous les statuts » + les statuts de la base (ENUM). */
    private static String[] tripStatusItems() {
        List<String> items = new ArrayList<>();
        items.add(ALL_STATUS);
        try {
            for (String s : TRIP_DAO.findStatuts()) items.add(Trajet.statutLabel(s));
        } catch (TrajetDAO.TrajetException e) {
            e.printStackTrace();
            items.add("Planifié");
        }
        return items.toArray(new String[0]);
    }

    /**
     * Filtre en mémoire sur le statut (colonne statusCol) et, pour les trajets, la date (colonne 1).
     * Retourne null s'il n'y a rien à filtrer.
     */
    private static Predicate<String[]> rowFilter(String status, int statusCol, LocalDate date) {
        boolean byStatus = status != null && !status.equals(ALL_STATUS);
        if (!byStatus && date == null) return null;
        String d = date == null ? null : date.format(DATE_FR);
        return row -> (!byStatus || status.equals(row[statusCol])) && (d == null || d.equals(row[1]));
    }

    /**
     * Lignes : [T + n°, date, heure de départ, destination, bus, chauffeur, inscrits / capacité,
     * statut, id_affectation] ; texte vide = tous. La recherche est faite en SQL par TrajetDAO.search().
     */
    private static String[][] searchTrips(String text) {
        try {
            List<Trajet> list = TRIP_DAO.search(text);
            String[][] rows = new String[list.size()][];
            for (int i = 0; i < rows.length; i++) {
                Trajet t = list.get(i);
                rows[i] = new String[]{
                        "T" + t.getIdAffectation(),
                        t.getDateTrajet() == null ? "" : t.getDateTrajet().format(DATE_FR),
                        t.getHeureDepart() == null ? "" : t.getHeureDepart().toString(),
                        nz(t.getLieuArrivee()),
                        nz(t.getImmatriculation()),
                        nz(t.getChauffeurNom()),
                        t.getInscrits() + " / " + t.getCapacite(),
                        Trajet.statutLabel(t.getStatut()),
                        String.valueOf(t.getIdAffectation())
                };
            }
            return rows;
        } catch (TrajetDAO.TrajetException e) {
            e.printStackTrace();
            showError("Impossible de charger les trajets", e.getMessage());
            return new String[0][];
        }
    }

    private static void addTrip(Listing listing, Ctx ctx) {
        TrajetDialog.showAdd().ifPresent(t -> {
            try {
                TRIP_DAO.insert(t);
                listing.reload();
                ctx.toast("Trajet T" + t.getIdAffectation() + " créé");
            } catch (TrajetDAO.TrajetException ex) {
                showError("Ajout impossible", ex.getMessage());
            }
        });
    }

    private static void editTrip(String[] row, Listing listing, Ctx ctx) {
        try {
            Trajet current = TRIP_DAO.findById(Integer.parseInt(row[8]));
            TrajetDialog.showEdit(current).ifPresent(t -> {
                try {
                    TRIP_DAO.update(t);
                    listing.reload();
                    ctx.toast("Trajet T" + t.getIdAffectation() + " modifié");
                } catch (TrajetDAO.TrajetException ex) {
                    showError("Modification impossible", ex.getMessage());
                }
            });
        } catch (TrajetDAO.TrajetException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private static void deleteTrip(String[] row, Listing listing, Ctx ctx) {
        int id = Integer.parseInt(row[8]);
        try {
            TrajetDAO.Usage u = TRIP_DAO.usage(id);
            List<String> linked = new ArrayList<>();
            DeleteDialog.addIf(linked, u.inscriptions(), "inscription(s)");
            DeleteDialog.addIf(linked, u.presences(), "présence(s) enregistrée(s)");
            DeleteDialog.addIf(linked, u.incidents(), "incident(s)");
            boolean alreadyCancelled = row[7].equals(Trajet.statutLabel("ANNULEE"));

            DeleteDialog.Choice choice = DeleteDialog.ask(
                    "le trajet " + row[0] + " du " + row[1] + " à " + row[2] + " vers " + row[3], linked,
                    alreadyCancelled ? null : "Annuler le trajet",
                    true, null);
            switch (choice) {
                case SOFT -> {
                    TRIP_DAO.cancel(id);
                    ctx.toast("Trajet " + row[0] + " annulé");
                }
                case DELETE -> {
                    if (u.any()) TRIP_DAO.deleteWithHistory(id);
                    else TRIP_DAO.delete(id);
                    ctx.toast("Trajet " + row[0] + " supprimé");
                }
                case CANCEL -> { return; }
            }
            listing.reload();
        } catch (TrajetDAO.TrajetException ex) {
            showError("Suppression impossible", ex.getMessage());
        }
    }

    // ================================================================== Incidents (MariaDB)
    /** Éléments du filtre « type » : « Tous les types » + les types de la base (ENUM). */
    private static String[] incidentTypeItems() {
        List<String> items = new ArrayList<>();
        items.add(ALL_TYPES);
        try {
            for (String s : INCIDENT_DAO.findTypes()) items.add(Incident.typeLabel(s));
        } catch (IncidentDAO.IncidentException e) {
            e.printStackTrace();
        }
        return items.toArray(new String[0]);
    }

    /** Éléments du filtre « bus » : « Tous les bus » + les immatriculations. */
    private static String[] incidentBusItems() {
        List<String> items = new ArrayList<>();
        items.add(ALL_BUSES);
        try {
            for (IncidentDAO.Option o : INCIDENT_DAO.findBuses()) items.add(o.label());
        } catch (IncidentDAO.IncidentException e) {
            e.printStackTrace();
        }
        return items.toArray(new String[0]);
    }

    /** BF22 : filtre en mémoire par type (colonne 1), date (colonne 9) et bus (colonne 10). */
    private static Predicate<String[]> incidentFilter(String type, LocalDate date, String bus) {
        boolean byType = type != null && !type.equals(ALL_TYPES);
        boolean byBus = bus != null && !bus.equals(ALL_BUSES);
        if (!byType && !byBus && date == null) return null;
        String d = date == null ? null : date.format(DATE_FR);
        return row -> (!byType || type.equals(row[1]))
                && (d == null || d.equals(row[9]))
                && (!byBus || bus.equals(row[10]));
    }

    /**
     * Lignes : [date/heure, type, trajet, bus, chauffeur, gravité, statut, description (abrégée),
     * id_incident, date (jj/mm/aaaa), immatriculation] ; texte vide = tous.
     */
    private static String[][] searchIncidents(String text) {
        try {
            List<Incident> list = INCIDENT_DAO.search(text);
            String[][] rows = new String[list.size()][];
            for (int i = 0; i < rows.length; i++) {
                Incident inc = list.get(i);
                String desc = nz(inc.getDescription()).replace('\n', ' ');
                rows[i] = new String[]{
                        inc.getDateIncident() == null ? "" : inc.getDateIncident().format(DATETIME_FR),
                        Incident.typeLabel(inc.getTypeIncident()),
                        "T" + inc.getIdAffectation() + " • " + nz(inc.getNomLigne()),
                        nz(inc.getImmatriculation()),
                        nz(inc.getChauffeurNom()),
                        Incident.graviteLabel(inc.getGravite()),
                        Incident.statutLabel(inc.getStatutTraitement()),
                        desc.length() > 60 ? desc.substring(0, 57) + "…" : desc,
                        String.valueOf(inc.getIdIncident()),
                        inc.getDateIncident() == null ? "" : inc.getDateIncident().toLocalDate().format(DATE_FR),
                        nz(inc.getImmatriculation())
                };
            }
            return rows;
        } catch (IncidentDAO.IncidentException e) {
            e.printStackTrace();
            showError("Impossible de charger les incidents", e.getMessage());
            return new String[0][];
        }
    }

    private static void addIncident(Listing listing, Ctx ctx) {
        IncidentDialog.showAdd(ctx.isDriver()).ifPresent(inc -> {
            try {
                INCIDENT_DAO.insert(inc);
                listing.reload();
                ctx.toast("Incident INC" + inc.getIdIncident() + " enregistré");
            } catch (IncidentDAO.IncidentException ex) {
                showError("Enregistrement impossible", ex.getMessage());
            }
        });
    }

    private static void editIncident(String[] row, Listing listing, Ctx ctx) {
        try {
            Incident current = INCIDENT_DAO.findById(Integer.parseInt(row[8]));
            IncidentDialog.showEdit(current).ifPresent(inc -> {
                try {
                    INCIDENT_DAO.update(inc);
                    listing.reload();
                    ctx.toast("Incident INC" + inc.getIdIncident() + " modifié");
                } catch (IncidentDAO.IncidentException ex) {
                    showError("Modification impossible", ex.getMessage());
                }
            });
        } catch (IncidentDAO.IncidentException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private static void deleteIncident(String[] row, Listing listing, Ctx ctx) {
        DeleteDialog.Choice choice = DeleteDialog.ask(
                "l'incident INC" + row[8] + " (" + row[1] + ", " + row[0] + ")", List.of(), null, true, null);
        if (choice != DeleteDialog.Choice.DELETE) return;
        try {
            INCIDENT_DAO.delete(Integer.parseInt(row[8]));
            listing.reload();
            ctx.toast("Incident INC" + row[8] + " supprimé");
        } catch (IncidentDAO.IncidentException ex) {
            showError("Suppression impossible", ex.getMessage());
        }
    }

    /** Détail en lecture seule (profil chauffeur). */
    private static void showIncident(String[] row) {
        try {
            Incident inc = INCIDENT_DAO.findById(Integer.parseInt(row[8]));
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Incident INC" + inc.getIdIncident());
            a.setHeaderText(Incident.typeLabel(inc.getTypeIncident()) + " • " + row[0]);
            a.setContentText("Trajet : " + row[2] + "\nBus : " + row[3] + "\nChauffeur : " + row[4]
                    + "\nGravité : " + row[5] + "\nStatut : " + row[6]
                    + "\nDéclaré par : " + Incident.modeLabel(inc.getModeDeclaration())
                    + (inc.getDeclarant().isEmpty() ? "" : " (" + inc.getDeclarant() + ")")
                    + "\n\n" + inc.getDescription());
            a.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            a.showAndWait();
        } catch (IncidentDAO.IncidentException e) {
            showError("Lecture impossible", e.getMessage());
        }
    }

    // ================================================================== boutons Modifier / •••
    /**
     * Relie « Modifier », « ••• » et le clic sur une ligne à la ligne réellement concernée.
     * onDelete peut être null : le menu ••• ne propose alors que « Modifier ».
     * La ligne est lue au moment du clic (jamais mémorisée), car JavaFX réutilise les cellules.
     */
    @SuppressWarnings("unchecked")
    private static void wireActions(TableView<String[]> tv,
                                    Consumer<String[]> onEdit, Consumer<String[]> onDelete) {
        tv.setRowFactory(t -> {
            TableRow<String[]> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (!r.isEmpty() && r.getItem() != null) onEdit.accept(r.getItem());
            });
            return r;
        });

        TableColumn<String[], String> actions =
                (TableColumn<String[], String>) tv.getColumns().get(tv.getColumns().size() - 1);

        // valeur différente pour chaque ligne : force JavaFX à rafraîchir la cellule à chaque changement de page
        actions.setCellValueFactory(cd -> new SimpleStringProperty(String.join("|", cd.getValue())));

        actions.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Button edit = new Button(onDelete == null ? "Voir" : "Modifier");
                edit.getStyleClass().add("table-action");
                edit.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
                edit.setOnAction(e -> {
                    String[] row = getTableRow().getItem();
                    if (row != null) onEdit.accept(row);
                });

                Button more = new Button("•••");
                more.getStyleClass().add("more");
                more.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
                more.setOnAction(e -> {
                    String[] row = getTableRow().getItem();
                    if (row == null) return;

                    ContextMenu menu = new ContextMenu();
                    MenuItem miEdit = new MenuItem(onDelete == null ? "Voir" : "Modifier");
                    miEdit.setOnAction(ev -> onEdit.accept(row));
                    menu.getItems().add(miEdit);
                    if (onDelete != null) {
                        MenuItem miDelete = new MenuItem("Supprimer");
                        miDelete.setOnAction(ev -> onDelete.accept(row));
                        menu.getItems().add(miDelete);
                    }
                    menu.show(more, Side.BOTTOM, 0, 0);
                });

                setGraphic(new HBox(6, edit, more));
            }
        });
    }

    private static void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}