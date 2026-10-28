package com.transportpro;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Petits composants réutilisables (boutons, badges, tableaux, cartes, graphiques...). */
public final class Ui {
    private Ui() {}

    // ---------------------------------------------------------------- icônes (mêmes tracés SVG que la maquette)
    private static final Map<String, String> ICONS = new HashMap<>();
    static {
        ICONS.put("dashboard", "M3 3h7v7H3zM14 3h7v4h-7zM14 11h7v10h-7zM3 14h7v7H3z");
        ICONS.put("employees", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75");
        ICONS.put("buses", "M5 17H3V6a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v11h-2M5 17h14M6 21a2 2 0 1 0 0-4 2 2 0 0 0 0 4M18 21a2 2 0 1 0 0-4 2 2 0 0 0 0 4M3 10h18M7 6h.01M17 6h.01");
        ICONS.put("drivers", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8");
        ICONS.put("trips", "M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 4v15.5M8 4h12v13H8zM12 8h4M12 12h4");
        ICONS.put("assignments", "M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11");
        ICONS.put("registrations", "M12 5v14M5 12h14M3 3h18v18H3z");
        ICONS.put("attendance", "M20 6L9 17l-5-5");
        ICONS.put("incidents", "M10.3 2.9L1.8 17a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 2.9a2 2 0 0 0-3.4 0M12 9v4M12 17h.01");
        ICONS.put("reports", "M4 19V9M10 19V5M16 19v-7M22 19V2");
        ICONS.put("accounts", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4 22a8 8 0 0 1 16 0M19 8v6M22 11h-6");
        ICONS.put("roles", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10M9 12l2 2 4-4");
        ICONS.put("planning", "M3 5h18v16H3zM16 3v4M8 3v4M3 10h18");
        ICONS.put("search", "M21 21l-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0");
        ICONS.put("bell", "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4");
        ICONS.put("plus", "M12 5v14M5 12h14");
        ICONS.put("logout", "M10 17l5-5-5-5M15 12H3M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4");
        ICONS.put("close", "M18 6L6 18M6 6l12 12");
    }

    public static Node icon(String name, double size, String color) {
        SVGPath p = new SVGPath();
        p.setContent(ICONS.getOrDefault(name, ICONS.get("dashboard")));
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(color));
        p.setStrokeWidth(1.8);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Pane box = new Pane(p);
        box.setMinSize(24, 24);
        box.setPrefSize(24, 24);
        box.setMaxSize(24, 24);
        double s = size / 24.0;
        box.setScaleX(s);
        box.setScaleY(s);
        return new Group(box);
    }

    // ---------------------------------------------------------------- briques de base
    public static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Espace extensible verticalement (pour VBox). */
    public static Region spacer2() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox page(Node... children) {
        VBox v = new VBox(14);
        v.setPadding(new Insets(20, 22, 25, 22));
        v.getChildren().addAll(children);
        return v;
    }

    public static VBox card(Node... children) {
        VBox v = new VBox();
        v.getStyleClass().add("card");
        v.getChildren().addAll(children);
        return v;
    }

    public static void fixHeight(Region r, double h) {
        r.setMinHeight(h);
        r.setPrefHeight(h);
        r.setMaxHeight(h);
    }

    public static Button button(String text, String kind, String icon, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", kind);
        if (icon != null) {
            b.setGraphic(icon(icon, 16, kind.equals("primary") ? "#FFFFFF" : "#522B5B"));
            b.setGraphicTextGap(7);
        }
        if (action != null) {
            b.setOnAction(e -> action.run());
        }
        return b;
    }

    public static ComboBox<String> combo(String... items) {
        ComboBox<String> c = new ComboBox<>();
        c.getItems().addAll(items);
        c.getSelectionModel().selectFirst();
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    public static VBox field(String text, Node input) {
        if (input instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return new VBox(5, label(text, "field-label"), input);
    }

    public static HBox search(String prompt, double width) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tf, Priority.ALWAYS);
        HBox b = new HBox(8, icon("search", 16, "#854F6C"), tf);
        b.setAlignment(Pos.CENTER_LEFT);
        b.getStyleClass().add("search");
        b.setMinWidth(width);
        b.setPrefWidth(width);
        b.setMaxWidth(width);
        return b;
    }

    public static StackPane avatar(String initials) {
        StackPane s = new StackPane(label(initials, "avatar-text"));
        s.getStyleClass().add("avatar");
        s.setMinSize(32, 32);
        s.setPrefSize(32, 32);
        s.setMaxSize(32, 32);
        return s;
    }

    // ---------------------------------------------------------------- badges de statut
    public static String badgeKind(String text) {
        String t = text.toLowerCase();
        boolean negative = t.contains("indisponible") || t.contains("inactif");
        if (!negative && t.matches(".*(disponible|actif|terminé|présent|acceptée).*")) return "success";
        if (t.matches(".*(maintenance|retard|planifié|affecter).*")) return "warning";
        if (t.matches(".*(hors|panne|annulé|inactif|indisponible|absent).*")) return "danger";
        return "neutral";
    }

    public static HBox badge(String text) {
        Circle dot = new Circle(3);
        dot.getStyleClass().add("badge-dot");
        HBox b = new HBox(5, dot, new Label(text));
        b.setAlignment(Pos.CENTER);
        b.getStyleClass().addAll("badge", badgeKind(text));
        b.setMaxWidth(Region.USE_PREF_SIZE);
        return b;
    }

    // ---------------------------------------------------------------- cartes
    public static HBox cardTitle(String title, String subtitle, String action, Runnable onAction) {
        VBox left = new VBox(2, label(title, "card-h3"), label(subtitle, "card-sub"));
        HBox box = new HBox(left, spacer());
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(0, 0, 12, 0));
        if (action != null) {
            Button b = new Button(action + " →");
            b.getStyleClass().add("link-btn");
            b.setOnAction(e -> {
                if (onAction != null) onAction.run();
            });
            box.getChildren().add(b);
        }
        return box;
    }

    public static HBox statCard(String title, String value, String delta, String icon, Runnable onClick) {
        HBox h = new HBox(12);
        h.getStyleClass().add("stat-card");
        h.setAlignment(Pos.TOP_LEFT);
        if (icon != null) {
            StackPane ic = new StackPane(icon(icon, 20, "#522B5B"));
            ic.getStyleClass().add("stat-icon");
            ic.setMinSize(40, 40);
            ic.setPrefSize(40, 40);
            ic.setMaxSize(40, 40);
            h.getChildren().add(ic);
        }
        h.getChildren().add(new VBox(3, label(title, "stat-title"), label(value, "stat-value"), label(delta, "stat-delta")));
        if (onClick != null) {
            h.setCursor(Cursor.HAND);
            h.setOnMouseClicked(e -> onClick.run());
        }
        return h;
    }

    // ---------------------------------------------------------------- tableau de données
    /**
     * @param badgeCol index de la colonne affichée sous forme de badge (-1 = aucune)
     * @param onRow    action au clic sur une ligne / sur « Modifier »
     */
    public static TableView<String[]> table(String[] headers, String[][] rows, int badgeCol, Runnable onRow) {
        TableView<String[]> tv = new TableView<>();
        tv.getStyleClass().add("data-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setFixedCellSize(46);

        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(headers[i].toUpperCase());
            c.setSortable(false);
            c.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[idx]));
            if (i == badgeCol) {
                c.setCellFactory(col -> new TableCell<String[], String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(null);
                        setGraphic(empty || item == null ? null : badge(item));
                    }
                });
            }
            tv.getColumns().add(c);
        }

        TableColumn<String[], String> actions = new TableColumn<>("ACTIONS");
        actions.setSortable(false);
        actions.setCellValueFactory(cd -> new SimpleStringProperty(""));
        actions.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Button edit = new Button("Modifier");
                edit.getStyleClass().add("table-action");
                edit.setOnAction(e -> {
                    if (onRow != null) onRow.run();
                });
                Button more = new Button("•••");
                more.getStyleClass().add("more");
                setGraphic(new HBox(6, edit, more));
            }
        });
        tv.getColumns().add(actions);

        tv.setRowFactory(t -> {
            TableRow<String[]> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && onRow != null) onRow.run();
            });
            return row;
        });

        tv.getItems().setAll(Arrays.asList(rows));
        double h = 40 + rows.length * 46 + 4;
        tv.setMinHeight(h);
        tv.setPrefHeight(h);
        tv.setMaxHeight(h);
        return tv;
    }

    // ---------------------------------------------------------------- graphiques
    public static AreaChart<String, Number> areaChart(String[] cats, double[] values) {
        AreaChart<String, Number> c = new AreaChart<>(new CategoryAxis(), new NumberAxis());
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (int i = 0; i < cats.length; i++) {
            s.getData().add(new XYChart.Data<String, Number>(cats[i], values[i]));
        }
        c.getData().add(s);
        c.setLegendVisible(false);
        c.setAnimated(false);
        c.setCreateSymbols(true);
        VBox.setVgrow(c, Priority.ALWAYS);
        return c;
    }

    public static BarChart<String, Number> barChart(String[] cats, double[] values) {
        BarChart<String, Number> c = new BarChart<>(new CategoryAxis(), new NumberAxis());
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (int i = 0; i < cats.length; i++) {
            s.getData().add(new XYChart.Data<String, Number>(cats[i], values[i]));
        }
        c.getData().add(s);
        c.setLegendVisible(false);
        c.setAnimated(false);
        c.setBarGap(4);
        c.setCategoryGap(18);
        VBox.setVgrow(c, Priority.ALWAYS);
        return c;
    }
}
