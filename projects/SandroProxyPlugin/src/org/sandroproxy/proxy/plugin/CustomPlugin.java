
package org.sandroproxy.proxy.plugin;

import java.io.IOException;
import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.model.Preferences;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.plugin.proxy.ProxyPlugin;
import org.sandroproxy.utils.PreferenceUtils;

public class CustomPlugin extends ProxyPlugin {
    
    private boolean _enabled = false;
    
    public CustomPlugin() {
        parseProperties();
    }
    
    public void parseProperties() {
        String prop = PreferenceUtils.proxyCustomPluginKey;
        _enabled = Preferences.getPreferenceBoolean(prop, false);
    }
    
    public String getPluginName() {
        return new String("Custom Plugin");
    }
    
    public void setEnabled(boolean bool) {
        _enabled = bool;
        String prop = PreferenceUtils.proxyCustomPluginKey;
        Preferences.setPreference(prop,Boolean.toString(bool));
    }

    public boolean getEnabled() {
        return _enabled;
    }
    
    public HTTPClient getProxyPlugin(HTTPClient in) {
        return new Plugin(in);
    }    
    
    private class Plugin implements HTTPClient {
    
        private HTTPClient _in;
        
        public Plugin(HTTPClient in) {
            _in = in;
        }
        
        public Response fetchResponse(Request request) throws IOException {
            if (_enabled) {
                // just adding some header to request
                request.addHeader("CustomPluginHeader", "CustomPluginValue");
            }
            return _in.fetchResponse(request);
        }
        
    }
    
}
