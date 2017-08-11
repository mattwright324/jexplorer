package mattw.jexplorer;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import mattw.jexplorer.io.Address;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.net.InetAddress;

/**
 * Model for Drives.
 */
public class Drive {
    private Type driveType;
    private String drivePath;
    private Address address;
    private String hostName;
    private File file;
    private String signIn;

    private NtlmPasswordAuthentication smbAuth;
    private SmbFile smbFile;

    private FTPClient ftpClient;
    private FTPFile ftpFile;

    public Drive(Type type, String path, File file) {
        this.driveType = type;
        this.drivePath = path;
        this.file = file;
    }

    public Drive(Type type, String path, String signIn, Address address, NtlmPasswordAuthentication auth, SmbFile file) {
        this.driveType = type;
        this.drivePath = path;
        this.signIn = signIn;
        this.hostName = findHostName(this.address = address);
        this.smbAuth = auth;
        this.smbFile = file;
    }

    public Drive(Type type, String path, String signIn, Address address, FTPClient client, FTPFile file) {
        this.driveType = type;
        this.drivePath = path;
        this.signIn = signIn;
        this.hostName = findHostName(this.address = address);
        this.ftpClient = client;
        this.ftpFile = file;
    }

    private String findHostName(Address address) {
        try {
            return InetAddress.getByName(address.ipv4).getHostName();
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean equals(Object o) {
        return o instanceof Drive && ((Drive) o).getPath().equals(drivePath);
    }

    public Type getType() { return driveType; }
    public String getPath() { return drivePath; }
    public Address getAddress() { return address; }
    public String getHostName() { return hostName; }
    public File getFile() { return file; }
    public String getSignIn() { return signIn; }

    public NtlmPasswordAuthentication getSmbAuth() { return smbAuth; }
    public SmbFile getSmbFile() { return smbFile; }
}
