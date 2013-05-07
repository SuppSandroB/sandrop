package org.sandrop.webscarab.model;

public class ClientDescriptor {
    private String namespace;
    private String name;
    private String version;
    private String address;
    private int port;
    private int id;
    
    public ClientDescriptor(String namespace, String name, String ver, String address, int port, int id) {
        this.namespace = namespace;
        this.name = name;
        this.version = ver;
        this.address = address;
        this.port = port;
        this.id = id;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public int  getPort() {
        return port;
    }
    
    public int  getId() {
        return id;
    }
    
    public String getAddress() {
        return address;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ClientDescriptor) {
            boolean c1 = ((ClientDescriptor) o).namespace.compareTo(this.namespace) == 0;
            boolean c2 = ((ClientDescriptor) o).version.compareTo(this.version) == 0;
            boolean c3 = ((ClientDescriptor) o).address.compareTo(this.address) == 0;
            boolean c4 = ((ClientDescriptor) o).port == this.port;
            
            return c1 && c2 && c3 && c4;
        }
    
        return false;
    }
}
