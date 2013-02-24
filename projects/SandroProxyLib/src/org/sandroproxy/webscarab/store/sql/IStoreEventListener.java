package org.sandroproxy.webscarab.store.sql;

public interface IStoreEventListener {
    
    void newConversation(long conversationId, int type, long timestamp);
    void startConversation(long conversationId, long timestamp);
    void endConversation(long conversationId, boolean protocolSwitch, long timestamp);
    
    void socketFrameSend(long conversationId, long messageId, long timestamp);
    void socketFrameReceived(long conversationId, long messageId, long timestamp);
}
