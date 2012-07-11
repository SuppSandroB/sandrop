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

package org.sandroproxy.utils.dex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.sandrop.webscarab.plugin.proxy.ProxyPlugin;

import dalvik.system.DexClassLoader;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class DexUtils {
    
    private Context context;
    private static String TAG = DexUtils.class.getName();
    private static boolean LOGD = true;
    
    private ProgressDialog mProgressDialog = null;
    
    // Buffer size for file copying.  While 8kb is used in this sample, you
    // may want to tweak it based on actual size of the secondary dex file involved.
    private static final int BUF_SIZE = 8 * 1024;
    
    public DexUtils(Context context){
        this.context = context;
    }
    
    public void prepareDexClasses(String fileName, String title, String message){
        final File dexInternalStoragePath = new File(context.getDir("dex", Context.MODE_PRIVATE),
                fileName);
        if (!dexInternalStoragePath.exists()) {
            mProgressDialog = ProgressDialog.show(context,
                    title, 
                    message, true, false);
            // Perform the file copying in an AsyncTask.
            (new PrepareDexTask()).execute(dexInternalStoragePath, fileName);
        }
    }
    
    
    public void removeDexFile(String fileName){
        final File dexInternalStoragePath = new File(context.getDir("dex", Context.MODE_PRIVATE),
                fileName);
        if (dexInternalStoragePath != null){
            if (dexInternalStoragePath.exists()) {
                dexInternalStoragePath.delete();
            }else{
                Log.e(TAG, "File not exist" + dexInternalStoragePath.getAbsolutePath());
            }
        }
    }
    
    public File[] getDexFiles(){
        File file = context.getDir("dex", Context.MODE_PRIVATE);
        File[] files = file.listFiles();
        return files;
    }
    
    public boolean dexFileExist(String fileName){
        File file = new File(context.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath() + "/" + fileName);
        return file.exists();
    }
    
    public File getTempDexFile(){
        File file = new File(context.getDir("plugin_temp", Context.MODE_PRIVATE).getAbsolutePath() + "/temp.jar" );
        return file;
    }
    
    public boolean deleteTempDexFile(){
        File file = new File(context.getDir("plugin_temp", Context.MODE_PRIVATE).getAbsolutePath() + "/temp.jar" );
        if (file.exists()){
            file.delete();
            return true;
        }
        return false;
    }
    
    
    private void copyfile(File srFile, File dtFile){
        try{
          InputStream in = new FileInputStream(srFile);
          OutputStream out = new FileOutputStream(dtFile);

          byte[] buf = new byte[4096];
          int len;
          while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
          }
          in.close();
          out.close();
        }
        catch(Exception e){
            e.printStackTrace();
            Log.e(TAG, "Error copying file" + e.getMessage());
        }
      }

    public void addTempDexToCollection(String dexName, String className){
        final File dexInternalTempStoragePath = getTempDexFile();
        final File dexInternalStoragePath = new File(context.getDir("dex", Context.MODE_PRIVATE),
                dexName);
        copyfile(dexInternalTempStoragePath, dexInternalStoragePath);
        deleteTempDexFile();
    }
    
    public void deleteDexFromCollection(String dexName, String className){
        final File dexInternalStoragePath = new File(context.getDir("dex", Context.MODE_PRIVATE),
                dexName);
        dexInternalStoragePath.delete();
    }
    
    public String getTempDexFileReport(String className, Class superTypeInstanceToTest, 
                                        String methodToTest, Class methodReturnTypeToTest){
        String report = "";
        try{
            final File optimizedDexOutputPath = context.getDir("outdextemp", Context.MODE_PRIVATE);
            final File dexInternalStoragePath = getTempDexFile();
            DexClassLoader cl = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
                    optimizedDexOutputPath.getAbsolutePath(),
                    null,
                    context.getClassLoader());
            Class classz = cl.loadClass(className);
            
            Class superClass = classz.getSuperclass();
            if (superTypeInstanceToTest != null){
                if (superClass != null && superClass.getCanonicalName().equals(superTypeInstanceToTest.getCanonicalName())){ 
                    report += "\n";
                    report += "SuperClass:" +superClass.getCanonicalName()+ "\n";
                }else{
                    report += "Error: missing superclass of " + superTypeInstanceToTest.getCanonicalName();
                }
            }
            report += "\n";
            report += "DeclaredClasses:";
            Class[] declaredClasses = classz.getDeclaredClasses();
            for (Class declaredClass : declaredClasses) {
                report += declaredClass.getCanonicalName() + " ";
            }
            report += "\n";
            Method[] methods = classz.getDeclaredMethods();
            report += "Functions: ";
            if (methodToTest != null){
                boolean foundMethod = false;
                for (Method method : methods) {
                    report += method.getName() + " ";
                    if (method.getName().equals(methodToTest)){
                        foundMethod = true;
                        if (methodReturnTypeToTest != null){
                            if (!method.getReturnType().getCanonicalName().equals(methodReturnTypeToTest.getCanonicalName())){
                                report += "\nError: method do not return proper type of " + methodReturnTypeToTest.getCanonicalName();
                                report += "\nError: method returns  " + method.getReturnType().getCanonicalName() + "\n";
                            }
                        }
                    }
                }
                if (!foundMethod){
                    report += "\nError: method not found  " + methodToTest + "\n";
                }
            }
            
            report += "\n";
            report += "Fields: ";
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                report += field.getName() + " ";
            }
            report += "\n";
            
        }catch (Exception ex){
            ex.printStackTrace();
            report = "Error:" + ex.getMessage();
        }
        return report;
    }
    
    public ProxyPlugin loadCustomPluginFromDex(String fileName, String classFullName){
        ProxyPlugin customPlugin = null;
        try{
            final File optimizedDexOutputPath = context.getDir("outdex", Context.MODE_PRIVATE);
            final File dexInternalStoragePath = new File(context.getDir("dex", Context.MODE_PRIVATE),
                    fileName);
            if (dexInternalStoragePath.exists()){
                DexClassLoader cl = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
                        optimizedDexOutputPath.getAbsolutePath(),
                        null,
                        context.getClassLoader());
                Class customPluginClazz = null;
                customPluginClazz = cl.loadClass(classFullName);
                customPlugin = (ProxyPlugin) customPluginClazz.newInstance();
            }else{
                // TODO add logging that file is missing to logtab and Toast
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return customPlugin;
    }
    
    private boolean prepareDex(File dexInternalStoragePath, String fileName) {
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;

        try {
            bis = new BufferedInputStream(context.getAssets().open(fileName));
            dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();
            return true;
        } catch (IOException e) {
            if (dexWriter != null) {
                try {
                    dexWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            return false;
        }
    }
    
    private class PrepareDexTask extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mProgressDialog != null) mProgressDialog.cancel();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (mProgressDialog != null) mProgressDialog.cancel();
        }

        @Override
        protected Boolean doInBackground(Object... dexParams) {
            prepareDex((File)dexParams[0], (String)dexParams[1]);
            return null;
        }
    }
}
