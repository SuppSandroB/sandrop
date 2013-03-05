package org.sandroproxy.webscarab.store.sql;

import java.util.HashMap;
import java.util.Map;


public class WebSocketMessagesFilter{
    public String whereClause;
    public Map<String, String> arg;
    public String orderBy;
    
    public WebSocketMessagesFilter(){
        whereClause = "1 = 1";
        arg = new HashMap<String, String>();
        orderBy = null;
    }
    
    public String[] getArgs(){
        if (arg.size() == 0){
            return null;
        } 
        String[] arrArgs = new String[arg.size()];
        int index = 0;
        for (Map.Entry<String, String> mapEntry : arg.entrySet()) {
            arrArgs[index] = mapEntry.getValue();
            index++;
        }
        return arrArgs;
    }
    
    public static WebSocketMessagesFilter createTransFilter(Long dateFrom, Long dateTo, Long channelId, Long historyId, String orderBy)
    {
        WebSocketMessagesFilter filter = new WebSocketMessagesFilter();
        filter.whereClause = SqlLiteStore.SOCKET_MSG_UNIQUE_ID +" > -1";
        if (dateFrom != null){
            long tsFrom = dateFrom;
            String strTsFrom = Long.toString(tsFrom);
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_MSG_TIMESTAMP + " >= ?";
            filter.arg.put("dateFrom", strTsFrom);
        }
        if (dateTo != null){
            long tsTo = dateTo;
            String strTsTo = Long.toString(tsTo);
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_MSG_TIMESTAMP + " <= ?";
            filter.arg.put("dateTo", strTsTo);
        }
        if (channelId != null){
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_MSG_CHANNEL_ID + " = ?";
            String strChannelId = Long.toString(channelId);
            filter.arg.put("channelId", strChannelId);
        }
        if (historyId != null){
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_MSG_HANDSHAKE_ID + " = ?";
            String strHistoryId = Long.toString(historyId);
            filter.arg.put("historyId", strHistoryId);
        }
        filter.orderBy = orderBy;
        return filter;
    }
}
