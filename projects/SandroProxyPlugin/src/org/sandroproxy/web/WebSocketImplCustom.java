package org.sandroproxy.web;

import java.net.Socket;
import java.util.List;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft;

public class WebSocketImplCustom extends WebSocketImpl{

    private boolean isInitialised = false;
    private boolean showRuntimeEvents = false;
    private boolean showStoredEvents = false;
    private String socketId;
    
    public WebSocketImplCustom(WebSocketListener listener, Draft draft,
            Socket sock) {
        super(listener, draft, sock);
    }
    
    public WebSocketImplCustom(WebSocketListener listener, List<Draft> d, Socket sock) {
        super(listener, d, sock);
    }

    /**
     * @return the isInitialised
     */
    public boolean isInitialised() {
        return isInitialised;
    }

    /**
     * @param isInitialised the isInitialised to set
     */
    public void setInitialised(boolean isInitialised) {
        this.isInitialised = isInitialised;
    }

    /**
     * @return the showRuntimeEvents
     */
    public boolean isShowRuntimeEvents() {
        return showRuntimeEvents;
    }

    /**
     * @param showRuntimeEvents the showRuntimeEvents to set
     */
    public void setShowRuntimeEvents(boolean showRuntimeEvents) {
        this.showRuntimeEvents = showRuntimeEvents;
    }

    /**
     * @return the showStoredEvents
     */
    public boolean isShowStoredEvents() {
        return showStoredEvents;
    }

    /**
     * @param showStoredEvents the showStoredEvents to set
     */
    public void setShowStoredEvents(boolean showStoredEvents) {
        this.showStoredEvents = showStoredEvents;
    }

    /**
     * @return the socketId
     */
    public String getSocketId() {
        return socketId;
    }

    /**
     * @param socketId the socketId to set
     */
    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }
    
}
