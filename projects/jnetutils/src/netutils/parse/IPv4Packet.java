package netutils.parse;

import netutils.utils.ByteUtils;
import netutils.utils.RandomGenerator;

/**
 * The class implement ipv4 packet.<br>
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
public class IPv4Packet extends IPPacketBase
{
	// some constants for local use.
	private static final int DEFAULT_TTL = 64;

	private static final short DEFAULT_TOS = 0;

	private static final int IPV4 = 4;

	private static final int DEFAULT_IP_HDR_LENGTH = 20;
	
	public static final int CALCULATE_CHKSUM = 0;

	/*
	 *  1                 8            16            24            31
	 *  -----------------------------------------------------------
	 * | 4 bits |4 bits | 8 bits tos   | 16 bits total length      |    
	 * | version|hdr ln |              | of packet (in bytes)      |
	 *  ------------------------------------------------------------
	 * | 16 bits id                    | 3 bits | 13-bit frag      |
	 * |                               |  flasg | offset           |
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
	 * const for the fields offsets from the start of the ip packet
	 * (not including the Etherent packet header)
	 */
	private static final int IP_TOS_POS = 1; // type of service

	private static final int IP_LEN_POS = 2; // total pkt length

	private static final int IP_ID_POS = 4; // the packet id

	private static final int IP_FRAG_POS = 6; // the fragemnt flags and offset

	private static final int IP_TTL_POS = 8; // the ttl 

	private static final int IP_Proto_POS = 9; // protocol type field

	private static final int IP_CHKSUM_POS = 10; // checksum field 

	private static final int IP_SRC_POS = 12; // src ip

	private static final int IP_DST_POS = 16; // dst ip


	// header version field
	private int myVersion = IPV4;

	// Type of Service field
	private int myTOS = 0;

	// IP header length
	protected int myIPTotalLength = 0;

	// IP packet id (seqance id)
	protected int myIPid = 0;

	// IP fragment flags field
	protected int myFragmentFlags = 0;

	// IP fragment offset field.
	private int myFragmentOffset = 0;

	// IP Time to Live field
	private int myTTL;

	// IP protocol carried field (TCP,UDP...etc)
	private int myIPProto = 0;

	// IP checksum field
	private int myChksum = 0;

	// IP source IP field
	private long mySrcIp = 0;

	// IP destination IP
	private long myDstIp = 0;
	
	// used when building packet to know which parameters were set by
	// the user and which will be needed to be completed.
	boolean _isWriteChksum = false;
	
	boolean _isWriteFragFlags = false;
	boolean _isWriteFragOffset = false;
	boolean _isWriteID = false;
	boolean _isWriteProto = false;
	boolean _isWriteSrcIp = false;
	boolean _isWriteTOS = false;
	boolean _isWriteTTl = false;
	boolean _isWriteVesrion = false;

	

	/**
	 * build empty packet.
	 */
	public IPv4Packet()
	{
		super();
		// make sure we will not to try to read parameters from internal buffer
		// that doesn't exists, because packet wasn't sniffed. 
		_isReadChksum = true;
		_isReadDstIp = true;
		_isReadFragFlags = true;
		_isReadFragOffset = true;
		_isReadID = true;
		_isReadProto = true;
		_isReadSrcIp = true;
		_isReadTOS = true;
		_isReadTTl = true;
		_isReadVesrion = true;
		myIPHdrLength = DEFAULT_IP_HDR_LENGTH;
	}
	
	/**
	 * create new IP packet
	 * @param thePacket - byte array of a valid IP packet.
	 */
	public IPv4Packet(byte[] thePacket)
	{
		super(thePacket);
		// only set pointers. fields will extracted only when 
		// thier value is needed to save performance.
		myIPHdrOffset = getHeaderOffset();
		myIPHdrLength = (thePacket[myIPHdrOffset] & (0x0f)) * 4;
		myIPDataOffset = myIPHdrOffset + myIPHdrLength;
		_isSniffedPkt = true;
	}
	
	/**
	 * will try to auto complete unfilled parameters such as
	 * ttl,tos... etc<br>
	 * <br>
	 * the following parameters must be set by the user:<br>
	 * 1. source ip<br>
	 * 2. destination ip<br>
	 * 3. protocol (tcp/udp..etc)<br>
	 * 4. payload
	 */
	public void atuoComplete()
	{
		if (_isWriteDstIp == false || _isWriteSrcIp == false || getIPData() == null || _isWriteProto == false)
			throw new IllegalPacketException("Src ip,Dst ip or data missing");
		
		if (_isWriteChksum == false)
			setCheckSum(CALCULATE_CHKSUM);
		
		if ( _isWriteFragFlags == false)
			setFragFlags(0);
		if ( _isWriteFragOffset == false)
			setFragOffset(0);
		if ( _isWriteID == false)
			setID(RandomGenerator.getRandInt());
		if ( _isWriteTOS == false)
			setTos(DEFAULT_TOS);
		if ( _isWriteTTl == false)
			setTTL(DEFAULT_TTL);
	}
	
	/**
	 * used by sub (TCP/UDP...etc) classes where the data is used there.
	 *
	 */
	protected void atuoCompleteNoData()
	{
		if (_isWriteChksum == false)
			setCheckSum(CALCULATE_CHKSUM);
		
		if ( _isWriteFragFlags == false)
			setFragFlags(0);
		if ( _isWriteFragOffset == false)
			setFragOffset(0);
		if ( _isWriteID == false)
			setID(RandomGenerator.getRandInt());
		if ( _isWriteTOS == false)
			setTos(DEFAULT_TOS);
		if ( _isWriteTTl == false)
			setTTL(DEFAULT_TTL);
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
		return !(_isWriteDstIp == false || _isWriteSrcIp == false || getIPData() == null || _isWriteProto == false);
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
		return !(_isWriteDstIp == false || _isWriteSrcIp == false || _isWriteProto == false);
	}

	boolean _isReadVesrion = false;

	/** 
	 * Get the IP version code.
	 */
	public int getVersion()
	{
		if (_isReadVesrion == false && _isSniffedPkt)
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
		return myIPHdrLength;
	}

	boolean _isReadTOS = false;

	/** 
	 * @return the type of service field. 
	 */
	public int getTypeOfService()
	{
		if (_isReadTOS == false && _isSniffedPkt)
		{
			myTOS = myPacket[myIPHdrOffset + IP_TOS_POS] & 0x0f;
			_isReadTOS = true;
		}
		return myTOS;
	}

	/**
	 * Set the tos field.
	 * this suppose to be unsigned_8int so value should be in the range 0-255
	 * (no enforcement - on larger numbers only last 8 bits would be used).
	 * @param newTos
	 */
	public void setTos(int newTos)
	{
		_isReadTOS = true;
		_isWriteTOS = true;
		myTOS = newTos;
	}

	boolean _readTotalLength = false;

	/** 
	 * @return the IP length in bytes.
	 */
	public int getIpPktTotalLength()
	{
		if (!_readTotalLength)
		{
			if (_isSniffedPkt)
			{
				myIPTotalLength = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset + IP_LEN_POS);
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
	
	boolean _isReadID = false;

	/**
	 * @return the ID of the IP segment. 
	 **/
	public int getId()
	{
		if (_isReadID == false && _isSniffedPkt )
		{
		    myIPid = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset + IP_ID_POS);
		    _isReadID = true;
		}
		return myIPid;
	}

	/**
	 * Set the packet id num<br>
	 * 
	 * @param id - suppose to be 16 bit unsigned.<br>
	 * (no enforcement - larger numbers would be cut)
	 */
	public void setID(int id)
	{
		_isReadID = true;
		_isWriteID = true;
		myIPid = id;
	}
	
	
	boolean _isReadFragFlags = false;
	
	/** 
	 *@return fragmentation flags field.
	 */
	public int getFragmentFlags()
	{
		if (_isReadFragFlags == false && _isSniffedPkt)
		{
			_isReadFragFlags = true;
		    myFragmentFlags = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset + IP_FRAG_POS) >> 13;
		}
		return myFragmentFlags;
	}
	
	/**
	 * @return true if this is a fragment packet.
	 */
	public boolean isFragmented()
	{
	    return (getFragmentOffset() > 0);  
	}
	
	/**
	 * set the fragment flags (3 bit frags).<br>
	 * @param flags (range 0-7)
	 */
	public void setFragFlags(int flags)
	{
		_isReadFragFlags = true;
		_isWriteFragFlags = true;
		myFragmentFlags = flags;
	}

	boolean _isReadFragOffset = false;
	
	/** 
	 *@return the fragment offset.
	 */
	public int getFragmentOffset()
	{
		if (_isReadFragOffset == false && _isSniffedPkt)
		{
			_isReadFragOffset = true;
		     myFragmentOffset = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset + IP_FRAG_POS) & 0x1fff;
		}
		return myFragmentOffset;
	}
	
	/**
	 * set the frag offset (13 bits)
	 * @param fragOffset
	 */
	public void setFragOffset(int fragOffset)
	{
		_isReadFragOffset = true;
		_isWriteFragOffset = true;
		myFragmentOffset = fragOffset;
	}

	boolean _isReadTTl = false;
	
	/**
	 * @return the time to live. 
	 */
	public int getTTL()
	{
		if (_isReadTTl == false && _isSniffedPkt)
		{
			_isReadTTl = true;
			myTTL = ByteUtils.getByteNetOrderTo_uint8(myPacket, myIPHdrOffset+IP_TTL_POS);			
		}
		return myTTL;
	}
	
	/**
	 * set the ttl - (8 bits num)
	 * @param ttl
	 * ( on bigger number would cut the last 8 bits)
	 */
	public void setTTL(int ttl)
	{
		_isWriteTTl = true;
		_isReadTTl = true;
		myTTL = ttl;
	}
	
	boolean _isReadProto = false;

	/**
	 * @return the IP protocol type of the packet.
	 * ( udp,icmp,tcp...etc)
	 */
	public int getIPProtocol()
	{
		if (_isReadProto == false)
		{
			_isReadProto = true;
		   myIPProto = ByteUtils.getByteNetOrderTo_uint8(myPacket, myIPHdrOffset + IP_Proto_POS);
		}
		return myIPProto;
	}
	
	/**
	 * Set the ip protocol.
	 * 0x06 == TCP,0x01 == ICMP,0x11 == UDP<br>
	 * @param proto - 8 bit number 
	 * 
	 */
	public void setIPProtocol(int proto)
	{
		_isWriteProto = true;
		_isReadProto = true;
		myIPProto = proto;
	}
	
	boolean _isReadChksum = false;

	/** 
	 * @return the header checksum.
	 */
	public int getIPChecksum()
	{
		if (_isReadChksum == false && _isSniffedPkt)
		{
			_isReadChksum = true;
		     myChksum = ByteUtils.getByteNetOrderTo_uint16(myPacket, myIPHdrOffset + IP_CHKSUM_POS);
		}
		return myChksum;
	}
	
	/**
	 * set the pkt check sum.
	 * @param chksum - 16 bit
	 * use CALCULATE_CHKSUM (..or put 0) if you want the check sum to be calculated automatically
	 */
	public void setCheckSum(int chksum)
	{
		_isWriteChksum = true;
		_isReadChksum = true;
		myChksum = chksum;
	}

	boolean _isReadSrcIp = false;
	
	/** 
	 * @return the source IP address.
	 */
	public IPv4Address getSourceIPv4()
	{
		if (_isReadSrcIp == false && _isSniffedPkt)
		{
		     mySrcIp = ByteUtils.getByteNetOrderTo_unit32(myPacket, myIPHdrOffset + IP_SRC_POS);
		     _isReadSrcIp = true;
		}
		return new IPv4Address(mySrcIp);
	}
	
	public IPv4Address getSourceIP()
	{
		return getSourceIPv4();
	}
	
	/**
	 * set the ip
	 * @param theIp - 32 bit long.
	 */
	public void setSrcIp(long theIp)
	{
		_isWriteSrcIp = true;
		_isReadSrcIp = true;
		mySrcIp = theIp;
	}
	
	/**
	 * @return IP as a readable string x.x.x.x
	 */
	public String getSourceIPAsString()
	{
		return getSourceIPv4().getAsReadableString();
	}

	boolean _isReadDstIp = false;
	
	/** 
	 * @return the destination IP address.
	 */
	public IPv4Address getDestinationIPv4()
	{
		if (_isReadDstIp == false && _isSniffedPkt)
		{
			_isReadDstIp = true;
		     myDstIp = ByteUtils.getByteNetOrderTo_unit32(myPacket, myIPHdrOffset + IP_DST_POS);
		}
		return new IPv4Address(myDstIp);
	}
	
	public IPv4Address getDestinationIP()
	{
		return getDestinationIPv4();
	}
	
	/**
	 * set the destination IP
	 * @param theDstIp
	 */
	public void setDstIp(long theDstIp)
	{
		_isWriteDstIp = true;
		_isReadDstIp = true;
		myDstIp = theDstIp;
	}
		
	/**
	 * @return destination ip as a readable string.
	 */
	public String getDestinationIPAsString()
	{
		return getDestinationIPv4().getAsReadableString();
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
	}
	
	/**
	 * Calculate the ip hdr check sum.<br>
	 * <br>
	 * should run on all header bytes and add them as 16 bit ones-complement values.
	 * adding two bytes at a time (network order) .<br>
	 * count the overflow.<br>
	 * return the ~(result)<br>
	 * <br>
	 * the checksum field should be counted as zero.<br>
	 * @return the checksum 
	 */
	protected int getIpHeaderCheckSum()
	{
		byte[] copyOfHeader = getIPHeader();

		int sum = 0;

		for (int i = 0; i < copyOfHeader.length; i += 2)
		{
			if (i == IP_CHKSUM_POS) // ignore the check sum filed itself
				continue;

			int byte1Val = copyOfHeader[i] & 0xff;
			int byte2Val = (i + 1 < copyOfHeader.length) ? copyOfHeader[i + 1] & 0xff : 0;
			sum = sum + ((byte1Val << 8) + byte2Val);
		}

		sum = (sum >> 16) + (sum & 0xffff);
		sum = sum + (sum >> 16);
		sum = ~sum & 0xffff;

		return sum;
	}

	/**
	 * static methods for faster performance.<br>
	 * <br>
	 * where we just want to know the type of the packet (udp or icmp...) there is no need to create 
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
		return ByteUtils.getByteNetOrderTo_uint8(thePacket, EthernetFrame.statGetEthHdrLen(thePacket) + IP_Proto_POS);
	}
	
	/**
	 * static method for faster performance<br>
	 * <br>
	 * <br>
	 * @pre assuming packet is a valid IP packet (not checking it here)<br>
	 * @param thePacket<br>
	 * @return true if packet is a fragment and false otherwise<br>
	 */
	public static boolean isFragment(byte[] thePacket)
	{
		int b =  ByteUtils.getByteNetOrderTo_uint16(thePacket, ETHERNET_HEADER_LENGTH+IP_FRAG_POS);
		b>>=8;
		return ((b & 0x1f )!= 0 ) | (( b & 0x20) != 0);
	}
	
	public void toReadableText(StringBuffer sb)
	{
		sb.append("Internet Protocol\n");
		sb.append("Ver: 4\n");
		sb.append("Hdr Length: "+getIPHeaderLength());
		sb.append("\n");
		sb.append("TOS: "+getTypeOfService());
		sb.append("\n");
		sb.append("Total Length: "+getIpPktTotalLength());
		sb.append("\n");
		sb.append("Identification: "+getId());
		sb.append("\n");
		sb.append("Flags: "+ Integer.toHexString(getFragmentFlags()));
		sb.append("\n");
		sb.append("Fragmnet Offset: "+getFragmentOffset());
		sb.append("\n");
		sb.append("TTL: "+getTTL());
		sb.append("\n");
		sb.append("Protocol: "+Integer.toHexString(getIPProtocol()));
		sb.append("\n");
		sb.append("Checksum: "+getIPChecksum());
		sb.append("\n");
		sb.append("Source: "+getSourceIPAsString());
		sb.append("\n");
		sb.append("Destination: "+getDestinationIPAsString());
		sb.append("\n");
	}

	/**
	 * the method receive IP packet and return the correct type instance, for example
	 * tcp,udp ...etc.<br>
	 * @param thePacket - the packet array<br>
	 * @return the instance.<br>
	 * <br>
	 * @throws the method may throw exception if protocol not supported or if the packet
	 * is illegal.<br>
	 */
	public static IPPacket getPacket(byte[] thePacket)
	{
		try
		{
			int type = getIpProtocolType(thePacket);
			System.out.println(type);
			switch (type)
			{
			case IPPacketType.TCP:
				return new TCPPacketIpv4(thePacket);
			case IPPacketType.UDP:
				return new UDPPacket(thePacket);
			case IPPacketType.ICMP:
				return new ICMPPacket(thePacket);
			default:
				throw new UnsupportedProtocol("unsupported protocol type:" + type);
			}
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
			System.out.println(thePacket.length);
			throw new IllegalPacketException("Got illeagl ip packet");
		}
	}

	@Override
	public boolean isIPv4()
	{
		return true;
	}
	
	protected void putIPHeader()
	{
		atuoComplete();
		myIPHdrOffset = getHeaderOffset();
		int pos = ETHERNET_HEADER_LENGTH;
		int n = (4 << 4) | (getIPHeaderLength()/4);
		myPacket[pos]=(byte) n;
		pos++;
		myPacket[pos]=(byte) myTOS;
		pos++;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, getIpPktTotalLength(), 2);
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, getId(), 2);
		pos+=2;
		n = getFragmentFlags() << 13 | getFragmentOffset();
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, n, 2);
		pos+=2;
		myPacket[pos] = (byte) myTTL;
		pos++;
		myPacket[pos] = (byte) myIPProto;
		pos++;
		
		// should be calculated later
		int checksumPos = pos;
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, getSourceIPv4().getIPasLong(), 4);
		pos+=4;
		ByteUtils.setBigIndianInBytesArray(myPacket, pos, getDestinationIPv4().getIPasLong(), 4);
		ByteUtils.setBigIndianInBytesArray(myPacket, checksumPos, getIpHeaderCheckSum(), 2);
	}

	

	
}

class UnsupportedProtocol extends RuntimeException
{
	public UnsupportedProtocol(String message)
	{
		super(message);
	}
}


