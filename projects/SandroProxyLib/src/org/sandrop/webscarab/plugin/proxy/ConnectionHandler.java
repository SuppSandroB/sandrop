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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.httpclient.HTTPClientFactory;
import org.sandrop.webscarab.model.ConversationID;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.util.HtmlEncoder;
import org.sandroproxy.websockets.ExtensionWebSocket;

public class ConnectionHandler implements Runnable {

    private ProxyPlugin[] _plugins = null;
    private Proxy _proxy;
    private Socket _sock = null;
    private HttpUrl _base;
    private boolean _transparent = false;
    private boolean _transparentSecure = false;
    private ITransparentProxyResolver _transparentResolver = null;

    private HTTPClient _httpClient = null;

    private Logger _logger = Logger.getLogger(getClass().getName());
    
    private InputStream _clientIn = null;
    private OutputStream _clientOut = null;

    public ConnectionHandler(Proxy proxy, Socket sock, HttpUrl base, boolean transparent, boolean transparentSecure, ITransparentProxyResolver transparentProxyResolver) {
        _logger.setLevel(Level.FINEST);
        _proxy = proxy;
        _sock = sock;
        _base = base;
        _transparent = transparent;
        _transparentSecure = transparentSecure;
        _transparentResolver = transparentProxyResolver;
        _plugins = _proxy.getPlugins();
        try {
            _sock.setTcpNoDelay(true);
            _sock.setSoTimeout(30 * 1000);
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
            Request request = null;
            // if we do not already have a base URL (i.e. we operate as a normal
            // proxy rather than a reverse proxy), check for a CONNECT
            if (_base == null && !_transparentSecure){
                try {
                    request = new Request(_transparent, _transparentSecure);
                    request.read(_clientIn);
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
                if (method == null) {
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
                    _logger.fine("Intercepting SSL connection!");
                    String hostName = null;
                    if (_transparentSecure){
                        if (_transparentResolver != null){
                            hostName = _transparentResolver.getSecureHostName();
                        }
                    }else{
                        hostName = _base.getHost();
                    }
                    String host = "sandroproxy.untrusted";
                    if (hostName == null || hostName.trim().length() == 0){
                        hostName = host;
                    }
                    
                    // this will fail on ws:// protocol
                    // but it should work on wss:// 
                    try{
                        _sock = negotiateSSL(_sock, hostName);
                        _clientIn = _sock.getInputStream();
                        _clientOut = _sock.getOutputStream();
                    }catch(Exception ex){
                        ex.printStackTrace();
                        if (request != null){
                            request.setURL(new HttpUrl("http://" + request.getURL().getHost() + "/"));
                            _base = request.getURL();
                        }
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

            // do we add an X-Forwarded-For header?
            String from = _sock.getInetAddress().getHostAddress();
            if (from.equals("127.0.0.1"))
                from = null;

            // do we keep-alive?
            String keepAlive = null;
            String version = null;

            do {
                conversationId = -1;
                // if we are reading the first from a reverse proxy, or the
                // continuation of a CONNECT from a normal proxy
                // read the request, otherwise we already have it.
                if (request == null) {
                    request = new Request(_transparent, _transparentSecure);
                    _logger.fine("Reading request from the browser");
                    request.read(_clientIn, _base);
                    if (request.getMethod() == null || request.getURL() == null) {
                        return;
                    }
                    if (proxyAuth != null) {
                        request.addHeader("Proxy-Authorization", proxyAuth);
                    }
                }
                if (request.getURL() == null){
                    return;
                }
                if (from != null) {
                    request.addHeader("X-Forwarded-For", from);
                }
                
                _logger.fine("Browser requested : " + request.getMethod() + " "+ request.getURL().toString());

                // report the request to the listener, and get the allocated ID
                conversationId = _proxy.gotRequest(request, from);

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
                            _proxy.getWebSocketManager().addWebSocketsChannel(response, _sock, response.getSocket(), response.getSocket().getInputStream());
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

                _logger.fine("Version: " + version + " Connection: "
                        + connection);
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


    private Socket negotiateSSL(Socket sock, String hostName) throws Exception {
        SSLSocketFactory factory = _proxy.getSocketFactory(hostName);
        if (factory == null)
            throw new RuntimeException(
                    "SSL Intercept not available - no keystores available");
        SSLSocket sslsock;
        try {
            int sockPort = sock.getPort();
            sslsock = (SSLSocket) factory.createSocket(sock, hostName, sockPort, false);
            sslsock.setUseClientMode(false);
            _logger.info("Finished negotiating SSL - algorithm is "
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
