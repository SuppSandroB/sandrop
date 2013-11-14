package netutils.parse;

/**
 * Factory class for creating the matching underlying ip packet object.
 * 
 * 
 * @author roni bar-yanai
 *
 */
public class IPFactory
{
	public static IPPacketBase createIPPacket(byte data[])
	{
		if(IPv4Packet.statIsIpv4Packet(data))
		{
			return new IPv4Packet(data);
		}
		
		if (IPv6Packet.statIsIpv6Packet(data))
		{
			return new IPv6Packet(data);
		}
		
		return null;
	}
	
	public static boolean isIPPacket(byte data[])
	{
		return IPv4Packet.statIsIpv4Packet(data) || IPv6Packet.statIsIpv6Packet(data);
	}
	
	public static boolean isTCPPacket(byte data[])
	{
		if(isIPPacket(data))
		{
			switch(EthernetFrame.statGetPacketType(data))
			{
			case EthernetFrame.ETHERNET_IP_PKT_TYPE:
				return IPv4Packet.getIpProtocolType(data) == IPPacketType.TCP;
			case EthernetFrame.ETHERNET_IPv6_PKT_TYPE:
				return IPv6Packet.getIpProtocolType(data) == IPPacketType.TCP;
			default:
				return false;
			
			}
		}
		return false;
	}
	
	public static boolean isUDPPacket(byte data[])
	{
		if(isIPPacket(data))
		{
			switch(EthernetFrame.statGetPacketType(data))
			{
			case EthernetFrame.ETHERNET_IP_PKT_TYPE:
				return IPv4Packet.getIpProtocolType(data) == IPPacketType.UDP;
			case EthernetFrame.ETHERNET_IPv6_PKT_TYPE:
				return IPv6Packet.getIpProtocolType(data) == IPPacketType.UDP;
			default:
				return false;
			
			}
		}
		return false;
	}
	
	public static boolean isSCTPPacket(byte data[])
    {
        if(isIPPacket(data))
        {
            switch(EthernetFrame.statGetPacketType(data))
            {
            case EthernetFrame.ETHERNET_IP_PKT_TYPE:
                return IPv4Packet.getIpProtocolType(data) == IPPacketType.SCTP;
            case EthernetFrame.ETHERNET_IPv6_PKT_TYPE:
                return IPv6Packet.getIpProtocolType(data) == IPPacketType.SCTP;
            default:
                return false;
            
            }
        }
        return false;
    }
	
	public static TCPPacket createTCPPacket(byte data[])
	{
		if(!isIPPacket(data))
			return null;
		
		switch(EthernetFrame.statGetPacketType(data))
		{
		case EthernetFrame.ETHERNET_IP_PKT_TYPE:
			return new TCPPacketIpv4(data);
		case EthernetFrame.ETHERNET_IPv6_PKT_TYPE:
			return new TCPPacketIpv6(data);
		default:
			return null;
		
		}
	}
	
	public static UDPPacket createUDPPacket(byte thePacket[])
	{
		if(!isIPPacket(thePacket))
			return null;
		
		switch(EthernetFrame.statGetPacketType(thePacket))
		{
		case EthernetFrame.ETHERNET_IP_PKT_TYPE:
			return new UDPPacketIpv4(thePacket);
		case EthernetFrame.ETHERNET_IPv6_PKT_TYPE:
			return new UDPPacketIv6(thePacket);
		default:
			return null;
		
		}
	}
	
	public static SCTPPacket createSCTPPacket(byte data[])
    {
        if(!isIPPacket(data))
            return null;
        
        switch(EthernetFrame.statGetPacketType(data))
        {
        case EthernetFrame.ETHERNET_IP_PKT_TYPE:
            return new SCTPPacketIpv4(data);
        case EthernetFrame.ETHERNET_IPv6_PKT_TYPE:
            return new SCTPPacketIpv6(data);
        default:
            return null;
        
        }
    }
}
