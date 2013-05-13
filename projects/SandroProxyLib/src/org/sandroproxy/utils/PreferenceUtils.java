/***********************************************************************
 *
 * This file is part of SandroProxy, 
 * For details, please see http://code.google.com/p/sandrop/
 *
 * Copyright (c) 2012 supp.sandrob@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Getting Source
 * ==============
 *
 * Source for this application is maintained at
 * http://code.google.com/p/sandrop/
 *
 * Software is build from sources of WebScarab project
 * For details, please see http://www.sourceforge.net/projects/owasp
 *
 */

package org.sandroproxy.utils;

import java.io.File;

import org.sandroproxy.constants.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {
    

    public static String dataStorageKey = "preference_proxy_data_storage";
    public static String dataLargeSize = "preference_performance_data_large_size";
    public static String proxyTransparentKey = "preference_proxy_transparent";
    public static String proxyTransparentActiveKey = "preference_proxy_transparent_active";
    public static String proxyTransparentHostNameKey = "preference_proxy_transparent_hostname";
    public static String proxyCustomPluginKey = "preference_proxy_custom_plugins";
    public static String proxyCustomPluginKeyActiveSetting = "preference_proxy_custom_plugins_active_setting";
    public static String proxyPort = "preference_proxy_port";
    public static String proxyListenNonLocal = "preference_proxy_listen_non_local";
    public static String proxyCaptureData = "preference_proxy_capture_data";
    public static String caFileNamePath = "preference_ca_cert_file_path";
    public static String caFilePassword = "preference_ca_cert_password";
    public static String dataCaptureWhiteListRegEx = "preference_data_capture_whitelist";
    public static String dataCaptureBlackListRegEx = "preference_data_capture_blacklist";
    
    public static String chainProxyEnabled = "preference_chain_proxy_enabled";
    public static String chainProxyHttp = "preference_chain_proxy_http";
    public static String chainProxyHttps = "preference_chain_proxy_https";
    public static String chainProxyExcludeList = "preference_chain_proxy_no_proxy_list";
    public static String chainProxyUsername = "preference_chain_proxy_username";
    public static String chainProxyPassword = "preference_chain_proxy_password";
    
    public static String ssTrustAllManager = "preference_ssl_trust_all_manager";
    
    
    public static String getProxyPort(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(proxyPort, "8008");
    }
    
    public static File getDataStorageDir(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String dirName = pref.getString(dataStorageKey, null);
        if (dirName != null && !dirName.equals("")){
            File dataDir = new File(dirName);
            if (IsDirWritable(dataDir)){
                return dataDir;
            }
        }else{
            File dataDir = context.getExternalCacheDir();
            if (IsDirWritable(dataDir)){
                pref.edit().putString(dataStorageKey, dataDir.getAbsolutePath()).commit();
                return dataDir;
            }
        }
        return null;
    }
    
    
    public static String getCAFilePath(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String fileName = pref.getString(caFileNamePath, null);
        if (fileName != null && !fileName.equals("")){
            File fileVal = new File(fileName);
            if (fileVal.canRead()){
                return fileVal.getAbsolutePath();
            }
        }else{
            File dataDir = getDataStorageDir(context);
            if (IsDirWritable(dataDir)){
                return dataDir.getAbsolutePath() + Constants.CA_FILE_NAME;
            }
        }
        return null;
    }
    
    public static String getCAExportFilePath(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String fileName = pref.getString(caFileNamePath, null);
        if (fileName != null && !fileName.equals("")){
            File fileVal = new File(fileName);
            if (fileVal.canRead()){
                return fileVal.getAbsolutePath() + Constants.CA_FILE_EXPORT_POSTFIX;
            }
        }else{
            File dataDir = getDataStorageDir(context);
            if (IsDirWritable(dataDir)){
                return dataDir.getAbsolutePath() + Constants.CA_FILE_NAME + Constants.CA_FILE_EXPORT_POSTFIX;
            }
        }
        return null;
    }
    
    
    
    public static String getCertFilePath(Context context){
        File dataDir = getDataStorageDir(context);
        if (IsDirWritable(dataDir)){
            return dataDir.getAbsolutePath() + Constants.CERTS_FILE_NAME;
        }
        return null;
    }
    
    public static String getCAFilePassword(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String caFilePasswordVal = pref.getString(caFilePassword, null);
        if (caFilePasswordVal != null && caFilePasswordVal.length() > 0 ){
            return caFilePasswordVal;
        }
        return Constants.CERT_DEFAULT_PASSWORD;
    }
    
    
    public static boolean IsDirWritable(File dir){
        if (dir != null && dir.exists() && dir.isDirectory() && dir.canWrite()){
            return true;
        }
        return false;
    }
    
    public static boolean isTransparentProxySelected(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(proxyTransparentKey, false);
    }
    
    public static boolean isTransparentProxyActive(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(proxyTransparentActiveKey, false);
    }
    
    public static void setTransparentProxyActive(Context context, boolean value){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(proxyTransparentActiveKey, value).commit();
    }
    
    public static boolean isCustomProxyPluginsStoreEnabled(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(proxyCustomPluginKey, false);
    }
    
    public static void setCustomProxyPluginsActive(Context context, boolean value){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(proxyCustomPluginKeyActiveSetting, value).commit();
    }

}
