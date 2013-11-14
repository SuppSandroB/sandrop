package netutils.build;

import netutils.NetUtilsException;
import netutils.parse.*;
import netutils.utils.RangeValidator;

/**
 *
 * UDP Layer builder.
 * 
 * Not all UDP header fields are configurable, for example, length, checksum, and 
 *  other fields that could be calculated can not be set.
 *  
 * The class define only the L4 part and is used with L2,Ethernet frame , and L3
 *  such as IPv4 and IPv6
 *  
 * 
 * @author roni bar-yanai
 *
 */
public class UDPPacketBuilder implements L4Builder
{
	
	/*
	 * 0                                    15 16                                     32
	 *  -------------------------------------------------------------------------------
	 *  |                                    |                                         |
	 *  |  16 bit source port                |    16 bit destination port              |
	 *  |                                    |                                         |
	 *  -------------------------------------------------------------------------------
	 *  |                                    |                                         |
	 *  |   16 bit udp length (include hdr)  |  16 bit check sum                       |
	 *  |                                    |                                         |
	 *   ------------------------------------------------------------------------------
	 *  |                                                                              |
	 *  |                            data if any                                       |
	 *  |                                                                              |
	 *  -------------------------------------------------------------------------------- 
	 * 
	 * 
	 * constants offset for the start of the udp packet of the fields. 
	 */
	
	// configurable fields
	private int mySrcPort = 0;
	private int myDstPort = 0;
	
	// UDP payload
	private byte myPayload[] = new byte[0];
	
	private L3Builder myL3 = null;
	
	public UDPPacketBuilder()
	{}

	/**
	 * create packet from existing packet.
	 * @param udppkt
	 */
	public UDPPacketBuilder(UDPPacketIpv4 udppkt)
	{
		mySrcPort = udppkt.getSourcePort();
		myDstPort = udppkt.getDestinationPort();
		
		myPayload = udppkt.getUDPData();
				
	    L3Builder l3 = null;
		
		if(udppkt.getUnderlyingIPPacketBase().isIPv4())
		{
			l3 = new IPv4PacketBuilder((IPv4Packet)udppkt.getUnderlyingIPPacketBase());
			l3.addL4Buider(this);
		} 
		else
		{
			throw new UnsupportedOperationException("Not supported yet");
		}
	}
	
	/**
	 * 
	 * @return source port
	 */
	public int getSrcPort()
	{
		return mySrcPort;
	}
	
	protected boolean _isSrcPortDirty = false;
	
	/**
	 * set source port (uint16)
	 * @param theSrcPort
	 */
	public void setSrcPort(int theSrcPort)
	{
		RangeValidator.checkRangeUint16(theSrcPort);
		mySrcPort = theSrcPort;
		_isSrcPortDirty = true;
	}
	
	/**
	 * 
	 * @return destination port
	 */
	public int getDstPort()
	{
		return myDstPort;
	}
	
	protected boolean _isDstPortDirty = false;
	
	/**
	 * set destination port
	 * @param theDstPort
	 */
	public void setDstPort(int theDstPort)
	{
		RangeValidator.checkRangeUint16(theDstPort);
		myDstPort = theDstPort;
		_isDstPortDirty = true;
	}

	@Override
	public L4Type getType()
	{
		return L4Type.UDP;
	}
	
	protected boolean _isPayloadDirty = false; 
	
	/**
	 * set UDP payload 
	 *  (no validation on length)
	 * @param data
	 */
	public void setPayload(byte data[])
	{
		myPayload = data;
		_isPayloadDirty = true;
	}
	
	@Override
	public void setL3(L3Builder theL3)
	{
		myL3 = theL3;
		
	}
	
	protected boolean sanityCheck(StringBuffer bf)
	{
		if(myL3 == null)
		{
			bf.append("No L3 builder");
			return false;
		}
		
		return myL3.sanityCheck(bf);
	}
	
	/**
	 * create udp packet
	 * @return UDPPacket
	 * @throws NetUtilsException 
	 */
	public UDPPacket createUDPPacket() throws NetUtilsException
	{
		StringBuffer bf = new StringBuffer();
		if (!sanityCheck(bf))
		{
			throw new NetUtilsException(bf.toString());
		}	
		
		UDPPacket toRet = null;

		// set matching underlying protocol
		switch (myL3.getType())
		{
		case IPv4:

			toRet = new UDPPacketIpv4();
			break;

		case IPv6:

			toRet = new UDPPacketIv6();
			break;
		default:
			throw new UnsupportedOperationException();
		}

		// copy data 
		toRet.setData(myPayload);
        toRet.setDstPort(myDstPort);
        toRet.setSrcPort(mySrcPort);
		
        // call upper layer to add information
		myL3.addInfo(toRet);

		toRet.createPacketBytes();

		return toRet;
	}

}
