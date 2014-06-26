package de.sloc.proto.ethernet;

import javax.xml.bind.DatatypeConverter;

import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUSerializable;
import de.sloc.dataformat.PDUSubtype;
import de.sloc.dataformat.PDUElement.Type;

public abstract class FrameHeader implements PDUSerializable, Constants
{
	public static final byte[] BROADCAST_ADDRESS = DatatypeConverter.parseHexBinary("FFFFFFFFFFFF");

	// @PDUElement(order = 1, type = Type.RAW, length = 8)
	// protected byte[] notInterested;

	@PDUElement(order = 2, type = Type.RAW, length = 6)
	protected byte[] targetAddress;

	@PDUElement(order = 3, type = Type.RAW, length = 6)
	protected byte[] sourceAddress;

	@PDUElement(order = 4, type = Type.UNSIGNED_INTEGER, length = 2)
	@PDUSubtype
	protected Ethertype ethertype;

	public FrameHeader(byte[] targetAddress, byte[] sourceAddress, Ethertype ethertype)
	{
		super();
		this.targetAddress = targetAddress;
		this.sourceAddress = sourceAddress;
		this.ethertype = ethertype;
	}

	protected FrameHeader()
	{
	}

	public byte[] getTargetAddress()
	{
		return targetAddress;
	}

	public byte[] getSourceAddress()
	{
		return sourceAddress;
	}

	public Ethertype getEthertype()
	{
		return ethertype;
	}

}
