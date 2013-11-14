package netutils.files.pcap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import netutils.files.CaptureFileReader;
import netutils.files.CaptureFileValid;

/**
 * class for reading libcap files and returning the packets raw bytes.
 * 
 * @see CaptureFileReader
 * @author roni bar-yanai
 */
public class PCapFileReader implements CaptureFileReader,CaptureFileValid
{
	private static final int MAX_PACKET_SIZE = 65356;
	
	// use to determine if file was recorded on a big indian or a little
	// indian
	protected static final long MAGIC_NUMBER_FLIP = 0xd4c3b2a1L;
	protected static final long MAGIC_NUMBER_DONT_FLIP = 0xa1b2c3d4L;
	
	private String myFileName = null;

	private InputStream myInStrm = null;

	private PCapFileHeader myPcapFileHeader = null;
	
	// holds last read packet libcap pkt hdr;
	private PCapPacketHeader myPHDR = null;
	
	private long myPktCnt = 0; 

	// used to be empty constructor
	// used by the static method for backward computability
	private PCapFileReader()
	{}

	/**
	 * open cap file
	 * @param theFileName
	 * @throws IOException
	 */
	public PCapFileReader(String theFileName) throws IOException
	{
		myFileName = theFileName;
		initStream(theFileName);
	}
	
	/**
	 * open cap from stream
	 * @param theInStream
	 */
	public PCapFileReader(InputStream theInStream)
	{
		initStream(theInStream);
	}

	private boolean _isValid = true;
	
	/**
	 * init input stream according to file name.
	 * read the cap file header so will be ready to read next packet.
	 * @param theFileName
	 * @throws IOException
	 */
	private void initStream(String theFileName) throws IOException
	{
		myInStrm = new FileInputStream(new File(theFileName));
		try
		{
			readHeader(myInStrm);
		}
		catch (Exception e) {
			_isValid = false;
		}
	}
	
	/**
	 * init from providied input stream (usually stdin).
	 * @param theInStream
	 */
	private void initStream(InputStream theInStream)
	{
		myInStrm = theInStream;
		try
		{
			readHeader(myInStrm);
		}
		catch (Exception e) {
			_isValid = false;
		}
	}

	/**
	 * read only the cap header.
	 * @param in
	 * @return
	 * @throws IOException
	 */
	protected PCapFileHeader readHeader(InputStream in) throws IOException
	{
		PCapFileHeader fh = new PCapFileHeader();
		fh.readHeader(in);
		myPcapFileHeader = fh;
		return fh;
	}
	

	/**
	 * return the next packet in the files.
	 * @param in
	 * @return array of bytes.
	 * @throws IOException
	 */
	protected byte[] readNextPacket(InputStream in) throws IOException
	{
		myPHDR = new PCapPacketHeader();
		myPHDR = myPHDR.readNextPcktHeader(in, myPcapFileHeader.isflip());

		if (myPHDR != null)
		{
			if (myPHDR.pktlenUint32 > MAX_PACKET_SIZE)
				throw new IOException("Corrupted file !!! illegal packet size : "+myPHDR.pktlenUint32);
			
			byte[] toReturn = new byte[(int) myPHDR.pktlenUint32];
			if (in.read(toReturn) != toReturn.length)
			{
				throw new IOException("Corrputed file!!!");
			}
			myPktCnt++;
			return toReturn;
		}
		return null;
	}

	/**
	 * return the cap file raw data as array of packets when each packet is
	 * byte array itself.
	 * @param fileName
	 * @return byte[][]
	 * @throws PCapFileException
	 * @throws IOException
	 */
	private byte[][] readAllCapRawData(String fileName) throws PCapFileException
	{
		InputStream in = null;
		try
		{
			in = new FileInputStream(new File(fileName));
			readHeader(in);
			ArrayList<byte[]> tmp = new ArrayList<byte[]>();
			byte[] pkt = null;
			while ((pkt = readNextPacket(in)) != null)
			{
				tmp.add(pkt);
			}
			return (byte[][]) tmp.toArray(new byte[][] { {} });
		}
		catch (Exception ex)
		{
			throw new PCapFileException(ex.toString());
		}
		finally
		{
			if (in != null) try
			{
				in.close();
			}
			catch (IOException e)
			{

			}
		}
	}

	/**
	 * will return only the PcapPktHeaders
	 * @param fileName
	 * @return array of packets headers.
	 * @throws IOException
	 */
	protected PCapPacketHeader[] getAllPktsHeaders(String fileName) throws IOException
	{
		InputStream in = null;
		try
		{
			in = new FileInputStream(new File(fileName));
			readHeader(in);
			ArrayList<PCapPacketHeader> tmp = new ArrayList<PCapPacketHeader>();
			
			PCapPacketHeader ph = new PCapPacketHeader();
			while ((ph = ph.readNextPcktHeader(in, myPcapFileHeader.isflip)) != null)
			{
				tmp.add(ph);
				in.skip(ph.pktlenUint32);
				ph = new PCapPacketHeader();
			}
			return (PCapPacketHeader[]) tmp.toArray(new PCapPacketHeader[] {});
		}
		finally
		{
			if (in != null) in.close();
		}
	}

	/**
	 * @return the next packet in cap file. will return null if no more packets.
	 * @throws IOException
	 */
	public byte[] ReadNextPacket() throws IOException
	{
		if (myInStrm == null)
		{
			initStream(myFileName);
		}
		return readNextPacket(myInStrm);
	}
	
	/**
	 * 
	 * @return next block, return null on end of file
	 * @throws IOException
	 */
	public PCapBlock readNextBlock() throws IOException
	{
		byte[] nextpkt = ReadNextPacket();
		if (nextpkt == null)
		{
			return null;
		}
		return new PCapBlock(myPHDR,nextpkt);
	}

	/**
	 * close the file.
	 *
	 */
	public void close()
	{
		if (myInStrm != null)
		{
			try
			{
				myInStrm.close();
			}
			catch (IOException e)
			{}
			myInStrm = null;
		}
	}
	
	/**
	 * make sure file is closed.
	 */
	protected void finalize() throws Throwable
	{
		close();
	}

	/**
	 * for switching big/small indian
	 * @param num
	 * @return 
	 */
	protected static long pcapflip32(long num)
	{
		long tmp = num;
		tmp = ((tmp & 0x000000FF) << 24) + ((tmp & 0x0000FF00) << 8) + ((tmp & 0x00FF0000) >> 8) + ((tmp & 0xFF000000) >> 24);

		return tmp;
	}

	/**
	 * for switching big/small indian
	 * @param num
	 * @return
	 */
	protected static int pcapflip16(int num)
	{
		int tmp = num;

		tmp = ((tmp & 0x00FF) << 8) + ((tmp & 0xFF00) >> 8);
		return tmp;
	}

	public PCapFileHeader getPcapFileHeader()
	{
		return myPcapFileHeader;
	}

	/**
	 * @param fileName
	 * @return all cap data as byte[][] array of byte arrays.
	 * each byte array is a packet in the cap file (udp,tcp...etc)
	 * @throws PCapFileException
	 */
	public static byte[][] readCapRawData(String fileName) throws PCapFileException
	{
		PCapFileReader rd = new PCapFileReader();
		return rd.readAllCapRawData(fileName);
	}
	
	/**
	 * will return only the PcapPktHeaders
	 * @param fileName
	 * @return array of packets headers.
	 * @throws IOException
	 */
	public static PCapPacketHeader[] getPktsHeaders(String fileName) throws IOException
	{
		PCapFileReader rd = new PCapFileReader();
		return rd.getAllPktsHeaders(fileName);
	}
	
	/**
	 * get only the header.
	 * @param theFileName
	 * @return the file header.
	 * @throws IOException
	 */
	public static PCapFileHeader getPcapFileHeader(String theFileName) throws IOException
	{
		PCapFileReader rd = new PCapFileReader(theFileName);
		PCapFileHeader hdr =  rd.getPcapFileHeader();
		rd.close();
		return hdr;
	}

	@Override
	public long getTimeStamp()
	{
		if(myPHDR != null)
			return myPHDR.getTime();
		return 0;
	}

	@Override
	public long getCurrentPacket()
	{
		return myPktCnt;
	}

	@Override
	public boolean isValid()
	{
		return _isValid;
	}


}
