package de.sloc.proto;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.sloc.dataformat.AsNumber;
import de.sloc.dataformat.UnsignedInteger;

public class MyInetAddress implements AsNumber
{
	protected InetAddress inetAddress;

	public MyInetAddress(InetAddress inetAddress)
	{
		this.inetAddress = inetAddress;
	}

	protected MyInetAddress()
	{

	}

	@Override
	public Number getNumberValue()
	{
		byte[] addressBytes = inetAddress.getAddress();
		return new BigInteger(addressBytes);
	}

	public static MyInetAddress getByValue(Number value) throws UnknownHostException
	{
		return new MyInetAddress(InetAddress.getByAddress(new UnsignedInteger(value, 4 /*
																						 * TODO
																						 * determine
																						 * protocol
																						 * type
																						 * from
																						 * value
																						 */).getValue()));
	}

	public InetAddress getInetAddress()
	{
		return inetAddress;
	}

	@Override
	public String toString()
	{
		return inetAddress.toString();
	}
}