package netutils.utils;

import netutils.parse.*;

/**
 * Utility functions for ipv4.
 *  
 * @author roni bar-yanai
 */
public class IP
{
	private static final long MAX_IP = 4294967296L;
	private static final int IPV6_SIZE = 16;

	/**
	 * The function receive an IP (string "x.x.x.x") and return 
	 * the next ip (as string).
	 * ( after 255.255.255.255 will return to 0.0.0.0 ). 
	 * @param theIp
	 * @return the next ip
	 */
	public static String incIp(String theIp)
	{
		long ipAsLong = getIPAsLong(theIp);
		ipAsLong++;

		ipAsLong = (ipAsLong > MAX_IP) ? 0 : ipAsLong;

		return getIPAsString(ipAsLong);
	}
	
	/** 
	 * The function receive an IP (integer) and a number.
	 * The function will return IP which is the sum of the both 
	 * 
	 * @return ip
	 */ 
	public static String incIp(int incBy , String theIp)
	{
		long ipAsLong = getIPAsLong(theIp);
		ipAsLong+=incBy;

		ipAsLong = (ipAsLong > MAX_IP) ? 0 : ipAsLong;

		return getIPAsString(ipAsLong);
	}
	

	/**
	 * Recive String ip and inc the ip filed.
	 * [field 0].[field 1].[field 2].[field 3]
	 * @param theIp
	 * @return the next ip
	 */
	public static String incIp(String theIp, int field)
	{
        return incIp(theIp,field,1);
	}
    
    public static String incIp(String theIp,int theField,int theGap)
    {
        if (theField < 0 || theField > 3) throw new IllegalArgumentException("range should be 0-3, got :" + theField);
        long ipAsLong = getIPAsLong(theIp);
        ipAsLong = (long) (ipAsLong + theGap*Math.pow(256, 3 - theField));

        ipAsLong = (ipAsLong > MAX_IP) ? ipAsLong % MAX_IP : ipAsLong;

        return getIPAsString(ipAsLong);
    }
    
    /**
     * return ip after mask. for example "10.10.10.10" and mask 16
     * will return "10.10.0.0"
     * @param theIp
     * @param bitmask
     * @return masked ip.
     */
    public String getSubnetFromIp(String theIp,int bitmask)
    {
        if(bitmask>32 || bitmask<0)
        	throw new IllegalArgumentException("bitmask should be between 0 and 32!");
        int ip = (int) IP.getIPAsLong(theIp);
               
        int count = bitmask;
        int sub = 0;
        while(count>0)
        {
            sub = sub >> 1;
            sub = sub | 0x80000000;
            count--;
        }
        ip = ip & sub;
    
        return IP.getIPAsString( ip & 0xffffffffl);
    }

	/**
	 * get ip as byte array in network order and return the ip as string
	 * @param bytesArr
	 * @return the ip as readable string (x.x.x.x)
	 */
	public static String getIpFromBytes(byte[] bytesArr)
	{
		String st = new String();
		for (int n = 0; n < 4; n++)
		{
			st += (int) ((char) bytesArr[n] % 256);
			if (n < 3)
			{
				st += ".";
			}
		}
		return st;
	}

	/**
	 * The function receive an IP (string "x.x.x.x") and return 
	 * the previous ip (as string).
	 * ( after 255.255.255.255 will return to 255.255.255.254 ). 
	 * @param theIp
	 * @return the next ip
	 */
	public static String decIp(String theIp)
	{
		long ipAsLong = getIPAsLong(theIp);
		ipAsLong--;

		ipAsLong = (ipAsLong < 0) ? MAX_IP : ipAsLong;

		return getIPAsString(ipAsLong);
	}

	/**
	 * Get String ip and return its value as long
	 * @param theIP
	 * @return the ip as long
	 */
	public static long getIPAsLong(String theIP)
	{
		return IPStringToNum(theIP);
	}

	/**
	 * Get String ip and return its number value.
	 * @param theIp - as String
	 * @return its value
	 */
	public static long IPStringToNum(String theIp)
	{
		String[] fields = stringToArray(theIp, ".");
		long sum = 0;
		for (int i = 0; i < fields.length; i++)
		{
			sum = sum * 256 + Integer.parseInt(fields[i]);
		}
		return sum;
	}

	/**
	 * This method get a string and a delimiter.
	 * All the field in the string are extract into a array that is return by the method.
	 *
	 * @param inString  The string to work on.
	 * @param delimiter The delimiter
	 * @return A string array.
	 */
	private static String[] stringToArray(String inString, String delimiter)
	{
		int start = 0, end = 0;
		if (inString == null || delimiter == null)
		{
			return null;
		}
		int count = 1;
		int index = 0;
		int dl = delimiter.length();
		while (true)
		{
			index = inString.indexOf(delimiter, index);
			if (index < 0)
			{
				break;
			}
			count++;
			index += dl;
		}
		String array[] = new String[count];
		for (int i = 0; i < count; i++)
		{
			end = inString.indexOf(delimiter, start);
			if (end == -1)
			{ // The delimiter wasn't found
				array[i] = new String(inString.substring(start)); // extract from start to the end
				return array;
			}
			array[i] = new String(inString.substring(start, end));
			start = end + dl; // move the start to the the end of the last delimiter
		}
		return array;
	}

	/**
	 * Get String ip and return on of its fileds.
	 * @param theIp
	 * @param theField
	 * @return the field
	 */
	public static String getIPField(String theIp, int theField)
	{
		String[] strFields = stringToArray(theIp, ".");
		return strFields[theField];
	}

	/**
	 * The method will return true if the ip is valid ip.
	 * @param theIp - the ip as string
	 */
	public static boolean isValidIp(String theIp)
	{
		if (theIp == null) return false;

		String[] fields = theIp.split("\\.");

		if (fields.length != 4) return false;

		if (fields[0].equals("0")) return false;

		try
		{
			for (int i = 0; i < fields.length; i++)
			{
				int num = Integer.parseInt(fields[i]);
				if (num > 255 || num < 0) return false;
			}
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		return true;
	}

	/**
	 * The method will return true if the subnet is valid subnet.
	 * @param theIp - the ip as string
	 */
	public static boolean isValidSubnet(String theSubnet)
	{
		if (theSubnet.indexOf("/") == -1)
			return false;

		else
		{
			String fields[] = theSubnet.split("/");
			if (!isValidIp(fields[0].trim())) return false;

			try
			{
				int n = Integer.parseInt(fields[1].trim());
				if (n < 1 || n > 32) return false;
			}
			catch (RuntimeException e)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * get string ip and return one of its fields as int.
	 * [0].[1].[2].[3]
	 * @param theIp
	 * @param theField
	 * @return the field
	 */
	public static int getIpFieldAsInt(String theIp, int theField)
	{
		return Integer.parseInt(getIPField(theIp, theField));
	}

	/**
	 * get ip value and return as readble string.
	 * 
	 * @param theIp
	 * @return the ip as string
	 */
	public static String getIPAsString(long theIp)
	{
		String toReturn = "";
		for (int i = 0; i < 4; i++)
		{
			long field = theIp % 256;
			theIp = theIp / 256;
			toReturn = field + toReturn;
			if (i != 3) toReturn = "." + toReturn;
		}
		return toReturn;
	}

	/**
	 * compare 2 ips and return true if the first is bigger. 
	 * @param ip1 - ip as string
	 * @param ip2 - ip as string
	 * @return true for ip1>ip2 and flase otherwise.
	 */
	public static boolean isGreater(String ip1, String ip2)
	{
		return getIPAsLong(ip1) > getIPAsLong(ip2);
	}

	/**
	 * compare 2 ips and return true if the first is bigger or equal to second. 
	 * @param ip1 - ip as string
	 * @param ip2 - ip as string
	 * @return true for ip1>=ip2 and flase otherwise.
	 */
	public static boolean isGreaterEqual(String ip1, String ip2)
	{
		return getIPAsLong(ip1) >= getIPAsLong(ip2);
	}
	
	/**
	 * change ipv4 to ipv6.
	 * Will create IPv6 consisted of ipv4.ipv4.ipv4.ipv4
	 * @param ip
	 * @return new ipv6.
	 */
	public static IPv6Address convertIPv4ToIPv6(long ip)
	{
		byte toRet[] = new byte[IPV6_SIZE];
		for(int i=0 ; i<4 ; i++)
		{
			byte tmp[] = ByteUtils.getAs_uint32_NetOrder((int)ip);
			System.arraycopy(tmp, 0, toRet, i*4, 4);
		}
		return new IPv6Address(toRet);
	}
	
	/**
	 * change ipv4 to ipv6.
	 * Will create IPv6 consisted of ipv4.ipv4.ipv4.ipv4
	 * @param ip
	 * @return new ipv6.
	 */
	public static IPv6Address convertIPv4ToIPv6(IPAddress ipaddr)
	{
		if(!(ipaddr instanceof IPv4Address))
		{
			return (IPv6Address) ipaddr;
		}
		long ip = ((IPv4Address )ipaddr).getIPasLong();
		byte toRet[] = new byte[IPV6_SIZE];
		for(int i=0 ; i<4 ; i++)
		{
			byte tmp[] = ByteUtils.getAs_uint32_NetOrder((int)ip);
			System.arraycopy(tmp, 0, toRet, i*4, 4);
		}
		return new IPv6Address(toRet);
	}
}
