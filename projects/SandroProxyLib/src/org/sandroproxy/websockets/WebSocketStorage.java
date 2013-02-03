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

import java.sql.SQLException;
import java.util.logging.Logger;

import org.sandroproxy.webscarab.store.sql.SqlLiteStore;
import org.sandroproxy.websockets.WebSocketProxy.State;

/**
 * Listens to all WebSocket messages and utilizes {@link TableWebSocket} to
 * store messages in database.
 */
public class WebSocketStorage implements WebSocketObserver {

	private static final Logger logger = Logger
			.getLogger(WebSocketStorage.class.getSimpleName());

	// determines when messages are stored in databases
	public static final int WEBSOCKET_OBSERVING_ORDER = 100;

	private SqlLiteStore store;

	public WebSocketStorage(SqlLiteStore store) {
		this.store = store;
	}

	@Override
	public int getObservingOrder() {
		return WEBSOCKET_OBSERVING_ORDER;
	}

	@Override
	public boolean onMessageFrame(long channelId, WebSocketMessage wsMessage) {
		if (wsMessage.isFinished()) {
			WebSocketMessageDTO message = wsMessage.getDTO();

			try {
			    store.insertMessage(message);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}

		// forward message frame to other observers and then send through
		return true;
	}

	@Override
	public void onStateChange(State state, WebSocketProxy proxy) {
		if (state.equals(State.OPEN) || state.equals(State.CLOSED) || state.equals(State.INCLUDED)) {
			try {
				if (store != null) {
				    store.insertOrUpdateChannel(proxy.getDTO());
				} else if (!state.equals(State.CLOSED)) {
					logger.info("Could not update state of WebSocket channel to '" + state.toString() + "'!");
				}
			} catch (SQLException e) {
				logger.info(e.getMessage());
			}
		} else if (state.equals(State.EXCLUDED)) {
			// when proxy is excluded, then messages are forwarded
			// but not stored - all existing communication is deleted
            try {
                store.purgeChannel(proxy.getChannelId());
			} catch (SQLException e) {
				logger.info(e.getMessage());
			}
		}
	}
}
