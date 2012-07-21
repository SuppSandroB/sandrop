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

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.bsf.BSFManager;
import org.apache.bsf.BSFException;

public class Hook {
    
    private String _name;
    private String _description;
    private List<Script> _scripts = new ArrayList<Script>();
    protected BSFManager _bsfManager = null;
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    /** Creates a new instance of Hook */
    public Hook(String name, String description) {
        _name = name;
        _description = description;
    }
    
    public void setBSFManager(BSFManager bsfManager) {
        _bsfManager = bsfManager;
    }
    
    protected void runScripts() {
        if (_bsfManager == null) return;
        synchronized(_bsfManager) {
            for (int i=0; i<_scripts.size(); i++) {
                Script script = _scripts.get(i);
                if (script.isEnabled()) {
//                    if (_scriptManager != null) _scriptManager.scriptStarted(this, script);
                    try {
                        _bsfManager.exec(script.getLanguage(), _name, 0, 0, script.getScript());
                    } catch (BSFException bsfe) {
                        _logger.warning("Script exception: " + bsfe);
//                        if (_scriptManager != null) _scriptManager.scriptError(this, script, bsfe);
                    }
//                    if (_scriptManager != null) _scriptManager.scriptEnded(this, script);
                }
            }
        }
    }
    
    public String getName() {
        return _name;
    }
    
    public String getDescription() {
        return _description;
    }
    
    public int getScriptCount() {
        return _scripts.size();
    }
    
    public Script getScript(int i) {
        return _scripts.get(i);
    }
    
    public void addScript(Script script) {
        _scripts.add(script);
    }
    
    public void addScript(Script script, int position) {
        _scripts.add(position, script);
    }
    
    public Script removeScript(int position) {
        return _scripts.remove(position);
    }
    
}
