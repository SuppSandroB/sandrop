# How to redirect app on device to local sandroproxy #

**1. first you check on which app would you like to redirect to transparent proxy**

APPS tab

**2. check in preferences that transparent proxy is enabled**

Transparent proxy = ON

**3. check version of iptables**

It should not be 1.3.x because have problems with nat table.

You can fix iptables with this app from market

https://play.google.com/store/apps/details?id=com.mgranja.iptables

**4. start proxy**

You can check iptables rules when proxy in active with Info Menu action.

Or from adb shell:

_**iptables -L**_

_**iptables -t nat -L**_

Capturing https is more tricky. It can be done but with some additional stuff.

When app make ssl request it states hostname.

If ssl server side certificate is not for the same hostname, by default connection is not trusted and dropped.

Sandroproxy has in settings that you can state name for generated certificate.

http://code.google.com/p/sandrop/issues/detail?id=40

Also you should put sandroproxy CA to android store.

http://code.google.com/p/sandrop/issues/detail?id=2

You can test if app will work if you make same request from browser and no popup that something is wrong with ssl appears.

If you redirect browser (native, opera, ...) to sandroproxy, and click continue on ssl popup, it will proceed.

To find out what kind of request app makes on ssl you should check in /proc/kmsg where iptables puts some info.

Or with this app.

https://play.google.com/store/apps/details?id=com.googlecode.networklog

FIX ME is from iptables command and will probably be gone in some new version.