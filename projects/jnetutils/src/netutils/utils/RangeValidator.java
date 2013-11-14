package netutils.utils;

/**
 * Helper class for validating ranges.
 * 
 * @author roni bar-yanai
 *
 */
public class RangeValidator
{
	public static void checkRangeUint8(int num)
	{
		if (num<0 || num  > 0xff)
			throw new IllegalArgumentException("Number is out range:"+num);
	}
	
	public static void checkRangeInt8(int num)
	{
		if ( num < 128 || num >127)
			throw new IllegalArgumentException("Number is out range:"+num);
	}
	
	
	public static void checkRangeUint16(int num)
	{
		if (num<0 || num  > 0xffff)
			throw new IllegalArgumentException("Number is out range:"+num);
		
	}

	public static void checkRangeUint32(long num)
	{
		if (num<0 || num  > 0xffffffffl)
			throw new IllegalArgumentException("Number is out range:"+num);
		
	}
	
	public static void checkRangeBits(int num,int bits)
	{
		if ( (num >> bits) > 0)
			throw new IllegalArgumentException("Number is out range:"+num);
	}

	public static void checknull(byte data[])
	{
		if ( data == null)
			throw new IllegalArgumentException("data is null");
	}
}
