![http://3.bp.blogspot.com/-C-fNp5sjiHE/TpcucLt9NqI/AAAAAAAAABs/e_GVQg8iWos/s1600/sandrob_teaser_black_2.png](http://3.bp.blogspot.com/-C-fNp5sjiHE/TpcucLt9NqI/AAAAAAAAABs/e_GVQg8iWos/s1600/sandrob_teaser_black_2.png)


**SSL Proxy, http analyzer for android**

https://play.google.com/store/apps/details?id=org.sandroproxy

**Make your custom plugin to handle request/response flow**

http://code.google.com/p/sandrop/issues/detail?id=31#c3

Proxy acts as SSL man-in-the-middle. It generates sites certificates on the fly.
Issuer is named UNTRUSTED.

Based on webscarab.
So all the credits goes there.

https://www.owasp.org/index.php/Category:OWASP_WebScarab_Project



Requests/Responses are stored in getExternalCacheDir()

/mnt/sdcard/Android/data/org.sandroproxy/cache

http://developer.android.com/reference/android/content/Context.html#getExternalCacheDir()

There is no security enforced with these files. All applications can read and write files placed here.




Use stock browser and change that wi-fi uses proxy on localhost:8008

http://code.google.com/p/sandrob/issues/detail?id=41#c27