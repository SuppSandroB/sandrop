package netutils.parse;

import netutils.NetUtilsException;
import netutils.NetUtilsFragmentException;
import netutils.utils.IP;


/**
 * Five Tuple is built of (source ip,destination ip,source port,destination port
 *  ,protocol type).<br>
 * Five tuple uniquely identifies a flow between to hosts on the network on a 
 * certain time. The tuple is not ordered.<br>
 * 
 * Note: The five tuple can be used as a key in hash, equals and hash are implemented 
 *  to match two keys if they have matching pairs of ip,port regardless of the direction. 
 * 
 * @author roni bar-yanai
 *
 */
public class FiveTuple
{
	
	private final static long MAX_IP = 0xffffffffl;
	
	// five tuple parameters.
	//protected long mySrcIp = 0;

	//protected long myDstIp = 0;

	private IPAddress mySrcIp = null;
	
	private IPAddress myDstIp = null;
	
	protected int mySrcPort = 0;

	protected int myDstPort = 0;

	protected int myType = 0;

	/**
	 * create tuple
	 * @param theSrcIp
	 * @param theSrcPort
	 * @param theDstIp
	 * @param theDstPort
	 * @param theType
	 * @throws NetUtilsException
	 */
	public FiveTuple(long theSrcIp, int theSrcPort, long theDstIp, int theDstPort, int theType) throws NetUtilsException
	{
		myDstIp = new IPv4Address(theDstIp);
		myDstPort = theDstPort;
		mySrcIp = new IPv4Address(theSrcIp);
		mySrcPort = theSrcPort;
		myType = theType;
		
		if (isValid() == false)
			throw new NetUtilsException("Got non valid tuple : "+this.toString());
				
	}
	
	public FiveTuple(IPAddress theSrcIp, int theSrcPort, IPAddress theDstIp, int theDstPort, int theType)
	{
		myDstIp = theDstIp;
		myDstPort = theDstPort;
		mySrcIp = theSrcIp;
		mySrcPort = theSrcPort;
		myType = theType;
	}

	/**
	 * Extract the tuple, in case of none TCP/UDP, but IP will
	 * set the ports to zero. In case of none IP packet will throw exception.
	 * 
	 * @param packet
	 * @throws NetUtilsException
	 */
	public FiveTuple(byte[] packet) throws NetUtilsException
	{
		if (packet == null || packet.length == 0)
		{
			return;
		}
				
		if (EthernetFrame.statIsIpv4Packet(packet))
		{
			if (IPv4Packet.getIpProtocolType(packet) == IPPacketType.TCP)
			{
				TCPPacket tcppkt = new TCPPacket(packet);
				if(tcppkt.getUnderlyingIPPacketBase().isFragmented())
				{
					throw new NetUtilsFragmentException("TCP over IP fragment, not supported");
				}
				myType = IPPacketType.TCP;
				mySrcIp = ((IPv4Packet)tcppkt.getUnderlyingIPPacketBase()).getSourceIPv4();
				mySrcPort = tcppkt.getSourcePort();
				myDstIp = ((IPv4Packet)tcppkt.getUnderlyingIPPacketBase()).getDestinationIPv4();
				myDstPort = tcppkt.getDestinationPort();
			}
			else if (IPv4Packet.getIpProtocolType(packet) == IPPacketType.UDP)
			{
				UDPPacket udppckt = new UDPPacket(packet);
				if(udppckt.getUnderlyingIPPacketBase().isFragmented())
				{
					throw new NetUtilsFragmentException("UDP over IP fragment, not supported");
				}
				myType = IPPacketType.UDP;
				mySrcIp = ((IPv4Packet)udppckt.getUnderlyingIPPacketBase()).getSourceIPv4();
				mySrcPort = udppckt.getSourcePort();
				myDstIp = ((IPv4Packet)udppckt.getUnderlyingIPPacketBase()).getDestinationIPv4();
				myDstPort = udppckt.getDestinationPort();
			}
			else
			{
				IPv4Packet pkt = new IPv4Packet(packet);
				if(pkt.isFragmented())
				{
					throw new NetUtilsFragmentException("IP fragment, not supported");
				}
				myType = pkt.getIPProtocol();
				mySrcIp = pkt.getSourceIPv4();
				mySrcPort = 0;
				myDstIp =  pkt.getDestinationIPv4();
				myDstPort = 0;
			}
		}
		else if (EthernetFrame.statIsIpv6Packet(packet))
		{
			if (IPv6Packet.getIpProtocolType(packet) == IPPacketType.TCP)
			{
				TCPPacketIpv6 tcppkt = new TCPPacketIpv6(packet);
				myType = IPPacketType.TCP;
				mySrcIp = tcppkt.getUnderlyingIPPacketBase().getSourceIP();
				mySrcPort = tcppkt.getSourcePort();
				myDstIp = tcppkt.getUnderlyingIPPacketBase().getDestinationIP();
				myDstPort = tcppkt.getDestinationPort();
			}
			else if (IPv4Packet.getIpProtocolType(packet) == IPPacketType.UDP)
			{
				UDPPacket udppckt = new UDPPacket(packet);

				myType = IPPacketType.UDP;
				mySrcIp = udppckt.getUnderlyingIPPacketBase().getSourceIP();
				mySrcPort = udppckt.getSourcePort();
				myDstIp =  udppckt.getUnderlyingIPPacketBase().getDestinationIP();
				myDstPort = udppckt.getDestinationPort();
			}
			else
			{
				IPv6Packet pkt = new IPv6Packet(packet);
				
				myType = 0;//pkt.getIPProtocol();
				mySrcIp = pkt.getSourceIP();
				mySrcPort = 0;
				myDstIp =  pkt.getDestinationIP();
				myDstPort = 0;
			}
		} else
		{
			throw new NetUtilsException("Got non tcp | udp packet ");
		}
	}
	
	/**
	 * create five tuple from TCP packet
	 * @param thePkt
	 */
	public FiveTuple(TCPPacket thePkt)
	{
		myType = IPPacketType.TCP;
		mySrcIp = ((IPv4Packet)thePkt.getUnderlyingIPPacketBase()).getSourceIPv4();
		mySrcPort = thePkt.getSourcePort();
		myDstIp = ((IPv4Packet)thePkt.getUnderlyingIPPacketBase()).getDestinationIPv4();
		myDstPort = thePkt.getDestinationPort();
	}

	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object theArg0)
	{
		if (!(theArg0 instanceof FiveTuple))
		{
			return false;
		}
		FiveTuple tmp = (FiveTuple) theArg0;
		
		return (this.myDstIp.equals(tmp.myDstIp) && this.mySrcIp.equals(tmp.mySrcIp) && this.myDstPort == tmp.myDstPort && this.mySrcPort == tmp.mySrcPort && this.myType == tmp.myType) || isOpposite(tmp);
	}
	
	final int PRIME = 31;
	
	@Override
	public int hashCode() {
		int result = 1;
		if (mySrcIp.isGreater(myDstIp))
		{
			result = (int) (result + mySrcIp.hashCode());
			result = result + mySrcPort;
			result = (int) (result + myDstIp.hashCode());
			result = result + myDstPort;
			result = result + myType;
			result = result * PRIME;
		} 
		else
		{
			result = (int) (result + myDstIp.hashCode());
			result = result + myDstPort;
			result = (int) (result + mySrcIp.hashCode());
			result = result + mySrcPort;
			result = result + myType;
			result = result * PRIME;
		}
		
		return result;
	}

	/**
	 * 
	 * @param theFiveTouple
	 * @return true if have the same ip and ports but in opposite directions.
	 */
	public boolean isOpposite(FiveTuple theFiveTouple)
	{
		return (this.myDstIp.equals(theFiveTouple.mySrcIp) && this.mySrcIp.equals(theFiveTouple.myDstIp) && this.myDstPort == theFiveTouple.mySrcPort && this.mySrcPort == theFiveTouple.myDstPort && this.myType == theFiveTouple.myType);
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "Src ip : " + mySrcIp.getAsReadableString() + "\n" + "Src Port : " + mySrcPort + "\n" + "Dst ip : " + myDstIp.getAsReadableString() + "\n" + "Dst Port " + myDstPort + "\n" + "My type " + myType + "\n";
	}

	/**
	 * 
	 * @return the five tuple as a readable string.
	 */
	public String getAsReadbleString()
	{
		return "src_" + mySrcIp.getAsReadableString() + "." + mySrcPort + "dst_" + myDstIp.getAsReadableString() + "." + myDstPort;
	}

	/**
	 * five tuple key is a string of the ips,ports ordered by the bigger ip.
	 * this way both of flow direction will return the same key.
	 * @return five tuple key.
	 */
	public String getKey()
	{
		if(mySrcIp.isGreater(myDstIp))
		{
			return myDstIp.getAsReadableString() + ":" + myDstPort + ":" + mySrcIp.getAsReadableString() + ":" + mySrcPort + ":" + myType;
		}
		
		return mySrcIp.getAsReadableString() + ":" + mySrcPort + ":" + myDstIp.getAsReadableString() + ":" + myDstPort + ":" + myType;
	}

	/**
	 * 
	 * @return unique key that will identifies packets between same clients.
	 * (only ips are the same, may be different ports and ip protocol)
	 */
	public String getIpsAsKey()
	{
		if (myDstIp.isGreater(mySrcIp))
		{
			return mySrcIp.getAsReadableString() + ":" + myDstIp.getAsReadableString();
		}
		return myDstIp + ":" + mySrcIp;
	}

	/**
	 * @return the flow initiator ip as string
	 */
	public String getMySrcIpAsString()
	{
		return mySrcIp.getAsReadableString();
	}

	/**
	 * @return the flow initiatie as string
	 */
	public String getMyDstIpAsString()
	{
		return myDstIp.getAsReadableString();
	}

	/**
	 * @return the flow initiate port
	 */
	public int getDstPort()
	{
		return myDstPort;
	}

	/**
	 * @return the flow initiator port
	 */
	public int getSrcPort()
	{
		return mySrcPort;
	}
	
	/**
	 * 
	 * @return true if five tuple contains valid ips and ports.
	 */
	public boolean isValid()
	{
		return ( IP.isValidIp(mySrcIp.getAsReadableString()) || IP.isValidIp((myDstIp.getAsReadableString()))
		|| mySrcPort >= 0 || mySrcPort <= 0xffff || myDstPort >= 0 || myDstPort <= 0xffff);
			
	}

	/**
	 * 
	 * @return the ip protocol type (tcp/udp)
	 */
	public int getMyType()
	{
		return myType;
	}

	/**
	 * 
	 * @return source ip
	 */
	public IPAddress getMySrcIp()
	{
		return mySrcIp;
	}

	/**
	 * 
	 * @param mySrcIp
	 */
	/*public void setMySrcIp(long mySrcIp)
	{
		this.mySrcIp = mySrcIp;
	}*/

	/**
	 * 
	 * @return my destination ip
	 */
	public IPAddress getMyDstIp()
	{
		return myDstIp;
	}

	/**
	 * 
	 * @param theDstIp
	 */
	/*public void setMyDstIp(long theDstIp)
	{
		if (theDstIp < 0 || theDstIp > MAX_IP)
		{
			throw new IllegalArgumentException("Got illegal ip");
		}
		this.myDstIp = theDstIp;
	}*/

	/**
	 * 
	 * @return source port
	 */
	public int getMySrcPort()
	{
		return mySrcPort;
	}

	/**
	 * 
	 * @param theSrcPort
	 */
	public void setMySrcPort(int theSrcPort)
	{
		if (theSrcPort < 0 || theSrcPort > MAX_IP)
		{
			throw new IllegalArgumentException("Got illegal ip");
		}
		this.mySrcPort = theSrcPort;
	}

	/**
	 * 
	 * @return destination port
	 */
	public int getMyDstPort()
	{
		return myDstPort;
	}

	/**
	 * set the destination port 
	 * @param myDstPort
	 */
	public void setMyDstPort(int myDstPort)
	{
		this.myDstPort = myDstPort;
	}

	/**
	 * 
	 * @param myType
	 */
	public void setMyType(int myType)
	{
		this.myType = myType;
	}
	
	/**
	 * 
	 * @return true if tuple type is TCP.
	 */
	public boolean isTCP()
	{
		return  myType == IPPacketType.TCP;
	}
	
	/**
	 * 
	 * @return true if tuple type is UDP.
	 */
	public boolean isUDP()
	{
		return  myType == IPPacketType.UDP;
	}
	
	public String oneLineReadbleString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(mySrcIp.getAsReadableString()+":"+mySrcPort+"<-->"+myDstIp.getAsReadableString()+":"+myDstPort+" "+IPPacketType.getTypeAsString(myType));
		return sb.toString();
	}
	
	static public FiveTuple reverseFiveTuple(FiveTuple ft) throws NetUtilsException
	{
		return new FiveTuple(ft.myDstIp,ft.myDstPort,ft.mySrcIp,ft.mySrcPort,ft.myType);
	}
	
}
