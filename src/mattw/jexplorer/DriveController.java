package mattw.jexplorer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Stack;

/**
 * Controller for a selected drive in the TreeView.
 * Manages connection, file traversal, searching, copying, deleting, and more.
 */
public class DriveController extends StackPane {
    private DriveExplorer explorer = new DriveExplorer();
    private Label pleaseSelect = new Label("Please select a drive.");

    private class DriveExplorer extends TabPane {
        private Drive drive;
        private Stack<Drive> crumbs = new Stack<>();

        public DriveExplorer() {}

        public DriveExplorer(Drive drive) {
            this.drive = drive;
            crumbs.add(this.drive);

            VBox evbox = new VBox(5);
            evbox.setPadding(new Insets(5,5,5,5));

            Tab explorer = new Tab("File Explorer");
            explorer.setContent(evbox);

            Tab search = new Tab("File Search");

            Tab about = new Tab("About this Machine");

            setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            getTabs().addAll(explorer, search, about);
        }

        private class FileWrapper extends HBox {
            public FileWrapper(Drive drive) {

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
