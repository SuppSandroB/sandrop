package netutils.parse;

import netutils.utils.ByteUtils;


/**
 * IPv6 Address.
 * 
 * As IPv6 does not fit into a native data, it requires 128 bits, which
 * is 16 bytes, we need to put it in a data structure.
 * 
 * 
 * @author roni bar-yanai
 *
 */
public class IPv6Address implements IPAddress
{
	// the size of ipv6 in bytes.
	private static final short IPV6_SIZE = 16;
	
	// the ipv6 as byte array.
	private byte myIPv6Addr[] = null;
	
	
	private boolean _isBigEndian = false;
	/**
	 * Build address from its network representation.
	 * @param theIpv6Addr
	 */
	public IPv6Address(byte theIpv6Addr[])
	{
		if(theIpv6Addr.length != IPV6_SIZE)
		{
			throw new IllegalArgumentException("Invalid address length:"+theIpv6Addr.length);
		}
		myIPv6Addr = theIpv6Addr;
		_isBigEndian = true;
	}
	
	/**
	 * Create IPv6 address from a string comma separated, where each field is 2 bytes.
	 * 1080:0:0:0:8:800:200C:417A
	 * @param theIpAddr
	 */
	public IPv6Address(String theIpAddr)
	{
		String fields[] = theIpAddr.split("\\:");
		myIPv6Addr = new byte[IPV6_SIZE];
		for(int i=0 ; i<fields.length ; i++)
		{
			int n = Integer.parseInt(fields[i], 16);
			ByteUtils.setLittleIndianInBytesArray(myIPv6Addr, IPV6_SIZE - i*2 - 2, n, 2);
		}
	}
	
	/**
	 * inc ip by one.
	 */
	public void inc()
	{
		for(int i=0 ; i<IPV6_SIZE ; i++)
		{
			int n = (myIPv6Addr[i] & 0xff) + 1;
			if (n > 0xff)
			{
				myIPv6Addr[i] = 0;
			} else {
				myIPv6Addr[i] = (byte) n;
				break;
			}
		}
	}
	
	/**
	 * 
	 * @return the ip in a readable format
	 *   2001::FFef::.....
	 */
	public String getAsReadableString()
	{
		StringBuffer toRet = new StringBuffer();
		for(int i=0 ; i<IPV6_SIZE ; i++)
		{
			if (i%2 == 0 && i>0)
			{
				toRet.append("::");
			}
			
			int n = myIPv6Addr[IPV6_SIZE-i-1] & 0xff;
			if (n<0x10)
			{
				toRet.append('0');
			}
			
			toRet.append(Integer.toHexString(n));
		}
		
		return toRet.toString();
	}
	
	/**
	 * 
	 * @return the ipv6 as byte array.
	 */
	public byte[] getIPv6AddressByteArray()
	{
		return myIPv6Addr;
	}
	
	/**
	 * 
	 * @return the ipv6 as big endian
	 */
	public byte[] getIpv6BigEndianByteaArray()
	{
		if(_isBigEndian)
		{
			return getIPv6AddressByteArray();
		}
		return ByteUtils.cobvertLittleToBig(myIPv6Addr);
	}

	/**
	 * Compare against other IPv6 address return true if greater then.
	 * @IPAdress theIp2 - the compared ip, must be of the same type,
	 *  that is IPv6Address.
	 */
	public boolean isGreater(IPAddress theIp2)
	{
		if( !(theIp2 instanceof IPv6Address))
		{
			throw new IllegalArgumentException("IP must be of same type");
		}
		
		IPv6Address ip2 = (IPv6Address) theIp2;
		
		byte[] addr = ip2.getIPv6AddressByteArray();
		
		for(int i=0 ; i<IPV6_SIZE ; i++)
		{
			if((myIPv6Addr[IPV6_SIZE-i-1] & 0xff) >( addr[IPV6_SIZE-i-1] & 0xff))
				return true;
			
			if ((myIPv6Addr[IPV6_SIZE-i-1] & 0xff) != ( addr[IPV6_SIZE-i-1] & 0xff))
			{
				return false;
			}
		}
		
		return false;
	}
	
	private static final int PRIME = 31;

	@Override
	public int hashCode()
	{
		int result = 1;
		for(int i=0 ; i<myIPv6Addr.length ; i++)
		{
			result += (myIPv6Addr[i] & 0xff);
		}
		result = result * PRIME;
		
		return result;
	}
	
	@Override
	public boolean equals(Object arg0)
	{
		if( !(arg0 instanceof IPv6Address))
				return false;
		
		IPv6Address ipv6 = (IPv6Address) arg0;
		
		for(int i=0 ; i<myIPv6Addr.length ; i++)
		{
			if(myIPv6Addr[i] != ipv6.myIPv6Addr[i])
				return false;
		}
		
		return true;
	}

}
