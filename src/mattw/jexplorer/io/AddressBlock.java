package mattw.jexplorer.io;

import java.io.IOException;

public class AddressBlock {
	
	public Address start;
	public Address end;
	public boolean isCidr = false;
	public int cidrValue = -1;
	
	public AddressBlock(Address s, Address e) {
		start = s;
		end = e;
	}
	
	public AddressBlock(String cidr) throws IOException {
		if(!isCIDR(cidr)) throw new IOException("Improper CIDR format.");
		String[] parts = cidr.split("/");
		cidrValue = Integer.parseInt(parts[1]);
		start = new Address(parts[0]);
		end = new Address(start.decimal+(long)Math.pow(2, 32-cidrValue)-1);
		isCidr = true;
	}
	
	public long size() {
		return end.decimal - start.decimal;
	}
	
	public static boolean isCIDR(String cidr) {
		return cidr.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}/[0-9]{1,2}"); // x.x.x.x/p
	}
	
	public String toString() {
		return isCidr ? start+"/"+cidrValue : start+"-"+end;
	}
}
