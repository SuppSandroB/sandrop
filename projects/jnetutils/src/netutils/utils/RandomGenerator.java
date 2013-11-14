package netutils.utils;

import java.util.Random;

public class RandomGenerator
{
	private static final int INT16_MAX = 0xffff;
	
    private static Random myRand = null;
	
	/**
	 * generate random int for creating sequance number.
	 * @return
	 */
	public static  int getRandInt()
	{
		if (myRand == null)
		{
			myRand = new Random(System.currentTimeMillis());
		}
		
		return myRand.nextInt(INT16_MAX);
	}
}
