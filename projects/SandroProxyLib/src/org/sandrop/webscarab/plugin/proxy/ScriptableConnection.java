/***********************************************************************
 *
 * This file is part of SandroProxy, 
 * For details, please see http://code.google.com/p/sandrop/
 *
 * Copyright (c) 20012 supp.sandrob@gmail.com
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

import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;

/**
 * This is a scriptable object, which represents a connection from a browser,
 * the request that the browser submits, and the response that is returned.
 * @author rogan
 */
public class ScriptableConnection {
    
    private Socket _socket = null;
    
    private Request _request = null;
    private Response _response = null;
    
    /** Creates a new instance of Connection */
    public ScriptableConnection(Socket socket) {
        _socket = socket;
    }
    
    /**
     * This is the address of the remote host that is connected. 
     * If the connection should not be allowed, call connection.closeConnection();
     * @return the address of the remote host
     */    
    public InetAddress getAddress() {
        return _socket.getInetAddress();
    }
    
    /**
     * closes the connection to the browser
     */
    public void closeConnection() {
        try {
            _socket.close();
        } catch (IOException ioe) {}
    }
    
    /**
     * Sets the Request object that will be sent to the server.
     *
     * Don't change this in a script that modifies the Response
     *
     * Set the Request to null to abort the request, and send an error back to the browser
     * If you set the response as well, that response will be returned to the browser, and
     * nothing will be added to the model
     * @param request The request that should be sent to the server
     */    
    public void setRequest(Request request) {
        _request = request;
    }
    
    /**
     * Can be called by a script to get the request that will be sent to the server
     * @return the Request that will be sent to the server
     */    
    public Request getRequest() {
        return _request;
    }
    
    /**
     * Sets the Response that will be sent back to the browser.
     *
     * If this is called before the request has ben sent to the server, the request
     * will be aborted, and the response will be sent back to the browser.
     * @param response the response to send back to the browser
     */    
    public void setResponse(Response response) {
        _response = response;
    }
    
    /**
     * Gets the Response that was returned by the server
     * @return the Response that was returned by the server
     */    
    public Response getResponse() {
        return _response;
    }
    
}
