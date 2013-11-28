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

public class CheckOptionPluginList {
    
    private static boolean LOGD = false;
    private static String TAG = CheckOptionPluginList.class.getSimpleName();
    
    private static String STORED_SETTINGS = "CheckOptionsPluginSettings";
    private static String ACTIVE_SETTINGS = "CheckOptionsPluginActiveSettings";
    
    public static Map<String, CheckOptionPlugin>  CreateStoredObjectListFromPreferences(Context context){
        return CreateObjectListFromPreferences(context, STORED_SETTINGS);
    }

    public static Map<String, CheckOptionPlugin>  CreateActiveObjectListFromPreferences(Context context){
        return CreateObjectListFromPreferences(context, ACTIVE_SETTINGS);
    }
    
    private static Map<String, CheckOptionPlugin>  CreateObjectListFromPreferences(Context context, String name){
        
        SharedPreferences mShPref = PreferenceManager.getDefaultSharedPreferences(context);
        String preferenceString = mShPref.getString(STORED_SETTINGS, null);
        Map<String, CheckOptionPlugin> dictOfPluginSettings = new HashMap<String, CheckOptionPlugin>();
        if (preferenceString != null){
            try {
                if (LOGD) Log.d(TAG, "Getting json from preferences " + preferenceString);
                JSONArray listOfAppSettings = new JSONArray(preferenceString);
                for (int i = 0; i < listOfAppSettings.length(); i++) {
                    JSONObject obj = listOfAppSettings.getJSONObject(i);
                    CheckOptionPlugin plugin = new CheckOptionPlugin();
                    plugin.EN = obj.getBoolean("EN");
                    plugin.FN = obj.getString("FN");
                    plugin.CN = obj.getString("CN");
                    
                    dictOfPluginSettings.put(plugin.FN, plugin);
                }
            } catch (JSONException e) {
                if (LOGD) Log.e(TAG, e.getMessage());
            }
        }
        return dictOfPluginSettings;
    }
    
    private static CheckOptionPlugin createDefaultEmptyPlugin(){
        CheckOptionPlugin app = new CheckOptionPlugin();
        app.FN = "";
        app.CN = "";
        app.EN = false;
        return app;
        
    }
    
    public static void addPlugin(Context context, String fileName, String className, boolean checkValue){
        Map<String, CheckOptionPlugin> pluginSettings = CreateStoredObjectListFromPreferences(context);
        boolean changed = false;
        if (!pluginSettings.containsKey(fileName)){
            CheckOptionPlugin app = createDefaultEmptyPlugin();
            app.FN = fileName;
            app.CN = className;
            app.EN = checkValue;
            pluginSettings.put(className, app);
            changed = true;
        }else if (pluginSettings.containsKey(fileName)){
            CheckOptionPlugin app = pluginSettings.get(fileName);
            app.EN = checkValue;
            app.CN = className;
            changed = true;
        }
        if (changed){
            StoreObjectListToPreferences(context, pluginSettings);
        }
    }
    
    public static void removePlugin(Context context, String fileName){
        Map<String, CheckOptionPlugin> pluginSettings = CreateStoredObjectListFromPreferences(context);
        boolean changed = false;
        if (pluginSettings.containsKey(fileName)){
            pluginSettings.remove(fileName);
            changed = true;
        }
        if (changed){
            StoreObjectListToPreferences(context, pluginSettings);
        }
    }
    
    public static synchronized void StoreObjectListToPreferences(Context context, Map<String, CheckOptionPlugin> objectList){
        ObjectListToPreferences(STORED_SETTINGS, context, objectList);
    }
    
    public static synchronized void StoreActiveObjectListToPreferences(Context context){
        Map<String, CheckOptionPlugin> pluginSettings = CreateStoredObjectListFromPreferences(context);
        ObjectListToPreferences(ACTIVE_SETTINGS, context, pluginSettings);
    }
    
    
    private static synchronized void ObjectListToPreferences(String name, Context context, Map<String, CheckOptionPlugin> objectList){
        
        SharedPreferences mShPref = PreferenceManager.getDefaultSharedPreferences(context);
        JSONArray jsonArray = new JSONArray();
        for (CheckOptionPlugin pluginSettings : objectList.values()) {
            JSONObject obj = new JSONObject();
            try{
                obj.put("FN", pluginSettings.FN);
                obj.put("CN", pluginSettings.CN);
                obj.put("EN", pluginSettings.EN);
                jsonArray.put(obj);
            }catch(Exception e){
                if (LOGD) Log.e(TAG, e.getMessage());
            }
            
        }
        if (LOGD) Log.d(TAG, "Storing json to preferences " + jsonArray.toString());
        mShPref.edit().putString(name, jsonArray.toString()).commit();
    }
    
}
