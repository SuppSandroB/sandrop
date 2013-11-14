package netutils.files;

import java.io.IOException;

import netutils.NetUtilsException;
//import netutils.files.enc.EncFileReader;
//import netutils.files.erf.ERFCapFileReader;
import netutils.files.pcap.PCapFileReader;
import netutils.files.pcap.PCapFileWriter;

/**
 * Factory class for creating capture file reader.
 * The file type is determined by the file name prefix.
 * If the file prefix don't match its prefix the behaviour is unexpected.
 * 
 * @author roni bar-yanai
 *
 */
public class CaptureFileFactory
{
	/**
	 * Create reader for capture file.
	 * @param theFileName
	 * @return CaptureFileReader instance.
	 * @throws IOException
	 * @throws NetUtilsException
	 */
	public static CaptureFileReader createCaptureFileReader(String theFileName) throws IOException, NetUtilsException
	{
		if(theFileName.toLowerCase().endsWith("cap"))
		{
			return new PCapFileReader(theFileName);
		}
//		else if (theFileName.toLowerCase().endsWith("enc"))
//		{
//			return new EncFileReader(theFileName);
//		}
		
		throw new NetUtilsException("Capture Format not supported");
	}
	
	/**
	 * create writer for capture file.
	 * @param theFileName
	 * @return CaptureFileWriter instance
	 * @throws IOException
	 * @throws NetUtilsException
	 */
	public static CaptureFileWriter createCaptureFileWriter(String theFileName) throws IOException, NetUtilsException
	{
		if(theFileName.toLowerCase().endsWith("cap"))
		{
			return new PCapFileWriter(theFileName);
		}
		
		throw new NetUtilsException("Capture Format not supported");
	}
	
	
	public static CaptureFileReader tryToCreateCaprtueFileReader(String theFileName) throws NetUtilsException
	{
		 
		try
		{
			PCapFileReader prd = new PCapFileReader(theFileName);
			if (prd.isValid())
				return prd;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
//		try
//		{
//			EncFileReader erd = new EncFileReader(theFileName);
//			if(erd.isValid())
//				return erd;
//		} catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//		}
		
//		try
//		{
//			ERFCapFileReader erfrd = new ERFCapFileReader(theFileName);
//			
//			return erfrd;
//		}
//		catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//		}
		
		throw new NetUtilsException("Capture Format not supported");
		
	}
}
