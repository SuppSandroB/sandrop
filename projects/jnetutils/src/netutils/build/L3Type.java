package netutils.build;

import netutils.parse.EthernetFrameType;

/**
 * 
 * @author roni bar-yanai
 *
 */
public enum L3Type
{
	IPv4,
	IPv6;
	
	static int L3toHexVal(L3Type type)
	{
		switch(type)
		{
		case IPv4:
			return EthernetFrameType.IP_CODE;
		case IPv6:
			return EthernetFrameType.IPv6_CODE;
		default:
			throw new IllegalArgumentException("Unsupported value");
		}
	}
}
