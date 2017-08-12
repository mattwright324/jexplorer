package mattw.jexplorer;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * View wrapper for the Drive class.
 * Displays for selection in the TreeView.
 */
public class DriveView extends HBox {
    private Drive drive;

    private final Image SMB_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/samba.png").toExternalForm());
    private final Image FTP_ICON = new Image(getClass().getResource("/mattw/jexplorer/img/ftp.png").toExternalForm());

    public DriveView(Drive drive) {
        super(5);
        this.drive = drive;
        setAlignment(Pos.CENTER_LEFT);
        if(drive.getType() == Type.LOCAL || drive.getType() == Type.LOCAL_SMB) {
            ImageView sysIcon = new ImageView(getFileSystemIcon(drive.getFile()));
            sysIcon.setFitWidth(22);
            sysIcon.setFitHeight(22);

            Label path = new Label(drive.getPath());

            getChildren().addAll(sysIcon, path);
        } else if(drive.getType() == Type.SAMBA) {
            ImageView icon = new ImageView(SMB_ICON);
            icon.setFitWidth(30);
            icon.setFitHeight(30);

            Label hostName = new Label(drive.getHostName());
            Label path = new Label(drive.getPath());
            path.setStyle("-fx-text-fill: lightgray");

            VBox vbox = new VBox();
            vbox.getChildren().addAll(hostName, path);

            getChildren().addAll(icon, vbox);
        } else if(drive.getType() == Type.FTP) {
            ImageView icon = new ImageView(FTP_ICON);
            icon.setFitWidth(30);
            icon.setFitHeight(30);

            Label hostName = new Label(drive.getHostName());
            Label path = new Label(drive.getPath());
            path.setStyle("-fx-text-fill: lightgray");

            VBox vbox = new VBox();
            vbox.getChildren().addAll(hostName, vbox);

            getChildren().addAll(icon, path);
        }
    }

    public Drive getDrive() { return drive; }

    public long getDriveDecimal() { return drive.getAddress().decimal; }

    public static Image getFileSystemIcon(File file) {
        ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(file);
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        icon.paintIcon(null, g, 0,0);
        g.dispose();
        return SwingFXUtils.toFXImage(bi, null);
    }
}
