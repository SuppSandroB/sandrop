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
package org.sandrop.webscarab.plugin.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketForwarder extends Thread {
    private InputStream in;
    private OutputStream out;

    public static void connect(String name, Socket clientSocket, Socket serverSocket) throws IOException {
        SocketForwarder clientServer = new SocketForwarder(name + "_clientServer", clientSocket.getInputStream(), serverSocket.getOutputStream());
        SocketForwarder serverClient = new SocketForwarder(name + "_serverClient", serverSocket.getInputStream(), clientSocket.getOutputStream());
            clientServer.start();
            serverClient.start();
            while (clientServer.isAlive()) {
                    try {
                        clientServer.join();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
            }
            while (serverClient.isAlive()) {
                    try {
                        serverClient.join();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
            }
    }

    public SocketForwarder(String name, InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            setName(name);
            setDaemon(true);
    }

    public void run() {
            try {
                    byte[] buff = new byte[4096];
                    int got;
                    while ((got = in.read(buff)) > -1)
                            out.write(buff, 0, got);
            } catch (IOException ignore) {
            } finally {
                    try {
                            in.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
                    try {
                            out.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
            }
    }

}
