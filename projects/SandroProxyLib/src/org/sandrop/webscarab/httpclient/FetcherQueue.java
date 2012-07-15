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
package org.sandrop.webscarab.httpclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;



/**
 *
 * @author rdawes
 */
public class FetcherQueue {
    
    private ConversationHandler _handler;
    
    private Fetcher[] _fetchers;
    private int _requestDelay;
    private long _lastRequest = 0;
    private List _requestQueue = new ArrayList();
    private boolean _running = true;
    private int _pending = 0;
    
    /** Creates a new instance of FetcherQueue */
    public FetcherQueue(String name, ConversationHandler handler, int threads, int requestDelay) {
        _handler = handler;
        _fetchers = new Fetcher[threads];
        _requestDelay = requestDelay;
        for (int i=0; i<threads; i++) {
            _fetchers[i] = new Fetcher(name+"-"+i);
        }
        start();
    }
    
    public void stop() {
        _running = false;
    }
    
    public void start() {
        _running = true;
        for (int i=0; i<_fetchers.length; i++) {
            _fetchers[i].start();
        }
        
    }
    
    public boolean isBusy() {
        return _pending > 0 || getRequestsQueued() > 0;
    }
    
    public void submit(Request request) {
        synchronized (_requestQueue) {
            _requestQueue.add(request);
            _requestQueue.notify();
        }
    }
    
    public int getRequestsQueued() {
        synchronized (_requestQueue) {
            return _requestQueue.size();
        }
    }
    
    public void clearRequestQueue() {
        synchronized (_requestQueue) {
            _requestQueue.clear();
        }
    }
    
    private void responseReceived(Response response) {
        _handler.responseReceived(response);
        _pending--;
    }
    
    private void requestError(Request request, IOException ioe) {
        _handler.requestError(request, ioe);
        _pending--;
    }
    
    private Request getNextRequest() {
    	Request nextRequest = null;
    	synchronized (_requestQueue) {
    		while (_requestQueue.size() == 0) {
    			try {
    				_requestQueue.wait();
    			} catch (InterruptedException ie) {
    				// check again
    			}
    		}
    		nextRequest = (Request) _requestQueue.remove(0);
    	}
        if (_requestDelay > 0) {
        	long currentTimeMillis = System.currentTimeMillis();
        	while (currentTimeMillis < _lastRequest + _requestDelay) {
        		try {
        			Thread.sleep(_lastRequest + _requestDelay - currentTimeMillis);
        		} catch (InterruptedException ie) {}
        		currentTimeMillis = System.currentTimeMillis();
        	}
        	_lastRequest = currentTimeMillis;
        }
        _pending++;
        return nextRequest;
    }
    
    private class Fetcher extends Thread {
        public Fetcher(String name) {
            super(name);
            setDaemon(true);
            setPriority(Thread.MIN_PRIORITY);
        }
        
        public void run() {
        	HTTPClient client = HTTPClientFactory.getValidInstance().getHTTPClient();
            while (_running) {
                Request request = getNextRequest();
                try {
                    Response response = client.fetchResponse(request);
                    response.flushContentStream();
                    responseReceived(response);
                } catch (IOException ioe) {
                    requestError(request, ioe);
                }
            }
        }
    }
}
