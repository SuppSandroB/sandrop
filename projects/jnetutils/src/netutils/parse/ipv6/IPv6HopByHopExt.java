package netutils.parse.ipv6;

public class IPv6HopByHopExt extends IPv6ExtBasic
{

	public IPv6HopByHopExt()
	{
		setMyOptionData(new byte[6]);
	}
	@Override
	public int getType()
	{
		return IPv6ExtBasic.IPV6_HOP_BY_HOP_TYPE;
	}
}
