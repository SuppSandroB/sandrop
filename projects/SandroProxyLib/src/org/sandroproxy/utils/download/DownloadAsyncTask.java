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

package org.sandroproxy.utils.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;


import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public class DownloadAsyncTask extends AsyncTask<Object, String, Boolean>{

    private static String TAG =  DownloadAsyncTask.class.getName();
    private static boolean LOGD = true;
    private ProgressDialog dialog;
    private IDownloadHandler handler;
    private String fileReport = "";  
    
    public DownloadAsyncTask(ProgressDialog progressDialog, IDownloadHandler downloadHandler){
        dialog = progressDialog;
        handler = downloadHandler;
    }
    
    @Override
    protected Boolean doInBackground(Object... param) {
        URL url = (URL)param[0];
        File fileName = (File)param[1];
        try {
            
            return downloadFileFromHttp(url, fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      dialog.setCancelable(false);
    }
    
    @Override
    protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      dialog.dismiss();
      handler.downloadActionComplete(result, fileReport);
    }
    
    @Override
    protected void onProgressUpdate(String... message) {
      super.onProgressUpdate(message);
      int percentComplete = 0;

      percentComplete = Integer.parseInt(message[1]);
      dialog.setMessage(message[0]);
      dialog.setProgress(percentComplete);
      dialog.show();
    }

    private boolean downloadFileFromHttp(URL url, File destinationFile)
            throws Exception {
          if (LOGD) Log.d(TAG, "Sending GET request to " + url + "...");
          publishProgress("Downloading file" +  url.toString(), "0");
          HttpURLConnection urlConnection = null;
          urlConnection = (HttpURLConnection) url.openConnection();
          urlConnection.setAllowUserInteraction(false);
          urlConnection.setInstanceFollowRedirects(true);
          urlConnection.setRequestMethod("GET");
          urlConnection.connect();
          if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.e(TAG, "Did not get HTTP_OK response.");
            Log.e(TAG, "Response code: " + urlConnection.getResponseCode());
            Log.e(TAG, "Response message: " + urlConnection.getResponseMessage().toString());
            return false;
          }
          int fileSize = urlConnection.getContentLength();
          InputStream inputStream = urlConnection.getInputStream();
          File tempFile = new File(destinationFile.toString());

          // Stream the file contents to a local file temporarily
          if (LOGD) Log.d(TAG, "Streaming download to " + destinationFile.toString());
          final int BUFFER = 8192;
          FileOutputStream fileOutputStream = null;
          Integer percentComplete;
          int percentCompleteLast = 0;
          try {
            fileOutputStream = new FileOutputStream(tempFile);
          } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception received when opening FileOutputStream.", e);
          }
          int downloaded = 0;
          byte[] buffer = new byte[BUFFER];
          int bufferLength = 0;
          while ((bufferLength = inputStream.read(buffer, 0, BUFFER)) > 0) {
            fileOutputStream.write(buffer, 0, bufferLength);
            downloaded += bufferLength;
            percentComplete = (int) ((downloaded / (float) fileSize) * 100);
            if (percentComplete > percentCompleteLast) {
              publishProgress(
                  "Downloading data for " + url.toString() + "...",
                  percentComplete.toString());
              percentCompleteLast = percentComplete;
            }
          }
          fileOutputStream.close();
          if (urlConnection != null) {
            urlConnection.disconnect();
          }
          fileReport += "digest sha1:" + getFileDigest(tempFile, "SHA1") + " \n";
          fileReport += "size:" + tempFile.length() + " \n";
          return true; 
        }
    
    private String getFileDigest(File datafile, String type) throws Exception{
        MessageDigest md = MessageDigest.getInstance(type);
        FileInputStream fis = new FileInputStream(datafile);
        byte[] dataBytes = new byte[1024];
     
        int nread = 0; 
     
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        };
     
        byte[] mdbytes = md.digest();
     
        //convert the byte to hex format
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    
}
