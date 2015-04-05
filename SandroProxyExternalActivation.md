Start proxy:
http://code.google.com/p/sandrop/wiki/SandroProxyExternalActivation?action=startProxy

Stop proxy:
http://code.google.com/p/sandrop/wiki/SandroProxyExternalActivation?action=stopProxy

If any of you are wondering how to send an Intent from your app to start , this is the command:



```

String url = "http://code.google.com/p/sandrop/wiki/SandroProxyExternalActivation?action=startProxy";

Intent i2 = new Intent(Intent.ACTION_VIEW);

i2.addCategory("android.intent.category.DEFAULT");

i2.addCategory("android.intent.category.BROWSABLE");

i2.setData(Uri.parse(url)); startActivity(i2);

```


