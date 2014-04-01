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
import java.net.Socket;
import java.util.Date;

import org.sandrop.webscarab.httpclient.HTTPClient;
import org.sandrop.webscarab.model.Cookie;
import org.sandrop.webscarab.model.FrameworkModel;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.NamedValue;
import org.sandrop.webscarab.model.Preferences;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.plugin.Framework;

public class CookieTracker extends ProxyPlugin {
    
    private FrameworkModel _model = null;
    
    private boolean _injectRequests = false;
    private boolean _readResponses = false;
    
    /** Creates a new instance of CookieTracker */
    public CookieTracker(Framework framework) {
        _model = framework.getModel();
        parseProperties();
    }
    
    public void parseProperties() {
        String prop = "CookieTracker.injectRequests";
        String value = Preferences.getPreference(prop, "false");
        _injectRequests = ("true".equalsIgnoreCase( value ) || "yes".equalsIgnoreCase( value ));
        prop = "CookieTracker.readResponses";
        value = Preferences.getPreference(prop, "true");
        _readResponses = ("true".equalsIgnoreCase( value ) || "yes".equalsIgnoreCase( value ));
    }
    
    public String getPluginName() {
        return new String("Cookie Tracker");
    }
    
    public void setInjectRequests(boolean bool) {
        _injectRequests = bool;
        String prop = "CookieTracker.injectRequests";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }

    public boolean getInjectRequests() {
        return _injectRequests;
    }
    
    public void setReadResponses(boolean bool) {
        _readResponses = bool;
        String prop = "CookieTracker.readResponses";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }

    public boolean getReadResponses() {
        return _readResponses;
    }
    
    public HTTPClient getProxyPlugin(HTTPClient in) {
        return new Plugin(in);
    }    
    
    private class Plugin implements HTTPClient {
    
        private HTTPClient _in;
        
        public Plugin(HTTPClient in) {
            _in = in;
        }
        
        public Socket getConnectedSocket(HttpUrl url, boolean makeHandshake, Request request) throws IOException{
            return _in.getConnectedSocket(url, makeHandshake, request);
        }
        
        public Response fetchResponse(Request request) throws IOException {
            if (_injectRequests) {
                // FIXME we should do something about any existing cookies that are in the Request
                // they could have been set via JavaScript, or some such!
                Cookie[] cookies = _model.getCookiesForUrl(request.getURL());
                if (cookies.length>0) {
                    StringBuffer buff = new StringBuffer();
                    buff.append(cookies[0].getName()).append("=").append(cookies[0].getValue());
                    for (int i=1; i<cookies.length; i++) {
                        buff.append("; ").append(cookies[i].getName()).append("=").append(cookies[i].getValue());
                    }
                    request.setHeader("Cookie", buff.toString());
                }
            }
            Response response = _in.fetchResponse(request);
            if (_readResponses && response != null) {
                NamedValue[] headers = response.getHeaders();
                for (int i=0; i<headers.length; i++) {
                    if (headers[i].getName().equalsIgnoreCase("Set-Cookie") || headers[i].getName().equalsIgnoreCase("Set-Cookie2")) {
                        Cookie cookie = new Cookie(new Date(), request.getURL(), headers[i].getValue());
                        _model.addCookie(cookie);
                    }
                }
            }
            return response;
        }
        
    }

    @Override
    public boolean getEnabled() {
        // TODO Auto-generated method stub
        return _injectRequests || _readResponses;
    }
    
}
