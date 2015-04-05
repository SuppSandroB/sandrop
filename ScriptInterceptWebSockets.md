# Implementation #

Because search condition can be very complex scripting is used. [BeanShell](http://www.beanshell.org/)

Logic can be written in java language and evaluated at each item in list.


_**Input to script:**_

_websocket message_

**request**

public abstract class WebSocketMessageDTO -[source](http://code.google.com/p/sandrop/source/browse/projects/SandroProxyLib/src/org/sandroproxy/websockets/WebSocketMessageDTO.java)

**Output:**

boolean **result** - true if item should be intercepted

_**Example of script:**_

```
import org.sandroproxy.websockets.WebSocketChannelDTO;
import org.sandroproxy.websockets.WebSocketMessageDTO;

// this will be replaced if click came from selected channel item
String currChannelId = "-1";

// should intercept message
result = false;

if (message != null && message.isOutgoing == true){
    if (!currChannelId.equals("-1")){
        if (message.channel != null 
            && message.channel.id.toString().equals(currChannelId)){
            result = true;
        }
    }else{
        result = true;
    }
}

```