package io.mattw.jexplorer;

import java.io.Serializable;

public class ConfigData implements Serializable {

    private boolean scanSmb = true;
    private boolean scanFtp = true;
    private boolean inspectFtpFolders = true;
    private String credentialsList = "";
    private String networksList = "";

    public boolean isScanSmb() {
        return scanSmb;
    }

    public ConfigData setScanSmb(boolean scanSmb) {
        this.scanSmb = scanSmb;
        return this;
    }

    public boolean isScanFtp() {
        return scanFtp;
    }

    public ConfigData setScanFtp(boolean scanFtp) {
        this.scanFtp = scanFtp;
        return this;
    }

    public boolean isInspectFtpFolders() {
        return inspectFtpFolders;
    }

    public ConfigData setInspectFtpFolders(boolean inspectFtpFolders) {
        this.inspectFtpFolders = inspectFtpFolders;
        return this;
    }

    public String getCredentialsList() {
        return credentialsList;
    }

    public ConfigData setCredentialsList(String credentialsList) {
        this.credentialsList = credentialsList;
        return this;
    }

    public String getNetworksList() {
        return networksList;
    }

    public ConfigData setNetworksList(String networksList) {
        this.networksList = networksList;
        return this;
    }
}
