package netutils.utils;

/**
 * Class for easy time handling.
 * 
 * 
 * @author roni bar yanai
 *
 */
public class StopTimer
{
	private static final int MSEC = 1000;
	
	
	private long myStartTime = 0;
	
	/**
	 * remember current time as start.
	 */
	public StopTimer()
	{
		startTime();
	}
	
	
	/**
	 * remember current time as start.
	 */
	public void startTime()
	{
		myStartTime = System.currentTimeMillis();
	}
	
	/**
	 * print current time to std output.
	 */
	public void showTimeToScreen()
	{
		long diff = System.currentTimeMillis() - myStartTime;
		
		if (diff/MSEC > 0)
		{
			System.out.println("Time: "+(diff/MSEC)+" Seconds");
		} 
		else 
		{
			System.out.println("Time: "+diff+" Mili Seconds");
		}
	}

}
