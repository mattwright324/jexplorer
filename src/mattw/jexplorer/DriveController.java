package mattw.jexplorer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Stack;

/**
 * Controller for a selected drive in the TreeView.
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
        private ComboBox<String> orderBy = new ComboBox<>();

        public DriveExplorer() {}

        public DriveExplorer(Drive drive) {
            this.drive = drive;
            if(drive.getType() == Type.LOCAL || drive.getType() == Type.LOCAL_SMB) {
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

            orderBy.getItems().addAll("By Directory", "By Name", "By Size", "By Date");
            orderBy.getSelectionModel().select(0);

            HBox hbox = new HBox(5);
            hbox.setAlignment(Pos.CENTER_RIGHT);
            hbox.setFillHeight(true);
            hbox.getChildren().addAll(btnHome, btnBack, btnReload, currentPath, orderBy);

            fileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            VBox.setVgrow(fileList, Priority.ALWAYS);
            orderBy.setOnAction(ae -> sortFileList(orderBy.getSelectionModel().getSelectedIndex()));

            VBox evbox = new VBox(5);
            evbox.setFillWidth(true);
            evbox.setPadding(new Insets(5));
            evbox.getChildren().addAll(hbox, fileList);

            Tab explorer = new Tab("File Explorer");
            explorer.setContent(evbox);

            // Tab search = new Tab("File Search");

            Label titleA = new Label("Connection");
            titleA.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 18));

            GridPane gridA = new GridPane();
            gridA.setVgap(5);
            gridA.setHgap(20);
            gridA.addRow(0, new Label("Type"), new TextField(drive.getType().toString()));
            gridA.addRow(1, new Label("Home Directory"), new TextField(drive.getPath()));
            gridA.addRow(2, new Label("Address"), new TextField(String.valueOf(drive.getAddress())));
            gridA.addRow(3, new Label("Host Name"), new TextField(drive.getHostName()));
            gridA.addRow(4, new Label("Credentials"), new TextField(drive.getSignIn()));

            VBox avbox = new VBox(5);
            avbox.setFillWidth(true);
            avbox.setPadding(new Insets(25));
            avbox.getChildren().addAll(titleA, gridA);

            ScrollPane scroll = new ScrollPane(avbox);
            scroll.setFitToHeight(true);
            scroll.setFitToWidth(true);

            Tab about = new Tab("About Drive");
            about.setContent(scroll);

            setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            getTabs().addAll(explorer, about);
        }

        private void sortFileList(int selection) {
            switch(selection) {
                case 1:
                    fileList.getItems().sort(Comparator.comparing(FileWrapper::getFileNameLowercase));
                    break;
                case 2:
                    fileList.getItems().sort(Comparator.comparing(FileWrapper::fileSize).reversed());
                    break;
                case 3:
                    fileList.getItems().sort(Comparator.comparing(FileWrapper::lastModified).reversed());
                    break;
                default:
                    fileList.getItems().sort(Comparator.comparing(FileWrapper::isDirectory).reversed().thenComparing(FileWrapper::getFileNameLowercase));

            }
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
            sortFileList(orderBy.getSelectionModel().getSelectedIndex());
        }

        /**
         * Wrapper for similar functions between File, SmbFile, and FTPFile.
         */
        private class FileWrapper extends HBox {
            private final Image FILE_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/file.png").toExternalForm());
            private final Image FOLDER_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/folder.png").toExternalForm());

            private File file;
            private NtlmPasswordAuthentication auth;
            private SmbFile smbFile;
            private FTPClient ftpClient;
            private FTPFile ftpFile;

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

                Label fileSize = new Label(isDirectory() ? "" : readableFileSize(fileSize()));
                fileSize.setPadding(new Insets(0, 5, 0, 5));
                fileSize.setStyle("-fx-text-fill: cornflowerblue; -fx-background-color: rgba(127,127,127,0.1); -fx-background-radius: 5;");

                Label fileDate = new Label(sdf.format(new Date(lastModified())));
                getChildren().addAll(sysIcon, fileName, fileSize, fileDate);
            }

            public FileWrapper(SmbFile smbFile, NtlmPasswordAuthentication auth) {
                super(10);
                setAlignment(Pos.CENTER_LEFT);
                this.smbFile = smbFile;
                this.auth = auth;

                ImageView sysIcon = new ImageView(isDirectory() ? FOLDER_ICON : FILE_ICON);
                sysIcon.setFitWidth(22);
                sysIcon.setFitHeight(22);

                Label fileName = new Label(smbFile.getName());
                fileName.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(fileName, Priority.ALWAYS);

                Label fileSize = new Label(isDirectory() ? "" : readableFileSize(fileSize()));
                fileSize.setStyle("-fx-text-fill: royalblue; -fx-background-color: rgba(127,127,127,0.1); -fx-background-radius: 5;");

                Label fileDate = new Label(sdf.format(new Date(lastModified())));
                getChildren().addAll(sysIcon, fileName, fileSize, fileDate);
            }

            public FileWrapper(FTPFile ftpFile, FTPClient ftpClient) {
                super(10);
                setAlignment(Pos.CENTER_LEFT);
                this.ftpFile = ftpFile;
                this.ftpClient = ftpClient;

                ImageView sysIcon = new ImageView(isDirectory() ? FOLDER_ICON : FILE_ICON);
                sysIcon.setFitWidth(22);
                sysIcon.setFitHeight(22);

                Label fileName = new Label(ftpFile.getName());
                fileName.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(fileName, Priority.ALWAYS);

                Label fileSize = new Label(isDirectory() ? "" : readableFileSize(fileSize()));
                fileSize.setStyle("-fx-text-fill: royalblue; -fx-background-color: rgba(127,127,127,0.1); -fx-background-radius: 5;");

                Label fileDate = new Label(sdf.format(new Date(lastModified())));
                getChildren().addAll(sysIcon, fileName, fileSize, fileDate);
            }

            public long lastModified() {
                if(file != null) return file.lastModified();
                if(smbFile != null) return smbFile.getLastModified();
                if(ftpFile != null) return ftpFile.getTimestamp().getTimeInMillis();
                return 0;
            }

            public long fileSize() {
                if(file != null) return file.length();
                if(smbFile != null) try { return smbFile.length(); } catch (Exception ignored) {}
                if(ftpFile != null) return ftpFile.getSize();
                return -1;
            }

            public boolean isDirectory() {
                if(file != null) return file.isDirectory();
                if(smbFile != null) try { return smbFile.isDirectory(); } catch (Exception ignored) {}
                if(ftpFile != null) return ftpFile.isDirectory();
                return false;
            }

            public String getFileNameLowercase() {
                if(file != null) return file.getName().toLowerCase();
                if(smbFile != null) return smbFile.getName().toLowerCase();
                if(ftpFile != null) return ftpFile.getName().toLowerCase();
                return "";
            }

            public String getPath() {
                if(file != null) return file.getAbsolutePath();
                if(smbFile != null) return smbFile.getPath();
                if(ftpFile != null)  try { return ftpClient.printWorkingDirectory()+"/"+ftpFile.getName(); } catch (Exception ignored) {}
                return "\\\\file_path_error\\";
            }

            public File getFile() { return file; }

            public SmbFile getSmbFile() { return smbFile; }
            public NtlmPasswordAuthentication getAuth() { return auth; }

            public FTPClient getFtpClient() { return ftpClient; }
            public FTPFile getFtpFile() { return ftpFile; }

            public String toString() {
                return getPath();
            }

            private String readableFileSize(long size) {
                if (size <= 0) return "0 B";
                final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
                int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
                return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
            }


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
