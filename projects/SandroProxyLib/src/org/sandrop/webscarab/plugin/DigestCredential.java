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

public class DigestCredential {
    
    private String _host;
    private String _realm;
    private String _username;
    private String _password;
    
    /**
     * Creates a new instance of DigestCredential 
     */
    public DigestCredential(String host, String realm, String username, String password) {
        _host = host;
        _realm = realm;
        _username = username;
        _password = password;
    }
    
    public String getHost() {
        return _host;
    }
    
    public String getRealm() {
        return _realm;
    }
    
    public String getUsername() {
        return _username;
    }
    
    public String getPassword() {
        return _password;
    }
    
}
