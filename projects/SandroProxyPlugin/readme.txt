Build instruction:


X> Checkout both projects SandroProxyLib and SandroProxyPlugin

X> Test plugin with project SandroProxyPlugin until it is ready.

X> To get jar that can be used in sandroproxy you should build with ant.
   First check local.properties file that it points to android sdk. (android update project --path .)
   Also you need to set local.properties in SandroProxyLib project to point to proper sdk location
 
 ant debug
 
  There will be some warrnings about included jars (bsh,...). Just ignore them. They are not used yet.

X> Now you have file in assets dir that can be used in SandroProxy as plugin
   custom_plugin_dex.jar
   You can import to SandroProxy as local file (/mnt/sdcard/<file_name>.jar)
   or as web download (e.g: https://dl.dropbox.com/u/2323/<file_name>.jar)

