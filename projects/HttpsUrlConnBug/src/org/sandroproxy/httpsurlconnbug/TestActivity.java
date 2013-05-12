package org.sandroproxy.httpsurlconnbug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import com.squareup.okhttp.OkHttpClient;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestActivity extends Activity {
    
    private static boolean LOGD = true;
    private static String  TAG = TestActivity.class.getSimpleName();
    
    
    // we need this if we test it on local https server and we have selfsigned cert
    private TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                                            @Override
                                            public X509Certificate[] getAcceptedIssuers() {
                                                return null;
                                            }
                                            @Override
                                            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                                                    throws CertificateException {
                                            }
                                            @Override
                                            public void checkServerTrusted(X509Certificate[] chain,
                                                    String authType) throws CertificateException {
                                            }
                                        } };
                
    // number of threads
    private static int NR_OF_THREADS = 10;
    
    // number of iterations
    private static int NR_OF_ITERATIONS = 10;
    
    // global store for threads 
    private Thread[] threads = new Thread[NR_OF_THREADS];
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        
        
        Button button = (Button) findViewById(R.id.buttonStart);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                for (int i = 0; i < NR_OF_THREADS; i++) {
                    threads[i] = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            createUrlRequest(false);
                        }
                    });
                    threads[i].setName("AndroidClientNr_" + i);
                    threads[i].start();
                }
            }
        });
        Button buttonOkHttp = (Button) findViewById(R.id.buttonStartOkHttp);
        buttonOkHttp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                for (int i = 0; i < NR_OF_THREADS; i++) {
                    threads[i] = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            createUrlRequest(true);
                        }
                    });
                    threads[i].setName("OkHttpClientNr_" + i);
                    threads[i].start();
                }
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.test, menu);
        return true;
    }
    
    
    
    // array of links to test
    private static String[] arrOfLinks = new String[]{
              "https://192.168.1.135/google.png"
//            "https://lh6.googleusercontent.com/-4opwuGcJP5s/AAAAAAAAAAI/AAAAAAAAABk/45YETLhHDjU/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh3.googleusercontent.com/-S0dsLdXhhnY/AAAAAAAAAAI/AAAAAAAAABE/rhcjX171Vug/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh3.googleusercontent.com/-X4wzGKC_OJI/AAAAAAAAAAI/AAAAAAAAABc/EV9zaSzy0K0/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh5.googleusercontent.com/-YdyNW3zTz40/AAAAAAAAAAI/AAAAAAAAARg/yeDyur4i3es/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh5.googleusercontent.com/-7W9v91pqDt4/AAAAAAAAAAI/AAAAAAAAADM/QnPKkFnDQfM/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh5.googleusercontent.com/-XKwS7_-SHE0/AAAAAAAAAAI/AAAAAAAAADU/e9WW3QW0Ekk/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh5.googleusercontent.com/-J81fTvZW81M/AAAAAAAAAAI/AAAAAAAAADo/zm_uwj-DsHU/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh4.googleusercontent.com/-CLRvibbWKKo/AAAAAAAAAAI/AAAAAAAAAFo/ZyZtZmOdF5c/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh5.googleusercontent.com/-F83NNZ8SAik/AAAAAAAAAAI/AAAAAAAAABc/Xaz4rPsJIv8/s96-d-no-p-rwu-k/photo.jpg",
//            "https://lh5.googleusercontent.com/-dIBmLOOpnUc/AAAAAAAAAAI/AAAAAAAAACc/TQEMf8suNno/s96-d-no-p-rwu-k/photo.jpg"
    };
    
    
    private HttpURLConnection HttpUrlConnectionByOkHttp(URL url){
        OkHttpClient okHttpClient = new OkHttpClient();
        // okHttpClient.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888)));
        HttpURLConnection localHttpURLConnection = (HttpURLConnection) okHttpClient.open(url);
        return localHttpURLConnection;
    }
    
    
    /**
     * Opens an {@link HttpURLConnection} with parameters.
     * @param url
     * @return an open connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        int timeoutMs = 30000;
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);
        return connection;
    }
    
    
    // create request with HttpsURLConnection 
    // bug is that on connection reuse, when proxy is set in android OS, 
    // https CONNECT command is send to server over already negotiated ssl layer
    // before 4.2.2 and use of okhttp this means that google plus do not show pictures if proxy is set and reuse of connection happens
    // okhttp in 4.2.2 still have bug but it is not propagated to upper layers <- this is assumption not checked all the code
    // so it can not be seen in 
    // https://android.googlesource.com/platform/frameworks/volley/+/master/src/com/android/volley/toolbox/HurlStack.java
    // but can still be seen in server access log as for example "CONNECT 192.168.1.135:443 HTTP/1.1" 405 588
    // UPDATE: 20130512 bug is still in build Build/JDQ39E 4.2.2_r1.2 but is fixed after that, probably with 
    // https://android.googlesource.com/platform/external/okhttp/+/cf335d418ba2340c2a3cd28038b6cc38a9679b6e
    private void createUrlRequest(boolean useOkHttp){
        try{
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance( "TLS" );
            // Create an ssl socket factory with our all-trusting manager
            sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
            SSLSocketFactory sslSocketFactory = null;sslSocketFactory = sslContext.getSocketFactory();
        
            Random rnd = new Random();
            for (int i = 0; i < NR_OF_ITERATIONS; i++) {
                String threadName = Thread.currentThread().getName();
                String logTagName = threadName + "i_" + i;
                try{
                
                    String url = arrOfLinks[rnd.nextInt(arrOfLinks.length)];
                    if (LOGD) Log.d(TAG, logTagName +  " :starting with connection " + url);
                    URL parsedUrl = new URL(url);
                    HttpsURLConnection localHttpURLConnection = null;
                    if (useOkHttp){
                        localHttpURLConnection = (HttpsURLConnection)HttpUrlConnectionByOkHttp(parsedUrl);
                    }else{
                        localHttpURLConnection = (HttpsURLConnection)openConnection(parsedUrl);
                    }
                      localHttpURLConnection.setSSLSocketFactory(sslSocketFactory);
                      // we need this if hostname do not match cert subject name
                      localHttpURLConnection.setHostnameVerifier(new HostnameVerifier() {
                          @Override
                          public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                      });
                    if (localHttpURLConnection == null){
                        Log.e(TAG, "Could not create localHttpURLConnection");
                    }
                    if (localHttpURLConnection.getResponseCode() == -1){
                        Log.e(TAG, "Could not retrieve response code from HttpUrlConnection.");
                    }
    
                    if (LOGD) Log.d(TAG, logTagName +  " : getting response code " + localHttpURLConnection.getResponseCode());
                    
                    InputStream input = localHttpURLConnection.getInputStream();
                    int len;
                    byte[] buf = new byte[4096];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                    while ((len = input.read(buf)) != -1) {
                        baos.write(buf, 0, len);
                    }
                    byte[] data = baos.toByteArray();
                    localHttpURLConnection.disconnect();
                    // all data is read and connection disconnected so MAY be put back in pool and reuse it...
                    // http://developer.android.com/reference/java/net/HttpURLConnection.html
                    
                    if (LOGD) Log.d(TAG, logTagName +  " : having data of length " + data.length);
                }catch(Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "Exception on executing : " + threadName  + ":"+ e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception on executing : " + e.getMessage());
        }

    }
}
