/***********************************************************************
 *
 * This file is part of SandroProxy, 
 * For details, please see http://code.google.com/p/sandrop/
 *
 * Copyright (c) 20012 supp.sandrob@gmail.com
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {
    

    public static String dataStorageKey = "preference_proxy_data_storage";
    public static String proxyTransparentKey = "preference_proxy_transparent";
    public static String proxyCustomPluginKey = "preference_proxy_custom_plugins";
    
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
                return dataDir;
            }
        }
        return null;
    }
    
    public static boolean IsDirWritable(File dir){
        if (dir.exists() && dir.isDirectory() && dir.canWrite()){
            return true;
        }
        return false;
    }
    
    public static boolean isTransparentProxyActivated(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(proxyTransparentKey, false);
    }
    
    public static boolean isCustomProxyPluginsActivated(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(proxyCustomPluginKey, false);
    }

    
}
