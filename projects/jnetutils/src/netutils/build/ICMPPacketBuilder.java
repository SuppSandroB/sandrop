package netutils.build;

import netutils.parse.*;
import netutils.NetUtilsException;
import netutils.utils.RangeValidator;

/**
 * ICMP packet builder.
 * 
 * Defines the ICMP (layer 4) fields. Should be used with l2 and l3 builders.
 * 
 * 
 * @author roni bar-yanai
 *
 */
public class ICMPPacketBuilder implements L4Builder
{
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
	private int myType = 0;
	private int myCode = 0;
	
	
	private byte [] myPayload = new byte[0];
	private L3Builder myL3;
	
	
	@Override
	public L4Type getType()
	{
		return L4Type.ICMP;
	}

	@Override
	public void setL3(L3Builder theL3)
	{
		myL3 = theL3;
	}

	/**
	 * 
	 * @return type field
	 */
	public int getICMPType()
	{
		return myType;
	}

	/**
	 * set type field (uint8)
	 * @param theType
	 */
	public void setType(int theType)
	{
		RangeValidator.checkRangeUint8(theType);
		myType = theType;
	}

	/**
	 * 
	 * @return type field
	 */
	public int getCode()
	{
		return myCode;
	}

	/**
	 * set code field
	 * @param theCode
	 */
	public void setCode(int theCode)
	{
		RangeValidator.checkRangeUint8(theCode);
		myCode = theCode;
	}
	
	
    /**
     * set payload
     * @param thePayload
     */
	public void setPayload(byte[] thePayload)
	{
		RangeValidator.checknull(thePayload);
		myPayload = thePayload;
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
	 * Create ICMP packet 
	 * @return
	 * @throws NetUtilsException
	 */
	public ICMPPacket createICMPPacket() throws NetUtilsException
	{
		StringBuffer bf = new StringBuffer();
		if (!sanityCheck(bf))
		{
			throw new NetUtilsException(bf.toString());
		}		
		
		ICMPPacket toRet = new ICMPPacket();

		// copy data
        toRet.setICMPData(myPayload);
		toRet.setICMPCode(myCode);
        toRet.setICMPType(myType);
        		
        // call upper layer to add information
		myL3.addInfo(toRet);

		toRet.createPacketBytes();

		return toRet;
	}

}
