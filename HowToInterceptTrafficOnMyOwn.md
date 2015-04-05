One way to capture is to set proxy in android wifi advanced settings.

Other is to redirect traffic with iptables rules. In SandroProxy all http(80) goes to 8009 and https(443) goes to 8010.

On this ports SandroProxy must listen as transparent proxy:

http://code.google.com/p/sandrop/source/browse/projects/SandroProxyLib/src/org/sandrop/webscarab/plugin/proxy/Proxy.java#654

Listen for http is trivial. You get plain text request and you parse where you must connect.

Https is more tricky. You must respond to client with proper certificate before you get plain text request.

JNI library socketdest is used to get socket destination data tcpip:port.

Then proxy connects to tcpip:port to retrive ssl info to generate server certificate.

So you should check what kind of iptables rules SandroProxyPlugin generates and do similar one.

http:

iptables -A -p tcp --dport 80 -j ACCEPT

iptables -A -t nat -p tcp --dport 80 -j REDIRECT --to-port 8009

iptables -t nat -A  -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 80 -j DNAT --to 127.0.0.1:8009


https:

iptables -A -p tcp --dport 443 -j ACCEPT

iptables -A -t nat -p tcp --dport 443 -j REDIRECT --to-port 8010

iptables -t nat -A  -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 443 -j DNAT --to 127.0.0.1:8010

Best way to start is to use SandroProxyPlugin app as template.

http://code.google.com/p/sandrop/source/browse/projects/SandroProxyPlugin/

http://code.google.com/p/sandrop/source/browse/projects/SandroProxyPlugin/src/org/sandroproxy/plugin/gui/MainActivity.java?spec=svn6e9a524c6ac09c2fbc6a8e11d52ad62e6d63b8e6&name=1_4_77_plugin_proj_new_features#219