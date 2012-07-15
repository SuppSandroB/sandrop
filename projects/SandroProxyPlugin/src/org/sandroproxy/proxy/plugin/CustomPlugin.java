
package org.sandroproxy.proxy.plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.plugin.proxy.ProxyPlugin;

import android.util.Log;

public class CustomPlugin extends ProxyPlugin {
    
    private static boolean LOGD = true;
    private static String TAG = CustomPlugin.class.getName();
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
                // adding some header to request
                String setCookieHeader = "Set-Cookie";
                request.addHeader("X-SandroProxyPlugin", "0.9.25");
                // sending request to server to get response
                Response response = _in.fetchResponse(request);
                
                // do we have any new cookies 
                List<String> headerNames = Arrays.asList(response.getHeaderNames());
                if (headerNames.contains(setCookieHeader)){
                    String newCookies = response.getHeader(setCookieHeader);
                    // log new cookie header value
                    if (LOGD) Log.d(TAG, "New cookies from server: " + newCookies);
                }
                
                // adding some header to response
                response.addHeader("X-SandroProxyVer", "0.9.25");
                // return changed response
                return response;
            }
            // just make normal action whitout any custom parsing
            return _in.fetchResponse(request);
        }
        
    }
    
}
