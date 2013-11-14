package netutils.parse.ipv6;

/**
 * .
 * 
 * @author roni bar yanai
 *
 */
public abstract class IPv6ExtBasic implements IPv6Extension
{
	protected static final int IPV6_HOP_BY_HOP_TYPE = 0;
	protected static final int IPV6_ROUTING_TYPE = 43;
	protected static final int IPV6_FRAGMENT_TYPE = 44;
	
	protected int myNextHdr = 0;
	
	protected byte[] myOptionData = new byte[0];
	
	@Override
	public byte[] getAsRawByArray()
	{
		byte arr[] = new byte[getLength()];
		arr[0] = (byte) myNextHdr;
		arr[1] = (byte) ((myOptionData.length+2)/8);
		arr[1] = (byte) (arr[1]>0?arr[1]-1:arr[1]);
		System.arraycopy(myOptionData, 0, arr, 2, myOptionData.length);
		return arr;
	}

	@Override
	public int getLength()
	{
		return myOptionData.length+2;
	}

	@Override
	public int getNextType()
	{
		return myNextHdr;
	}

	protected void setMyOptionData(byte[] optionData)
	{
		this.myOptionData = optionData;
	}
	
	public void setNextType(int type)
	{
		myNextHdr = type;
	}	
}
