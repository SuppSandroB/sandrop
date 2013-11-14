package netutils.parse;

import netutils.utils.IP;

/**
 * IPv4 address.
 * 
 * @author roni bar yanai
 *
 */
public class IPv4Address implements IPAddress
{
	private long myValue = 0;
	
	public IPv4Address(long theIP)
	{
		myValue = theIP;
		if (theIP<0 || theIP > 0xffffffffl)
			throw new IllegalArgumentException("Not in range:"+theIP);
	}
	
	public IPv4Address(String theIP)
	{
		if(!IP.isValidIp(theIP))
			throw new IllegalArgumentException("Not a valid IP:"+theIP);
		
		myValue = IP.getIPAsLong(theIP);
	}
	
	@Override
	public boolean isGreater(IPAddress theIp2)
	{
		if( !(theIp2 instanceof IPv4Address))
		{
			throw new IllegalArgumentException("IP must be of same type");
		}
		
		return myValue > ((IPv4Address)theIp2).myValue;
	}

	@Override
	public String getAsReadableString()
	{
		return IP.getIPAsString(myValue);
	}
	
	@Override
	public int hashCode()
	{
		return (int) myValue;
	}
	
	@Override
	public boolean equals(Object arg0)
	{
		if(! (arg0 instanceof IPv4Address))
			return false;
		return myValue == ((IPv4Address)arg0).myValue;
	}
	
	public long getIPasLong()
	{
		return myValue;
	}

}
