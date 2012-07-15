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

import java.util.EventObject;

/**
 *
 * @author  rogan
 */
public class FrameworkEvent extends EventObject {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 6301623751009629601L;
	private ConversationID _id = null;
    private HttpUrl _url = null;
    private Cookie _cookie = null;
    private String _property = null;
    
    /** Creates a new instance of FrameworkEvent */
    public FrameworkEvent(Object source, ConversationID id, String property) {
        super(source);
        _id = id;
        _property = property;
    }
    
    public FrameworkEvent(Object source, HttpUrl url, String property) {
        super(source);
        _url = url;
        _property = property;
    }
    
    public FrameworkEvent(Object source, Cookie cookie) {
        super(source);
        _cookie = cookie;
    }
    
    public ConversationID getConversationID() {
        return _id;
    }
    
    public HttpUrl getUrl() {
        return _url;
    }
    
    public Cookie getCookie() {
        return _cookie;
    }
    
    public String getPropertyName() {
        return _property;
    }
    
}
