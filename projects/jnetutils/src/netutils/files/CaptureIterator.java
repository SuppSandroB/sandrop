package netutils.files;

import java.io.IOException;
import java.util.Iterator;

/**
 * The class implements iterator for capture file.  
 * 
 * @author roni bar yanai
 *
 */
public class CaptureIterator implements Iterator<byte[]>
{
	private CaptureFileReader myCaptureReader = null;
	
	private byte myNext[] = null;
	
	/**
	 * create iterator
	 * @param rd 
	 * @throws IOException
	 */
	public CaptureIterator(CaptureFileReader rd) throws IOException
	{
		myCaptureReader = rd;
		loadNext();
	}
	
	private long myLastTimestamp = 0;

	private void loadNext() throws IOException
	{
		myNext = myCaptureReader.ReadNextPacket();	
		myLastTimestamp = myCaptureReader.getTimeStamp();
	}
	
	public long getLastTimestamp()
	{
		return myLastTimestamp;
	}
	
	public boolean hasNext()
	{
		return myNext != null;
	}

	@Override
	public byte[] next() {
		byte data[] = myNext;
		try
		{
			loadNext();
		} catch (IOException e)
		{
			// we can't throw the excpetion, so
			// we must at least print it.
			e.printStackTrace();
		}
		return data;
	}
	
	

	@Override
	public void remove() {
				
	}
}
