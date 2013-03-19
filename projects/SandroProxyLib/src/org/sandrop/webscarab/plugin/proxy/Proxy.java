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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.NumberFormatException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;



import org.sandrop.R;
import org.sandrop.webscarab.model.ConversationID;
import org.sandrop.webscarab.model.FrameworkModel;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Preferences;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.model.StoreException;
import org.sandrop.webscarab.plugin.Framework;
import org.sandrop.webscarab.plugin.Hook;
import org.sandrop.webscarab.plugin.Plugin;
import org.sandrop.websockets.ExtensionWebSocket;
import org.sandroproxy.constants.Constants;
import org.sandroproxy.utils.PreferenceUtils;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;

import android.database.sqlite.SQLiteStatement;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * The Proxy plugin supports multiple Listeners, and starts and stops them as
 * instructed. All requests and responses are submitted to the model, unless
 * there is an error while retrieving the response.
 */
public class Proxy implements Plugin {

    private boolean _running = false;

    private Framework _framework = null;
    private ITransparentProxyResolver _transparentProxyResolver;

    private ProxyUI _ui = null;

    private ArrayList<ProxyPlugin> _plugins = new ArrayList<ProxyPlugin>();
    private TreeMap<ListenerSpec, Listener> _listeners = new TreeMap<ListenerSpec, Listener>();

    private Logger _logger = Logger.getLogger(getClass().getName());

    private String _status = "Stopped";
    private int _pending = 0;

    private static HashMap<String, SSLSocketFactory> _factoryMap = new HashMap<String, SSLSocketFactory>();

    private static char[] _keystorepass = "password".toCharArray();
    private static char[] _keypassword = "password".toCharArray();
    private SSLSocketFactoryFactory _certGenerator = null;
    private static String _certDir = "./certs/";

    private Proxy.ConnectionHook _allowConnection = new ConnectionHook(
            "Allow connection",
            "Called when a new connection is received from a browser\n"
                    + "use connection.getAddress() and connection.closeConnection() to decide and react");

    private Proxy.ConnectionHook _interceptRequest = new ConnectionHook(
            "Intercept request",
            "Called when a new request has been submitted by the browser\n"
                    + "use connection.getRequest() and connection.setRequest(request) to perform changes");

    private Proxy.ConnectionHook _interceptResponse = new ConnectionHook(
            "Intercept response",
            "Called when the request has been submitted to the server, and the response "
                    + "has been recieved.\n"
                    + "use connection.getResponse() and connection.setResponse(response) to perform changes");
    
    private static ExtensionWebSocket _webSocketManager;

    /**
     * Creates a Proxy Object with a reference to the Framework. Creates (but
     * does not start) the configured Listeners.
     * 
     * @param model
     *            The Model to submit requests and responses to
     */
    public Proxy(Framework framework, ITransparentProxyResolver transparentProxyResolver) {
        _logger.setLevel(Level.FINEST);
        _framework = framework;
        _transparentProxyResolver = transparentProxyResolver;
        parseListenerConfig();
        _certGenerator = null;
        try {
            File dataStorageDir = PreferenceUtils.getDataStorageDir(_framework.getAndroidContext());
            
            if (dataStorageDir == null){
                String errorText = "ERROR \nNo external storage available...";
                _logger.fine(errorText);
                _logger.fine("Make external sdcard available to android");
                return;
            }
            _logger.fine("Using " + dataStorageDir.getAbsolutePath() + " for data storage");
            String keystoreCAFullPath = PreferenceUtils.getCAFilePath(_framework.getAndroidContext());
            String keystoreCertFullPath = PreferenceUtils.getCertFilePath(_framework.getAndroidContext());
            String caPassword = PreferenceUtils.getCAFilePassword(_framework.getAndroidContext());
            String keyStoreType = "PKCS12";
            if (keystoreCAFullPath != null && keystoreCAFullPath.length() > 0 && 
                keystoreCertFullPath != null && keystoreCertFullPath.length() > 0){
                
                try{
                    _certGenerator = new SSLSocketFactoryFactory(keystoreCAFullPath, keystoreCertFullPath, keyStoreType, caPassword.toCharArray());
                    _certGenerator.setReuseKeys(true);
                    _logger.fine("Using CA from file: " + keystoreCAFullPath);
                }catch(Exception ex){
                    _logger.fine("Error getting custom CA certificate:" + ex.getMessage());
                }
            }else{
                _logger.fine("Error getting custom CA certificate: Invalid file path");
            }
            
            _webSocketManager = new ExtensionWebSocket(SqlLiteStore.getInstance(_framework.getAndroidContext(), dataStorageDir.getAbsolutePath()));
        } catch (NoClassDefFoundError e) {
            _certGenerator = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Hook[] getScriptingHooks() {
        return new Hook[] { _allowConnection, _interceptRequest,
                _interceptResponse };
    }
    
    public ExtensionWebSocket getWebSocketManager(){
        return _webSocketManager;
    }

    public Object getScriptableObject() {
        return null;
    }
    
    public ITransparentProxyResolver getTransparentProxyResolver(){
        return _transparentProxyResolver;
    }

    /**
     * called by Listener to determine whether to allow a connection or not
     */
    void allowClientConnection(ScriptableConnection connection) {
        _allowConnection.runScripts(connection);
    }

    /**
     * called by Connectionhandler via Listener to perform any required
     * modifications to the Request
     */
    void interceptRequest(ScriptableConnection connection) {
        _interceptRequest.runScripts(connection);
    }

    /**
     * called by Connectionhandler via Listener to perform any required
     * modifications to the Response
     */
    void interceptResponse(ScriptableConnection connection) {
        _interceptResponse.runScripts(connection);
    }

    public void setUI(ProxyUI ui) {
        _ui = ui;
        if (_ui != null)
            _ui.setEnabled(_running);
    }

    public void addPlugin(ProxyPlugin plugin) {
        _plugins.add(plugin);
    }

    /**
     * retrieves the named plugin, if it exists
     * 
     * @param name
     *            the name of the plugin
     * @return the plugin if it exists, or null
     */
    public ProxyPlugin getPlugin(String name) {
        ProxyPlugin plugin = null;
        Iterator<ProxyPlugin> it = _plugins.iterator();
        while (it.hasNext()) {
            plugin = it.next();
            if (plugin.getPluginName().equals(name))
                return plugin;
        }
        return null;
    }

    /**
     * The plugin name
     * 
     * @return The name of the plugin
     * 
     */
    public String getPluginName() {
        return new String("Proxy");
    }

    /**
     * returns a list of keys describing the configured Listeners
     * 
     * @return the list of keys
     */
    public ListenerSpec[] getProxies() {
        if (_listeners.size() == 0) {
            return new ListenerSpec[0];
        }
        return (ListenerSpec[]) _listeners.keySet()
                .toArray(new ListenerSpec[0]);
    }

    /**
     * called by ConnectionHandler to see which plugins have been configured.
     * 
     * @return an array of ProxyPlugin's
     */
    protected ProxyPlugin[] getPlugins() {
        ProxyPlugin[] plugins = new ProxyPlugin[_plugins.size()];
        for (int i = 0; i < _plugins.size(); i++) {
            plugins[i] = _plugins.get(i);
        }
        return plugins;
    }

    /**
     * used by the User Interface to start a new proxy listening with the
     * specified parameters
     * 
     * @param spec
     *            the details of the Listener
     * @throws IOException
     *             if there are any problems starting the Listener
     */

    public void addListener(ListenerSpec spec) {
        createListener(spec);
        startListener(_listeners.get(spec));

        String key = getKey(spec);
        Preferences.setPreference("Proxy.listener." + key + ".base", spec
                .getBase() == null ? "" : spec.getBase().toString());
        Preferences.setPreference("Proxy.listener." + key + ".primary", spec
                .isPrimaryProxy() == true ? "yes" : "no");

        String value = null;
        Iterator<ListenerSpec> i = _listeners.keySet().iterator();
        while (i.hasNext()) {
            key = getKey(i.next());
            if (value == null) {
                value = key;
            } else {
                value = value + ", " + key;
            }
        }
        Preferences.setPreference("Proxy.listeners", value);
    }

    private String getKey(ListenerSpec spec) {
        return spec.getAddress() + ":" + spec.getPort();
    }

    private void startListener(Listener l) {
        Thread t = new Thread(l, "Listener-" + getKey(l.getListenerSpec()));
        t.setDaemon(true);
        t.start();
        if (_ui != null)
            _ui.proxyStarted(l.getListenerSpec());
    }

    private boolean stopListener(Listener l) {
        boolean stopped = l.stop();
        if (stopped && _ui != null)
            _ui.proxyStopped(l.getListenerSpec());
        return stopped;
    }

    /**
     * Used to stop the referenced listener
     * 
     * @param key
     *            the Listener to stop
     * @return true if the proxy was successfully stopped, false otherwise
     */
    public boolean removeListener(ListenerSpec spec) {
        Listener l = _listeners.get(spec);
        if (l == null)
            return false;
        if (stopListener(l)) {
            _listeners.remove(spec);
            if (_ui != null)
                _ui.proxyRemoved(spec);
            String key = getKey(spec);
            Preferences.remove("Proxy.listener." + key + ".base");
            Preferences.remove("Proxy.listener." + key + ".simulator");
            Preferences.remove("Proxy.listener." + key + ".primary");
            String value = null;
            Iterator<ListenerSpec> i = _listeners.keySet().iterator();
            while (i.hasNext()) {
                key = getKey(i.next());
                if (value == null) {
                    value = key;
                } else {
                    value = value + ", " + key;
                }
            }
            if (value == null) {
                value = "";
            }
            Preferences.setPreference("Proxy.listeners", value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Starts the Listeners
     */
    public void run() {
        Iterator<ListenerSpec> it = _listeners.keySet().iterator();
        while (it.hasNext()) {
            ListenerSpec spec = it.next();
            try {
                spec.verifyAvailable();
                Listener l = _listeners.get(spec);
                if (l == null) {
                    createListener(spec);
                    l = _listeners.get(spec);
                }
                startListener(l);
            } catch (IOException ioe) {
                _logger.warning("Unable to start listener " + spec);
                if (_ui != null)
                    _ui.proxyStartError(spec, ioe);
                removeListener(spec);
            }
        }
        _running = true;
        if (_ui != null)
            _ui.setEnabled(_running);
        _status = "Started, Idle";
    }

    /**
     * Stops the Listeners
     * 
     * @return true if successful, false otherwise
     */
    public boolean stop() {
        _running = false;
        Iterator<ListenerSpec> it = _listeners.keySet().iterator();
        while (it.hasNext()) {
            ListenerSpec spec = it.next();
            Listener l = _listeners.get(spec);
            if (l != null && !stopListener(l)) {
                _logger
                        .severe("Failed to stop Listener-"
                                + l.getListenerSpec());
                _running = true;
            }
        }
        if (_ui != null)
            _ui.setEnabled(_running);
        _status = "Stopped";
        _webSocketManager.unload();
        return !_running;
    }

    /**
     * used by ConnectionHandler to notify the Proxy (and any listeners) that it
     * is handling a particular request
     * 
     * @param request
     *            the request to log
     * @return the conversation ID
     */
    protected long gotRequest(Request request, String from) {
        long id = _framework.createConversation(request, new Date(System.currentTimeMillis()), FrameworkModel.CONVERSATION_TYPE_PROXY , from);
        _framework.gotRequest(id, new Date(System.currentTimeMillis()), request);
        return id;
    }

    /**
     * used by ConnectionHandler to notify the Proxy (and any listeners) that it
     * has handled a particular request and response, and that it should be
     * logged and analysed
     * 
     * @param id
     *            the Conversation ID
     * @param response
     *            the Response
     */
    protected void gotResponse(long id, Request request, Response response, boolean dataModified) {
        _framework.gotResponse(id, new Date(System.currentTimeMillis()), request, response, dataModified);
        _framework.cleanConversation(request, response);
    }

    protected SSLSocketFactory getSocketFactory(String host) {
        synchronized (_factoryMap) {
            // If it has been loaded already, use it
            if (_factoryMap.containsKey(host))
                return (SSLSocketFactory) _factoryMap.get(host);
            SSLSocketFactory factory;
            // Check if there is a specific keypair to use
            File p12 = new File(_certDir + host + ".p12");
            factory = loadSocketFactory(p12, host);
            if (factory != null) {
                _factoryMap.put(host, factory);
                return factory;
            }
            // See if we can generate one directly
            factory = generateSocketFactory(host);
            if (factory != null) {
                _factoryMap.put(host, factory);
                return factory;
            }
            // Has the default keypair been loaded already?
            if (_factoryMap.containsKey(null)) {
                _logger.info("Using default SSL keystore for " + host);
                return (SSLSocketFactory) _factoryMap.get(null);
            }
            // Check for a user-provided "default keypair"
            p12 = new File(_certDir + "server.p12");
            factory = loadSocketFactory(p12, host);
            if (factory != null) {
                _factoryMap.put(null, factory);
                return factory;
            }
            // Fall back to the distribution-provided keypair
            _logger.info("Loading default SSL keystore from internal resource");
            InputStream is = _framework.getAndroidContext().getResources()
                                            .openRawResource(R.raw.server_p12);
            // sandrob fix this
            //InputStream is = getClass().getClassLoader().getResourceAsStream(
            //        "server.p12");
            if (is == null) {
                _logger
                        .severe("WebScarab JAR was built without a certificate!");
                _logger.severe("SSL Intercept not available!");
                return null;
            }
            factory = loadSocketFactory(is, "WebScarab JAR");
            _factoryMap.put(null, factory);
            return factory;
        }
    }

    private SSLSocketFactory loadSocketFactory(File p12, String host) {
        if (p12.exists() && p12.canRead()) {
            _logger.info("Loading SSL keystore for " + host + " from " + p12);
            try {
                InputStream is = new FileInputStream(p12);
                return loadSocketFactory(is, p12.getPath());
            } catch (IOException ioe) {
                _logger.severe("Error reading from " + p12 + ": "
                        + ioe.getLocalizedMessage());
            }
        }
        return null;
    }

    private SSLSocketFactory loadSocketFactory(InputStream is, String source) {
        try {
            KeyManagerFactory kmf = null;
            SSLContext sslcontext = null;
            KeyStore ks = null;
            ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(is, _keystorepass);
            kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(ks, _keypassword);
            //sslcontext = SSLContext.getInstance("SSLv3");
            sslcontext = SSLContext.getInstance("TLS");
            
            // here, trust managers is a single trust-all manager
            TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                        _logger.fine("trust manager checkClientTrusted authType:" + authType);
                        if (certs != null){
                            for (int i = 0; i < certs.length; i++) {
                                _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                            }
                        }
                    }

                    public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                        _logger.fine("trust manager checkClientTrusted authType:" + authType);
                        if (certs != null){
                            for (int i = 0; i < certs.length; i++) {
                                _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                            }
                        }
                    }
                }
            };
            
            
            sslcontext.init(kmf.getKeyManagers(), trustManagers, null);
            return sslcontext.getSocketFactory();

            /*
            String keyStoreType = "PKCS12";
            String keyStoreProvider = "BC";
            SSLClientSessionCache cache = null;
            
            ks = KeyStore.getInstance(keyStoreType, keyStoreProvider);
            ks.load(is, _keystorepass);
            
            String kmfa = KeyManagerFactory.getDefaultAlgorithm();
            kmf = KeyManagerFactory.getInstance(kmfa);
            kmf.init(ks, _keypassword);
            
            SSLContextImpl sslContext = new SSLContextImpl();
            // sslcontext = SSLContext.getInstance("TLS");
            
            // here, trust managers is a single trust-all manager
            TrustManager[] tm = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            sslContext.engineInit(kmf.getKeyManagers(), tm, null, cache, null);
            return sslContext.engineGetSocketFactory();
            */
            
            // sslcontext = SSLContext.getInstance("TLS");
            // sslcontext.init(kmf.getKeyManagers(), tm, null);
            // return sslcontext.getSocketFactory();
            
            
            
        } catch (IOException ioe) {
            _logger.info("Error reading SSL keystore from " + source + ": "
                    + ioe.getLocalizedMessage());
        } catch (GeneralSecurityException gse) {
            _logger.info("Error reading SSL keystore from " + source + ": "
                    + gse.getLocalizedMessage());
        }
        return null;
    }

    private SSLSocketFactory generateSocketFactory(String host) {
        if (_certGenerator == null)
            return null;
        try {
            _logger.info("Generating custom SSL keystore for " + host);
            return _certGenerator.getSocketFactory(host);
        } catch (IOException ioe) {
            _logger.info("Error generating custom SSL keystore for " + host
                    + ": " + ioe);
        } catch (GeneralSecurityException gse) {
            _logger.info("Error generating custom SSL keystore for " + host
                    + ": " + gse);
        }
        return null;
    }

    /**
     * notifies any observers that the request failed to complete, and the
     * reason for it
     * 
     * @param reason
     *            the reason for failure
     * @param id
     *            the conversation ID
     */
    protected void failedResponse(Request request, Response response, long id, String reason, boolean dataModified) {
        _framework.failedResponse(id, new Date(System.currentTimeMillis()), request, response, reason, dataModified);
        _framework.cleanConversation(request, response);
    }

    private void parseListenerConfig() {
        List<String> listListeners = _framework.getListeners();
        
        String[] listeners = new String[listListeners.size()];
        listListeners.toArray(listeners);

        String addr = "";
        String portAsString = null;
        int port = 0;
        HttpUrl base;
        boolean primary = false;

        for (int i = 0; i < listeners.length; i++) {
            addr = "";
            portAsString = null;
            String[] addrParts = listeners[i].split(":");
            for (int j = 0; j < addrParts.length; j++) {
                if (j < addrParts.length - 1){
                    addr += addrParts[j];
                    if (j < addrParts.length - 2){
                        addr += ":";
                    }
                }else{
                    portAsString = addrParts[j];
                }
            }
            try {
                port = Integer.parseInt(portAsString.trim());
            } catch (NumberFormatException nfe) {
                System.err.println("Error parsing port for " + listeners[i]
                        + ", skipping it!");
                continue;
            }
            /*
            prop = "Proxy.listener." + listeners[i] + ".base";
            value = Preferences.getPreference(prop, "");
            if (value.equals("")) {
                base = null;
            } else {
                try {
                    base = new HttpUrl(value);
                } catch (MalformedURLException mue) {
                    _logger.severe("Malformed 'base' parameter for listener '"
                            + listeners[i] + "'");
                    break;
                }
            }

            prop = "Proxy.listener." + listeners[i] + ".primary";
            value = Preferences.getPreference(prop, "false");
            primary = value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("yes");
            */
            base = null;
            if (!addr.equalsIgnoreCase("") && port != 0){
                boolean captureData = Preferences.getPreferenceBoolean(PreferenceUtils.proxyCaptureData, true);
                _listeners.put(new ListenerSpec(addr, port, base, primary, false, false, captureData), null);
                if (Preferences.getPreferenceBoolean("preference_proxy_transparent", false)){
                    _listeners.put(new ListenerSpec(addr, Constants.TRANSPARENT_PROXY_HTTP, base, primary, true, false, captureData), null);
                    _listeners.put(new ListenerSpec(addr, Constants.TRANSPARENT_PROXY_HTTPS, base, primary, true, true, captureData), null);
                }
            }else{
                _logger.fine("Warrning Skipping " + listeners[i]);
            }
            
        }
    }

    private void createListener(ListenerSpec spec) {
        Listener l = new Listener(this, spec);

        _listeners.put(spec, l);

        if (_ui != null)
            _ui.proxyAdded(spec);
    }

    public void flush() throws StoreException {
        // we do not run our own store, but our plugins might
        Iterator<ProxyPlugin> it = _plugins.iterator();
        while (it.hasNext()) {
            ProxyPlugin plugin = it.next();
            plugin.flush();
        }
    }

    public boolean isBusy() {
        return _pending > 0;
    }

    public String getStatus() {
        return _status;
    }

    public boolean isModified() {
        return false;
    }

    public void analyse(ConversationID id, Request request, Response response,
            String origin) {
        // we do no analysis
    }

    public void setSession(String type, Object store, String session)
            throws StoreException {
        // we have no listeners to remove
        Iterator<ProxyPlugin> it = _plugins.iterator();
        while (it.hasNext()) {
            ProxyPlugin plugin = it.next();
            plugin.setSession(type, store, session);
        }
    }

    public boolean isRunning() {
        return _running;
    }

    private class ConnectionHook extends Hook {

        public ConnectionHook(String name, String description) {
            super(name, description);
        }

        public void runScripts(ScriptableConnection connection) {
            if (_bsfManager == null)
                return;
            synchronized (_bsfManager) {
                try {
                    _bsfManager.declareBean("connection", connection,
                            connection.getClass());
                    super.runScripts();
                    _bsfManager.undeclareBean("connection");
                } catch (Exception e) {
                    _logger
                            .severe("Declaring or undeclaring a bean should not throw an exception! "
                                    + e);
                }
            }
        }

    }

}
