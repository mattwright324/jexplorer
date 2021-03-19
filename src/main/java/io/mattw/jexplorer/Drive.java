package io.mattw.jexplorer;

import io.mattw.jexplorer.io.Address;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.InetAddress;

@Deprecated
/**
 * Model for Drives.
 */
public class Drive {

    private static final Logger logger = LogManager.getLogger();

    private Type driveType;
    private String drivePath;
    private String drivePathName;
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
        this.drivePath = drivePathName = path;
        this.file = file;
        try {
            this.address = new Address("127.0.0.1");
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}
    }

    public Drive(Type type, String path, File file, String signIn, Address address, NtlmPasswordAuthentication auth) {
        this.driveType = type;
        this.drivePath = drivePathName = path;
        this.signIn = signIn;
        this.file = file;
        this.hostName = findHostName(this.address = address);
        this.smbAuth = auth;
    }

    public Drive(Type type, String path, String signIn, Address address, NtlmPasswordAuthentication auth, SmbFile file) {
        this.driveType = type;
        this.drivePath = drivePathName = path;
        this.signIn = signIn;
        this.hostName = findHostName(this.address = address);
        this.smbAuth = auth;
        this.smbFile = file;
    }

    public Drive(Type type, String path, String signIn, Address address, FTPClient client, FTPFile file) {
        this.driveType = type;
        this.drivePath = drivePathName = path;
        this.signIn = signIn;
        this.hostName = findHostName(this.address = address);
        this.ftpClient = client;
        this.ftpFile = file;
    }

    private String findHostName(Address address) {
        try {
            String hostName = InetAddress.getByName(address.ipv4).getHostName();
            drivePathName = drivePathName.replace(address.ipv4, hostName);
            return hostName;
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean equals(Object o) {
        return o instanceof Drive && ((Drive) o).getPath().equals(drivePath);
    }

    public Type getType() { return driveType; }
    public String getPath() { return drivePath; }
    public String getPathHostName() { return drivePathName; }
    public Address getAddress() { return address; }
    public String getHostName() { return hostName; }
    public File getFile() { return file; }
    public String getSignIn() { return signIn; }

    public NtlmPasswordAuthentication getSmbAuth() { return smbAuth; }
    public SmbFile getSmbFile() { return smbFile; }

    public FTPClient getFtpClient() { return ftpClient; }
    public FTPFile getFtpFile() { return ftpFile; }
}
