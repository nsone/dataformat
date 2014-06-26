package de.sloc.proto.ethernet;

import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUElement.Type;

public class IPv4TCP extends IPv4
{

	@PDUElement(order = 1, type = Type.UNSIGNED_INTEGER, length = 2)
	protected int sourcePort;

	@PDUElement(order = 2, type = Type.UNSIGNED_INTEGER, length = 2)
	protected int destinationPort;

	@PDUElement(order = 3, type = Type.UNSIGNED_INTEGER, length = 4)
	protected long sequenceNumber;

	@PDUElement(order = 3, type = Type.UNSIGNED_INTEGER, length = 4)
	protected long acknowledgmentNumber;
	
	protected IPv4TCP(){}

	public int getSourcePort()
	{
		return sourcePort;
	}

	public int getDestinationPort()
	{
		return destinationPort;
	}

	public long getSequenceNumber()
	{
		return sequenceNumber;
	}

	public long getAcknowledgmentNumber()
	{
		return acknowledgmentNumber;
	}
	
	

}
