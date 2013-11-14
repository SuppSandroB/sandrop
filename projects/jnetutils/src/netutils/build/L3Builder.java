package netutils.build;

import netutils.parse.IPPacket;

/**
 * Builder part uses the TCP/IP layers abstraction.
 * Each protocol which belongs to later 3 must extend this class
 *  (IPv6,IPv4, Arp)
 * 
 * @author roni bar-yanai
 *
 */
public abstract class L3Builder
{
	/**
	 * 
	 * @return the type (IPv4,IPv6...etv)
	 */
	public abstract L3Type getType();
	
	/**
	 * add L4 layer.
	 * @param theL4
	 */
	public abstract void addL4Buider(L4Builder theL4);
	/**
	 * used to connect the layers
	 * @param frameBuilder
	 */
	protected abstract void setL2(EthernetFrameBuilder frameBuilder);

	/**
	 * Copy all relevant information into the IPPacket.
	 * @param theToRet
	 */
	protected abstract void addInfo(IPPacket theToRet);

	protected abstract boolean sanityCheck(StringBuffer theBf);
			
}
