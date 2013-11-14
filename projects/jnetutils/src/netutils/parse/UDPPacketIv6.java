package netutils.parse;

import netutils.utils.ByteUtils;

/**
 * UDP packet over ipv6.
 * 
 * 
 * @author roni bar yanai
 *
 */
public class UDPPacketIv6 extends UDPPacket
{
	private static final int PSEUDO_HDR_LEN_IPv6 = 40;
	private IPv6Packet myIpv6Packet = null;
	
	public UDPPacketIv6()
	{
		super(true);
		myIpv6Packet = (IPv6Packet) myIPPacket;
	}
	
	public UDPPacketIv6(byte thePacket[])
	{
		super(thePacket);
		myIpv6Packet = (IPv6Packet) myIPPacket;
	}
	
	/**
	 * set packet source address.
	 * @param theAddr
	 */
	public void setSrcIp(IPv6Address theAddr)
	{
		myIpv6Packet.setSrcIp(theAddr);
	}
	
	/**
	 * set packet destination address
	 * @param theAddr
	 */
	public void setDstIp(IPv6Address theAddr)
	{
		myIpv6Packet.setDstIp(theAddr);
	}
	
	/**
	 * @return the pseudo header for the packet.
	 */
	protected byte[] buildPseudoHeader()
	{
		if (!myIPPacket.isIPv4())
		{
			int pos = 0;
			byte[] _pseudo_header = new byte[PSEUDO_HDR_LEN_IPv6];
			System.arraycopy(myIpv6Packet.getSourceIP().getIpv6BigEndianByteaArray(), 0, _pseudo_header, pos, 16);
			pos+=16;
			System.arraycopy(myIpv6Packet.getDestinationIP().getIpv6BigEndianByteaArray(), 0, _pseudo_header, pos, 16);
			pos+=16;
			ByteUtils.setBigIndianInBytesArray(_pseudo_header,
					pos, getUDPLength(), 4);
			pos+=4;
			int n = IPPacketType.UDP;
			ByteUtils.setBigIndianInBytesArray(_pseudo_header,
					pos, n, 4);
			return _pseudo_header;
		}
		throw new UnsupportedOperationException("Not supported yet");
	}
	

	/**
	 * 
	 * @param payload
	 */
	public void createUDPPacketIpv6(byte payload[])
	{
		myIPPacket.setIPProtocol(IPPacketType.UDP);
		int len = payload.length;
		myIPPacket.setData(payload);
		myIPPacket.createIPPacketBytes(len);
		// now put the UDP stuff.
		// copy UDP part to its place in the ipv6 packet.
		int pos = EthernetFrame.ETHERNET_HEADER_LENGTH+ myIPPacket.getIPHeaderLength();
		System.arraycopy(payload, 0, myIPPacket.myPacket,pos ,payload.length);
	
	}

}
