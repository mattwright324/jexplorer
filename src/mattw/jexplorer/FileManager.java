package mattw.jexplorer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class FileManager extends VBox {

    public VBox fileList;
    public DrivePath path;
    public List<FilePane> history = new ArrayList<>();
    public TextField pathField;
    public ProgressIndicator loading;
    public ComboBox<String> orderBy;

    public FileManager() {
        super();
        ImageView backImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_back.png")));
        backImg.setFitHeight(16);
        backImg.setFitWidth(16);
        Button back = new Button();
        back.setGraphic(backImg);
        back.setOnAction(ae -> goBack());
        ImageView reloadImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_reload.png")));
        reloadImg.setFitHeight(16);
        reloadImg.setFitWidth(16);
        Button reload = new Button();
        reload.setGraphic(reloadImg);
        reload.setOnAction(ae -> reload());
        ImageView homeImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_home.png")));
        homeImg.setFitHeight(16);
        homeImg.setFitWidth(16);
        Button home = new Button();
        home.setGraphic(homeImg);
        home.setOnAction(ae -> goHome());
        pathField = new TextField();
        pathField.setPromptText("C:\\\\Samba\\Or\\NetworkDrive\\PathHere\\");
        pathField.setEditable(false);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        loading = new ProgressIndicator();
        loading.setMaxHeight(25);
        loading.setMaxWidth(25);
        loading.setVisible(false);

        HBox pathbox = new HBox();
        pathbox.setAlignment(Pos.CENTER_LEFT);
        pathbox.getChildren().addAll(home,back,reload,pathField,loading);
        HBox.setHgrow(pathbox, Priority.ALWAYS);

        orderBy = new ComboBox<>();
        orderBy.getItems().addAll("By File", "By Date", "By Size", "By File Count", "By Directory");
        orderBy.setOnAction(ae -> sort());
        orderBy.getSelectionModel().select(0);

        HBox fileMenu = new HBox(10);
        fileMenu.setPadding(new Insets(0, 10, 0, 10));
        fileMenu.setAlignment(Pos.CENTER_LEFT);
        fileMenu.getChildren().addAll(orderBy, pathbox);

        fileList = new VBox(5);
        fileList.setAlignment(Pos.TOP_CENTER);
        fileList.setFillWidth(true);
        fileList.setPadding(new Insets(10,10,10,10));
        ScrollPane scroll = new ScrollPane(fileList);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);

        getChildren().addAll(fileMenu, scroll);
    }

    public void sort() {
        int order = orderBy.getSelectionModel().getSelectedIndex();
        Comparator<FilePane> comp = null;
        if(order == 0) {
            comp = Comparator.comparing(FilePane::getFileNameLower);
        } else if(order == 1) {
            comp = Comparator.comparingLong(FilePane::getFileDate);
            comp = comp.reversed();
        } else if(order == 2) {
            comp = Comparator.comparingLong(FilePane::getFileSize);
            comp = comp.reversed();
        } else if(order == 3) {
            comp = Comparator.comparingLong(FilePane::getFileCount);
            comp = comp.reversed();
        } else if(order == 4) {
            comp = Comparator.comparing(FilePane::getDirectory);
            comp = comp.reversed();
        }
        ObservableList<Node> list = fileList.getChildren();
        FXCollections.sort(list, Comparator.comparing(node -> (FilePane) node, comp));
        IntStream.range(0,  fileList.getChildren().size()).forEach(i -> checkOdd(i, fileList.getChildren().get(i)));
    }

    public void checkOdd(int i, Node fp) {
        if(i % 2 == 0) {
            fp.setId("filePane");
        } else {
            fp.setId("filePaneOdd");
        }
    }

    public void loadDrive(DrivePath drivePath) {
        loading.setVisible(true);
        path = drivePath;
        history.clear();
        history.add(path.filePane);
        pathField.setText(path.getBasePath());
        fileList.getChildren().clear();
        Task<Void> task = new Task<Void>() {
            protected Void call() throws Exception {
                List<FilePane> list = path.filePane.getFileList();
                Platform.runLater(() -> {
                    // stage.setTitle("JExplorer - Showing "+list.size()+" files");
                    fileList.getChildren().addAll(list);
                    loading.setVisible(false);
                    sort();
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    public void loadDirectory(FilePane filePane) {
        if(filePane.getDirectory()) {
            if(!history.stream().anyMatch(f -> f.getFilePath().equals(filePane.getFilePath()))) {
                history.add(filePane);
            }
            loading.setVisible(true);
            pathField.setText(filePane.getFilePath());
            fileList.getChildren().clear();
            Task<Void> task = new Task<Void>() {
                protected Void call() throws Exception {
                    List<FilePane> list = filePane.getFileList();
                    Platform.runLater(() -> {
                        // stage.setTitle("JExplorer - Showing "+list.size()+" files");
                        fileList.getChildren().addAll(list);
                        loading.setVisible(false);
                        sort();
                    });
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    public void goHome() {
        loadDrive(path);
    }

    public void goBack() {
        System.out.println(history);
        history.remove(history.size()-1);
        loadDirectory(history.get(history.size()-1));
    }

    public void reload() {
        loadDirectory(history.get(history.size()-1));
    }
}
