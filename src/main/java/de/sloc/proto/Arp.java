package de.sloc.proto;

import java.net.InetAddress;

import javax.xml.bind.DatatypeConverter;

import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUElement.Type;

public class Arp extends FrameHeader
{
	@PDUElement(order = 1, type = Type.UNSIGNED_INTEGER, length = 2)
	protected ArpAddressType addressType;

	@PDUElement(order = 2, type = Type.UNSIGNED_INTEGER, length = 2)
	protected Ethertype protocolType;

	@PDUElement(order = 3, type = Type.LENGTH, references = "sourceMac,destinationMac", length = 1)
	protected short addressLength;

	@PDUElement(order = 4, type = Type.LENGTH, references = "nlSourceAddress,nlDestinationAddress", length = 1)
	protected short protocolLength;

	@PDUElement(order = 5, type = Type.UNSIGNED_INTEGER, length = 2)
	protected ArpOperation operation;

	@PDUElement(order = 6, type = Type.RAW)
	protected byte[] sourceMac;

	@PDUElement(order = 7, type = Type.UNSIGNED_INTEGER)
	protected ConvertableInetAddress nlSourceAddress;

	@PDUElement(order = 8, type = Type.RAW)
	protected byte[] destinationMac;

	@PDUElement(order = 9, type = Type.UNSIGNED_INTEGER)
	protected ConvertableInetAddress nlDestinationAddress;

	protected Arp()
	{
		super();
	}

	public Arp(byte[] targetAddress, byte[] sourceAddress, Ethertype protocolType, ArpAddressType addressType, ArpOperation operation,
	           byte[] sourceMac, InetAddress nlSourceAddress, byte[] destinationMac, InetAddress nlDestinationAddress)
	{
		super(targetAddress, sourceAddress, Ethertype.ARP);
		this.addressType = addressType;
		this.protocolType = protocolType;
		this.operation = operation;
		this.sourceMac = sourceMac;
		this.nlSourceAddress = new ConvertableInetAddress(nlSourceAddress);
		this.destinationMac = destinationMac;
		this.nlDestinationAddress = new ConvertableInetAddress(nlDestinationAddress);
	}

	public ArpAddressType getAddressType()
	{
		return addressType;
	}

	public Ethertype getProtocolType()
	{
		return protocolType;
	}

	public short getAddressLength()
	{
		return addressLength;
	}

	public short getProtocolLength()
	{
		return protocolLength;
	}

	public ArpOperation getOperation()
	{
		return operation;
	}

	public byte[] getSourceMac()
	{
		return sourceMac;
	}

	public ConvertableInetAddress getNlSourceAddress()
	{
		return nlSourceAddress;
	}

	public ConvertableInetAddress getNlDestinationAddress()
	{
		return nlDestinationAddress;
	}

	public byte[] getDestinationMac()
	{
		return destinationMac;
	}

	@Override
	public String toString()
	{
		if (operation == ArpOperation.REQUEST)
		{
			return "Who has " + nlDestinationAddress + "?" + " Tell " + nlSourceAddress;
		}
		else if (operation == ArpOperation.REPLY)
		{
			return nlSourceAddress + " is at " + DatatypeConverter.printHexBinary(sourceAddress);
		}
		else
		{
			return "ARP operation: " + operation;
		}
	}
}
