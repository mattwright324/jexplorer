package io.mattw.jexplorer;

import io.mattw.jexplorer.io.Address;
import io.mattw.jexplorer.io.AddressBlock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class JExplorer extends Application {

    private static Stage stage;
    private static Config config = new Config();
    private SimpleBooleanProperty scanningProperty = new SimpleBooleanProperty(false);
    private TextArea networkList = new TextArea(), credList = new TextArea();
    private ProgressIndicator localIndicator, networkIndicator;
    private TreeView<Node> tree;
    private TreeItem<Node> localRoot, networkRoot;
    private Button btnRefresh = new Button(), btnTrash = new Button(), btnStart = new Button(), btnSettings = new Button();
    private StackPane main = new StackPane(), networkSettings = createSettingsPane();
    private AtomicLong scanValue = new AtomicLong(0);
    private Label scanPosition = new Label(". : . : .");

    private DriveController driveController;

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getStage() { return stage; }

    public static Config getConfig() { return config; }

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
        this.stage = stage;

        Label lTitle = new Label("Local Machine");
        lTitle.setMaxWidth(Double.MAX_VALUE);

        localIndicator = new ProgressIndicator();
        localIndicator.setVisible(false);
        localIndicator.setManaged(false);
        localIndicator.setMaxWidth(20);
        localIndicator.setMaxHeight(20);

        ImageView refresh = new ImageView(new Image(getClass().getResource("/io/mattw/jexplorer/img/refresh.png").toExternalForm()));
        refresh.setFitHeight(16);
        refresh.setFitWidth(16);

        btnRefresh.setGraphic(refresh);
        btnRefresh.setTooltip(new Tooltip("Refresh list of local drives."));
        btnRefresh.setOnAction(ae -> startLocalScan());

        HBox lbox = new HBox(5);
        HBox.setHgrow(lTitle, Priority.ALWAYS);
        lbox.setAlignment(Pos.CENTER_LEFT);
        lbox.getChildren().addAll(lTitle, localIndicator, btnRefresh);

        localRoot = new TreeItem<>(lbox);
        localRoot.getValue().setId("treeCell");

        Label nTitle = new Label("Network Scan");
        nTitle.setMaxWidth(Double.MAX_VALUE);

        ImageView settings = new ImageView(new Image(getClass().getResource("/io/mattw/jexplorer/img/settings.png").toExternalForm()));
        settings.setFitHeight(16);
        settings.setFitWidth(16);

        ImageView trash = new ImageView(new Image(getClass().getResource("/io/mattw/jexplorer/img/trash.png").toExternalForm()));
        trash.setFitHeight(16);
        trash.setFitWidth(16);

        ImageView start = new ImageView(new Image(getClass().getResource("/io/mattw/jexplorer/img/start.png").toExternalForm()));
        start.setFitHeight(16);
        start.setFitWidth(16);

        btnSettings.setTooltip(new Tooltip("Configure scan."));
        btnSettings.setGraphic(settings);
        btnSettings.setOnAction(ae -> {
            if(!main.getChildren().contains(networkSettings)) {
                Platform.runLater(() -> main.getChildren().add(networkSettings));
            }
        });

        btnTrash.setTooltip(new Tooltip("Clear results."));
        btnTrash.setGraphic(trash);
        btnTrash.setOnAction(ae -> Platform.runLater(() -> networkRoot.getChildren().clear()));

        btnStart.setTooltip(new Tooltip("Start scan."));
        btnStart.setGraphic(start);
        btnStart.setOnAction(ae -> startNetworkScan(credList.getText(), networkList.getText()));

        HBox hbox = new HBox(5);
        HBox.setHgrow(nTitle, Priority.ALWAYS);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().addAll(nTitle, btnTrash, btnStart, btnSettings);

        networkRoot = new TreeItem<>(hbox);
        networkRoot.getValue().setId("treeCell");

        TreeItem<Node> dummyRoot = new TreeItem<>();
        dummyRoot.getChildren().add(localRoot);
        dummyRoot.getChildren().add(networkRoot);

        driveController = new DriveController();

        tree = new TreeView<>(dummyRoot);
        tree.setShowRoot(false);
        tree.getSelectionModel().selectedItemProperty().addListener((o, ov, treeItem) -> {
            if(treeItem.getValue() instanceof DriveView) {
                DriveView drivePane = (DriveView) treeItem.getValue();
                driveController.exploreDrive(drivePane.getDrive());
            }
        });
        tree.setCellFactory((callback) -> new MyTreeCell());
        VBox.setVgrow(tree, Priority.ALWAYS);

        networkIndicator = new ProgressBar();
        networkIndicator.setVisible(false);
        networkIndicator.setManaged(false);
        networkIndicator.setMaxWidth(Double.MAX_VALUE);
        networkIndicator.setMaxHeight(30);
        networkIndicator.setPadding(new Insets(0,5,0,5));

        scanPosition.setPadding(new Insets(0,5,5,5));
        scanPosition.managedProperty().bind(networkIndicator.managedProperty());
        scanPosition.visibleProperty().bind(networkIndicator.managedProperty());

        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setFillWidth(true);
        vbox.getChildren().addAll(tree, networkIndicator, scanPosition);

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.4);
        split.getItems().addAll(vbox, driveController);
        SplitPane.setResizableWithParent(vbox, Boolean.FALSE);

        main.setAlignment(Pos.TOP_CENTER);
        main.getChildren().addAll(split);

        Scene scene = new Scene(main, 760, 500);
        stage.setScene(scene);
        stage.setTitle("JExplorer");
        stage.setOnCloseRequest(we -> {
            Platform.exit();
            System.exit(0);
        });
        stage.getIcons().add(new Image(getClass().getResource("/io/mattw/jexplorer/img/icon.png").toExternalForm()));
        stage.show();

        btnRefresh.fire();
    }

    /**
     * Settings window for the network scanner.
     * Customize network scan locations and credentials.
     */
    private StackPane createSettingsPane() {
        try {
            config.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Label label = new Label("Scan Configuration");
        label.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 18));

        CheckBox checkSmb = new CheckBox("Scan SMB (Ports 137, 138, 139, 445)");
        checkSmb.setSelected(config.scanSmb);

        CheckBox checkFtp = new CheckBox("Scan FTP (Port 21)");
        checkFtp.setSelected(config.scanFtp);

        CheckBox inspectFtpFolders = new CheckBox("Inspect FTP Folders for contents count (slows down listing)");
        inspectFtpFolders.setSelected(config.inspectFtpFolders);

        credList.setMinWidth(400);
        credList.setMinHeight(100);
        credList.setPromptText("username:\r\nusername:password\r\nusername:password|domain\r\n# Add Comments");
        credList.setText(config.credentialsList);

        networkList.setMinWidth(400);
        networkList.setMinHeight(100);
        networkList.setPromptText("192.168.0.124\r\n192.168.0.0-192.168.255.255\r\n192.168.0.0/16\r\n# Add Comments");
        networkList.setText(config.networksList);
        btnStart.disableProperty().bind(networkList.textProperty().isEmpty().or(scanningProperty));

        Button saveClose = new Button("Save Settings");

        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.getChildren().add(saveClose);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(25,25,25,25));
        vbox.setMaxWidth(0);
        vbox.setMaxHeight(0);
        vbox.setStyle("-fx-background-color: #eeeeee; -fx-opacity: 1;");
        vbox.getChildren().addAll(label, checkSmb, checkFtp, inspectFtpFolders, credList, networkList, hbox);

        VBox vbox0 = new VBox();
        vbox0.setMaxWidth(0);
        vbox0.setAlignment(Pos.CENTER_LEFT);
        vbox0.setStyle("-fx-background-color: rgba(92,92,92,0.5);");
        vbox0.getChildren().addAll(vbox);

        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.CENTER_LEFT);
        overlay.setStyle("-fx-background-color: rgba(127,127,127,0.2);");
        overlay.getChildren().add(vbox0);
        saveClose.setOnAction(ae -> {
            if(main.getChildren().contains(overlay)) {
                config.setScanSmb(checkSmb.isSelected());
                config.setScanFtp(checkFtp.isSelected());
                config.setInspectFtpFolders(inspectFtpFolders.isSelected());
                config.setCredentials(credList.getText());
                config.setNetworks(networkList.getText());
                try { config.save(); } catch (Exception e) { e.printStackTrace(); }
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
                return Long.compare(dleft.getDriveDecimal(), dright.getDriveDecimal());
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
            System.out.println("Local Scan complete.");
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
                }
            });
        }).start();
    }

    class Credential {
        private String user="", pass="", domain="";

        public Credential(String username, String password, String domain) {
            this.user = username;
            this.pass = password;
            this.domain = domain;
        }

        public String getUser() { return user; }
        public String getPass() { return pass; }
        public String getDomain() { return domain; }

        public String toString() {
            return user+":"+pass+"|"+domain;
        }
    }

    private List<Credential> parseCredentials(String credentials) {
        List<Credential> creds = new ArrayList<>();
        Pattern p = Pattern.compile("(.*):(.*)(?:\\|(.*))");
        for(String s : Arrays.asList(credentials.split("\n"))) {
            if(!s.startsWith("#")) {
                if(!s.contains("|") && !s.endsWith("|")) s += "|";
                Matcher m = p.matcher(s);
                while(m.find()) {
                    if(m.groupCount() == 3) {
                        String user = m.group(1);
                        String pass = m.group(2);
                        String domain = m.group(3);
                        creds.add(new Credential(user, pass, domain));
                    }
                }
            }
        }
        return creds;
    }

    /**
     * Returns either String address or AddressBlock objects.
     */
    private List<Object> parseNetworkLocations(String locations) {
        List<Object> locs = new ArrayList<>();
        AddressBlock block = null;
        for(String s : Arrays.asList(locations.split("\n"))) {
            if(!s.startsWith("#")) {
                try {
                    locs.add(InetAddress.getByName(s).getHostAddress());
                } catch (Exception ignored) {
                    try {
                        if(s.contains("-") || s.contains("/")) {
                            if(s.contains("/") && AddressBlock.isCIDR(s)) {
                                block = new AddressBlock(s);
                            } else if(s.contains("-")) {
                                String[] parts = s.split("-");
                                if(parts.length == 2) {
                                    block = new AddressBlock(new Address(parts[0].trim()), new Address(parts[1].trim()));
                                }
                            }
                            if(block != null) {
                                locs.add(block);
                            }
                        } else {
                            locs.add(s);
                        }
                    } catch (Exception e) {
                        System.err.println(s);
                    }
                }
            }
        }
        System.out.println(locs);
        return locs;
    }

    /**
     * Scans the configured network locations for connectable SMB and FTP systems.
     * Checks for anonymous (FTP) and cycles through provided credentials to obtain file access.
     */
    private void startNetworkScan(final String credentials, final String locations) {
        Platform.runLater(() -> {
            scanningProperty.set(true);
            networkIndicator.setProgress(-1);
            networkIndicator.setVisible(true);
            networkIndicator.setManaged(true);
        });
        System.out.println("Starting network scan.");
        new Thread(() -> {
            List<Credential> creds = parseCredentials(credentials);
            List<Object> locs = parseNetworkLocations(locations);
            long maximum = 0;
            scanValue.set(0);
            for(Object o : locs) {
                if(o instanceof String) maximum++;
                if(o instanceof AddressBlock) maximum += ((AddressBlock) o).size();
            }
            final double max = maximum;
            for(Object o : locs) {
                if(o instanceof String) {
                    Platform.runLater(() -> scanPosition.setText(o.toString()));
                    try {
                        Address address = new Address(o.toString());
                        if(tryConnections(address, creds)) {
                            System.out.println("One or more successes: "+address);
                        }
                    } catch (Exception ignored) {}
                    scanValue.incrementAndGet();
                    Platform.runLater(() -> networkIndicator.setProgress((scanValue.get() / max)));
                } else if(o instanceof AddressBlock) {
                    AddressBlock block = (AddressBlock) o;
                    Address addr = block.start;
                    ExecutorService es = Executors.newCachedThreadPool();
                    for(int i=0; i<32; i++) {
                        es.execute(() -> {
                            Address thisAddr = addr.syncNextAddress();
                            while(thisAddr.decimal < block.end.decimal) {
                                final Address a = thisAddr;
                                Platform.runLater(() -> scanPosition.setText(a.toString()));
                                if(tryConnections(thisAddr, creds)) {
                                    System.out.println("One or more successes: "+thisAddr);
                                }
                                thisAddr = addr.syncNextAddress();
                                scanValue.incrementAndGet();
                                Platform.runLater(() -> networkIndicator.setProgress((scanValue.get() / max)));
                            }
                        });
                    }
                    try {
                        es.shutdown();
                        es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch(Exception ignored) {}
                }
            }
            Platform.runLater(() -> {
                scanningProperty.set(false);
                networkIndicator.setVisible(false);
                networkIndicator.setManaged(false);
            });
            System.out.println("Network scan complete.");
        }).start();
    }

    /**
     * Attempts SMB & FTP connections.
     * Adds successful connections to the network list.
     */
    private boolean tryConnections(Address address, final List<Credential> credentials) {
        boolean hasConnected = false;
        if(config.scanSmb && hasPortOpen(address.toString(), 300, 445, 139, 138, 137)) {
            for(Credential cred : credentials) {
                try {
                    final NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(cred.getDomain(), cred.getUser(), cred.getPass());
                    try {
                        SmbFile[] smbDomains = (new SmbFile("smb://"+address+"/", auth)).listFiles();
                        System.out.println("Connected to ["+address+"] with ["+cred.toString()+"]");
                        for (SmbFile domain : smbDomains) {
                            File f = new File(domain.getPath().replace("smb:", "").replace("/", "\\"));
                            if (f.exists()) {
                                final TreeItem<Node> networkItem = new TreeItem<>(new DriveView( new Drive(Type.LOCAL_SMB, f.getAbsolutePath(), f, cred.toString(), address, auth) ));
                                hasConnected = true;
                                Platform.runLater(() -> {
                                    networkRoot.getChildren().add(networkItem);
                                    sortNetworkList();
                                });
                            } else {
                                try {
                                    domain.listFiles();
                                    final TreeItem<Node> networkItem = new TreeItem<>(new DriveView( new Drive(Type.SAMBA, domain.getPath(), cred.toString(), address, auth, domain) ));
                                    hasConnected = true;
                                    Platform.runLater(() -> {
                                        networkRoot.getChildren().add(networkItem);
                                        sortNetworkList();
                                    });
                                } catch (Exception ignored) { }
                            }
                        }
                        break;
                    } catch(NullPointerException npe) {
                        npe.printStackTrace();
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }
        }
        if(config.scanFtp && hasPortOpen(address.toString(), 300, 21)) {
            for(Credential cred : credentials) {
                try {
                    FTPClient client = new FTPClient();
                    client.setConnectTimeout(300);
                    client.connect(address.ipv4);
                    System.out.println("Connected ftp://"+address.ipv4);
                    if(client.login(cred.getUser(), cred.getPass())) {
                        final TreeItem<Node> networkItem = new TreeItem<>(new DriveView( new Drive(Type.FTP, "ftp://"+address.ipv4, cred.toString(), address, client, null) ));
                        Platform.runLater(() -> {
                            networkRoot.getChildren().add(networkItem);
                            sortNetworkList();
                        });
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
        return hasConnected;
    }

    /**
     * Checks if at least one of provided port(s) are open.
     */
    private boolean hasPortOpen(final String address, int timeoutMillis, int... ports) {
        for(int port : ports) {
            try {
                final Socket soc = new Socket();
                soc.connect(new InetSocketAddress(address, port), timeoutMillis);
                soc.close();
                return true;
            } catch (IOException ignored) {}
        }
        return false;
    }
}
