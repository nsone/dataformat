package de.sloc.proto;

import de.sloc.dataformat.PDU;
import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUElement.Type;

public class ICMPv4 extends IPv4 {

	@PDUElement(order = 1, type = Type.UNSIGNED_INTEGER, length = 1)
	protected int icmpType;

	@PDUElement(order = 2, type = Type.UNSIGNED_INTEGER, length = 1)
	protected int icmpCode;

	@PDUElement(order = 3, type = Type.UNSIGNED_INTEGER, length = 2)
	protected int icmpChecksum;

	@PDUElement(order = 4, type = Type.RAW, length = 4)
	protected byte[] icmpData;

	protected ICMPv4() {
		super();
	}

	public ICMPv4(ConvertibleInetAddress src, ConvertibleInetAddress dst, byte[] sourceMac, byte[] targetMac) {
		super();
		// IPv4
		intro = new byte[] { 0x45, 0x00 };
		length = 4; // als methode
		notInterested2 = new byte[] { 0x0, 0x0, 0x0, 0x0 };
		ttl = 64;
		protocol = IPProtocol.ICMP;
		sourceIPAddress = src;
		destinationIPAddress = dst;
		checksum = 0x0;
		// ICMP
		icmpCode = 0;
		icmpType = 8;
		icmpChecksum = 0x0;
		icmpData = new byte[54];
		// ETHERNET
		targetAddress = targetMac;
		sourceAddress = sourceMac;
		ethertype = Ethertype.IPV4;
	}

	public int getICMPCode() {
		return icmpCode;
	}

	public int getICMPType() {
		return icmpType;
	}

	public int getICMPChecksum() {
		return icmpChecksum;
	}

	public byte[] getData() {
		return icmpData;
	}

	@Override
	public String toString() {
		return PDU.dump(this);
	}
}
