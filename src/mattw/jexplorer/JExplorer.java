package mattw.jexplorer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbFile;
import mattw.jexplorer.io.Address;
import mattw.jexplorer.io.AddressBlock;

public class JExplorer extends Application {

	/**
	 * TODO Refactor, refactor, refactor.
	 * TODO Introduce FTP scanning.
	 */

	private static FileManager explorer;

	private ExplorerConfig config = new ExplorerConfig();
	private Stage stage;
	private StackPane layout, main, settings;
	private ComboBox<String> orderBy;
	private ProgressIndicator progress;
	private Label label;
	private Button searchNetwork, searchSettings;
	private VBox driveList;
	
	public static void main(String[] args) {
		launch(args);
	}

	public static FileManager getExplorer() {
		return explorer;
	}

	public StackPane createSettingsPane() {
		Label title = new Label("Settings");
		title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));
		
		TextArea ranges = new TextArea();
		ranges.setPromptText("192.168.0.0/16\r\n172.16.0.0-172.16.255.255\r\nserver1.domain.com");
		ranges.setMinWidth(250);
		ranges.setMinHeight(200);
		String address = config.addressList.stream().collect(Collectors.joining("\n"));
		String range = config.rangeList.stream().map(AddressBlock::toString).collect(Collectors.joining("\n"));
		if(!config.addressList.isEmpty() && !config.rangeList.isEmpty()) address += "\n";
		ranges.setText(address+range);
		TextArea logins = new TextArea();
		logins.setPromptText("username:password|domain\nusername:password\nusername:|domain");
		logins.setMinWidth(250);
		logins.setMinHeight(200);
		logins.setText(config.loginList.stream().map(Login::toString).collect(Collectors.joining("\n")));
		
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
		cancel.setOnAction(ae -> layout.getChildren().remove(glass));
		return glass;
	}
	
	public HBox createMenuBar() {
		searchNetwork = new Button("Search Network");
		ImageView settingImg = new ImageView(new Image(getClass().getResourceAsStream("/mattw/jexplorer/images/settings.png")));
		settingImg.setFitHeight(16);
		settingImg.setFitWidth(16);
		searchSettings = new Button();
		searchSettings.setGraphic(settingImg);
		searchSettings.setOnAction(ae -> layout.getChildren().add(settings));
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
		
		searchNetwork.setOnAction(ae -> searchNetwork());
		return hbox;
	}
	
	public StackPane createMainPane() {
		driveList = new VBox(0);
		driveList.setFillWidth(true);
		driveList.setAlignment(Pos.TOP_CENTER);
		ScrollPane scroll = new ScrollPane(driveList);
		scroll.setFitToHeight(true);
		scroll.setFitToWidth(true);
		
		orderBy = new ComboBox<>();
		orderBy.getItems().addAll("By Path", "By Access", "By Login");
		orderBy.getSelectionModel().select(0);
		orderBy.setOnAction(ae -> sortDrives(orderBy.getSelectionModel().getSelectedIndex()));
		
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

	private void findLocalDrives() {
		IntStream.range('A', 'Z').forEach(c -> {
			File drive = new File((char) c + "://");
			if(drive.exists()) {
				Platform.runLater(() -> driveList.getChildren().add(new DrivePath(Type.LOCAL, drive.getAbsolutePath(), new Login("", "", ""), DrivePath.LOCAL, null)));
			}
		});
	}

	private void findNetworkDrives() {
		System.out.println("Searching for Network Drives");
		config.addressList.stream().forEach(this::checkAddress);
		config.rangeList.stream().forEach(this::checkRange);
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
		} catch (InterruptedException ignored) {}
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
			Platform.runLater(() -> label.setText(addr+" ("+hostName+")"));
			boolean connected = false;
			for(Login login : config.loginList) {
				NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(login.getDomain(), login.getUsername(), login.getPassword());
				try {
					SmbFile[] domains = (new SmbFile("smb://"+hostName+"/", auth)).listFiles();
					System.out.println("Connected to ["+addr+"] with ["+login.toString()+"]");
					Platform.runLater(() -> label.setText(addr+" ("+hostName+") -> "+login.toString()));
					connected = true;
					for (SmbFile domain : domains) {
						File f = new File(domain.getPath().replace("smb:", "").replace("/", "\\"));
						if (f.exists()) {
							addDrive(dp = new DrivePath(Type.LOCAL, f.getAbsolutePath(), login, DrivePath.NETWORK, auth));
						} else {
							try {
								domain.listFiles();
								addDrive(dp = new DrivePath(Type.SAMBA, domain.getPath(), login, DrivePath.NETWORK, auth));
							} catch (SmbAuthException sae) {
								addDrive(dp = new DrivePath(Type.SAMBA, domain.getPath(), login, DrivePath.ACCESS_FAILED, auth));
								dp.setErrorMessage(sae.getLocalizedMessage());
							} catch (Exception ignored) {
							}
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
				addDrive(dp = new DrivePath(Type.SAMBA, hostName, null, DrivePath.NO_ACCESS, null));
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
				Platform.runLater(() -> label.setText(""));
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
}
