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

/**
 * Data Transfer Object used for displaying WebSocket connection channels.
 * Intended to decouple user interface representation from version specific
 * {@link WebSocketProxy}.
 */
public class WebSocketChannelDTO implements Comparable<WebSocketChannelDTO> {
	
	/**
	 * ChannelId.
	 */
	public Long id;
	
	/**
	 * Is not necessarily the same as the {@link WebSocketChannelDTO#url}.
	 */
	public String host;
	
	/**
	 * Port where this channel is connected at. Usually 80 or 443.
	 */
	public Integer port;
	
	/**
	 * URL used in HTTP handshake.
	 */
	public String url;
	
	/**
	 * Timestamp taken, when connection was established successfully.
	 */
	public Long startTimestamp;
	
	/**
	 * Timestamp of close, otherwise <code>null</code>.
	 */
	public Long endTimestamp;
	
	/**
	 * Id of handshake message.
	 */
	public long historyId;

	public WebSocketChannelDTO() {
		
	}
	
	public WebSocketChannelDTO(String host) {
		this.host = host;
	}

	/**
	 * Determined upon {@link #startTimestamp} and {@link #endTimestamp}.
	 * 
	 * @return True if connection is still alive.
	 */
	public boolean isConnected() {
		if (startTimestamp != null && endTimestamp == null) {
			return true;
		}
		return false;
	}
	

	/**
	 * Returns URL without trailing slash and without parameters. This
	 * representation is used for the Mode+Context feature.
	 * 
	 * @return
	 */
	public String getContextUrl() {
		if (url == null) {
			return null;
		}
		
		if (url.indexOf("?") != -1) {		
    		url = url.substring(0, url.indexOf("?"));
		}
		
		return url.replaceFirst("/$", "");
	}

	@Override
	public String toString() {
		if (port != null && id != null) {
			return host + ":" + port + " (#" + id + ")";
		}
		return host;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (getClass() != object.getClass()) {
			return false;
		}
		WebSocketChannelDTO other = (WebSocketChannelDTO) object;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return 31 + ((id == null) ? 0 : id.hashCode());
	}

	/**
	 * Used for sorting items. If two items have identical host names and ports,
	 * the channel number is used to determine order.
	 */
	@Override
	public int compareTo(WebSocketChannelDTO other) {
		int result = host.compareTo(other.host);

		if (result == 0) {
			result = port.compareTo(other.port);
			if (result == 0) {
				return id.compareTo(other.id);
			}
		}

		return result;
	}

	public String getFullUri() {
    	StringBuilder regex = new StringBuilder();
    	if (url.matches(".*[^:/]/.*")) {
    		// place port into regex
    		String wsUri = url.replaceFirst("([^:/])/", "$1:" + port + "/");
    		
    		// transform http/https to ws/wss
    		wsUri = wsUri.replaceFirst("http(s?://)", "ws$1");
    		regex.append(wsUri);
    	} else {
    		if (port == 80) {
    			regex.append("ws://");
    		} else {
        		regex.append("wss://");
    		}
    		regex.append(host);
    		regex.append(":");
    		regex.append(port);
    	}
		return regex.toString();
	}
}