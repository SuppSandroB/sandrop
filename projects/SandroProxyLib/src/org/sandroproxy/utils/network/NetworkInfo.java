package org.sandroproxy.utils.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.conn.util.InetAddressUtils;
import org.sandrop.webscarab.model.ConnectionDescriptor;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class NetworkInfo {
    
    private static boolean LOGD = false;
    private static String TAG = NetworkInfo.class.getSimpleName();
    
    public static String TCP_TYPE ="tcp";
    public static String TCP6_TYPE ="tcp6";
    
    public static final String TCP_4_FILE_PATH     = "/proc/net/tcp";
    public static final String TCP_6_FILE_PATH     = "/proc/net/tcp6";
    
    // (address) (port) (pid)
    public static final String TCP_6_PATTERN   = "\\s+\\d+:\\s([0-9A-F]{32}):([0-9A-F]{4})\\s([0-9A-F]{32}):([0-9A-F]{4})\\s([0-9A-F]{2})\\s[0-9]{8}:[0-9]{8}\\s[0-9A-F]{2}:[0-9A-F]{8}\\s[0-9A-F]{8}\\s+([0-9A-F]+)";
    // (address) (port) (pid)
    public static final String TCP_4_PATTERN   = "\\s+\\d+:\\s([0-9A-F]{8}):([0-9A-F]{4})\\s([0-9A-F]{8}):([0-9A-F]{4})\\s([0-9A-F]{2})\\s[0-9A-F]{8}:[0-9A-F]{8}\\s[0-9A-F]{2}:[0-9A-F]{8}\\s[0-9A-F]{8}\\s+([0-9A-F]+)";

    public static String getIPAddress(boolean useIPv4) throws SocketException {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface intf : interfaces) {
            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress()) {
                    String sAddr = addr.getHostAddress().toUpperCase();
                    boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                    
                    if (useIPv4) {
                        if (isIPv4) 
                            return sAddr;
                    } else {
                        if (!isIPv4) {
                            if (sAddr.startsWith("fe80") || sAddr.startsWith("FE80")) // skipping link-local addresses
                                continue;
                            int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                            return delim<0 ? sAddr : sAddr.substring(0, delim);
                        }
                    }
                }
            }
        }
        
        return "";
    }
    
    private static Map<String, String> resolvedHostnamesCache;
    
    public static String getHostName(String hostAddress)
    {
        String hostname;
       if (resolvedHostnamesCache == null || resolvedHostnamesCache.size() > 1024){
            resolvedHostnamesCache = new HashMap<String, String>();
        }
        if (resolvedHostnamesCache.containsKey(hostAddress)){
            return resolvedHostnamesCache.get(hostAddress);
        }
        if (hostAddress.equals("0.0.0.0")){
            hostname = "*";
        }
        if (hostAddress.equals("::")){
            hostname = "[::]";
        }
        else{
            try{
                hostname = InetAddress.getByName(hostAddress).getHostName();
            }catch (UnknownHostException uh){
                uh.printStackTrace();
                hostname = hostAddress;
            }
        }
        resolvedHostnamesCache.put(hostAddress, hostname);
        return hostname;
    }
    
    public static InetAddress getAddress(int addrSize, String str) {
        InetAddress ret = null;
        byte[] addr = new byte[addrSize];

        for (int i = 0; i < addrSize; i++)
            addr[addrSize - 1 - i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
        try {
            ret = InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
        }

        return ret;
    }
    
    private static String hexToNumber(String strNumber){
        return String.valueOf(Integer.parseInt(strNumber, 16));
    }
    
    public static String getIpv4FromIpv6(String ipv6Address){
        final String part1 = "0000000000000000FFFF";
        final String allZeros = "00000000000000000000000000000000";
        String ipv4 = ipv6Address;
        if (ipv6Address != null && ipv6Address.length() >= 32){
            ipv4 = ipv6Address.trim(); 
            if (ipv6Address.toUpperCase().startsWith(part1)){
                ipv4 = hexToNumber(ipv6Address.substring(30, 32)) + "." + hexToNumber(ipv6Address.substring(28, 30)) + "." + hexToNumber(ipv6Address.substring(26, 28)) + "." + hexToNumber(ipv6Address.substring(24, 26));
            }else if (ipv6Address.equals(allZeros)){
                ipv4 = "0.0.0.0";
            }
        }
        
        return ipv4;
    }
    
    
    public static List<ConnectionDescriptor> getNetworkInfo(Context context, boolean resolveHostNames){
        PackageManager packageManager;
        List<ConnectionDescriptor> listPortInfo;
        packageManager = context.getPackageManager();
        listPortInfo = new ArrayList<ConnectionDescriptor>();
        
        try{
            File tcp4File = new File(NetworkInfo.TCP_4_FILE_PATH);
            BufferedReader readerForTcp4File = new BufferedReader(new FileReader(tcp4File));
            String lineFromTcp4File = "";
            StringBuilder sbFromTcp4File = new StringBuilder();
            
            while ((lineFromTcp4File = readerForTcp4File.readLine()) != null) {
                sbFromTcp4File.append(lineFromTcp4File);
            }
            readerForTcp4File.close();
            
            String contentOfTcp4File = sbFromTcp4File.toString();
            if (contentOfTcp4File != null && contentOfTcp4File .length() > 0){
                Matcher m4 = Pattern.compile(NetworkInfo.TCP_4_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(contentOfTcp4File);
                while (m4.find()) {
                    String srcAddressEntry = m4.group(1);
                    String srcPortEntry    = m4.group(2);
                    String dstAddressEntry = m4.group(3);
                    String dstPortEntry    = m4.group(4);
                    String connStatus      = m4.group(5);
                    
                    // parsing numbers
                    int uidEntry    = Integer.valueOf(m4.group(6));
                    int srcPort = Integer.valueOf(srcPortEntry, 16);
                    int dstPort = Integer.valueOf(dstPortEntry, 16);
                    int status = Integer.valueOf(connStatus, 16);
                    
                    // parsing adresses
                    InetAddress local = getAddress(4, srcAddressEntry);
                    InetAddress remote = getAddress(4, dstAddressEntry);
                    
                    String srcAddress = local.getHostAddress();
                    String dstAddress = remote.getHostAddress();
                    
                    if (LOGD) Log.d(TAG, "parsing v4 line for data: " + srcAddressEntry + " " +  srcPortEntry + " " +dstAddressEntry+ " " + dstPort + " " + uidEntry);
                    String[] packagesForUid = packageManager.getPackagesForUid(uidEntry);
                    String[] packageNames = null;
                    String[] versions = null;
                    String[] names = null;
                    String hostname = dstAddress;
                    if (packagesForUid != null && packagesForUid.length > 0) {
                        packageNames = new String[packagesForUid.length];
                        versions = new String[packagesForUid.length];
                        names = new String[packagesForUid.length];
                        for (int i = 0; i < packagesForUid.length; i ++) {
                            PackageInfo pInfo = packageManager.getPackageInfo(packagesForUid[i], 0);
                            packageNames[i] = packagesForUid[i];
                            versions[i] = pInfo.versionName;
                            names[i] = pInfo.applicationInfo.name;
                        }
                    }
                    if (resolveHostNames){
                        hostname = getHostName(dstAddress);
                    }
                    listPortInfo.add(new ConnectionDescriptor(packageNames, names, versions, TCP_TYPE, status, srcAddress, srcPort, dstAddress, dstPort, hostname, uidEntry));
                }
            }
            
            File tcp6File = new File(NetworkInfo.TCP_6_FILE_PATH);
            BufferedReader reader = new BufferedReader(new FileReader(tcp6File));
            String line = "";
            StringBuilder sbtcp6 = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                sbtcp6.append(line);
            }
            reader.close();
            
            String contentOfTcp6File = sbtcp6.toString();
            if (contentOfTcp6File != null && contentOfTcp6File .length() > 0){
                Matcher m6 = Pattern.compile(NetworkInfo.TCP_6_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(contentOfTcp6File);
                while (m6.find()) {
                    String srcAddressEntry = m6.group(1);
                    String srcPortEntry    = m6.group(2);
                    String dstAddressEntry = m6.group(3);
                    String dstPortEntry    = m6.group(4);
                    String status          = m6.group(5);
                    
                    // parsing numbers
                    int uidEntry    = Integer.valueOf(m6.group(6));
                    int srcPort = Integer.valueOf(srcPortEntry, 16);
                    int dstPort = Integer.valueOf(dstPortEntry, 16);
                    int connStatus = Integer.valueOf(status, 16);

                    // parsing addresses
                    InetAddress local = getAddress(16, srcAddressEntry);
                    InetAddress remote = getAddress(16, dstAddressEntry);
                    
                    String srcAddress = local.getHostAddress();
                    String dstAddress = remote.getHostAddress();
                    
                    if (LOGD) Log.d(TAG, "parsing v6 line for data: "  + srcAddressEntry + " " +  srcPortEntry + " " +dstAddressEntry+ " " + dstPort + " " + uidEntry);
                    String[] packagesForUid = packageManager.getPackagesForUid(uidEntry);
                    String[] packageNames = null;
                    String[] versions = null;
                    String[] names = null;
                    String hostname = dstAddress;
                    if (packagesForUid != null && packagesForUid.length > 0) {
                        packageNames = new String[packagesForUid.length];
                        versions = new String[packagesForUid.length];
                        names = new String[packagesForUid.length];
                        for (int i = 0; i < packagesForUid.length; i ++) {
                            PackageInfo pInfo = packageManager.getPackageInfo(packagesForUid[i], 0);
                            packageNames[i] = packagesForUid[i];
                            versions[i] = pInfo.versionName;
                            names[i] = pInfo.applicationInfo.name;
                        }
                    }
                    String dstipv4address = getIpv4FromIpv6(dstAddressEntry);
                    String dstipv6address = "::ffff:"+ dstipv4address;
                    String srcipv4address = getIpv4FromIpv6(srcAddressEntry);
                    String srcipv6address = "::ffff:"+ srcipv4address;
                    if (resolveHostNames){
                        hostname = getHostName(dstipv4address);
                    }
                    listPortInfo.add(new ConnectionDescriptor(packageNames, names, versions, TCP6_TYPE, connStatus, srcipv6address, srcPort, dstipv6address, dstPort, hostname, uidEntry));
                }
            }

        }catch(Exception ex){
            ex.printStackTrace();
        }
        return listPortInfo;
    }
    
}
