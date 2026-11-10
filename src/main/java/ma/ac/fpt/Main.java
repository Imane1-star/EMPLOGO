
package ma.ac.fpt;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        Label message = new Label("Bienvenue dans EMPLOGO !");

        Scene scene = new Scene(message, 600, 400);

        stage.setTitle("EMPLOGO - Gestion du transport");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
