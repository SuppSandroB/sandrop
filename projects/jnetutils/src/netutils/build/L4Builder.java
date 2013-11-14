package netutils.build;

/**
 * All L4 builders must implement this interface.
 * 
 * 
 * @author roni bar-yanai
 *
 */
public interface L4Builder
{
	/**
	 * 
	 * @return 
	 */
	public L4Type getType();
	
	/**
	 * used to connect the layers.
	 * @param theL3
	 */
	public void setL3(L3Builder theL3);
}
