package netutils.parse;

import netutils.NetUtilsException;
import netutils.utils.ByteUtils;
import netutils.utils.RandomGenerator;


/**
 * TCP packet implementation. The class provides methods for parsing
 * and building TCP packets.<br>
 * Mainly accessing and setting fields.<br>
 * <br>
 * 
 * @author roni bar-yanai
 *<br>
 */
public class TCPPacket extends IPPacket
{
	/*
	 *  UDP and TCP include a 12-byte pseudo-header with the UDP datagram (or TCP segment)
	 *  just for the checksum computation. This pseudo-header includes certain fields from the IP header.
	 * 
	 *  the 12 byte include:
	 *  0-4 : the ip src 
	 *  5-8 : the ip dst
	 *  9 : zero byte
	 *  10 : the protocl type 0x11 for udp
	 *  11 - 12 : the total length of the udp packet.
	 *  
	 *  constants for udp packet pseudo header
	 */
	private static final int PSEUDO_HDR_LEN = 12;

	private static final int PSEUDO_IP_SRC_POS = 0;

	private static final int PSEUDO_IP_DST_POS = 4;

	private static final int PSEUDO_ZERO_POS = 8;

	private static final int PSEUDO_PROTO_CODE_POS = 9;

	private static final int PSEUDO_UDP_LENGTH_POS = 10;

	public static final int CALCULATE_TCP_CHECKSUM = 0;
	
	// constants for internal use.
	private static final int DEFAULT_WINDOW_SIZE = 8192;
		
	private static final int DEFAULT_TCP_HDR_LENGTH = 20;

	public final static int TCP_URG_MASK = 0x0020;

	public final static int TCP_ACK_MASK = 0x0010;

	public final static int TCP_PSH_MASK = 0x0008;

	public final static int TCP_RST_MASK = 0x0004;

	public final static int TCP_SYN_MASK = 0x0002;

	public final static int TCP_FIN_MASK = 0x0001;
	
	 

	/* 
	 * tcp packet structure:
	 * ---------------------
	 * 
	 * 
	 * 0                                 15 16                                    32
	 * ------------------------------------------------------------------------------
	 * |    16 bit src port                |          16 bit dst port               |
	 * ------------------------------------------------------------------------------
	 * |                32 bit sequence number                                      |
	 * ------------------------------------------------------------------------------
	 * |                32 bit acknowledgment number                                |
	 * ------------------------------------------------------------------------------
	 * |  hdr |  6  reserved   |U|A|P|R|S|F|                                        |
	 * | size |   bits         |R|C|S|S|Y|I|      16 bit, window size               |
	 * |4 bits|                |G|K|H|T|N|N|                                        |
	 * ------------------------------------------------------------------------------
	 * |  16 bit tcp check sum             |    16 bit urgent pointer               |
	 * ------------------------------------------------------------------------------
	 * |      options if any                                                        |
	 * |                                   |                                        |
	 * -----------------------------------------------------------------------------
	 * |
	 * |                 DATA 
	 * |
	 * --------------------------------------------------------------------------=-
	 *
	 * Constants for common offsers in the header.
	 */
	private static final int TCP_SRC_PORT_POS = 0;

	private static final int TCP_DST_PORT_POS = 2;

	private static final int TCP_SEQUENCE_NUM_POS = 4;

	private static final int TCP_ACK_NUM_POS = 8;

	private static final int TCP_HDR_LEN_POS = 12;

	private static final int TCP_HDR_FLAGS_POS = 12;

	private static final int TCP_WINDOW_SIZE_POS = 14;

	protected static final int TCP_CHECKSUM_POS = 16;

	private static final int TCP_URGENT_PTR_POS = 18;

	// members for holding fields values.
	// As in other classes, fields are only parsed when need
	// or set in order to save performance.
	private int _tcpOffset = 0;
	
	// holds the header length
	private int myTcpHdrLh = DEFAULT_TCP_HDR_LENGTH;

	// holds the source port
	protected int mySrcPort = 0;

	// holds the destination port
	protected int myDstPort = 0;

	// holds the packet sequence number
	private long mySequenceNumber = 0;

	// holds the acknowledgment number
	private long myAcknowledgmentNumber = 0;

	// holds payload length
	private int myPayloadDataLength = 0;

	// tcp window size
	private int myWindowSize = 0;

	// hold the checksum field
	private int myTcpChecksum = 0;

	// urger pointer field
	private int myUrgentPointer = 0;

	// holds the tcp header flags (syn,rst...etc)
	private int myFlags = 0;
	
	// total tcp packet length
	protected int myTotalTcpLength = 0;
		
	// tracing which fields were set by the user.
	boolean _isWriteAckNum = false;
	boolean _isWriteSequenceNum = false;
	boolean _isWriteTCPCheckSum = false;
	boolean _isWriteUrgentPtr = false;
	boolean _isWriteWindowSize = false;

	/**
	 * Create new empty tcp packet.
	 */
	public TCPPacket()
	{
		super();
		myIPPacket = new IPv4Packet();
		// make sure all fields were set as field so 
		// we will not try to read them from non existing buffer.
		_isReadAckNum = true;
		_isReadDstPort = true;
		_isReadSequenceNum = true;
		_isReadSrcPort = true;
		_isReadTCPCheckSum = true;
		_isReadUrgentPtr = true;
		_isReadWindowSize = true;
		_isReadAllFlags = true;
		myIPPacket.setIPProtocol(IPPacketType.TCP);
		myTotalTcpLength = DEFAULT_TCP_HDR_LENGTH;
	}

	/**
	 * Create new TCP packet. <br>
	 * @pre the buffer contains valid buffer.
	 * @param thePacket - byte buffer with valid packet (including ip and eth parts).
	 */
	public TCPPacket(byte[] thePacket)
	{
		myIPPacket = IPFactory.createIPPacket(thePacket);
		_tcpOffset = myIPPacket.myIPHdrOffset + myIPPacket.getIPHeaderLength();
		myTcpHdrLh = ((ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_HDR_LEN_POS) >> 12) & 0x0f) * 4;
		myPayloadDataLength = myIPPacket.getIpPktTotalLength() - myIPPacket.myIPHdrLength - myTcpHdrLh;
		myTotalTcpLength = myTcpHdrLh+myPayloadDataLength; 
	}
	
	protected TCPPacket(boolean isIPv6)
	{
		this();
		if(isIPv6)
		{
			myIPPacket = new IPv6Packet();
			myIPPacket.setIPProtocol(IPPacketType.TCP);
			myIPPacket.setPacketType(EthernetFrame.ETHERNET_IPv6_PKT_TYPE);
		}
	}

	boolean _isReadSrcPort = false;

	/** 
	 * @return the source port number.
	 */
	public int getSourcePort()
	{
		if (_isReadSrcPort == false)
		{
			_isReadSrcPort = true;
			mySrcPort = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_SRC_PORT_POS);
		}
		return mySrcPort;
	}

	boolean _isWriteSrcPort = false;

	/**
	 * Set the destination port<br>
	 * 
	 * @param port - 0-65535 (16 bits)
	 *  (no enforcement - lager values would be trimmed)
	 */
	public void setSourcePort(int port)
	{
		_isReadSrcPort = true;
		_isWriteSrcPort = true;
		mySrcPort = port;
	}

	boolean _isReadDstPort = false;

	/** 
	 * @return the destination port number.
	 */
	public int getDestinationPort()
	{
		if (_isReadDstPort == false)
		{
			myDstPort = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_DST_PORT_POS);
			_isReadDstPort = true;
		}
		return myDstPort;
	}

	boolean _isWriteDstPort = false;

	/**
	 * set the destination port
	 * @param port - 16 bit unsigned.
	 */
	public void setDestinationPort(int port)
	{
		_isWriteDstPort = true;
		_isReadDstPort = true;
		myDstPort = port;
	}

	boolean _isReadSequenceNum = false;

	/** 
	 * @return the packet sequence number.
	 */
	public long getSequenceNumber()
	{
		if (_isReadSequenceNum == false)
		{
			mySequenceNumber = ByteUtils.getByteNetOrderTo_unit32(myIPPacket.myPacket, _tcpOffset + TCP_SEQUENCE_NUM_POS);
			_isReadSequenceNum = true;
		}
		return mySequenceNumber;
	}

	/**
	 * set the tcp sequence num
	 * @param sequence - 32 bit unsigned integer.
	 */
	public void setSequenceNum(long sequence)
	{
		_isWriteSequenceNum = true;
		_isReadSequenceNum = true;
		mySequenceNumber = sequence;
	}

	boolean _isReadAckNum = false;

	/** 
	 *@return the packet acknowledgment number.
	 */
	public long getAcknowledgmentNumber()
	{
		if (_isReadAckNum == false)
		{
			_isReadAckNum = true;
			myAcknowledgmentNumber = ByteUtils.getByteNetOrderTo_unit32(myIPPacket.myPacket, _tcpOffset + TCP_ACK_NUM_POS);
		}
		return myAcknowledgmentNumber;
	}

	/**
	 * set the acknowledgment number
	 * @param ack - 32 bit unsigned integer. 
	 */
	public void setAckNum(long ack)
	{
		_isWriteAckNum = true;
		_isReadAckNum = true;
		myAcknowledgmentNumber = ack;
	}

	/** 
	 * @return the TCP header length in bytes.
	 */
	public int getTCPHeaderLength()
	{
		return myTcpHdrLh;
	}

	/**
	 * @return the length of the payload data.
	 */
	public int getPayloadDataLength()
	{
		return myPayloadDataLength;
	}

	boolean _isReadWindowSize = false;

	/**
	 * get the window size.
	 */
	public int getWindowSize()
	{
		if (_isReadWindowSize == false)
		{
			myWindowSize = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_WINDOW_SIZE_POS);
			_isReadWindowSize = true;
		}
		return myWindowSize;
	}

	/**
	 * set the window size.
	 * @param size - 16 bits unsigned integer
	 */
	public void setWindowSize(int size)
	{
		_isWriteWindowSize = true;
		_isReadWindowSize = true;
		myWindowSize = size;
	}

	boolean _isReadTCPCheckSum = false;

	/** 
	 * @return header checksum.
	 */
	public int getTCPChecksum()
	{
		if (_isReadTCPCheckSum == false || myTcpChecksum == 0)
		{
			myTcpChecksum = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_CHECKSUM_POS);
			_isReadTCPCheckSum = true;
		}
		return myTcpChecksum;
	}

	/**
	 * set the tcp check sum.
	 * set to CALCULATE_TCP_CHECKSUM to be filled automatically.
	 * @param checksum - 16 bit unsigned int
	 * 
	 */
	public void setTCPChecksum(int checksum)
	{
		_isReadTCPCheckSum = true;
		myTcpChecksum = checksum;
	}

	boolean _isReadUrgentPtr = false;

	/** 
	 * @return the urgent pointer.
	 */
	public int getUrgentPointer()
	{

		if (_isReadUrgentPtr == false)
		{
			myUrgentPointer = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_URGENT_PTR_POS);
			_isReadUrgentPtr = true;
		}
		return myUrgentPointer;
	}
	
	/**
	 * set the urgent ptr val
	 * @param val - 16 bit unsigned int.
	 */
	public void setUrgentPointer(int val)
	{
		_isReadUrgentPtr = true;
		myUrgentPointer = val;
	}

	private boolean _isReadAllFlags = false;

	/**
	 * @return flags field.
	 */
	public int getAllFlags()
	{
		if (_isReadAllFlags == false)
		{
			myFlags = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket, _tcpOffset + TCP_HDR_LEN_POS) & 0x3f;
			_isReadAllFlags = true;
		}
		return myFlags;
	}

	/**
	 * @return true if URG flag is on. 
	 * Check the URG flag, flag indicates if the urgent pointer is valid.
	 */
	public boolean isUrg()
	{
		if (_isReadAllFlags == false) getAllFlags();

		return (myFlags & TCP_URG_MASK) != 0;
	}
	
	/**
	 * set the urgent flag
	 * @param value
	 */
	public void setUrg(boolean value)
	{
		_isReadAllFlags = true;
		myFlags = (value)?(myFlags | TCP_URG_MASK):(myFlags & (~TCP_URG_MASK)); 
	}

	/** 
	 * Check the ACK flag, flag indicates if the ack number is set.
	 */
	public boolean isAck()
	{
		if (_isReadAllFlags == false) getAllFlags();
		return (myFlags & TCP_ACK_MASK) != 0;
	}
	
	/**
	 * set the ack flag state.
	 * @param value - true or false
	 */
	public void setAck(boolean value)
	{
		_isReadAllFlags = true;
		myFlags = (value)?(myFlags | TCP_ACK_MASK):(myFlags & (~TCP_ACK_MASK));
	}

	/** 
	 * Check the PSH flag, flag indicates the receiver should pass the
	 * data to the application as soon as possible.<br>
	 * (not really in use)
	 */
	public boolean isPsh()
	{
		if (_isReadAllFlags == false) getAllFlags();
		return (myFlags & TCP_PSH_MASK) != 0;
	}
	
	/**
	 * set the push flag state
	 * @param value - true or false.
	 */
	public void setPsh(boolean value)
	{
		_isReadAllFlags = true;
		myFlags = (value)?(myFlags | TCP_PSH_MASK):(myFlags & (~TCP_PSH_MASK));
	}

	/** 
	 * Check the RST flag, flag indicates the session should be reset between
	 * the sender and the receiver.
	 */
	public boolean isRst()
	{
		if (_isReadAllFlags == false)
		{
			getAllFlags();
		}
		return (myFlags & TCP_RST_MASK) != 0;
	}
	
	/**
	 * set the reset flag state.
	 * @param value - true or false
	 */
	public void setRst(boolean value)
	{
		_isReadAllFlags = true;
		myFlags = (value)?(myFlags | TCP_RST_MASK):(myFlags & (~TCP_RST_MASK));
	}

	/** 
	 * Check the SYN flag, flag indicates the sequence numbers should
	 * be synchronized between the sender and receiver to initiate
	 * a connection.,br>
	 */
	public boolean isSyn()
	{
		if (_isReadAllFlags == false)
		{
			getAllFlags();
		}

		return (myFlags & TCP_SYN_MASK) != 0;
	}
	
	/**
	 * set the tcp syn flag state.
	 * @param value
	 */
	public void setSyn(boolean value)
	{
		_isReadAllFlags = true;
		myFlags = (value)?(myFlags | TCP_SYN_MASK):(myFlags & (~TCP_SYN_MASK));
	}

	/** 
	 * @return the FIN flag, flag indicates the sender is finished sending.
	 */
	public boolean isFin()
	{
		if (_isReadAllFlags == false) getAllFlags();

		return (myFlags & TCP_FIN_MASK) != 0;
	}
	
	/**
	 * Set the fin flag state.
	 * @param value - true or false
	 */
	public void setFin(boolean value)
	{
		_isReadAllFlags = true;
		myFlags = (value)?(myFlags | TCP_FIN_MASK):(myFlags & (~TCP_FIN_MASK));
	}

	private byte[] _tcpHeaderBytes = null;

	/**
	 * @return the TCP header as a byte array.
	 */
	public byte[] getTCPHeader()
	{
		if (_tcpHeaderBytes == null && (myIPPacket._isSniffedPkt || myIPPacket.myPacket != null))
		{
			_tcpHeaderBytes = ByteUtils.extractBytesArray(myIPPacket.myPacket, _tcpOffset, myTcpHdrLh);
		}
		
		return _tcpHeaderBytes;
	}
	
	public void setTCPHeader(byte hdr[])
	{
		_tcpHeaderBytes = hdr;
	}

	protected byte[] _tcpDataBytes = null;

	/** 
	 * @return the TCP data as a byte array.
	 */
	public byte[] getTCPData()
	{
		if (_tcpDataBytes == null && myIPPacket._isSniffedPkt)
		{
			_tcpDataBytes = ByteUtils.extractBytesArray(myIPPacket.myPacket, _tcpOffset + myTcpHdrLh, myPayloadDataLength);
		}
		return _tcpDataBytes;
	}
	
	

	/**
	 * will put defaults on all fields that were not set.<br>
	 * mandatory fields:<br>
	 * <br>
	 * src ip and src port.<br>
	 * dst ip and dst port.<br>
	 * payload <br>
	 * 
	 * @Override
	 */
	public void atuoComplete()
	{
		if (_isWriteSequenceNum == false)
			mySequenceNumber = RandomGenerator.getRandInt();
		
		if (_isWriteWindowSize == false)
			setWindowSize(DEFAULT_WINDOW_SIZE);
				
		myIPPacket.atuoCompleteNoData();
		
	}

	/**
	 * check if all mandatory fields are filled.<br>
	 *<br>
	 * mandatory fileds:<br>
	 * 1. src ip and src port.<br>
	 * 2. dst ip and dst port.<br>
	 * 3. data <br>
	 */
	public boolean isMandatoryFieldsSet()
	{
		if (myIPPacket.isMandatoryFieldsSetNoData() == false)
			return false;
		
		if (_isWriteSrcPort == false || myIPPacket._isWriteDstIp == false)
			return false;
		 
		return true;
	}

	/**
	 * set the payload.
	 * @param theData - byte array of the payload, mustn't be longer then the 
	 *  maximum allowed tcp payload.
	 */
	public void setData(byte[] theData)
	{
		if (theData == null)
			return;
		 _tcpDataBytes = theData;
		 myPayloadDataLength = theData.length;
		 myTotalTcpLength = getTCPHeaderLength()+theData.length;
		 myIPPacket.setTotaIPLength(myTotalTcpLength+myIPPacket.getIPHeaderLength());
		
	}
	
	protected byte[] _tcpOptions = new byte[0];
	
	/**
	 * set TCP header options
	 * @param theOptions - options byte array.
	 */
	public void setTCPHeaderOptions(byte[] theOptions)
	{
		if(theOptions == null)
			return;
		
		_tcpOptions = theOptions;
		updateTCPHeaderLength();
		myTotalTcpLength = getTCPHeaderLength()+getPayloadDataLength();
		myIPPacket.setTotaIPLength(myTotalTcpLength+myIPPacket.getIPHeaderLength());
	}
	

	/**
	 * 
	 * @return TCP options if exists or empty array.
	 */
	public byte[] getTcpOptions()
	{
		if (_tcpOptions.length == 0 && myIPPacket._isSniffedPkt && myTcpHdrLh > DEFAULT_TCP_HDR_LENGTH)
		{
			_tcpOptions = ByteUtils.extractBytesArray(myIPPacket.myPacket, _tcpOffset + DEFAULT_TCP_HDR_LENGTH, myTcpHdrLh-DEFAULT_TCP_HDR_LENGTH);
		}
		return _tcpOptions;
	}
	
	/**
	 * for internal use.
	 * When use add options we should update header length.
	 */
	private void updateTCPHeaderLength()
	{
		myTcpHdrLh = DEFAULT_TCP_HDR_LENGTH+_tcpOptions.length;
	}
	
	/**
	 * 
	 * @return the total tcp part length.
	 */
	public int getTotalTCPPlength()
	{
		return myTotalTcpLength;
	}
	
	/**
	 * @return the pseudo header for the packet.
	 */
	protected byte[] buildPseudoHeader()
	{
		if (myIPPacket.isIPv4())
		{
			byte[] _pseudo_header = new byte[PSEUDO_HDR_LEN];
			ByteUtils.setBigIndianInBytesArray(_pseudo_header,
					PSEUDO_IP_SRC_POS,  ((IPv4Packet)myIPPacket).getSourceIPv4().getIPasLong(), 4);
			ByteUtils.setBigIndianInBytesArray(_pseudo_header,
					PSEUDO_IP_DST_POS, ((IPv4Packet)myIPPacket).getDestinationIPv4().getIPasLong(), 4);
			_pseudo_header[PSEUDO_ZERO_POS] = 0;
			_pseudo_header[PSEUDO_PROTO_CODE_POS] = IPPacketType.TCP;
			ByteUtils.setBigIndianInBytesArray(_pseudo_header,
					PSEUDO_UDP_LENGTH_POS, getTotalTCPPlength(), 2);
			return _pseudo_header;
		}
		throw new UnsupportedOperationException("Not supported yet");
	}

	public int getTCPChksum()
	{
		byte[] _pseudo_header = buildPseudoHeader();

		long sum = 0;

		// first run on the pseudo header 
		for (int i = 0; i < _pseudo_header.length; i += 2)
		{
			int byte1Val = _pseudo_header[i] & 0xff;
			int byte2Val = (i + 1 < _pseudo_header.length) ? _pseudo_header[i + 1] & 0xff : 0;
			sum = sum + ((byte1Val << 8) + byte2Val);
		}

		// run on the tcp part
		for (int i = EthernetFrame.ETHERNET_HEADER_LENGTH+myIPPacket.getIPHeaderLength(); i < myIPPacket.myPacket.length; i += 2)
		{
			int byte1Val = myIPPacket.myPacket[i] & 0xff;
			int byte2Val = (i + 1 < myIPPacket.myPacket.length) ? myIPPacket.myPacket[i + 1] & 0xff : 0;
			sum = sum + ((byte1Val << 8) + byte2Val);
		}

		while(sum >> 16 > 0)
		{
			sum = (sum >> 16) + (sum & 0xffff);
		}
		//sum = (sum >> 16) + (sum & 0xffff);
		//sum = sum + (sum >> 16);
		sum = ~sum & 0xffff;

		return (int)sum;
	}
	

	/**
	 * @return the packet as a readable string.
	 */
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("TCP : src ip[");
		buf.append(myIPPacket.getSourceIPAsString());
		buf.append(":" + getSourcePort());
		buf.append("] , dst ip [");
		buf.append(myIPPacket.getDestinationIPAsString());
		buf.append(":" + getDestinationPort() + "]" );
		buf.append( new ByteUtils().getAsString(myIPPacket.getIPData()));

		return buf.toString();
	}
	
	/**
	 * @return the packet as a readable string.
	 */
	public String toHex()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("TCP : src [");
		buf.append(myIPPacket.getSourceIPAsString());
		buf.append(":" + getSourcePort());
		buf.append("] , ip [");
		buf.append(myIPPacket.getDestinationIPAsString());
		buf.append(":" + getDestinationPort() + "]" );
		buf.append( new ByteUtils().getAsString(myIPPacket.getIPData()));

		return buf.toString();
	}
	
	public void createPacketBytes()
	{
		myIPPacket.setIPProtocol(IPPacketType.TCP);
		int len = getPayloadDataLength()+getTCPHeaderLength();
		myIPPacket.setData(new byte[len]);
		myIPPacket.createIPPacketBytes(len);
		// now put the TCP stuff.
		 
		atuoComplete();
		int pos = EthernetFrame.ETHERNET_HEADER_LENGTH+ myIPPacket.getIPHeaderLength();
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, mySrcPort, 2);
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, myDstPort, 2);
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getSequenceNumber(), 4);
		pos+=4;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getAcknowledgmentNumber(), 4);
		pos+=4;
		int n = ((getTCPHeaderLength()/4) << 12) | (getAllFlags() );
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, n, 2);
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getWindowSize(), 2);
		pos+=2;
		int chksumPos = pos;
		pos+=2;
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, pos, getUrgentPointer(), 2);
		pos+=2;
		
		// if we have tcp options configured we copy it.
		if (_tcpOptions != null)
		{
			System.arraycopy(_tcpOptions, 0, myIPPacket.myPacket,pos ,_tcpOptions.length);
			pos+=+_tcpOptions.length;
		}
		
		if(_tcpDataBytes != null)
		{
			pos = EthernetFrame.ETHERNET_HEADER_LENGTH+myIPPacket.getIPHeaderLength()+getTCPHeaderLength();
			System.arraycopy(_tcpDataBytes, 0, myIPPacket.myPacket,pos ,_tcpDataBytes.length);
		}
		
		ByteUtils.setBigIndianInBytesArray(myIPPacket.myPacket, chksumPos, getTCPChksum(), 2);
		
	}

	@Override
	public boolean isIPv4()
	{
		return myIPPacket.isIPv4();
	}

	@Override
	public IPPacketBase getUnderlyingIPPacketBase()
	{
		return myIPPacket;
	}

	@Override
	public byte[] getRawBytes() throws NetUtilsException
	{
		return myIPPacket.getRawBytes();
	}
	
	public void toReadableText(StringBuffer sb)
	{
		sb.append("TCP\n");
		sb.append("Source Port : "+getSourcePort());
		sb.append("\n");
		sb.append("Destination Port : "+getDestinationPort());
		sb.append("\n");
		sb.append("Sequence : "+getSequenceNumber());
		sb.append("\n");
		sb.append("Acknowledgemet : "+getAcknowledgmentNumber());
		sb.append("\n");
		sb.append("Header Length: "+getTCPHeaderLength());
		sb.append("\n");
		sb.append("SYN : "+isSyn());
		sb.append("\n");
		sb.append("ACK : "+isAck());
		sb.append("\n");
		sb.append("RST : "+isRst());
		sb.append("\n");
		sb.append("FIN : "+isFin());
		sb.append("\n");
		sb.append("Window Size: "+getWindowSize());
		sb.append("\n");
		sb.append("Checksum: "+getTCPChecksum());
		sb.append("\n");
	}
}
