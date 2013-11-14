package netutils.parse;

import netutils.NetUtilsException;


/**
 * SCTP packet implementation. The class provides methods for parsing
 * and building SCTP packets.<br>
 * Mainly accessing and setting fields.<br>
 * <br>
 * 
 * 
 *<br>
 */
public class SCTPPacket extends IPPacket
{
	
	/* 
	 * SCTP packet structure:
	 * ---------------------
	 * 
	 * 
	 * 0                                 15 16                                    32
	 * ------------------------------------------------------------------------------
	 * |    16 bit src port                |          16 bit dst port               |
	 * ------------------------------------------------------------------------------
	 * |                32 bit verification tag                                     |
	 * ------------------------------------------------------------------------------
	 * |                32 bit check sum                                            |
	 * ------------------------------------------------------------------------------
	 * 
	 *  SCTP data Chunk
	 * -------------------
	 * 
	 * | 8 bit chunktype | 8 bit chuckflag | 16 bits chunk length                   |                                                               
	 * ------------------------------------------------------------------------------
	 * | 32 bits tsn                                                                |
	 * ------------------------------------------------------------------------------
	 * | 16 bits stream identifier         | 16 bits stream sequence number         |
	 * ------------------------------------------------------------------------------
	 * | 32 bits payload protocol identifier                                        |
	 * ------------------------------------------------------------------------------    
	 * 
	 * SCTP ABORT Chunk
	 * -----------------
	 * 
	 * SCTP HEART_BEAT chunk
	 * ---------------------
	 * 
	 * SCTP HEART_BEAT_ACK chunk
	 * -------------------------
	 * 
	 * SCTP SHUTDOWN Chunk
     * -----------------
     * 
     * SCTP SHUTDOWN_ACK chunk
     * ---------------------
     * 
     * SCTP SHUTDOWN_COMPLETE chunk
     * -------------------------
	 * 
	 * SCTP INIT chunk
     * ---------------------
     * 
     * SCTP INIT_ACK chunk
     * -------------------------
     * 
     * SCTP COOKIE_ECHO chunk
     * ---------------------
     * 
     * SCTP COOKIE_ACK chunk
     * -------------------------
     * 
     * SCTP SACK chunk
     * -------------------------
	 * 
	*/

	public byte[] mySCTPPacket;
	
	/**
	 * Create new empty tcp packet.
	 */
	public SCTPPacket()
	{
		super();
		myIPPacket = new IPv4Packet();
	}

	/**
	 * Create new TCP packet. <br>
	 * @pre the buffer contains valid buffer.
	 * @param thePacket - byte buffer with valid packet (including ip and eth parts).
	 */
	public SCTPPacket(byte[] thePacket)
	{
		myIPPacket = IPFactory.createIPPacket(thePacket);
		 
	}
	
	protected SCTPPacket(boolean isIPv6)
	{
		this();
		if(isIPv6)
		{
			myIPPacket = new IPv6Packet();
			myIPPacket.setIPProtocol(IPPacketType.TCP);
			myIPPacket.setPacketType(EthernetFrame.ETHERNET_IPv6_PKT_TYPE);
		}
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
	
	public void createPacketBytes()
	{
		myIPPacket.setIPProtocol(IPPacketType.SCTP);
		int len = mySCTPPacket.length;
		myIPPacket.setData(mySCTPPacket);
		myIPPacket.createIPPacketBytes(len);
		
		
		if(mySCTPPacket != null)
		{
			int pos = EthernetFrame.ETHERNET_HEADER_LENGTH+myIPPacket.getIPHeaderLength();
			System.arraycopy(mySCTPPacket, 0, myIPPacket.myPacket,pos ,mySCTPPacket.length);
		}

				
	}

	//TODO: real support for SCTP packets.
	/**
	 * for building SCTP packets.
	 * currently no real support in protocol, we just know how to put SCTP 
	 * packet inside an IP packet.
	 * 
	 * 
	 * @param SCTPPacket
	 */
	public void setMySCTPPacket(byte[] SCTPPacket) {
		this.mySCTPPacket = SCTPPacket;
	}
}
