package netutils.build;

import netutils.utils.RandomGenerator;


/**
 * Utility class for mac address manipulation. 
 * 
 * @author roni bar-yanai
 */
public class MACAddress 
{
	private final static int NUM_OF_FIELDS = 6;
	
	private byte[] myBytes = null;
	
	/**
	 * create new mac address
	 * @param theBytes - the bytes array (length == 6).
	 */
	public MACAddress(byte[] theBytes)
	{
		myBytes = theBytes;
		if (myBytes.length < NUM_OF_FIELDS)
		{
			byte[] tmp = new byte[NUM_OF_FIELDS];
			System.arraycopy(myBytes,0,tmp,NUM_OF_FIELDS-myBytes.length,myBytes.length);
			myBytes = tmp;
		}
	}
	
	/**
	 * create new mac.
	 * @param theMac - the mac string "00:0d:dd:e1:12:4f"
	 * The max should include all bytes including the padding zeros
	 */
	public MACAddress(String theMac)
	{
		String[] fields = theMac.split(":");
		if (fields.length > NUM_OF_FIELDS)
		{
			String[] tmp = new  String[NUM_OF_FIELDS];
			System.arraycopy(fields,0,tmp,0,NUM_OF_FIELDS);
			fields = tmp;
		}
		myBytes = new byte[NUM_OF_FIELDS];
		for (int i=NUM_OF_FIELDS - fields.length ; i<NUM_OF_FIELDS ; i++)
		{
			myBytes[i] = Integer.decode("0x"+fields[i-(NUM_OF_FIELDS - fields.length)]).byteValue();
		}
	}
	
	/**
	 * create new mac
	 * @param theMacAsInt - the mac value. ( example "00:0d:00:00:01:4f" == 335) 
	 */
	public MACAddress(long theMacAsInt)
	{
		long sum =(theMacAsInt>0)?theMacAsInt:0;
		myBytes = new byte[NUM_OF_FIELDS];
		int counter = NUM_OF_FIELDS-1;
		while (sum>0)
		{
			myBytes[counter] = (byte)(sum % 256);
			sum = sum/256;
			counter--;
			if (counter<0)
				break;
		}
	}

	/**
	 * @return the mac address as byte array
	 */
	public byte[] asByteArray()
	{
		return myBytes;
	}
	
	/**
	 * return the mac as readable string.
	 */
	public String toString()
	{
		long sum =0;
		for (int i=0; i<myBytes.length ; i++)
		{
			sum=sum*256+(myBytes[i] & 0xff);
		}
		String toReturn = Long.toHexString(sum);
		for (int i= toReturn.length() ; i<12 ; i++)
		{
			toReturn = "0"+toReturn;
		}
		return toReturn;
	}
	
	/**
	 * @return the mac as string where ":" seperates between bytes.
	 */
	public String toReadbleString()
	{
		char[] tmpArr = new char[NUM_OF_FIELDS*3];
		int idx=0;
		for(int i=0 ; i<myBytes.length ; i++)
		{
			if (i > 0 )
			{
				tmpArr[idx++]=':';
			}
			int num = 0xff & myBytes[i];

			int second1 = (num & 0x0f);
			int first1 = ((num & 0xf0) >> 4);
			
			char second  = (char) ((second1<10)?'0'+second1:'A'+second1-10); 
			char first = (char) ((first1<10)?'0'+first1:'A'+first1-10);
			
			tmpArr[idx++]=first;
			tmpArr[idx++]=second;
		}
		return new String(tmpArr,0,idx);
	}
	
	private boolean _longSet = false;
	private long _longValue = 0;
	
	/**
	 * @return the mac as long value.
	 */
	public long getAsLong()
	{
		if (_longSet == true)
			return _longValue;
		
		long sum =0;
		for (int i=0; i<myBytes.length ; i++)
		{
			sum=sum*256+(myBytes[i] & 0xff);
		
		}
		_longValue = sum;
		_longSet = true;
		return sum;
	}
	
	/**
	 * increment mac by one.
	 * @param theMac
	 * @return the mac+1.
	 */
	public static MACAddress incMAC(MACAddress theMac)
	{
		long val = theMac.getAsLong();
		return new MACAddress(++val);
	}
	
	/**
	 * dec mac by one.
	 * @param theMAC
	 * @return the mac - 1.
	 */
	public static MACAddress decMAC(MACAddress theMAC)
	{
		long val = theMAC.getAsLong();
		return new MACAddress(++val);
	}
	
	/**
	 * @return random mac address
	 */
	public static MACAddress randMAC()
	{
		byte arr[] = new byte[NUM_OF_FIELDS];
	
		for(int i=0 ; i<NUM_OF_FIELDS ; i++)
		{
			arr[i] = (byte) RandomGenerator.getRandInt();
		}
		
		return new MACAddress(arr);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object theObj)
	{
		if (theObj == null || !(theObj instanceof MACAddress))
				return false;
		return getAsLong() == (((MACAddress)theObj).getAsLong());
	}
	
	@Override
	public int hashCode()
	{
		return (int) getAsLong();
	}
	
}
