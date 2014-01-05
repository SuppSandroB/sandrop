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
package org.sandroproxy.logger;

import java.lang.Thread.State;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.os.Message;

public class LogHandler extends Handler {
    
    private static android.os.Handler mHandlerCallback;
    
    private static String messageBuffer;
    
    private static Object messageLock = new Object();

    private long lastSendMessageTs = 0;
    
    private Thread postingThread;
    
    public LogHandler(android.os.Handler guiHandlerCallBack){
        mHandlerCallback = guiHandlerCallBack;
        this.setLevel(Level.FINEST);
        messageBuffer = "";
        postingThread = new Thread(new Runnable() {
            public void run() {
                boolean checkLast = true;
                int count = 0;
                while (true){
                    if (messageBuffer.length() > 0){
                        long ts = System.currentTimeMillis();
                        if ((ts  - 100) > lastSendMessageTs){
                            String sendMessage = LogHandler.getMessageBuffer();
                            if (sendMessage.length() > 0){
                                Message msg = mHandlerCallback.obtainMessage(1, sendMessage);
                                mHandlerCallback.sendMessage(msg);
                                lastSendMessageTs = ts;
                            }
                        }
                    }
                    try {
                        if (checkLast){
                            Thread.sleep(50);
                            count++;
                            if (count >= 20){
                                checkLast = false;
                                count = 0;
                            }
                        }else{
                            synchronized (postingThread) {
                                postingThread.wait();
                                checkLast = true;
                                count = 0;
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
          },"LogHandler.logPumper");
        postingThread.start();
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void flush() {
        // TODO Auto-generated method stub
        
    }
    
    public static String getMessageBuffer(){
        String response = "";
        synchronized (messageLock){
            response = String.copyValueOf(messageBuffer.toCharArray());
            messageBuffer = "";
            
        }
        return response;
    }
    
    @Override
    public void publish(LogRecord record) {
        if (record != null && record.getMessage() != null){
            synchronized (messageLock){
                messageBuffer = record.getMessage()+ "\n" + messageBuffer;
            }
            State threadState = postingThread.getState();
            if (postingThread != null && threadState == State.WAITING){
                synchronized (postingThread) {
                    postingThread.notify();
                }
            }
        }
    }
}