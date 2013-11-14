package netutils.build;

import netutils.NetUtilsException;
import netutils.parse.*;
import netutils.utils.RangeValidator;

/**
 * TCP Layer builder.
 * 
 * Not all TCP header fields are configurable, for example, length, checksum, and 
 *  other fields that could be calculated can not be set.
 *  
 * The class define only the L4 part and is used with L2,Ethernet frame , and L3
 *  such as IPv4 and IPv6
 *  
 * 
 * 
 * 
 * @author roni bar-yanai
 *
 */
public class TCPPacketBuilder implements L4Builder
{
	private static final int TCP_B_DEFAULT_WINDOW_SIZE = 0xffff;
	
	
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
	 */
	
	// configurable fields
	private int mySrcPort = 0;
	private int myDstPort = 0;

	private long mySeqNum = 0;
	private long myAckNum = 0;

	private boolean isURGFlag = false;
	private boolean isACKFlag = false;
	private boolean isPSHFlag = false;
	private boolean isRSTFlag = false;
	private boolean isSYNFlag = false;
	private boolean isFINFlag = false;

	private int myWindowSize = TCP_B_DEFAULT_WINDOW_SIZE;

	// the TCP payload
	private byte myPayload[] = null;

	// reference to upper layer
	private L3Builder myL3 = null;

	// track user changes for better performance.
	private boolean _isSrcPortDirty = false;
	private boolean _isDstPortDitry = false;
	private boolean _isSeqNumDirty = false;
	private boolean _isAckNumDirty = false;
	private boolean _isFlagsDirty = false;
	private boolean _isWindowSizeDirty = false;
	private boolean _isPayloadDirty = false;
	

	private byte myOptions[] = null;
	
	
	public TCPPacketBuilder()
	{}
	
	public TCPPacketBuilder(TCPPacket tcppkt)
	{
		mySrcPort = tcppkt.getSourcePort();
		myDstPort = tcppkt.getDestinationPort();

		mySeqNum = tcppkt.getSequenceNumber();
		myAckNum = tcppkt.getAcknowledgmentNumber(); 
			
		myWindowSize = tcppkt.getWindowSize();
		
		myPayload = tcppkt.getTCPData();
		setFlags(tcppkt.getAllFlags());
		
	    L3Builder l3 = null;
		
		if(tcppkt.getUnderlyingIPPacketBase().isIPv4())
		{
			l3 = new IPv4PacketBuilder((IPv4Packet)tcppkt.getUnderlyingIPPacketBase());
			l3.addL4Buider(this);
		} 
		else
		{
			throw new UnsupportedOperationException("Not supported yet");
		}
	}
	
	@Override
	public L4Type getType()
	{
		return L4Type.TCP;
	}

	/**
	 * 
	 * @return source port.
	 */
	public int getSrcPort()
	{
		return mySrcPort;
	}

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

	/**
	 * set destination port (uint16)
	 * @param theDstPort
	 */
	public void setDstPort(int theDstPort)
	{
		RangeValidator.checkRangeUint16(theDstPort);
		myDstPort = theDstPort;
		_isDstPortDitry = true;
	}

	/**
	 * 
	 * @return tcp sequence field
	 */
	public long getSeqNum()
	{
		return mySeqNum;
	}

	/**
	 * set sequence number (uint32).
	 * @param theSeqNum
	 */
	public void setSeqNum(long theSeqNum)
	{
		RangeValidator.checkRangeUint32(theSeqNum);
		mySeqNum = theSeqNum;
		_isSeqNumDirty = true;
	}

	/**
	 * 
	 * @return TCP acknowledge number
	 */
	public long getAckNum()
	{
		return myAckNum;
	}

	/**
	 * set acknowledge number (uint32)
	 * @param theAckNum
	 */
	public void setAckNum(long theAckNum)
	{
		RangeValidator.checkRangeUint32(theAckNum);
		myAckNum = theAckNum;
		_isAckNumDirty = true;
	}

	/**
	 * 
	 * @return URG flag
	 */
	public boolean isURGFlag()
	{
		return isURGFlag;
	}

	/**
	 * set URG flag
	 * @param theIsURGFlag
	 */
	public void setURGFlag(boolean theIsURGFlag)
	{
		isURGFlag = theIsURGFlag;
		_isFlagsDirty = true;
	}

	/**
	 * 
	 * @return ACK flag
	 */
	public boolean isACKFlag()
	{
		return isACKFlag;
	}

	/**
	 * set ACK flag
	 * @param theIsACKFlag
	 */
	public void setACKFlag(boolean theIsACKFlag)
	{
		isACKFlag = theIsACKFlag;
		_isFlagsDirty = true;
	}

	/**
	 * 
	 * @return PSH flag
	 */
	public boolean isPSHFlag()
	{
		return isPSHFlag;
	}

	/**
	 * set PSH flag
	 * @param theIsPSHFlag
	 */
	public void setPSHFlag(boolean theIsPSHFlag)
	{
		isPSHFlag = theIsPSHFlag;
		_isFlagsDirty = true;
	}

	/**
	 * 
	 * @return RST flag
	 */
	public boolean isRSTFlag()
	{
		return isRSTFlag;
	}

	/**
	 * set RST flag
	 * @param theIsRSTFlag
	 */
	public void setRSTFlag(boolean theIsRSTFlag)
	{
		isRSTFlag = theIsRSTFlag;
		_isFlagsDirty = true;
	}

	/**
	 * 
	 * @return SYN flag
	 */
	public boolean isSYNFlag()
	{
		return isSYNFlag;
	}

	/**
	 * Set SYN flag
	 * @param theIsSYNFlag
	 */
	public void setSYNFlag(boolean theIsSYNFlag)
	{
		isSYNFlag = theIsSYNFlag;
		_isFlagsDirty = true;
	}

	/**
	 * 
	 * @return FIN flag
	 */
	public boolean isFINFlag()
	{
		return isFINFlag;
	}

	/**
	 * set FIN flag
	 * @param theIsFINFlag
	 */
	public void setFINFlag(boolean theIsFINFlag)
	{
		isFINFlag = theIsFINFlag;
		_isFlagsDirty = true;
	}

	/**
	 * 
	 * @return TCP window size
	 */
	public int getWindowSize()
	{
		return myWindowSize;
	}

	/**
	 * set window size (uint16)
	 * @param theWindowSize
	 */
	public void setWindowSize(int theWindowSize)
	{
		RangeValidator.checkRangeUint16(theWindowSize);
		myWindowSize = theWindowSize;
		_isWindowSizeDirty = true;
	}

	/**
	 * set payload.
	 *  (no size is validated)
	 * @param data
	 */
	public void setPayload(byte data[])
	{
		myPayload = data;
		_isPayloadDirty = true;
	}

	/**
	 * set all flags (6 bits)
	 * @param theAllFlags
	 */
	public void setFlags(int theAllFlags)
	{
		RangeValidator.checkRangeBits(theAllFlags, 6);
		setACKFlag((theAllFlags & TCPPacket.TCP_ACK_MASK) != 0);
		setURGFlag((theAllFlags & TCPPacket.TCP_URG_MASK) != 0);

		setPSHFlag((theAllFlags & TCPPacket.TCP_PSH_MASK) != 0);
		setRSTFlag((theAllFlags & TCPPacket.TCP_RST_MASK) != 0);
		setSYNFlag((theAllFlags & TCPPacket.TCP_SYN_MASK) != 0);
		setFINFlag((theAllFlags & TCPPacket.TCP_FIN_MASK) != 0);
	}

	@Override
	public void setL3(L3Builder theL3)
	{
		myL3 = theL3;

	}

	/**
	 * 
	 * @return true if one of the fields have changed since last time.
	 */
	protected boolean isDirty()
	{
		return (_isSrcPortDirty || _isDstPortDitry || _isSeqNumDirty || _isAckNumDirty || _isFlagsDirty || _isWindowSizeDirty || _isPayloadDirty);
	}

	/**
	 * 
	 * @param bf
	 * @return true if all mandatory fields are defined
	 */
	protected boolean sanityCheck(StringBuffer bf)
	{
		if (myL3 == null)
		{
			bf.append("No Layer 3 added");
			return false;
		}
		return myL3.sanityCheck(bf);
	}

	/**
	 * 
	 * @return TCP Packet
	 * @throws NetUtilsException 
	 */
	public TCPPacket createTCPPacket() throws NetUtilsException
	{
		StringBuffer bf = new StringBuffer();
		if (!sanityCheck(bf))
		{
			throw new NetUtilsException(bf.toString());
		}
		TCPPacket toRet = null;

		// setting matching underlying layer
		switch (myL3.getType())
		{
		case IPv4:

			toRet = new TCPPacketIpv4();
			break;

		case IPv6:

			toRet = new TCPPacketIpv6();
			break;
		default:
			throw new UnsupportedOperationException();
		}

		// copy data to packet.
		toRet.setAckNum(myAckNum);
		toRet.setAck(isACKFlag);
		toRet.setDestinationPort(myDstPort);
		toRet.setFin(isFINFlag);
		toRet.setPsh(isPSHFlag);
		toRet.setRst(isRSTFlag);
		toRet.setSequenceNum(mySeqNum);
		toRet.setSourcePort(mySrcPort);
		toRet.setSyn(isSYNFlag);
		toRet.setUrg(isURGFlag);
		toRet.setWindowSize(myWindowSize);
		toRet.setData(myPayload);
		toRet.setTCPHeaderOptions(myOptions);

		// call upper layer to add info
		myL3.addInfo(toRet);

		toRet.createPacketBytes();

		return toRet;
	}

	/**
	 * 
	 * @return TCP options array
	 */
	public byte[] getOptions()
	{
		return myOptions;
	}

	/**
	 * set TCP options
	 * @param theOptions
	 */
	public void setOptions(byte[] theOptions)
	{
		myOptions = theOptions;
	}

}
