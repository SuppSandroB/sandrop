package org.sandroproxy.utils;

import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.sandrop.webscarab.httpclient.HTTPClientFactory;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.plugin.proxy.ITransparentProxyResolver;
import org.sandrop.webscarab.plugin.proxy.SiteData;
import org.sandroproxy.utils.PreferenceUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class NetworkHostNameResolver implements ITransparentProxyResolver{

    
    private Context mContext;
    private String mHostName;
    private boolean mListenerStarted = false;
    private Map<Integer, SiteData> siteData;
    private Map<String, SiteData> ipPortSiteData;
    private List<SiteData> unresolvedSiteData;
    private HostNameResolver hostNameResolver;
    
    public static String DEFAULT_SITE_NAME = "sandroproxy.untrusted";
    private static String TAG = NetworkHostNameResolver.class.getSimpleName();
    private static boolean LOGD = false;
    
    private Thread workerThread;
    
    private native String getOriginalDest(Socket socket);
    
    static
    {
        System.loadLibrary("socketdest");
    }
    
    public NetworkHostNameResolver(Context context){
        mContext = context;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String hostName = pref.getString(PreferenceUtils.proxyTransparentHostNameKey, null);
        if (hostName != null && hostName.length() > 0){
            mHostName = hostName;
        }else{
            startListenerForEvents();
        }
    }
    
    public void cleanUp(){
        if (mListenerStarted){
            stopListenerForEvents();
        }
    }
    
    
    private class HostNameResolver implements Runnable{
        public boolean running = false;

        public void stop() {
          running = false;
          if (workerThread != null && workerThread.getState() == Thread.State.WAITING){
              synchronized (workerThread) {
                  workerThread.notify();
              }
          }
        }

        @Override
        public void run() {
          running = true;
          while(running) {
              if (unresolvedSiteData.size() > 0){
                  final SiteData siteDataCurrent = unresolvedSiteData.remove(0);
                  TrustManager[] trustAllCerts = new TrustManager[] {
                      new X509TrustManager() {
                          public X509Certificate[] getAcceptedIssuers() {
                              return null;
                          }
                          public void checkClientTrusted(X509Certificate[] certs, String authType) {
                          }
                          public void checkServerTrusted(X509Certificate[] certs, String authType) {
                              try{
                                  if (certs != null && certs.length > 0 && certs[0].getSubjectDN() != null){
                                      // getting subject common name 
                                      String cnValue = certs[0].getSubjectDN().getName();
                                      String[] cnValues = cnValue.split(",");
                                      for (String  val : cnValues) {
                                        String[] parts = val.split("=");
                                        if (parts != null && parts.length == 2 && parts[0].equalsIgnoreCase("cn") && parts[1] != null && parts[1].length() > 0){
                                            siteDataCurrent.name = parts[1].trim();
                                            if (LOGD) Log.d(TAG, "Adding hostname to dictionary " + siteDataCurrent.name + " port:" + siteDataCurrent.sourcePort);
                                            siteDataCurrent.certs = certs;
                                            siteData.put(siteDataCurrent.sourcePort, siteDataCurrent);
                                            ipPortSiteData.put(siteDataCurrent.tcpAddress + ":" + siteDataCurrent.destPort, siteDataCurrent);
                                            break;
                                        }
                                      }
                                  }
                              }catch(Exception e){
                                  if (LOGD) Log.d(TAG, e.getMessage());
                              }
                          }
                      }
                  };
                try {
                  if (!ipPortSiteData.containsKey(siteDataCurrent.tcpAddress + ":" + siteDataCurrent.destPort)){
                      String hostName = siteDataCurrent.hostName != null ? siteDataCurrent.hostName : siteDataCurrent.tcpAddress;
                      if (LOGD) Log.d(TAG, "Connect to " + hostName + " on port:" + siteDataCurrent.destPort);
                      HttpUrl base = new HttpUrl("https://" + hostName + ":" + siteDataCurrent.destPort);
                      Socket socket = HTTPClientFactory.getValidInstance().getConnectedSocket(base, false);
                      SSLContext sslContext = SSLContext.getInstance("TLS");
                      sslContext.init(null, trustAllCerts, new SecureRandom());
                      SSLSocketFactory factory = sslContext.getSocketFactory();
                      SSLSocket sslsocket=(SSLSocket)factory.createSocket(socket,socket.getInetAddress().getHostAddress(),socket.getPort(),true);
                      // sslsocket.setEnabledProtocols(new String[] {"SSLv3"});
                      sslsocket.setUseClientMode(true);
                      OutputStream os = sslsocket.getOutputStream();
                      if (LOGD) Log.d(TAG, "Creating ssl session " + siteDataCurrent.tcpAddress + " on port:" + siteDataCurrent.destPort);
                      sslsocket.getSession();
                      // TODO what would be more appropriate to send to server...
                      if (LOGD) Log.d(TAG, "Sending http get request " + siteDataCurrent.tcpAddress + " on port:" + siteDataCurrent.destPort);
                      os.write("GET / HTTP1.0\n\n".getBytes());
                  }else{
                      SiteData siteDataCached = ipPortSiteData.get(siteDataCurrent.tcpAddress + ":" + siteDataCurrent.destPort);
                      if (LOGD) Log.d(TAG, "Already have candidate for " + siteDataCached.name + ". No need to fetch " + siteDataCurrent.tcpAddress + " on port:" + siteDataCurrent.destPort);
                      siteData.put(siteDataCurrent.sourcePort, siteDataCached);
                  }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (LOGD) Log.d(TAG, e.getMessage());
                }
              }else{
                  try {
                        synchronized (workerThread) {
                            workerThread.wait();
                        }
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
              }
          }
        }
    }
    
    private void parseData(Socket socket){
        SiteData newSiteData = new SiteData();
        String originalDest = getOriginalDest(socket);
        String[] tokens = originalDest.split(":");
        if (tokens.length == 2){
            String destIP = tokens[0];
            String hostName = DNSProxy.getHostNameFromIp(destIP);
            int destPort = Integer.parseInt(tokens[1]);
            newSiteData.destPort = destPort;
            newSiteData.tcpAddress = destIP;
            newSiteData.sourcePort = socket.getPort();
            newSiteData.hostName = hostName;
        }else{
            
        }
        if (!siteData.containsKey(newSiteData.sourcePort)){
            if (LOGD) Log.d(TAG, "Add hostname to resolve :" + 
                    newSiteData.tcpAddress + " source port " + 
                    newSiteData.sourcePort + " uid " + 
                    newSiteData.appUID);
            unresolvedSiteData.add(newSiteData);
            if (workerThread != null && workerThread.getState() == Thread.State.WAITING){
                synchronized (workerThread) {
                    workerThread.notify();
                }
            }
        }
    }
    
    private void startListenerForEvents(){
        try{
            siteData = new HashMap<Integer, SiteData>();
            ipPortSiteData = new HashMap<String, SiteData>();
            unresolvedSiteData = new ArrayList<SiteData>();
            hostNameResolver = new HostNameResolver();
            workerThread = new Thread(hostNameResolver, "hostNameResolver");
            workerThread.start();
            mListenerStarted = true;
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void stopListenerForEvents(){
        if (hostNameResolver != null){
            hostNameResolver.stop();
        }
        mListenerStarted = false;
    }
    
    @Override
    public SiteData getSecureHost(Socket socket) {
        SiteData secureHost = null;
        int port =  socket.getPort();
        int localport =  socket.getLocalPort();
        if (LOGD) Log.d(TAG, "Search site for port " + port + " local:" + localport);
        parseData(socket);
        if (siteData.size() == 0 || !siteData.containsKey(port)){
            try {
                for(int i=0; i < 100; i++){
                    Thread.sleep(100);
                    if (siteData.containsKey(port)){
                        secureHost = siteData.get(port);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (secureHost == null && siteData.containsKey(port)){
            secureHost = siteData.get(port);
        }
        if (secureHost == null && mHostName != null && mHostName.length() > 0){
            secureHost =  new SiteData();
            secureHost.name = mHostName;
        }
        if (secureHost == null){
            if (LOGD) Log.d(TAG, "Nothing found for site for port " + port);
        }else{
            if (LOGD) Log.d(TAG, "Having site for port " + port + " " 
                            +  secureHost.name + " addr: " 
                            + secureHost.tcpAddress 
                            + " port " + secureHost.destPort);
        }
        return secureHost;
    }
    
}
