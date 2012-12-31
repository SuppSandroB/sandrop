package org.sandroproxy.webscarab.store.sql;

public class Conversation {
    public long UNIQUE_ID;
    public long REQUEST_ID;
    public long REQUEST_CHANGED_ID;
    public long RESPONSE_ID;
    public long TS_START;
    public long TS_END;
    public int STATUS;
    public String STATUS_DESC;
    public int TYPE;
    public String CLIENT_ADDRESS;
    public String REQ_METHOD;
    public String REQ_PATH;
    public String REQ_QUERY_STRING;
    public String REQ_SCHEMA;
    public String REQ_HOST;
    public long RESP_STATUS_CODE;
    public String RESP_CONTENT_TYPE;
    public String RESP_FILE_TYPE;
    public String RESP_APP_TYPE;
    
    
    public long getRequestId(){
        if (REQUEST_CHANGED_ID != -1){
            return REQUEST_CHANGED_ID;
        }
        return REQUEST_ID;
    }
}
