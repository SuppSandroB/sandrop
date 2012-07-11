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

package org.sandrop.webscarab.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
    
    private static Context mContext;
    
    /** Creates a new instance of Preferences */
    private Preferences() {
    }
    
    public static void init(Context context){
        mContext = context;
    }

    public static void setPreference(String key, String value) {
       SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
       Editor prefEditor = pref.edit();
       prefEditor.putString(key, value);
       prefEditor.commit();
    }
    
    public static String getPreference(String key) {
        return getPreference(key, null);
    }
    
    public static String getPreference(String key, String defaultValue) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String value = pref.getString(key, defaultValue);
        return value;
    }
    
    public static boolean getPreferenceBoolean(String key, boolean defaultValue) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean value = pref.getBoolean(key, defaultValue);
        return value;
    }
    
    

    public static void remove(String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor prefEditor = pref.edit();
        prefEditor.remove(key);
        prefEditor.commit();
        
    }
}
