package org.sandroproxy.webscarab.store.sql;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class WebSocketChannelFilter{
    public String whereClause;
    public Map<String, String> arg;
    public String orderBy;
    
    public WebSocketChannelFilter(){
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
    
    public static WebSocketChannelFilter createTransFilter(long dateFrom, long dateTo, String url, String port, String orderBy)
    {
        WebSocketChannelFilter filter = new WebSocketChannelFilter();
        filter.whereClause = SqlLiteStore.SOCKET_CHANNEL_UNIQUE_ID +" > -1";
        if (dateFrom != -1){
            long tsFrom = dateFrom;
            String strTsFrom = Long.toString(tsFrom);
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_CHANNEL_START_TIMESTAMP + " >= ?";
            filter.arg.put("dateFrom", strTsFrom);
        }
        if (dateTo != -1){
            long tsTo = dateTo;
            String strTsTo = Long.toString(tsTo);
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_CHANNEL_START_TIMESTAMP + " <= ?";
            filter.arg.put("dateTo", strTsTo);
        }
        if (url != null){
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_CHANNEL_URL + " = ?";
            filter.arg.put("url", url);
        }
        if (port != null){
            filter.whereClause += " AND " + SqlLiteStore.SOCKET_CHANNEL_PORT + " = ?";
            filter.arg.put("port", port);
        }
        filter.orderBy = orderBy;
        return filter;
    }
}
