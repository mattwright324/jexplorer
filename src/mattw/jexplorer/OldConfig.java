package mattw.jexplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import jcifs.util.Base64;
import mattw.jexplorer.io.Address;
import mattw.jexplorer.io.AddressBlock;

public class OldConfig {
	
	private final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
	private final File CONFIG_FILE = new File("jexplorer.config");
	
	public List<String> addressList = new ArrayList<>();
	public List<AddressBlock> rangeList = new ArrayList<>();
	public List<Login> loginList = new ArrayList<>();
	
	private void loadAs(OldConfig config) {
		setAddressList(config.addressList);
		setRangeList(config.rangeList);
		setLoginList(config.loginList);
	}
	
	public void parseAddresses(List<String> list) throws IOException {
		addressList.clear();
		rangeList.clear();
		AddressBlock block = null;
		for(String s : list) {
			try {
				if(s.contains("-") || s.contains("/")) {
	    			if(s.contains("/") && AddressBlock.isCIDR(s)) {
	    				block = new AddressBlock(s);
	    			} else if(s.contains("-")) {
	    				String[] parts = s.split("-");
	    				if(parts.length == 2) {
	    					block = new AddressBlock(new Address(parts[0].trim()), new Address(parts[1].trim()));
	    				}
	    			} else {
	    			}
	    			if(block != null) {
	    				rangeList.add(block);
	    			}
	    		} else {
	    			addressList.add(s);
	    		}
			} catch (Exception e) {
				System.err.println(s);
			}
		}
	}
	
	public void parseLogins(List<String> list) {
		loginList.clear();
		Pattern p = Pattern.compile("(.*):(.*)(?:\\|(.*))");
    	for(String s : list) {
        	if(!s.contains("|") && !s.endsWith("|")) s += "|";
    		Matcher m = p.matcher(s);
        	while(m.find()) {
        		if(m.groupCount() == 3) {
        			String user = m.group(1);
        			String pass = m.group(2);
        			String domain = m.group(3);
        			loginList.add(new Login(user, pass, domain));
        		}
        	}
    	}
	}
	
	public void setAddressList(List<String> list) {
		addressList = list;
	}
	
	public void addAddressList(List<String> list) {
		addressList.addAll(list);
	}
	
	public void setRangeList(List<AddressBlock> list) {
		rangeList = list;
	}
	
	public void addRangeList(List<AddressBlock> list) {
		rangeList.addAll(list);
	}
	
	public void setLoginList(List<Login> list) {
		loginList = list;
	}
	
	public void addLoginList(List<Login> list) {
		loginList.addAll(list);
	}
	
	public void save() throws IOException {
		if(!CONFIG_FILE.exists()) {
			CONFIG_FILE.createNewFile();
		}
		FileWriter fr = new FileWriter(CONFIG_FILE);
		fr.write(Base64.encode(gson.toJson(this).getBytes()));
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
		String decoded = new String(Base64.decode(json.toString()));
		loadAs(gson.fromJson(decoded, OldConfig.class));
	}
}
