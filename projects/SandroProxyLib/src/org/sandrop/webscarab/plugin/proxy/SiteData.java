package org.sandrop.webscarab.plugin.proxy;

import java.security.cert.X509Certificate;
import java.util.List;

public class SiteData{
    public String name;
    public X509Certificate[] certs;
    public String tcpAddress;
    public int destPort;
    public int sourcePort;
    public int appUID;
}