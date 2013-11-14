package netutils.parse;

import netutils.NetUtilsException;

/**
 * Common for all IP packets, so they can be treated in object oriented 
 * manner. For exmaple, TCP,UDP and ICMP are all overIP
 * 
 * @author roni bar-yanai
 *
 */
public abstract class IPPacket
{
	// holds the IP packet part, that can be both IPv4 and IPv4
	protected IPPacketBase myIPPacket = null;
	
	public abstract boolean isIPv4();
	
	/**
	 * @return the underlying IP packet (could be IPv4 or IPv6)
	 */
	public IPPacketBase getUnderlyingIPPacketBase()
	{
		return myIPPacket;
	}

	/**
	 * 
	 * @return the packet as a raw byte array.
	 * @throws NetUtilsException 
	 */
	public byte[] getRawBytes() throws NetUtilsException
	{
		return myIPPacket.getRawBytes();
	}
	
	/**
	 * @return the destination mac address.
	 * may return null
	 */
	public byte[] getDstMacByteArray()
	{
		return myIPPacket.getDstMacByteArray();
	}
	
	/**
	 * set the destination mac address.
	 * @param mac
	 */
	public void setDstMacAddress(byte[] mac)
	{
		myIPPacket.setDstMacAddress(mac);
	}
	
	/**
	 * @return the source mac address
	 */
	public byte[] getSrcMacByteArray()
	{
		return myIPPacket.getSrcMacByteArray();
	}
	
	/**
	 * Set the src mac address.
	 * @param mac
	 */
	public void setSrcMacAddress(byte[] mac)
	{
		myIPPacket.setSrcMacAddress(mac);
	}
}
