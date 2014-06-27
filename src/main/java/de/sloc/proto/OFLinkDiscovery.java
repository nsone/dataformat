package de.sloc.proto;

import java.math.BigInteger;

import javax.xml.bind.DatatypeConverter;

import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUElement.Type;

public class OFLinkDiscovery extends FrameHeader
{
	@PDUElement(order = 1, type = Type.UNSIGNED_INTEGER, length = 8)
	protected BigInteger datapathId;

	@PDUElement(order = 2, type = Type.UNSIGNED_INTEGER, length = 4)
	protected long portNumber;

	@PDUElement(order = 3, type = Type.PADDING, length = 24)
	protected byte[] padding;

	protected static final byte[] DEFAULT_MAC = DatatypeConverter.parseHexBinary("ACDE480FBABE");

	protected OFLinkDiscovery()
	{
	}

	public OFLinkDiscovery(BigInteger datapathId, long portNumber)
	{
		super(DEFAULT_MAC, DEFAULT_MAC, Ethertype.OF_LINK_DISCOVERY);
		this.datapathId = datapathId;
		this.portNumber = portNumber;
	}

	public BigInteger getDatapathId()
	{
		return datapathId;
	}

	public long getPortNumber()
	{
		return portNumber;
	}
}
