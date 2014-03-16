package org.sandroproxy.utils.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.IClientResolver;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ClientResolver implements IClientResolver{

    
    private static boolean LOGD = false;
    private static String TAG = ClientResolver.class.getSimpleName();
    
    private Context mContext;
    private PackageManager packageManager;

    public ClientResolver(Context context){
        this.mContext = context;
        packageManager = context.getPackageManager();
    }
    
    @Override
    public ConnectionDescriptor getClientDescriptorBySocket(Socket socket) {

        int port = socket.getPort();
        String address = socket.getInetAddress().getHostAddress();
        BufferedReader reader = null;
        try {
            
            String ipv4Address = NetworkInfo.getIPAddress(true);
            String ipv6Address = NetworkInfo.getIPAddress(false);

            boolean hasIPv6 = (ipv6Address.length() > 0); //TODO use this value to skip ipv6 check, eventually
            
            File tcp = new File(NetworkInfo.TCP_6_FILE_PATH);
            reader = new BufferedReader(new FileReader(tcp));
            String line = "";
            StringBuilder builder = new StringBuilder();
            
            // find line that has port number inside 
            String hexPort = Integer.toHexString(port);
            while ((line = reader.readLine()) != null) {
                if (line.toUpperCase().contains(hexPort.toUpperCase())){
                    builder.append(line);
                }
            }
            reader.close();
            
            String content = builder.toString();
            if (content != null && content .length() > 0){
                Matcher m6 = Pattern.compile(NetworkInfo.TCP_6_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(content);
                
                while (m6.find()) {
                    String srcAddressEntry = m6.group(1);
                    String srcPortEntry    = m6.group(2);
                    String dstAddressEntry = m6.group(3);
                    String dstPortEntry    = m6.group(4);
                    String status          = m6.group(5);
                    int uidEntry    = Integer.valueOf(m6.group(6));
                    int srcPort = Integer.valueOf(srcPortEntry, 16);
                    int dstPort = Integer.valueOf(dstPortEntry, 16);
                    int connStatus = Integer.valueOf(status, 16);
                    
                    if (srcPort == port) {
                        
                        if (LOGD) Log.d(TAG, "parsing v6 line for data: " + srcAddressEntry + " " +  srcPort + " " + uidEntry);
                        
                        String[] packagesForUid = packageManager.getPackagesForUid(uidEntry);
                        if (packagesForUid != null) {
                            String packageName = packagesForUid[0];
                            PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
                            String version = pInfo.versionName;
                            String name = pInfo.applicationInfo.name;
                            return new ConnectionDescriptor(new String[]{packageName}, new String[]{name}, new String[]{version}, NetworkInfo.TCP6_TYPE, connStatus, srcAddressEntry, srcPort, dstAddressEntry, dstPort, null, uidEntry); 
                        }
                    }
                }
            }
            
            // this means that no connection with that port could be found in the tcp6 file
            // try the tcp one
            
            tcp = new File(NetworkInfo.TCP_4_FILE_PATH);
            reader = new BufferedReader(new FileReader(tcp));
            line = "";
            builder = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                if (line.toUpperCase().contains(hexPort.toUpperCase())){
                    builder.append(line);
                }
            }
            
            reader.close();
            
            content = builder.toString();
            
            if (content != null && content .length() > 0){
                Matcher m4 = Pattern.compile(NetworkInfo.TCP_4_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(content);
                
                while (m4.find()) {
                    String srcAddressEntry = m4.group(1);
                    String srcPortEntry    = m4.group(2);
                    String dstAddressEntry = m4.group(3);
                    String dstPortEntry    = m4.group(4);
                    String connStatus      = m4.group(5);
                    int uidEntry    = Integer.valueOf(m4.group(6));
                    int srcPort = Integer.valueOf(srcPortEntry, 16);
                    int dstPort = Integer.valueOf(dstPortEntry, 16);
                    int status  = Integer.valueOf(connStatus, 16);
                    
                    if (LOGD) Log.d(TAG, "parsing v4 line for data: " + srcAddressEntry + " " +  srcPortEntry + " " + uidEntry);
                    
                    if (srcPort == port) {
                        String[] packagesForUid = packageManager.getPackagesForUid(uidEntry);
                        
                        if (packagesForUid != null) {
                            String packageName = packagesForUid[0];
                            PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
                            String version = pInfo.versionName;
                            String name = pInfo.applicationInfo.name;
                            return new ConnectionDescriptor(new String[]{packageName}, new String[]{name}, new String[]{version}, NetworkInfo.TCP_TYPE, status, srcAddressEntry, srcPort, dstAddressEntry, dstPort, null, uidEntry);
                        }
                    }
                }
            }
            
            // nothing found we create descriptor with what we got as input
            if (LOGD) Log.d(TAG, "No data for " + address + ":" + port);
            return new ConnectionDescriptor(new String[]{""}, new String[]{""}, new String[]{""}, null, -1, address, port, null, -1, null, -1); 
            
        } catch (Exception e) {
            Log.e(TAG, "parsing client data error : " + e.getMessage());
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        if (LOGD) Log.d(TAG, "No data for " + address + ":" + port);
        return new ConnectionDescriptor(new String[]{""}, new String[]{""}, new String[]{""}, null, -1, address, port, null, -1, null, -1);
    }
    
}
