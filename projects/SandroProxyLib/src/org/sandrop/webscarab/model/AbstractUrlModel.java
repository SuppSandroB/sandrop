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
package org.sandrop.webscarab.model;

import EDU.oswego.cs.dl.util.concurrent.Sync;


import java.util.logging.Logger;

import org.sandrop.webscarab.util.EventListenerList;

/**
 *
 * @author  rogan
 */
public abstract class AbstractUrlModel implements UrlModel {
    
    private EventListenerList _listenerList = new EventListenerList();
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    /** Creates a new instance of AbstractUrlModel */
    public AbstractUrlModel() {
    }
    
    public abstract int getChildCount(HttpUrl parent);
    
    public abstract int getIndexOf(HttpUrl url);
    
    public abstract HttpUrl getChildAt(HttpUrl parent, int index);
    
    // public abstract Sync readLock();
    
    public void addUrlListener(UrlListener listener) {
        synchronized(_listenerList) {
            _listenerList.add(UrlListener.class, listener);
        }
    }
    
    public void removeUrlListener(UrlListener listener) {
        synchronized(_listenerList) {
            _listenerList.remove(UrlListener.class, listener);
        }
    }
    
    /**
     * tells listeners that a new Url has been added
     * @param url the url that was added
     */
    protected void fireUrlAdded(HttpUrl url, int position) {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        UrlEvent evt = new UrlEvent(this, url, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlAdded(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * tells listeners that a Url has been removed, after the fact
     * @param url the url that was removed
     */
    protected void fireUrlRemoved(HttpUrl url, int position) {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        UrlEvent evt = new UrlEvent(this, url, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlRemoved(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    /**
     * fired to tell listeners that a particular Url has had a property change
     * @param url the url that was changed
     */
    protected void fireUrlChanged(HttpUrl url, int position) {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        UrlEvent evt = new UrlEvent(this, url, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlChanged(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * fired to tell listeners that all Url have changed
     */
    protected void fireUrlsChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlsChanged();
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
}
