package org.sandroproxy.web;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sandrop.webscarab.model.NamedValue;
import org.sandrop.webscarab.model.Request;
import org.sandroproxy.plugin.gui.MainActivity;
import org.sandroproxy.utils.PreferenceUtils;
import org.sandroproxy.webscarab.store.sql.Conversation;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

/**
 * <p>
 * AndroidHTTPD version 0.5, Copyright &copy; 2012 Free Beachler
 * (fbeachler@gmail.com, http://github.com/tenaciousRas)
 * 
 * @author fbeachler
 * @ modified supp.sandrob@gmail.com
 * 
 */
public class AndroidHTTPD extends NanoHTTPD {

    public static final String TAG = AndroidHTTPD.class.getName();
    public static final boolean LOGD = false;
    
    private Map<String,String> cacheDict;
    private Context context;
    private String hostUrl;

    /**
     * Implementors handle HTTP requests delegated by AndroidHTTPD#serve(java.lang.String,
     * java.lang.String, java.util.Properties, java.util.Properties,
     * java.util.Properties)
     */
    public static interface RequestHandler {
        public Response onRequestReceived(String uri, String method,
                Properties header, Properties parms, Properties files);
    }

    /**
     * The request handler set for this instance.
     */
    private RequestHandler requestHandler;
    private int webPort;
    private int webSocketPort;
    private boolean useLocalWebGuiResources;

    public AndroidHTTPD(Context ctx, int port, int socketPort, File wwwroot, String hostUrl, 
            boolean useLocalWebGuiResources, RequestHandler requestHandler) throws IOException {
        super(port, wwwroot);
        this.requestHandler = requestHandler;
        this.context = ctx;
        this.hostUrl = hostUrl;
        this.webPort = port;
        this.webSocketPort = socketPort;
        this.useLocalWebGuiResources = useLocalWebGuiResources;
        cacheDict = new HashMap<String, String>();
        if (LOGD) Log.i(TAG,
                new StringBuilder().append(
                        "server started and listening in background")
                        .toString());
    }

    /**
     * @return the requestHandler
     */
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * NanoHTTPD#serve(java.lang.String,
     * java.lang.String, java.util.Properties, java.util.Properties,
     * java.util.Properties)
     */
    @Override
    public Response serve(String uri, String method, Properties header,
            Properties parms, Properties files) {
        if (LOGD) Log.i(TAG,
                new StringBuilder().append("#serve called with uri=")
                        .append(uri).append(", method=").append(method)
                        .append(", header=")
                        .append(null == header ? null : header.toString())
                        .append(", parms=")
                        .append(null == parms ? null : parms.toString())
                        .toString());
        if (null != requestHandler) {
            return requestHandler.onRequestReceived(uri, method, header, parms,
                    files);
        }
        String mime = null;
        if (uri.equalsIgnoreCase("") || uri.equalsIgnoreCase("/")){
            uri = "/index.html";
            Response res = new Response( HTTP_REDIRECT, MIME_HTML,
                    "<html><body>Redirected: <a href=\"" + uri + "\">" +
                    uri + "</a></body></html>");
            res.addHeader( "Location", uri );
            return res;
        }
        int dot = uri.lastIndexOf( '.' );
        if ( dot >= 0 )
            mime = (String)theMimeTypes.get(uri.substring( dot + 1 ).toLowerCase());
        if ( mime == null )
            mime = MIME_DEFAULT_BINARY;
        if (uri.startsWith("/devtools")){
            try {
                String absoluteUrl;
                String baseUrl = this.hostUrl;
                absoluteUrl = baseUrl + uri.replace("/devtools/", "");
                String ifNoneMatch = header.getProperty("if-none-match");
                if (ifNoneMatch != null){
                    if (cacheDict.containsKey(uri)){
                        if (cacheDict.get(uri).equalsIgnoreCase(ifNoneMatch)){
                            Response res = new Response(HTTP_NOTMODIFIED, MIME_PLAINTEXT, "Read from cache");
                            return res;
                        }
                    }
                    
                }
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                if (!useLocalWebGuiResources){
                    // make request to server specified in settings
                    HttpClient httpclient = new DefaultHttpClient();
                    httpclient.getParams().setParameter("If-None-Match", ifNoneMatch);
                    String cacheControl = header.getProperty("Cache-Control");
                    if (cacheControl != null){
                        httpclient.getParams().setParameter("Cache-Control", cacheControl);
                    }
                    HttpResponse response = httpclient.execute(new HttpGet(absoluteUrl));

                    // Pull content stream from response
                    HttpEntity entity = response.getEntity();
                    InputStream inputStream = entity.getContent();

                    // Read response into a buffered stream
                    int readBytes = 0;
                    byte[] sBuffer = new byte[8192];
                    while ((readBytes = inputStream.read(sBuffer)) != -1) {
                        content.write(sBuffer, 0, readBytes);
                    }
                    ByteArrayInputStream fetchedContent = new ByteArrayInputStream(content.toByteArray());

                    Response res = new Response(String.valueOf(response.getStatusLine().getStatusCode()), mime, fetchedContent);
                    Header[] etagHeaders = response.getHeaders("ETag"); 
                    if (etagHeaders != null && etagHeaders.length > 0){
                        String etagVal = response.getHeaders("ETag")[0].getValue();
                        res.addHeader( "ETag", etagVal);
                        if (cacheDict.containsKey(uri)){
                            cacheDict.remove(uri);
                        }
                        cacheDict.put(uri, etagVal);
                        
                        // we do not send all content to client if etag is the same
                        if (ifNoneMatch != null && ifNoneMatch.equals(etagVal)){
                            res = new Response(HTTP_NOTMODIFIED, MIME_PLAINTEXT, "Read from cache");
                            return res;
                        }
                    }
                    Header[] cacheControlHeaders = response.getHeaders("Cache-Control"); 
                    if (cacheControlHeaders != null && cacheControlHeaders.length > 0){
                        res.addHeader( "Cache-Control", "" + response.getHeaders("Cache-Control")[0].getValue());
                    }
                    Header[] ageHeaders = response.getHeaders("Age"); 
                    if (ageHeaders != null && ageHeaders.length > 0){
                        res.addHeader( "Age", "" + response.getHeaders("Age")[0].getValue());
                    }
                    res.addHeader( "Content-Length", "" + content.size());
                    return res;
                }else{
                    String fileName = uri.replace("/devtools/", "chrome_devtools/inspector/");
                    InputStream is = context.getAssets().open(fileName);
                    int size = is.available();
                    Response res = new Response( HTTP_OK, mime, is);
                    res.addHeader( "Content-Length", "" + size);
                    String etagVal = String.valueOf(size);
                    res.addHeader( "ETag", etagVal);
                    if (cacheDict.containsKey(uri)){
                        cacheDict.remove(uri);
                    }
                    cacheDict.put(uri, etagVal);
                    return res;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                        "INTERNAL ERRROR: " + e.getMessage() );
                return res;
            }
        }else if (uri.startsWith("/index.html")){
            // gets content from assets and inject urls for stored and realtime link
            try {
                InputStream is = context.getAssets().open("chrome_devtools/index.html");
                BufferedReader bs = new BufferedReader(new InputStreamReader(is));
                String line = "";
                String content = "";
                while((line = bs.readLine()) != null){
                    content += line + "\n";
                }
                String hostName = "localhost";
                if (header.getProperty("host") != null){
                    String host = header.getProperty("host");
                    String[] hostValues = host.split(":");
                    hostName = hostValues[0];
                }
                String baseUrl = "http://" + hostName + ":" + this.webPort + "/devtools/devtools.html?host=" + hostName +  ":" +this.webSocketPort +"&page=";
                String realtimeUrl = baseUrl + "1";
                String storedDataUrl = baseUrl + "2";
                content = content.replace("<!-- REALTIME URL -->", realtimeUrl);
                content = content.replace("<!-- STORED URL -->", storedDataUrl);
                Response res = new Response( HTTP_OK, mime, new ByteArrayInputStream(content.getBytes()));
                res.addHeader( "Content-Length", "" + content.length());
                return res;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }else if (uri.startsWith("/action")){
            if (uri.startsWith("/action/send/request")){
                // TODO send request and put response to client
            }
            if (uri.startsWith("/action/proxy/stop")){
                AsyncTask<Boolean, Void, Boolean> startStop = new AsyncTask<Boolean, Void, Boolean>(){
                    @Override
                    protected Boolean doInBackground(Boolean... params) {
                        if (MainActivity.proxyStarted){
                            try {
                                Intent service = new Intent(context, MainActivity.class);
                                context.stopService(service);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return true;
                    }
                };
                startStop.execute(MainActivity.proxyStarted);
                Response res = new Response( HTTP_OK, MIME_PLAINTEXT,
                        "PROXY STOPPED");
                return res;
            }
            if (uri.startsWith("/action/proxy/start")){
                AsyncTask<Boolean, Void, Boolean> startStop = new AsyncTask<Boolean, Void, Boolean>(){
                    @Override
                    protected Boolean doInBackground(Boolean... params) {
                        if (!MainActivity.proxyStarted){
                            try {
                                // TODO start/stop service
                                // Intent service = new Intent(context.getApplicationContext(), MainActivity.class);
                                // context.startService(service);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return true;
                    }
                };
                startStop.execute(MainActivity.proxyStarted);
                Response res = new Response( HTTP_OK, MIME_PLAINTEXT,
                        "PROXY STARTED");
                return res;
            }
            if (uri.startsWith("/action/proxy/status")){
                JSONObject responseObj = new JSONObject();
                try {
                    responseObj.put("proxy_started", MainActivity.proxyStarted);
                    String responseObjString = responseObj.toString();
                    Response res;
                    res = new Response( HTTP_OK, MIME_JSON, new ByteArrayInputStream(responseObjString.getBytes("UTF-8")));
                    res.addHeader( "Content-Length", "" + responseObjString.length());
                    return res;
                } catch (Exception e) {
                    e.printStackTrace();
                    Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                            "Error:" + e.getMessage());
                    return res;
                }
                
            }
        }else if (uri.startsWith("/data")){
            if (uri.startsWith("/data/conversation")){
                if (uri.startsWith("/data/conversation/list.json")){
                        SqlLiteStore database = SqlLiteStore.getInstance(context, null);
                        List<Conversation> conversations = database.getConversation(null, null, null, null, null);
                        JSONObject responseListObj = new JSONObject();
                        JSONArray array = new JSONArray();
                        for (Conversation conversation : conversations) {
                            try{
                                JSONObject conversationObj = new JSONObject();
                                // request
                                JSONObject requestObj = new JSONObject();
                                {
                                    Request request = database.getRequest(conversation.getRequestId());
                                    requestObj.put("schema", request.getURL().getScheme());
                                    requestObj.put("hostname", request.getURL().getHost());
                                    requestObj.put("method", request.getMethod());
                                    requestObj.put("url", request.getURL().getPath());
                                    requestObj.put("query", request.getURL().getQuery() == null ? "" : request.getURL().getQuery());
                                    requestObj.put("id", conversation.getRequestId());
                                    NamedValue[] headers = request.getHeaders();
                                    JSONArray requestHeadersArray = new JSONArray();
                                    if (headers != null && headers.length > 0){
                                        for (NamedValue namedValue : headers) {
                                            JSONObject requestHeadersObj = new JSONObject();
                                            requestHeadersObj.put("name", namedValue.getName());
                                            requestHeadersObj.put("value", namedValue.getValue());
                                            requestHeadersArray.put(requestHeadersObj);
                                        }
                                    }
                                    requestObj.put("headers", requestHeadersArray);
                                }
                                // response
                                JSONObject responseObj = new JSONObject();
                                {
                                    org.sandrop.webscarab.model.Response response = database.getResponse(conversation.RESPONSE_ID);
                                    if (response != null){
                                        responseObj.put("status", response.getStatus());
                                        responseObj.put("statusline", response.getStatusLine());
                                        responseObj.put("version", response.getVersion());
                                        responseObj.put("id", conversation.RESPONSE_ID);
                                        NamedValue[] headers = response.getHeaders();
                                        JSONArray responseHeadersArray = new JSONArray();
                                        if (headers != null && headers.length > 0){
                                            for (NamedValue namedValue : headers) {
                                                JSONObject responseHeadersObj = new JSONObject();
                                                responseHeadersObj.put("name", namedValue.getName());
                                                responseHeadersObj.put("value", namedValue.getValue());
                                                responseHeadersArray.put(responseHeadersObj);
                                            }
                                            responseObj.put("headers", responseHeadersArray);
                                        }
                                        String[] contentTypeHeader = response.getHeaders("Content-Type");
                                        if (contentTypeHeader != null && contentTypeHeader.length > 0){
                                            responseObj.put("content_type", contentTypeHeader[0]);
                                        }
                                        else{
                                            responseObj.put("content_type", "");
                                        }
                                        String[] contentLengthHeader = response.getHeaders("Content-Length");
                                        if (contentLengthHeader != null && contentLengthHeader.length > 0){
                                            responseObj.put("content_length", contentLengthHeader[0]);
                                        }
                                        else{
                                            responseObj.put("content_length", "");
                                        }
                                    }else{
                                        responseObj.put("status", "");
                                        responseObj.put("statusline", "");
                                        responseObj.put("version", "");
                                        responseObj.put("id", conversation.RESPONSE_ID);
                                        responseObj.put("headers", new JSONArray());
                                        responseObj.put("content_type", "");
                                        responseObj.put("content_length", "");
                                    }
                                }
                                conversationObj.put("request", requestObj);
                                conversationObj.put("response", responseObj);
                                array.put(conversationObj);
                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                            
                        }
                        try {
                            responseListObj.put("aaData", array);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                    "INTERNAL ERRROR: " + e.getMessage() );
                            return res;
                        }
                        String requestListObjString = responseListObj.toString();
                        try {
                            Response res;
                            res = new Response( HTTP_OK, mime, new ByteArrayInputStream(requestListObjString.getBytes("UTF-8")));
                            res.addHeader( "Content-Length", "" + requestListObjString.length());
                            return res;
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                    "INTERNAL ERRROR: " + e.getMessage() );
                            return res;
                        }
                }
            }
            if (uri.startsWith("/data/request")){
                if (uri.startsWith("/data/request/list.json")){
                    File[] conversationsFileList = null;
                    File conversationDir = new File(PreferenceUtils.getDataStorageDir(context).getAbsolutePath() + "/conversations");
                    conversationsFileList = conversationDir.listFiles();
                    if (conversationsFileList != null){
                        if (conversationsFileList != null){
                            JSONObject responseListObj = new JSONObject();
                            JSONArray array = new JSONArray();
                            for (int i=0; i<conversationsFileList.length; i++) {
                                if (conversationsFileList[i].getName().contains("request")){
                                    try{
                                        String fileName = conversationsFileList[i].getName();
                                        String[] nameParts = fileName.split("-");
                                        Request request = new org.sandrop.webscarab.model.Request();
                                        FileInputStream requestFileInputStream = new FileInputStream(conversationsFileList[i]);
                                        request.read(requestFileInputStream);
                                        JSONObject requestObj = new JSONObject();
                                        requestObj.put("schema", request.getURL().getScheme());
                                        requestObj.put("hostname", request.getURL().getHost());
                                        requestObj.put("method", request.getMethod());
                                        requestObj.put("url", request.getURL().getPath());
                                        requestObj.put("query", request.getURL().getQuery() == null ? "" : request.getURL().getQuery());
                                        requestObj.put("id", nameParts[0]);
                                        array.put(requestObj);
                                        if (requestFileInputStream != null){
                                            requestFileInputStream.close();
                                        }
                                    }catch (Exception ex){
                                        Log.e(TAG, "Error parsing conversation:" + ex.getMessage());
                                    }
                                }
                            }
                            try {
                                responseListObj.put("aaData", array);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage() );
                                return res;
                            }
                            String requestListObjString = responseListObj.toString();
                            try {
                                Response res;
                                res = new Response( HTTP_OK, mime, new ByteArrayInputStream(requestListObjString.getBytes("UTF-8")));
                                res.addHeader( "Content-Length", "" + requestListObjString.length());
                                return res;
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage() );
                                return res;
                            }
                            
                        }
                    }
                }
                if (uri.startsWith("/data/request/headers.json")){
                    // TODO get request content
                    if (parms.containsKey("id")){
                        String id = parms.getProperty("id").trim();
                        SqlLiteStore database = SqlLiteStore.getInstance(context, null);
                        Request request = database.getRequest(Long.valueOf(id));
                        if (request != null){
                            try {
                                JSONArray headers = new JSONArray();
                                JSONObject responseHeadersObj = new JSONObject();
                                NamedValue[] headerNameValue = request.getHeaders();
                                for (int i = 0; i < headerNameValue.length; i++) {
                                    JSONObject headerObj = new JSONObject();
                                    headerObj.put("name", headerNameValue[i].getName());
                                    headerObj.put("value", headerNameValue[i].getValue());
                                    headers.put(headerObj);
                                }
                                responseHeadersObj.put("aaData", headers);
                                String requestHeadersObjString = responseHeadersObj.toString();
                                Response res;
                                res = new Response( HTTP_OK, mime, new ByteArrayInputStream(requestHeadersObjString.getBytes("UTF-8")));
                                res.addHeader( "Content-Length", "" + requestHeadersObjString.length());
                                return res;
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage());
                                return res;
                            }
                        }
                    }else{
                        Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                "INTERNAL ERRROR: missing id" );
                        return res;
                    }
                    
                }
                if (uri.startsWith("/data/request/content.json")){
                    // TODO get request content
                    if (parms.containsKey("id")){
                        String id = parms.getProperty("id").trim();
                        SqlLiteStore database = SqlLiteStore.getInstance(context, null);
                        Request request = database.getRequest(Long.valueOf(id));
                        if (request != null){
                            try {
                                InputStream is = request.getContentStream();
                                if (is != null && request.getHeader("Content-Length") != null){
                                    Response res = new Response( HTTP_OK, "text/html", is);
                                    res.addHeader( "Content-Length", request.getHeader("Content-Length"));
                                    return res;
                                }else{
                                    Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                            "INTERNAL ERRROR: No data");
                                    return res;
                                }
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage());
                                return res;
                            }
                        }
                    }else{
                        Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                "INTERNAL ERRROR: missing id" );
                        return res;
                    }
                    
                }
            }
            if (uri.startsWith("/data/response/")){
                if (uri.startsWith("/data/response/list.json")){
                    File[] conversationsFileList = null;
                    File conversationDir = new File(PreferenceUtils.getDataStorageDir(context).getAbsolutePath() + "/conversations");
                    conversationsFileList = conversationDir.listFiles();
                    if (conversationsFileList != null){
                        if (conversationsFileList != null){
                            JSONObject responseListObj = new JSONObject();
                            JSONArray array = new JSONArray();
                            for (int i=0; i<conversationsFileList.length; i++) {
                                if (conversationsFileList[i].getName().contains("response")){
                                    try{
                                        String fileName = conversationsFileList[i].getName();
                                        String[] nameParts = fileName.split("-");
                                        org.sandrop.webscarab.model.Response response = new org.sandrop.webscarab.model.Response();
                                        FileInputStream responseFileInputStream = new FileInputStream(conversationsFileList[i]);
                                        response.read(responseFileInputStream);
                                        JSONObject responseObj = new JSONObject();
                                        
                                        responseObj.put("status", response.getStatus());
                                        responseObj.put("statusline", response.getStatusLine());
                                        responseObj.put("version", response.getVersion());
                                        responseObj.put("id", nameParts[0]);
                                        if (responseFileInputStream != null){
                                            responseFileInputStream.close();
                                        }
                                        array.put(responseObj);
                                    }catch (Exception ex){
                                        Log.e(TAG, "Error parsing conversation:" + ex.getMessage());
                                    }
                                }
                            }
                            try {
                                responseListObj.put("aaData", array);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage() );
                                return res;
                            }
                            String requestListObjString = responseListObj.toString();
                            try {
                                Response res;
                                res = new Response( HTTP_OK, mime, new ByteArrayInputStream(requestListObjString.getBytes("UTF-8")));
                                res.addHeader( "Content-Length", "" + requestListObjString.length());
                                return res;
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage() );
                                return res;
                            }
                        }
                    }
                }
                if (uri.startsWith("/data/response/headers.json")){
                    // TODO get request content
                    if (parms.containsKey("id")){
                        String id = parms.getProperty("id").trim();
                        SqlLiteStore database = SqlLiteStore.getInstance(context, null);
                        org.sandrop.webscarab.model.Response response = database.getResponse(Long.valueOf(id));
                        if (response != null){
                            try {
                                JSONArray headers = new JSONArray();
                                JSONObject responseHeadersObj = new JSONObject();
                                NamedValue[] headerNameValue = response.getHeaders();
                                // status line 
                                JSONObject headerObj = new JSONObject();
                                headerObj.put("name", "Status");
                                headerObj.put("value", response.getStatusLine());
                                headers.put(headerObj);
                                for (int i = 0; i < headerNameValue.length; i++) {
                                    headerObj = new JSONObject();
                                    headerObj.put("name", headerNameValue[i].getName());
                                    headerObj.put("value", headerNameValue[i].getValue());
                                    headers.put(headerObj);
                                }
                                responseHeadersObj.put("aaData", headers);
                                String requestHeadersObjString = responseHeadersObj.toString();
                                Response res;
                                res = new Response( HTTP_OK, mime, new ByteArrayInputStream(requestHeadersObjString.getBytes("UTF-8")));
                                res.addHeader( "Content-Length", "" + requestHeadersObjString.length());
                                return res;
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage());
                                return res;
                            }
                        }
                    }else{
                        Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                "INTERNAL ERRROR: missing id" );
                        return res;
                    }
                }
                if (uri.startsWith("/data/response/content.json")){
                    // TODO get request content
                    if (parms.containsKey("id")){
                        String id = parms.getProperty("id").trim();
                        SqlLiteStore database = SqlLiteStore.getInstance(context, null);
                        org.sandrop.webscarab.model.Response response = database.getResponse(Long.valueOf(id));
                        if (response != null){
                            try {
                                InputStream is = null;
                                is = response.getContentStream();
                                if (is != null){
                                    Response res = new Response( HTTP_OK, response.getHeader("Content-Type"), is);
                                    if (response.isCompressed()){
                                        res.addHeader( "Content-Encoding", "gzip");
                                    }
                                    if (response.getHeader("Content-Length") != null){
                                        res.addHeader( "Content-Length", response.getHeader("Content-Length"));
                                    }
                                    if (response.getHeader("Content-Disposition") != null){
                                        res.addHeader( "Content-Disposition", response.getHeader("Content-Disposition"));
                                    }
                                    return res;
                                }else{
                                    Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                            "INTERNAL ERRROR: No data");
                                    return res;
                                }
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                        "INTERNAL ERRROR: " + e.getMessage());
                                return res;
                            }
                        }
                    }else{
                        Response res = new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,
                                "INTERNAL ERRROR: missing id" );
                        return res;
                    }
                    
                }
            }
        }
        Response res = new Response( HTTP_NOTFOUND, MIME_PLAINTEXT,
                "File not found" );
        return res;
        // return super.serve(uri, method, header, parms, files);
    }
}
