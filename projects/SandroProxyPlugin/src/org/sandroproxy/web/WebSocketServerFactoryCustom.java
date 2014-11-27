package org.sandroproxy.web;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.java_websocket.WebSocketAdapter;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.server.WebSocketServer.WebSocketServerFactory;


public class WebSocketServerFactoryCustom implements WebSocketServerFactory{
    @Override
    public WebSocketImpl createWebSocket( WebSocketAdapter a, Draft d, Socket s ) {
        return new WebSocketImplCustom( a, d, s );
    }
    @Override
    public WebSocketImpl createWebSocket( WebSocketAdapter a, List<Draft> d, Socket s ) {
        return new WebSocketImplCustom( a, d, s );
    }
    @Override
    public SocketChannel wrapChannel( SelectionKey c ) {
        return (SocketChannel) c.channel();
    }
}
