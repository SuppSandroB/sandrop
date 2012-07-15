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

package org.sandrop.webscarab.plugin.manualrequest;


import org.sandrop.webscarab.model.ConversationModel;
import org.sandrop.webscarab.model.Cookie;
import org.sandrop.webscarab.model.FrameworkModel;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.plugin.AbstractPluginModel;

public class ManualRequestModel extends AbstractPluginModel {
    
    private FrameworkModel _model;
    
    /** Creates a new instance of ManualRequestModel */
    public ManualRequestModel(FrameworkModel model) {
        _model = model;
    }
    
    public ConversationModel getConversationModel() {
        return _model.getConversationModel();
    }
    
    public Cookie[] getCookiesForUrl(HttpUrl url) {
        return _model.getCookiesForUrl(url);
    }
    
    public void addCookie(Cookie cookie) {
        _model.addCookie(cookie);
    }
    
}
