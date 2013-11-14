package netutils.parse;

/**
 * UDP packet over ipv4
 * 
 * @author roni bar yanai 
 */
public class UDPPacketIpv4 extends UDPPacket
{

	// internal reference to save casting each time 
	private IPv4Packet myIpPacketV4 = null;
	
	public UDPPacketIpv4(byte[] thePacket)
	{
		super(thePacket);
		myIpPacketV4 = (IPv4Packet) myIPPacket;
	}
	
	public UDPPacketIpv4()
	{
		super();
		myIpPacketV4 = (IPv4Packet) myIPPacket;
	}

	
	/** 
	 *@return fragmentation flags field (from ip header).
	 */
	public int getFragmentFlags()
	{
		return myIpPacketV4.getFragmentFlags();
	}
	

	/** 
	 *@return the fragment offset.
	 */
	public int getFragmentOffset()
	{
		return myIpPacketV4.getFragmentOffset();
	}

	/**
	 * @return the ID of the IP segment. 
	 **/
	public int getId()
	{
		return myIpPacketV4.getId();
	}

	/**
	 * @return the time to live. 
	 */
	public int getTTL()
	{
		return myIpPacketV4.getTTL();
	}

	/** 
	 * @return the header checksum.
	 */
	public int getIPChecksum()
	{
		return myIpPacketV4.getIPChecksum();
	}

	/**
	 * set the ip
	 * @param theIp - 32 bit long.
	 */
	public void setSrcIp(long theIp)
	{
		myIpPacketV4.setSrcIp(theIp);
		
	}

	/**
	 * set the destination IP
	 * @param theDstIp
	 */
	public void setDstIp(long theDstIp)
	{
		myIpPacketV4.setDstIp(theDstIp);		
	}
}
