package netutils.parse;

/**
 * The class implements SCTP packet over IPv4.
 * The purpose is the allow specific methods for accessing IPv4 specifics
 *  such as IP.
 * 
 * @author roni bar yanai
 *
 */
public class SCTPPacketIpv4 extends SCTPPacket
{

	public SCTPPacketIpv4()
	{
		super();
	}

	public SCTPPacketIpv4(byte[] thePacket)
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
}
