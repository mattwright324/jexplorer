package io.mattw.jexplorer;

class Credential {

    private final String user;
    private final String pass;
    private final String domain;

    public Credential(String username, String password, String domain) {
        this.user = username;
        this.pass = password;
        this.domain = domain;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getDomain() {
        return domain;
    }

    public String toString() {
        return user + ":" + pass + "|" + domain;
    }

}
