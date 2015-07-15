package de.sloc.proto;

import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUElement.Type;
import de.sloc.dataformat.PDUSubtype;

public class IPv4 extends FrameHeader {
	@PDUElement(order = 1, type = Type.RAW, length = 2)
	protected byte[] intro;

	@PDUElement(order = 2, type = Type.LENGTH, length = 2)
	protected int length;

	@PDUElement(order = 3, type = Type.RAW, length = 4)
	protected byte[] notInterested2;

	@PDUElement(order = 8, type = Type.UNSIGNED_INTEGER, length = 1)
	protected int ttl;

	@PDUElement(order = 9, type = Type.UNSIGNED_INTEGER, length = 1)
	@PDUSubtype
	protected IPProtocol protocol;

	@PDUElement(order = 10, type = Type.UNSIGNED_INTEGER, length = 2)
	protected int checksum;

	@PDUElement(order = 11, type = Type.UNSIGNED_INTEGER, length = 4)
	protected ConvertibleInetAddress sourceIPAddress;

	@PDUElement(order = 12, type = Type.UNSIGNED_INTEGER, length = 4)
	protected ConvertibleInetAddress destinationIPAddress;

	protected IPv4() {
	}

	public int getTtl() {
		return ttl;
	}

	public IPProtocol getProtocol() {
		return protocol;
	}

	public int getChecksum() {
		return checksum;
	}

	public ConvertibleInetAddress getSourceIPAddress() {
		return sourceIPAddress;
	}

	public ConvertibleInetAddress getDestinationIPAddress() {
		return destinationIPAddress;
	}

}
