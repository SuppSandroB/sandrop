package org.sandroproxy.webscarab.store.sql;

public interface IStoreEventListener {
    
    void newConversation(long conversationId, int type);
    void startConversation(long conversationId);
    void endConversation(long conversationId, boolean protocolSwitch);
    
    void socketFrameSend(long conversationId, long messageId);
    void socketFrameReceived(long conversationId, long messageId);
}
