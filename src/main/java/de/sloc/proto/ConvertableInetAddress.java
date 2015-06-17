package de.sloc.proto;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.sloc.dataformat.AsNumber;
import de.sloc.dataformat.UnsignedInteger;

public class ConvertableInetAddress implements AsNumber
{
	protected InetAddress inetAddress;

	public ConvertableInetAddress(InetAddress inetAddress)
	{
		this.inetAddress = inetAddress;
	}

	protected ConvertableInetAddress()
	{

	}

	@Override
	public Number getNumberValue()
	{
		byte[] addressBytes = inetAddress.getAddress();
		return new BigInteger(addressBytes);
	}

	public static ConvertableInetAddress getByValue(Number value) throws UnknownHostException
	{
		return new ConvertableInetAddress(InetAddress.getByAddress(new UnsignedInteger(value, 4 /*
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