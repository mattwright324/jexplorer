package io.mattw.jexplorer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class JExplorer2 extends Application {

    private static final Logger logger = LogManager.getLogger();

    private static final ConfigFile<ConfigData> config = new ConfigFile<>("jexplorer.json", new ConfigData());

    public static void main(String[] args) {
        logger.debug("Starting Application");

        launch(args);
    }

    public void start(final Stage stage) {
        try {
            final Parent parent = FXMLLoader.load(getClass().getResource("/io/mattw/jexplorer/fxml/Main.fxml"));

            final Scene scene = new Scene(parent);
            scene.getStylesheets().add("Styles.css");
            stage.setTitle("JExplorer");
            stage.setScene(scene);
            stage.getIcons().add(new Image(getClass().getResource("/io/mattw/jexplorer/img/icon2.png").toExternalForm()));
            stage.setOnCloseRequest(we -> {
                logger.debug("Closing - Exiting Application");
                Platform.exit();
                System.exit(0);
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            Platform.exit();
            System.exit(0);
        }
    }

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

}
