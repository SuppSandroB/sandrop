package netutils;

/**
 * General exception used in this package.<br>
 * Exception also used by the c implementation in case of fatal error.<br>
 * 
 * @author roni bar yanai
 *
 */
public class NetUtilsException extends Exception
{

	/**
	 * 
	 */
	public NetUtilsException()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param theMessage
	 * @param theCause
	 */
	public NetUtilsException(String theMessage, Throwable theCause)
	{
		super(theMessage, theCause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param theMessage
	 */
	public NetUtilsException(String theMessage)
	{
		super(theMessage);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param theCause
	 */
	public NetUtilsException(Throwable theCause)
	{
		super(theCause);
		// TODO Auto-generated constructor stub
	}

}
