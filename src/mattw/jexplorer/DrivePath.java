package mattw.jexplorer;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;

public class DrivePath extends HBox {
    final static int LOCAL = 0;
    final static int NETWORK = 1;
    final static int ACCESS_FAILED = 2;
    final static int NO_ACCESS = 3;

    private static DrivePath lastSelection;
    private final Image SHARE = new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/share64.png"));
    private final Type type;
    private final int access;
    private String error_msg = "";
    private final String path;
    private final Login login;
    public FilePane filePane;
    private boolean selected;
    private NtlmPasswordAuthentication ntmlAuth;

    private Label label, auth;

    public boolean isSelected() {
        return selected;
    }

    private void setSelected(boolean select) {
        this.selected = select;
        if(!this.equals(lastSelection)) {
            if(lastSelection != null) {
                lastSelection.setSelected(false);
            }
            lastSelection = this;
        }
        if(select) {
            setStyle("-fx-background-color: derive(cornflowerblue, 65%);");
        } else {
            setStyle("");
        }
    }

    public DrivePath(Type type, String path, Login login, int access, NtlmPasswordAuthentication ntmlAuth) {
        super(5);
        setId("drivePane");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(5, 10, 5, 10));
        setMinWidth(290);
        setPrefWidth(300);
        setMaxWidth(310);
        this.type = type;
        this.access = access;
        this.path = path;
        this.login = login;
        this.ntmlAuth = ntmlAuth;

        ImageView img = new ImageView(access != NO_ACCESS ? SHARE : null);
        img.setFitHeight(24);
        img.setFitWidth(24);
        if(type == Type.LOCAL) {
            File f = new File(path);
            img.setImage(getFileSystemIcon(f));
            filePane = new FilePane(f);
        } else {
            try {
                SmbFile smb = new SmbFile(path, ntmlAuth);
                filePane = new FilePane(smb);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        label = new Label(path);
        label.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 12));

        auth = new Label(access == LOCAL ? "Current user." : access == NETWORK ? login.toString() : access == ACCESS_FAILED ? "Access Failed "+login.toString() : "");
        auth.setFont(Font.font("Tahoma", FontWeight.NORMAL, FontPosture.ITALIC, 11));
        auth.setStyle("-fx-text-fill: gray");

        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(label, auth);

        getChildren().addAll(img, vbox);

        if(access == NETWORK || access == LOCAL) {
            setOnMouseClicked(me -> {
                setSelected(true);
                JExplorer.getExplorer().loadDrive(this);
            });
        } else if(access == ACCESS_FAILED) {
            setStyle("-fx-background-color: derive(firebrick, 95%)");
        } else if(access == NO_ACCESS) {
            setStyle("-fx-background-color: derive(lightgray, 45%)");
            label.setStyle("-fx-text-fill: gray");
        }
    }

    public void setErrorMessage(String s) {
        error_msg = s;
        Platform.runLater(() -> auth.setText(s));
    }

    public String getErrorMsg() {
        return error_msg;
    }

    public NtlmPasswordAuthentication getNtmlAuth() {
        return ntmlAuth;
    }

    public String getLogin() {
        return login != null ? login.toString() : "\u0255";
    }

    public Type getType() {
        return type;
    }

    public int getAccess() {
        return access;
    }

    public String getBasePath() {
        return path;
    }

    public static Image getFileSystemIcon(File file) {
        ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(file);
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        icon.paintIcon(null, g, 0,0);
        g.dispose();
        return SwingFXUtils.toFXImage(bi, null);
    }
}
