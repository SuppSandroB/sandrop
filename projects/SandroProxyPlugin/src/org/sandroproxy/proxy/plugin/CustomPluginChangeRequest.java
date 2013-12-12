
package org.sandroproxy.proxy.plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.plugin.proxy.ProxyPlugin;

import android.util.Log;

public class CustomPluginChangeRequest extends ProxyPlugin {
    
    private static boolean LOGD = false;
    private static String TAG = CustomPluginChangeRequest.class.getName();
    private boolean _enabled = true;
    
    public CustomPluginChangeRequest() {
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
                String method = request.getMethod();
                HttpUrl host = request.getURL();
                String methodGET = "GET";
                String hostName = "www.google.com";
                String secureSchema = "https";
                Response response;
                if (host != null && host.getHost().equalsIgnoreCase(hostName) && 
                        host.getScheme().equalsIgnoreCase(secureSchema) && 
                        method.equals(methodGET)){
                    
                    request.flushContentStream();
                    byte[] content = request.getContent();
                    request.clean();
                    Request newRequest = new Request();
                    HttpUrl newUrl = new HttpUrl("http://www.google.com");
                    newRequest.setMethod(methodGET);
                    newRequest.setURL(newUrl);
                    newRequest.setContent(content);
                    response = _in.fetchResponse(newRequest);
                }else{
                    // just make normal action whitout any custom parsing
                    response = _in.fetchResponse(request);
                }
                return response;
            }
            // just make normal action whitout any custom parsing
            return _in.fetchResponse(request);
        }
        
    }
    
}
