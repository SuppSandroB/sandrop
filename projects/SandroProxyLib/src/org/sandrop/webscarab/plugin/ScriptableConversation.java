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

public class ScriptableConversation {
    
	private ConversationID _id;
    private Request _request;
    private Response _response;
    private String _origin;
    
    private boolean _cancelled = false;
    private boolean _analyse = true;
    
    /** Creates a new instance of ScriptableConversation */
    public ScriptableConversation(ConversationID id, Request request, Response response, String origin) {
    	_id = id;
        _request = request;
        _response = response;
        _origin = origin;
    }
    
    public ConversationID getId() {
    	return _id;
    }
    
    public Request getRequest() {
        return new Request(_request); // protective copy
    }
    
    public Response getResponse() {
        return new Response(_response); // protective copy
    }
    
    public String getOrigin() {
        return _origin;
    }
    
    public void setCancelled(boolean cancelled) {
        _cancelled = cancelled;
    }
    
    public boolean isCancelled() {
        return _cancelled;
    }
    
    public void setAnalyse(boolean analyse) {
        _analyse = analyse;
    }
    
    public boolean shouldAnalyse() {
        return _analyse;
    }
    
}
