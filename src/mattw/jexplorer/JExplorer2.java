package mattw.jexplorer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.util.stream.IntStream;

public class JExplorer2 extends Application {

    private static JExplorer2 app;

    private ProgressIndicator localIndicator, networkIndicator;
    private TreeView<Node> tree;
    private TreeItem<Node> localRoot, networkRoot;
    private Button btnRefresh, btnTrash, btnStart, btnSettings;

    public static JExplorer2 getApp() { return app; }

    public static void main(String[] args) {
        launch(args);
    }

    class MyTreeCell extends TreeCell<Node> {
        protected void updateItem(Node item, boolean empty) {
            super.updateItem(item, empty);
            if(empty) {
                this.setGraphic(null);
                this.setStyle("");
            } else {
                if("treeCell".equals(item.getId())) {
                    this.setStyle("-fx-background-color: linear-gradient(to bottom, transparent, lightgray);");
                } else {
                    this.setStyle("");
                }
                this.setGraphic(item);
            }
        }
    }

    public void start(Stage stage) {
        app = this;

        Label lTitle = new Label("Local Machine");
        lTitle.setMaxWidth(Double.MAX_VALUE);

        localIndicator = new ProgressIndicator();
        localIndicator.setVisible(false);
        localIndicator.setManaged(false);
        localIndicator.setMaxWidth(20);
        localIndicator.setMaxHeight(20);

        ImageView refresh = new ImageView(new Image(getClass().getResource("/mattw/jexplorer/img/refresh.png").toExternalForm()));
        refresh.setFitHeight(16);
        refresh.setFitWidth(16);
        refresh.setCursor(Cursor.HAND);

        btnRefresh = new Button();
        btnRefresh.setGraphic(refresh);
        btnRefresh.setOnAction(ae -> startLocalScan());

        HBox lbox = new HBox(5);
        HBox.setHgrow(lTitle, Priority.ALWAYS);
        lbox.setAlignment(Pos.CENTER_LEFT);
        lbox.getChildren().addAll(lTitle, localIndicator, btnRefresh);

        localRoot = new TreeItem<>(lbox);
        localRoot.getValue().setId("treeCell");

        networkIndicator = new ProgressIndicator();
        networkIndicator.setVisible(false);
        networkIndicator.setManaged(false);
        networkIndicator.setMaxWidth(20);
        networkIndicator.setMaxHeight(20);

        Label nTitle = new Label("Network Scan");
        nTitle.setMaxWidth(Double.MAX_VALUE);

        ImageView settings = new ImageView(new Image(getClass().getResource("/mattw/jexplorer/img/settings.png").toExternalForm()));
        settings.setFitHeight(16);
        settings.setFitWidth(16);
        settings.setCursor(Cursor.HAND);

        ImageView trash = new ImageView(new Image(getClass().getResource("/mattw/jexplorer/img/trash.png").toExternalForm()));
        trash.setFitHeight(16);
        trash.setFitWidth(16);
        trash.setCursor(Cursor.HAND);

        ImageView start = new ImageView(new Image(getClass().getResource("/mattw/jexplorer/img/start.png").toExternalForm()));
        start.setFitHeight(16);
        start.setFitWidth(16);
        start.setCursor(Cursor.HAND);

        btnSettings = new Button();
        btnSettings.setGraphic(settings);

        btnTrash = new Button();
        btnTrash.setGraphic(trash);
        btnTrash.setDisable(true);

        btnStart = new Button();
        btnStart.setGraphic(start);
        btnStart.setDisable(true);

        HBox hbox = new HBox(5);
        HBox.setHgrow(nTitle, Priority.ALWAYS);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().addAll(nTitle, networkIndicator, btnTrash, btnStart, btnSettings);

        networkRoot = new TreeItem<>(hbox);
        networkRoot.getValue().setId("treeCell");

        TreeItem dummyRoot = new TreeItem<>();
        dummyRoot.getChildren().addAll(localRoot, networkRoot);

        Label label = new Label("TabbedPane Here");

        tree = new TreeView<>(dummyRoot);
        tree.setShowRoot(false);
        tree.setMaxWidth(300);
        tree.getSelectionModel().selectedItemProperty().addListener((o, ov, treeItem) -> {
            label.setText(treeItem.toString());
        });
        tree.setCellFactory((callback) -> {
           return new MyTreeCell();
        });

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);
        split.getItems().addAll(tree, label);

        StackPane main = new StackPane();
        main.setAlignment(Pos.TOP_CENTER);
        main.getChildren().addAll(split);

        Scene scene = new Scene(main, 760, 500);
        stage.setScene(scene);
        stage.setTitle("JExplorer");
        stage.setOnCloseRequest(we -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
        app.startLocalScan();
    }

    /**
     * Cycles A through Z to check for available drive paths.
     * Local only.
     */
    private void startLocalScan() {
        new Thread(() -> {
            Platform.runLater(() -> {
                localRoot.getChildren().clear();
                localIndicator.setVisible(true);
                localIndicator.setManaged(true);
                btnRefresh.setDisable(true);
            });
            System.out.println("Scanning for local drives.");
            IntStream.range('A', 'Z').forEach(c -> {
                File drive = new File((char) c+":\\\\");
                if(drive.exists()) {
                    System.out.println("Drive found.. "+(char) c+":\\\\");
                    Platform.runLater(() -> {
                        TreeItem<Node> driveNode = new TreeItem<>(new Label(drive.getAbsolutePath()));
                        localRoot.getChildren().add(driveNode);
                        localRoot.setExpanded(true);
                    });
                }
            });
            System.out.println("Scan complete.");
            Platform.runLater(() -> {
                localIndicator.setVisible(false);
                localIndicator.setManaged(false);
                btnRefresh.setDisable(false);
            });
        }).start();
    }
}
