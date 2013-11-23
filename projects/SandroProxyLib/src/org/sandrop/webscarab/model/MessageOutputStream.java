package org.sandrop.webscarab.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class MessageOutputStream extends OutputStream implements java.io.Closeable{

    private ByteArrayOutputStream memoryStream;
    private FileOutputStream fileStream;
    private boolean useFileStream = false;
    private File file;
    
    private static boolean LOGD = true;
    private static String TAG = MessageOutputStream.class.getName();
    
    public static int LARGE_CONTENT_SIZE = 1024 * 1024;
    private static long SUM_MEMORY_CONTENT_ALL = 0;
    
    public static synchronized void addRemoveActiveContentSum(int dataSize, boolean remove){
        String action = "";
        if (!remove){
            SUM_MEMORY_CONTENT_ALL = SUM_MEMORY_CONTENT_ALL + dataSize;
            action = "add";
        }else{
            SUM_MEMORY_CONTENT_ALL = SUM_MEMORY_CONTENT_ALL - dataSize;
            action = "remove";
        }
        if (LOGD) Log.d(TAG, "Memory content. " + action + " " + dataSize + " size is :" + SUM_MEMORY_CONTENT_ALL);
    } 
    
    
    public MessageOutputStream(){
        memoryStream = new ByteArrayOutputStream();
    }
    
    public MessageOutputStream(String fileName) throws FileNotFoundException{
        useFileStream = true;
        file = new File(fileName);
    }
    
    public String getFileName(){
        String result = null;
        if (useFileStream){
            result = file.getAbsolutePath();
        }
        return result;
    }

    @Override
    public void write(int oneByte) throws IOException {
        write(new byte[]{(byte)oneByte}, 0 ,1);
    }
    
    @Override
    public void write(byte[] buffer, int offset, int len) throws IOException {
        if (!useFileStream &&  SUM_MEMORY_CONTENT_ALL > LARGE_CONTENT_SIZE){
            int size = memoryStream.size();
            useFileStream = true;
            file = File.createTempFile("SandroProxy", ".tmp");
            if (LOGD) Log.d(TAG, "Memory content. Creating temp file:"  + file.getAbsoluteFile());
            fileStream = new FileOutputStream(file);
            byte[] data = memoryStream.toByteArray();
            fileStream.write(data, 0, data.length);
            fileStream.flush();
            memoryStream = null;
            addRemoveActiveContentSum(size, true);
        }
        if (useFileStream){
            fileStream.write(buffer, offset, len);
        }else{
            memoryStream.write(buffer, offset, len);
            addRemoveActiveContentSum(len, false);
        }
    }
    
    public InputStream getInputStream(){
        InputStream result = null;
        if (!useFileStream){
            byte[] contentByteArray = memoryStream.toByteArray();
            result = new ByteArrayInputStream(contentByteArray);
        }else{
            try {
                result = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    public int size(){
        int result = 0;
        if (!useFileStream){
            result = memoryStream.size();
        }else{
            try {
                if (fileStream != null){
                    result = (int)fileStream.getChannel().size();
                }else{
                    FileInputStream fis = new FileInputStream(file);
                    result = (int)fis.getChannel().size();
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        if (useFileStream){
            fileStream.close();
            fileStream = null;
            if (LOGD) Log.d(TAG, "Memory content. Deleting temp file:"  + file.getAbsoluteFile());
            file.delete();
        }else{
            addRemoveActiveContentSum(size(), true);
            memoryStream.close();
            memoryStream = null;
        }
    }
}
