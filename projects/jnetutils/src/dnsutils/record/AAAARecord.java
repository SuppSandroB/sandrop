package dnsutils.record;

import java.io.*;
import java.net.*;

import dnsutils.DNSInputStream;
import dnsutils.DNSRR;


public class AAAARecord extends DNSRR {
  private  InetAddress ipAddress;
  
  protected void decode (DNSInputStream dnsIn) throws IOException {
      byte[] inetByteArray = new byte[16];
      dnsIn.readByteArray(inetByteArray) ;
      ipAddress = InetAddress.getByAddress(inetByteArray);
  }
  
  public InetAddress getInetAddress () throws UnknownHostException {
    return ipAddress;
  }
  
  public String toString () {
    return getRRName () + "\tinternet address = " + ipAddress.toString();
  }
}
