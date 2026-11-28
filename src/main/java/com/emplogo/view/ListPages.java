package com.emplogo.view;

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
import javafx.scene.layout.Region;
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

import com.emplogo.Ctx;
import com.emplogo.Data;
import com.emplogo.Page;
import com.emplogo.controller.MainController;
import com.emplogo.dao.AffectationDAO;
import com.emplogo.dao.BusDAO;
import com.emplogo.dao.EmployeDAO;
import com.emplogo.model.Affectation;
import com.emplogo.model.Bus;
import com.emplogo.model.Chauffeur;
import com.emplogo.model.Employe;
import com.emplogo.model.Incident;
import com.emplogo.model.enums.StatutTrajet;
import com.emplogo.model.enums.TypeIncident;

/**
 * Écrans « liste » : Employés, Bus, Chauffeurs, Trajets, Incidents, Comptes.
 * Les données passent toujours par le MainController (View -> Controller -> DAO).
 */
public final class ListPages {
    private ListPages() {}

    /** Nombre de lignes affichées par page. */
    private static final int PAGE_SIZE = 8;

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm");
    private static final String ALL_STATUS = "Tous les statuts";
    private static final String ALL_TYPES = "Tous les types";
    private static final String ALL_BUSES = "Tous les bus";

    /**
     * loader     : fournit les lignes (base de données ou données de démonstration).
     * dbSearch   : si non null, la recherche est faite par la base (SQL) et non en mémoire.
     * searchCols : colonnes (index dans la ligne) utilisées par la recherche ; null = toutes les colonnes visibles.
     */
    private record Cfg(String action, String[] headers, Supplier<String[][]> loader,
                       Function<String, String[][]> dbSearch,
                       int badgeCol, String note, int[] searchCols) {}

    public static Node build(Page p, Ctx ctx, MainController controller) {
        final MainController c0 = controller;
        Cfg c = switch (p) {

            // Employés : ligne = [id, matricule, nom + prénom, téléphone, adresse, statut] (recherche SQL)
            case EMPLOYEES -> new Cfg("Ajouter un employé",
                    new String[]{"ID", "Matricule", "Nom", "Téléphone", "Point d’embarquement", "Statut"},
                    () -> employeRows(c0, ""), q -> employeRows(c0, q), 5, null, null);

            // Bus : ligne = [immatriculation, capacité, marque / modèle, statut, id_bus]
            case BUSES -> new Cfg("Ajouter un bus",
                    new String[]{"Immatriculation", "Capacité", "Marque / modèle", "Statut"},
                    () -> busRows(c0), null, 3,
                    "Un bus en maintenance ou hors service ne peut pas être affecté.",
                    new int[]{0, 1, 2, 3});

            // Chauffeurs : ligne = [nom + prénom, téléphone, n° de permis, statut, id_chauffeur] (recherche SQL)
            case DRIVERS -> new Cfg("Ajouter un chauffeur",
                    new String[]{"Nom", "Téléphone", "N° de permis", "Disponibilité"},
                    () -> chauffeurRows(c0, ""), q -> chauffeurRows(c0, q), 3, null, null);

            // Trajets : ligne = [T + n°, date, départ, destination, bus, chauffeur, inscrits / capacité, statut,
            //                    id_affectation] (recherche SQL)
            case TRIPS -> new Cfg("Créer un trajet",
                    new String[]{"ID", "Date", "Départ", "Destination", "Bus", "Chauffeur", "Inscrits", "Statut"},
                    () -> affectationRows(c0, ""), q -> affectationRows(c0, q), 7, null, null);

            // Incidents : ligne = [date/heure, type, trajet, bus, chauffeur, gravité, statut, description,
            //                     id_incident, date (jj/mm/aaaa), immatriculation] (recherche SQL)
            case INCIDENTS -> new Cfg(ctx.isDriver() ? "Signaler un incident" : "Enregistrer un incident",
                    new String[]{"Date / heure", "Type", "Trajet", "Bus", "Chauffeur", "Gravité", "Statut", "Description"},
                    () -> incidentRows(c0, ""), q -> incidentRows(c0, q), 6, null, null);

            case ACCOUNTS -> new Cfg("Créer un compte",
                    new String[]{"Identifiant", "Rôle", "Dernière connexion", "Statut"},
                    () -> Data.ACCOUNTS, null, 3, null, null);

            default -> throw new IllegalArgumentException("Page non gérée : " + p);
        };

        VBox card = Ui.card();
        card.getStyleClass().add("main-card");

        if (c.note() != null) {
            HBox banner = new HBox(8, Ui.icon("incidents", 17, "#8A5A00"), Ui.label(c.note(), "info-text"));
            banner.getStyleClass().add("info-banner");
            banner.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(banner);
        }

        HBox searchBox = Ui.search("Rechercher...", 210);
        TextField searchField = (TextField) searchBox.getChildren().get(1);

        ComboBox<String> statusBox = switch (p) {
            case DRIVERS -> Ui.combo(withAll(ALL_STATUS, Arrays.stream(Chauffeur.STATUTS).map(Chauffeur::statutLabel).toList()));
            case TRIPS -> Ui.combo(withAll(ALL_STATUS, Arrays.stream(StatutTrajet.values()).map(StatutTrajet::libelle).toList()));
            case INCIDENTS -> Ui.combo(withAll(ALL_TYPES, Arrays.stream(TypeIncident.values()).map(TypeIncident::libelle).toList()));
            default -> Ui.combo(ALL_STATUS, "Actif");
        };

        HBox filters = new HBox(8, searchBox, statusBox);
        filters.setAlignment(Pos.CENTER_LEFT);

        DatePicker dateFilter = null;
        if (p == Page.TRIPS || p == Page.INCIDENTS) {
            dateFilter = new DatePicker();
            dateFilter.setPromptText("jj/mm/aaaa");
            filters.getChildren().add(dateFilter);
        }
        final DatePicker tripDate = dateFilter;

        // Incidents (BF22) : filtre supplémentaire par bus
        ComboBox<String> busFilter = p == Page.INCIDENTS
                ? Ui.combo(withAll(ALL_BUSES, controller.chargerBus().stream().map(Bus::getImmatriculation).toList()))
                : null;
        if (busFilter != null) filters.getChildren().add(busFilter);

        // Tableau + pagination pilotés par un même objet (recherche, pages, rafraîchissement)
        Listing listing = new Listing(c, ctx, p);

        Runnable onAdd = switch (p) {
            case EMPLOYEES -> () -> addEmploye(controller, listing);
            case BUSES -> () -> addBus(controller, listing);
            case DRIVERS -> () -> addChauffeur(controller, listing);
            case TRIPS -> () -> addAffectation(controller, listing);
            case INCIDENTS -> () -> addIncident(controller, ctx, listing);
            default -> () -> ctx.openModal(c.action());
        };

        HBox toolbar = new HBox(filters, Ui.spacer(), Ui.button(c.action(), "primary", "plus", onAdd));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 15, 0));

        // Recherche pendant la saisie (requête SQL après une courte pause de frappe)
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(e -> listing.setQuery(searchField.getText()));
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (c.dbSearch() != null) debounce.playFromStart();
            else listing.setQuery(newV);
        });

        switch (p) {
            case EMPLOYEES -> wireActions(listing.table(),
                    row -> editEmploye(controller, row, listing),
                    row -> deleteEmploye(controller, row, listing));
            case BUSES -> wireActions(listing.table(),
                    row -> editBus(controller, row, listing),
                    row -> deleteBus(controller, row, listing));
            case DRIVERS -> {
                wireActions(listing.table(),
                        row -> editChauffeur(controller, row, listing),
                        row -> deleteChauffeur(controller, row, listing));
                statusBox.setOnAction(e -> listing.setExtraFilter(rowFilter(statusBox.getValue(), ALL_STATUS, 3, null, -1)));
            }
            case TRIPS -> {
                wireActions(listing.table(),
                        row -> editAffectation(controller, row, listing),
                        row -> deleteAffectation(controller, row, listing));
                Runnable apply = () -> listing.setExtraFilter(
                        rowFilter(statusBox.getValue(), ALL_STATUS, 7, tripDate.getValue(), 1));
                statusBox.setOnAction(e -> apply.run());
                tripDate.valueProperty().addListener((obs, o, n) -> apply.run());
            }
            case INCIDENTS -> {
                if (ctx.isDriver()) {
                    // BF21 : le chauffeur signale et consulte, sans modifier ni supprimer
                    wireActions(listing.table(), row -> showIncident(controller, row), null);
                } else {
                    wireActions(listing.table(),
                            row -> editIncident(controller, row, listing),
                            row -> deleteIncident(controller, row, listing));
                }
                // BF22 : filtres type (colonne 1), date (colonne 9) et bus (colonne 10)
                Runnable apply = () -> {
                    Predicate<String[]> f = rowFilter(statusBox.getValue(), ALL_TYPES, 1, tripDate.getValue(), 9);
                    String bus = busFilter.getValue();
                    Predicate<String[]> byBus = bus == null || bus.equals(ALL_BUSES) ? null : r -> bus.equals(r[10]);
                    listing.setExtraFilter(f == null ? byBus : byBus == null ? f : f.and(byBus));
                };
                statusBox.setOnAction(e -> apply.run());
                busFilter.setOnAction(e -> apply.run());
                tripDate.valueProperty().addListener((obs, o, n) -> apply.run());
            }
            default -> { }
        }

        card.getChildren().addAll(toolbar, listing.table(), listing.footer());
        return Ui.page(card);
    }

    private static String[] withAll(String all, List<String> values) {
        List<String> items = new ArrayList<>();
        items.add(all);
        items.addAll(values);
        return items.toArray(new String[0]);
    }

    /**
     * Filtre en mémoire : valeur de la colonne valueCol (sauf « tous ») et date jj/mm/aaaa de la colonne dateCol.
     * Retourne null s'il n'y a rien à filtrer.
     */
    private static Predicate<String[]> rowFilter(String value, String all, int valueCol, LocalDate date, int dateCol) {
        boolean byValue = value != null && !value.equals(all);
        if (!byValue && date == null) return null;
        String d = date == null ? null : date.format(DATE_FR);
        return row -> (!byValue || value.equals(row[valueCol])) && (d == null || d.equals(row[dateCol]));
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


    private static String nz(String s) { return s == null ? "" : s; }

    private static int id(String s) { return Integer.parseInt(s); }

    // ================================================================== Employés
    /** Lignes : [id, matricule, nom + prénom, téléphone, adresse, statut]. */
    private static String[][] employeRows(MainController ctrl, String text) {
        return ctrl.rechercherEmployes(text).stream().map(e -> new String[]{
                String.valueOf(e.getIdEmploye()), nz(e.getMatricule()), nz(e.getNomComplet()).trim(),
                nz(e.getTelephone()), nz(e.getAdresse()), e.isActif() ? "Actif" : "Inactif"
        }).toArray(String[][]::new);
    }

    private static void addEmploye(MainController ctrl, Listing listing) {
        EmployeDialog.showAdd().ifPresent(e -> {
            if (ctrl.ajouterEmploye(e)) listing.reload();
        });
    }

    private static void editEmploye(MainController ctrl, String[] row, Listing listing) {
        Employe current = ctrl.trouverEmploye(id(row[0]));
        if (current == null) return;
        EmployeDialog.showEdit(current).ifPresent(e -> {
            if (ctrl.modifierEmploye(e)) listing.reload();
        });
    }

    private static void deleteEmploye(MainController ctrl, String[] row, Listing listing) {
        int id = id(row[0]);
        EmployeDAO.Usage u = ctrl.usageEmploye(id);
        if (u == null) return;
        List<String> linked = new ArrayList<>();
        if (u.chauffeur() > 0) linked.add("une fiche chauffeur"
                + (u.trajetsConduits() > 0 ? " avec " + u.trajetsConduits() + " trajet(s)" : ""));
        DeleteDialog.addIf(linked, u.inscriptions(), "inscription(s) à des trajets");
        DeleteDialog.addIf(linked, u.presences(), "présence(s) enregistrée(s)");
        DeleteDialog.addIf(linked, u.comptes(), "compte utilisateur");
        boolean inactive = "Inactif".equals(row[5]);

        boolean ok = switch (DeleteDialog.ask("l'employé " + row[2] + " (" + row[1] + ")", linked,
                inactive ? null : "Désactiver", u.canDeleteWithHistory(),
                "Suppression impossible : ses trajets de chauffeur concernent d'autres employés."
                        + (inactive ? " Il est déjà désactivé." : ""))) {
            case SOFT -> ctrl.desactiverEmploye(id);
            case DELETE -> u.any() ? ctrl.supprimerEmployeEtHistorique(id) : ctrl.supprimerEmploye(id);
            case CANCEL -> false;
        };
        if (ok) listing.reload();
    }

    // ================================================================== Bus
    /** Lignes : [immatriculation, capacité, marque / modèle, statut, id_bus]. */
    private static String[][] busRows(MainController ctrl) {
        return ctrl.chargerBus().stream().map(b -> new String[]{
                b.getImmatriculation(), b.getCapacite() + " places",
                (nz(b.getMarque()) + " " + nz(b.getModele())).trim(),
                b.getStatut() == null ? "" : b.getStatut().libelle(),
                String.valueOf(b.getIdBus())
        }).toArray(String[][]::new);
    }

    private static void addBus(MainController ctrl, Listing listing) {
        BusDialog.showAdd().ifPresent(b -> {
            if (ctrl.ajouterBus(b)) listing.reload();
        });
    }

    private static void editBus(MainController ctrl, String[] row, Listing listing) {
        Bus current = ctrl.trouverBus(id(row[4]));
        if (current == null) return;
        BusDialog.showEdit(current).ifPresent(b -> {
            if (ctrl.modifierBus(b)) listing.reload();
        });
    }

    private static void deleteBus(MainController ctrl, String[] row, Listing listing) {
        int id = id(row[4]);
        BusDAO.Usage u = ctrl.usageBus(id);
        if (u == null) return;
        List<String> linked = new ArrayList<>();
        DeleteDialog.addIf(linked, u.affectations(), "trajet(s) planifié(s) ou effectué(s)");
        DeleteDialog.addIf(linked, u.maintenances(), "maintenance(s)");
        boolean out = "Hors service".equals(row[3]);

        boolean ok = switch (DeleteDialog.ask("le bus " + row[0], linked, out ? null : "Mettre hors service",
                u.canDeleteWithHistory(), "Suppression impossible : l'historique des trajets doit être conservé."
                        + (out ? " Le bus est déjà hors service." : ""))) {
            case SOFT -> ctrl.mettreBusHorsService(id);
            case DELETE -> u.any() ? ctrl.supprimerBusEtHistorique(id) : ctrl.supprimerBus(id);
            case CANCEL -> false;
        };
        if (ok) listing.reload();
    }

    // ================================================================== Chauffeurs
    /** Lignes : [nom + prénom, téléphone, n° de permis, statut, id_chauffeur]. */
    private static String[][] chauffeurRows(MainController ctrl, String text) {
        return ctrl.rechercherChauffeurs(text).stream().map(ch -> new String[]{
                ch.getNomComplet(), nz(ch.getTelephone()), nz(ch.getNumeroPermis()),
                Chauffeur.statutLabel(ch.getStatut()), String.valueOf(ch.getIdChauffeur())
        }).toArray(String[][]::new);
    }

    private static void addChauffeur(MainController ctrl, Listing listing) {
        ChauffeurDialog.showAdd(ctrl).ifPresent(ch -> {
            if (ctrl.ajouterChauffeur(ch)) listing.reload();
        });
    }

    private static void editChauffeur(MainController ctrl, String[] row, Listing listing) {
        Chauffeur current = ctrl.trouverChauffeur(id(row[4]));
        if (current == null) return;
        ChauffeurDialog.showEdit(ctrl, current).ifPresent(ch -> {
            if (ctrl.modifierChauffeur(ch)) listing.reload();
        });
    }

    private static void deleteChauffeur(MainController ctrl, String[] row, Listing listing) {
        int id = id(row[4]);
        int trajets = ctrl.nombreTrajetsChauffeur(id);
        if (trajets < 0) return;
        List<String> linked = new ArrayList<>();
        DeleteDialog.addIf(linked, trajets, "trajet(s) planifié(s) ou effectué(s)");
        boolean suspended = row[3].equals(Chauffeur.statutLabel("SUSPENDU"));

        boolean ok = switch (DeleteDialog.ask("le chauffeur " + row[0] + " (permis " + row[2] + ")", linked,
                suspended ? null : "Suspendre", false,
                "Suppression impossible : l'historique des trajets doit être conservé."
                        + (suspended ? " Le chauffeur est déjà suspendu." : ""))) {
            case SOFT -> ctrl.desactiverChauffeur(id);
            case DELETE -> ctrl.supprimerChauffeur(id);   // seulement possible sans trajet
            case CANCEL -> false;
        };
        if (ok) listing.reload();
    }

    // ================================================================== Trajets (affectations)
    /** Lignes : [T + n°, date, départ, destination, bus, chauffeur, inscrits / capacité, statut, id_affectation]. */
    private static String[][] affectationRows(MainController ctrl, String text) {
        return ctrl.rechercherAffectations(text).stream().map(a -> new String[]{
                "T" + a.getIdAffectation(),
                a.getDateTrajet() == null ? "" : a.getDateTrajet().format(DATE_FR),
                a.getHeureDepart() == null ? "" : a.getHeureDepart().toString(),
                nz(a.getLieuArrivee()), nz(a.getImmatriculation()), nz(a.getChauffeurNom()),
                a.getInscrits() + " / " + a.getCapacite(),
                StatutTrajet.libelleOf(a.getStatut()),
                String.valueOf(a.getIdAffectation())
        }).toArray(String[][]::new);
    }

    private static void addAffectation(MainController ctrl, Listing listing) {
        AffectationDialog.showAdd(ctrl).ifPresent(a -> {
            if (ctrl.ajouterAffectation(a)) listing.reload();
        });
    }

    private static void editAffectation(MainController ctrl, String[] row, Listing listing) {
        Affectation current = ctrl.trouverAffectation(id(row[8]));
        if (current == null) return;
        AffectationDialog.showEdit(ctrl, current).ifPresent(a -> {
            if (ctrl.modifierAffectation(a)) listing.reload();
        });
    }

    private static void deleteAffectation(MainController ctrl, String[] row, Listing listing) {
        int id = id(row[8]);
        AffectationDAO.Usage u = ctrl.usageAffectation(id);
        if (u == null) return;
        List<String> linked = new ArrayList<>();
        DeleteDialog.addIf(linked, u.inscriptions(), "inscription(s)");
        DeleteDialog.addIf(linked, u.presences(), "présence(s) enregistrée(s)");
        DeleteDialog.addIf(linked, u.incidents(), "incident(s)");
        boolean cancelled = row[7].equals(StatutTrajet.ANNULE.libelle());

        boolean ok = switch (DeleteDialog.ask("le trajet " + row[0] + " du " + row[1] + " à " + row[2] + " vers " + row[3],
                linked, cancelled ? null : "Annuler le trajet", true, null)) {
            case SOFT -> ctrl.annulerAffectation(id);
            case DELETE -> u.any() ? ctrl.supprimerAffectationEtHistorique(id) : ctrl.supprimerAffectation(id);
            case CANCEL -> false;
        };
        if (ok) listing.reload();
    }

    // ================================================================== Incidents
    /**
     * Lignes : [date/heure, type, trajet, bus, chauffeur, gravité, statut, description (abrégée),
     * id_incident, date (jj/mm/aaaa), immatriculation].
     */
    private static String[][] incidentRows(MainController ctrl, String text) {
        return ctrl.rechercherIncidents(text).stream().map(inc -> {
            String desc = nz(inc.getDescription()).replace('\n', ' ');
            return new String[]{
                    inc.getDateHeure() == null ? "" : inc.getDateHeure().format(DATETIME_FR),
                    inc.getType() == null ? "" : inc.getType().libelle(),
                    "T" + inc.getIdAffectation() + " • " + nz(inc.getNomLigne()),
                    nz(inc.getImmatriculation()), nz(inc.getChauffeurNom()),
                    Incident.graviteLabel(inc.getGravite()), Incident.statutLabel(inc.getStatut()),
                    desc.length() > 60 ? desc.substring(0, 57) + "…" : desc,
                    String.valueOf(inc.getIdIncident()),
                    inc.getDateHeure() == null ? "" : inc.getDateHeure().toLocalDate().format(DATE_FR),
                    nz(inc.getImmatriculation())
            };
        }).toArray(String[][]::new);
    }

    private static void addIncident(MainController ctrl, Ctx ctx, Listing listing) {
        IncidentDialog.showAdd(ctrl, ctx.isDriver()).ifPresent(inc -> {
            if (ctrl.ajouterIncident(inc)) listing.reload();
        });
    }

    private static void editIncident(MainController ctrl, String[] row, Listing listing) {
        Incident current = ctrl.trouverIncident(id(row[8]));
        if (current == null) return;
        IncidentDialog.showEdit(ctrl, current).ifPresent(inc -> {
            if (ctrl.modifierIncident(inc)) listing.reload();
        });
    }

    private static void deleteIncident(MainController ctrl, String[] row, Listing listing) {
        if (DeleteDialog.ask("l'incident INC" + row[8] + " (" + row[1] + ", " + row[0] + ")", List.of(), null, true, null)
                == DeleteDialog.Choice.DELETE && ctrl.supprimerIncident(id(row[8]))) {
            listing.reload();
        }
    }

    /** Détail en lecture seule (profil chauffeur). */
    private static void showIncident(MainController ctrl, String[] row) {
        Incident inc = ctrl.trouverIncident(id(row[8]));
        if (inc == null) return;
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Incident INC" + inc.getIdIncident());
        a.setHeaderText(row[1] + " • " + row[0]);
        a.setContentText("Trajet : " + row[2] + "\nBus : " + row[3] + "\nChauffeur : " + row[4]
                + "\nGravité : " + row[5] + "\nStatut : " + row[6]
                + "\nDéclaré par : " + Incident.modeLabel(inc.getModeDeclaration())
                + (inc.getDeclarant().isEmpty() ? "" : " (" + inc.getDeclarant() + ")")
                + "\n\n" + inc.getDescription());
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
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

}
