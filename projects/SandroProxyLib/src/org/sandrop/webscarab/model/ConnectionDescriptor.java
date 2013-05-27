package org.sandrop.webscarab.model;

public class ConnectionDescriptor {
    private String[] namespaces;
    private String[] names;
    private String[] versions;
    private String laddress;
    private int lport;
    private String lportprotocol;
    private String raddress;
    private String rhostname;
    private int rport;
    private String rportprotocol;
    private int id;
    private int state;
    private String type;
    
    public ConnectionDescriptor(String[] namespaces, String[] names, String[] versions, String type, int state, String laddress, int lport, String raddress, int rport, String rhostname,  int id) {
        this.namespaces = namespaces;
        this.names = names;
        this.versions = versions;
        this.laddress = laddress;
        this.lport = lport;
        this.lportprotocol = getPortProtocol(lport);
        this.raddress = raddress;
        this.rport = rport;
        this.rportprotocol = getPortProtocol(rport);
        this.id = id;
        this.state = state;
        this.type = type;
        this.rhostname = rhostname;
    }
    
    public String[] getNamespaces() {
        return namespaces;
    }
    
    public String getNamespace() {
        if (namespaces != null && namespaces.length > 0)
        {
            return namespaces[0];
        }
        return "";
    }
    
    public String[] getNames() {
        return names;
    }
    
    public String getName() {
        if (names != null && names.length > 0)
        {
            return names[0];
        }
        return "";
    }
    
    public String[] getVersions() {
        return versions;
    }
    
    public int  getStateCode() {
        return state;
    }
    
    public String  getStateShortCode() {
        return getStateShortDesc(state);
    }
    
    public String  getStateDescription() {
        // TODO
        return getStateShortDesc(state);
    }
    
    public String getType() {
        return type;
    }
    
    public int  getLocalPort() {
        return lport;
    }
    
    public String  getLocalPortProtocol() {
        return lportprotocol;
    }
    
    public int  getRemotePort() {
        return rport;
    }
    
    public String  getRemotePortProtocol() {
        return rportprotocol;
    }
    
    public String getRemoteHostName() {
        return rhostname;
    }
    
    public int  getId() {
        return id;
    }
    
    public String getLocalAddress() {
        return laddress;
    }
    
    public String getRemoteAddress() {
        return raddress;
    }
    
    public String getPortProtocol(int port){
        if (port == 21){
            return "ftp";
        }else if (port == 22){
            return "ssh";
        }else if (port == 25){
            return "smtp";
        }else if (port == 53){
            return "dns";
        }else if (port == 80){
            return "http";
        }else if (port == 110){
            return "pop3";
        }else if (port == 143){
            return "imap";
        }else if (port == 443){
            return "https";
        }else if (port == 993){
            return "imaps";
        }else if (port == 995){
            return "pop3s";
        }else{
            return String.valueOf(port);
        }
    }
    
    public static String getStateShortDesc(int state){
        if (state == 1){
            return "ESTABLISHED";
        }
        if (state == 2){
            return "SYN-SENT";
        }
        if (state == 3){
            return "SYN_RECV";
        }
        if (state == 4){
            return "FIN_WAIT1";
        }
        if (state == 5){
            return "FIN_WAIT2";
        }
        if (state == 6){
            return "TIME_WAIT";
        }
        if (state == 7){
            return "CLOSE";
        }
        if (state == 8){
            return "CLOSE_WAIT";
        }
        if (state == 9){
            return "LAST_ACK";
        }
        if (state == 10){
            return "LISTEN";
        }
        if (state == 11){
            return "CLOSING";
        }
        if (state == 127){
            return "CLOSED";
        }
        return "UNKNOWN_STATE";
    }
}
