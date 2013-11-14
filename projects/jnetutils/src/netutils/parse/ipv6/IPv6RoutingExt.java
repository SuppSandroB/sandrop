package netutils.parse.ipv6;

public class IPv6RoutingExt extends IPv6ExtBasic
{

	public IPv6RoutingExt()
	{
		setMyOptionData(new byte[6]);
	}
	@Override
	public int getType()
	{
		return IPV6_ROUTING_TYPE;
	}

}
