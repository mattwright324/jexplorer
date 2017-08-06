package mattw.jexplorer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import mattw.jexplorer.io.FilesTransferrable;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FilePane extends HBox {

    public Type type;
    public File file;
    public SmbFile smbfile;
    public ImageView icon;

    private final Image imgUnknown = new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/about.png"));
    private final Image imgFile = new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/file.png"));
    private final Image imgFolder = new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/smbtypes/folder.png"));
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

    private Label fileName;
    private Label fileType, fileDate, fileSize;

    private String filePath;
    private boolean isDirectory = false;
    private int pos = 0;
    private int fileCount = 0;
    private long size = 0;
    private long date = 0;

    public FilePane(File file) {
        super(10);
        this.file = file;
        type = Type.LOCAL;
        isDirectory = file.isDirectory();
        icon = new ImageView(imgUnknown);
        icon.setFitHeight(24);
        icon.setFitWidth(24);
        filePath = file.getAbsolutePath();
        icon.setImage(DrivePath.getFileSystemIcon(file));
        if(isDirectory) {
            File[] listFile = file.listFiles();
            if(listFile == null) {
                fileCount = -1;
            } else {
                fileCount = listFile.length;
            }
        }
        if(!file.canWrite() || !file.canRead() || fileCount == -1) {
            setDisable(true);
        }
        size = file.length();
        date = file.lastModified();
        build(file.getName(), FileSystemView.getFileSystemView().getSystemTypeDescription(file));
    }

    public boolean getDirectory() {
        return isDirectory;
    }

    public String getFileNameLower() {
        return fileName.getText().toLowerCase();
    }

    public String getFileName() {
        return fileName.getText();
    }

    public long getFileSize() {
        return size;
    }

    public long getFileDate() {
        return date;
    }

    public int getFileCount() {
        return fileCount;
    }

    public String getFilePath() {
        return filePath;
    }

    public FilePane(SmbFile smbfile) {
        super(10);
        boolean unknown = false;
        isDirectory = true;
        size = 0;
        date = 0;
        try {
            isDirectory = smbfile.isDirectory();
            date = smbfile.lastModified();
            size = smbfile.length();
        } catch (SmbException e) {unknown = true;}
        type = Type.SAMBA;
        this.smbfile = smbfile;
        icon = new ImageView(unknown ? imgUnknown : isDirectory ? imgFolder : imgFile);
        icon.setFitHeight(24);
        icon.setFitWidth(24);
        filePath = smbfile.getPath();
        build(smbfile.getName(), unknown ? "Error" : isDirectory ? "File folder" : "File");
    }

    private void build(String fileName, String fileType) {
        setPadding(new Insets(2,2,2,2));
        setAlignment(Pos.CENTER_LEFT);
        this.fileName = new Label(fileName);
        this.fileName.setId("context");
        this.fileType = new Label(fileType);
        this.fileType.setId("filePaneText");
        this.fileType.setMaxWidth(100);
        this.fileType.setPrefWidth(100);
        this.fileType.setMinWidth(100);
        this.fileDate = new Label(sdf.format(new Date(date)));
        this.fileDate.setId("filePaneText");
        this.fileDate.setMaxWidth(125);
        this.fileDate.setPrefWidth(125);
        this.fileDate.setMinWidth(125);
        this.fileSize = new Label(readableFileSize(size));
        this.fileSize.setId("filePaneText");
        this.fileSize.setAlignment(Pos.CENTER_RIGHT);
        this.fileSize.setMaxWidth(50);
        this.fileSize.setPrefWidth(50);
        this.fileSize.setMinWidth(50);

        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().addAll(this.fileType, this.fileSize, this.fileDate);
        if(isDirectory) {
            hbox.getChildren().add(new Label(getFileCount()+" files"));
        }

        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(this.fileName, hbox);

        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem(type == Type.LOCAL ? "Copy" : "Copy to Desktop");
        cm.getItems().addAll(copy);

        copy.setOnAction(ae -> {
            if(type == Type.LOCAL) {
                List<File> list = new ArrayList<>();
                list.add(file);
                FilesTransferrable ft = new FilesTransferrable(list);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ft, ft);
            } else if(type == Type.SAMBA) {
                try {
                    File c = new File(System.getProperty("user.home")+"\\Desktop\\"+smbfile.getName());
                    c.createNewFile();
                    SmbFileInputStream out = new SmbFileInputStream(smbfile);
                    FileOutputStream fis = new FileOutputStream(c);
                    byte[] buff = new byte[(int) smbfile.length()];
                    out.read(buff);
                    out.close();
                    fis.write(buff);
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        getChildren().addAll(icon, vbox);
        setOnMouseClicked(me -> {
            if(me.getClickCount() == 2 && me.getButton().equals(MouseButton.PRIMARY)) {
                JExplorer.getExplorer().loadDirectory(this);
            } else if(me.isPopupTrigger()) {
                cm.show((Node) me.getSource(), me.getScreenX(), me.getScreenY());
            }
        });
    }

    public List<FilePane> getFileList() throws SmbException {
        pos = 0;
        if(type == Type.LOCAL && file.isDirectory()) {
            return Arrays.asList(file.listFiles()).stream().map(FilePane::new).peek(file -> checkOdd(pos, file)).collect(Collectors.toList());
        } else if(type == Type.SAMBA && smbfile.isDirectory()) {
            return Arrays.asList(smbfile.listFiles()).stream().map(FilePane::new).peek(file -> checkOdd(pos, file)).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private void checkOdd(int i, FilePane fp) {
        if(pos % 2 == 0) {
            fp.setId("filePane");
        } else {
            fp.setId("filePaneOdd");
        }
        pos++;
    }

    public String toString() {
        return filePath;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
