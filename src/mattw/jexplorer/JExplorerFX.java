package mattw.jexplorer;

import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class JExplorerFX extends Application {
	
	private JEFXConfig config = new JEFXConfig();
	private Stage stage;
	private StackPane layout, main, settings;

	private ComboBox<String> orderBy;
	private ProgressIndicator progress;
	private Label label;
	public ObservableList<DrivePath> pathList = FXCollections.observableArrayList();
	private static FileManager explorer;
	private Button searchNetwork, searchSettings;
	private VBox driveList;

	private final static int FILE = 0, SAMBA = 1;

	private void findLocalDrives() {
		IntStream.range('A', 'Z').forEach(c -> {
    		File drive = new File((char) c + "://");
    		if(drive.exists()) {
    			Platform.runLater(() -> {
    				driveList.getChildren().add(new DrivePath(FILE, drive.getAbsolutePath(), new Login("", "", ""), DrivePath.LOCAL, null));
    			});
    		}
    	});
	}

	private void findNetworkDrives() {
		System.out.println("Searching for Network Drives");
		config.addressList.stream().forEach(addr -> checkAddress(addr));
		config.rangeList.stream().forEach(block -> checkRange(block));
	}

	private void checkRange(AddressBlock block) {
		System.out.println("Checking block: "+block.toString());
		final int threads = 32;
		ExecutorService es = Executors.newCachedThreadPool();
		for(int i=0; i<threads; i++) {
			final int tid = i;
			es.execute(() -> checkRangeThread(tid, block, threads));
		}
		es.shutdown();
		try {
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
		} catch (InterruptedException e) {}
	}

	private void checkRangeThread(int tid, AddressBlock block, int tcount) {
		Address addr = block.start.nextAddress(tid);
		while(addr.decimal < block.end.decimal) {
			if(portOpen(addr.ipv4, 445, 300) || portOpen(addr.ipv4, 137, 300) || portOpen(addr.ipv4, 138, 300) || portOpen(addr.ipv4, 139, 300)) {
				checkAddress(addr.ipv4);
			}
			addr = addr.nextAddress(tcount);
		}
	}

	private void checkAddress(String addr) {
		try {
			InetAddress inet = InetAddress.getByName(addr);
			DrivePath dp = null;
			String hostName = inet.getHostName();
			Platform.runLater(() -> {
				label.setText(addr+" ("+hostName+")");
			});
			boolean connected = false;
			for(Login login : config.loginList) {
				NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(login.getDomain(), login.getUsername(), login.getPassword());
				try {
					SmbFile[] domains = (new SmbFile("smb://"+hostName+"/", auth)).listFiles();
					System.out.println("Connected to ["+addr+"] with ["+login.toString()+"]");
					Platform.runLater(() -> {
						label.setText(addr+" ("+hostName+") -> "+login.toString());
					});
					connected = true;
					for(int i = 0; i < domains.length; i++) {
						File f = new File(domains[i].getPath().replace("smb:", "").replace("/", "\\"));
						if(f.exists()) {
							addDrive(dp = new DrivePath(FILE, f.getAbsolutePath(), login, DrivePath.NETWORK, auth));
						} else {
							try {
								domains[i].listFiles();
								addDrive(dp = new DrivePath(SAMBA, domains[i].getPath(), login, DrivePath.NETWORK, auth));
							} catch (SmbAuthException sae) {
								addDrive(dp = new DrivePath(SAMBA, domains[i].getPath(), login, DrivePath.ACCESS_FAILED, auth));
								dp.setErrorMessage(sae.getLocalizedMessage());
							} catch (Exception e) {}
						}
					}
					break;
				} catch(NullPointerException npe) {
					npe.printStackTrace();
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}
			if(!connected) {
				addDrive(dp = new DrivePath(SAMBA, hostName, null, DrivePath.NO_ACCESS, null));
				dp.setErrorMessage("Could not connect.");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private void addDrive(DrivePath dp) {
		Platform.runLater(() -> {
			driveList.getChildren().add(dp);
			sortDrives(orderBy.getSelectionModel().getSelectedIndex());
		});
	}

	private void searchNetwork() {
		driveList.getChildren().clear();
		Task<Void> task = new Task<Void>() {
			protected Void call() throws Exception {
				searchNetwork.setDisable(true);
				searchSettings.setDisable(true);
				progress.setVisible(true);
				findLocalDrives();
				findNetworkDrives();
				searchNetwork.setDisable(false);
				searchSettings.setDisable(false);
				progress.setVisible(false);
				Platform.runLater(() -> {
					label.setText("");
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	private boolean portOpen(final String addr, final int port, final int millis) {
		try {
			final Socket soc = new Socket();
			soc.connect(new InetSocketAddress(addr, port), millis);
			soc.close();
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
	
	public static class DrivePath extends HBox {
		final static int LOCAL = 0;
		final static int NETWORK = 1;
		final static int ACCESS_FAILED = 2;
		final static int NO_ACCESS = 3;
		
		private static DrivePath lastSelection;
		private final Image SHARE = new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/share64.png"));
		private final int type, access;
		private String error_msg = "";
		private final String path;
		private final Login login;
		private FilePane file;
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
		
		public DrivePath(int type, String path, Login login, int access, NtlmPasswordAuthentication ntmlAuth) {
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
			if(type == FILE) {
				File f = new File(path);
				img.setImage(getFileSystemIcon(f));
				file = new FilePane(f);
			} else {
				try {
					SmbFile smb = new SmbFile(path, ntmlAuth);
					file = new FilePane(smb);
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
					explorer.loadDrive(this);
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
			Platform.runLater(() -> {
				auth.setText(s);
			});
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
		
		public int getType() {
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
	
	public static class FilePane extends HBox {
		public int type;
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
		
		private boolean selected = false;
		public boolean getSelected() {
			return selected;
		}
		
		public void setSelected(boolean select) {
			selected = select;
			if(select) {
				
			} else {
				
			}
		}
		
		public FilePane(File file) {
			super(10);
			this.file = file;
			type = FILE;
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
			type = SAMBA;
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
			MenuItem copy = new MenuItem(type == FILE ? "Copy" : "Copy to Desktop");
			cm.getItems().addAll(copy);
			
			copy.setOnAction(ae -> {
				if(type == FILE) {
					List<File> list = new ArrayList<File>();
					list.add(file);
					FilesTransferrable ft = new FilesTransferrable(list);
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ft, ft);
				} else if(type == SAMBA) {
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
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
			getChildren().addAll(icon, vbox);
			setOnMouseClicked(me -> {
				if(me.getClickCount() == 2 && me.getButton().equals(MouseButton.PRIMARY)) {
					explorer.loadDirectory(this);
				} else if(me.isPopupTrigger()) {
					cm.show((Node) me.getSource(), me.getScreenX(), me.getScreenY());
				}
			});
		}
		
		public List<FilePane> getFileList() throws SmbException {
			pos = 0;
			if(type == FILE && file.isDirectory()) {
				return Arrays.asList(file.listFiles()).stream().map(file -> new FilePane(file)).peek(file -> checkOdd(pos, file)).collect(Collectors.toList());
			} else if(type == SAMBA && smbfile.isDirectory()) {
				return Arrays.asList(smbfile.listFiles()).stream().map(file -> new FilePane(file)).peek(file -> checkOdd(pos, file)).collect(Collectors.toList());
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
	}
	
	public class FileManager extends VBox {
		public VBox fileList;
		public DrivePath path;
		public List<FilePane> history = new ArrayList<FilePane>();
		public TextField pathField;
		public ProgressIndicator loading;
		public ObservableList<FilePane> files = FXCollections.observableArrayList();
		public ComboBox<String> orderBy;
		
		public FileManager() {
			super();
			ImageView backImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_back.png")));
			backImg.setFitHeight(16);
			backImg.setFitWidth(16);
			Button back = new Button();
			back.setGraphic(backImg);
			back.setOnAction(ae -> {
				goBack();
			});
			ImageView reloadImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_reload.png")));
			reloadImg.setFitHeight(16);
			reloadImg.setFitWidth(16);
			Button reload = new Button();
			reload.setGraphic(reloadImg);
			reload.setOnAction(ae -> {
				reload();
			});
			ImageView homeImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_home.png")));
			homeImg.setFitHeight(16);
			homeImg.setFitWidth(16);
			Button home = new Button();
			home.setGraphic(homeImg);
			home.setOnAction(ae -> {
				goHome();
			});
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
			
			orderBy = new ComboBox<String>();
			orderBy.getItems().addAll("By File", "By Date", "By Size", "By File Count", "By Directory");
			orderBy.setOnAction(ae -> {
				sort();
			});
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
		
		public void loadDrive(DrivePath p) {
			loading.setVisible(true);
			path = p;
			history.clear();
			history.add(path.file);
			pathField.setText(path.getBasePath());
			fileList.getChildren().clear();
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					List<FilePane> list = path.file.getFileList();
					Platform.runLater(() -> {
						stage.setTitle("JExplorer - Showing "+list.size()+" files");
						fileList.getChildren().addAll(list);
						loading.setVisible(false);
						sort();
					});
					return null;
				}
			};
			new Thread(task).start();
		}
		
		public void loadDirectory(FilePane file) {
			if(file.isDirectory) {
				if(!history.stream().anyMatch(f -> f.filePath.equals(file.filePath))) {
					history.add(file);
				}
				loading.setVisible(true);
				pathField.setText(file.filePath);
				fileList.getChildren().clear();
				Task<Void> task = new Task<Void>() {
					protected Void call() throws Exception {
						List<FilePane> list = file.getFileList();
						Platform.runLater(() -> {
							stage.setTitle("JExplorer - Showing "+list.size()+" files");
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
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public StackPane createSettingsPane() {
		Label title = new Label("Settings");
		title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));
		
		TextArea ranges = new TextArea();
		ranges.setPromptText("192.168.0.0/16\r\n172.16.0.0-172.16.255.255\r\nserver1.domain.com");
		ranges.setMinWidth(250);
		ranges.setMinHeight(200);
		String address = config.addressList.stream().collect(Collectors.joining("\n"));
		String range = config.rangeList.stream().map(b -> b.toString()).collect(Collectors.joining("\n"));
		if(!config.addressList.isEmpty() && !config.rangeList.isEmpty()) address += "\n";
		ranges.setText(address+range);
		TextArea logins = new TextArea();
		logins.setPromptText("username:password|domain\nusername:password\nusername:|domain");
		logins.setMinWidth(250);
		logins.setMinHeight(200);
		logins.setText(config.loginList.stream().map(l -> l.toString()).collect(Collectors.joining("\n")));
		
		GridPane grid = new GridPane();
		grid.setVgap(5);
		grid.setHgap(5);
		grid.addColumn(0, new Label("Network Ranges"), ranges);
		grid.addColumn(1, new Label("Logins"), logins);
		
		Button cancel = new Button("Cancel");
		Button finish = new Button("Save and Finish");
		HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER_RIGHT);
		hbox.getChildren().addAll(cancel, finish);
		
		VBox vbox = new VBox(10);
		vbox.setId("stackMenu");
		vbox.setMaxHeight(0);
		vbox.setMaxWidth(0);
		vbox.setFillWidth(true);
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.setAlignment(Pos.CENTER_LEFT);
		vbox.getChildren().addAll(title, grid, hbox);
		
		StackPane glass = new StackPane();
		glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);"); 
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		glass.getChildren().add(vbox);
		finish.setOnAction(ae -> {
			try {
				config.parseAddresses(Arrays.asList(ranges.getText().split("\n")));
				config.parseLogins(Arrays.asList(logins.getText().split("\n")));
				layout.getChildren().remove(glass);
				config.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		cancel.setOnAction(ae -> {
			layout.getChildren().remove(glass);
		});
		return glass;
	}
	
	public HBox createMenuBar() {
		searchNetwork = new Button("Search Network");
		ImageView settingImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/settings.png")));
		settingImg.setFitHeight(16);
		settingImg.setFitWidth(16);
		searchSettings = new Button();
		searchSettings.setGraphic(settingImg);
		searchSettings.setOnAction(ae -> {
			layout.getChildren().add(settings);
		});
		/*ImageView refresh = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_reload.png")));
		refresh.setFitHeight(16);
		refresh.setFitWidth(16);
		Button refreshList = new Button();
		refreshList.setDisable(true);
		refreshList.setGraphic(refresh);*/
		
		HBox search = new HBox();
		search.setAlignment(Pos.CENTER_LEFT);
		search.getChildren().addAll(searchNetwork, searchSettings);
		
		progress = new ProgressIndicator();
		progress.setVisible(false);
		progress.setMaxWidth(20);
		progress.setMaxHeight(20);
		
		label = new Label();
		label.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 12));
		label.setStyle("-fx-border-color: white white lightgray white; -fx-border-width: 0 0 2 0; -fx-text-fill: gray; -fx-border-style: segments(5 2 5 2) line-cap round;");
		
		HBox prog = new HBox(10);
		prog.setAlignment(Pos.CENTER_LEFT);
		prog.getChildren().addAll(progress, label);
		
		HBox hbox = new HBox(10);
		hbox.setPadding(new Insets(10,10,10,10));
		hbox.setAlignment(Pos.CENTER_LEFT);
		hbox.getChildren().addAll(search, prog);
		
		searchNetwork.setOnAction(ae -> {
			searchNetwork();
		});
		return hbox;
	}
	
	public StackPane createMainPane() {
		driveList = new VBox(0);
		driveList.setFillWidth(true);
		driveList.setAlignment(Pos.TOP_CENTER);
		ScrollPane scroll = new ScrollPane(driveList);
		scroll.setFitToHeight(true);
		scroll.setFitToWidth(true);
		
		orderBy = new ComboBox<String>();
		orderBy.getItems().addAll("By Path", "By Access", "By Login");
		orderBy.getSelectionModel().select(0);
		orderBy.setOnAction(ae -> {
			sortDrives(orderBy.getSelectionModel().getSelectedIndex());
		});
		
		// ToggleButton hideNoAccess = new ToggleButton("Hide No-Access");
		
		HBox driveMenu = new HBox(10);
		driveMenu.setMinWidth(300);
		driveMenu.setPadding(new Insets(0, 10, 0, 10));
		driveMenu.setAlignment(Pos.CENTER);
		driveMenu.getChildren().addAll(new Label("Order"), orderBy);
		
		VBox drives = new VBox(10);
		drives.setFillWidth(true);
		drives.setAlignment(Pos.TOP_CENTER);
		drives.getChildren().addAll(driveMenu, scroll);
		explorer = new FileManager();
		
		VBox files = new VBox(10);
		files.setFillWidth(true);
		files.setAlignment(Pos.TOP_CENTER);
		files.getChildren().addAll(explorer);
		
		SplitPane split = new SplitPane();
		split.setStyle("-fx-box-border: transparent;");
		split.getItems().addAll(drives, files);
		split.setDividerPosition(0, 0.0);
		SplitPane.setResizableWithParent(drives, Boolean.FALSE);
		
		VBox vbox = new VBox();
		vbox.setAlignment(Pos.TOP_CENTER);
		vbox.setFillWidth(true);
		vbox.getChildren().addAll(createMenuBar(), split);
		
		StackPane pane = new StackPane();
		pane.setAlignment(Pos.TOP_CENTER);
		pane.getChildren().add(vbox);
		return pane;
	}
	
	public void sortDrives(int order) {
		Comparator<DrivePath> comp = null;
		if(order == 0) {
			comp = Comparator.comparing(DrivePath::getBasePath);
		} else if(order == 1) {
			comp = Comparator.comparing(DrivePath::getAccess);
		} else if(order == 2) {
			comp = Comparator.comparing(DrivePath::getLogin);
		}
		ObservableList<Node> list = driveList.getChildren();
		FXCollections.sort(list, Comparator.comparing(node -> (DrivePath) node, comp));
	}
	
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		config.load();
		config.save();
		jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords","false");
		
		main = createMainPane();
		settings = createSettingsPane();
		
		layout = new StackPane();
		layout.getChildren().add(main);
		
		Scene scene = new Scene(layout, 700, 500);
    	scene.getStylesheets().add(
    			getClass().getResource("/mattw/jexplorer/jexplorer.css").toExternalForm()
    	);
    	stage.setTitle("JExplorer");
    	stage.setScene(scene);
    	stage.getIcons().add(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/btn_remote.png")));
    	stage.setOnCloseRequest(e -> {
    		Platform.exit();
    		System.exit(0);
    	});
    	stage.show();
    	
    	Task<Void> task = new Task<Void>() {
			protected Void call() throws Exception {
				findLocalDrives();
				return null;
			}
		};
		new Thread(task).start();
	}
	
	public static String readableFileSize(long size) {
		if (size <= 0) return "0 B";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
