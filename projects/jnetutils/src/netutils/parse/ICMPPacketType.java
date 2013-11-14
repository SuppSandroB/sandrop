package netutils.parse;

import java.util.HashMap;

/**
 * ICMP type constants list.<br>
 * 
 * 
 * @author roni bar-yanai
 *
 */
public class ICMPPacketType
{
	  /**
	   * Echo reply.
	   */
	  public static final int ECHO_REPLY_TYPE = 0x0000;
	  
	  /**
	   * Destination unreachable.
	   */
	  public static final int DESTINATION_UNREACHBLE_TYPE = 0x0003;

	  /**
	   * Destination network unreachable.
	   */
	  public static final int UNREACH_NET_CODE = 0x0000;

	  /**
	   * Destination host unreachable.
	   */
	  public static final int UNREACH_HOST_CODE = 0x0001;

	  /**
	   * Unreachable protocol 
	   */
	  public static final int UNREACH_PROTOCOL_CODE = 0x0002;

	  /**
	   * Port unreachable
	   */
	  public static final int UNREACH_PORT_CODE = 0x0003;

	  /**
	   * Framgmention needed but don't-fragment bits set.
	   */
	  public static final int NEEDFRAG_CODE = 0x0004;

	  /**
	   * Src route failed.
	   */
	  public static final int SRC_ROUTE_FAIL_CODE = 0x0005;

	  /**
	   * Got Unknown desitiantion network.
	   */
	  public static final int UNREACH_NET_UNKNOWN_CODE = 0x0006;

	  /**
	   * Unknown host.
	   */
	  public static final int UNREACH_HOST_UNKNOWN_CODE = 0x0007;

	  /**
	   * Src host isolated.
	   */
	  public static final int UNREACH_ISOLATED_CODE = 0x0008;

	  /**
	   * Network access admin prohibited.
	   */
	  public static final int UNREACH_NET_PROHIB_CODE = 0x0009;

	  /**
	   * Host access admin prohibited.
	   */
	  public static final int UNREACH_HOST_PROHIB_CODE = 0x000a;

	  /**
	   * newtwok unreachable for tos
	   */
	  public static final int UNREACH_TOS_NET_CODE = 0x000b;

	  /**
	   * host unreachable for tos
	   */
	  public static final int UNREACH_TOSHOST_CODE = 0x000c;

	  /**
	   * communication admin prohibited by filtering
	   */
	  public static final int COM_ADMIN_PHROBITEN = 0x00d;
	  
	  /**
	   * Packet lost, slow down.
	   */
	  public static final int SOURCE_QUENCH_TYPE = 0x04;

	  /**
	   * redirect.
	   */
	  public static final int REDIRECT_NET_TYPE = 0x05;

	  /**
	   * Redirect for netwok.
	   */
	  public static final int REDIRCET_NETWROK_CODE = 0x00;
	  
	  /**
	   * resirect for host.
	   */
	  public static final int REDIRECT_HOST_CODE = 0x01;

	  /**
	   * redirect for tos and network
	   */
	  public static final int REDIRECT_TOSNET_CODE = 0x02;

	  /**
	   * redirect for tos and host
	   */
	  public static final int REDIRECT_TOSHOST_CODE = 0x03;

	  /**
	   * Echo request.
	   */
	  public static final int ECHO_REQUEST_TYPE = 0x08;

	  /**
	   * router advertisement
	   */
	  public static final int ROUTER_ADVERT_CODE = 0x09;

	  /**
	   * router solicitation
	   */
	  public static final int ROUTER_SOLICIT_CODE = 0x0a;

	  /**
	   * time exceeded type
	   */
	  public static final int TIME_EXCEEDED_TYPE = 0x0b;
	  
	  /**
	   * time exceeded in transit.
	   */  
	  public static final int TIME_EXCEED_INTRANS_CODE = 0x00;

	  /**
	   * time exceeded in reass.
	   */  
	  public static final int TIME_EXCEED_REASS_CODE = 0x01;

	  /**
	   * parameter problem
	   */
	  public static final int PARAMETER_PROBLEM_TYPE = 0x0c;
	  
	  /**
	   * ip header bad; option absent.
	   */
	  public static final int PARAM_PROB_TYPE = 0x01;

	  /**
	   * timestamp request 
	   */
	  public static final int TSTAMP_REQUEST_TYPE = 0x0d;

	  /**
	   * timestamp reply 
	   */
	  public static final int TSTAMP_REPLY = 0x0e;

	  /**
	   * information request 
	   */
	  public static final int INFORMATION_REQUEST_TYPE = 0x0f;

	  /**
	   * information reply 
	   */
	  public static final int INFORMATION_REPLY = 0x10;

	  /**
	   * address mask request 
	   */
	  public static final int ADDRESS_MASK_REQUEST = 0x11;

	  /**
	   * address mask reply 
	   */
	  public static final int ADDRESS_MASK_REPLY = 0x12;
	  
	  private static HashMap <Integer,String> statICMPMessageDescription = new HashMap<Integer,String>();
	  
	  static
	  {
		  statICMPMessageDescription.put(new Integer(ECHO_REPLY_TYPE),"Echo Replay");
		  statICMPMessageDescription.put(new Integer(DESTINATION_UNREACHBLE_TYPE),"Destination Unreachable");
		  statICMPMessageDescription.put(new Integer(ECHO_REQUEST_TYPE),"Echo Request");
		  statICMPMessageDescription.put(new Integer(SOURCE_QUENCH_TYPE),"Source Quench");
		  statICMPMessageDescription.put(new Integer(ROUTER_ADVERT_CODE),"Router Advertisement");
		  statICMPMessageDescription.put(new Integer(ROUTER_SOLICIT_CODE),"Router Soliciation");
		  statICMPMessageDescription.put(new Integer(TIME_EXCEEDED_TYPE),"Time Exceeded");
		  statICMPMessageDescription.put(new Integer(PARAMETER_PROBLEM_TYPE),"Parameter Time");
		  statICMPMessageDescription.put(new Integer(TSTAMP_REQUEST_TYPE),"Time Stamp Request");
		  statICMPMessageDescription.put(new Integer(ECHO_REPLY_TYPE),"Time Stamp Replay");
		  statICMPMessageDescription.put(new Integer(INFORMATION_REQUEST_TYPE),"Information Request");
		  statICMPMessageDescription.put(new Integer(INFORMATION_REPLY),"Information Replay");
		  statICMPMessageDescription.put(new Integer(ADDRESS_MASK_REQUEST),"Address Mask Request");
		  statICMPMessageDescription.put(new Integer(ADDRESS_MASK_REPLY),"Address Mask Replay");
	  }
	  
	  /**
	   * Return the icmp type (8 bits value) description.
	   * @param type
	   * @return the description string or unknown type if not exits.
	   */
	  public static String getTypeDescription(int type)
	  {
		  if (statICMPMessageDescription.containsKey(type))
		  {
			  return statICMPMessageDescription.get(type);
		  }
		  else
		  {
			  return "Unkown Type";
		  }
	  }
}
