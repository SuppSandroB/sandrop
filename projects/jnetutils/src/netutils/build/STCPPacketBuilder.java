package netutils.build;

import java.io.IOException;

import netutils.files.pcap.PCapFileWriter;
import netutils.NetUtilsException;
import netutils.parse.IPv4Address;
import netutils.parse.SCTPPacket;

/**
 * 
 * @author roni bar yanai
 *
 */
public class STCPPacketBuilder implements L4Builder
{

	// reference to upper layer
	private L3Builder myL3 = null;
	
	private byte[] mySCTPPacket = null;
	
	@Override
	public L4Type getType()
	{
		return L4Type.SCTP;
	}

	@Override
	public void setL3(L3Builder theL3)
	{
		myL3 = theL3;			
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
		
		if(mySCTPPacket == null)
		{
			bf.append("no sctp packet were set");
			return false;
		}
		
		return myL3.sanityCheck(bf);
	}
	
	/**
	 * 
	 * @return TCP Packet
	 * @throws NetUtilsException 
	 */
	public SCTPPacket createSCTPPacket() throws NetUtilsException
	{
		StringBuffer bf = new StringBuffer();
		if (!sanityCheck(bf))
		{
			throw new NetUtilsException(bf.toString());
		}
		SCTPPacket toRet = null;

		// setting matching underlying layer
		switch (myL3.getType())
		{
		case IPv4:

			toRet = new SCTPPacket();
			break;

		default:
			throw new UnsupportedOperationException();
		}
	
		toRet.setMySCTPPacket(mySCTPPacket);
		
		// call upper layer to add info
		myL3.addInfo(toRet);

		toRet.createPacketBytes();

		return toRet;
	}

	public byte[] getSCTPPacket() {
		return mySCTPPacket;
	}

	public void setSCTPPacket(byte[] SCTPPacket) {
		this.mySCTPPacket = SCTPPacket;
	}
	
	public static void main(String[] args) throws NetUtilsException, IOException
	{
		IPv4PacketBuilder pb = new IPv4PacketBuilder();
		pb.setDstAddr(new IPv4Address("10.0.0.1"));
		pb.setSrcAddr(new IPv4Address("10.0.0.2"));
		
		STCPPacketBuilder sb = new STCPPacketBuilder();
		sb.setL3(pb);
		
		sb.setSCTPPacket(new byte[]{0xb,0x59,0xb,0x59,0,0,0,0,(byte) 0x90,(byte) 0x83,
				                    0x7f,(byte) 0xdf,0x1,0,0,0x24,(byte) 0x9a,(byte) 0xd2,(byte) 0x94,0x6f,0x0,0,(byte) 0xf4,
				                    0,0,0xa,(byte) 0xff,(byte) 0xff,0x66,0x23,(byte) 0x9d,(byte) 0xb4,0x00,0x0c,0x00,0x08,0x00,0x6
				                    ,00,0x5,(byte) 0x80,0x0,0x0,0x4,(byte) 0xc0,0x0,0x0,0x4});
		
		
		byte arr[] = sb.createSCTPPacket().getRawBytes();
		
		PCapFileWriter wr = new PCapFileWriter("/home/rbaryanai/gsctp.pcap");
		wr.addPacket(arr);
		wr.close();
	}

}
