package netutils.parse;

import netutils.utils.ByteUtils;

/**
 * TCP packet over IPv6.
 * 
 * 
 * @author roni bar yanai
 * 
 */
public class TCPPacketIpv6 extends TCPPacket
{
	private static final int PSEUDO_HDR_LEN_IPv6 = 40;
	private IPv6Packet myIpv6Packet = null;

	/**
	 * create empty packet.
	 */
	public TCPPacketIpv6()
	{
		super(true);
		myIpv6Packet = (IPv6Packet) myIPPacket;
	}
	
	public TCPPacketIpv6(byte data[])
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
	
	public int getTCPChksum()
	{
		byte[] _pseudo_header = buildPseudoHeader();

		long sum = 0;

		// first run on the pseudo header 
		for (int i = 0; i < _pseudo_header.length; i += 2)
		{
			int byte1Val = _pseudo_header[i] & 0xff;
			int byte2Val = (i + 1 < _pseudo_header.length) ? _pseudo_header[i + 1] & 0xff : 0;
			sum = sum + ((byte1Val << 8) + byte2Val);
		}

		// run on the tcp part
		for (int i = EthernetFrame.ETHERNET_HEADER_LENGTH+myIPPacket.getIPHeaderLength(); i < myIPPacket.myPacket.length; i += 2)
		{
			
			int byte1Val = myIPPacket.myPacket[i] & 0xff;
			int byte2Val = (i + 1 < myIPPacket.myPacket.length) ? myIPPacket.myPacket[i + 1] & 0xff : 0;
			sum = sum + ((byte1Val << 8) + byte2Val);
		}

		while(sum >> 16 > 0)
		{
			sum = (sum >> 16) + (sum & 0xffff);
		}
		//sum = (sum >> 16) + (sum & 0xffff);
		//sum = sum + (sum >> 16);
		sum = ~sum & 0xffff;

		return (int)sum;
	}

	/**
	 * @return the pseudo header for the packet.
	 */
	protected byte[] buildPseudoHeader()
	{
		int pos = 0;
		byte[] _pseudo_header = new byte[PSEUDO_HDR_LEN_IPv6];
		System.arraycopy(myIpv6Packet.getSourceIP().getIpv6BigEndianByteaArray(), 0, _pseudo_header, pos, 16);
		pos += 16;
		System.arraycopy(myIpv6Packet.getDestinationIP().getIpv6BigEndianByteaArray(), 0, _pseudo_header, pos, 16);
		pos += 16;
		ByteUtils.setBigIndianInBytesArray(_pseudo_header, pos, getTotalTCPPlength(), 4);
		pos += 4;
		int n = IPPacketType.TCP;
		ByteUtils.setBigIndianInBytesArray(_pseudo_header, pos, n, 4);
		return _pseudo_header;

	}

	/**
	 * put the tcp packet inside the ipv6.
	 * @param payload - tcp as raw data including the tcp header.
	 */
	public void createTCPPacketIpv6(byte payload[])
	{
		myIPPacket.setIPProtocol(IPPacketType.TCP);

		int len = payload.length;
		myIPPacket.setData(payload);
		myIPPacket.createIPPacketBytes(len);
		// now put the TCP stuff.
		myTotalTcpLength = payload.length;

		// copy tcp part to its place in the ipv6 packet.
		int pos = EthernetFrame.ETHERNET_HEADER_LENGTH + myIPPacket.getIPHeaderLength();
		System.arraycopy(payload, 0, myIPPacket.myPacket, pos, payload.length);

		// checksum should be fixed as it uses the ip addresses which have
		// changed.
		pos = EthernetFrame.ETHERNET_HEADER_LENGTH + myIPPacket.getIPHeaderLength() + TCP_CHECKSUM_POS;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, 0, 2);
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getTCPChksum(), 2);
	}

	/**
	 * auto complete the fields and build all the required headers.
	 */
	public void createPacketBytes()
	{
		myIPPacket.setIPProtocol(IPPacketType.TCP);
		int len = getPayloadDataLength() + getTCPHeaderLength();
		myIPPacket.setData(new byte[len]);
		myIPPacket.createIPPacketBytes(len);
		// now put the TCP stuff.
		atuoComplete();
		int pos = EthernetFrame.ETHERNET_HEADER_LENGTH + myIPPacket.getIPHeaderLength();
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, mySrcPort, 2);
		pos += 2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, myDstPort, 2);
		pos += 2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getSequenceNumber(), 4);
		pos += 4;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getAcknowledgmentNumber(), 4);
		pos += 4;
		int n = ((getTCPHeaderLength() / 4) << 12) | (getAllFlags());
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, n, 2);
		pos += 2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getWindowSize(), 2);
		pos += 2;
		int chksumPos = pos;
		pos += 2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getUrgentPointer(), 2);
		pos+=2;
		
		// if we have tcp options configured we copy it.
		if (_tcpOptions != null)
		{
			System.arraycopy(_tcpOptions, 0, myIPPacket.myPacket,pos ,_tcpOptions.length);
			pos+=+_tcpOptions.length;
		}
	
		
		if (_tcpDataBytes != null)
		{
			pos = EthernetFrame.ETHERNET_HEADER_LENGTH + myIPPacket.getIPHeaderLength() + getTCPHeaderLength();
			System.arraycopy(_tcpDataBytes, 0, myIPPacket.myPacket, pos, _tcpDataBytes.length);
		}

		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, chksumPos, getTCPChksum(), 2);
		
		
	}
}
