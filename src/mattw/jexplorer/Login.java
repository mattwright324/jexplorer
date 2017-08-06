package mattw.jexplorer;

public class Login {
	
	public String username, password, domain = "";
	
	public Login(String u, String p, String d) {
		this.username = u;
		this.password = p;
		this.domain = d;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String toString() {
		return username+":"+password+"|"+domain;
	}
}
