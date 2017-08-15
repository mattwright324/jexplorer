package mattw.jexplorer.io;

import java.io.IOException;

public class Address {

	private static final double POW1 = Math.pow(256, 1);
	private static final double POW2 = Math.pow(256, 2);
	private static final double POW3 = Math.pow(256, 3);

	public String ipv4;
	public long decimal;
	
	public String toString() {
		return ipv4;
	}
	
	public Address(String ip) throws IOException {
		if(!isIPv4(ip)) throw new IOException("Improper IPv4 format [x.x.x.x] != ["+ip+"]");
		decimal = IPtoDEC(ip);
		ipv4 = DECtoIP(decimal);
	}
	
	public Address(long dec) {
		decimal = dec;
		ipv4 = DECtoIP(dec);
	}

	public Address nextAddress() {
		return new Address(decimal+1);
	}

	public Address nextAddress(int dist) {
		return new Address(decimal+dist);
	}

	public synchronized Address syncNextAddress() {
		decimal += 1;
		ipv4 = DECtoIP(decimal);
		return new Address(decimal);
	}
	
	public static boolean isIPv4(String ip) {
		return ip != null && ip.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}"); // More efficient way than regex? | split(".").length == 4 TODO regex not working
	}
	
	public static long IPtoDEC(String ip) throws IOException {
		if(!isIPv4(ip)) { throw new IOException("Improper IPv4 format [x.x.x.x] != ["+ip+"]"); }
		String[] parts = ip.split("\\.");
		int[] partsN = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])};
		return (long) (partsN[0]*POW3 + partsN[1]*POW2 + partsN[2]*POW1 + partsN[3]);
	}
	
	public static String DECtoIP(long dec) {
		int[] parts = new int[4];
		int pos = 0;
		while(dec > 0 && pos < 4) {
			parts[pos] = (int) (dec % 256);
			dec /= 256;
			pos++;
		}
		return parts[3]+"."+parts[2]+"."+parts[1]+"."+parts[0];
	}
}
