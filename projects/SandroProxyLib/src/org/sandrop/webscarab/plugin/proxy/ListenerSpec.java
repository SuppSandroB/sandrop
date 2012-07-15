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
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.sandrop.webscarab.model.HttpUrl;

public class ListenerSpec implements Comparable {

    private String _address;
    private int _port;
    private HttpUrl _base = null;
    private boolean _primaryProxy = false;
    private boolean _transparentProxy = false;
    private boolean _transparentProxySecure = false;

    private InetSocketAddress _sockAddr = null;
    
    public ListenerSpec(String address, int port, HttpUrl base, boolean primaryProxy, boolean transparentProxy, boolean transparentProxySecure) {
        if (address == null) {
            address = "*";
        }
        _address = address;
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65536");
        }
        _port = port;
        _base = base;
        _primaryProxy = primaryProxy;
        _transparentProxy = transparentProxy;
        _transparentProxySecure = transparentProxySecure;
    }
    
    public String getAddress() {
        return _address;
    }
    
    public int getPort() {
        return _port;
    }
    
    public HttpUrl getBase() {
        return _base;
    }
    
    public boolean isPrimaryProxy() {
        return _primaryProxy;
    }
    
    public boolean isTransparentProxy() {
        return _transparentProxy;
    }
    
    public boolean isTransparentProxySecure() {
        return _transparentProxySecure;
    }
    
    public String getKey() {
        return _address + ":" + _port;
    }
    
    public int hashCode() {
        return toString().hashCode();
    }
    
    public String toString() {
        return _address + ":" + _port + (_base != null ? " => " + _base : "") + (_primaryProxy ? " Primary" : "");
    }
    
    public boolean equals(Object obj) {
        return toString().equals(obj.toString());
    }
    
    public InetSocketAddress getInetSocketAddress() {
        if (_sockAddr == null) {
            _sockAddr = new InetSocketAddress(_address, _port);
        }
        return _sockAddr;
    }
    
    public void verifyAvailable() throws IOException {
        // make sure we can listen on the port
        InetSocketAddress sa = getInetSocketAddress();
        ServerSocket serversocket = new ServerSocket(sa.getPort(), 5, sa.getAddress());
        serversocket.close();
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
    
    
}
