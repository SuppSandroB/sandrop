package org.sandroproxy.utils.preference;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class CheckOptionAppList {
    
    private static boolean LOGD = false;
    private static String TAG = CheckOptionAppList.class.getSimpleName();
    
    private static String STORED_SETTINGS = "CheckOptionsAppSettings";
    private static String ACTIVE_SETTINGS = "CheckActiveOptionsAppSettings";
    
    public static Map<Integer, CheckOptionApp>  CreateStoredObjectListFromPreferences(Context context){
        return CreateObjectListFromPreferences(context, STORED_SETTINGS);
    }
    
    public static Map<Integer, CheckOptionApp>  CreateActiveObjectListFromPreferences(Context context){
        return CreateObjectListFromPreferences(context, ACTIVE_SETTINGS);
    }
    
    
    private static Map<Integer, CheckOptionApp>  CreateObjectListFromPreferences(Context context, String name){
        
        SharedPreferences mShPref = PreferenceManager.getDefaultSharedPreferences(context);
        String preferenceString = mShPref.getString(name, null);
        Map<Integer, CheckOptionApp> dictOfAppSettings = new HashMap<Integer, CheckOptionApp>();
        if (preferenceString != null){
            try {
                if (LOGD) Log.d(TAG, "Getting json from preferences " + preferenceString);
                JSONArray listOfAppSettings = new JSONArray(preferenceString);
                for (int i = 0; i < listOfAppSettings.length(); i++) {
                    JSONObject obj = listOfAppSettings.getJSONObject(i);
                    CheckOptionApp app = new CheckOptionApp();
                    app.AUid = obj.getInt("AUid");
                    app.AN = obj.getString("AN");
                    app.WH = obj.getBoolean("WH");
                    app.WHS = obj.getBoolean("WHS");
                    app.CA = obj.getString("CA");
                    if (obj.has("CE")){
                        app.CE = obj.getBoolean("CE");
                    }else{
                        app.CE = false;
                    }

                    app.CustomPortRules = new HashMap<Integer, CheckOptionCustomPorts>();
                    if (obj.has("CustomPortRules")){
                        JSONArray customArray = obj.getJSONArray("CustomPortRules");
                        for (int j = 0; j < customArray.length(); j++) {
                            JSONObject customItemObj = customArray.getJSONObject(j);
                            CheckOptionCustomPorts customItem = new CheckOptionCustomPorts();
                            customItem.CP = customItemObj.getInt("CP");
                            customItem.SF = customItemObj.getBoolean("SF");
                            customItem.TSSL = customItemObj.getBoolean("TSSL");
                            app.CustomPortRules.put(customItem.CP, customItem);
                        }
                    }
                    dictOfAppSettings.put(app.AUid, app);
                }
            } catch (JSONException e) {
                if (LOGD) Log.e(TAG, e.getMessage());
            }
        }
        return dictOfAppSettings;
    }
    
    public static CheckOptionApp createDefaultEmptyApp(){
        CheckOptionApp app = new CheckOptionApp();
        app.AUid = -1;
        app.AN = "";
        app.WH = false;
        app.WHS = false;
        app.CA = "";
        app.CE = false;
        app.CustomPortRules = new HashMap<Integer, CheckOptionCustomPorts>();
        return app;
        
    }
    
    
    public static void setAppClearExtra(Context context, int applicationUid){
        Map<Integer, CheckOptionApp> appsSettings = CreateStoredObjectListFromPreferences(context);
        boolean changed = false;
        if (!appsSettings.containsKey(applicationUid)){
        }else if (appsSettings.containsKey(applicationUid)){
            CheckOptionApp app = appsSettings.get(applicationUid);
            app.CE = false;
            app.CustomPortRules = new HashMap<Integer, CheckOptionCustomPorts>();
            changed = true;
        }
        if (changed){
            StoreObjectListToPreferences(context, appsSettings);
        }
    }
    
    public static void setAppCheckedExtra(Context context, int applicationUid, String applicationName, boolean checkValue){
        Map<Integer, CheckOptionApp> appsSettings = CreateStoredObjectListFromPreferences(context);
        boolean changed = false;
        if (!appsSettings.containsKey(applicationUid) && checkValue){
            CheckOptionApp app = createDefaultEmptyApp();
            app.AUid = applicationUid;
            app.AN = applicationName;
            app.CE = checkValue;
            app.CustomPortRules = new HashMap<Integer, CheckOptionCustomPorts>();
            appsSettings.put(applicationUid, app);
            changed = true;
        }else if (appsSettings.containsKey(applicationUid)){
            CheckOptionApp app = appsSettings.get(applicationUid);
            app.CE = checkValue;
            changed = true;
        }
        if (changed){
            StoreObjectListToPreferences(context, appsSettings);
        }
    }
    
    public static void setAppCheckedHttp(Context context, int applicationUid, String applicationName, boolean checkValue){
        Map<Integer, CheckOptionApp> appsSettings = CreateStoredObjectListFromPreferences(context);
        boolean changed = false;
        if (!appsSettings.containsKey(applicationUid) && checkValue){
            CheckOptionApp app = createDefaultEmptyApp();
            app.AUid = applicationUid;
            app.AN = applicationName;
            app.WH = checkValue;
            app.CustomPortRules = new HashMap<Integer, CheckOptionCustomPorts>();
            appsSettings.put(applicationUid, app);
            changed = true;
        }else if (appsSettings.containsKey(applicationUid)){
            CheckOptionApp app = appsSettings.get(applicationUid);
            app.WH = checkValue;
            changed = true;
        }
        if (changed){
            StoreObjectListToPreferences(context, appsSettings);
        }
    }
    
    public static void setAppCheckedHttps(Context context, int applicationUid, String applicationName, boolean checkValue){
        Map<Integer, CheckOptionApp> appsSettings = CreateStoredObjectListFromPreferences(context);
        boolean changed = false;
        if (!appsSettings.containsKey(applicationUid) && checkValue){
            CheckOptionApp app = createDefaultEmptyApp();
            app.AUid = applicationUid;
            app.AN = applicationName;
            app.WHS = checkValue;
            appsSettings.put(applicationUid, app);
            app.CustomPortRules = new HashMap<Integer, CheckOptionCustomPorts>();
            changed = true;
        }else if (appsSettings.containsKey(applicationUid)){
            CheckOptionApp app = appsSettings.get(applicationUid);
            app.WHS = checkValue;
            changed = true;
        }
        if (changed){
            StoreObjectListToPreferences(context, appsSettings);
        }
    }
    
    public static synchronized void StoreObjectListToPreferences(Context context, Map<Integer, CheckOptionApp> objectList){
        ObjectListToPreferences(context, objectList, STORED_SETTINGS);
    }
    
    public static synchronized void StoreActiveObjectListToPreferences(Context context){
        Map<Integer, CheckOptionApp>  objectList = CreateStoredObjectListFromPreferences(context);
        ObjectListToPreferences(context, objectList, ACTIVE_SETTINGS);
    }
    
    private static synchronized void ObjectListToPreferences(Context context, Map<Integer, CheckOptionApp> objectList, String name){
        
        SharedPreferences mShPref = PreferenceManager.getDefaultSharedPreferences(context);
        JSONArray jsonArray = new JSONArray();
        for (CheckOptionApp appSettings : objectList.values()) {
            JSONObject obj = new JSONObject();
            try{
                obj.put("AUid", appSettings.AUid);
                obj.put("AN", appSettings.AN);
                obj.put("WH", appSettings.WH);
                obj.put("WHS", appSettings.WHS);
                obj.put("CA", appSettings.CA);
                obj.put("CE", appSettings.CE);
                Map<Integer, CheckOptionCustomPorts> customPorts = appSettings.CustomPortRules;
                if (customPorts != null && customPorts.size() > 0){
                    JSONArray customArray = new JSONArray();
                    for (CheckOptionCustomPorts customPort : customPorts.values()) {
                        JSONObject customObjItem = new JSONObject();
                        customObjItem.put("CP", customPort.CP);
                        customObjItem.put("SF", customPort.SF);
                        customObjItem.put("TSSL", customPort.TSSL);
                        customArray.put(customObjItem);
                    }
                    obj.put("CustomPortRules", customArray);
                }
                jsonArray.put(obj);
            }catch(Exception e){
                if (LOGD) Log.e(TAG, e.getMessage());
            }
            
        }
        if (LOGD) Log.d(TAG, "Storing json to preferences " + jsonArray.toString());
        mShPref.edit().putString(name, jsonArray.toString()).commit();
    }
    
}
