package org.sandroproxy.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.model.FrameworkModel;
import org.sandrop.webscarab.model.Message;
import org.sandrop.webscarab.model.MessageOutputStream;
import org.sandrop.webscarab.model.NamedValue;
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandroproxy.webscarab.store.sql.Conversation;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;
import org.sandrop.websockets.WebSocketMessage;
import org.sandrop.websockets.WebSocketMessageDTO;

import android.content.Context;
import android.net.NetworkInfo;
import android.util.Base64;

public class ChromeParser {
    
    private SqlLiteStore mStore;
    private Context mContext;
    
    public ChromeParser(Context context, SqlLiteStore store){
        mStore = store;
        mContext = context;
    }
    
    
    public List<Long> getAllNetworkEventsIds(){
        List<Long> listConversationsIds =  mStore.getConversationIds(null, null, null, null, SqlLiteStore.CONVERSATION_TS_START);
        return listConversationsIds;
        
    }
    
    public List<Long> getAllWebSocketEventsIds(long conversationId){
        List<Long> listConvSocketMesageIds =  mStore.getSocketChannelMessageIds(conversationId);
        return listConvSocketMesageIds;
    }
    
    public List<String> getConversationData(long conversationId){
        List<String> eventData = new LinkedList<String>();
        Conversation conv =  mStore.getConversation(conversationId);
        if (conv == null) return null;
        String sendData = getRequestWillBeSend(conv, conversationId);
        if (sendData != null){
            eventData.add(sendData);
            boolean protocolSwitch = false;
            if (conv.RESP_STATUS_CODE == 101){
                protocolSwitch = true;
            }
            List<String> responseData = getResponseReceived(conv, conversationId, protocolSwitch);
            if (responseData != null){
                eventData.addAll(responseData);
            }
        }
        return eventData;
    }
    
    public String createResponseOnMethod(long id, boolean isEnabled){
        try {
            JSONObject resp = new JSONObject();
            JSONObject result = new JSONObject();
            resp.put("id", id);
            resp.put("error", "");
            result.put("result", isEnabled);
            resp.put("result", result);
            String response = resp.toString();
            return response;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }
    
    public List<String> getResponseReceived(Conversation conv , long conversationId, boolean protocolSwitch){
        if (conv == null){
            conv =  mStore.getConversation(conversationId);
        }
        if (conv == null) return null;
        JSONObject obj = new JSONObject();
        List<String> result = new LinkedList<String>();
        if (conv.STATUS == FrameworkModel.CONVERSATION_STATUS_ABORTED){
            try {
                obj.put("method", "Network.loadingFailed");
                JSONObject params = new JSONObject();
                double secondsEnd = (double)conv.TS_END/(double)1000;
                params.put("requestId", String.valueOf(conv.getRequestId()));
                params.put("errorText", conv.STATUS_DESC);
                params.put("canceled", true);
                params.put("timestamp", secondsEnd);
                obj.put("params", params);
                result.add(obj.toString());
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Request request = mStore.getRequest(conv.getRequestId());
        if (request == null) return null;
        Response response = mStore.getResponse(conv.RESPONSE_ID);
        if (response == null) return null;
        try {
            obj.put("method", "Network.responseReceived");
            JSONObject params = new JSONObject();
            double secondsStart = (double)conv.TS_START/(double)1000;
            params.put("requestId", String.valueOf(conv.REQUEST_ID));
            params.put("frameId", String.valueOf(conv.CLIENT_ADDRESS));
            params.put("loaderId", String.valueOf(conv.CLIENT_ADDRESS));
            params.put("timestamp", secondsStart);
            
            
            JSONObject responseJson = new JSONObject();
            responseJson.put("url", request.getURL().toString());
            int statusCode = 0;
            String statusCodeStr = response.getStatus();
            try{
                statusCode = Integer.valueOf(statusCodeStr);
                responseJson.put("status", statusCode);
            }catch(Exception ex){
                responseJson.put("status", statusCodeStr);
            }
            
            responseJson.put("statusText", response.getMessage());
            // TODO mime type from response
            // responseJson.put("mimeType", "text/html");
            // [ "Document" , "Font" , "Image" , "Other" , "Script" , "Stylesheet" , "WebSocket" , "XHR" ] 
            String contentType = response.getHeader("Content-Type");
            String docType = "Other";
            String contentTypeVal = "text/html";
            if (protocolSwitch){
                docType = "WebSocket";
            }else{
                if (contentType != null){
                    if (contentType.contains("image")){
                        docType = "Image";
                    }else if (contentType.contains("javascript")){
                        docType = "Script";
                    }else if (contentType.contains("css")){
                        docType = "Stylesheet";
                    }else if (contentType.contains("font")){
                        docType = "Font";
                    }else if (contentType.contains("text")){
                        docType = "Document";
                    }
                    
                    String[] contentTypeArr = contentType.split(";");
                    contentTypeVal = contentTypeArr[0].trim();
                }
            }
            
            // TODO mapping document, image, css, javascript, ...
            params.put("type", docType);
            responseJson.put("mimeType", contentTypeVal);
            responseJson.put("connectionReused", false);
            responseJson.put("connectionId", 1);
            responseJson.put("fromDiskCache", false);
            
//            "timing":{"requestTime":1333314083.649673,"proxyStart":-1,"proxyEnd":-1,"dnsStart":1,"dnsEnd":134,"connectStart":1,
//                "connectEnd":270,"sslStart":-1,"sslEnd":-1,"sendStart":270,"sendEnd":270,"receiveHeadersEnd":407}
            JSONObject timing = new JSONObject();
            
            timing.put("requestTime", secondsStart);
            timing.put("proxyStart", 0);
            timing.put("proxyEnd", 0);
            timing.put("dnsStart", 0);
            timing.put("dnsEnd", 0);
            timing.put("connectStart", 0);
            timing.put("connectEnd", 0);
            timing.put("sslStart", 0);
            timing.put("sslEnd", 0);
            timing.put("sendStart", 0);
            timing.put("sendEnd", 0);
            timing.put("receiveHeadersEnd", 0);
            
            responseJson.put("timing", timing);

            JSONObject headers = new JSONObject();
            NamedValue[] headersValues = response.getHeaders();
            for (NamedValue namedValue : headersValues) {
                headers.put(namedValue.getName(), namedValue.getValue());
            }
            
            responseJson.put("headers", headers);
            params.put("response", responseJson);
            obj.put("params", params);
            // add Network.responseReceived
            result.add(obj.toString());
            // add network loading finished
            if (!protocolSwitch){
                obj.put("method", "Network.loadingFinished");
                JSONObject fishedParams = new JSONObject();
                fishedParams.put("requestId", String.valueOf(conv.REQUEST_ID));
                double secondsEnd = (double)conv.TS_END/(double)1000;
                fishedParams.put("timestamp", secondsEnd);
                obj.put("params", fishedParams);
                result.add(obj.toString());
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getRequestWillBeSend(Conversation conv, long conversationId){
        if (conv == null){
            conv =  mStore.getConversation(conversationId);
        }
        if (conv == null) return null;
        Request request = mStore.getRequest(conv.getRequestId());
        if (request == null) return null;
        JSONObject obj = new JSONObject();
        try {
            obj.put("method", "Network.requestWillBeSent");
            JSONObject params = new JSONObject();
            params.put("requestId", String.valueOf(conv.REQUEST_ID));
            params.put("frameId", String.valueOf(conv.CLIENT_ADDRESS));
            params.put("loaderId", String.valueOf(conv.CLIENT_ADDRESS));
            params.put("documentUrl", request.getURL().toString());
            
            JSONObject requestJson = new JSONObject();
            requestJson.put("url", request.getURL().toString());
            requestJson.put("method", request.getMethod());
            if (request.getContentSize() > 0){
                byte[] content = request.getContent();
                String contentString = new String(content);
                requestJson.put("postData", contentString);
            }
            
            JSONObject headers = new JSONObject();
            NamedValue[] headersValues = request.getHeaders();
            for (NamedValue namedValue : headersValues) {
                headers.put(namedValue.getName(), namedValue.getValue());
            }
            
            requestJson.put("headers", headers);
            double seconds = (double)conv.TS_START/(double)1000;
            requestJson.put("timestamp", seconds);
            params.put("request", requestJson);

            JSONObject initiator = new JSONObject();
            
            
            JSONArray stackTrace = new JSONArray();
            
            if (conv.CLIENT_APP_NAME != null){
                initiator.put("type", "parser");
                if (conv.CLIENT_APP_NAME.length() == 0){
                    initiator.put("url", "http://" + conv.CLIENT_ADDRESS);
                }else{
                    initiator.put("url", "https://play.google.com/store/apps/details?id=" + conv.CLIENT_APP_NAME);
                }
                initiator.put("lineNumber", conv.CLIENT_PORT);
            }else{
                initiator.put("type", "other");
            }
            params.put("initiator", initiator);
            
            params.put("stackTrace", stackTrace);
            
            obj.put("params", params);
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<String> generateWebSocketFrameEvent(long conversationId, long messageId){
        List<String> result = new LinkedList<String>();
        Conversation conv =  mStore.getConversation(conversationId);
        if (conv == null) return null;
        long requestId = conv.getRequestId();
        WebSocketMessageDTO message = mStore.getSocketChannelMessageByConvId(conversationId, messageId);
        JSONObject obj = new JSONObject();
        try {
            String methodName = "Network.webSocketFrameSent";
            if (!message.isOutgoing){
                methodName = "Network.webSocketFrameReceived";
            }
            obj.put("method", methodName);
            JSONObject params = new JSONObject();
            params.put("requestId", String.valueOf(requestId));
            double messageTimestamp = (double)message.timestamp/(double)1000;
            params.put("timestamp", messageTimestamp);
            
            JSONObject responseJson = new JSONObject();
            responseJson.put("opcode", message.opcode);
            responseJson.put("mask", true);
            responseJson.put("payloadData", message.getReadablePayload());
            params.put("response", responseJson);
            
            obj.put("params", params);
            result.add(obj.toString());
            if (message.opcode == WebSocketMessage.OPCODE_CLOSE){
                JSONObject objCloseMessage = new JSONObject();
                objCloseMessage.put("method", "Network.loadingFinished");
                JSONObject fishedParams = new JSONObject();
                fishedParams.put("requestId", String.valueOf(requestId));
                fishedParams.put("timestamp", messageTimestamp);
                objCloseMessage.put("params", fishedParams);
                result.add(objCloseMessage.toString());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<String> getWebSocketFrameSent(long conversationId, long messageId){
        return generateWebSocketFrameEvent(conversationId, messageId);
    }
    
    public List<String> getWebSocketFrameReceived(long conversationId, long messageId){
        return generateWebSocketFrameEvent(conversationId, messageId);
    }
    
    public List<String> getChromeAssetsResources(String assetFileName){
        InputStream is;
        try {
            is = mContext.getAssets().open("chrome_devtools/" + assetFileName);
            BufferedReader bs = new BufferedReader(new InputStreamReader(is));
            String line = "";
            List<String> arr = new ArrayList<String>();
            while((line = bs.readLine()) != null){
                arr.add(line);
            }
            return arr;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public String getCSSProperties(long id){
        String cssPropertiesJsonString = getChromeAssetsResources("css_properties.json").get(0);
        JSONObject responseObj;
        try {
            responseObj = new JSONObject(cssPropertiesJsonString);
            responseObj.put("id", id);
            return responseObj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getResourceContent(long id, long responseId){
        Response response = mStore.getResponse(responseId);
        if (response == null){
            return null;
        }
        JSONObject obj = new JSONObject();
        try {
            JSONObject body = new JSONObject();
            String contentType = response.getHeader("Content-Type");
            boolean base64Encode = false;
            if (contentType != null){
                if (contentType.contains("image")){
                    base64Encode = true;
                }
            }
            int responseSize = response.getContentSize();
            if ( responseSize > MessageOutputStream.LARGE_CONTENT_SIZE){
                String largeResponse = String.format("Content is to big to process (%s). Try open it in new tab.",responseSize);
                body.put("content", largeResponse);
                body.put("base64Encoded", false);
            }else{
                if (base64Encode){
                    body.put("content", Base64.encodeToString(response.getContent(),Base64.DEFAULT));
                    body.put("base64Encoded", true);
                }else{
                    body.put("content", new String (response.getContent()));
                    body.put("base64Encoded", false);
                }
            }
            
            obj.put("result", body);
            obj.put("id", id);
            return obj.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    
    public String getResponseBody(long id, long requestId){
        Response response = mStore.getResponseByRequestId(requestId);
        if (response == null){
            return null;
        }
        JSONObject obj = new JSONObject();
        try {
            JSONObject body = new JSONObject();
            String contentType = response.getHeader("Content-Type");
            boolean base64Encode = false;
            if (contentType != null){
                if (contentType.contains("image")){
                    base64Encode = true;
                }
            }
            int responseSize = response.getContentSize();
            if ( responseSize > MessageOutputStream.LARGE_CONTENT_SIZE){
                String largeResponse = String.format("Content is to big to process (%s). Try open it in new tab.",responseSize);
                body.put("body", largeResponse);
                body.put("base64Encoded", false);
            }else{
                byte[] content = response.getContent();
                if (base64Encode){
                    body.put("body", Base64.encodeToString(content, Base64.DEFAULT));
                    body.put("base64Encoded", true);
                }else{
                    body.put("body", new String(content));
                    body.put("base64Encoded", false);
                }
            }
            
            obj.put("result", body);
            obj.put("id", id);
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getRuntimeEvaluationResponse(long id, String runtimeResponse){
        String typeTag = "type";
        String valueTag = "value";
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        JSONObject resultObj = new JSONObject();
        try {
            resultObj.put(typeTag, "string");
            resultObj.put(valueTag, runtimeResponse);
            result.put("result", resultObj);
            responseObj.put("result", result);
            responseObj.put("wasThrown", false);
            responseObj.put("id", id);
            return responseObj.toString();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public String getCookiesEmpty(long id){
        String cookies = "cookies";
        String cookieString = "cookiesString";
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            result.put(cookies, new JSONArray());
            result.put(cookieString, " ");
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getDatabaseNamesEmpty(long id){
        String databaseNames = "databaseNames";
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            result.put(databaseNames, new JSONArray());
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getDOMStorageItemsEmpty(long id){
        String entries = "entries";
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            result.put(entries, new JSONArray());
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getProfilerHeadersEmpty(long id){
        String headers = "headers";
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            result.put(headers, new JSONArray());
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public String getFramesWithEmptyManifests(long id){
        String tag = "frameIds";
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            result.put(tag, new JSONArray());
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getEmptyResourceTree(long id){
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        JSONObject mainFrame = new JSONObject();
        JSONArray frameTreeArray = new JSONArray();
        try {
            JSONObject frame = new JSONObject();
            String url = "http://sandroproxy";
            frame.put("id", "0");
            frame.put("loaderId", "0");
            frame.put("url", url);
            frame.put("securityOrigin", url);
            frame.put("mimeType", "text/html");
            frame.put("resources", new JSONArray());
            
            frame.put("frameTree", frameTreeArray);
            mainFrame.put("frame", frame);
            mainFrame.put("childFrames", frameTreeArray);
            mainFrame.put("resources", new JSONArray());

            result.put("frameTree", mainFrame);
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public String getResourceTree(long id){
        JSONObject responseObj = new JSONObject();
        JSONObject result = new JSONObject();
        JSONObject mainFrame = new JSONObject();
        JSONArray frameTreeArray = new JSONArray();
        try {
            // TODO this can be huge so it must be count limited to last 100 order by date?
            // only fetch the one that have status code 200, no 404, 304 or other
            List<Conversation> listConversations =  mStore.getConversation(null, null, "200", null, SqlLiteStore.CONVERSATION_REQ_HOST);
            Iterator<Conversation> it = listConversations.iterator();
            boolean haveFetchedAlready = false;
            Conversation conversation = null;
            while(it.hasNext()){
                if (!haveFetchedAlready){
                    conversation = it.next();
                }
                JSONObject frameData = new JSONObject();
                JSONObject frameObj = new JSONObject();
                String url = conversation.REQ_SCHEMA + "://" +  conversation.REQ_HOST + conversation.REQ_PATH;
                String hostUrl = conversation.REQ_SCHEMA + "://" +  conversation.REQ_HOST;
                frameData.put("id", String.valueOf(conversation.RESPONSE_ID));
                frameData.put("loaderId", conversation.CLIENT_ADDRESS);
                frameData.put("url", hostUrl);
                frameData.put("securityOrigin", hostUrl);
                frameData.put("mimeType", conversation.RESP_CONTENT_TYPE);
                frameObj.put("frame", frameData);
                
                
                String contentType = conversation.RESP_CONTENT_TYPE;
                String hostName = conversation.REQ_HOST;
                JSONArray resourceArray = new JSONArray();
                boolean resourceFetch = true;
                while(resourceFetch && conversation.REQ_HOST.equalsIgnoreCase(hostName)){
                    hostName = conversation.REQ_HOST;
                    contentType = conversation.RESP_CONTENT_TYPE;
                    // url, type, mimeType
                    url = conversation.REQ_SCHEMA + "://" +  conversation.REQ_HOST + conversation.REQ_PATH;
                    hostUrl = conversation.REQ_SCHEMA + "://" +  conversation.REQ_HOST;
                    JSONObject resource = new JSONObject();
                    resource.put("url", url);
                    resource.put("mimeType", contentType);
                    String docType = "Document";
                    if (contentType != null){
                        if (contentType.contains("image")){
                            docType = "Image";
                        }else if (contentType.contains("javascript")){
                            docType = "Script";
                        }else if (contentType.contains("css")){
                            docType = "Stylesheet";
                        }else if (contentType.contains("font")){
                            docType = "Font";
                        }
                    }
                    resource.put("type", docType);
                    resource.put("resourceId", String.valueOf(conversation.RESPONSE_ID));
                    resourceArray.put(resource);
                    if (it.hasNext()){
                        conversation = it.next();
                        haveFetchedAlready = true;
                    }else{
                        resourceFetch = false;
                    }
                }
                frameObj.put("resources", resourceArray);
                frameObj.put("childFrames", new JSONArray());
                frameTreeArray.put(frameObj);
            }
            
            JSONObject frame = new JSONObject();
            String url = "http://sandroproxy";
            frame.put("id", "0");
            frame.put("loaderId", "0");
            frame.put("url", url);
            frame.put("securityOrigin", url);
            frame.put("mimeType", "text/html");
            frame.put("resources", new JSONArray());
            
            frame.put("frameTree", frameTreeArray);
            mainFrame.put("frame", frame);
            mainFrame.put("childFrames", frameTreeArray);
            mainFrame.put("resources", new JSONArray());

            result.put("frameTree", mainFrame);
            responseObj.put("result", result);
            responseObj.put("id", id);
            return responseObj.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public String getConnectionsSnapshot(boolean resolveHostNames){
        JSONObject responseObj = new JSONObject();
        JSONObject resultSnapshot = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            double messageTimestamp = (double)System.currentTimeMillis()/(double)1000;
            result.put("timestamp", messageTimestamp);
            List<ConnectionDescriptor> connectionsInfo = org.sandroproxy.utils.network.NetworkInfo.getNetworkInfo(mContext, resolveHostNames);
            JSONArray connections = new JSONArray();
            for (ConnectionDescriptor connectionDescriptor : connectionsInfo) {
                JSONObject connection = new JSONObject();
                connection.put("type", connectionDescriptor.getType());
                // connection.put("state", connectionDescriptor.getStateShortCode());
                connection.put("statecode", connectionDescriptor.getStateCode());
                connection.put("laddress", connectionDescriptor.getLocalAddress());
                connection.put("lport", connectionDescriptor.getLocalPort());
                connection.put("lportprotocol", connectionDescriptor.getLocalPortProtocol());
                connection.put("raddress", connectionDescriptor.getRemoteAddress());
                connection.put("rhost", connectionDescriptor.getRemoteHostName());
                connection.put("rport", connectionDescriptor.getRemotePort());
                connection.put("rportprotocol", connectionDescriptor.getRemotePortProtocol());
                connection.put("uid", connectionDescriptor.getId());
                connection.put("name", connectionDescriptor.getName());
                connection.put("namespace", connectionDescriptor.getNamespace());
                String[] names = connectionDescriptor.getNames();
                String[] namespaces = connectionDescriptor.getNamespaces();
                JSONArray namesArray = new JSONArray();
                if (names != null && names.length > 1){
                    for (String name : names){
                        namesArray.put(name);
                    }
                }
                connection.put("names", namesArray);
                JSONArray namespacesArray = new JSONArray();
                if (namespaces != null && namespaces.length > 1){
                    for (String namespace : namespaces){
                        namespacesArray.put(namespace);
                    }
                }
                connection.put("namespaces", namespacesArray);
                connections.put(connection);
            }
            result.put("connections", connections);
            resultSnapshot.put("snapshot", result);
            responseObj.put("method", "Network.sandroProxyConnectionsSnapshot");
            responseObj.put("params", resultSnapshot);
            return responseObj.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
}
