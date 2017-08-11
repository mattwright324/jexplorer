package mattw.jexplorer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jcifs.smb.NtlmPasswordAuthentication;
import mattw.jexplorer.io.Address;
import mattw.jexplorer.io.AddressBlock;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class JExplorer2 extends Application {

    private static JExplorer2 app;

    private ProgressIndicator localIndicator, networkIndicator;
    private TreeView<Node> tree;
    private TreeItem<Node> localRoot, networkRoot;
    private Button btnRefresh = new Button(), btnTrash = new Button(), btnStart = new Button(), btnSettings = new Button();
    private StackPane main = new StackPane(), networkSettings = createSettingsPane();


    private DriveController driveController;

    public static JExplorer2 getApp() { return app; }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Styles TreeCells in the drive selector menu.
     */
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

        btnRefresh.setGraphic(refresh);
        btnRefresh.setTooltip(new Tooltip("Refresh list of local drives."));
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

        btnSettings.setTooltip(new Tooltip("Configure scan."));
        btnSettings.setGraphic(settings);
        btnSettings.setOnAction(ae -> {
            if(!main.getChildren().contains(networkSettings)) {
                main.getChildren().add(networkSettings);
            }
        });

        btnTrash.setTooltip(new Tooltip("Clear results."));
        btnTrash.setGraphic(trash);
        btnTrash.setOnAction(ae -> networkRoot.getChildren().clear());

        btnStart.setTooltip(new Tooltip("Start scan."));
        btnStart.setGraphic(start);
        btnStart.setOnAction(ae -> startNetworkScan());

        HBox hbox = new HBox(5);
        HBox.setHgrow(nTitle, Priority.ALWAYS);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().addAll(nTitle, networkIndicator, btnTrash, btnStart, btnSettings);

        networkRoot = new TreeItem<>(hbox);
        networkRoot.getValue().setId("treeCell");

        TreeItem<Node> dummyRoot = new TreeItem<>();
        dummyRoot.getChildren().addAll(localRoot, networkRoot);

        driveController = new DriveController();

        tree = new TreeView<>(dummyRoot);
        tree.setShowRoot(false);
        tree.setMaxWidth(350);
        tree.getSelectionModel().selectedItemProperty().addListener((o, ov, treeItem) -> {
            if(treeItem.getValue() instanceof DriveView) {
                DriveView drivePane = (DriveView) treeItem.getValue();
                driveController.exploreDrive(drivePane.getDrive());
            }
        });
        tree.setCellFactory((callback) -> new MyTreeCell());

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);
        split.getItems().addAll(tree, driveController);

        main.setAlignment(Pos.TOP_CENTER);
        main.getChildren().addAll(split);

        Scene scene = new Scene(main, 760, 500);
        stage.setScene(scene);
        stage.setTitle("JExplorer");
        stage.setOnCloseRequest(we -> {
            Platform.exit();
            System.exit(0);
        });
        stage.getIcons().add(new Image(getClass().getResource("/mattw/jexplorer/img/icon.png").toExternalForm()));
        stage.show();

        btnRefresh.fire();
    }

    /**
     * Settings window for the network scanner.
     * Customize network scan locations and credentials.
     */
    private StackPane createSettingsPane() {
        TextArea networkList = new TextArea();
        networkList.setMinWidth(400);
        networkList.setMinHeight(200);
        networkList.setPromptText("192.168.0.124\r\n192.168.0.0-192.168.255.255\r\n192.168.0.0/16");
        btnStart.disableProperty().bind(networkList.textProperty().isEmpty());

        Button saveClose = new Button("Save Settings");

        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.getChildren().add(saveClose);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(25,25,25,25));
        vbox.setMaxWidth(0);
        vbox.setMaxHeight(0);
        vbox.getChildren().addAll(networkList, hbox);

        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(127,127,127,0.2);");
        overlay.getChildren().add(vbox);
        saveClose.setOnAction(ae -> {
            if(main.getChildren().contains(overlay)) {
                main.getChildren().remove(overlay);
            }
        });

        return overlay;
    }

    /**
     * Sorts network items based on IP.
     */
    private void sortNetworkList() {
        networkRoot.getChildren().sort((left, right) -> {
            if(left.getValue() instanceof DriveView && right.getValue() instanceof DriveView) {
                DriveView dleft = (DriveView) left.getValue(), dright = (DriveView) right.getValue();
                if(dleft.getDriveDecimal() == dright.getDriveDecimal()) {
                    return 0;
                } else if(dleft.getDriveDecimal() > dright.getDriveDecimal()) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return -1;
        });
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
                File file = new File((char) c+":\\\\");
                if(file.exists()) {
                    System.out.println("Drive found.. "+(char) c+":\\\\");
                    Platform.runLater(() -> {
                        TreeItem<Node> driveNode = new TreeItem<>(new DriveView(new Drive(Type.LOCAL, file.getAbsolutePath(), file)));
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
                if(localRoot.getChildren().isEmpty()) {
                    Label error = new Label("No local drives found.");
                    error.setStyle("-fx-text-fill: red");
                    localRoot.getChildren().add(new TreeItem<>(error));
                } else {
                    tree.getSelectionModel().select(localRoot.getChildren().get(0));
                    // driveExplorer.explore(localRoot.getChildren().get(0));
                }
            });
        }).start();
    }

    /**
     * Scans the configured network locations for connectable SMB and FTP systems.
     * Checks for anonymous (FTP) and cycles through provided credentials to obtain file access.
     */
    private void startNetworkScan() {
        Platform.runLater(() -> {
            btnStart.setDisable(true);
            networkIndicator.setVisible(true);
            networkIndicator.setManaged(true);
        });
        new Thread(() -> {
            try {
                AddressBlock block = new AddressBlock("67.232.0.0/16");
                Address addr = block.start;
                ExecutorService es = Executors.newCachedThreadPool();
                for(int i=0; i<32; i++) {
                    es.execute(() -> {
                        Address thisAddr = addr.syncNextAddress();
                        while(thisAddr.decimal < block.end.decimal) {
                            if(portOpen(thisAddr.toString(), 300, 445, 139, 138, 137)) {
                                try {
                                    final NtlmPasswordAuthentication auth = null;
                                    final TreeItem<Node> networkItem = new TreeItem<>(new DriveView( new Drive(Type.SAMBA, "smb://"+thisAddr.ipv4, "...", thisAddr, auth, null) ));
                                    Platform.runLater(() -> {
                                        networkRoot.getChildren().add(networkItem);
                                        sortNetworkList();
                                    });
                                } catch (Exception ignored) {}
                            }
                            if(portOpen(thisAddr.toString(), 300, 21)) {
                                try {
                                    FTPClient client = new FTPClient();
                                    client.setConnectTimeout(300);
                                    client.connect(thisAddr.ipv4);
                                    System.out.println("Connected ftp://"+thisAddr.ipv4);
                                    if(client.login("anonymous", "")) {
                                        final TreeItem<Node> networkItem = new TreeItem<>(new DriveView( new Drive(Type.FTP, "ftp://"+thisAddr.ipv4, "anonymous", thisAddr, client, null) ));
                                        Platform.runLater(() -> {
                                            networkRoot.getChildren().add(networkItem);
                                            sortNetworkList();
                                        });
                                    }
                                    client.disconnect();
                                } catch (Exception ignored) {}
                            }
                            thisAddr = addr.syncNextAddress();
                        }
                    });
                }
                try {
                    es.shutdown();
                    es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch(Exception ignored) {}
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                btnStart.setDisable(false);
                networkIndicator.setVisible(false);
                networkIndicator.setManaged(false);
            });
        }).start();
    }

    private boolean portOpen(final String addr, int millis, int... ports) {
        for(int port : ports) {
            try {
                final Socket soc = new Socket();
                soc.connect(new InetSocketAddress(addr, port), millis);
                soc.close();
                return true;
            } catch (IOException ignored) {}
        }
        return false;
    }
}
