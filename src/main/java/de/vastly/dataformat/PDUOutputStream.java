package de.vastly.dataformat;

import java.io.IOException;
import java.io.OutputStream;

public class PDUOutputStream<T extends PDUSerializable> extends OutputStream
{
	protected OutputStream outputStream;

	public PDUOutputStream(OutputStream outputStream)
	{
		this.outputStream = outputStream;
	}

	@Override
	public void write(int b) throws IOException
	{
		throw new IllegalArgumentException("Please call writePDU");

	}

	public void write(T pdu) throws PDUException, IOException
	{
		outputStream.write(PDU.encode(pdu));
	}

}
