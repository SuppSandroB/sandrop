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
import java.util.Date;

import org.sandrop.webscarab.util.EventListenerList;

/**
 *
 * @author  rogan
 */
public abstract class AbstractConversationModel implements ConversationModel {
    
    private FrameworkModel _model;
    
    private EventListenerList _listenerList = new EventListenerList();
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    /** Creates a new instance of AbstractConversationModel */
    public AbstractConversationModel(FrameworkModel model) {
        _model = model;
    }
    
    public abstract int getConversationCount();
    
    public abstract ConversationID getConversationAt(int index);
    
    public abstract int getIndexOfConversation(ConversationID id);
    
    // public abstract Sync readLock();
    
    public String getConversationOrigin(ConversationID id) {
        return _model.getConversationOrigin(id);
    }
    
    public Date getConversationDate(ConversationID id) {
        return _model.getConversationDate(id);
    }
    
    public String getRequestMethod(ConversationID id) {
        return _model.getRequestMethod(id);
    }
    
    public String getConversationProperty(ConversationID id, String property) {
        return _model.getConversationProperty(id, property);
    }
    
    public void setConversationProperty(ConversationID id, String property, String value) {
        _model.setConversationProperty(id, property, value);
    }
    
    public String getResponseStatus(ConversationID id) {
        return _model.getResponseStatus(id);
    }
    
    public HttpUrl getRequestUrl(ConversationID id) {
        return _model.getRequestUrl(id);
    }
    
    public Request getRequest(ConversationID id) {
        return _model.getRequest(id);
    }
    
    public Response getResponse(ConversationID id) {
        return _model.getResponse(id);
    }
    
    /**
     * adds a listener to the model
     * @param listener the listener to add
     */
    public void removeConversationListener(ConversationListener listener) {
        synchronized(_listenerList) {
            _listenerList.remove(ConversationListener.class, listener);
        }
    }
    
    /**
     * adds a listener to the model
     * @param listener the listener to add
     */
    public void addConversationListener(ConversationListener listener) {
        synchronized(_listenerList) {
            _listenerList.add(ConversationListener.class, listener);
        }
    }
    
    /**
     * tells listeners that a new Conversation has been added
     * @param id the conversation
     * @param position the position in the list
     */
    protected void fireConversationAdded(ConversationID id, int position) {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        ConversationEvent evt = new ConversationEvent(this, id, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationAdded(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    /**
     * tells listeners that a conversation has been removed, after the fact
     * @param id the conversation ID
     * @param position the position in the overall conversation list prior to removal
     */
    protected void fireConversationRemoved(ConversationID id, int position) {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        ConversationEvent evt = new ConversationEvent(this, id, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]== ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationRemoved(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    /**
     * fired to tell listeners that a particular conversation has had a property change
     * @param id the conversation
     * @param property the name of the property that was changed
     */
    protected void fireConversationChanged(ConversationID id, int position) {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        ConversationEvent evt = new ConversationEvent(this, id, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationChanged(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    /**
     * fired to tell listeners that a particular conversation has had a property change
     * @param id the conversation
     * @param property the name of the property that was changed
     */
    protected void fireConversationsChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationsChanged();
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
}
