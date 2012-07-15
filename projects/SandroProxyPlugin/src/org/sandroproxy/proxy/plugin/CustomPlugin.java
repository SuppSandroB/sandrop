
package org.sandroproxy.proxy.plugin;

import java.io.IOException;
import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.plugin.proxy.ProxyPlugin;

public class CustomPlugin extends ProxyPlugin {
    
    private boolean _enabled = true;
    
    public CustomPlugin() {
    }
    
    public void parseProperties() {
    }
    
    public String getPluginName() {
        return new String("Custom Plugin");
    }
    
    public void setEnabled(boolean bool) {
        _enabled = bool;
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
