package com.transportpro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.function.Consumer;

/**
 * Page de connexion « Employee Go / EMPLOGO ».
 * Le logo est lu dans /images/emplogo.png (src/main/resources/images) : remplacez ce fichier
 * par votre logo d'origine pour une meilleure qualité.
 * Seuls LoginView.java, styles.css (section « connexion ») et le dossier images changent.
 */
public class LoginView extends GridPane {

    public LoginView(Consumer<String> onLogin) {
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(52.5);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(47.5);
        RowConstraints r = new RowConstraints();
        r.setPercentHeight(100);
        r.setVgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(c1, c2);
        getRowConstraints().add(r);

        add(visual(), 0, 0);
        add(panel(onLogin), 1, 0);
    }

    // ------------------------------------------------------------ côté gauche
    private Node visual() {
        // marque en haut à gauche : pastille crème + « Employee Go »
        StackPane mark = new StackPane(Ui.icon("buses", 28, "#2B124C"));
        mark.getStyleClass().add("brand-mark");
        mark.setMinSize(57, 57);
        mark.setPrefSize(57, 57);
        mark.setMaxSize(57, 57);
        HBox brand = new HBox(18, mark, new VBox(2,
                Ui.label("Employee Go", "emp-go-title"),
                Ui.label("Gestion du transport du personnel", "emp-go-sub")));
        brand.setAlignment(Pos.CENTER_LEFT);

        // carte blanche avec le logo EMPLOGO, alignée à droite
        HBox logoRow = new HBox(Ui.spacer(), logoCard());
        logoRow.setPadding(new Insets(0, 50, 0, 0));

        // texte d'accroche en bas
        VBox copy = new VBox(8,
                Ui.label("TRANSPORT D’ENTREPRISE, SIMPLEMENT.", "login-eyebrow"),
                Ui.label("Simplifiez le transport\nde vos équipes", "login-h1"),
                wrapped("Planifiez les trajets, affectez vos ressources et suivez les présences depuis un espace unique.", "login-p", 620));

        HBox footer = new HBox(Ui.label("Taroudant • Maroc", "login-footer"), Ui.spacer(), Ui.label("Version 1.0", "login-footer"));

        VBox root = new VBox(22, brand, logoRow, Ui.spacer2(), copy, footer);
        root.getStyleClass().add("login-visual");
        root.setPadding(new Insets(40, 60, 28, 68));
        return root;
    }

    private Node logoCard() {
        StackPane card = new StackPane();
        card.getStyleClass().add("logo-card");
        card.setMinSize(330, 330);
        card.setPrefSize(330, 330);
        card.setMaxSize(330, 330);

        URL url = LoginView.class.getResource("/images/emplogo.png");
        if (url != null) {
            ImageView iv = new ImageView(new Image(url.toExternalForm()));
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setFitWidth(330);
            card.getChildren().add(iv);
        } else { // repli si l'image est absente
            VBox fallback = new VBox(6, Ui.icon("buses", 70, "#2B124C"),
                    Ui.label("E-M-P-L-O-G-O", "logo-fallback"),
                    Ui.label("Employee Go", "emp-sub"),
                    Ui.label("Gestion du transport du personnel", "muted"));
            fallback.setAlignment(Pos.CENTER);
            card.getChildren().add(fallback);
        }
        return card;
    }

    private static Label wrapped(String text, String style, double maxWidth) {
        Label l = Ui.label(text, style);
        l.setWrapText(true);
        l.setMaxWidth(maxWidth);
        return l;
    }

    // ------------------------------------------------------------ côté droit : formulaire
    private Node panel(Consumer<String> onLogin) {
        HBox error = new HBox(8, Ui.icon("incidents", 18, "#A92323"),
                Ui.label("Identifiant ou mot de passe incorrect. Accès refusé.", "login-error-text"));
        error.getStyleClass().add("login-error");
        error.setAlignment(Pos.CENTER_LEFT);
        error.setVisible(false);
        error.setManaged(false);

        TextField user = new TextField("transport.resp");
        user.setPromptText("Votre identifiant");

        PasswordField pass = new PasswordField();
        pass.setText("transportpro");
        pass.setPromptText("Votre mot de passe");
        pass.getStyleClass().add("bare");
        TextField passVisible = new TextField();
        passVisible.getStyleClass().add("bare");
        passVisible.textProperty().bindBidirectional(pass.textProperty());
        passVisible.setVisible(false);
        passVisible.setManaged(false);
        StackPane stack = new StackPane(pass, passVisible);
        HBox.setHgrow(stack, Priority.ALWAYS);

        Button toggle = new Button("Afficher");
        toggle.getStyleClass().add("link-btn");
        toggle.setOnAction(e -> {
            boolean show = !passVisible.isVisible();
            passVisible.setVisible(show);
            passVisible.setManaged(show);
            pass.setVisible(!show);
            pass.setManaged(!show);
            toggle.setText(show ? "Masquer" : "Afficher");
        });
        HBox passRow = new HBox(6, stack, toggle);
        passRow.setAlignment(Pos.CENTER_LEFT);
        passRow.getStyleClass().add("password-row");

        // Profil de démonstration : sert uniquement à choisir le rôle ouvert par AppShell.
        ComboBox<String> role = Ui.combo("Responsable", "Chauffeur", "Administrateur");

        CheckBox remember = new CheckBox("Se souvenir de moi");
        remember.setWrapText(false);
        Button forgot = new Button("Mot de passe oublié ?");
        forgot.getStyleClass().add("link-btn");
        HBox options = new HBox(remember, Ui.spacer(), forgot);
        options.setAlignment(Pos.CENTER_LEFT);

        Runnable submit = () -> {
            if (user.getText().isBlank() || "erreur".equals(pass.getText())) {
                error.setVisible(true);
                error.setManaged(true);
            } else {
                onLogin.accept(role.getValue());
            }
        };
        Button login = Ui.button("Se connecter", "primary", null, submit);
        login.setMaxWidth(Double.MAX_VALUE);
        user.setOnAction(e -> submit.run());
        pass.setOnAction(e -> submit.run());

        Label hint = wrapped("Astuce : utilisez « erreur » comme mot de passe pour tester l’état d’erreur.", "login-hint", 460);
        hint.setAlignment(Pos.CENTER);
        hint.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(16,
                Ui.label("ESPACE SÉCURISÉ", "eyebrow"),
                Ui.label("Bienvenue", "login-h2"),
                wrapped("Connectez-vous pour accéder à votre espace de gestion.", "login-sub", 460),
                error,
                Ui.field("Identifiant", user),
                Ui.field("Mot de passe", passRow),
                Ui.field("Profil de démonstration", role),
                options,
                login,
                hint);
        form.getStyleClass().add("login-form");
        form.setMinWidth(460);
        form.setPrefWidth(460);
        form.setMaxWidth(460);
        form.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane wrap = new StackPane(form);
        wrap.getStyleClass().add("login-side");
        return wrap;
    }
}
