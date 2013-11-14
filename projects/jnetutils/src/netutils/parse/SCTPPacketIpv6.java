package netutils.parse;


/**
 * SCTP packet over IPv6.
 * 
 * 
 * @author roni bar yanai
 * 
 */
public class SCTPPacketIpv6 extends SCTPPacket
{
	private static final int PSEUDO_HDR_LEN_IPv6 = 40;
	private IPv6Packet myIpv6Packet = null;

	/**
	 * create empty packet.
	 */
	public SCTPPacketIpv6()
	{
		super(true);
		myIpv6Packet = (IPv6Packet) myIPPacket;
	}
	
	public SCTPPacketIpv6(byte data[])
	{
		super(data);
		myIpv6Packet = (IPv6Packet) myIPPacket;
	}

	/**
	 * set source ip.
	 * 
	 * @param theAddr
	 */
	public void setSrcIp(IPv6Address theAddr)
	{
		myIpv6Packet.setSrcIp(theAddr);
	}

	/**
	 * set destination ip.
	 * 
	 * @param theAddr
	 */
	public void setDstIp(IPv6Address theAddr)
	{
		myIpv6Packet.setDstIp(theAddr);
	}
	
}
