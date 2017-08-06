package mattw.jexplorer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class JExplorer extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {

        StackPane main = new StackPane();

        Scene scene = new Scene(main, 700, 500);
        stage.setScene(scene);
        stage.setTitle("JExplorer");
        stage.setOnCloseRequest(we -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

}
