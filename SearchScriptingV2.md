# Implementation #

Because search condition can be very complex scripting is used. [BeanShell](http://www.beanshell.org/)

Logic can be written in java language and evaluated at each item in list.


_**Input to script:**_

_HTTP request that can be sent to an HTTP server_

**request**

public class **Request** extends Message  - [source](http://code.google.com/p/sandrop/source/browse/projects/SandroProxyLib/src/org/sandrop/webscarab/model/Request.java)

_HTTP response as sent by an HTTP server_

**response**

public class **Response** extends Message - [source](http://code.google.com/p/sandrop/source/browse/projects/SandroProxyLib/src/org/sandrop/webscarab/model/Response.java)

it can be null if response is not retrieved from server so it should be checked for null


**Output:**

boolean **result** - true if item should be show on list

_**Example of script:**_

```
// needed imports for scripting
import org.sandrop.webscarab.model.Request;
import org.sandrop.webscarab.model.Response;
import org.sandrop.webscarab.model.Message;
import org.sandrop.webscarab.model.HttpUrl;

// simple logic checking if request
// has google in hostname
// if conditions are met item is shown on the list
if(true){
    String hostName = request.getURL().getHost();
    if(hostName.contains("google")) result = true;
}
```

public class Message [source](http://code.google.com/p/sandrop/source/browse/projects/SandroProxyLib/src/org/sandrop/webscarab/model/Message.java)