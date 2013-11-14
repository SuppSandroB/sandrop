package netutils.build;

import netutils.parse.*;
import netutils.utils.RangeValidator;

/**
 * Builder for IPv4 layer.
 * 
 * Not all IPv4 header are configurable, length, checksum and 
 *  fields that can be calculated can not be set.
 *  
 * The class define only the L3 part and is used with L2,Ethernet frame , and L4
 *  such as TCP or UDP.
 * 
 * @author roni bar-yanai
 *
 */
public class IPv4PacketBuilder extends L3Builder
{
	/*
	 *  1                 8            16            24            31
	 *  -----------------------------------------------------------
	 * | 4 bits |4 bits | 8 bits tos   | 16 bits total length      |    
	 * | version|hdr ln |              | of packet (in bytes)      |
	 *  ------------------------------------------------------------
	 * | 16 bits id                    | 3 bits | 13-bit frag      |
	 * |                               |  flags | offset           |
	 * -------------------------------------------------------------
	 * | 8 bit ttl      | 8 bit        | 16 bit hdr check sum      | 
	 * |                | protocol     |                           |
	 * -------------------------------------------------------------
	 * |   32 bit src addr                                         |
	 * |                                                           |
	 * -------------------------------------------------------------
	 * |   32 bits dst addr                                        |
	 * |                                                           |
	 * -------------------------------------------------------------
	 * |          options if any                                   |
	 * |   (changes size                                           |
	 * -------------------------------------------------------------
	 * |         data (changed size)                               |
	 * |                                                           |
	 * -------------------------------------------------------------
	 * 
	 */
	private static final int IPV4_B_DEFAULT_TOS = 0;
	private static final int IPV4_B_DEFAULT_TTL = 64;

	// Configurable Fields.
	private int myTos = IPV4_B_DEFAULT_TOS;
	private int myId = 0;
	private int myFragFlags = 0;
	private int myFragOffset = 0;
	private int myTTL = IPV4_B_DEFAULT_TTL;
	
	// payload type (TCP/UDP/ICMP...etc)
	private int myL4Type = 0;

	private IPv4Address mySrcAddr = null;
	private IPv4Address myDstAddr = null;
	
	// IP header options
	private byte[] myOpt = new byte[0];
	
	// L4 part
	private L4Builder myL4 = null; 
	
	// L2 part
	private EthernetFrameBuilder myL2 = null;
	
	public IPv4PacketBuilder()
	{}
	
	public IPv4PacketBuilder(IPv4Packet pkt)
	{
        EthernetFrameBuilder eth = new EthernetFrameBuilder();
		
		eth.setDstMac(new MACAddress(pkt.getDstMacByteArray()));
		eth.setSrcMac(new MACAddress(pkt.getSrcMacByteArray()));
		
		myTos = pkt.getTypeOfService();
		myId =  pkt.getId();
		myFragFlags = pkt.getFragmentFlags();
		myFragOffset = pkt.getFragmentOffset();
		myTTL = pkt.getTTL();
		
		// payload type (TCP/UDP/ICMP...etc)
		myL4Type = pkt.getIPProtocol();

		mySrcAddr = pkt.getSourceIP();
		myDstAddr = pkt.getDestinationIP();
		
		// IP header options
		eth.addL3Buider(this);
	}
	
	
	@Override
	public L3Type getType()
	{
		return L3Type.IPv4;
	}

    /**
     *
     * @return TOS value
     */
	public int getTos()
	{
		return myTos;
	}

	/**
	 * set TOS value (uint8)
	 * @param theTos
	 */
	public void setTos(int theTos)
	{
		RangeValidator.checkRangeUint8(theTos);
		myTos = theTos;
	}

	/**
	 * 
	 * @return packet id
	 */
	public int getId()
	{
		return myId;
	}

    /**
     * set packet id (uint16).
     * @param theId
     */
	public void setId(int theId)
	{
		RangeValidator.checkRangeUint16(theId);
		myId = theId;
	}


    /**
     * 
     * @return fragment offset
     */
	public int getFragOffset()
	{
		return myFragOffset;
	}

    /**
     * set fragment offset 13 bits.
     * @param theFregOffset
     */
	public void setFragOffset(int theFregOffset)
	{
		myFragOffset = theFregOffset;
	}


	/**
	 * 
	 * @return TTL (time to live)
	 */
	public int getTTL()
	{
		return myTTL;
	}

	/**
	 * set TTL uint8
	 * @param theTTL
	 */
	public void setTTL(int theTTL)
	{
		RangeValidator.checkRangeUint8(theTTL);
		myTTL = theTTL;
	}

    /**
     * 
     * @return source IPv4 address
     *  (may return null if was not initialized by user)
     */
	public IPv4Address getSrcAddr()
	{
		return mySrcAddr;
	}

	/**
	 * Set source IPv4 address
	 * @param theSrcAddr
	 */
	public void setSrcAddr(IPv4Address theSrcAddr)
	{
		mySrcAddr = theSrcAddr;
	}


	/**
	 * 
	 * @return destination IPv4 address
	 *  (may return null if was not initialized by user)
	 */
	public IPv4Address getDstAddr()
	{
		return myDstAddr;
	}

    /**
     * set destination IPv4 address.
     * @param theDstAddr
     */
	public void setDstAddr(IPv4Address theDstAddr)
	{
		myDstAddr = theDstAddr;
	}

    /**
     * 
     * @return IPv4 header options
     */
	public byte[] getOpt()
	{
		return myOpt;
	}

    /**
     * Set IPv4 header options
     * @param theOpt
     */
	public void setOpt(byte[] theOpt)
	{
		if(theOpt == null)
			throw new IllegalArgumentException("Got null");
		myOpt = theOpt;
	}

    /**
     * 
     * @return fragments flags
     */
	public int getFragFlags()
	{
		return myFragFlags;
	}

	/**
	 * set fragments flag (3 bits)
	 * @param theMyFregFlags
	 */
	public void setFragFlags(int theMyFregFlags)
	{
		myFragFlags = theMyFregFlags;
	}
	
	/**
	 * add layer 4.
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
	 * copy information into packet.
	 */
	@Override
	protected void addInfo(IPPacket theToRet)
	{
		IPv4Packet ipv4 = (IPv4Packet) theToRet.getUnderlyingIPPacketBase();
		
		ipv4.setID(myId);
		ipv4.setDstIp(myDstAddr.getIPasLong());
		ipv4.setSrcIp(mySrcAddr.getIPasLong());
		ipv4.setFragFlags(myFragFlags);
		ipv4.setFragOffset(myFragOffset);
		ipv4.setIPProtocol(myL4Type);
		ipv4.setTos(myTos);
		ipv4.setTTL(myTTL);
		
		// In case no frame, we add it for the user.
		if(myL2 == null)
		{
			myL2 = new EthernetFrameBuilder();
			myL2.addL3Buider(this);
			myL2.setDstMac(new MACAddress("00:0d:dd:e1:12:4f"));
			myL2.setSrcMac(new MACAddress("11:0d:dd:e1:12:4f"));
		}
		myL2.addInfo(ipv4);
		
	}

    /**
     * return true if all mandatory fields were defined.
     */
	@Override
	protected boolean sanityCheck(StringBuffer theBf)
	{
		if (myDstAddr == null)
		{
			theBf.append("No source ip address");
			return false;
		}
		
		if(myDstAddr == null)
		{
			theBf.append("No destination address");
			return false;
		}
		
		if(myL2 == null)
		{
			myL2 = new EthernetFrameBuilder();
			myL2.addL3Buider(this);
			myL2.setDstMac(new MACAddress("00:0d:dd:e1:12:4f"));
			myL2.setSrcMac(new MACAddress("11:0d:dd:e1:12:4f"));
		}
		
		return myL2.sanityCheck(theBf);
	}
}
