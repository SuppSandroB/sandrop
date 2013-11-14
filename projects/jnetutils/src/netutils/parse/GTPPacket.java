package netutils.parse;

public class GTPPacket
{
	
	private static final int MIN_IP_HDR_LEN = 20;
	private static final int GTP_FLAGS_MASK = 0x5;
	private static final int GTP_HDR_LEN = 8;
	private static final int GTP_HDR_OPTION_LEN = 1;
	private static final int GTP_NEXT_HDR_OPTION_LEN = 2;
	private static final int GTP_NEXT_HDR_OPTION_MASK = 0x2;
	
	public static boolean isGTPPacket(UDPPacket udp)
	{
		byte data[] = udp.getUDPData();
				
		if(data.length == 0)
			return false;
		
		
		int flags = data[0] & GTP_FLAGS_MASK;
		int n = 0;
		while(flags > 0) {
			n = n + flags & 1;
			flags = flags >> 1;
		}
				
		int offset = (n>0?4:0)+GTP_HDR_LEN;//GTP_HDR_LEN + GTP_HDR_OPTION_LEN*n +(GTP_NEXT_HDR_OPTION_LEN*((flags & GTP_NEXT_HDR_OPTION_MASK) >> 1));
		
		
		
		if (data.length < offset + MIN_IP_HDR_LEN)
			return false;
		
		return data[offset] == 0x45;
	}
	
	/**
	 * 
	 * @param flags
	 * @param thePacket - gtp packet (without the udp part.)
	 * @return new packet including place for the flags
	 */
	public static byte[] addOptions(int flags,byte thePacket[])
	{
		if (flags >0x7)
			return thePacket;
		
		int dflags = flags & GTP_FLAGS_MASK;
		int n = 0;
		while(dflags > 0) {
			n = n + (dflags & 1);
			dflags = dflags >> 1;
		}
		
		int length = 4;//n*GTP_HDR_OPTION_LEN+(GTP_NEXT_HDR_OPTION_LEN*((flags & GTP_NEXT_HDR_OPTION_MASK) >> 1));
		
		byte data2[] = new byte[thePacket.length + length];
		System.arraycopy(thePacket, 0, data2, 0, GTP_HDR_LEN);
		System.arraycopy(thePacket, GTP_HDR_LEN, data2, GTP_HDR_LEN+length, thePacket.length - GTP_HDR_LEN);
		data2[0] = (byte) (data2[0] | (byte)flags);
		return data2;
	}
}
