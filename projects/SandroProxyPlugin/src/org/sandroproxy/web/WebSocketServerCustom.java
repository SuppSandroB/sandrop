package org.sandroproxy.web;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;
import org.sandroproxy.webscarab.store.sql.IStoreEventListener;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


public class WebSocketServerCustom extends WebSocketServer implements IStoreEventListener{

    private Context mContext;
    private SqlLiteStore mStore;
    
    private SocketWorkerThread mWorkerThread;
    private TestWorkerHandler mHandler;
    private Looper myLooper;
    private ChromeParser mParser;
    private HashMap<Long, Process> mProcessList;
    
    private static boolean LOGD = false;
    private static boolean LOGD_DETAILS = false;
    private static String TAG = WebSocketServerCustom.class.getName();
    
    private static boolean connSnapshotShouldStop = false;
    
    
    public WebSocketServerCustom(Context context, int port) throws UnknownHostException {
        super( new InetSocketAddress(port));
        mContext = context;
        mStore = SqlLiteStore.getInstance(context, null);
        mStore.addEventListeners(TAG, this);
        mParser = new ChromeParser(mContext, mStore);
        mProcessList = new HashMap<Long, Process>();
        mWorkerThread = new SocketWorkerThread("WebSocketServiceWorker", this);
        mWorkerThread.start();
       
    }
    
    private class SocketWorkerThread extends Thread {
        
        private final WebSocketServerCustom mServer; 
        public SocketWorkerThread(String name, final WebSocketServerCustom server) {
            super(name);
            mServer = server;
        }
        public void run() {
            Looper.prepare();
            mHandler = new TestWorkerHandler(mServer);
            myLooper = Looper.myLooper();
            Looper.loop();
        }
    }
 
    private class TestWorkerHandler extends Handler {
        
        private WebSocketServerCustom mServer;
        public TestWorkerHandler(WebSocketServerCustom server){
            mServer = server;
        }
        
        private static final int MESSAGE_NEW_CONVERSATION = 0;
        private static final int MESSAGE_START_CONVERSATION = 1;
        private static final int MESSAGE_END_CONVERSATION = 2;
        private static final int MESSAGE_GET_RESPONSE_BODY = 3;
        private static final int MESSAGE_GET_CSS_PROPERTIES = 4;
        private static final int MESSAGE_GET_RESOURCE_TREE = 6;
        private static final int MESSAGE_GET_RESOURCE_TREE_EMPTY = 7;
        private static final int MESSAGE_GET_RESOURCE_CONTENT = 8;
        private static final int MESSAGE_GET_STORED_NETWORK_EVENTS = 9;
        private static final int MESSAGE_RESPONSE_FLAG = 10;
        //private static final int MESSAGE_CONSOLE_LOG = 6;
        private static final int MESSAGE_FRAMES_WITH_MANIFEST = 11;
        private static final int MESSAGE_GET_COOKIES = 12;
        private static final int MESSAGE_GET_DATABASE_NAMES = 13;
        
        private static final int MESSAGE_GET_SOCKET_FRAME_SEND = 14;
        private static final int MESSAGE_GET_SOCKET_FRAME_RECEIVED = 15;
        private static final int MESSAGE_GET_DOM_STORAGE_ITEMS = 16;
        private static final int MESSAGE_GET_PROFILER_HEADERS = 17;
        
        private static final int MESSAGE_SANDROPROXY_CONN_SNAPSHOTS_START = 18;
        private static final int MESSAGE_SANDROPROXY_CONN_SNAPSHOTS_STOP = 19;
        
        private static final int MESSAGE_RUNTIME_EVALUATE = 20;
        
        
        
        @Override
        public void handleMessage(Message msg) {
            List<String> responseArrayOfJsonStrings= new ArrayList<String>();
            String socketId = msg.getData().getString("socketId");
            long id =  msg.getData().getLong("id");
            if (msg.what == MESSAGE_GET_CSS_PROPERTIES){
                String result = mParser.getCSSProperties(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_RESOURCE_CONTENT){
                long responseId =  msg.getData().getLong("responseId");
                String result = mParser.getResourceContent(id, responseId);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_RESOURCE_TREE){
                String result = mParser.getResourceTree(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_RESOURCE_TREE_EMPTY){
                String result = mParser.getEmptyResourceTree(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_FRAMES_WITH_MANIFEST){
                String result = mParser.getFramesWithEmptyManifests(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_COOKIES){
                String result = mParser.getCookiesEmpty(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_DATABASE_NAMES){
                String result = mParser.getDatabaseNamesEmpty(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_DOM_STORAGE_ITEMS){
                String result = mParser.getDOMStorageItemsEmpty(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_PROFILER_HEADERS){
                String result = mParser.getProfilerHeadersEmpty(id);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_RESPONSE_BODY){
                long requestId =  msg.getData().getLong("requestId");
                String result = mParser.getResponseBody(id, requestId);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_START_CONVERSATION) {
                long conversationId =  msg.getData().getLong("conversationId");
                String result = mParser.getRequestWillBeSend(null, conversationId);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToAll(responseArrayOfJsonStrings, true, false, true);
                }
            }else if (msg.what == MESSAGE_END_CONVERSATION) {
                long conversationId =  msg.getData().getLong("conversationId");
                boolean protocolSwitch =  msg.getData().getBoolean("protocolSwitch");
                List<String> result = mParser.getResponseReceived(null, conversationId, protocolSwitch);
                if (result != null && result.size() > 0){
                    responseArrayOfJsonStrings.addAll(result);
                    sentResponseToAll(responseArrayOfJsonStrings, true, false, true);
                }
            }else if (msg.what == MESSAGE_GET_SOCKET_FRAME_SEND) {
                long conversationId =  msg.getData().getLong("conversationId");
                long messageId =  msg.getData().getLong("messageId");
                List<String> result = mParser.getWebSocketFrameSent(conversationId, messageId);
                if (result != null && result.size() > 0){
                    responseArrayOfJsonStrings.addAll(result);
                    sentResponseToAll(responseArrayOfJsonStrings, true, false, true);
                }
            }else if (msg.what == MESSAGE_GET_SOCKET_FRAME_RECEIVED) {
                long conversationId =  msg.getData().getLong("conversationId");
                long messageId =  msg.getData().getLong("messageId");
                List<String> result = mParser.getWebSocketFrameReceived(conversationId, messageId);
                if (result != null && result.size() > 0){
                    responseArrayOfJsonStrings.addAll(result);
                    sentResponseToAll(responseArrayOfJsonStrings, true, false, true);
                }
            }else if (msg.what == MESSAGE_RUNTIME_EVALUATE) {
                long conversationId =  msg.getData().getLong("conversationId");
                long messageId =  msg.getData().getLong("messageId");
                String expression =  msg.getData().getString("expression");
                String group = msg.getData().getString("objectGroup");
                StringBuilder sb = new StringBuilder();
                if (expression != null && expression != null && !expression.equalsIgnoreCase("this")
                        && group != null && group.equalsIgnoreCase("console")){
                    if (!mProcessList.containsKey(conversationId)){
                        try {
                            Process process = Runtime.getRuntime().exec("sh");
                            mProcessList.put(conversationId, process);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    Process process = mProcessList.get(messageId);
                    DataOutputStream fout = new DataOutputStream(process.getOutputStream());
                    try {
                        fout.writeBytes(expression.toString() + "\n");
                        fout.flush();
                        fout.close();
                        
                        DataInputStream des = new DataInputStream(process.getErrorStream());
                        DataInputStream dis = new DataInputStream(process.getInputStream());
                        sb.append("\n");
                        byte[] buffer = new byte[4096];
                        int read = 0;
                        while(true){
                            read = des.read(buffer);
                            if (read > 0){
                                sb. append(new String(buffer, 0, read));
                            }
                            if(read < 4096){
                                break;
                            }
                        }
                        while(true){
                            read = dis.read(buffer);
                            if (read > 0){
                                sb. append(new String(buffer, 0, read));
                            }
                            if(read < 4096){
                                break;
                            }
                        }
                        dis.close();
                        des.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mProcessList.remove(conversationId);
                    
                    
                }else{
                    sb.append("");
                }
                String result;
                result = mParser.getRuntimeEvaluationResponse(id, sb.toString());
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_RESPONSE_FLAG) {
                boolean flag =  msg.getData().getBoolean("flag");
                String result = mParser.createResponseOnMethod(id, flag);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_SANDROPROXY_CONN_SNAPSHOTS_START) {
                String interval =  msg.getData().getString("interval");
                class ConnectionsSnapshotsRunnable extends Thread {
                    private final String socketId;
                    private final String interval;

                    ConnectionsSnapshotsRunnable(final String socketId, final String interval) {
                      this.socketId = socketId;
                      this.interval = interval;
                    }

                    public void run() {
                        // sleep so we do not send events to soon 
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        boolean validSocket = true;
                        // TODO this works well only on one client fix for many clients attached
                        connSnapshotShouldStop = false;
                        while(!connSnapshotShouldStop && validSocket){
                            try {
                                Thread.sleep(Integer.valueOf(interval));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            String result = mParser.getConnectionsSnapshot(true);
                            if (result != null){
                                List<String> responseArrayOfJsonStrings = new ArrayList<String>();
                                responseArrayOfJsonStrings.add(result);
                                validSocket = sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                            }
                        }
                    }
                 }
                ConnectionsSnapshotsRunnable runner = new ConnectionsSnapshotsRunnable(socketId, interval);
                runner.setName("ConnectionsSnapshotsRunnable");
                runner.start();
                // send response so client will know that task is started
                String result = mParser.createResponseOnMethod(id, true);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_SANDROPROXY_CONN_SNAPSHOTS_STOP) {
                connSnapshotShouldStop = true;
                String result = mParser.createResponseOnMethod(id, true);
                if (result != null){
                    responseArrayOfJsonStrings.add(result);
                    sentResponseToSocket(responseArrayOfJsonStrings, socketId, -1);
                }
            }else if (msg.what == MESSAGE_GET_STORED_NETWORK_EVENTS) {
                
                class NetworkEventsRunnable extends Thread {
                    private final String socketId;

                    NetworkEventsRunnable(final String socketId) {
                      this.socketId = socketId;
                    }

                    public void run() {
                        // sleep so we do not send events to soon 
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        List<Long> ids =  mParser.getAllNetworkEventsIds();
                        if (ids != null){
                            for (Long eventId : ids) {
                                List<String> eventData = mParser.getConversationData(eventId);
                                if (eventData != null && eventData.size() > 0){
                                    boolean result = sentResponseToSocket(eventData, socketId, -1);
                                    if (!result){
                                        break;
                                    }
                                }
                                List<Long> webSocketMessages = mParser.getAllWebSocketEventsIds(eventId);
                                if (webSocketMessages != null){
                                    for (Long webSocketMessageId : webSocketMessages) {
                                        List<String> socketEventData = mParser.generateWebSocketFrameEvent(eventId, webSocketMessageId);
                                        if (socketEventData != null && socketEventData.size() > 0){
                                            boolean result = sentResponseToSocket(socketEventData, socketId, -1);
                                            if (!result){
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                    }
                 }
                NetworkEventsRunnable runner = new NetworkEventsRunnable(socketId);
                runner.setName("GetStoredNetworkEvents");
                runner.start();
            }
        }
        
        private boolean sentResponseToSocket(List<String> responseArray, String socketId, long timeout){
           // create json to send to listeners
           Iterator<WebSocket> iter = mServer.connections().iterator();
           while(iter.hasNext()){
               WebSocket socket = iter.next();
               if (socket != null && socket.isOpen()){
                   if (socket instanceof WebSocketImplCustom){
                       WebSocketImplCustom socketImplCustom = (WebSocketImplCustom) socket;
                       if (socketImplCustom.isOpen()){
                           boolean validSocket = false;
                           if (socketId != null && socketImplCustom.getSocketId() != null){
                               if (socketImplCustom.getSocketId().equalsIgnoreCase(socketId)){
                                   validSocket = true;
                               }
                           }
                           if (validSocket == true){
                               sendToSocket(responseArray, socketImplCustom, timeout);
                               return true;
                           }
                       }
                  }
              }
           }
           return false;
        }
        
        private void sentResponseToAll(List<String> responseArray, boolean mustHaveRealTime, boolean mustHaveStoredData, boolean mustBeInitialised){
           Iterator<WebSocket> iter = mServer.connections().iterator();
           while(iter.hasNext()){
               WebSocket socket = iter.next();
               if (socket != null && socket.isOpen()){
                   if (socket instanceof WebSocketImplCustom){
                       WebSocketImplCustom socketImplCustom = (WebSocketImplCustom) socket;
                       if (socketImplCustom.isOpen()){
                           boolean validSocket = true;
                           if (mustHaveRealTime && !socketImplCustom.isShowRuntimeEvents()){
                               validSocket = false;
                           }
                           if (mustHaveStoredData && !socketImplCustom.isShowStoredEvents()){
                               validSocket = false;
                           }
                           if (mustBeInitialised && !socketImplCustom.isInitialised()){
                               validSocket = false;
                           }
                           if (validSocket == true){
                               sendToSocket(responseArray, socketImplCustom, -1);
                           }
                       }
                   }
               }
           }
        }
        
        private void sendToSocket(List<String> list, WebSocket socket, long timeout){
            for (String responseJsonString : list) {
                if (LOGD && LOGD_DETAILS) Log.d(TAG, "devtools message handler send" + responseJsonString);
                if (socket.isOpen()){
                    socket.send(responseJsonString);
                    if (timeout > 0){
                        try {
                            Thread.sleep(timeout);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        
    }

    @Override
    public void onClose(WebSocket webSocket, int arg1, String arg2, boolean arg3) {
        if (webSocket instanceof WebSocketImplCustom){
            ((WebSocketImplCustom)webSocket).setInitialised(false);
        }
        
    }

    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        if (LOGD) Log.d(TAG, String.format("exception message : %s ", arg1.getMessage()));
    }

    @Override
    public void onMessage(WebSocket webSocket, String arg1) {
        if (LOGD) Log.d(TAG, String.format("onMessage message : %s ", arg1));
        try {
            JSONObject obj = new JSONObject(arg1);
            String id = obj.getString("id");
            String method  = obj.getString("method");
            WebSocketImplCustom webSocketImplCustom =  (WebSocketImplCustom)webSocket;
            String socketId = webSocketImplCustom.getSocketId();
            boolean setEnabledFeature = false;
            
            
            if (method.equalsIgnoreCase("network.sandroProxyStartSendingConnSnapshots")){
                JSONObject params = obj.getJSONObject("params");
                String interval = params.getString("interval");
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_SANDROPROXY_CONN_SNAPSHOTS_START;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putString("interval", interval);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
            if (method.equalsIgnoreCase("network.sandroProxyStopSendingConnSnapshots")){
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_SANDROPROXY_CONN_SNAPSHOTS_STOP;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
            if (method.equalsIgnoreCase("network.getresponsebody")){
                JSONObject params = obj.getJSONObject("params");
                String requestId = params.getString("requestId");
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_RESPONSE_BODY;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("requestId", Long.valueOf(requestId));
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } 
            if (method.equalsIgnoreCase("network.enable")){
                setEnabledFeature = true;
                if (webSocket instanceof WebSocketImplCustom){
                    ((WebSocketImplCustom)webSocket).setInitialised(true);
                }
            }else if (method.equalsIgnoreCase("Network.canClearBrowserCookies")){
                
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_RESPONSE_FLAG;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                bundle.putBoolean("flag", setEnabledFeature);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                
                // we presume that this is last config query so we start sending network data 
                // if we have storage feature we send another msg to 
                if (webSocketImplCustom.isShowStoredEvents()){
                    Message msgStoredEvents = Message.obtain();
                    msg.what = TestWorkerHandler.MESSAGE_GET_STORED_NETWORK_EVENTS;
                    Bundle bundleStoredEvents = new Bundle();
                    bundleStoredEvents.putString("socketId", socketId);
                    msgStoredEvents.setData(bundleStoredEvents);
                    mHandler.sendMessage(msgStoredEvents);
                }
                return;
            } else if (method.equalsIgnoreCase("console.enable")){
                    setEnabledFeature = true;
            } else if (method.equalsIgnoreCase("CSS.getSupportedCSSProperties")){
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_CSS_PROPERTIES;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
                
            } else if (method.equalsIgnoreCase("ApplicationCache.getFramesWithManifests")){
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_FRAMES_WITH_MANIFEST;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("Page.getCookies")){
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_COOKIES;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("Page.getResourceTree")){
                Message msg = Message.obtain();
                if (webSocketImplCustom.isShowStoredEvents()){
                    msg.what = TestWorkerHandler.MESSAGE_GET_RESOURCE_TREE;
                }else{
                    msg.what = TestWorkerHandler.MESSAGE_GET_RESOURCE_TREE_EMPTY;
                }
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("Page.getResourceContent")){
                JSONObject params = obj.getJSONObject("params");
                String resourceId = params.getString("resourceId");
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_RESOURCE_CONTENT;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                bundle.putLong("responseId", Long.valueOf(resourceId));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("IndexedDB.requestDatabaseNames")){
                JSONObject params = obj.getJSONObject("params");
                String securityOrigin = params.getString("securityOrigin");
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_DATABASE_NAMES;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                bundle.putString("securityOrigin", securityOrigin);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("DOMStorage.getDOMStorageItems")){
                JSONObject params = obj.getJSONObject("params");
                String storageId = params.getString("storageId");
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_DOM_STORAGE_ITEMS;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                bundle.putString("storageId", storageId);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("Profiler.getProfileHeaders") || 
                    method.equalsIgnoreCase("HeapProfiler.getProfileHeaders")){
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_GET_PROFILER_HEADERS;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            } else if (method.equalsIgnoreCase("Runtime.evaluate")){
                JSONObject params = obj.getJSONObject("params");
                String expression = params.getString("expression");
                String group = params.getString("objectGroup");
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_RUNTIME_EVALUATE;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                bundle.putString("expression", expression);
                bundle.putString("objectGroup", group);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }else{
                Message msg = Message.obtain();
                msg.what = TestWorkerHandler.MESSAGE_RESPONSE_FLAG;
                Bundle bundle = new Bundle();
                bundle.putString("socketId", socketId);
                bundle.putLong("id", Long.valueOf(id));
                bundle.putBoolean("flag", setEnabledFeature);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (LOGD) Log.d(TAG, "error parsing " + e.getMessage());
        }
    }

    @Override
    public void onOpen(final WebSocket webSocket, ClientHandshake handshake) {
        String resourceId = handshake.getResourceDescriptor();
        WebSocketImplCustom webSocketImplCustom = (WebSocketImplCustom)webSocket;
        if (LOGD) Log.d(TAG, "Resource descriptor: " + resourceId);
        for (Iterator<String> iterator = handshake.iterateHttpFields(); iterator.hasNext();) {
            String httpField = (String) iterator.next();
            String value = handshake.getFieldValue(httpField);
            if (LOGD) Log.d(TAG, String.format("httpField message : %s  value: %s", httpField, value));
        }
        String id = handshake.getFieldValue("sec-websocket-key");
        id = id + new Random().nextLong();
        webSocketImplCustom.setSocketId(id);
        if (resourceId.equalsIgnoreCase("/devtools/page/1")){
            webSocketImplCustom.setShowRuntimeEvents(true);
        }else if (resourceId.equalsIgnoreCase("/devtools/page/2")){
            webSocketImplCustom.setShowStoredEvents(true);
        }
    }
    
    private boolean haveRuntimeEventsListener(){
        boolean haveValidListener = false;
        Iterator<WebSocket> it = this.connections().iterator();
        while(it.hasNext()){
            WebSocketImplCustom webSocketImplCustom = (WebSocketImplCustom) it.next();
            if (webSocketImplCustom.isOpen() && webSocketImplCustom.isShowRuntimeEvents()){
                haveValidListener = true;
                break;
            }
        }
        return haveValidListener;
    }
    
    private boolean haveStorageEventsListener(){
        boolean haveValidListener = false;
        Iterator<WebSocket> it = this.connections().iterator();
        while(it.hasNext()){
            WebSocketImplCustom webSocketImplCustom = (WebSocketImplCustom) it.next();
            if (webSocketImplCustom.isOpen() && webSocketImplCustom.isShowStoredEvents()){
                haveValidListener = true;
                break;
            }
        }
        return haveValidListener;
    }

    @Override
    public void newConversation(long conversationId, int type, long timestamp) {
        if (mHandler != null && haveRuntimeEventsListener()){
            Message msg = Message.obtain();
            msg.what = TestWorkerHandler.MESSAGE_NEW_CONVERSATION;
            Bundle bundle = new Bundle();
            bundle.putLong("conversationId", conversationId);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        
    }
    
    @Override
    public void startConversation(long conversationId, long timestamp) {
        if (mHandler != null && haveRuntimeEventsListener()){
            Message msg = Message.obtain();
            msg.what = TestWorkerHandler.MESSAGE_START_CONVERSATION;
            Bundle bundle = new Bundle();
            bundle.putLong("conversationId", conversationId);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }
    
    @Override
    public void endConversation(long conversationId, boolean protocolSwitch, long timestamp) {
        
        if (mHandler != null && haveRuntimeEventsListener()){
            Message msg = Message.obtain();
            msg.what = TestWorkerHandler.MESSAGE_END_CONVERSATION;
            Bundle bundle = new Bundle();
            bundle.putLong("conversationId", conversationId);
            bundle.putBoolean("protocolSwitch", protocolSwitch);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }
    
    @Override
    public void socketFrameSend(long conversationId, long channelId, long messageId, long timestamp) {
        if (mHandler != null && haveRuntimeEventsListener()){
            Message msg = Message.obtain();
            msg.what = TestWorkerHandler.MESSAGE_GET_SOCKET_FRAME_SEND;
            Bundle bundle = new Bundle();
            bundle.putLong("conversationId", conversationId);
            bundle.putLong("messageId", messageId);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        
    }

    @Override
    public void socketFrameReceived(long conversationId, long channelId, long messageId, long timestamp) {
        if (mHandler != null && haveRuntimeEventsListener()){
            Message msg = Message.obtain();
            msg.what = TestWorkerHandler.MESSAGE_GET_SOCKET_FRAME_RECEIVED;
            Bundle bundle = new Bundle();
            bundle.putLong("conversationId", conversationId);
            bundle.putLong("messageId", messageId);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        
    }
    
    @Override
    public void stop()throws IOException{
        try {
            // TODO possible race condition and crash
            super.stop();
            // TODO just remove this listener no all set to null
            mStore.removeEventListeners(TAG);
            if (myLooper != null){
                myLooper.quit();
            }
            mHandler = null;
            myLooper = null;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public void start(){
        super.start();
    }

    @Override
    public void socketChannelChanged(long conversationId, long channelId, long timestamp) {
        // TODO Auto-generated method stub
        
    }

    
}
