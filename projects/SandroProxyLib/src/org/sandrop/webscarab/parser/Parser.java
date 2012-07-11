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

package org.sandrop.webscarab.parser;


import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Message;
import org.sandrop.webscarab.util.MRUCache;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * provides an interface for generic parsing of message content. Allows for sharing
 * of parsed content between plugins without performing addional parse steps.
 *
 * Parsed representations should NOT be modified.
 * @author knoppix
 */
public class Parser {
    
    private static List _parsers = new ArrayList();
    
    // we cache the 8 most recent messages and their parsed versions
    private static MRUCache _cache = new MRUCache(8);
    
    static {
        _parsers.add(new HTMLParser());
    }
    
    /** Creates a new instance of Parser */
    private Parser() {
    }
    
    /**
     * returns a parsed representation of the message, requesting
     * the parsers to resolve any links relative to the url provided
     */    
    public static Object parse(HttpUrl url, Message message) {
        if (_cache.containsKey(message)) {
            return _cache.get(message);
        }
        Iterator it = _parsers.iterator();
        Object parsed = null;
        ContentParser parser;
        while(it.hasNext()) {
            parser = (ContentParser) it.next();
            parsed = parser.parseMessage(url, message);
            if (parsed != null) break;
        }
        _cache.put(message, parsed);
        return parsed;
    }
    
}
