Set Android OS proxy to point to SandroProxy:

> Go to Android OS Settings->Wifi

> ![http://2.bp.blogspot.com/-0jqMFhVaUsg/UYCm49I4JbI/AAAAAAAAAOM/ZqVBKsEfHVM/s320/wifi-settings.png](http://2.bp.blogspot.com/-0jqMFhVaUsg/UYCm49I4JbI/AAAAAAAAAOM/ZqVBKsEfHVM/s320/wifi-settings.png)

> Long click on active wifi connection. Menu will appear. Choose Modify network.

> ![http://3.bp.blogspot.com/-4PXvXl7xzok/UYCm4Sw1B4I/AAAAAAAAAOE/vvtwjM5ejx0/s320/wifi-settings_modify.png](http://3.bp.blogspot.com/-4PXvXl7xzok/UYCm4Sw1B4I/AAAAAAAAAOE/vvtwjM5ejx0/s320/wifi-settings_modify.png)

> On the bottom there is option Advanced settings. Turn it on. Scroll down.

> ![http://4.bp.blogspot.com/-ZDwJuMtzyGk/UYCm3QxAZSI/AAAAAAAAAN8/qu3UF87qHsM/s320/wifi-settings_advanced.png](http://4.bp.blogspot.com/-ZDwJuMtzyGk/UYCm3QxAZSI/AAAAAAAAAN8/qu3UF87qHsM/s320/wifi-settings_advanced.png)

> Set proxy to manual, localhost 8020 for Drony or 8008 for SandroProxy

> ![http://4.bp.blogspot.com/-oUVBfOLga_c/UYCm479NEpI/AAAAAAAAAOU/R7gKhSGqDeE/s320/wifi-settings_set_proxy.png](http://4.bp.blogspot.com/-oUVBfOLga_c/UYCm479NEpI/AAAAAAAAAOU/R7gKhSGqDeE/s320/wifi-settings_set_proxy.png)


Set SandroProxy to connect to other proxy:

> Start SandroProxy and go to wizard if it doesn't appear. (Log tab->Menu->Wizard)

> ![http://3.bp.blogspot.com/-12TdrxCx8LI/UYCo7JopJeI/AAAAAAAAAOs/Q1o1i1bS0Gw/s320/sandroproxy_start_wizard.png](http://3.bp.blogspot.com/-12TdrxCx8LI/UYCo7JopJeI/AAAAAAAAAOs/Q1o1i1bS0Gw/s320/sandroproxy_start_wizard.png)

> Select **Connect to other proxy**

> Set (tcpip or hostname) and port to proxy, username (_domain\user_ or _realm\user_) and password

> Don't forget to activate with **Active** checkbox and click on **Set** button

> ![http://1.bp.blogspot.com/-IwSxLIpZJfk/UYCm4GYzbLI/AAAAAAAAAOA/C17WZOeWMwk/s320/sandroproxy_connect_to_other_proxy.png](http://1.bp.blogspot.com/-IwSxLIpZJfk/UYCm4GYzbLI/AAAAAAAAAOA/C17WZOeWMwk/s320/sandroproxy_connect_to_other_proxy.png)

You are done. All programs that respect android OS proxy settings will go through SandroProxy.

This will work if you have at least android 3.x