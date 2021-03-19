package io.mattw.jexplorer;

import javafx.scene.Node;
import javafx.scene.control.TreeCell;

@Deprecated
/**
 * Styles TreeCells in the drive selector menu.
 */
class MyTreeCell extends TreeCell<Node> {
    protected void updateItem(Node item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            this.setGraphic(null);
            this.setStyle("");
        } else {
            if ("treeCell".equals(item.getId())) {
                this.setStyle("-fx-background-color: linear-gradient(to bottom, transparent, lightgray);");
            } else {
                this.setStyle("");
            }
            this.setGraphic(item);
        }
    }
}
