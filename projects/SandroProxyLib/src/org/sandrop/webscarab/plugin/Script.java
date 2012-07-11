/***********************************************************************
 *
 * This file is part of SandroProxy, 
 * For details, please see http://code.google.com/p/sandrop/
 *
 * Copyright (c) 20012 supp.sandrob@gmail.com
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class Script {
    
    private File _file;
    private String _script;
    private long _lastModified;
    private boolean _enabled;
    private String _language = null;
    
    private Logger _logger = Logger.getLogger(getClass().toString());
    
    /** Creates a new instance of Script */
    public Script(File file) throws IOException {
        _file = file;
        reload();
        _enabled = false;
    }
    
    public void reload() throws IOException {
    	_logger.info("reloading " + _file);
        FileReader fr = null;
        try {
            fr = new FileReader(_file);
            int got;
            char[] buff = new char[1024];
            StringBuffer script = new StringBuffer();
            while ((got=fr.read(buff))>0) {
                script.append(buff,0,got);
            }
            _script = script.toString();
            _lastModified = _file.lastModified();
        } catch (IOException ioe) {
            _enabled = false;
            _script = "";
            throw ioe;
        } finally {
            if (fr!=null) fr.close();
        }
    }
    
    public boolean isEnabled() {
        return _enabled;
    }
    
    public void setEnabled(boolean enabled) {
        if (enabled)
        	try {
        		reload();
        	} catch (IOException ioe) {
        		_logger.severe("Error reloading script " + _file + " : " + ioe);
        		_enabled = false;
        		return;
        	}
        _enabled = enabled;
    }
    
    public File getFile() {
        return _file;
    }
    
    public String getScript() {
        return _script;
    }
    
    public void setScript(String script) throws IOException {
        _script = script;
        FileWriter fw = null;
        try { 
            fw = new FileWriter(_file);
            fw.write(_script);
        } catch (IOException ioe) {
            _script = null;
            _lastModified = -1;
            _language = null;
            _enabled = false;
            throw ioe;
        } finally {
            if (fw != null) fw.close();
        }
    }
    
    public long getLastModified() {
        return _lastModified;
    }
    
    public String getLanguage() {
        return _language;
    }
    
    public void setLanguage(String language) {
        _language = language;
    }
}
