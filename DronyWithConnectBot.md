# How it works #

ConnectBot can used to create SOCKS proxy to ssh server.

Drony makes ConnectBot connection through corporate/school/company proxy if needed.

After that all traffic goes to ConnectBot SOCKS server


# How to setup #

First try ConnectBot connection to ssh server in enviroment that you trust to have no restrictions.

Also check that PortForward Dynamic SOCKS server is set. Port will be used in Drony.

![http://3.bp.blogspot.com/-lUolxiMy-4o/U9pv0KIv9kI/AAAAAAAAAbA/gmTJ8ufoZo8/s320/cert_only_from_file_selection.png](http://3.bp.blogspot.com/-lUolxiMy-4o/U9pv0KIv9kI/AAAAAAAAAbA/gmTJ8ufoZo8/s320/cert_only_from_file_selection.png)

Now select in Drony local SOCKS server to be ConnectBot and use port that you setup.

![http://4.bp.blogspot.com/-kYbr_unfVE4/U9pw2AR9h-I/AAAAAAAAAbM/ibfXwoNBqq8/s320/drony_set_local_socks_proxy_connectbot_sel.png](http://4.bp.blogspot.com/-kYbr_unfVE4/U9pw2AR9h-I/AAAAAAAAAbM/ibfXwoNBqq8/s320/drony_set_local_socks_proxy_connectbot_sel.png)

Start Drony with VPN mode and ConnectBot.

If all goes okey, ConnectBot will ask Drony to create connection.

In some enviroments proxies doesn't allow connections to port 22 so maybe you should set your ssh server to listens on port 443.

Drony will then redirect all other apps traffic to ConnectBot SOCKS server.