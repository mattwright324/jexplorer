package io.mattw.jexplorer;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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

    public DriveView(Drive drive) {
        super(5);
        this.drive = drive;
        setAlignment(Pos.CENTER_LEFT);
        if(drive.getType() == Type.LOCAL || drive.getType() == Type.LOCAL_SMB) {
            ImageView sysIcon = new ImageView(getFileSystemIcon(drive.getFile()));
            sysIcon.setFitWidth(22);
            sysIcon.setFitHeight(22);

            Label path = new Label(drive.getPathHostName());
            path.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

            getChildren().addAll(sysIcon, path);
        } else if(drive.getType() == Type.SAMBA) {
            Image SMB_ICON = new Image(getClass().getResource("/io/mattw/jexplorer/img/samba.png").toExternalForm());
            ImageView icon = new ImageView(SMB_ICON);
            icon.setFitWidth(30);
            icon.setFitHeight(30);

            Label title = new Label(drive.getPathHostName());
            title.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
            Label subTitle = new Label(drive.getSignIn());
            subTitle.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

            VBox vbox = new VBox();
            vbox.getChildren().addAll(title, subTitle);

            getChildren().addAll(icon, vbox);
        } else if(drive.getType() == Type.FTP) {
            Image FTP_ICON = new Image(getClass().getResource("/io/mattw/jexplorer/img/ftp.png").toExternalForm());
            ImageView icon = new ImageView(FTP_ICON);
            icon.setFitWidth(30);
            icon.setFitHeight(30);

            Label title = new Label(drive.getPathHostName());
            title.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
            Label subTitle = new Label(drive.getSignIn());
            subTitle.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

            VBox vbox = new VBox();
            vbox.getChildren().addAll(title, subTitle);

            getChildren().addAll(icon, vbox);
        }
    }

    public Drive getDrive() { return drive; }

    public long getDriveDecimal() { return drive.getAddress().decimal; }

    public static Image getFileSystemIcon(File file) {
        ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(file);
        if (icon != null) {
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            icon.paintIcon(null, g, 0,0);
            g.dispose();
            return SwingFXUtils.toFXImage(bi, null);
        }
        return DriveController.FILE_ICON;
    }
}
