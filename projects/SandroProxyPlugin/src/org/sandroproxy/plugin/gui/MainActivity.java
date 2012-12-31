package org.sandroproxy.plugin.gui;

import java.io.File;

import org.sandrop.webscarab.model.Preferences;
import org.sandrop.webscarab.model.StoreException;
import org.sandrop.webscarab.plugin.Framework;
import org.sandrop.webscarab.plugin.proxy.Proxy;
import org.sandrop.webscarab.plugin.proxy.ProxyPlugin;
import org.sandroproxy.logger.Logger;
import org.sandroproxy.plugin.R;
import org.sandroproxy.proxy.plugin.CustomPlugin;
import org.sandroproxy.utils.PreferenceUtils;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
    
    private static Framework framework = null;
    // private static String TAG = MainActivity.class.getName();
    // private static boolean LOGD = false;
    
    
    public static boolean proxyStarted = false;
    
    private static Handler mHandlerLog = null;
    private static TextView mLogView;
    
    private static Logger mLogger;
    private static int MAX_LOG_SIZE = 20000;
    private static int MAX_MSG_SIZE = 3000;
    private static String mLogWindowMessage = "";
    
    private static boolean mInitChecked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogView = (TextView) findViewById(R.id.logView); 
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonOnOff);
        toggleButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                boolean value = ((ToggleButton)v).isChecked();
                if (value && !proxyStarted){
                    // start
                    Thread thread = new Thread()
                    {
                        @Override
                        public void run() {
                            Preferences.init(getApplicationContext());
                            framework = new Framework(getApplicationContext());
                            setStore(getApplicationContext());
                            Proxy proxy = new Proxy(framework, null);
                            framework.addPlugin(proxy);
                            if (true){
                                ProxyPlugin plugin = new CustomPlugin();
                                proxy.addPlugin(plugin);
                            }
                            proxy.run();
                            proxyStarted = true;
                        }
                    };
                    thread.setName("Starting proxy");
                    thread.start();
                }else if (proxyStarted){
                    //stop
                    Thread thread = new Thread()
                    {
                        @Override
                        public void run() {
                            if (framework != null){
                                framework.stop();
                            }
                            framework = null;
                            proxyStarted = false;
                        }
                    };
                    thread.setName("Stoping proxy");
                    thread.start();
                }
            }
        });
        
        if (mHandlerLog == null){
            mHandlerLog =new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    String previousText = mLogWindowMessage;
                    String message = (String)msg.obj;
                    if (message.length() > MAX_MSG_SIZE){
                        message = message.substring(0,MAX_MSG_SIZE);
                    }
                    String newText = message + previousText;
                    int newSize = newText.length();
                    if (newSize > MAX_LOG_SIZE){
                        int size = MAX_LOG_SIZE - (MAX_LOG_SIZE / 4);
                        newText = newText.substring(0, size);
                    }
                    mLogWindowMessage = newText;
                    mLogView.setText(mLogWindowMessage);
                }
            };
        }
        if (mLogger == null){
            mLogger = new Logger(mHandlerLog);
        }
        
        // long click clears the message window
        mLogView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((TextView)v).setText("");
                mLogWindowMessage = "";
                return false;
            }
        });
        
        // set some values if needed
        if (!mInitChecked){
            initValues();
            mInitChecked = true;
        }
        
        mLogView.setText(mLogWindowMessage);
    }
    
    
    public static void setStore(Context context){
        if (framework != null){
            try {
                File file =  PreferenceUtils.getDataStorageDir(context);
                if (file != null){
                    File rootDir = new File(file.getAbsolutePath() + "/content");
                    if (!rootDir.exists()){
                        rootDir.mkdir();
                    }
                    framework.setSession("Database", SqlLiteStore.getInstance(context, rootDir.getAbsolutePath()), "");
                }
            } catch (StoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    /*
     * TODO this should be handled with preference settings activity
     */
    private void initValues(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        
        // checking for directory to write data...
        String dirName = pref.getString(PreferenceUtils.dataStorageKey, null);
        if (dirName == null){
            File dataDir = getExternalCacheDir();
            if (PreferenceUtils.IsDirWritable(dataDir)){
                pref.edit().putString(PreferenceUtils.dataStorageKey, dataDir.getAbsolutePath()).commit();
            }else{
                Toast.makeText(this, R.string.data_storage_missing, Toast.LENGTH_LONG).show();
            }
        }else{
            File dataStorage = new File(dirName);
            if (!PreferenceUtils.IsDirWritable(dataStorage)){
                Toast.makeText(this, R.string.data_storage_missing, Toast.LENGTH_LONG).show();
            }
        }
        
        // if not set we set to 9008
        String port = pref.getString(PreferenceUtils.proxyPort, null);
        if (port == null){
            pref.edit().putString(PreferenceUtils.proxyPort, "9008").commit();
        }
        
        // by default we listen on all adapters
        boolean listenNonLocal = pref.getBoolean(PreferenceUtils.proxyListenNonLocal, false);
        if (!listenNonLocal){
            pref.edit().putBoolean(PreferenceUtils.proxyListenNonLocal, true).commit();
        }
    }
}
