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
/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sandroproxy.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.sandrop.webscarab.model.Response;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;

 
/**
 * The WebSockets-extension takes over after the HTTP based WebSockets handshake
 * is finished.
 * 
 * @author Robert Koch
 */
public class ExtensionWebSocket {
    
    private static final Logger logger = Logger.getLogger(ExtensionWebSocket.class.getName());
    
    public static final int HANDSHAKE_LISTENER = 10;
    
    /**
     * Name of this extension.
     */
    public static final String NAME = "ExtensionWebSocket";

    /**
     * Used to shorten the time, a listener is started on a WebSocket channel.
     */
    private ExecutorService listenerThreadPool;

    /**
     * List of observers where each element is informed on all channel's
     * messages.
     */
    private Map<String, WebSocketObserver> allChannelObservers;

    /**
     * Contains all proxies with their corresponding handshake message.
     */
    private Map<Long, WebSocketProxy> wsProxies;


    private WebSocketStorage storageObserver;

    
    public ExtensionWebSocket(SqlLiteStore store) {
        allChannelObservers = new HashMap<String, WebSocketObserver>();
        wsProxies = new HashMap<Long, WebSocketProxy>();
        storageObserver = new WebSocketStorage(store);
        allChannelObservers.put(WebSocketStorage.class.getName(), storageObserver);
    }
    
    

    
    public void unload() {
        
        // close all existing connections
        for (Entry<Long, WebSocketProxy> wsEntry : wsProxies.entrySet()) {
            WebSocketProxy wsProxy = wsEntry.getValue();
            wsProxy.shutdown();
        }
    }

    
    /**
     * Add an observer that is attached to every channel connected in future.
     * 
     * @param observer
     */
    public void addAllChannelObserver(String name, WebSocketObserver observer) {
        allChannelObservers.put(name, observer);
    }
    
    /**
     * Add an observer that is attached to every channel connected in future.
     * 
     * @param observer
     */
    public void removeAllChannelObserver(String name, WebSocketObserver observer) {
        allChannelObservers.remove(name);
    }



    public boolean onHandshakeResponse(long handshakeReference, Response httpResponse, Socket inSocket, Socket outWebSocket, InputStream outWebInputStream) {

        boolean keepSocketOpen = false;
        
        logger.info("Got WebSockets upgrade request. Handle socket connection over to WebSockets extension.");
        
        Socket outSocket = outWebSocket;
        InputStream outReader = outWebInputStream;
        addWebSocketsChannel(handshakeReference, httpResponse, inSocket, outSocket, outReader);
        
        return keepSocketOpen;
    }

    /**
     * Add an open channel to this extension after
     * HTTP handshake has been completed.
     * 
     * @param handshakeMessage HTTP-based handshake.
     * @param localSocket Current connection channel from the browser to ZAP.
     * @param remoteSocket Current connection channel from ZAP to the server.
     * @param remoteReader Current {@link InputStream} of remote connection.
     */
    public void addWebSocketsChannel(long historyId, Response httpResponse, Socket localSocket, Socket remoteSocket, InputStream remoteReader) {
        try {            

            String source = (localSocket != null) ? localSocket.getInetAddress().toString() + ":" + localSocket.getPort() : "SandroProxy";
            String destination = remoteSocket.getInetAddress() + ":" + remoteSocket.getPort();
            
            logger.info("Got WebSockets channel from " + source + " to " + destination);
            
            // parse HTTP handshake
            Map<String, String> wsExtensions = parseWebSocketExtensions(httpResponse);
            String wsProtocol = parseWebSocketSubProtocol(httpResponse);
            String wsVersion = parseWebSocketVersion(httpResponse);
    
            WebSocketProxy wsProxy = null;
            wsProxy = WebSocketProxy.create(wsVersion, localSocket, remoteSocket, wsProtocol, wsExtensions);
            
            // set other observers and handshake reference, before starting listeners
            for (WebSocketObserver observer : allChannelObservers.values()) {
                // TODO here we could also have map so we can dynamically remove observers
                wsProxy.addObserver(observer);
            }
            
            wsProxy.setHandshakeReference(historyId);
            
            // TODO sandrop some regular expression what to have in ignore list 
            // wsProxy.setForwardOnly(isChannelIgnored(wsProxy.getDTO()));
            wsProxy.startListeners(getListenerThreadPool(), remoteReader);
            
            synchronized (wsProxies) {
                wsProxies.put(wsProxy.getChannelId(), wsProxy);
            }
        } catch (Exception e) {
            // defensive measure to catch all possible exceptions
            // cleanly close resources
            if (localSocket != null && !localSocket.isClosed()) {
                try {
                    localSocket.close();
                } catch (IOException e1) {
                    logger.info(e.getMessage());
                }
            }
            
            if (remoteReader != null) {
                try {
                    remoteReader.close();
                } catch (IOException e1) {
                    logger.info(e.getMessage());
                }
            }
            
            if (remoteSocket != null && !remoteSocket.isClosed()) {
                try {
                    remoteSocket.close();
                } catch (IOException e1) {
                    logger.info(e.getMessage());
                }
            }
            logger.info("Adding WebSockets channel failed due to: '" + e.getClass() + "' " + e.getMessage());
            return;
        }
    }

    /**
     * Parses the negotiated WebSockets extensions. It splits them up into name
     * and params of the extension. In future we want to look up if given
     * extension is available as ZAP extension and then use their knowledge to
     * process frames.
     * <p>
     * If multiple extensions are to be used, they can all be listed in a single
     * {@link WebSocketProtocol#HEADER_EXTENSION} field or split between multiple
     * instances of the {@link WebSocketProtocol#HEADER_EXTENSION} header field.
     * 
     * @param msg
     * @return Map with extension name and parameter string.
     */
    private Map<String, String> parseWebSocketExtensions(Response msg) {
        Vector<String> extensionHeaders = null;
        // TODO sandrop this is not used so can be null currently
//        Vector<String> extensionHeaders = msg.getHeader(
//                WebSocketProtocol.HEADER_EXTENSION);

        if (extensionHeaders == null) {
            return null;
        }
        
        /*
         * From http://tools.ietf.org/html/rfc6455#section-4.3:
         *   extension-list = 1#extension
           *   extension = extension-token *( ";" extension-param )
         *   extension-token = registered-token
         *   registered-token = token
         *   extension-param = token [ "=" (token | quoted-string) ]
         *    ; When using the quoted-string syntax variant, the value
         *    ; after quoted-string unescaping MUST conform to the
         *    ; 'token' ABNF.
         *    
         * e.g.:      Sec-WebSocket-Extensions: foo
         *             Sec-WebSocket-Extensions: bar; baz=2
         *      is exactly equivalent to:
         *             Sec-WebSocket-Extensions: foo, bar; baz=2
         * 
         * e.g.:    Sec-WebSocket-Extensions: deflate-stream
         *             Sec-WebSocket-Extensions: mux; max-channels=4; flow-control, deflate-stream
         *             Sec-WebSocket-Extensions: private-extension
         */
        Map<String, String> wsExtensions = new LinkedHashMap<String, String>();
        for (String extensionHeader : extensionHeaders) {
            for (String extension : extensionHeader.split(",")) {
                String key = extension.trim();
                String params = "";
                
                int paramsIndex = key.indexOf(";");
                if (paramsIndex != -1) {
                    key = extension.substring(0, paramsIndex).trim();
                    params = extension.substring(paramsIndex + 1).trim();
                }
                
                wsExtensions.put(key, params);
            }
        }
        
        /*
         * The interpretation of any extension parameters, and what constitutes
         * a valid response by a server to a requested set of parameters by a
         * client, will be defined by each such extension.
         * 
         * Note that the order of extensions is significant!
         */
        
        return wsExtensions;
    }

    /**
     * Parses negotiated protocols out of the response header.
     * <p>
     * The {@link WebSocketProtocol#HEADER_PROTOCOL} header is only allowed to
     * appear once in the HTTP response (but several times in the HTTP request).
     * 
     * A server that speaks multiple sub-protocols has to make sure it selects
     * one based on the client's handshake and specifies it in its handshake.
     * 
     * @param msg
     * @return Name of negotiated sub-protocol or null.
     */
    private String parseWebSocketSubProtocol(Response msg) {
        String subProtocol = msg.getHeader(
                WebSocketProtocol.HEADER_PROTOCOL);
        return subProtocol;
    }

    /**
     * The {@link WebSocketProtocol#HEADER_VERSION} header might not always
     * contain a number. Therefore I return a string. Use the version to choose
     * the appropriate processing class.
     * 
     * @param msg
     * @return Version of the WebSockets channel, defining the protocol.
     */
    private String parseWebSocketVersion(Response msg) {
        String version = msg.getHeader(
                WebSocketProtocol.HEADER_VERSION);
        
        if (version == null) {
            // check for requested WebSockets version
            version = msg.getHeader(WebSocketProtocol.HEADER_VERSION);
            
            if (version == null) {
                // default to version 13 if non is given, for whatever reason
                logger.info("No " + WebSocketProtocol.HEADER_VERSION + " header was provided - try version 13");
                version = "13";
            }
        }
        
        return version;
    }

    /**
     * Creates and returns a cached thread pool that should speed up
     * {@link WebSocketListener}.
     * 
     * @return
     */
    private ExecutorService getListenerThreadPool() {
        if (listenerThreadPool == null) {
            listenerThreadPool = Executors.newCachedThreadPool();
        }
        return listenerThreadPool;
    }


    /**
     * Returns true if given channel id is connected.
     * 
     * @param channelId
     * @return True if connection is still alive.
     */
    public boolean isConnected(long channelId) {
        synchronized (wsProxies) {
            if (wsProxies.containsKey(channelId)) {
                return wsProxies.get(channelId).isConnected();
            }
        }
        return false;
    }
    
    /**
     * send message with proxy with channelId
     * @param channelId
     * @param message
     * @return true if sucessfull, false if proxy not connected
     * @throws IOException
     */
    public boolean sendMessage(long channelId, WebSocketMessageDTO message, boolean notify) throws IOException{
        if (isConnected(channelId)){
            WebSocketProxy proxy =  wsProxies.get(channelId);
            if (proxy != null){
                WebSocketMessage msg = proxy.sendAndNotify(message, notify);
                // we still store message if notify is false
                if (msg != null && !notify){
                    storageObserver.insertMessage(msg.getDTO());
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * retruns list of connected proxies
     * @return ap<Long, String> channelId, description
     */
    public Map<Long, String> getConnectedProxies(){
        Map<Long, String> proxies = new LinkedHashMap<Long, String>();
        for(WebSocketProxy proxy : wsProxies.values()){
            if (proxy.isConnected()){
                proxies.put(proxy.getChannelId(), proxy.toString());
            }
        }
        return proxies;
    }
    
}
