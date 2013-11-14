package netutils.build;

import netutils.parse.*;
import netutils.utils.RangeValidator;

/**
 * Builder for IPv46 layer.
 * 
 * Not all IPv6 header are configurable, for example, length, and 
 *  fields that could be calculated can not be set.
 *  
 * The class define only the L3 part and is used with L2,Ethernet frame , and L4
 *  such as TCP or UDP.
 *  
 * @author roni bar-yanai
 *
 */
public class IPv6PacketBuilder extends L3Builder
{
	/*
	 *  1                 8            16            24            31
	 *  -----------------------------------------------------------
	 * | 4 bits |traffic class |  Flow Label                       |    
	 * | version|  1 Byte      |  20 bits                          |
	 *  ------------------------------------------------------------
	 * | Payload Length                | Next Header | Hop Limit   |
	 * |      2 Bytes                  |  1 Byte     |  1 Byte     |
	 * -------------------------------------------------------------

	 * |   128 bit src addr                                        |
	 * |                                                           |
	 *     / /                       /   /                   /   /
	 * |                                                           |
	 * |                                                           |
	 * -------------------------------------------------------------
	 * |   128 bit dst addr                                        |
	 * |                                                           |
	 *     / /                       /   /                   /   /
	 * |                                                           |
	 * |                                                           |
	 * -------------------------------------------------------------
	 * 
	 *  Ip header may continue if one of the IPv6 extension is included
	 *  
	 */
	
	private static final int IPV6_B_DEFAULT_HOP_LIMIT = 64;
	
	// configurable fields
	private int myTrafficClass = 0;
	private int myFlowLabel = 0;
	
	private int myNextHdr = 0;
	private int myHopLimit = IPV6_B_DEFAULT_HOP_LIMIT;
	
	private IPv6Address mySrcIp = null;
	private IPv6Address myDstIP = null;

	// L4 type, eg. TCP/UDP ...etc
	private int myL4Type = 0;

	// underline L4 builder
	private L4Builder myL4 = null;
	
	// L2 part
	private EthernetFrameBuilder myL2 = null;
	
	/**
	 * 
	 * @return traffic class value
	 */
	public int getTrafficClass()
	{
		return myTrafficClass;
	}
	
	/**
	 * set traffic class (uint8).
	 * @param theTrafficClass
	 */
	public void setTrafficClass(int theTrafficClass)
	{
		RangeValidator.checkRangeUint8(theTrafficClass);
		myTrafficClass = theTrafficClass;
	}
	
	/**
	 * 
	 * @return flow label
	 */
	public int getFlowLabel()
	{
		return myFlowLabel;
	}
	
	/**
	 * set flow label (20 bits)
	 * @param theFlowLabel
	 */
	public void setFlowLabel(int theFlowLabel)
	{
		myFlowLabel = theFlowLabel;
	}
	
	/**
	 * 
	 * @return hop limit (same as TTL in IPv4)
	 */
	public int getHopLimit()
	{
		return myHopLimit;
	}
	
	/**
	 * set Hop Limit (uint8)
	 * @param theHopLimit
	 */
	public void setHopLimit(int theHopLimit)
	{
		RangeValidator.checkRangeUint8(theHopLimit);
		myHopLimit = theHopLimit;
	}
	
	/**
	 * 
	 * @return source IPv6 address
	 *  (may return null is field was not initialized)
	 */
	public IPv6Address getSrcIp()
	{
		return mySrcIp;
	}
	
	/**
	 * set source IPv6 address
	 * @param theSrcIp
	 */
	public void setSrcIp(IPv6Address theSrcIp)
	{
		mySrcIp = theSrcIp;
	}
	
	/**
	 * 
	 * @return destination IPv6 address.
	 *  (may return null is field was not initialized)
	 */
	public IPv6Address getDstIP()
	{
		return myDstIP;
	}
	
	/**
	 * set destination IPv6 address.
	 * @param theDstIP
	 */
	public void setDstIP(IPv6Address theDstIP)
	{
		myDstIP = theDstIP;
	}
	@Override
	public L3Type getType()
	{
		return L3Type.IPv6;
	}

	/**
	 * add L4 layer.
	 * @param theL4
	 */
	public void addL4Buider(L4Builder theL4)
	{
		myL4Type = L4Type.L4toHexVal(theL4.getType());
		myL4 = theL4;
		theL4.setL3(this);
	}
	
	@Override
	protected void setL2(EthernetFrameBuilder theFrameBuilder)
	{
		myL2 = theFrameBuilder;
	}
	
	/**
	 * copy data into packet.
	 */
	@Override
	protected void addInfo(IPPacket theToRet)
	{
        IPv6Packet ipv6 = (IPv6Packet) theToRet.getUnderlyingIPPacketBase();
		
        ipv6.setDstIp(myDstIP);
        ipv6.setSrcIp(mySrcIp);
        ipv6.setHopLimit(myHopLimit);
        ipv6.setIPProtocol(myL4Type);
        ipv6.setFlowLabel(myFlowLabel);
        ipv6.setTrafficClass(myTrafficClass);
        		
		myL2.addInfo(ipv6);
		
	}
	
	/**
	 * @return true if all mandatory fields were defined.
	 */
	@Override
	protected boolean sanityCheck(StringBuffer theBf)
	{
		if (mySrcIp == null)
		{
			theBf.append("No source IP");
			return false;
		}
		
		if(myDstIP == null)
		{
			theBf.append("No destination IP");
			return false;
		}
		
		if(myL2 == null)
		{
			theBf.append("No layer 2");
			return false;
		}
		
		return true;
	}

}
