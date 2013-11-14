package netutils.parse;

/**
 * The class implements TCP packet over IPv4.
 * The purpose is the allow specific methods for accessing IPv4 specifics
 *  such as IP.
 * 
 * @author roni bar yanai
 *
 */
public class TCPPacketIpv4 extends TCPPacket
{

	public TCPPacketIpv4()
	{
		super();
	}

	public TCPPacketIpv4(byte[] thePacket)
	{
		super(thePacket);
	}

	/**
	 * Set src IP.
	 * If underlying IP protocol is not IPv4 then will throw exception. 
	 * @param theIp
	 */
	public void setSrcIp(long theIp)
	{
		if(myIPPacket.isIPv4())
		{
			((IPv4Packet) myIPPacket).setSrcIp(theIp);
		}
		else 
		{
			throw new IllegalPacketException("underlying IP protocol is IPv6");
		}
	}

	/**
	 * Set dst IP.
	 * If underlying IP protocol is not IPv4 then will throw exception. 
	 * @param theIp
	 */
	public void setDstIp(long theIp)
	{
		if(myIPPacket.isIPv4())
		{
			((IPv4Packet) myIPPacket).setDstIp(theIp);
		}
		else 
		{
			throw new IllegalPacketException("underlying IP protocol is IPv6");
		}
		
	}
	
	public void setFiveTuple(FiveTuple ft)
	{
		if (!ft.isTCP())
		{
			throw new IllegalArgumentException("Five Tuple is not TCP type");
		}
		setDstIp(((IPv4Address)ft.getMyDstIp()).getIPasLong());
		setSrcIp(((IPv4Address)ft.getMySrcIp()).getIPasLong());
		setSourcePort(ft.getMySrcPort());
		setDestinationPort(ft.getMyDstPort());
	}

}
