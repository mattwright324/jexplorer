package mattw.jexplorer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Stack;

/**
 * Controller for a selected drive in the TreeView.
 * Manages connection, file traversal, searching, copying, deleting, and more.
 */
public class DriveController extends StackPane {
    private DriveExplorer explorer = new DriveExplorer();
    private Label pleaseSelect = new Label("Please select a drive.");

    /**
     * Tabbed file explorer, search, and about for each selected drive.
     */
    private class DriveExplorer extends TabPane {
        private final Image BACK_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/back.png").toExternalForm());
        private final Image HOME_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/home.png").toExternalForm());
        private final Image RELOAD_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/reload.png").toExternalForm());

        private Drive drive;
        private FileWrapper homeDir;
        private Stack<FileWrapper> crumbs = new Stack<>();
        private ListView<FileWrapper> fileList = new ListView<>();
        private TextField currentPath = new TextField();
        private Button btnBack = new Button(), btnHome = new Button(), btnReload = new Button();

        public DriveExplorer() {}

        public DriveExplorer(Drive drive) {
            this.drive = drive;
            if(drive.getType() == Type.LOCAL) {
                homeDir = new FileWrapper(drive.getFile());
                listFiles(homeDir);
            }

            ImageView home = new ImageView(HOME_ICON);
            home.setFitHeight(16);
            home.setFitWidth(16);

            ImageView back = new ImageView(BACK_ICON);
            back.setFitHeight(16);
            back.setFitWidth(16);

            ImageView reload = new ImageView(RELOAD_ICON);
            reload.setFitWidth(16);
            reload.setFitHeight(16);

            btnHome.setGraphic(home);
            btnHome.setOnAction(ae -> {
                crumbs.clear();
                listFiles(homeDir);
            });

            btnBack.setGraphic(back);
            btnBack.setOnAction(ae -> {
                btnBack.setDisable(true);
                if(crumbs.size() >= 2) {
                    crumbs.pop();
                    listFiles(crumbs.pop());
                }
                btnBack.setDisable(false);
            });

            btnReload.setGraphic(reload);
            btnReload.setOnAction(ae -> {
                btnBack.setDisable(true);
                listFiles(crumbs.pop());
                btnBack.setDisable(false);
            });

            currentPath.setText(drive.getPath());
            currentPath.setEditable(false);
            currentPath.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(currentPath, Priority.ALWAYS);

            HBox hbox = new HBox(5);
            hbox.setAlignment(Pos.CENTER_RIGHT);
            hbox.setFillHeight(true);
            hbox.getChildren().addAll(btnHome, btnBack, btnReload, currentPath);

            VBox.setVgrow(fileList, Priority.ALWAYS);

            VBox evbox = new VBox(5);
            evbox.setFillWidth(true);
            evbox.setPadding(new Insets(5,5,5,5));
            evbox.getChildren().addAll(hbox, fileList);

            Tab explorer = new Tab("File Explorer");
            explorer.setContent(evbox);

            Tab search = new Tab("File Search");

            Tab about = new Tab("About this Machine");

            setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            getTabs().addAll(explorer, search, about);
        }

        private void listFiles(FileWrapper dir) {
            crumbs.push(dir);
            Platform.runLater(() -> {
                fileList.getItems().clear();
                currentPath.setText(dir.getPath());
            });
            Arrays.stream(dir.getFile().listFiles())
                    .map(f -> new FileWrapper(f))
                    .forEach(fw -> {
                        fw.setOnMouseClicked(me -> {
                            if(me.getClickCount() == 2 && fw.isDirectory()) {
                                listFiles(fw);
                            }
                        });
                        Platform.runLater(() -> fileList.getItems().add(fw));
                    });
        }

        /**
         * Wrapper for similar functions between File, SmbFile, and FTPFile.
         */
        private class FileWrapper extends HBox {
            private File file;
            private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
            public FileWrapper(File file) {
                super(10);
                setAlignment(Pos.CENTER_LEFT);
                this.file = file;
                ImageView sysIcon = new ImageView(DriveView.getFileSystemIcon(file));
                sysIcon.setFitWidth(22);
                sysIcon.setFitHeight(22);
                Label fileName = new Label(file.getName());
                fileName.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(fileName, Priority.ALWAYS);
                Label fileSize = new Label(file.isDirectory() ? "" : readableFileSize(file.length()));
                fileSize.setStyle("-fx-text-fill: royalblue; -fx-background-color: rgba(127,127,127,0.1); -fx-background-radius: 5;");
                Label fileDate = new Label(sdf.format(new Date(file.lastModified())));
                getChildren().addAll(sysIcon, fileName, fileSize, fileDate);
            }

            public boolean isDirectory() {
                if(file != null && file.isDirectory()) return true;
                return false;
            }

            public String getPath() {
                if(file != null) return file.getAbsolutePath();
                return null;
            }

            public String toString() {
                return getPath();
            }

            private String readableFileSize(long size) {
                if (size <= 0) return "0 B";
                final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
                int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
                return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
            }

            public File getFile() { return file; }
        }

        public Drive getDrive() { return drive; }
    }

    public DriveController() {
        setAlignment(Pos.CENTER);
        exploreDrive(null);
    }

    public DriveExplorer getExplorer() { return explorer; }

    public void exploreDrive(Drive drive) {
        if(drive == null) {
            setAlignment(Pos.CENTER);
            getChildren().clear();
            pleaseSelect.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 24));
            getChildren().add(pleaseSelect);
        } else {
            if(!drive.equals(explorer.getDrive())) {
                explorer = new DriveExplorer(drive);
                getChildren().clear();
                getChildren().add(explorer);
            }
        }
    }
}
