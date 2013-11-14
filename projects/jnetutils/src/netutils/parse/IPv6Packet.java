package netutils.parse;

import java.util.LinkedList;

import netutils.parse.ipv6.IPv6Extension;
import netutils.utils.ByteUtils;



/**
 * The class implement ipv6 packet.<br>
 * <br>
 * The class is used both for sniffing and injecting.<br>
 * Used to build packets for injection or to parse packets
 * that were sniffed.<br>
 * The class supply extracting and setting methods for all
 * IP header fields and also for the data.<br>
 * <br>
 *<br>
 * @author roni bar-yanai
 */
public class IPv6Packet extends IPPacketBase
{
		
	private static final int DEFAULT_TRAFFIC_CLASS = 0;
	private static final int DEFAULT__CLASS_LABEL = 0;
	private static final int DEFAULT_HOP_LIMIT = 64;
	
	private static final int IPV6 = 6;

	private static final int DEFAULT_IPv6_HDR_LENGTH = 40;
	
	//public static final int CALCULATE_CHKSUM = 0;

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
	 * const for the fields offsets from the start of the ip packet
	 * (not including the Etherent packet header)
	 */

	private static final int IP_PAYLOAD_LENGTH_POS = 4; 
	
	private static final int IP_Proto_POS = 6; // protocol type field
	
	private static final int IP_HOP_LIMIT_POS = 7;
	
	private static final int IP_SRC_POS = 8; // src ip

	private static final int IP_DST_POS = 24; // dst ip

	private static final int DEFAULT_IP_HDR_LENGTH = 40;

	
	// header version field
	private int myVersion = IPV6;

	// IP header length
	protected int myIPTotalLength = 0;
	
	// the next header (from start).
	private int myNextHeadr = 0;
	
	private int myLastHdrType = 0;

	// IP Time to Live field
	private int myHopLimit;
	
	private int myTrafficClass = 0;
	
	private int myFlowLabel = 0;

	// IP source IP field
	private IPv6Address mySrcIp = null;

	// IP destination IP
	private IPv6Address myDstIp = null;
	
	// used when building packet to know which parameters were set by
	// the user and which will be needed to be completed.
	
	boolean _isWriteDstIp = false;
	boolean _isWriteSrcIp = false;
	boolean _isWriteHopLimit = false;
	boolean _isWriteVesrion = false;

	// is TCP or UDP
	boolean _isWriteUndelyingProto = false;
	
	boolean _isExtension = false;

	private LinkedList<IPv6Extension> myExt = new LinkedList<IPv6Extension>();
	
	/**
	 * build empty packet.
	 */
	public IPv6Packet()
	{
		// make sure we will not to try to read parameters from internal buffer
		// that doesn't exists, because packet wasn't sniffed. 
		_isReadDstIp = true;
		_isReadSrcIp = true;
		_isReadTTl = true;
		_isReadVesrion = true;
		myIPHdrLength = DEFAULT_IP_HDR_LENGTH;
	}
	
	/**
	 * create new IP packet
	 * @param thePacket - byte array of a valid IP packet.
	 */
	public IPv6Packet(byte[] thePacket)
	{
		super(thePacket);
		// only set pointers. fields will extracted only when 
		// their value is needed to save performance.
		myIPHdrOffset = getHeaderOffset();
		// TBD - read extensions.
		myIPHdrLength = DEFAULT_IPv6_HDR_LENGTH;
		myIPDataOffset = myIPHdrOffset + myIPHdrLength;
		_isSniffedPkt = true;
	}
	
	public void addExtension(IPv6Extension ext)
	{
		if(myPacket != null)
		{
			byte arr[] = new byte[myPacket.length+ext.getLength()];
			int  pos = getIPHeaderLength();
			System.arraycopy(myPacket, 0, arr, 0, pos);
			System.arraycopy(myPacket, pos+ext.getLength(), arr, pos+ext.getLength(), myPacket.length - pos);
			myPacket = arr;
		}
		
		if(myExt.size() == 0)
		{
			myNextHeadr = ext.getType();
			myExt.add(ext);
			ext.setNextType(myLastHdrType);
		} 
		else
		{
			myExt.getLast().setNextType(ext.getType());
			myExt.addLast(ext);
			ext.setNextType(myLastHdrType);
		}
		myPayloadLength+=getTotalExtensionsLength();
	}
	
	/**
	 * will try to auto complete unfilled parameters such as
	 * hop limiet,flow label... etc<br>
	 * <br>
	 * the following parameters must be set by the user:<br>
	 * 1. source ip<br>
	 * 2. destination ip<br>
	 * 3. protocol (tcp/udp..etc)<br>
	 * 4. payload
	 */
	public void atuoComplete()
	{
		if (_isWriteDstIp == false || _isWriteSrcIp == false || getIPData() == null || _isWriteUndelyingProto == false)
			throw new IllegalPacketException("Src ip,Dst ip or data missing");
				
		if ( _isWriteHopLimit == false)
			setHopLimit(DEFAULT_HOP_LIMIT);
	}
	
	/**
	 * used by sub (TCP/UDP...etc) classes where the data is used there.
	 *
	 */
	protected void atuoCompleteNoData()
	{
		
		if ( _isWriteHopLimit == false)
			setHopLimit(DEFAULT_HOP_LIMIT);
	}
	
	/**
	 * @return true if all mandatory fields were set by user.<br>
	 * <br>
	 * mandatory fields:<br>
	 * 1. src ip.<br>
	 * 2. dst ip.<br>
	 * 3. data.<br>
	 * 4. protocol<br>
	 */
	public boolean isMandatoryFieldsSet()
	{
		return !(_isWriteDstIp == false || _isWriteSrcIp == false || getIPData() == null || _isWriteUndelyingProto == false);
	}
	
	/**
	 * @return true if all mandatory fields were set by user.
	 * use for subclasses <br>
	 * mandatory fields:<br>
	 * 1. src ip.<br>
	 * 2. dst ip.<br>
	 * 3. protocol<br>
	 */
	protected boolean isMandatoryFieldsSetNoData()
	{
		return !(_isWriteDstIp == false || _isWriteSrcIp == false || _isWriteUndelyingProto == false);
	}

	boolean _isReadVesrion = false;

	/** 
	 * Get the IP version code.
	 */
	public int getVersion()
	{
		if (_isReadVesrion == false)
		{
			myVersion = (myPacket[myIPHdrOffset] & (0xf0)) >> 4;
			_isReadVesrion = true;
		}
		return myVersion;
	}

	/** 
	 * Get the IP header length in bytes. 
	 */
	public int getIPHeaderLength()
	{
		return myIPHdrLength+getTotalExtensionsLength();
	}
	

	boolean _readTotalLength = false;

	/** 
	 * @return the IP length in bytes.
	 * (IP header + Payload).
	 */
	public int getIpPktTotalLength()
	{
		if (!_readTotalLength)
		{
			if (_isSniffedPkt)
			{
				myIPTotalLength = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset + IP_PAYLOAD_LENGTH_POS)+DEFAULT_IP_HDR_LENGTH;
				_readTotalLength = true;
			}
			// if not sniffed packet then the total length will be set when adding data
			// or ip options.
			else
			{
				_readTotalLength = true;
				myIPHdrLength = DEFAULT_IP_HDR_LENGTH;
			}
		}
		return myIPTotalLength;
	}
	
	
	
	/**
	 * called by sub classes when changing payload.
	 * will set the total ip pakcet.
	 * @param theLengh
	 */
	protected void setTotaIPLength(int theLengh)
	{
		myIPTotalLength = theLengh;
	}
	

	boolean _isReadTTl = false;
	
	/**
	 * @return the time to live. 
	 */
	public int getHopLimit()
	{
		if (_isReadTTl == false)
		{
			_isReadTTl = true;
			myHopLimit = ByteUtils.getByteNetOrderTo_uint8(myPacket, myIPHdrOffset+IP_HOP_LIMIT_POS);			
		}
		return myHopLimit;
	}
	
	/**
	 * set the ttl - (8 bits num)
	 * @param hopeLimit
	 * ( on bigger number would cut the last 8 bits)
	 */
	public void setHopLimit(int hopeLimit)
	{
		_isWriteHopLimit = true;
		_isReadTTl = true;
		myHopLimit = hopeLimit;
	}
	

	boolean _isReadSrcIp = false;
	
	/** 
	 * @return the source IP address.
	 */
	public IPv6Address getSourceIP()
	{
		if (_isReadSrcIp == false)
		{
		     mySrcIp =  new  IPv6Address(ByteUtils.extractBytesArray(myPacket, myIPHdrOffset + IP_SRC_POS,16));
		     _isReadSrcIp = true;
		}
		return mySrcIp;
	}
	
	/**
	 * set the ip
	 * @param theIp - 32 bit int.
	 */
	public void setSrcIp(IPv6Address theIp)
	{
		_isWriteSrcIp = true;
		_isReadSrcIp = true;
		mySrcIp = theIp;
	}

	boolean _isReadDstIp = false;
	
	/** 
	 * @return the destination IP address.
	 */
	public IPv6Address getDestinationIP()
	{
		if (_isReadDstIp == false)
		{
			_isReadDstIp = true;
		     myDstIp = new IPv6Address(ByteUtils.extractBytesArray(myPacket, myIPHdrOffset+IP_DST_POS, 16));
		}
		return myDstIp;
	}
	
	/**
	 * set the destination IP
	 * @param theDstIp
	 */
	public void setDstIp(IPv6Address theDstIp)
	{
		_isWriteDstIp = true;
		_isReadDstIp = true;
		myDstIp = theDstIp;
	}

	private byte[] _ipHeaderBytes = null;

	/** 
	 * @return the IP header a byte array.
	 */
	protected byte[] getIPHeader()
	{
		if (_ipHeaderBytes == null)
		{
			_ipHeaderBytes = new byte[getIPHeaderLength()];
			System.arraycopy(myPacket, myIPHdrOffset, _ipHeaderBytes, 0, _ipHeaderBytes.length);
		}
		return _ipHeaderBytes;
	}

	private byte[] _ipDataBytes = null;

	

	/** 
	 * @return the IP data (payload) as a byte array.
	 */
	public byte[] getIPData()
	{
		if (_ipDataBytes == null && _isSniffedPkt)
		{
			int ln = getIpPktTotalLength() - getIPHeaderLength();
			_ipDataBytes = new byte[ln];
			System.arraycopy(myPacket, myIPDataOffset, _ipDataBytes, 0, _ipDataBytes.length);
		}
		return _ipDataBytes;
	}
	
	private int myPayloadLength = 0; 
	
	/**
	 * set the packet data.
	 * @param data
	 */
	public void setData(byte data[])
	{
		if (data == null)
			return;
		_ipDataBytes = data;
		myIPTotalLength = getIPHeaderLength()+data.length;
		myPayloadLength = data.length+getTotalExtensionsLength();
	}
	
	/**
	 * static methods for faster performance.<br>
	 * <br>
	 * we just want to know the type of the packet (udp or icmp...) there is no need to create 
	 * new instance of ip and then again create icmp,udp...etc.
	 * we use a static method just to check the field value.<br>
	 * <br>
	 * @pre assuming packet is a valid IP packet (not checking it here)<br>
	 * @see IPPacketType<br>
	 * @param thePacket<br>
	 * @return the type. 
	 */
	public static int getIpProtocolType(byte[] thePacket)
	{
		int n = ByteUtils.getByteNetOrderTo_uint8(thePacket, EthernetFrame.ETHERNET_HEADER_LENGTH + IP_Proto_POS); 
		if (isExtension(n))
		{
			
			throw new UnsupportedOperationException("packet has extension (not supported yet!)");
		}
		return n;
	}
	
	private static boolean isExtension(int n)
	{
		switch(n)
		{
		case IPPacketType.ICMP:
		case IPPacketType.TCP:
		case IPPacketType.UDP:
			return false;
		default: 
			return true;
		}
	}
	
	@Override
	public String getSourceIPAsString()
	{
		return mySrcIp.getAsReadableString();
	}

	@Override
	public String getDestinationIPAsString()
	{
		return myDstIp.getAsReadableString();
	}

	@Override
	public boolean isIPv4()
	{
		return false;
	}

	@Override
	public void setIPProtocol(int TheIpProtocol)
	{
		if (myExt.size() > 0)
		{
			myExt.getLast().setNextType(TheIpProtocol);
		}
		else 
		{
			myNextHeadr = TheIpProtocol;
			myLastHdrType = TheIpProtocol;
			_isWriteUndelyingProto = true;
		}
	}
	
	protected int getTotalExtensionsLength()
	{
		int sum = 0;
		for(IPv6Extension next : myExt)
		{
			sum+=next.getLength();
		}
		return sum;
	}
	
	private boolean _isIPread = false;
	
	public int getIpProtocol()
	{
		if (_isSniffedPkt && !_isIPread)
		{
			myNextHeadr = myPacket[getHeaderOffset()+IP_Proto_POS] & 0xff;
			_isIPread = true;
		}
		if (myExt.size() == 0)
			return myNextHeadr;
		return myExt.getLast().getNextType();
	}

	protected void putIPHeader()
	{
		atuoComplete();
		myIPHdrOffset = getHeaderOffset();
		int pos = ETHERNET_HEADER_LENGTH;
		long n = (6 << 28) | (myTrafficClass << 20) | myFlowLabel;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, n, 4);
		pos+=4;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, myPayloadLength, 2);
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, myNextHeadr, 1);
		pos+=1;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, myHopLimit, 1);
		pos+=1;
		System.arraycopy(getSourceIP().getIpv6BigEndianByteaArray(), 0, myPacket, pos, 16);
		pos+=16;
		System.arraycopy(getDestinationIP().getIpv6BigEndianByteaArray(), 0, myPacket, pos, 16);
		pos+=16;
		for(IPv6Extension next : myExt)
		{
			byte raw[] = next.getAsRawByArray();
			System.arraycopy(raw, 0, myPacket, pos, raw.length);
			pos+=raw.length;
		}
		
	}
	
	private boolean _isReadTrafficClass = false;

	public int getTrafficClass()
	{
		if (_isReadTrafficClass == false && _isSniffedPkt )
		{
			myTrafficClass = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset);
			myTrafficClass = (myTrafficClass >> 4) & 0xffff;
			_isReadTrafficClass = true;
		}
		return myTrafficClass;
	}

	public void setTrafficClass(int theMyTrafficClass)
	{
		myTrafficClass = theMyTrafficClass;
	}

	private boolean _isReadFlowLabel = false;
	
	public int getFlowLabel()
	{
		if( !_isReadFlowLabel && _isSniffedPkt)
		{
			long tmp = ByteUtils.getByteNetOrder(myPacket, myIPHdrLength, 0);
			myFlowLabel = (int) (tmp & 0xfffff);
			_isReadFlowLabel = true;
		}
		return myFlowLabel;
	}

	public void setFlowLabel(int theMyFlowLabel)
	{
		myFlowLabel = theMyFlowLabel;
	}

	@Override
	public void toReadableText(StringBuffer sb)
	{
		sb.append("Internet Protocol\n");
		sb.append("Ver: 6\n");
		sb.append("Traffic Class : "+getTrafficClass());
		sb.append("\n");
		sb.append("Flow Label: "+getFlowLabel());
		sb.append("\n");
		sb.append("Payload Length: "+getIpPktTotalLength());
		sb.append("\n");
		sb.append("Next Header: "+Integer.toHexString(getIpProtocol()));
		sb.append("\n");
		sb.append("Hop Limit: "+getHopLimit());
		sb.append("\n");
		sb.append("Source: "+getSourceIPAsString());
		sb.append("\n");
		sb.append("Destination: "+getDestinationIPAsString());
		sb.append("\n");
		
	}
}



