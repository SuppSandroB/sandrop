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
/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sandroproxy.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;


/**
 * Wrap it in a thread to listen for one end of a WebSockets connection. It does
 * so by using blocking reads and passes read bytes to {@link WebSocketProxy} for
 * processing. If you want to listen (a.k.a. observe) for WebSocket messages, see
 * the {@link WebSocketObserver} class.
 */
public class WebSocketListener implements Runnable {

	private static final Logger logger = Logger.getLogger(WebSocketListener.class.getSimpleName());

	/**
	 * Listen from one side of this communication channel.
	 */
	private final InputStream in;

	/**
	 * Write/Forward frames to the other side.
	 */
	private final OutputStream out;

	/**
	 * This proxy object is used to process the read.
	 */
	private final WebSocketProxy wsProxy;

	/**
	 * Name of this thread (in-, or outgoing)
	 */
	private final String name;

	/**
	 * Indicates if it still listens.
	 */
	private boolean isFinished = false;

	/**
	 * Create listener, that calls the WebSocketsProxy instance to process read
	 * data. It contains also the other end's writer to forward frames.
	 * 
	 * @param wsProxy When a read has to be processed, it delegates it to this object.
	 * @param in Read from one side.
	 * @param out Write to the other side.
	 * @param name Name of this thread (used for logging too).
	 */
	public WebSocketListener(WebSocketProxy wsProxy, InputStream in, OutputStream out, String name) {
		this.wsProxy = wsProxy;
		this.in = in;
		this.out = out;
		this.name = name;
	}

	/**
	 * Start listening and process messages as long as the Socket is open.
	 */
	@Override
	public void run() {
		Thread.currentThread().setName(name);
		
		try {
			if (in != null) {
				byte[] buffer = new byte[1];
				while (in.read(buffer) != -1) {
					// there is something to read => process in WebSockets version specific message
					wsProxy.processRead(in, out, buffer[0]);
				}
			}
		} catch (IOException e) {
			// includes SocketException
			// no more reading possible
			stop();
		} finally {				
			// mark as finished
			isFinished = true;
			
			// close the other listener too
			wsProxy.shutdown();
		}
	}

	/**
	 * Properly close incoming stream.
	 */
	private void closeReaderStream() {
		try {
			if (in != null) {
				in.close();
			}
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
	}

	/**
	 * Properly close outgoing stream.
	 */
	private void closeWriterStream() {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
	}

	/**
	 * Interrupts current thread, stopping its execution.
	 */
	public void stop() {
		// no more bytes can be read
		closeReaderStream();
		
		// no more bytes can be written
		closeWriterStream();
	}

	/**
	 * Has this listener already stopped working?
	 * 
	 * @return True if listener stopped listening.
	 */
	public boolean isFinished() {
		return isFinished;
	}
	
	/**
	 * Use this stream to send custom messages.
	 * 
	 * @return outgoing stream
	 */
	public OutputStream getOutputStream() {
		return out;
	}
}
