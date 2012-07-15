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
package org.sandroproxy.logger;

import java.util.logging.LogManager;

import android.os.Handler;

public class Logger {
    
    private static boolean LOG_TO_HANDLER = true;
    private static boolean LOG_TO_FILE = false;
    private static boolean LOG_TO_GUI = true;
    
    private static Logger mLogger;
    
    
    public Logger(Handler guiHandler){
        if (LOG_TO_GUI){
            LogHandler logHandler = new LogHandler(guiHandler);
            LogManager.getLogManager().getLogger("").addHandler(logHandler);
        }
    }
}
