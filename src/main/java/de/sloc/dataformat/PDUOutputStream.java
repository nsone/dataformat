package de.sloc.dataformat;

import static de.sloc.dataformat.PDU.NEWLINE;

import java.io.IOException;
import java.io.OutputStream;

public class PDUOutputStream<T extends PDUSerializable> extends OutputStream
{
    protected OutputStream outputStream;
    protected boolean newline;

    public PDUOutputStream(OutputStream outputStream, boolean newline)
    {
        this.outputStream = outputStream;
        this.newline = newline;
    }

    public PDUOutputStream(OutputStream outputStream)
    {
        this(outputStream, false);
    }

    @Override
    public void write(int b) throws IOException
    {
        throw new IllegalArgumentException("Please call writePDU");

    }

    public void write(T pdu) throws PDUException, IOException
    {
        outputStream.write(PDU.encode(pdu));
        if (newline)
        {
            outputStream.write(NEWLINE);
        }
    }

}
