package netutils.parse;

import java.io.Serializable;

/**
 * Common interface for IPv6 and IPv4 addresses.
 * 
 * 
 * @author roni bar-yanai
 *
 */
public interface IPAddress extends Serializable
{
	public boolean isGreater(IPAddress ip2);
	
	public String getAsReadableString();
}
