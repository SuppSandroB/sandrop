package netutils.parse;

import netutils.NetUtilsException;
import netutils.utils.ByteUtils;

/**
 * Class for parsing/building ICMP packet.<br>
 * 
 * @author roni bar-yanai
 *
 */
public class ICMPPacket extends IPPacket
{
	/**
	 * 
	 */
	public static final int CALCULATE_ICMP_CHECKSUM = 0;
	
	/*
	 * ICMP header structure: 
	 * 
	 * 0-7    : type
	 * 7 - 16 : code
	 * 17 -32 : checksum
	 * 
	 *  0                32
	 *  ------------------------------------------
	 * |  ICMP header    |   pay load       
	 *  ------------------------------------------
	 */ 
	
	
	// header offset constants.
	private static final int ICMP_HER_LENGTH = 4;
	private static final int ICMP_TYPE_OFFSET = 0;
	private static final int ICMP_TYPE_LENGTH = 1;
	
	private static final int ICMP_CODE_OFFSET = 1;
	private static final int ICMP_CODE_LENGTH = 1;
	
	private static final int ICMP_CHKSUM_OFFSET = 2;
	private static final int ICMP_CHKSUM_LENGTH = 2;
	
	private int myType = 0;
	private int myCode = 0;
	private int myCheckSum = 0;
	
	
	private byte[]  myData = null;
	
	private int myIcmpOffset = 0;
	
	// Holds the IP part.
	private IPv4Packet myIPPacket = null;
	
	/**
	 * create new empty icmp packet.
	 */
	public ICMPPacket()
	{
		myIPPacket = new IPv4Packet();
		myIPPacket.setIPProtocol(IPPacketType.ICMP);
	}
	
	/**
	 * create icmp packet from byte array.
	 * @param thePacket - byte array that contains icmp packet.
	 */
	public ICMPPacket(byte[] thePacket)
	{
		myIPPacket = new IPv4Packet(thePacket);
		myIcmpOffset = myIPPacket.myIPDataOffset;
	}
	
	
	boolean _isReadType = false;
	
	/**
	 * @return the icmp packet type as int.
	 * @see ICMPPacketType
	 */
	public int getICMPType()
	{
		if (_isReadType == false)
		{
			_isReadType = true;
			myType = ByteUtils.getByteNetOrderTo_uint8(myIPPacket.myPacket,myIcmpOffset);
		}
		return myType;
	}
	
	/**
	 * Set the ICMP type.
	 * (8 bit unsinged int)
	 */
	public void setICMPType(int type)
	{
		myType = type;		
	}
	
	boolean _isReadCode = false;
	
	/**
	 * @return the icmp packet code
	 */
	public int getICMPCode()
	{
		if (_isReadCode == false)
		{
			_isReadCode = true;
			myCode = ByteUtils.getByteNetOrderTo_uint8(myIPPacket.myPacket,myIcmpOffset+ICMP_CODE_OFFSET);
		}
		return myCode;
	}

	/**
	 * set the icmp code value
	 * @param code - 8bits unsinged int.
	 */
	public void setICMPCode(int code)
	{
		myCode = code;
	}
	
	boolean _isReadICMPChksum = false;
	
	/**
	 * 
	 * @return the checksum field as int.
	 */
	public int getICMPChksum()
	{
		if (_isReadICMPChksum == false )
		{
			_isReadICMPChksum = true;
			myCheckSum = ByteUtils.getByteNetOrderTo_uint16(myIPPacket.myPacket,myIcmpOffset+ICMP_CHKSUM_OFFSET);
		}
		return myCheckSum;
	}
	
	boolean _isWriteChkSum = false;
	
	/**
	 * set the checksum of the icmp pakcet.
	 * CALCULATE_ICMP_CHECKSUM - for auto checksum generation
	 * @param checksum
	 */
	public void setChkSum(int checksum)
	{
		_isWriteChkSum = true;
		myCheckSum = checksum;
	}
	
	/**
	 * @return the icmp payload.
	 */
    public byte[] getICMPData()
    {
    	if (myData == null && myIPPacket._isSniffedPkt)
    	{
    		myData = new byte[myIPPacket.myPacket.length-(myIcmpOffset+ICMP_HER_LENGTH)];
    		System.arraycopy(myIPPacket.myPacket,myIcmpOffset+ICMP_HER_LENGTH,myData,0,myData.length);
    	}
    	return myData;
   }
	
    
   /**
    * Set icmp packet payload. 
    * @param data
    */
   public void setICMPData(byte[] data)
   {
	   myData = data;
	   //myIPPacket.setData(data);
   }
   
   /**
    * 
    * @return total packet length (without ip header). 
    */
   public int getICMPLength()
   {
	   if (myData != null)
	   {
		   return myData.length + ICMP_HER_LENGTH;
	   }
	   else
		   return ICMP_HER_LENGTH;
   }
   
   
   
   /**
    * @return the icmp packet as a byte array.
    */
   public byte[] getIPData()
   {
	   return getICMPRawBytes();
   }



   /**
    * @return total icmp packet length+the ip header length.
    */
   public int getIpPktTotalLength()
   {
	   return myIPPacket.getIPHeaderLength()+getICMPLength();
   }

   /**
    * build the icmp packet as byte array ready for injection.
    * (without underlying protocols)
    * @return the icmp packet.
    */
   public byte[] getICMPRawBytes()
   {
	   byte[] toReturn = new byte[getICMPLength()];
	   
	   ByteUtils.setBigIndianInBytesArray(toReturn,ICMP_TYPE_OFFSET,myType,ICMP_TYPE_LENGTH);
	   ByteUtils.setBigIndianInBytesArray(toReturn,ICMP_CODE_OFFSET,myType,ICMP_CODE_LENGTH);
	   
	   byte[] data = getICMPData();
	   if (data == null)
		   data = new byte[]{};
	   
	   System.arraycopy(data,0,toReturn,ICMP_HER_LENGTH,data.length);
	   
	   if (myCheckSum == IPv4Packet.CALCULATE_CHKSUM || _isWriteChkSum == false)
	   {
		   myCheckSum = getICMPChksum(toReturn);
	   }
	   
	   ByteUtils.setBigIndianInBytesArray(toReturn,ICMP_CHKSUM_OFFSET,myCheckSum,ICMP_CHKSUM_LENGTH);
	   
	   return toReturn;
   }
   
   	/**
   	 * for internal use. calculate the checksum
   	 * @param icmpPacket
   	 * @return checksum
   	 */
	protected int getICMPChksum(byte[] icmpPacket)
	{
		int sum = 0;

		// run on the udp part
		for (int i = 0; i < icmpPacket.length; i += 2)
		{
			int byte1Val = icmpPacket[i] & 0xff;
			int byte2Val = (i + 1 < icmpPacket.length) ? icmpPacket[i + 1] & 0xff : 0;
			sum = sum + ((byte1Val << 8) + byte2Val);
		}

		sum = (sum >> 16) + (sum & 0xffff);
		sum = sum + (sum >> 16);
		sum = ~sum & 0xffff;
		return sum;
	}
	
	/**
	 * 
	 * @return the underlying IPv4 packet.
	 */
	public IPv4Packet getIpv4Packet()
	{
		return myIPPacket;
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

	public void setSrcIp(long theIp)
	{
		myIPPacket.setSrcIp(theIp);
		
	}
	
	public void setDstIp(long theIp)
	{
		myIPPacket.setDstIp(theIp);
	}
	
	public void createPacketBytes()
	{
		myIPPacket.setIPProtocol(IPPacketType.ICMP);
		int len = getICMPLength();
		byte data[] = new byte[len];
		myIPPacket.setData(data);
		myIPPacket.createIPPacketBytes(len);
		// now put the TCP stuff.
		 
		
		int pos =EthernetFrame.ETHERNET_HEADER_LENGTH + myIPPacket.getIPHeaderLength();
		pos = 0;
		ByteUtils.setBigIndianInBytesArray(data, pos, myType, 1);
		pos+=1;
		ByteUtils.setBigIndianInBytesArray(data, pos, myCode, 1);
		pos+=1;
				
		int chesumPos = pos;
		pos+=2;
		if(myData  != null)
		{
			//pos = EthernetFrame.ETHERNET_HEADER_LENGTH + myIPPacket.getIPHeaderLength()+ 4;
			System.arraycopy(myData, 0, data,pos ,myData.length);
		}
		
		ByteUtils.setBigIndianInBytesArray(data, chesumPos, getICMPChksum(data), 2);
		
	}
	
}
