package io.mattw.jexplorer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Modifier;

public class Config {

	private static final Logger logger = LogManager.getLogger();
	
	private final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
	private final File CONFIG_FILE = new File("jexplorer.json");

	public boolean scanSmb = true, scanFtp = true;
	public boolean inspectFtpFolders = true;
	public String credentialsList = "";
	public String networksList = "";
	
	private void loadAs(Config config) {
		setScanSmb(config.scanSmb);
		setScanFtp(config.scanFtp);
		setInspectFtpFolders(config.inspectFtpFolders);
		setNetworks(config.networksList);
		setCredentials(config.credentialsList);
	}

	public void setScanFtp(boolean scan) { scanFtp = scan; }
	public void setScanSmb(boolean scan) { scanSmb = scan; }
	public void setInspectFtpFolders(boolean inspect) { inspectFtpFolders = inspect; }
	public void setNetworks(String list) { networksList = list; }
	public void setCredentials(String list) { credentialsList = list; }
	
	public void save() throws IOException {
		if(!CONFIG_FILE.exists()) {
			CONFIG_FILE.createNewFile();
		}
		FileWriter fr = new FileWriter(CONFIG_FILE);
		fr.write(gson.toJson(this));
		fr.close();
	}
	
	public void load() throws IOException, JsonSyntaxException {
		if(!CONFIG_FILE.exists()) {
			save();
		}
		BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
		StringBuilder json = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			json.append(line);
		}
		br.close();
		loadAs(gson.fromJson(json.toString(), Config.class));
	}
}
