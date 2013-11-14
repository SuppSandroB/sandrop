package netutils.parse;

import netutils.NetUtilsException;
import netutils.utils.ByteUtils;

/**
 * The class implements Ethernet frame.<br>
 * building and manipulating frames, for example extracting 
 * the header src,dst address and the packet type ip/arp.. etc.
 * <br>
 * <br>
 * Supports only basic frames with 14 bytes header length.<br>
 * (Vlan not supported currently).<br>
 * 
 * @author roni bar-yanai
 *
 */
public class EthernetFrame
{
	/*
	 * Ethernet header structure: 
	 * 
	 * 0-6    : dst mac
	 * 7 - 12 : src mac
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
	// eth header constants.
	public static final int ETHERNET_HEADER_LENGTH = 14;
	private static final int ETHERNET_SRC_MAC_OFFSET = 6;
	private static final int ETHERNET_DST_MAC_OFFSET = 0;
	private static final int ETHERNET_MAC_LENGTH = 6;
	private static final int ETHERNET_TYPE_OFFSET = 12;
	private static final int VLAN_ETHERNET_TYPE_OFFSET = 14;
	public static final int ETHERNET_IP_PKT_TYPE = 0x800;
	public static final int ETHERNET_VLAN_PKT_TYPE = 0x800;
	public static final int ETHERNET_IPv6_PKT_TYPE = 0x86dd;
	public static final int ETHERNET_OVER_VLAN= 0x8100;
	
	/*
	 * holds the packet raw array
	 */
	protected byte[] myPacket = null;
	
	/*
	 * instance of bytes utils for fast running
	 */
	private ByteUtils myBytesUtils = null;
	
	/*
	 * holds the source mac as string
	 */
	private String mySrcMac = null;
	
	/*
	 * holds the destination mac as string
	 */
	private String myDstMac = null;
	
	/*
	 * will hold the mac as byte array.
	 */
	private byte[] mySrcMacAsByteArr = new byte[]{0,0,(byte) 0xde,(byte) 0xad,0,0};
	
	/*
	 * will hold the dst mac as byte array
	 */
	private byte[] myDstMacAsByteArr = new byte[]{0,0,(byte) 0xed,(byte) 0xee,0,0};
	
	private int    myType = ETHERNET_IP_PKT_TYPE;
	
	private int    myOffset = ETHERNET_HEADER_LENGTH;
	
	protected boolean _isSniffedPkt = false;
	
	protected EthernetFrame()
	{
		_isReadSrcMac = true;
		_isReadDstMac = true;
	}
	
	/**
	 * create new Ethernet packet from the byte array.
	 *  
	 * @param thePacket - byte array which contains a valid Ethernet frame.
	 */
	public EthernetFrame(byte[] thePacket)
	{
		myPacket = thePacket;
		_isSniffedPkt = true;
	}
	
	boolean _isReadSrcMac = false;
	
	/**
	 * @return the eth src mac address as string.
	 */
	public String getSrcMAC()
	{
		if (_isReadSrcMac == false && _isSniffedPkt)
		{
			mySrcMac = getMyBytesUtils().getAsMac(myPacket,ETHERNET_SRC_MAC_OFFSET,ETHERNET_MAC_LENGTH);
			mySrcMacAsByteArr = ByteUtils.extractBytesArray(myPacket,ETHERNET_SRC_MAC_OFFSET,ETHERNET_SRC_MAC_OFFSET+ETHERNET_MAC_LENGTH);
		}
		return  mySrcMac;
	}
	
	/**
	 * @return the source mac address
	 */
	public byte[] getSrcMacByteArray()
	{
		if (_isReadSrcMac == false)
		{
			getSrcMAC();
		}
		return mySrcMacAsByteArr;
	}
	
	/**
	 * Set the src mac address.
	 * @param mac
	 */
	public void setSrcMacAddress(byte[] mac)
	{
		mySrcMacAsByteArr = mac;
		mySrcMac = getMyBytesUtils().getAsMac(mac,0,mac.length);
	}
	
	boolean _isReadDstMac = false;
	
	/**
	 * @return the destination mac address as string
	 */
	public String getDstMAC()
	{
		if ( _isReadDstMac == false )
		{
			_isReadDstMac = true;
			myDstMac = getMyBytesUtils().getAsMac(myPacket,ETHERNET_DST_MAC_OFFSET,ETHERNET_DST_MAC_OFFSET+ETHERNET_MAC_LENGTH);
			myDstMacAsByteArr = ByteUtils.extractBytesArray(myPacket,ETHERNET_DST_MAC_OFFSET,ETHERNET_DST_MAC_OFFSET+ETHERNET_MAC_LENGTH);
		}
		return myDstMac;
	}
	
	/**
	 * @return the destination mac address.
	 * may return null
	 */
	public byte[] getDstMacByteArray()
	{
		if (_isReadDstMac == false)
		{
			getDstMAC();
		}
		return myDstMacAsByteArr;
	}
	
	/**
	 * set the destination mac address.
	 * @param mac
	 */
	public void setDstMacAddress(byte[] mac)
	{
		myDstMacAsByteArr = mac;
		myDstMac = getMyBytesUtils().getAsMac(mac,0,mac.length);
	}
	
	private boolean _isReadType = false;
	
	/**
	 * @return the packet type field value
	 */
	public int getPacketType()
	{
		if(!_isReadType && _isSniffedPkt )
		{
			myType = ByteUtils.getByteNetOrderTo_uint16(myPacket,ETHERNET_TYPE_OFFSET);
			// vlan
			if(myType == 0x8100)
			{
				myType = ByteUtils.getByteNetOrderTo_uint16(myPacket,ETHERNET_TYPE_OFFSET+4);
				myOffset+=4;
			}
			_isReadType = true;
		}
		return myType;
	}
	
	/**
	 * Set the Ethernet packet type (ARP,IPv4,IPv6..etc).
	 * No validation is done on the value!.
	 * @param type
	 */
	public void setPacketType(int type)
	{
		myType = type;
	}
	
	/**
	 * @return true if packet type is ip and false otherwise
	 */
	public boolean isIpPacket()
	{
		return  (getPacketType() == EthernetFrameType.IP_CODE);
	}
	
	/**
	 * @return true if packet type is arp and false otherwise
	 */
	public boolean isArpPacket()
	{
		return  (getPacketType() == EthernetFrameType.ARP_CODE);
	}
	

	/**
	 * @return the header offset
	 */
	protected int getHeaderOffset()
	{
		if (!_isReadType && _isSniffedPkt)
		{
			// init the offset
			getPacketType();
		}
		return myOffset;
	}
	
	/**
	 * should be called only when packet sniffed.
	 * @return the packet raw data as bytes array.
	 * @throws NetUtilsException
	 */
	public byte[] getRawBytes() throws NetUtilsException
	{
		if(myPacket != null)
		{
			return myPacket;
		}
		throw new NetUtilsException("No packet was sniffed");
	}
	
	/**
	 * @return
	 */
    private ByteUtils getMyBytesUtils()
    {
    	if (myBytesUtils == null) {
			myBytesUtils = new ByteUtils();
		}
    	return myBytesUtils;
    }
    
    /**
     * @param thePacket
     * @return the packet src mac as String
     */
    public static String statGetSrcMAC(byte[] thePacket)
	{
		return  getMyBytesStatUtils().getAsMac(thePacket,ETHERNET_SRC_MAC_OFFSET,ETHERNET_MAC_LENGTH);
	}
	
    /**
     * @param thePkt
     * @return the eth packet dst mac as string
     */
	public static String statGetDstMAC(byte []thePkt)
	{
		return  getMyBytesStatUtils().getAsMac(thePkt,ETHERNET_DST_MAC_OFFSET,ETHERNET_DST_MAC_OFFSET+ETHERNET_MAC_LENGTH);
	}
	
	/**
	 * @param thePacket
	 * @return the packet type as int.
	 */
	public static int statGetPacketType(byte[] thePacket)
	{	
		// if vlan then get correct offset
		if (statIsVlan(thePacket))
		{
			return ByteUtils.getByteNetOrderTo_uint16(thePacket,ETHERNET_TYPE_OFFSET+4);
		}
		return ByteUtils.getByteNetOrderTo_uint16(thePacket,ETHERNET_TYPE_OFFSET);
	}
	
	/**
	 * 
	 * @param thePacket
	 * @return true if Ethernet header includes vlan.
	 */
	public static boolean statIsVlan(byte[] thePacket)
	{
        int type =  ByteUtils.getByteNetOrderTo_uint16(thePacket,ETHERNET_TYPE_OFFSET);
		
		// if vlan then get correct offset
		return (( type & 0xff00) == ETHERNET_OVER_VLAN);
	}
	
	/**
	 * 
	 * @param thePacket
	 * @return the Ethernet header length
	 */
	public static int statGetEthHdrLen(byte[] thePacket)
	{
        int type =  ByteUtils.getByteNetOrderTo_uint16(thePacket,ETHERNET_TYPE_OFFSET);
		
		// if vlan then get correct offset
		if (( type & 0xff00) == ETHERNET_OVER_VLAN)
		{
			return ETHERNET_HEADER_LENGTH+4;
		}
		return ETHERNET_HEADER_LENGTH;
	}
	
	/**
	 *
	 * @param thePacket
	 * @return true if the eth packet is ip packet
	 */
	public static boolean statIsIpv4Packet(byte [] thePacket)
	{
		return  (statGetPacketType(thePacket) == ETHERNET_IP_PKT_TYPE);
	}
	
	/**
	 *
	 * @param thePacket
	 * @return true if the eth packet is ip packet
	 */
	public static boolean statIsIpv6Packet(byte [] thePacket)
	{
		return  (statGetPacketType(thePacket) == ETHERNET_IPv6_PKT_TYPE);
	}
	
	/**
	 * 
	 * @param thePacket
	 * @return true if the eth packet is an arp
	 */
	public static boolean statIsArpPacket(byte [] thePacket)
	{
		return (statGetPacketType(thePacket) ==  EthernetFrameType.ARP_CODE);
	}
	
	/**
	 * strip vlan from ethenet header.
	 * @param thePacket
	 * @return stripped packet, will return same packet if packet has no vlan.
	 */
	public static byte[] statStripVlan(byte [] thePacket)
	{
		if(!statIsVlan(thePacket))
			return thePacket;
			
		byte data2[] = new byte[thePacket.length - 4];
		System.arraycopy(thePacket, 0, data2, 0, 12);
		System.arraycopy(thePacket, 16, data2, 12, data2.length - 12);
		
		return data2;
	}
	
	/**
	 * return vlan part from Ethenet header.
	 * @param thePacket
	 * @return return the vlan 4 bytes or null if no vlan
	 */
	public static byte[] statGetVlan(byte [] thePacket)
	{
		if(!statIsVlan(thePacket))
			return null;
			
		byte data2[] = new byte[4];
		System.arraycopy(thePacket, 12, data2, 0, 4);
		return data2;
	}
	
	/**
	 * add vlan part from Ethenet header.
	 * @param thePacket
	 * @param vlan - the vlan 4 bytes or null. in case of null returns the same packet.
	 * @return return the vlan 4 bytes or null if no vlan
	 */
	public static byte[] statAddVlan(byte [] thePacket,byte [] vlan)
	{
		if(vlan == null)
			return thePacket;
		
		byte data2[] = new byte[thePacket.length + 4];
		System.arraycopy(thePacket, 0, data2, 0, 12);
		System.arraycopy(vlan, 0, data2, 12, 4);
		System.arraycopy(thePacket, 12, data2, 16, thePacket.length - 12);
		return data2;
	}
	
	
	private static ByteUtils MyBytesStatUtils = null;
	
	/**
	 * save the new on each call.
	 * @return 
	 */
    private static ByteUtils getMyBytesStatUtils()
    {
    	if (MyBytesStatUtils == null) {
    		MyBytesStatUtils = new ByteUtils();
		}
    	return MyBytesStatUtils;
    }

    /**
     * allocate array and put the mac address.
     * @param size
     */
	protected void createIPPacketBytes(int size)
	{
		myPacket = new byte[size+ETHERNET_HEADER_LENGTH];
		System.arraycopy(getSrcMacByteArray(), 0, myPacket, ETHERNET_SRC_MAC_OFFSET, ETHERNET_MAC_LENGTH);
		System.arraycopy(getDstMacByteArray(), 0, myPacket, ETHERNET_DST_MAC_OFFSET, ETHERNET_MAC_LENGTH);
		ByteUtils.setBigIndianInBytesArray(myPacket,ETHERNET_TYPE_OFFSET, myType, 2);
	}
}
