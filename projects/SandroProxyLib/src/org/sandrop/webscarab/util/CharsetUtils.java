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

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

/**
 * @author rdawes
 *
 */
public class CharsetUtils {
    
    public static String getCharset(byte[] bytes) {
        nsDetector det = new nsDetector(nsPSMDetector.ALL);
        CharsetListener listener = new CharsetListener();
        det.Init(listener);
        
        boolean isAscii = det.isAscii(bytes,bytes.length);
        // DoIt if non-ascii and not done yet.
        if (!isAscii)
            det.DoIt(bytes,bytes.length, false);
        det.DataEnd();
        if (isAscii) return "ASCII";
        
        return listener.getCharset();
    }
    
    private static class CharsetListener implements nsICharsetDetectionObserver {
        
        private String charset = null;
        
        public void Notify(String charset) {
            this.charset = charset;
        }
        
        public String getCharset() {
            return this.charset;
        }
        
    }
    
}
