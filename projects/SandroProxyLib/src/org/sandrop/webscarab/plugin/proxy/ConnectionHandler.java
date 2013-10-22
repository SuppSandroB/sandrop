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

package org.sandrop.webscarab.plugin.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.httpclient.HTTPClientFactory;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.util.HtmlEncoder;
import org.sandroproxy.utils.DNSProxy;

import android.util.Log;

public class ConnectionHandler implements Runnable {
    
    private ProxyPlugin[] _plugins = null;
    private Proxy _proxy;
    private Socket _sock = null;
    private HttpUrl _base;
    private boolean _transparent = false;
    private boolean _transparentSecure = false;
    private boolean _captureData = true;
    private ITransparentProxyResolver _transparentResolver = null;
    private IClientResolver _clientResolver = null;

    private HTTPClient _httpClient = null;

    private Logger _logger = Logger.getLogger(getClass().getName());
    
    private static boolean LOGD = false;
    private static String TAG = ConnectionHandler.class.getSimpleName();
    private static int _socket_timeout_large = 1000 * 60 * 30;
    private static int _socket_timeout_normal = 1000 * 30;
    
    private InputStream _clientIn = null;
    private OutputStream _clientOut = null;

    public ConnectionHandler(Proxy proxy, Socket sock, HttpUrl base, boolean transparent, boolean transparentSecure, boolean captureData, 
                                                            ITransparentProxyResolver transparentProxyResolver,
                                                            IClientResolver clientResolver) {
        _logger.setLevel(Level.FINEST);
        _proxy = proxy;
        _sock = sock;
        _base = base;
        _transparent = transparent;
        _transparentSecure = transparentSecure;
        _transparentResolver = transparentProxyResolver;
        _clientResolver = clientResolver;
        _plugins = _proxy.getPlugins();
        _captureData = captureData;
        try {
            _sock.setTcpNoDelay(true);
            _sock.setSoTimeout(_socket_timeout_normal);
        } catch (SocketException se) {
            _logger.warning("Error setting socket parameters");
        }
    }

    public void run() {
        ScriptableConnection connection = new ScriptableConnection(_sock);
        _proxy.allowClientConnection(connection);
        if (_sock.isClosed())
            return;

        try {
            _clientIn = _sock.getInputStream();
            _clientOut = _sock.getOutputStream();
        } catch (IOException ioe) {
            _logger.severe("Error getting socket input and output streams! "
                    + ioe);
            return;
        }
        long conversationId = -1;
        boolean httpDataModified = false;
        boolean switchProtocol = false;
        try {
            
            ConnectionDescriptor connectionDescriptor = null;
            if (_clientResolver != null && _captureData){
                connectionDescriptor = _clientResolver.getClientDescriptorBySocket(_sock);
            }
            
            Request request = null;
            // if we do not already have a base URL (i.e. we operate as a normal
            // proxy rather than a reverse proxy), check for a CONNECT
            if (_base == null && !_transparentSecure){
                try {
                    request = new Request(_transparent, _transparentSecure);
                    request.read(_clientIn);
                    HttpUrl requestUrl = request.getURL();
                    String host = requestUrl.getHost();
                    String reverseHost = DNSProxy.getHostNameFromIp(host);
                    if (reverseHost != null){
                        host = reverseHost != null ? reverseHost : host;
                        requestUrl = new HttpUrl(requestUrl.getScheme() + "://" + host +":" +  requestUrl.getPort() + requestUrl.getPath());
                        request.setURL(requestUrl);
                        request.setHeader("Host", host);
                    }
                } catch (IOException ioe) {
                    _logger.severe("Error reading the initial request" + ioe);
                    return;
                }
            }
            // if we are a normal proxy (because request is not null)
            // and the request is a CONNECT, get the base URL from the request
            // and send the OK back. We set request to null so we read a new
            // one from the SSL socket later
            // If it exists, we pull the ProxyAuthorization header from the
            // CONNECT
            // so that we can use it upstream.
            String proxyAuth = null;
            if (request != null) {
                String method = request.getMethod();
                if (request.getURL() == null) {
                    return;
                } else if (method.equals("CONNECT")) {
                    if (_clientOut != null) {
                        try {
                            _clientOut.write(("HTTP/1.0 200 Ok\r\n\r\n")
                                    .getBytes());
                            _clientOut.flush();
                        } catch (IOException ioe) {
                            _logger
                                    .severe("IOException writing the CONNECT OK Response to the browser "
                                            + ioe);
                            return;
                        }
                    }
                    _base = request.getURL();
                    proxyAuth = request.getHeader("Proxy-Authorization");
                    request = null;
                }
            }
            // if we are servicing a CONNECT, or operating as a reverse
            // proxy with an https:// base URL, negotiate SSL
            if (_base != null || _transparentSecure) {
                if (_transparentSecure || _base.getScheme().equals("https")) {
                    SiteData hostData = null;
                    if (!_captureData){
                        if (_transparentSecure){
                            if (_transparentResolver != null){
                                hostData = _transparentResolver.getSecureHost(_sock);
                            }else{
                                _logger.fine("!! Error Can not act as forwarder on transparent ssl, not knowing where to connect.");
                                _sock.close();
                                return;
                            }
                            String forwarderName = hostData.name + ":" + hostData.destPort;
                            _logger.fine("Acting as forwarder on " + forwarderName);
                            String hostName = hostData.hostName != null ? hostData.hostName : hostData.tcpAddress;
                            _base = new HttpUrl("https://" + hostName + ":" +  hostData.destPort);
                            boolean useFakeCertificates = true;
                            Socket target;
                            if (useFakeCertificates){
                                // make ssl tunnel with client with fake certificates
                                _sock = negotiateSSL(_sock, hostData, true);
                                // make ssl with server 
                                target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, true);
                            } else{
                                target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, false);
                            }
                            SocketForwarder.connect(forwarderName, _sock, target);
                            return;
                        }else{
                            String forwarderName = _base.getHost() + ":" + _base.getPort();
                            _logger.fine("Acting as forwarder on " + forwarderName);
                            Socket target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, false);
                            SocketForwarder.connect(forwarderName, _sock, target);
                            return;
                        }
                        
                    }
                    _logger.fine("Intercepting SSL connection!");
                    if (_transparentSecure){
                        if (_transparentResolver != null){
                            hostData = _transparentResolver.getSecureHost(_sock);
                        }
                    }else{
                        hostData = new SiteData();
                        hostData.name = _base.getHost();
                    }
                    String host = "sandroproxy.untrusted";
                    if (hostData == null || hostData.name.trim().length() == 0){
                        hostData = new SiteData();
                        hostData.name = host;
                    }
                    
                    boolean isSSLPort = false;
                    boolean checkForSSL = true;
                    if (!_transparentSecure){
                        // 433 port should be ssl, 80 without
                        if (_base.getPort() == 443){
                            isSSLPort = true;
                            checkForSSL = false;
                        }else if (_base.getPort() == 80){
                            isSSLPort = false;
                            checkForSSL = false;
                        }
                    }else{
                        checkForSSL = false;
                        isSSLPort = true;
                    }
                    
                    // 2. trying to connect to server to see if ssl works 
                    if (checkForSSL){
                        try{
                            int testReadTimeout = 2000;
                            HTTPClient hc =  HTTPClientFactory.getValidInstance().getHTTPClient(-1, testReadTimeout);
                            Request testRequest = new Request();
                            testRequest.setURL(_base);
                            testRequest.setMethod("GET");
                            testRequest.setNoBody();
                            hc.fetchResponse(testRequest);
                            isSSLPort = true;
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                    }
                    
                    // should ssl be used to connect to client 
                    if (isSSLPort){
                        SSLSocket sslSocket = null;
                        try{
                            _sock = negotiateSSL(_sock, hostData, false);
                            sslSocket = (SSLSocket)_sock;
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                        // we have no exception but ssl chiper is null-> switch to http
                        if (sslSocket == null || sslSocket.getSession() == null){
                            _logger.finest("!!Error Check if client trust SandroProxy CA certificate or ignore on ws:// protocol");
                            String oldHost = _base.getHost();
                            int oldPort = _base.getPort();
                            _base = new HttpUrl("http://" + oldHost + ":"+ oldPort);
                        }else{
                            
                            _clientIn = _sock.getInputStream();
                            PushbackInputStream pis = new PushbackInputStream(_clientIn);
                            int readBit;
                            try{
                                readBit = pis.read();
                            }catch (Exception ex){
                                _logger.finest("!!Error Check if client trust SandroProxy CA certificate \n!! or could be using SSL pinning so mitm will not work");
                                return;
                            }
                            
                            if (readBit != -1){
                                pis.unread(readBit);
                            }else{
                                _logger.finest("!!Error Check if client trust SandroProxy CA certificate \n!! or could be using SSL pinning so mitm will not work");
                                return;
                            }
                            _clientIn = pis;
                            _clientOut = _sock.getOutputStream();
                        }
                    }else{
                        String oldHost = _base.getHost();
                        int oldPort = _base.getPort();
                        _base = new HttpUrl("http://" + oldHost + ":"+ oldPort);
                    }
                }
            }
            
            if (_httpClient == null)
                _httpClient = HTTPClientFactory.getValidInstance().getHTTPClient();

            HTTPClient hc = _httpClient;

            // Maybe set SSL ProxyAuthorization here at a connection level?
            // I prefer it in the Request itself, since it gets archived, and
            // can be replayed trivially using netcat

            // layer the proxy plugins onto the recorder. We do this
            // in reverse order so that they operate intuitively
            // the first plugin in the array gets the first chance to modify
            // the request, and the last chance to modify the response
            // we also set flag if there is any chance that request/response is modified
            if (_plugins != null) {
                for (int i = _plugins.length - 1; i >= 0; i--) {
                    ProxyPlugin plugin = _plugins[i];
                    if (plugin.getEnabled()){
                        httpDataModified = true;
                    }
                    hc = plugin.getProxyPlugin(hc);
                }
            }

            // do we keep-alive?
            String keepAlive = null;
            String version = null;
            int reuseCount = 1;
            do {
                conversationId = -1;
                // if we are reading the first from a reverse proxy, or the
                // continuation of a CONNECT from a normal proxy
                // read the request, otherwise we already have it.
                if (request == null) {
                    request = new Request(_transparent, _transparentSecure);
                    _logger.fine("Reading request from the browser");
                    _sock.setSoTimeout(_socket_timeout_large);
                    request.read(_clientIn, _base);
                    if (request.getMethod() == null || request.getURL() == null) {
                        return;
                    }
                    HttpUrl requestUrl = request.getURL();
                    String host = requestUrl.getHost();
                    String reverseHost = DNSProxy.getHostNameFromIp(host);
                    if (reverseHost != null){
                        host = reverseHost != null ? reverseHost : host;
                        requestUrl = new HttpUrl(requestUrl.getScheme() + "://" + host +":" +  requestUrl.getPort() + requestUrl.getPath());
                        request.setURL(requestUrl);
                        request.setHeader("Host", host);
                    }
                    if (proxyAuth != null) {
                        request.addHeader("Proxy-Authorization", proxyAuth);
                    }
                }
                if (request.getURL() == null){
                    return;
                }
                
                if (request.getMethod().equals("CONNECT")){
                    if (_clientOut != null) {
                        try {
                            if (LOGD) Log.d(TAG, "Having connect method so we send that we are already connected");
                            _clientOut.write(("HTTP/1.0 200 Ok\r\n\r\n")
                                    .getBytes());
                            _clientOut.flush();
                            
                            Response response = new Response();
                            response.setStatus("200");
                            response.setMessage("OK");
                            response.setHeader("X-SandroProxy-Hack", "CONNECT_OVER_SSL_BUG http://code.google.com/p/android/issues/detail?id=55003");
                            response.setNoBody();
                            // store this conversation in store if enabled
                            conversationId = _proxy.gotRequest(request, connectionDescriptor);
                            _proxy.gotResponse(conversationId, request, response, false);
                            request = null;
                            continue;
                        } catch (IOException ioe) {
                            _logger
                                    .severe("IOException writing the CONNECT OK Response to the browser "
                                            + ioe);
                            return;
                        }
                    }
                }
                
                String clientDesc = "";
                if (connectionDescriptor != null && connectionDescriptor.getNamespaces() != null && connectionDescriptor.getNamespaces().length > 0){
                    clientDesc = connectionDescriptor.getNamespaces()[0];
                }
                _logger.fine( clientDesc + " requested : " + request.getMethod() + " "+ request.getURL().toString());

                // report the request to the listener, and get the allocated ID
                conversationId = _proxy.gotRequest(request, connectionDescriptor);

                // pass the request for possible modification or analysis
                connection.setRequest(request);
                connection.setResponse(null);
                _proxy.interceptRequest(connection);
                request = connection.getRequest();
                Response response = connection.getResponse();

                if (request == null)
                    throw new IOException("Request was cancelled");
                if (response != null) {
                    _proxy.failedResponse(request, response, conversationId, "Response provided by script", httpDataModified);
                    _proxy = null;
                } else {

                    // pass the request through the plugins, and return the
                    // response
                    try {
                        response = hc.fetchResponse(request);
                        if (response.getRequest() != null)
                            request = response.getRequest();
                    } catch (IOException ioe) {
                        _logger
                                .severe("IOException retrieving the response for "
                                        + request.getURL() + " : " + ioe);
                        ioe.printStackTrace();
                        response = errorResponse(request, ioe);
                        // prevent the conversation from being
                        // submitted/recorded
                        _proxy.failedResponse(request, response, conversationId, ioe.toString(), httpDataModified);
                        _proxy = null;
                    }
                    if (response == null) {
                        _logger.severe("Got a null response from the fetcher");
                        _proxy.failedResponse(request, response, conversationId, "Null response", httpDataModified);
                        return;
                    }
                }

                if (_proxy != null) {
                    // pass the response for analysis or modification by the
                    // scripts
                    connection.setResponse(response);
                    _proxy.interceptResponse(connection);
                    response = connection.getResponse();
                }

                if (response == null)
                    throw new IOException("Response was cancelled");

                try {
                    if (_clientOut != null) {
                        _logger.fine("Writing the response to the browser");
                        if (response.getStatus().equalsIgnoreCase("101")){
                            switchProtocol = true;
                            _logger.fine("Switching protocols on 101 code");
                            _proxy.getWebSocketManager().addWebSocketsChannel(conversationId, response, _sock, response.getSocket(), response.getSocket().getInputStream());
                            response.writeSwitchProtocol(_clientOut);
                            _logger.fine("Finished writing headers to client");
                        }else{
                            response.write(_clientOut);
                        }
                        
                        _logger.fine("Finished writing the response to the browser");
                    }
                } catch (IOException ioe) {
                    _logger
                            .severe("Error writing back to the browser : "
                                    + ioe);
                } finally {
                    if (!switchProtocol){
                        response.flushContentStream(); // this simply flushes the
                        // content from the server
                    }
                }
                // this should not happen, but might if a proxy plugin is
                // careless
                if (response.getRequest() == null) {
                    _logger.warning("Response had no associated request!");
                    response.setRequest(request);
                }
                if (_proxy != null && !request.getMethod().equals("CONNECT")) {
                    _proxy.gotResponse(conversationId, request, response, httpDataModified);
                }

                keepAlive = response.getHeader("Connection");
                version = response.getVersion();

                request = null;

                _logger.fine("Version: " + version + " keepAlive: " + keepAlive + " reuseCount:" + reuseCount);
                reuseCount++;
            } while (!switchProtocol && 
                    ((version.equals("HTTP/1.0") && "keep-alive".equalsIgnoreCase(keepAlive)) 
                    || (version.equals("HTTP/1.1") && !"close".equalsIgnoreCase(keepAlive)))
                    );
            _logger.fine("Finished handling connection");
        } catch (Exception e) {
            if (conversationId != -1)
                _proxy.failedResponse(null, null, conversationId, e.getMessage(), httpDataModified);
            _logger.severe("ConnectionHandler got an error : " + e);
            e.printStackTrace();
        } finally {
            try {
                if (!switchProtocol){
                    if (_clientIn != null)
                        _clientIn.close();
                    if (_clientOut != null)
                        _clientOut.close();
                    if (_sock != null && !_sock.isClosed()) {
                        _sock.close();
                    }
                }
                
            } catch (IOException ioe) {
                _logger.warning("Error closing client socket : " + ioe);
            }
        }
    }

    // http://anonsvn.wireshark.org/wireshark/trunk-1.0/epan/dissectors/packet-ssl-utils.c static SslCipherSuite cipher_suites[]={
    // we take wireshark version 1.10, currently is 1.10.2 names are from trunk 
    // http://anonsvn.wireshark.org/wireshark/trunk/epan/dissectors/packet-ssl-utils.c static SslCipherSuite cipher_suites[]={
    // if you have some older version of wireshark cipher could not be supported 
    // there will be error line like <dissect_ssl3_hnd_srv_hello can't find cipher suite >
    /*
    static SslCipherSuite cipher_suites[]={
        {1,KEX_RSA,SIG_RSA,ENC_NULL,1,0,0,DIG_MD5,16,0, SSL_CIPHER_MODE_STREAM},
        {2,KEX_RSA,SIG_RSA,ENC_NULL,1,0,0,DIG_SHA,20,0, SSL_CIPHER_MODE_STREAM},
        {3,KEX_RSA,SIG_RSA,ENC_RC4,1,128,40,DIG_MD5,16,1, SSL_CIPHER_MODE_STREAM},
        {4,KEX_RSA,SIG_RSA,ENC_RC4,1,128,128,DIG_MD5,16,0, SSL_CIPHER_MODE_STREAM},
        {5,KEX_RSA,SIG_RSA,ENC_RC4,1,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_STREAM},
        {6,KEX_RSA,SIG_RSA,ENC_RC2,8,128,40,DIG_SHA,20,1, SSL_CIPHER_MODE_STREAM},
        {7,KEX_RSA,SIG_RSA,ENC_IDEA,8,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_STREAM},
        {8,KEX_RSA,SIG_RSA,ENC_DES,8,64,40,DIG_SHA,20,1, SSL_CIPHER_MODE_CBC},
        {9,KEX_RSA,SIG_RSA,ENC_DES,8,64,64,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {10,KEX_RSA,SIG_RSA,ENC_3DES,8,192,192,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {11,KEX_DH,SIG_DSS,ENC_DES,8,64,40,DIG_SHA,20,1, SSL_CIPHER_MODE_CBC},
        {12,KEX_DH,SIG_DSS,ENC_DES,8,64,64,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {13,KEX_DH,SIG_DSS,ENC_3DES,8,192,192,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {14,KEX_DH,SIG_RSA,ENC_DES,8,64,40,DIG_SHA,20,1, SSL_CIPHER_MODE_CBC},
        {15,KEX_DH,SIG_RSA,ENC_DES,8,64,64,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {16,KEX_DH,SIG_RSA,ENC_3DES,8,192,192,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {17,KEX_DH,SIG_DSS,ENC_DES,8,64,40,DIG_SHA,20,1, SSL_CIPHER_MODE_CBC},
        {18,KEX_DH,SIG_DSS,ENC_DES,8,64,64,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {19,KEX_DH,SIG_DSS,ENC_3DES,8,192,192,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {20,KEX_DH,SIG_RSA,ENC_DES,8,64,40,DIG_SHA,20,1, SSL_CIPHER_MODE_CBC},
        {21,KEX_DH,SIG_RSA,ENC_DES,8,64,64,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {22,KEX_DH,SIG_RSA,ENC_3DES,8,192,192,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {23,KEX_DH,SIG_NONE,ENC_RC4,1,128,40,DIG_MD5,16,1, SSL_CIPHER_MODE_STREAM},
        {24,KEX_DH,SIG_NONE,ENC_RC4,1,128,128,DIG_MD5,16,0, SSL_CIPHER_MODE_STREAM},
        {25,KEX_DH,SIG_NONE,ENC_DES,8,64,40,DIG_MD5,16,1, SSL_CIPHER_MODE_CBC},
        {26,KEX_DH,SIG_NONE,ENC_DES,8,64,64,DIG_MD5,16,0, SSL_CIPHER_MODE_CBC},
        {27,KEX_DH,SIG_NONE,ENC_3DES,8,192,192,DIG_MD5,16,0, SSL_CIPHER_MODE_CBC},
        {47,KEX_RSA,SIG_RSA,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {48,KEX_DH,SIG_DSS,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_DSS_WITH_AES_128_CBC_SHA *
        {49,KEX_DH,SIG_RSA,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_RSA_WITH_AES_128_CBC_SHA *
        {50,KEX_DH,SIG_DSS,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DHE_DSS_WITH_AES_128_CBC_SHA *
        {51,KEX_DH, SIG_RSA,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {52,KEX_DH,SIG_NONE,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_anon_WITH_AES_128_CBC_SHA *
        {53,KEX_RSA,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {54,KEX_DH,SIG_DSS,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_DSS_WITH_AES_256_CBC_SHA *
        {55,KEX_DH,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_RSA_WITH_AES_256_CBC_SHA *
        {56,KEX_DH,SIG_DSS,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DHE_DSS_WITH_AES_256_CBC_SHA *
        {57,KEX_DH,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},   * TLS_DHE_RSA_WITH_AES_256_CBC_SHA *
        {58,KEX_DH,SIG_NONE,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},  * TLS_DH_anon_WITH_AES_256_CBC_SHA *
        {59,KEX_RSA,SIG_RSA,ENC_NULL,1,0,0,DIG_SHA256,32,0, SSL_CIPHER_MODE_STREAM},
        {60,KEX_RSA,SIG_RSA,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},
        {61,KEX_RSA,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},
        {62,KEX_DH,SIG_DSS,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_DSS_WITH_AES_128_CBC_SHA256 *
        {63,KEX_DH,SIG_RSA,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_RSA_WITH_AES_128_CBC_SHA256 *
        {64,KEX_DH,SIG_DSS,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DHE_DSS_WITH_AES_128_CBC_SHA256 *
        {96,KEX_RSA,SIG_RSA,ENC_RC4,1,128,56,DIG_MD5,16,1, SSL_CIPHER_MODE_STREAM},
        {97,KEX_RSA,SIG_RSA,ENC_RC2,1,128,56,DIG_MD5,16,1, SSL_CIPHER_MODE_STREAM},
        {98,KEX_RSA,SIG_RSA,ENC_DES,8,64,64,DIG_SHA,20,1, SSL_CIPHER_MODE_STREAM},
        {99,KEX_DH,SIG_DSS,ENC_DES,8,64,64,DIG_SHA,16,1, SSL_CIPHER_MODE_CBC},
        {100,KEX_RSA,SIG_RSA,ENC_RC4,1,128,56,DIG_SHA,20,1, SSL_CIPHER_MODE_STREAM},
        {101,KEX_DH,SIG_DSS,ENC_RC4,1,128,56,DIG_SHA,20,1, SSL_CIPHER_MODE_STREAM},
        {102,KEX_DH,SIG_DSS,ENC_RC4,1,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_STREAM},
        {103,KEX_DH,SIG_RSA,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 *
        {104,KEX_DH,SIG_DSS,ENC_AES256,16,256,256,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_DSS_WITH_AES_256_CBC_SHA256 *
        {105,KEX_DH,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_RSA_WITH_AES_256_CBC_SHA256 *
        {106,KEX_DH,SIG_DSS,ENC_AES256,16,256,256,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 *
        {107,KEX_DH,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},
        {108,KEX_DH,SIG_NONE,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_anon_WITH_AES_128_CBC_SHA256 *
        {109,KEX_DH,SIG_NONE,ENC_AES256,16,256,256,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_DH_anon_WITH_AES_256_CBC_SHA256 *
        *{138,KEX_PSK,SIG_RSA,ENC_RC4,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},*
        {139,KEX_PSK,SIG_RSA,ENC_3DES,8,192,192,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {140,KEX_PSK,SIG_RSA,ENC_AES,16,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {141,KEX_PSK,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA,20,0, SSL_CIPHER_MODE_CBC},
        {49169,KEX_DH,SIG_RSA,ENC_RC4,1,128,128,DIG_SHA,20,0, SSL_CIPHER_MODE_STREAM},    * TLS_ECDHE_RSA_WITH_RC4_128_SHA *
        {49187,KEX_DH,SIG_DSS,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 *
        {49188,KEX_DH,SIG_DSS,ENC_AES256,16,256,256,DIG_SHA384,48,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 *
        {49189,KEX_DH,SIG_DSS,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256 *
        {49190,KEX_DH,SIG_DSS,ENC_AES256,16,256,256,DIG_SHA384,48,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384 *
        {49191,KEX_DH,SIG_RSA,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 *
        {49192,KEX_DH,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA384,48,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 *
        {49193,KEX_DH,SIG_RSA,ENC_AES,16,128,128,DIG_SHA256,32,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256 *
        {49194,KEX_DH,SIG_RSA,ENC_AES256,16,256,256,DIG_SHA384,48,0, SSL_CIPHER_MODE_CBC},   * TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384 *
        {-1, 0,0,0,0,0,0,0,0,0, 0}
    };
     */
    
    private static String[] wiresharkSupportedCiphers = new String[]
    {
        "TLS_RSA_WITH_NULL_MD5",
        "TLS_RSA_WITH_NULL_SHA",
        "TLS_RSA_EXPORT_WITH_RC4_40_MD5",
        "TLS_RSA_WITH_RC4_128_MD5",
        "TLS_RSA_WITH_RC4_128_SHA",
        "TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
        "TLS_RSA_WITH_IDEA_CBC_SHA",
        "TLS_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_RSA_WITH_DES_CBC_SHA",
        "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
        //"TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA",
        //"TLS_DH_DSS_WITH_DES_CBC_SHA",
        //"TLS_DH_DSS_WITH_3DES_EDE_CBC_SHA",
        //"TLS_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",
        //"TLS_DH_RSA_WITH_DES_CBC_SHA",
        //"TLS_DH_RSA_WITH_3DES_EDE_CBC_SHA",
        //"TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
        //"TLS_DHE_DSS_WITH_DES_CBC_SHA",
        //"TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
        //"TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        //"TLS_DHE_RSA_WITH_DES_CBC_SHA",
        //"TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
        //"TLS_DH_anon_EXPORT_WITH_RC4_40_MD5",
        //"TLS_DH_anon_WITH_RC4_128_MD5",
        //"TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
        //"TLS_DH_anon_WITH_DES_CBC_SHA",
        //"TLS_DH_anon_WITH_3DES_EDE_CBC_SHA", // 1-27
        "TLS_RSA_WITH_AES_128_CBC_SHA", // 47
        //"TLS_DH_DSS_WITH_AES_128_CBC_SHA", // 48
        //"TLS_DH_RSA_WITH_AES_128_CBC_SHA", // 49
        //"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", // 50
        //"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", //51
        //"TLS_DH_anon_WITH_AES_128_CBC_SHA", // 52
        "TLS_RSA_WITH_AES_256_CBC_SHA", // 53
        //"TLS_DH_DSS_WITH_AES_256_CBC_SHA", // 54
        //"TLS_DH_RSA_WITH_AES_256_CBC_SHA", // 55
        //"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",  // 56
        //"TLS_DHE_RSA_WITH_AES_256_CBC_SHA", // 57
        //"TLS_DH_anon_WITH_AES_256_CBC_SHA", // 58
        "TLS_RSA_WITH_NULL_SHA256", // 59
        "TLS_RSA_WITH_AES_128_CBC_SHA256", // 60
        "TLS_RSA_WITH_AES_256_CBC_SHA256", // 61
        //"TLS_DH_DSS_WITH_AES_128_CBC_SHA256", // 62
        //"TLS_DH_RSA_WITH_AES_128_CBC_SHA256", // 63
        //"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", // 64
        "TLS_RSA_EXPORT1024_WITH_DES_CBC_SHA", //98
        //"TLS_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA", //99
        //"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", // 103
        //"TLS_DH_DSS_WITH_AES_256_CBC_SHA256", // 104
        //"TLS_DH_RSA_WITH_AES_256_CBC_SHA256", // 105
        //"TLS_DHE_DSS_WITH_AES_256_CBC_SHA256", // 106
        //"TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", // 107
        //"TLS_DH_anon_WITH_AES_128_CBC_SHA256", // 108
        //"TLS_DH_anon_WITH_AES_256_CBC_SHA256", // 109
        // not working errors with : ssl_generate_keyring_material not enough data to generate key
        // "TLS_ECDHE_RSA_WITH_RC4_128_SHA", //49169
        // "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", //49187
        // "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", // 49188
        // "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", // 49189
        // "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384", //49190
        // "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", //49191
        // "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", //49192
        // "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", //49193
        // "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384", //49194
    };
    
    private static List<String> listWiresharkSupportedCiphers = Arrays.asList(wiresharkSupportedCiphers);
    private static String[] selectedCiphers = null;
    
    private String[] selectCiphers(String[] supportedCiphers){
        if (selectedCiphers == null){
            List<String> listSelectedCiphers = new ArrayList<String>();
            for (String supportedCipher : supportedCiphers) {
                if (listWiresharkSupportedCiphers.contains(supportedCipher)){
                    _logger.info("Cipher added to list " + supportedCipher);
                    listSelectedCiphers.add(supportedCipher);
                }else{
                    _logger.info("!!! Cipher removed from list " + supportedCipher);
                }
            }
            Collections.reverse(listSelectedCiphers);
            selectedCiphers = new String[listSelectedCiphers.size()];
            for (int i = 0; i < selectedCiphers.length; i++) {
                String selectedCipher = listSelectedCiphers.get(i);
                _logger.info("adde cipher to pos " + i + " : " + selectedCipher);
                selectedCiphers[i] = selectedCipher;
            }
            return selectedCiphers;
        }else{
            return selectedCiphers;
        }
    }

    private Socket negotiateSSL(Socket sock, SiteData hostData, boolean useOnlyWiresharkDissCiphers) throws Exception {
        SSLSocketFactory factory = _proxy.getSocketFactory(hostData);
        if (factory == null)
            throw new RuntimeException(
                    "SSL Intercept not available - no keystores available");
        SSLSocket sslsock;
        try {
            int sockPort = sock.getPort();
            String hostName = hostData.tcpAddress != null ? hostData.tcpAddress : hostData.name;
            sslsock = (SSLSocket) factory.createSocket(sock, hostName, sockPort, false);
            if (useOnlyWiresharkDissCiphers){
                // force chiper that can be decrypted with wireshark
                String[] ciphers = selectCiphers(sslsock.getSupportedCipherSuites());
                sslsock.setEnabledCipherSuites(ciphers);
            }
            sslsock.setUseClientMode(false);
            _logger.info("Finished negotiating client SSL - algorithm is "
                    + sslsock.getSession().getCipherSuite());
            return sslsock;
        } catch (Exception e) {
            _logger.severe("Error layering SSL over the socket: " + e);
            throw e;
        }
    }

    private Response errorResponse(Request request, Exception e) {
        Response response = new Response();
        response.setRequest(request);
        response.setVersion("HTTP/1.0");
        response.setStatus("500");
        response.setMessage("SandroProxy error");
        response.setHeader("Content-Type", "text/html");
        response.setHeader("Connection", "Close");
        String template = "<HTML><HEAD><TITLE>SandroProxy Error</TITLE></HEAD>";
        template = template
                + "<BODY>SandroProxy encountered an error trying to retrieve <P><pre>"
                + HtmlEncoder.encode(request.toString()) + "</pre><P>";
        template = template + "The error was : <P><pre>"
                + HtmlEncoder.encode(e.getLocalizedMessage()) + "\n";
        StackTraceElement[] trace = e.getStackTrace();
        if (trace != null) {
            for (int i = 0; i < trace.length; i++) {
                template = template + "\tat " + trace[i].getClassName() + "."
                        + trace[i].getMethodName() + "(";
                if (trace[i].getLineNumber() == -2) {
                    template = template + "Native Method";
                } else if (trace[i].getLineNumber() == -1) {
                    template = template + "Unknown Source";
                } else {
                    template = template + trace[i].getFileName() + ":"
                            + trace[i].getLineNumber();
                }
                template = template + ")\n";
            }
        }
        template = template + "</pre><P></HTML>";
        response.setContent(template.getBytes());
        return response;
    }

}
