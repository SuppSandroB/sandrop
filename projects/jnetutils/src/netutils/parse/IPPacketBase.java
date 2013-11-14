package netutils.parse;

/**
 * The class defines common actions for ipv4 and ipv6, making 
 *  underlying protocols api (such as TCP), transparent to the ip version
 *  as possible.
 *  
 * @author roni bar-yanai
 */
public abstract class IPPacketBase extends EthernetFrame
{
	// local pointers for important offsets and ip header members.
	// to save performance when parsed all members are evaluated only on demand and kept
	// in a member for reuse.
	protected int myIPHdrOffset = 0;
	protected int myIPDataOffset = 0;
	protected int myIPHdrLength = 0;
	
	protected boolean _isWriteDstIp = false;
	
	protected IPPacketBase(byte[] thePacket)
	{
		super(thePacket);
	}

	protected IPPacketBase()
	{
		super();
	}
	
	/**
	 * will put defaults on all fields that were not set.<br>
	 * mandatory fields:<br>
	 * <br>
	 * src ip and src port.<br>
	 * dst ip and dst port.<br>
	 * payload <br>
	 * 
	 * @Override
	 */
	public abstract void atuoComplete();
	
	/**
	 * check if all mandatory fields are filled.<br>
	 *<br>
	 * mandatory fileds:<br>
	 * 1. src ip and src port.<br>
	 * 2. dst ip and dst port.<br>
	 * 3. data <br>
	 */
	public abstract boolean isMandatoryFieldsSet();
	

	/**
	 * set the payload.
	 * @param theData - byte array of the payload, mustn't be longer then the 
	 *  maximum allowed tcp payload.
	 */
	public abstract void setData(byte[] theData);

	/**
	 * Set the carried protocol id (TCP,UDP...etc)
	 * @param TheIpProtocol
	 */
	public abstract void setIPProtocol(int TheIpProtocol);
	
	/**
	 * 
	 * @return the IP packet total length (include all,but l2)
	 */
	public abstract int getIpPktTotalLength();

	/**
	 * 
	 * @return the IP header length. The length includes the IP options in case
	 *  of IPv4 and total extensions (except l4 extension TCP,UDP...) in case of
	 *  IPv6.
	 */
	public abstract int getIPHeaderLength();
	
	/**
	 * 
	 * @return Source IP
	 */
	public abstract IPAddress getSourceIP();
	
	/**
	 * 
	 * @return Destination IP
	 */
	public abstract IPAddress getDestinationIP();
		
	/**
	 * @return Source IP as readable string
	 */
	public abstract String getSourceIPAsString();
	
	/**
	 * @return Destination IP as readable string
	 */
	public abstract String getDestinationIPAsString();
	
	/** 
	 * @return the IP data (payload) as a byte array.
	 */
	public abstract byte[] getIPData();
	
	/**
	 * @return true if packet version is IPv4 and else otherwise.
	 */
	public abstract boolean isIPv4();
	
	/**
	 * @return true if this is a fragment packet.
	 */
	public boolean isFragmented()
	{
	    return false;  
	}
	
	public abstract void toReadableText(StringBuffer sb);
		
	// protected function for internal use.
	
	protected abstract void atuoCompleteNoData();
	
	protected abstract boolean isMandatoryFieldsSetNoData();
	
	protected abstract void setTotaIPLength(int theLengh);

	protected void createIPPacketBytes(int len)
	{
		super.createIPPacketBytes(len+getIPHeaderLength());
		myIPDataOffset = getIPHeaderLength();
		putIPHeader();
	}
	
	protected abstract void putIPHeader();
}
