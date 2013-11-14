package netutils.parse;

/**
 * Exception thrown when user try to build illegal packet.<br>
 *
 * @author roni bar-yanai
 *
 */
public class IllegalPacketException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4517217012322861127L;

	public IllegalPacketException(String message)
	{
		super(message);
	}
}
