package de.sloc.proto;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.sloc.dataformat.AsNumber;
import de.sloc.dataformat.UnsignedInteger;

public class ConvertibleInetAddress implements AsNumber
{
    protected InetAddress inetAddress;

    public ConvertibleInetAddress(InetAddress inetAddress)
    {
        this.inetAddress = inetAddress;
    }

    protected ConvertibleInetAddress()
    {

    }

    @Override
    public Number getNumberValue()
    {
        byte[] addressBytes = inetAddress.getAddress();
        return new BigInteger(addressBytes);
    }

    public static ConvertibleInetAddress getByValue(Number value) throws UnknownHostException
    {
        return new ConvertibleInetAddress(InetAddress.getByAddress(new UnsignedInteger(value, 4 /*
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
    public boolean equals(Object otherConvertibleInetAddressObject)
    {
        if (!(otherConvertibleInetAddressObject instanceof ConvertibleInetAddress))
        {
            return false;
        }

        ConvertibleInetAddress otherConvertibleInetAddress = (ConvertibleInetAddress) otherConvertibleInetAddressObject;

        return this.getInetAddress().equals(otherConvertibleInetAddress.getInetAddress());
    }

    @Override
    public String toString()
    {
        return inetAddress.toString();
    }
}