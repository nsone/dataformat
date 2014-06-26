package de.vastly.proto.ethernet;

import de.vastly.dataformat.PDUElement;
import de.vastly.dataformat.PDUElement.Type;

public class RawFrame extends FrameHeader
{
	@PDUElement(order = 1, type = Type.RAW)
	protected byte[] payload;

}
