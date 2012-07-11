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

package org.sandrop.webscarab.util;

import java.util.Stack;

/**
 * Utility class for converting a Unix shell style glob to a Java Regular Expression
 * Shameless "stolen" from JEdit, with many thanks.
 *
 * @author  jedit team
 */
public class Glob {
    
    /** has no instance methods */
    private Glob() {
    }
    
    /**
     * Converts a Unix-style glob to a regular expression.
     *
     * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
     * @param glob The glob pattern
     */
    public static String globToRE(String glob) {
        final Object NEG = new Object();
        final Object GROUP = new Object();
        Stack state = new Stack();
        
        StringBuffer buf = new StringBuffer();
        boolean backslash = false;
        
        for(int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if(backslash) {
                buf.append('\\');
                buf.append(c);
                backslash = false;
                continue;
            }
            
            switch(c) {
                case '\\':
                    backslash = true;
                    break;
                case '?':
                    buf.append('.');
                    break;
                case '.':
                case '+':
                case '(':
                case ')':
                    buf.append('\\');
                    buf.append(c);
                    break;
                case '*':
                    buf.append(".*");
                    break;
                case '|':
                    if(backslash)
                        buf.append("\\|");
                    else
                        buf.append('|');
                    break;
                case '{':
                    buf.append('(');
                    if(i + 1 != glob.length() && glob.charAt(i + 1) == '!') {
                        buf.append('?');
                        state.push(NEG);
                    }
                    else
                        state.push(GROUP);
                    break;
                case ',':
                    if(!state.isEmpty() && state.peek() == GROUP)
                        buf.append('|');
                    else
                        buf.append(',');
                    break;
                case '}':
                    if(!state.isEmpty()) {
                        buf.append(")");
                        if(state.pop() == NEG)
                            buf.append(".*");
                    }
                    else
                        buf.append('}');
                    break;
                default:
                    buf.append(c);
            }
        }
        
        return buf.toString();
    }
    
}
