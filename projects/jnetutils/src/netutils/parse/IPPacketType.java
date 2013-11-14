package netutils.parse;

/**
 * IP packet types constants.<br>
 * To be compared with the IP packet type field in the IP header.<br>
 * 
 * @author roni bar yanai
 *
 */
final public class IPPacketType
{
	final public static int UDP = 0x11;

	/**
	 * Internet Control Message Protocol. 
	 */
	final public static int ICMP = 1;

	/**
	 * Internet Group Management Protocol.
	 */
	final public static int IGMP = 2;

	/**
	 * Transmission Control Protocol. 
	 */
	final public static int TCP = 6;

	/**
     * stream control transmission protocol  
     */
    final public static int SCTP = 132; //hex 0x84

	/**
	 * Raw IP packets. 
	 */
	final public static int RAW = 255;
	
	public static String getTypeAsString(int type)
	{
		switch(type)
		{
		case UDP:
			return "UDP";
		case TCP:
			return "TCP";
		case ICMP:
			return "ICMP";
		case IGMP:
			return "IGMP";
		case SCTP:
            return "SCTP";
		default:
			return "Other";
		}
	}
}
