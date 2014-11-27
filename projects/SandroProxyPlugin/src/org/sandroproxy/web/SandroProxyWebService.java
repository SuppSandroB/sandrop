package org.sandroproxy.web;

import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.sandroproxy.plugin.R;
import org.sandroproxy.utils.PreferenceUtils;
import org.sandroproxy.utils.preferences.PreferenceConstants;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class SandroProxyWebService extends Service{

    public static int SANDROPROXY_WEB_NOTIFICATION_ID = 200;
    public static boolean webServerStarted = false;
    private static Logger _logger = Logger.getLogger(SandroProxyWebService.class.getName());
    private AndroidHTTPD server;
    
    WebSocketServerCustom mSocketServer;
    
    
    
    public SandroProxyWebService(){
        super();
        _logger.setLevel(Level.FINEST);
    }
    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                try {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String webPortString = pref.getString(PreferenceConstants.PREF_WEB_GUI_PORT, "8020");
                    String webSocketPortString = pref.getString(PreferenceConstants.PREF_WEB_SOCKET_PORT, "8021");
                    String webHostString = pref.getString(PreferenceConstants.PREF_WEB_GUI_HOST_ROOT, "http://sandrop.googlecode.com/git-history/chrome_devtools_1_0/projects/SandroProxyWeb/chrome_devtools/");
                    boolean useLocalWebGuiResources = pref.getBoolean(PreferenceConstants.PREF_WEB_LOCAL_RESOURCES,true);
                    int webPort = 8020;
                    int webSocketPort = 8021;
                    try{
                        webPort = Integer.parseInt(webPortString);
                        webSocketPort = Integer.parseInt(webSocketPortString);
                    }catch(Exception ex){
                        return;
                    }
                    server = new AndroidHTTPD(getApplicationContext(), webPort, webSocketPort, 
                                PreferenceUtils.getDataStorageDir(getApplicationContext()), webHostString, useLocalWebGuiResources, null);
                    server.startServer();
                    webServerStarted = true;
                    
                    setWebSocketServer(webSocketPort);
                    
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    int icon = R.drawable.ic_menu_view;
                    long when = System.currentTimeMillis();
                    Notification notification = new Notification(icon, "SandroProxy Web activated", when);
                    notification.flags|=Notification.FLAG_NO_CLEAR;
                    Context context = getApplicationContext();
                    
                    boolean haveChromeInstalled = false;
                    Intent chromeIntent = new Intent("android.intent.action.MAIN");
                    Intent notificationIntent = null;
                    
                    String packageNameChrome = "com.chrome.beta";
                    String activityNameChrome = "com.android.chrome.Main";
                    
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {

                        chromeIntent.setComponent(ComponentName.unflattenFromString(packageNameChrome +"/" + activityNameChrome));
                        chromeIntent.addCategory("android.intent.category.LAUNCHER");
                        List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(chromeIntent, 0);
    
                        for(ResolveInfo info : resolveInfoList){
                            if(info.activityInfo.packageName.equalsIgnoreCase("com.chrome.beta")){
                                haveChromeInstalled = true;
                            }
                        }
                    }

                    String NotificationMessage = "";
                    if (haveChromeInstalled){
                        chromeIntent.setAction(Intent.ACTION_MAIN);
                        //chromeIntent.setData(Uri.parse("http://localhost:" + webPort+ "/devtools/devtools.html?host=localhost:"+webSocketPort+ "&page=1"));
                        chromeIntent.setData(Uri.parse("http://localhost:" + webPort+ "/"));
                        NotificationMessage = "Activate Chrome browser (" + webPort + "/" + webSocketPort + ")";
                        notificationIntent = chromeIntent;
                    }else{
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                            NotificationMessage = "Install Chrome to use web interface";
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageNameChrome));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            notificationIntent = intent;
                        }else{
                            NotificationMessage = "Access port " + + webPort + " from Chrome";
                        }
                    }
                    
                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                    notification.setLatestEventInfo(context, "SandroProxy Web", NotificationMessage, contentIntent);
                    
                    startForeground(SANDROPROXY_WEB_NOTIFICATION_ID, notification);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        };
        thread.start();
        return START_STICKY;
    }
    
    private void setWebSocketServer(int port){
        try {
            WebSocket.DEBUG = false;
            mSocketServer = new WebSocketServerCustom(getApplicationContext(), port);
            mSocketServer.setWebSocketFactory(new WebSocketServerFactoryCustom());
            mSocketServer.start();
            
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
    }
    
    @Override
    public void onDestroy() {
        stopForeground(true);
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                if (server != null){
                    server.stop();
                    server = null;
                    webServerStarted = false;
                }
                if (mSocketServer != null){
                    try {
                        mSocketServer.stop();
                        mSocketServer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        super.onDestroy();
    }
    
    private void doStop() {
        // TODO sandrop
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
