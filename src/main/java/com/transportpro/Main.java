package com.transportpro;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    private Scene scene;

    @Override
    public void start(Stage stage) {
        scene = new Scene(new StackPane(), 1366, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        showLogin();

        stage.setTitle("TransportPro — Gestion du transport des employés");
        stage.setMinWidth(1180);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    private void showLogin() {
        scene.setRoot(new LoginView(this::showApp));
    }

    private void showApp(String role) {
        scene.setRoot(new AppShell(role, this::showLogin));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
