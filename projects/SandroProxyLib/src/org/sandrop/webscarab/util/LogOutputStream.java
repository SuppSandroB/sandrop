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


package org.sandrop.webscarab.util;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 *
 * @author  rdawes
 */

public class LogOutputStream extends FilterOutputStream {
    OutputStream _os;
    PrintStream _ps;
    
    public LogOutputStream(OutputStream os, PrintStream ps) {
        super(os);
        _os = os;
        _ps = ps;
    }
    
    public void write(int b) throws IOException {
        _os.write(b);
        _ps.write(b);
    }
    
    public void write(byte b[], int off, int len) throws IOException {
        _os.write(b, off, len);
        _ps.write(b, off, len);
    }
    
}

