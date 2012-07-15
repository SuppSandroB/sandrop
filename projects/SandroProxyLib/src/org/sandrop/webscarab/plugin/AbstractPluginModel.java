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

package org.sandrop.webscarab.plugin;

import org.sandrop.webscarab.util.ReentrantReaderPreferenceReadWriteLock;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 *
 * @author  rogan
 */
public class AbstractPluginModel {
    
    public final static String PROPERTY_STATUS = "Status";
    public final static String PROPERTY_RUNNING = "Running";
    public final static String PROPERTY_STOPPING = "Stopping";
    public final static String PROPERTY_MODIFIED = "Modified";
    public final static String PROPERTY_BUSY = "Busy";
    
    protected PropertyChangeSupport _changeSupport = new PropertyChangeSupport(this);
    protected ReentrantReaderPreferenceReadWriteLock _rwl = new ReentrantReaderPreferenceReadWriteLock();
    
    private String _status = "Stopped";
    private boolean _running = false;
    private boolean _stopping = false;
    private boolean _modified = false;
    private boolean _busy = false;
    
    /** Creates a new instance of AbstractPluginModel */
    public AbstractPluginModel() {
    }
    
    public void setStatus(String status) {
        if (!_status.equals(status)) {
            String old = _status;
            _status = status;
            _changeSupport.firePropertyChange(PROPERTY_STATUS, old, _status);
        }
    }
    
    public String getStatus() {
        return _status;
    }
    
    public void setRunning(boolean running) {
        if (_running != running) {
            _running = running;
            _changeSupport.firePropertyChange(PROPERTY_RUNNING, !_running, _running);
        }
    }
    
    public boolean isRunning() {
        return _running;
    }
    
    public void setStopping(boolean stopping) {
        if (_stopping != stopping) {
            _stopping = stopping;
            _changeSupport.firePropertyChange(PROPERTY_STOPPING, !_stopping, _stopping);
        }
    }
    
    public boolean isStopping() {
        return _stopping;
    }
    
    public void setModified(boolean modified) {
        if (_modified != modified) {
            _modified = modified;
            _changeSupport.firePropertyChange(PROPERTY_MODIFIED, !_modified, _modified);
        }
    }
    
    public boolean isModified() {
        return _modified;
    }
    
    public void setBusy(boolean busy) {
        if (_busy != busy) {
            _busy = busy;
            _changeSupport.firePropertyChange(PROPERTY_BUSY, !_busy, _busy);
        }
    }
    
    public boolean isBusy() {
        return _busy;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        _changeSupport.addPropertyChangeListener(listener);
    }
    
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        _changeSupport.addPropertyChangeListener(propertyName, listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        _changeSupport.removePropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        _changeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
}
