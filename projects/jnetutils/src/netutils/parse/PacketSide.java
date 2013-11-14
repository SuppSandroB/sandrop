/*
 * Created on Feb 23, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package netutils.parse;


/**
 * Flow side.
 * 
 * Client is defined as the initiating size, regardless if this is 
 *  a server<-->client communication (web for example), or p2p.
 * 
 * @author roni bar yanai
 *
 */
public enum PacketSide
{
	CLIENT_TO_SERVER("Client to Server"),
	SERVER_TO_CLIENT("Server to Client");
    
	private String mySide = null;
    
    private PacketSide(String theSide)
    {
        mySide = theSide;
    }
        
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
       return mySide;
    }
    
    public String toArrow()
    {
    	if (mySide.equals("Client to Server"))
    		return "--->>";
    	
    	return "<<---";
    }
 
}
