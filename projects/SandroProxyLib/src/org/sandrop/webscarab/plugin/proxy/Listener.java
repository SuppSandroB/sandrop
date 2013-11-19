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

import java.lang.Runnable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.lang.Thread;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.sandrop.webscarab.model.ConnectionDescriptor;

import android.widget.GridLayout.Spec;

public class Listener implements Runnable {
    
    private Proxy _proxy;
    private ListenerSpec _spec;
    
    private ServerSocket _serversocket = null;

    private boolean _stop = false;
    private boolean _stopped = true;
    
    private int _count = 1;
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    /** Creates a new instance of Listener */
    public Listener(Proxy proxy, ListenerSpec spec) {
        _logger.setLevel(Level.FINEST);
        _proxy = proxy;
        _spec = spec;
    }

    public void run() {
        _stop = false;
        _stopped = false;
        Socket sock;
        ConnectionHandler ch;
        Thread thread;
        if (_serversocket == null || _serversocket.isClosed()) {
            try {
                listen();
            } catch (IOException ioe) {
                _logger.severe("Can't listen at " + _spec + ": " + ioe);
                _stopped = true;
                return;
            }
        }
        while (! _stop) {
            try {
            	sock = _serversocket.accept();
            	ConnectionDescriptor connectionDescriptor = null;
            	IClientResolver clientResolver = _proxy.getClientResolver();
            	String threadName = Thread.currentThread().getName();
            	if (clientResolver != null){
            	    connectionDescriptor = clientResolver.getClientDescriptorBySocket(sock);
            	    threadName = connectionDescriptor.getId() + "_" + connectionDescriptor.getNamespace();
            	}
                ch = new ConnectionHandler(_proxy, sock, _spec.getBase(), _spec.isTransparentProxy(), _spec.isTransparentProxySecure(),
                                           _spec.mustCaptureData(), _spec.useFakeCerts(),
                                           _proxy.getTransparentProxyResolver(), connectionDescriptor);
                thread = new Thread(ch, Thread.currentThread().getName()+"-"+Integer.toString(_count++));
                thread.setName(threadName);
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                String exMessage = e.getMessage();
                if (exMessage != null){
                    if (!e.getMessage().equals("Try again")) {
                        _logger.fine("I/O error while waiting for connection: " + exMessage);
                    }
                }else{
                }
                    
            }
        }
        _stopped = true;
        try {
            _serversocket.close();
        } catch (IOException ioe) {
            System.err.println("Error closing socket : " + ioe);
        }
        _logger.info("Not listening on " + _spec);
    }
    
    private void listen() throws IOException {
        InetSocketAddress sa = _spec.getInetSocketAddress();
        _serversocket = new ServerSocket(sa.getPort(), 5, sa.getAddress());
        
        _logger.info("Proxy listening on " + _spec);
        
        try {
            _serversocket.setSoTimeout(100);
        } catch (SocketException se) {
            _logger.warning("Error setting sockettimeout " + se);
            _logger.warning("It is likely that this listener will be unstoppable!");
        }
    }
    
    public boolean stop() {
        _stop = true;
        if (!_stopped) {
            for (int i=0; i<20; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {}
                if (_stopped) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    public ListenerSpec getListenerSpec() {
        return _spec;
    }
    
}
