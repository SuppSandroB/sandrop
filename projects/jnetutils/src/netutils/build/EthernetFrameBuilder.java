package netutils.build;

import netutils.parse.*;


/**
 * class for building Ethernet frame.
 * This is the lowest non physical layer in the network (L2).
 *
 * Example for usage:
 * 
 *  EthernetFrameBuilder bd = new EthernetFrameBuilder().
 *  
 *  frame.setDstMac(new MACAddress("00:00:dd:aa:00:01"));
 *	frame.setSrcMac(new MACAddress("00:00:dd:aa:00:02"));
 * 	
 *	// here we add the data of the frame (the next layer)
 *	frame.addL3Buider(ipv6bld)
 * 
 * @author roni bar-yanai
 *
 */
public class EthernetFrameBuilder
{
	private MACAddress mySrcMac = null;
	private MACAddress myDstMac = null;
	
	// the internal protocol (in other words the frame payload type)
	public  int myFrameType = 0;

	// the payload/L3 builder
	public L3Builder myL3 = null;
	
	/*
	 * Ethernet header structure: 
	 * 
	 * 0-6    : src mac
	 * 7 - 12 : dst mac
	 * 13 -14 : protocol type
	 * 
	 *  0                14
	 *  ------------------------------------------
	 * |  Ethernet hdr    |   pay load       
	 *  ------------------------------------------
	 *  
	 *  Some types if packets have different size, for example vlan frames.
	 *  Currently not supported. 
	 *  Some implementation notes. The class use lazy initialization of the
	 *  parameters, namely some of the parameters would be initialized only when
	 *  their value is required.
	 */
	
	public EthernetFrameBuilder()
	{}

	/**
	 * 
	 * @return the destination mac address.
	 *  (return null if was not initialized)
	 */
	public MACAddress getDstMac()
	{
		return myDstMac;
	}

	/**
	 * set destination address
	 * @param theDstMac
	 */
	public void setDstMac(MACAddress theDstMac)
	{
		myDstMac = theDstMac;
	}

	/**
	 * @return source mac address
	 *  (return null if was not initialized)
	 */
	public MACAddress getSrcMac()
	{
		return mySrcMac;
	}

	/**
	 * set source address.
	 * @param theSrcMac
	 */
	public void setSrcMac(MACAddress theSrcMac)
	{
		mySrcMac = theSrcMac;
	}
	
	/**
	 * add the layer 3 (payload of the frame)
	 * (maybe IPv6builder,IPv4Builder...etc)
	 * @param theL3
	 */
	public void addL3Buider(L3Builder theL3)
	{
		myFrameType = L3Type.L3toHexVal(theL3.getType());
		myL3 = theL3;
		theL3.setL2(this);
	}

	/**
	 * copy data from IPPacketBase
	 * @param theIp
	 */
	protected void addInfo(IPPacketBase theIp)
	{
		theIp.setSrcMacAddress(mySrcMac.asByteArray());
		theIp.setDstMacAddress(myDstMac.asByteArray());
		
		if(theIp instanceof IPv4Packet)
		{
			((IPv4Packet)theIp).setPacketType(myFrameType);
		} 
		else 
		{
			((IPv6Packet)theIp).setPacketType(myFrameType);
		}
	}

	/**
	 * 
	 * @param theBf
	 * @return true if all manadtory parameters were defined.
	 */
	protected boolean sanityCheck(StringBuffer theBf)
	{
		if(myDstMac == null)
		{
			theBf.append("No destination mac address");
			return false;
		}
		
		if(mySrcMac == null)
		{
			theBf.append("no source ip address");
			return false;
		}
		
		return true;
	}
	
}
