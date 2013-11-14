package netutils.build;

import netutils.parse.IPPacketType;

/**
 * 
 * 
 * @author roni bar-yanai
 *
 */
public enum L4Type
{
	TCP,
	UDP,
	SCTP,
	ICMP;
	
	static int L4toHexVal(L4Type type)
	{
		switch(type)
		{
		case TCP:
			return IPPacketType.TCP;
		case UDP:
			return IPPacketType.UDP;
		case ICMP:
			return IPPacketType.ICMP;
		case SCTP:
			return IPPacketType.SCTP;
		default:
			throw new IllegalArgumentException("Unsupported value:"+type);
		}
	}

}
