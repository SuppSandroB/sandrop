package org.sandroproxy.webscarab.store.sql;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class ConversationFilter{
    public String whereClause;
    public Map<String, String> arg;
    public String orderBy;
    
    public ConversationFilter(){
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
    
    public static ConversationFilter createTransFilter(Date dateFrom, Date dateTo, String statusCode, String contentType, String orderBy)
    {
        ConversationFilter filter = new ConversationFilter();
        filter.whereClause = SqlLiteStore.CONVERSATION_UNIQUE_ID +" > -1";
        if (dateFrom != null){
            long tsFrom = dateFrom.getTime();
            String strTsFrom = Long.toString(tsFrom);
            filter.whereClause += " AND " + SqlLiteStore.CONVERSATION_TS_END + " >= ?";
            filter.arg.put("dateFrom", strTsFrom);
        }
        if (dateTo != null){
            long tsTo = dateTo.getTime();
            String strTsTo = Long.toString(tsTo);
            filter.whereClause += " AND " + SqlLiteStore.CONVERSATION_TS_END + " <= ?";
            filter.arg.put("dateTo", strTsTo);
        }
        if (statusCode != null){
            filter.whereClause += " AND " + SqlLiteStore.CONVERSATION_RESP_STATUS_CODE + " = ?";
            filter.arg.put("statusCode", statusCode);
        }
        if (contentType != null){
            filter.whereClause += " AND " + SqlLiteStore.CONVERSATION_RESP_CONTENT_TYPE + " = ?";
            filter.arg.put("contentType", contentType);
        }
        filter.orderBy = orderBy;
        return filter;
    }
}
