package com.transportpro.view;

import com.transportpro.controller.MainController;
import com.transportpro.model.Employe;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.transportpro.*;

import java.util.List;
/** Écrans « liste » : Employés, Bus, Chauffeurs, Trajets, Incidents, Comptes. */
public final class ListPages {
    private ListPages() {}

    private record Cfg(String action, String[] headers, String[][] rows,
                       int badgeCol, String note, int total) {}

    public static Node build(Page p, Ctx ctx, MainController controller) {
        Cfg c = switch (p) {

            // Employés : données chargées depuis MariaDB
            case EMPLOYEES -> {
                // ✅ Passer par le controller, pas par le DAO
                List<Employe> employes = controller.chargerEmployes();
                String[][] rows = employes.stream()
                        .map(ListPages::employeToRow)
                        .toArray(String[][]::new);

                yield new Cfg("Ajouter un employé",
                        new String[]{"ID", "Nom", "Téléphone",
                                "Point d’embarquement", "Statut"},
                        rows, 5, null, rows.length);
            }

            case BUSES -> {
                String[][] rows = new String[Data.BUSES.length][];
                for (int i = 0; i < rows.length; i++) {
                    String[] b = Data.BUSES[i];
                    rows[i] = new String[]{
                            b[0], b[1],
                            i < 2 ? "Aujourd’hui • 17:30" : "—",
                            b[2]
                    };
                }
                yield new Cfg("Ajouter un bus",
                        new String[]{"Immatriculation", "Capacité",
                                "Prochain trajet", "Statut"},
                        rows, 3,
                        "Un bus en maintenance ou hors service ne peut pas être affecté.",
                        rows.length);
            }

            case DRIVERS -> new Cfg("Ajouter un chauffeur",
                    new String[]{"Nom", "Téléphone", "N° de permis",
                            "Disponibilité"},
                    Data.DRIVERS, 3, null, Data.DRIVERS.length);

            case TRIPS -> new Cfg("Créer un trajet",
                    new String[]{"ID", "Date", "Départ", "Destination",
                            "Bus", "Chauffeur", "Inscrits", "Statut"},
                    Data.TRIPS, 7, null, Data.TRIPS.length);

            case INCIDENTS -> new Cfg("Enregistrer un incident",
                    new String[]{"Date / heure", "Type", "Trajet", "Bus",
                            "Chauffeur", "Description"},
                    Data.INCIDENTS, 1, null, Data.INCIDENTS.length);

            case ACCOUNTS -> new Cfg("Créer un compte",
                    new String[]{"Identifiant", "Rôle",
                            "Dernière connexion", "Statut"},
                    Data.ACCOUNTS, 3, null, Data.ACCOUNTS.length);

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

        HBox filters = new HBox(
                8,
                Ui.search("Rechercher...", 210),
                Ui.combo("Tous les statuts", "Actif")
        );
        filters.setAlignment(Pos.CENTER_LEFT);

        if (p == Page.TRIPS) {
            DatePicker d = new DatePicker();
            d.setPromptText("jj/mm/aaaa");
            filters.getChildren().add(d);
        }

        HBox toolbar = new HBox(
                filters,
                Ui.spacer(),
                Ui.button(c.action(), "primary", "plus",
                        () -> ctx.openModal(c.action()))
        );
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 15, 0));

        card.getChildren().addAll(
                toolbar,
                Ui.table(c.headers(), c.rows(), c.badgeCol(),
                        () -> ctx.openModal("Détail — " + p.title)),
                pagination(c.rows().length, c.total())
        );

        return Ui.page(card);
    }

    
    // ----- Mapper Model → String[] pour l'affichage -----
    private static String[] employeToRow(Employe e) {
        return new String[]{
                String.valueOf(e.getIdEmploye()),
                e.getNomComplet(),
                e.getTelephone() == null ? "—" : e.getTelephone(),
                e.getAdresse() == null ? "—" : e.getAdresse(),
                e.isActif() ? "Actif" : "Inactif"
        };
    }

    private static HBox pagination(int shown, int total) {
        HBox pages = new HBox(4);

        for (String t : new String[]{"‹", "1", "2", "3", "›"}) {
            Button b = new Button(t);
            b.getStyleClass().add("page-btn");

            if (t.equals("1")) {
                b.getStyleClass().add("active");
            }

            pages.getChildren().add(b);
        }

        HBox h = new HBox(
                Ui.label("Affichage de 1 à " + shown + " sur " + total,
                        "muted"),
                Ui.spacer(),
                pages
        );

        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 0, 0, 0));

        return h;
    }
}

