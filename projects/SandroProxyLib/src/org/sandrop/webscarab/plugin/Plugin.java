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

import org.sandrop.webscarab.model.ConversationID;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.model.StoreException;

public interface Plugin extends Runnable {
    
    /** The plugin name
     * @return The name of the plugin
     */    
    String getPluginName();
    
    /**
     * informs the plugin that the Session has changed
     * @param model the new model
     */    
    void setSession(String type, Object store, String session) throws StoreException;
    
    /**
     * starts the plugin running
     */
    void run();
    
    boolean isRunning();
    
    /** called to test whether the plugin is able to be stopped
     * @return false if the plugin can be stopped
     */
    boolean isBusy();
    
    /** called to determine what the current status of the plugin is
     */
    String getStatus();
    
    /**
     * called to suspend or stop the plugin
     */
    boolean stop();
    
    /** called to determine whether the data stored within the plugin has been modified
     * and should be saved
     */
    boolean isModified();
    
    /**
     * called to instruct the plugin to flush any memory-only state to the store.
     * @throws StoreException if there is any problem saving the session data
     */    
    void flush() throws StoreException;
    
    void analyse(ConversationID id, Request request, Response response, String origin);
    
    Hook[] getScriptingHooks();
    
    Object getScriptableObject();
    
}
