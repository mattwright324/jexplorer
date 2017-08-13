package mattw.jexplorer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.lang.reflect.Modifier;

public class NewConfig {
	
	private final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
	private final File CONFIG_FILE = new File("jexplorer.json");

	public boolean scanSmb = true, scanFtp = true;
	public String credentialsList = "";
	public String networksList = "";
	
	private void loadAs(NewConfig config) {
		setScanSmb(config.scanSmb);
		setScanFtp(config.scanFtp);
		setNetworks(config.networksList);
		setCredentials(config.credentialsList);
	}

	public void setScanFtp(boolean scan) { scanFtp = scan; }
	public void setScanSmb(boolean scan) { scanSmb = scan; }
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
		loadAs(gson.fromJson(json.toString(), NewConfig.class));
	}
}
