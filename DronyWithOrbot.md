# How it works #

Orbot can be used to create local SOCKS proxy.

Drony makes Orbot connection through corporate/school/company proxy if needed.

After that all traffic goes to Orbot SOCKS server.
Also DNS traffic goes to Orbot DNS server.


# How to setup #

Make sure that android OS wifi proxy settings are None.


Not Manual and localhost 8020.


Drony currently only redirect traffic from vpn to Orbot.

Select in Drony local SOCKS server to be Orbot and use port 9050 and 5400.

![http://1.bp.blogspot.com/-IRssfMdcD4k/VMUM2j1eMXI/AAAAAAAAAdk/d_Pa88cDi-k/s320/device-2015-01-25-161024.png](http://1.bp.blogspot.com/-IRssfMdcD4k/VMUM2j1eMXI/AAAAAAAAAdk/d_Pa88cDi-k/s320/device-2015-01-25-161024.png)

Start Drony with VPN mode and start Orbot afterwards.

![http://3.bp.blogspot.com/-zrw7u3Yd45Y/VMUM4N5sFTI/AAAAAAAAAds/PhzUjyMKFNo/s320/start_drony.png](http://3.bp.blogspot.com/-zrw7u3Yd45Y/VMUM4N5sFTI/AAAAAAAAAds/PhzUjyMKFNo/s320/start_drony.png)

![http://1.bp.blogspot.com/-Hz75feQWKJM/VMUM6PM4qYI/AAAAAAAAAd0/N9-HNvzntJQ/s320/start_orbot.png](http://1.bp.blogspot.com/-Hz75feQWKJM/VMUM6PM4qYI/AAAAAAAAAd0/N9-HNvzntJQ/s320/start_orbot.png)


If all goes okey, Orbot will use Drony to create connection.

![http://2.bp.blogspot.com/-U8E-enG7Jqg/VMUM785StQI/AAAAAAAAAd8/X62JxvnJoZA/s320/notifications.png](http://2.bp.blogspot.com/-U8E-enG7Jqg/VMUM785StQI/AAAAAAAAAd8/X62JxvnJoZA/s320/notifications.png)

![http://1.bp.blogspot.com/-mfWkDAL1TGI/VMUM9oxo0JI/AAAAAAAAAeE/3YjN27-y6KA/s320/check_tor_chrome.png](http://1.bp.blogspot.com/-mfWkDAL1TGI/VMUM9oxo0JI/AAAAAAAAAeE/3YjN27-y6KA/s320/check_tor_chrome.png)


Drony will then redirect all other apps traffic to Orbot SOCKS server.