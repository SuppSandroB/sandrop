package org.sandrop.webscarab.plugin.proxy;

import java.security.cert.X509Certificate;

public class SiteData{
    public String name;
    public X509Certificate[] certs;
    public String tcpAddress;
    public String hostName;
    public int destPort;
    public int sourcePort;
    public int appUID;
}