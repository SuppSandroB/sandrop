package netutils.parse.ipv6;

public interface IPv6Extension
{
	public byte[] getAsRawByArray();
	
	public int getLength();
	
	public int getNextType();
	
	public void setNextType(int type);

	public int getType();
}
