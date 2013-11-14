package netutils.files;

import java.io.IOException;

/**
 * Interface for reading capture file.<br>
 * There are many formats for capture files which may require
 * different handling, but this difference is not relevant when reading
 * a capture file for analyzing it packets.<br>
 * The interface provides the required abstraction.<br>
 * 
 * 
 * @author roni bar yanai
 *
 */
public interface CaptureFileReader
{
	/**
	 * read next packet in file.
	 * @return next packet in file as a raw byte array, if no more packets are 
	 *    available then will return null.
	 * @throws IOException
	 */
	public byte[] ReadNextPacket() throws IOException;
	
	/**
	 * 
	 * @return the last packet read timestamp
	 */
	public long getTimeStamp();
	
	/**
	 * 
	 * @return the last packet read number.
	 */
	public long getCurrentPacket();
}
