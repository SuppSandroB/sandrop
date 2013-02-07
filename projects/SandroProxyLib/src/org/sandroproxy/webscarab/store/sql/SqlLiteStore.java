package org.sandroproxy.webscarab.store.sql;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sandrop.webscarab.model.ConversationID;
import org.sandrop.webscarab.model.Cookie;
import org.sandrop.webscarab.model.FrameworkModel;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Message;
import org.sandrop.webscarab.model.NamedValue;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.model.SiteModelStore;
import org.sandrop.webscarab.model.StoreException;
import org.sandrop.webscarab.plugin.fragments.FragmentsStore;
import org.sandrop.webscarab.plugin.spider.Link;
import org.sandrop.webscarab.plugin.spider.SpiderStore;
import org.sandroproxy.websockets.WebSocketChannelDTO;
import org.sandroproxy.websockets.WebSocketMessage;
import org.sandroproxy.websockets.WebSocketMessageDTO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class SqlLiteStore implements SiteModelStore, FragmentsStore, SpiderStore{
    
    private Context mContext;
    
    private static String mRootDirName;
    
    public static final String DATABASE_FILE = "sandroproxy.db";
    
    // log tag
    protected static final String LOGTAG = "database";
    
    protected static final boolean LOGD = true;
    
    private static final int DATABASE_VERSION = 2;
    
    private static SqlLiteStore mInstance = null;
    
    private static SQLiteDatabase mDatabase = null;
    
    private List<IStoreEventListener> listOfEventListeners;
    
    public static final String mTableNames[] = {
        "conversation", "request", "response", "content", "headers", "cookies", "urls", "websocket_channel", "websocket_message"
    };
    
    // Table ids (they are index to mTableNames)
    public static final int TABLE_COVERSATION_ID = 0;
    public static final int TABLE_REQUEST_ID     = 1;
    public static final int TABLE_RESPONSE_ID    = 2;
    public static final int TABLE_CONTENT_ID     = 3;
    public static final int TABLE_HEADERS_ID     = 4;
    public static final int TABLE_COOKIES_ID     = 5;
    public static final int TABLE_URLS_ID        = 6;
    
    public static final int TABLE_SOCKET_CHANNEL = 7;
    public static final int TABLE_SOCKET_MESSAGE = 8;
    
    // column id strings for "_id" which can be used by any table
    public static final String ID_COL = "_id";
    
    // conversation table columns
    public static final String CONVERSATION_UNIQUE_ID = ID_COL;
    public static final String CONVERSATION_TYPE = "type";
    public static final String CONVERSATION_CLIENT_ADDRESS = "client_ip";
    public static final String CONVERSATION_STATUS = "status";
    public static final String CONVERSATION_STATUS_DESCRIPTION = "status_desc";
    public static final String CONVERSATION_REQUEST_ID = "request_id";
    public static final String CONVERSATION_REQUEST_CHANGED_ID = "request_changed_id";
    public static final String CONVERSATION_RESPONSE_ID = "response_id";
    public static final String CONVERSATION_TS_START = "ts_start";
    public static final String CONVERSATION_TS_END = "ts_end";
    public static final String CONVERSATION_REQ_PATH = "req_path";
    public static final String CONVERSATION_REQ_QUERY_STRING = "req_query_string";
    public static final String CONVERSATION_REQ_SCHEMA = "req_schema";
    public static final String CONVERSATION_REQ_HOST = "req_host";
    public static final String CONVERSATION_REQ_METHOD = "req_method";
    public static final String CONVERSATION_RESP_STATUS_CODE = "resp_status_code";
    public static final String CONVERSATION_RESP_CONTENT_TYPE = "resp_content_type";
    public static final String CONVERSATION_RESP_APPLICATION_TYPE = "resp_app_type";
    public static final String CONVERSATION_RESP_FILE_TYPE = "resp_file_type";
    
    /*
    public static final int CONVERSATION_TYPE_PROXY = 0;
    public static final int CONVERSATION_TYPE_MANUAL = 1;
    public static final int CONVERSATION_TYPE_INTERCEPT = 2;
    
    
    public static final int CONVERSATION_STATUS_NEW = 0;
    public static final int CONVERSATION_STATUS_REQ_SEND = 1;
    public static final int CONVERSATION_STATUS_RESP_RECEIVED = 2;
    */
    
    // request table
    public static final String REQUEST_UNIQUE_ID = ID_COL;
    public static final String REQUEST_TS = "ts";
    public static final String REQUEST_METHOD = "method";
    public static final String REQUEST_HOST = "host";
    public static final String REQUEST_SCHEME = "scheme";
    public static final String REQUEST_QUERY = "query";
    public static final String REQUEST_HEADERS_ID = "headers_id";
    public static final String REQUEST_CONTENT_ID = "content_id";
    public static final String REQUEST_CONTENT_TYPE = "content_type";
    public static final String REQUEST_CONTENT_LENGTH = "content_length";
    public static final String REQUEST_URL = "url";
    public static final String REQUEST_GZIPED = "gziped";
    public static final String REQUEST_CHUNKED = "chunked";
    
    // response table
    public static final String RESPONSE_UNIQUE_ID = ID_COL;
    public static final String RESPONSE_TS = "ts";
    public static final String RESPONSE_STATUS_MESSAGE = "status_message";
    public static final String RESPONSE_STATUS_CODE = "status_code";
    public static final String RESPONSE_HEADERS_ID = "headers_id";
    public static final String RESPONSE_CONTENT_TYPE = "content_type";
    public static final String RESPONSE_CONTENT_LENGTH = "content_length";
    public static final String RESPONSE_CONTENT_ID = "content_id";
    public static final String RESPONSE_GZIPED = "gziped";
    public static final String RESPONSE_CHUNKED = "chunked";
    
    // headers table
    public static final String HEADERS_UNIQUE_ID = ID_COL;
    public static final String HEADERS_PARENT_ID = "parent_id";
    public static final String HEADERS_PARENT_TYPE = "parent_type";
    public static final String HEADERS_NAME = "name";
    public static final String HEADERS_VALUE = "value";
    
    public static final int HEADERS_PARENT_TYPE_REQUEST = 0;
    public static final int HEADERS_PARENT_TYPE_RESPONSE = 1;
    
    // content table
    public static final String CONTENT_UNIQUE_ID = ID_COL;
    public static final String CONTENT_PARENT_ID = "parent_id";
    public static final String CONTENT_PARENT_TYPE = "parent_type";
    public static final String CONTENT_GZIPED = "gziped";
    public static final String CONTENT_CHUNKED = "chunked";
    public static final String CONTENT_FILE_STORE = "file_store";
    public static final String CONTENT_DATA = "data";
    public static final String CONTENT_FILE_NAME = "file_name";
    
    // socket channel
    public static final String SOCKET_CHANNEL_UNIQUE_ID = ID_COL;
    public static final String SOCKET_CHANNEL_ID = "id";
    public static final String SOCKET_CHANNEL_HOST = "host";
    public static final String SOCKET_CHANNEL_PORT = "port";
    public static final String SOCKET_CHANNEL_URL = "url";
    public static final String SOCKET_CHANNEL_START_TIMESTAMP = "start_timestamp";
    public static final String SOCKET_CHANNEL_END_TIMESTAMP = "end_timestamp";
    public static final String SOCKET_CHANNEL_CONV_ID = "conv_id";
    
    // socket message
    public static final String SOCKET_MSG_UNIQUE_ID = ID_COL;
    public static final String SOCKET_MSG_ID = "id";
    public static final String SOCKET_MSG_TIMESTAMP = "timestamp";
    public static final String SOCKET_MSG_OPCODE = "opcode";
    public static final String SOCKET_MSG_PAYLOAD_UTF8 = "payload_utf8";
    public static final String SOCKET_MSG_PAYLOAD_BYTES = "payload_bytes";
    public static final String SOCKET_MSG_PAYLOAD_LENGTH = "payload_length";
    public static final String SOCKET_MSG_IS_OUTGOING = "is_outgoing";
    public static final String SOCKET_MSG_CHANNEL_ID = "channel_id";
    
    
    public static final int CONTENT_PARENT_TYPE_REQUEST = 0;
    public static final int CONTENT_PARENT_TYPE_RESPONSE = 1;
    
    
    public static synchronized SqlLiteStore getInstance(Context context, String rootDirName) {
        if (mInstance == null) {
            mInstance = new SqlLiteStore(context, rootDirName);
            try {
                mDatabase = context
                        .openOrCreateDatabase(DATABASE_FILE, 0, null);
                // mDatabase.setLockingEnabled(false);
                try{
                    mDatabase.execSQL("PRAGMA read_uncommitted = true;");
                    mDatabase.execSQL("PRAGMA synchronous=OFF");
                }catch(Exception ex){
                    Log.e(LOGTAG, ex.getMessage());
                }
                
            } catch (SQLiteException e) {
                // try again by deleting the old db and create a new one
                if (context.deleteDatabase(DATABASE_FILE)) {
                    mDatabase = context.openOrCreateDatabase(DATABASE_FILE, 0,
                            null);
                }
            }
            if (mDatabase != null && mDatabase.getVersion() != DATABASE_VERSION) {
                mDatabase.beginTransaction();
                try {
                    upgradeDatabase();
                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }
            }
            if (mDatabase != null) {
                mDatabase.setLockingEnabled(false);
            }
        }
        if (rootDirName != null && rootDirName.length() > 0 &&  mRootDirName == null){
            mRootDirName = rootDirName;
        }
        return mInstance;
    }
    
    
    private static void createHtmlTables(){
        // conversation
        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_COVERSATION_ID]
                + " (" + CONVERSATION_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + CONVERSATION_REQUEST_ID + " INTEGER, " 
                + CONVERSATION_REQUEST_CHANGED_ID + " INTEGER, " 
                + CONVERSATION_RESPONSE_ID + " INTEGER, " 
                + CONVERSATION_TYPE + " INTEGER, "
                + CONVERSATION_STATUS + " INTEGER, "
                + CONVERSATION_STATUS_DESCRIPTION + " TEXT, "
                + CONVERSATION_CLIENT_ADDRESS + " TEXT, "
                + CONVERSATION_TS_START + " INTEGER, "
                + CONVERSATION_TS_END + " INTEGER, "
                + CONVERSATION_REQ_METHOD + " TEXT, "
                + CONVERSATION_REQ_PATH + " TEXT, "
                + CONVERSATION_REQ_QUERY_STRING + " TEXT, "
                + CONVERSATION_REQ_SCHEMA + " TEXT, "
                + CONVERSATION_REQ_HOST + " TEXT, "
                + CONVERSATION_RESP_STATUS_CODE + " INTEGER, "
                + CONVERSATION_RESP_CONTENT_TYPE + " TEXT, "
                + CONVERSATION_RESP_FILE_TYPE + " TEXT, "
                + CONVERSATION_RESP_APPLICATION_TYPE + " TEXT"
                + ");");
        
//        
//        // add index for timestamp
//        String conversation_index1 = "CREATE INDEX idx_1_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_TS_START 
//                + ");";
//        mDatabase.execSQL(conversation_index1);
//        
//        String conversation_index2 = "CREATE INDEX idx_2_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_TS_END 
//                + ");";
//        mDatabase.execSQL(conversation_index2);
//        
//        String conversation_index3 = "CREATE INDEX idx_3_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_RESP_STATUS_CODE 
//                + ");";
//        mDatabase.execSQL(conversation_index3);
//        
//        // schema index
//        String conversation_index4 = "CREATE INDEX idx_4_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_REQ_SCHEMA
//                + ");";
//        mDatabase.execSQL(conversation_index4);
//        
//        // TODO client address index
//        String conversation_index5 = "CREATE INDEX idx_5_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_CLIENT_ADDRESS
//                + ");";
//        mDatabase.execSQL(conversation_index5);
//        
//        // status index
//        String conversation_index6 = "CREATE INDEX idx_6_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_STATUS
//                + ");";
//        mDatabase.execSQL(conversation_index6);
//        
//        // req host index
//        String conversation_index7 = "CREATE INDEX idx_7_" + mTableNames[TABLE_COVERSATION_ID]  + 
//                " ON " + mTableNames[TABLE_COVERSATION_ID] + "(" 
//                + CONVERSATION_REQ_HOST
//                + ");";
//        mDatabase.execSQL(conversation_index7);
        
        // request
        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_REQUEST_ID]
                + " (" + REQUEST_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + REQUEST_TS  + " INTEGER, "
                + REQUEST_METHOD  + " TEXT, "
                + REQUEST_HOST  + " TEXT, "
                + REQUEST_SCHEME  + " TEXT, "
                + REQUEST_QUERY  + " TEXT, "
                + REQUEST_URL  + " TEXT, "
                + REQUEST_CONTENT_TYPE  + " TEXT, "
                + REQUEST_HEADERS_ID  + " INTEGER, "
                + REQUEST_CONTENT_ID  + " INTEGER, "
                + REQUEST_CONTENT_LENGTH  + " INTEGER, "
                + REQUEST_CHUNKED  + " INTEGER, "
                + REQUEST_GZIPED  + " INTEGER"
                + ");");
//        // add index
//        String request_index1 = "CREATE INDEX idx_1_" + mTableNames[TABLE_REQUEST_ID]  + 
//                " ON " + mTableNames[TABLE_REQUEST_ID] + "(" 
//                + REQUEST_HOST 
//                + ");";
//        mDatabase.execSQL(request_index1);
//        String request_index2 = "CREATE INDEX idx_2_" + mTableNames[TABLE_REQUEST_ID]  + 
//                " ON " + mTableNames[TABLE_REQUEST_ID] + "(" 
//                + REQUEST_CONTENT_TYPE 
//                + ");";
//        mDatabase.execSQL(request_index2);
//        String request_index3 = "CREATE INDEX idx_3_" + mTableNames[TABLE_REQUEST_ID]  + 
//                " ON " + mTableNames[TABLE_REQUEST_ID] + "(" 
//                + REQUEST_TS 
//                + ");";
//        mDatabase.execSQL(request_index3);
//        String request_index4 = "CREATE INDEX idx_4_" + mTableNames[TABLE_REQUEST_ID]  + 
//                " ON " + mTableNames[TABLE_REQUEST_ID] + "(" 
//                + REQUEST_CONTENT_TYPE
//                + ");";
//        mDatabase.execSQL(request_index4);
        
        
        // response
        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_RESPONSE_ID]
                + " (" + RESPONSE_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + RESPONSE_TS  + " INTEGER, "
                + RESPONSE_STATUS_MESSAGE  + " TEXT, " 
                + RESPONSE_STATUS_CODE  + " INTEGER, "
                + RESPONSE_HEADERS_ID  + " INTEGER, "
                + RESPONSE_CHUNKED  + " INTEGER, "
                + RESPONSE_GZIPED  + " INTEGER, "
                + RESPONSE_CONTENT_ID  + " INTEGER, "
                + RESPONSE_CONTENT_TYPE  + " TEXT, "
                + RESPONSE_CONTENT_LENGTH  + " INTEGER"
                + ");");
        
//        // add index
//        String response_index1 = "CREATE INDEX idx_1_" + mTableNames[TABLE_RESPONSE_ID]  + 
//                " ON " + mTableNames[TABLE_RESPONSE_ID] + "(" 
//                + RESPONSE_CONTENT_TYPE
//                + ");";
//        mDatabase.execSQL(response_index1);
//        String response_index2 = "CREATE INDEX idx_2_" + mTableNames[TABLE_RESPONSE_ID]  + 
//                " ON " + mTableNames[TABLE_RESPONSE_ID] + "(" 
//                + RESPONSE_STATUS_CODE
//                + ");";
//        mDatabase.execSQL(response_index2);
//        String response_index3 = "CREATE INDEX idx_3_" + mTableNames[TABLE_RESPONSE_ID]  + 
//                " ON " + mTableNames[TABLE_RESPONSE_ID] + "(" 
//                + RESPONSE_TS
//                + ");";
//        mDatabase.execSQL(response_index3);

        // headers
        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_HEADERS_ID]
                + " (" + HEADERS_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + HEADERS_PARENT_ID  + " INTEGER, "
                + HEADERS_PARENT_TYPE  + " TEXT, "
                + HEADERS_NAME  + " TEXT, " 
                + HEADERS_VALUE  + " TEXT"
                + ");");
//        // add index
//        String headers_index1 = "CREATE INDEX idx_1_" + mTableNames[TABLE_HEADERS_ID]  + 
//                " ON " + mTableNames[TABLE_HEADERS_ID] + "(" 
//                + HEADERS_PARENT_ID
//                + ", "
//                + HEADERS_PARENT_TYPE
//                + ");";
//        mDatabase.execSQL(headers_index1);
//        String headers_index2 = "CREATE INDEX idx_2_" + mTableNames[TABLE_HEADERS_ID]  + 
//                " ON " + mTableNames[TABLE_HEADERS_ID] + "(" 
//                + HEADERS_NAME
//                + ", "
//                + HEADERS_VALUE
//                + ");";
//        mDatabase.execSQL(headers_index2);
//        String headers_index3 = "CREATE INDEX idx_3_" + mTableNames[TABLE_HEADERS_ID]  + 
//                " ON " + mTableNames[TABLE_HEADERS_ID] + "(" 
//                + HEADERS_NAME
//                + ");";
//        mDatabase.execSQL(headers_index3);

        
        // content
        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_CONTENT_ID]
                + " (" + CONTENT_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + CONTENT_PARENT_ID  + " INTEGER, "
                + CONTENT_PARENT_TYPE  + " INTEGER, "
                + CONTENT_GZIPED  + " INTEGER, " 
                + CONTENT_CHUNKED  + " INTEGER, "
                + CONTENT_FILE_STORE  + " INTEGER, "
                + CONTENT_FILE_NAME  + " INTEGER, "
                + CONTENT_DATA  + " BLOB DEFAULT NULL"
                + ");");
//        // add index
//        String content_index1 = "CREATE INDEX idx_1_" + mTableNames[TABLE_CONTENT_ID]  + 
//                " ON " + mTableNames[TABLE_CONTENT_ID] + "(" 
//                + CONTENT_PARENT_ID
//                + ", "
//                + CONTENT_PARENT_TYPE
//                + ");";
//        mDatabase.execSQL(content_index1);

    }
    
    private static void createWebSocketsTables(){
        // socket channel
        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_SOCKET_CHANNEL]
                + " (" + SOCKET_CHANNEL_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + SOCKET_CHANNEL_ID  + " TEXT, "
                + SOCKET_CHANNEL_HOST  + " TEXT, "
                + SOCKET_CHANNEL_PORT  + " INTEGER, "
                + SOCKET_CHANNEL_URL  + " TEXT, " 
                + SOCKET_CHANNEL_START_TIMESTAMP  + " INTEGER, "
                + SOCKET_CHANNEL_END_TIMESTAMP  + " INTEGER, "
                + SOCKET_CHANNEL_CONV_ID  + " INTEGER "
                + ");");

        mDatabase.execSQL("CREATE TABLE " + mTableNames[TABLE_SOCKET_MESSAGE]
                + " (" + SOCKET_MSG_UNIQUE_ID + " INTEGER PRIMARY KEY, "
                + SOCKET_MSG_ID  + " INTEGER, "
                + SOCKET_MSG_CHANNEL_ID  + " INTEGER, "
                + SOCKET_MSG_TIMESTAMP  + " INTEGER, "
                + SOCKET_MSG_OPCODE  + " INTEGER, " 
                + SOCKET_MSG_PAYLOAD_UTF8  + " BLOB, "
                + SOCKET_MSG_PAYLOAD_BYTES  + " BLOB, "
                + SOCKET_MSG_PAYLOAD_LENGTH  + " INTEGER, "
                + SOCKET_MSG_IS_OUTGOING  + " INTEGER "
                + ");");
    }
    
    private static void upgradeDatabase() {
        int oldVersion = mDatabase.getVersion();
        if (oldVersion < 2) {
            Log.i(LOGTAG, "Upgrading database from version "
                    + oldVersion + " to "
                    + DATABASE_VERSION + ", which will destroy old data");
            createWebSocketsTables();
        }
        if (oldVersion < 1){
            createHtmlTables();
        }
        mDatabase.setVersion(DATABASE_VERSION);
    }

    SqlLiteStore(Context context, String rootDirName){
        mContext = context;
        mRootDirName = rootDirName;
    }
    
    public void setEventListeners(List<IStoreEventListener> listOfEventListeners){
        this.listOfEventListeners = listOfEventListeners;
    }
    
    private void eventNewConversation(long conversationId, int type){
        if (listOfEventListeners != null){
            for (IStoreEventListener storeEventListener : listOfEventListeners) {
                storeEventListener.newConversation(conversationId, type);
            }
        }
    }
    
    private void eventUpdateConversation(long conversationId,  int status){
        if (listOfEventListeners != null){
            for (IStoreEventListener storeEventListener : listOfEventListeners) {
                if (status == FrameworkModel.CONVERSATION_STATUS_REQ_SEND){
                    storeEventListener.startConversation(conversationId);
                }
                if (status == FrameworkModel.CONVERSATION_STATUS_RESP_RECEIVED || status == FrameworkModel.CONVERSATION_STATUS_ABORTED){
                    storeEventListener.endConversation(conversationId);
                }
            }
        }
    }
    
    private List<WebSocketChannelDTO> buildChannelDTOs(Cursor cs){
        ArrayList<WebSocketChannelDTO> channels = new ArrayList<WebSocketChannelDTO>();
        while (cs.moveToNext()) {
            WebSocketChannelDTO channel = new WebSocketChannelDTO();
            channel.id = cs.getLong(cs.getColumnIndex(SOCKET_CHANNEL_ID));
            channel.host = cs.getString(cs.getColumnIndex(SOCKET_CHANNEL_HOST));
            channel.port = cs.getInt(cs.getColumnIndex(SOCKET_CHANNEL_PORT));
            channel.url = cs.getString(cs.getColumnIndex(SOCKET_CHANNEL_URL));
            channel.startTimestamp = cs.getLong(cs.getColumnIndex(SOCKET_CHANNEL_START_TIMESTAMP));
            channel.endTimestamp = cs.getLong(cs.getColumnIndex(SOCKET_CHANNEL_END_TIMESTAMP));
            
            channel.historyId = cs.getInt(cs.getColumnIndex(SOCKET_CHANNEL_CONV_ID));
            
            channels.add(channel);
        }
        return channels;
    }
    
    public List<WebSocketChannelDTO> getSocketChannels(){
        List<WebSocketChannelDTO> channelList = new ArrayList<WebSocketChannelDTO>();
        Cursor cs = mDatabase.query(mTableNames[TABLE_SOCKET_CHANNEL], null, null, null, null, null, null);
        if (cs != null){
            try{
                return buildChannelDTOs(cs);
            } catch (Exception ex){
                ex.printStackTrace();
            } finally{
                cs.close();
            }
            
        }
        return channelList;
    }
    
    private List<WebSocketMessageDTO> buildChannelMessagesDTOs(Cursor cs){
        ArrayList<WebSocketMessageDTO> messages = new ArrayList<WebSocketMessageDTO>();
        while (cs.moveToNext()) {
            WebSocketMessageDTO message = new WebSocketMessageDTO();
            message.id = cs.getInt(cs.getColumnIndex(SOCKET_MSG_ID));
            message.opcode = cs.getInt(cs.getColumnIndex(SOCKET_MSG_OPCODE));
            message.payloadLength = cs.getInt(cs.getColumnIndex(SOCKET_MSG_PAYLOAD_LENGTH));
            
            if (message.opcode == WebSocketMessage.OPCODE_TEXT){
                byte[] payloadAsString = cs.getBlob(cs.getColumnIndex(SOCKET_MSG_PAYLOAD_UTF8)); 
                message.payload = new String(payloadAsString);
            }else{
                message.payload = cs.getBlob(cs.getColumnIndex(SOCKET_MSG_PAYLOAD_BYTES)); 
            }
            message.isOutgoing = cs.getInt(cs.getColumnIndex(SOCKET_MSG_IS_OUTGOING)) == 1 ? true : false;
            message.timestamp = cs.getLong(cs.getColumnIndex(SOCKET_MSG_TIMESTAMP)); 
            message.readableOpcode = WebSocketMessage.opcode2string(message.opcode);
            messages.add(message);
        }
        return messages;
    }
    
    public List<WebSocketMessageDTO> getSocketChannelMessages(long channelId){
        List<WebSocketMessageDTO> listMessages = new ArrayList<WebSocketMessageDTO>();
        String selection = SOCKET_MSG_CHANNEL_ID + " = ? ";
        String [] args = new String[]{ String.valueOf(channelId)};
        Cursor cs = mDatabase.query(mTableNames[TABLE_SOCKET_MESSAGE], null, selection, args, null, null, null);
        if (cs != null){
            try{
                return buildChannelMessagesDTOs(cs);
            } catch (Exception ex){
                ex.printStackTrace();
            } finally{
                cs.close();
            }
            
        }
        return listMessages;
    }
    
    
    public void insertMessage(WebSocketMessageDTO message) throws Exception {
        ContentValues msgVal = new ContentValues();
        msgVal.put(SOCKET_MSG_CHANNEL_ID, message.channel.id);
        msgVal.put(SOCKET_MSG_ID, message.id);
        msgVal.put(SOCKET_MSG_IS_OUTGOING, message.isOutgoing);
        msgVal.put(SOCKET_MSG_OPCODE, message.opcode);
        msgVal.put(SOCKET_MSG_TIMESTAMP, message.timestamp);
        if (message.payload instanceof String){
            msgVal.put(SOCKET_MSG_PAYLOAD_UTF8, message.getReadablePayload());
        }else{
            msgVal.put(SOCKET_MSG_PAYLOAD_UTF8, (byte[])message.payload);
        }
        msgVal.put(SOCKET_MSG_PAYLOAD_LENGTH, message.payloadLength);
        mDatabase.insertOrThrow(mTableNames[TABLE_SOCKET_MESSAGE], null, msgVal);
    }
    
    
    private List<Long> channelsIds = new ArrayList<Long>();
    
    public void insertOrUpdateChannel(WebSocketChannelDTO channel) throws SQLException {
        ContentValues msgVal = new ContentValues();
        msgVal.put(SOCKET_CHANNEL_ID, channel.id);
        msgVal.put(SOCKET_CHANNEL_END_TIMESTAMP, channel.endTimestamp);
        msgVal.put(SOCKET_CHANNEL_START_TIMESTAMP, channel.startTimestamp);
        msgVal.put(SOCKET_CHANNEL_HOST, channel.host);
        msgVal.put(SOCKET_CHANNEL_PORT, channel.port);
        msgVal.put(SOCKET_CHANNEL_URL, channel.url);
        
        synchronized (this) {
            // TODO fill chanellIds from existing in database;
            if (channelsIds.contains(channel.id)){
                mDatabase.update(mTableNames[TABLE_SOCKET_CHANNEL], msgVal, " id = ?", new String[]{ String.valueOf(channel.id) });
            }else{
                channelsIds.add(channel.id);
                mDatabase.insertOrThrow(mTableNames[TABLE_SOCKET_CHANNEL], null, msgVal);
            }
        }
    }
    
    public void purgeChannel(Long channelId) throws SQLException {
        // TODO 
        if (LOGD) Log.d(LOGTAG, "purge websocket channels");
    }
    
    private final Object mConversationLock = new Object();
    
    private void addHeaders(long id, Message message, int headersParentType){
        NamedValue[] requestHeaders =  message.getHeaders();
        if (requestHeaders != null && requestHeaders.length > 0){
            for (NamedValue namedValue : requestHeaders) {
                ContentValues reqHeadersCV = new ContentValues();
                reqHeadersCV.put(HEADERS_PARENT_TYPE, headersParentType);
                reqHeadersCV.put(HEADERS_PARENT_ID, id);
                reqHeadersCV.put(HEADERS_NAME, namedValue.getName());
                reqHeadersCV.put(HEADERS_VALUE, namedValue.getValue());
                long headerId = mDatabase.insertOrThrow(mTableNames[TABLE_HEADERS_ID], 
                        null, reqHeadersCV);
            }
        }
    }
    
    public void clearDatabase(){
        // clear all tables
        synchronized (mConversationLock) {
            String where = "1";
            mDatabase.delete(mTableNames[TABLE_COVERSATION_ID], where, null);
            mDatabase.delete(mTableNames[TABLE_HEADERS_ID], where, null);
            mDatabase.delete(mTableNames[TABLE_REQUEST_ID], where, null);
            mDatabase.delete(mTableNames[TABLE_RESPONSE_ID], where, null);
            mDatabase.delete(mTableNames[TABLE_CONTENT_ID], where, null);
            mDatabase.delete(mTableNames[TABLE_SOCKET_CHANNEL], where, null);
            mDatabase.delete(mTableNames[TABLE_SOCKET_MESSAGE], where, null);
        }
    }
    
    private long addContent(long id, Message message, int contentParentType, String fileName) throws Exception{
        ContentValues reqContentCV = new ContentValues();
        reqContentCV.put(CONTENT_PARENT_ID, id);
        reqContentCV.put(CONTENT_PARENT_TYPE, contentParentType);
        reqContentCV.put(CONTENT_GZIPED, message.isCompressed());
        reqContentCV.put(CONTENT_CHUNKED, message.isChunked());
        // TODO we need that message object return if we have file store or memory store
        String contentFileName = fileName;
        if (message.moveContentToFile(new File(contentFileName))){
            reqContentCV.put(CONTENT_FILE_NAME, contentFileName);
            reqContentCV.put(CONTENT_FILE_STORE, true);
        }else{
            reqContentCV.put(CONTENT_FILE_STORE, false);
        }
        
        long contentId = mDatabase.insertOrThrow(mTableNames[TABLE_CONTENT_ID], 
                null, reqContentCV);
        return contentId;
    }
    

    @Override
    public long createNewConversation(Date when, int type, String clientAddress){
        ContentValues convCV = new ContentValues();
        convCV.put(CONVERSATION_STATUS, FrameworkModel.CONVERSATION_STATUS_NEW);
        convCV.put(CONVERSATION_TYPE, type);
        convCV.put(CONVERSATION_TS_START, when.getTime());
        convCV.put(CONVERSATION_CLIENT_ADDRESS, clientAddress);
        long conversationId = mDatabase.insertOrThrow(mTableNames[TABLE_COVERSATION_ID], 
                null, convCV);
        eventNewConversation(conversationId, type);
        return conversationId;
    }
    
    
    @Override
    public long updateFailedConversation(long  conversationId, Date when, Request request, String reason){
        int conversationStatus = FrameworkModel.CONVERSATION_STATUS_ABORTED;
        boolean haveValidData = false;
        try{
            mDatabase.beginTransaction();
            ContentValues convCV = new ContentValues();
            
            if (request != null){
                long requestId = insertRequest(request, "txt");
                convCV.put(CONVERSATION_REQUEST_CHANGED_ID, requestId);
                convCV.put(CONVERSATION_REQ_METHOD, request.getMethod());
                convCV.put(CONVERSATION_REQ_PATH, request.getURL().getPath());
                convCV.put(CONVERSATION_REQ_QUERY_STRING, request.getURL().getQuery());
                convCV.put(CONVERSATION_REQ_SCHEMA, request.getURL().getScheme());
                convCV.put(CONVERSATION_REQ_HOST, request.getURL().getHost());
            }
            
            convCV.put(CONVERSATION_STATUS, conversationStatus);
            convCV.put(CONVERSATION_STATUS_DESCRIPTION, reason);
            convCV.put(CONVERSATION_TS_END, when.getTime());

            String where = CONVERSATION_UNIQUE_ID  + " = ?";
            String[] args = new String[]{ String.valueOf(conversationId)};
            long updatedRows = mDatabase.update(mTableNames[TABLE_COVERSATION_ID], convCV, where, args);
            mDatabase.setTransactionSuccessful();
            haveValidData = true;
            return updatedRows;
        }catch(Exception e){
            Log.e(LOGTAG, "Error on addConversation" + e.getMessage());
        } finally {
            mDatabase.endTransaction();
            if (haveValidData){
                eventUpdateConversation(conversationId, conversationStatus);
            }
        }
        return -1;
    }
    
    @Override
    public long updateGotRequestConversation(long  conversationId, Date when, Request request){
        long requestId = -1;
        boolean haveValidData = false;
        int conversationStatus = FrameworkModel.CONVERSATION_STATUS_REQ_SEND;
        try{
            mDatabase.beginTransaction();
            ContentValues convCV = new ContentValues();
            requestId = insertRequest(request, "txt");
            
            convCV.put(CONVERSATION_REQUEST_ID, requestId);
            convCV.put(CONVERSATION_REQUEST_CHANGED_ID, -1);
            convCV.put(CONVERSATION_REQ_METHOD, request.getMethod());
            convCV.put(CONVERSATION_REQ_PATH, request.getURL().getPath());
            convCV.put(CONVERSATION_REQ_QUERY_STRING, request.getURL().getQuery());
            convCV.put(CONVERSATION_REQ_SCHEMA, request.getURL().getScheme());
            convCV.put(CONVERSATION_REQ_HOST, request.getURL().getHost());
            convCV.put(CONVERSATION_STATUS, conversationStatus);

            String where = CONVERSATION_UNIQUE_ID  + " = ?";
            String[] args = new String[]{ String.valueOf(conversationId)};
            long updatedRows = mDatabase.update(mTableNames[TABLE_COVERSATION_ID], convCV, where, args);
            mDatabase.setTransactionSuccessful();
            haveValidData = true;
            return updatedRows;
        }catch(Exception e){
            Log.e(LOGTAG, "Error on updateGotRequestConversation" + e.getMessage());
        } finally {
            mDatabase.endTransaction();
            if (haveValidData){
                eventUpdateConversation(conversationId, conversationStatus);
            }
        }
        return -1;
    }
    
    
    @Override
    public long updateGotResponseConversation(long  conversationId, Date when, Request request, Response response){
        long requestId = -1;
        long responseId = -1;
        boolean haveValidData = false;
        int conversationStatus = FrameworkModel.CONVERSATION_STATUS_ABORTED;
        try{
            mDatabase.beginTransaction();
            ContentValues convCV = new ContentValues();
            if (request != null){
                requestId = insertRequest(request, "txt");
                convCV.put(CONVERSATION_REQUEST_CHANGED_ID, requestId);
                convCV.put(CONVERSATION_REQ_METHOD, request.getMethod());
                convCV.put(CONVERSATION_REQ_PATH, request.getURL().getPath());
                convCV.put(CONVERSATION_REQ_QUERY_STRING, request.getURL().getQuery());
                convCV.put(CONVERSATION_REQ_SCHEMA, request.getURL().getScheme());
                convCV.put(CONVERSATION_REQ_HOST, request.getURL().getHost());
            }
            if (response != null){
                // add file type
                String fileType = "txt";
                String applicationType = "text";
                if (response.getHeader("Content-Type") != null){
                    
                    String contentTypeString = response.getHeader("Content-Type");
                    String[] contentItemsFirst = contentTypeString.split(";");
                    String[] contentItemsSecond = contentItemsFirst[0].split("\\+");
                    if (contentItemsSecond.length > 0){
                        fileType = contentItemsSecond[0];
                        String[] contentTypeParts =  fileType.split("/");
                        applicationType = contentTypeParts[0];
                        if (contentTypeParts.length > 1){
                            fileType = contentTypeParts[1];
                        }
                    }
                }
                convCV.put(CONVERSATION_RESP_FILE_TYPE, fileType);
                convCV.put(CONVERSATION_RESP_APPLICATION_TYPE, applicationType);
                responseId = insertResponse(response, fileType);
                convCV.put(CONVERSATION_RESPONSE_ID, responseId);
                convCV.put(CONVERSATION_RESP_STATUS_CODE, response.getStatus());
                convCV.put(CONVERSATION_RESP_CONTENT_TYPE, response.getHeader("Content-Type"));
                convCV.put(CONVERSATION_TS_END, when.getTime());
                conversationStatus = FrameworkModel.CONVERSATION_STATUS_RESP_RECEIVED;
            }
            convCV.put(CONVERSATION_STATUS, conversationStatus);

            String where = CONVERSATION_UNIQUE_ID  + " = ?";
            String[] args = new String[]{ String.valueOf(conversationId)};
            long updatedRows = mDatabase.update(mTableNames[TABLE_COVERSATION_ID], convCV, where, args);
            mDatabase.setTransactionSuccessful();
            haveValidData = true;
            return updatedRows;
        }catch(Exception e){
            Log.e(LOGTAG, "Error on addConversation" + e.getMessage());
        } finally {
            mDatabase.endTransaction();
            if (haveValidData){
                eventUpdateConversation(conversationId, conversationStatus);
            }
        }
        return -1;
    }
    
    
    private long insertRequest(Request request, String fileType) throws Exception{
        
        String fileStorageDir = mRootDirName + "/";
        // create request row
        ContentValues reqCV = new ContentValues();
        reqCV.put(REQUEST_METHOD, request.getMethod());
        reqCV.put(REQUEST_URL, request.getURL().toString());
        reqCV.put(REQUEST_HOST, request.getURL().getHost());
        reqCV.put(REQUEST_SCHEME, request.getURL().getScheme());
        reqCV.put(REQUEST_QUERY, request.getURL().getQuery());
        reqCV.put(REQUEST_GZIPED, request.isCompressed());
        reqCV.put(REQUEST_CHUNKED, request.isChunked());
        
        long requestId = mDatabase.insertOrThrow(mTableNames[TABLE_REQUEST_ID], 
                                        null, reqCV);
        addHeaders(requestId, request, HEADERS_PARENT_TYPE_REQUEST);
        addContent(requestId, request, CONTENT_PARENT_TYPE_REQUEST, fileStorageDir  + requestId + "-request-content." + fileType);
        return requestId;
        
    }
    
    private long insertResponse(Response response, String fileType) throws Exception{
        
        String fileStorageDir = mRootDirName + "/";
        // create response row
        ContentValues resCV = new ContentValues();
        resCV.put(RESPONSE_STATUS_MESSAGE, response.getMessage());
        resCV.put(RESPONSE_STATUS_CODE, response.getStatus());
        resCV.put(RESPONSE_CHUNKED, response.isChunked());
        resCV.put(RESPONSE_GZIPED, response.isCompressed());
        
        long responseId = mDatabase.insertOrThrow(mTableNames[TABLE_RESPONSE_ID], 
                null, resCV);
        
        // create response headers
        addHeaders(responseId, response, HEADERS_PARENT_TYPE_RESPONSE);
        
        String fileName  = fileStorageDir  + responseId + "-response-content." + fileType;
        addContent(responseId, response, CONTENT_PARENT_TYPE_RESPONSE, fileName);
        return responseId;
    }
    
    @Override
    public int addConversation(ConversationID id, Date when, Request request,
            Response response) {
            String fileStorageDir = mRootDirName + "/";
            try{
                
                // create request row
                ContentValues reqCV = new ContentValues();
                reqCV.put(REQUEST_METHOD, request.getMethod());
                reqCV.put(REQUEST_URL, request.getURL().toString());
                reqCV.put(REQUEST_HOST, request.getURL().getHost());
                reqCV.put(REQUEST_SCHEME, request.getURL().getScheme());
                reqCV.put(REQUEST_QUERY, request.getURL().getQuery());
                reqCV.put(REQUEST_GZIPED, request.isCompressed());
                reqCV.put(REQUEST_CHUNKED, request.isChunked());
                
                mDatabase.beginTransaction();
                long requestId = mDatabase.insertOrThrow(mTableNames[TABLE_REQUEST_ID], 
                                                null, reqCV);
                addHeaders(requestId, request, HEADERS_PARENT_TYPE_REQUEST);
                addContent(requestId, request, CONTENT_PARENT_TYPE_REQUEST, fileStorageDir  + id + "-request-content");
                
                // create response row
                ContentValues resCV = new ContentValues();
                resCV.put(RESPONSE_STATUS_MESSAGE, response.getMessage());
                resCV.put(RESPONSE_STATUS_CODE, response.getStatus());
                resCV.put(RESPONSE_CHUNKED, response.isChunked());
                resCV.put(RESPONSE_GZIPED, response.isCompressed());
                
                long responseId = mDatabase.insertOrThrow(mTableNames[TABLE_RESPONSE_ID], 
                        null, resCV);
                
                // create response headers
                addHeaders(responseId, response, HEADERS_PARENT_TYPE_RESPONSE);
                
                String fileName  = fileStorageDir  + id + "-response-content";
                addContent(responseId, response, CONTENT_PARENT_TYPE_RESPONSE, fileName);
                
                // create conversation row
                ContentValues convCV = new ContentValues();
                convCV.put(CONVERSATION_REQUEST_ID, requestId);
                convCV.put(CONVERSATION_RESPONSE_ID, responseId);
                convCV.put(CONVERSATION_RESP_STATUS_CODE, response.getStatus());
                convCV.put(CONVERSATION_RESP_CONTENT_TYPE, response.getHeader("Content-Type"));
                convCV.put(CONVERSATION_REQ_METHOD, request.getMethod());
                convCV.put(CONVERSATION_REQ_PATH, request.getURL().getPath());
                convCV.put(CONVERSATION_REQ_QUERY_STRING, request.getURL().getQuery());
                convCV.put(CONVERSATION_TS_START, when.getTime());
                convCV.put(CONVERSATION_TS_END, when.getTime());
                long conversationId = mDatabase.insertOrThrow(mTableNames[TABLE_COVERSATION_ID], 
                        null, convCV);
                mDatabase.setTransactionSuccessful();
                return (int)conversationId;
            }catch(Exception e){
                Log.e(LOGTAG, "Error on addConversation" + e.getMessage());
            } finally {
                mDatabase.endTransaction();
            }
        return 0;
    }
    
    private Request createRequest(Cursor cs){
        Request req = new Request();
        req.setMethod(cs.getString(cs.getColumnIndex(REQUEST_METHOD)));
        String url = cs.getString(cs.getColumnIndex(REQUEST_URL));
        try {
            req.setURL(new HttpUrl(url));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return req;
    }
    
    public Request getRequest(long requestId){
        Cursor cs = null;
        try{
            String where = REQUEST_UNIQUE_ID + " = ?";
            String[] arg = new String[] {String.valueOf(requestId)};
            cs = mDatabase.query(mTableNames[TABLE_REQUEST_ID], null, where, arg, null, null, null);
            if (cs.moveToFirst()){
                Request request = createRequest(cs);
                // fill headers
                NamedValue[] headers = getHeaders(requestId, HEADERS_PARENT_TYPE_REQUEST);
                request.setHeaders(headers);
                // get content fileName
                String fileName = geContentFileName(requestId, HEADERS_PARENT_TYPE_REQUEST);
                request.setContentFileName(fileName);
                return request;
            }
            
        } finally{
            if (cs != null) cs.close();
        }
        return null;
    }
    
    private Response createResponse(Cursor cs){
        Response resp = new Response();
        resp.setStatus(cs.getString(cs.getColumnIndex(RESPONSE_STATUS_CODE)));
        resp.setMessage(cs.getString(cs.getColumnIndex(RESPONSE_STATUS_MESSAGE)));
        return resp;
    }
    
    public Response getResponseByRequestId(long requestId){
        Cursor cs = null;
        Cursor csConversation = null;
        try{
            String whereConv = CONVERSATION_REQUEST_ID + " = ?";
            String[] argConv = new String[] {String.valueOf(requestId)};
            csConversation = mDatabase.query(mTableNames[TABLE_COVERSATION_ID], null, whereConv, argConv, null, null, null);
            long responseId = -1;
            if (csConversation.moveToFirst()){
                responseId = csConversation.getLong(csConversation.getColumnIndex(CONVERSATION_RESPONSE_ID));
            }
            String where = RESPONSE_UNIQUE_ID + " = ?";
            String[] arg = new String[] {String.valueOf(responseId)};
            cs = mDatabase.query(mTableNames[TABLE_RESPONSE_ID], null, where, arg, null, null, null);
            if (cs.moveToFirst()){
                Response response = createResponse(cs);
                // fill headers
                NamedValue[] headers = getHeaders(responseId, HEADERS_PARENT_TYPE_RESPONSE);
                response.setHeaders(headers);
                // get content fileName
                String fileName = geContentFileName(responseId, HEADERS_PARENT_TYPE_RESPONSE);
                response.setContentFileName(fileName);
                return response;
            }
        } finally{
            if (csConversation != null) csConversation.close();
            if (cs != null) cs.close();
        }
        return null;
    }
    
    public Response getResponse(long responseId){
        Cursor cs = null;
        try{
            String where = RESPONSE_UNIQUE_ID + " = ?";
            String[] arg = new String[] {String.valueOf(responseId)};
            cs = mDatabase.query(mTableNames[TABLE_RESPONSE_ID], null, where, arg, null, null, null);
            if (cs.moveToFirst()){
                Response response = createResponse(cs);
                // fill headers
                NamedValue[] headers = getHeaders(responseId, HEADERS_PARENT_TYPE_RESPONSE);
                response.setHeaders(headers);
                // get content fileName
                String fileName = geContentFileName(responseId, HEADERS_PARENT_TYPE_RESPONSE);
                response.setContentFileName(fileName);
                return response;
            }
        } finally{
            if (cs != null) cs.close();
        }
        return null;
    }
    
    public NamedValue[] getHeaders(long parentId, int parentType){
        Cursor cs = null;
        try{
            String where = HEADERS_PARENT_ID + " = ? AND " + HEADERS_PARENT_TYPE + " = ?";
            String[] arg = new String[] {String.valueOf(parentId), String.valueOf(parentType)};
            cs = mDatabase.query(mTableNames[TABLE_HEADERS_ID], null, where, arg, null, null, null);
            NamedValue[] values = new NamedValue[cs.getCount()];
            int i = 0;
            while(cs.moveToNext()){
                String name = cs.getString(cs.getColumnIndex(HEADERS_NAME));
                String value = cs.getString(cs.getColumnIndex(HEADERS_VALUE));
                values[i++] = new NamedValue(name, value);
            }
            return values;
        } finally{
            if (cs != null) cs.close();
        }
    }
    
    private String geContentFileName(long parentId, int parentType){
        Cursor cs = null;
        try{
            String where = CONTENT_PARENT_ID + " = ? AND " + CONTENT_PARENT_TYPE + " = ?";
            String[] arg = new String[] {String.valueOf(parentId), String.valueOf(parentType)};
            cs = mDatabase.query(mTableNames[TABLE_CONTENT_ID], null, where, arg, null, null, null);
            if(cs.moveToFirst()){
                String fileName = cs.getString(cs.getColumnIndex(CONTENT_FILE_NAME));
                return fileName;
            }
        } finally{
            if (cs != null) cs.close();
        }
        return null;
    }
    
    
    private Conversation createConversationObj(Cursor cs){
        Conversation conv = new Conversation();
        conv.UNIQUE_ID = cs.getLong(cs.getColumnIndex(CONVERSATION_UNIQUE_ID));
        conv.REQUEST_ID = cs.getLong(cs.getColumnIndex(CONVERSATION_REQUEST_ID));
        conv.REQUEST_CHANGED_ID = cs.getLong(cs.getColumnIndex(CONVERSATION_REQUEST_CHANGED_ID));
        conv.REQ_METHOD = cs.getString(cs.getColumnIndex(CONVERSATION_REQ_METHOD));
        conv.REQ_PATH = cs.getString(cs.getColumnIndex(CONVERSATION_REQ_PATH));
        conv.REQ_QUERY_STRING = cs.getString(cs.getColumnIndex(CONVERSATION_REQ_QUERY_STRING));
        conv.REQ_SCHEMA = cs.getString(cs.getColumnIndex(CONVERSATION_REQ_SCHEMA));
        conv.REQ_HOST = cs.getString(cs.getColumnIndex(CONVERSATION_REQ_HOST));
        conv.RESPONSE_ID = cs.getLong(cs.getColumnIndex(CONVERSATION_RESPONSE_ID));
        conv.RESP_STATUS_CODE = cs.getLong(cs.getColumnIndex(CONVERSATION_RESP_STATUS_CODE));
        conv.RESP_CONTENT_TYPE = cs.getString(cs.getColumnIndex(CONVERSATION_RESP_CONTENT_TYPE));
        conv.RESP_FILE_TYPE = cs.getString(cs.getColumnIndex(CONVERSATION_RESP_FILE_TYPE));
        conv.RESP_APP_TYPE = cs.getString(cs.getColumnIndex(CONVERSATION_RESP_APPLICATION_TYPE));
        conv.TS_START = cs.getLong(cs.getColumnIndex(CONVERSATION_TS_START));
        conv.TS_END = cs.getLong(cs.getColumnIndex(CONVERSATION_TS_END));
        conv.STATUS = cs.getInt(cs.getColumnIndex(CONVERSATION_STATUS));
        conv.STATUS_DESC = cs.getString(cs.getColumnIndex(CONVERSATION_STATUS_DESCRIPTION));
        conv.TYPE = cs.getInt(cs.getColumnIndex(CONVERSATION_TYPE));
        conv.CLIENT_ADDRESS = cs.getString(cs.getColumnIndex(CONVERSATION_CLIENT_ADDRESS));
        
        return conv;
    }
    
    public List<Conversation> getConversation(Date dateFrom, Date dateTo, String statusCode, String contentType, String orderBy){
        List<Conversation> list = new ArrayList<Conversation>();
        Cursor cs = null;
        try{
            ConversationFilter filter = ConversationFilter.createTransFilter(dateFrom, dateTo, statusCode, contentType, orderBy);
            cs = mDatabase.query(mTableNames[TABLE_COVERSATION_ID], null, filter.whereClause, filter.getArgs(), null, null, filter.orderBy);
            while (cs.moveToNext()){
                Conversation conv = createConversationObj(cs);
                list.add(conv);
            }
        } finally{
            if (cs != null) cs.close();
        }
        return list;
    }
    
    public List<Long> getConversationIds(Date dateFrom, Date dateTo, String statusCode, String contentType, String orderBy){
        List<Long> list = new ArrayList<Long>();
        Cursor cs = null;
        try{
            ConversationFilter filter = ConversationFilter.createTransFilter(dateFrom, dateTo, statusCode, contentType, orderBy);
            cs = mDatabase.query(mTableNames[TABLE_COVERSATION_ID], null, filter.whereClause, filter.getArgs(), null, null, filter.orderBy);
            while (cs.moveToNext()){
                long id = cs.getLong(cs.getColumnIndex(CONVERSATION_UNIQUE_ID));
                list.add(id);
            }
        } finally{
            if (cs != null) cs.close();
        }
        return list;
    }
    
    
    
    public Conversation getConversation(long conversationId){
        Cursor cs = null;
        try{
            String where = CONVERSATION_UNIQUE_ID + " = ?";
            String[] args = new String[] {String.valueOf(conversationId)};
            cs = mDatabase.query(mTableNames[TABLE_COVERSATION_ID], null, where, args, null, null, null);
            if (cs.moveToFirst()){
                Conversation conv = createConversationObj(cs);
                return conv;
            }
        } finally{
            if (cs != null) cs.close();
        }
        return null;
    }
    
    @Override
    public void setConversationProperty(ConversationID id, String property,
            String value) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean addConversationProperty(ConversationID id, String property,
            String value) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public String[] getConversationProperties(ConversationID id, String property) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getIndexOfConversation(HttpUrl url, ConversationID id) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public int getConversationCount(HttpUrl url) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public ConversationID getConversationAt(HttpUrl url, int index) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void addUrl(HttpUrl url) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean isKnownUrl(HttpUrl url) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public void setUrlProperty(HttpUrl url, String property, String value) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean addUrlProperty(HttpUrl url, String property, String value) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public String[] getUrlProperties(HttpUrl url, String property) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getChildCount(HttpUrl url) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public HttpUrl getChildAt(HttpUrl url, int index) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getIndexOf(HttpUrl url) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public void setRequest(ConversationID id, Request request) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Request getRequest(ConversationID id) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void setResponse(ConversationID id, Response response) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Response getResponse(ConversationID id) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getCookieCount() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public int getCookieCount(String key) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public String getCookieAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Cookie getCookieAt(String key, int index) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Cookie getCurrentCookie(String key) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getIndexOfCookie(Cookie cookie) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public int getIndexOfCookie(String key, Cookie cookie) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public boolean addCookie(Cookie cookie) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean removeCookie(Cookie cookie) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public void flush() throws StoreException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeUnseenLinks(Link[] links) throws StoreException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Link[] readUnseenLinks() throws StoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeSeenLinks(String[] links) throws StoreException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String[] readSeenLinks() throws StoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFragmentTypeCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFragmentType(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFragmentCount(String type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFragmentKeyAt(String type, int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int indexOfFragment(String type, String key) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int putFragment(String type, String key, String fragment) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFragment(String key) {
        // TODO Auto-generated method stub
        return null;
    }
}
