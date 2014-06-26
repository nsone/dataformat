package de.sloc.proto.ethernet;

import de.sloc.dataformat.PDUElement;
import de.sloc.dataformat.PDUElement.Type;

public class RawFrame extends FrameHeader
{
	@PDUElement(order = 1, type = Type.RAW)
	protected byte[] payload;

}
