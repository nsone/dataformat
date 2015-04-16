package de.sloc.dataformat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PDUInputStream<T extends PDUSerializable> extends InputStream
{
    private final int MAX_SIZE = 163840;

    protected Class<T> pduClass;
    protected BufferedInputStream inputStream;
    protected byte[] readBuffer = new byte[MAX_SIZE];
    protected int lengthOffset;
    protected int lengthFieldLength;
    protected int delta;

    // protected ExecutorService es = Executors.newFixedThreadPool(3);

    public PDUInputStream(Class<T> pduClass, InputStream inputStream)
    {
        this.pduClass = pduClass;
        this.inputStream = new BufferedInputStream(inputStream);
        int[] metadata = PDU.getLengthMetadata(pduClass);
        this.lengthOffset = metadata[0];
        this.lengthFieldLength = metadata[1];
        this.delta = metadata[2];
    }

    @Override
    public int read() throws IOException
    {
        return this.inputStream.read();
    }

    public int waitForBytes(int offset) throws IOException
    {
        return inputStream.read(readBuffer, offset, MAX_SIZE - offset);
    }

    public T readPDU() throws IOException
    {
        int bytesRead = 0;
        inputStream.mark(MAX_SIZE);
        long pduLength = 0;

        for (;;)
        {
            bytesRead += waitForBytes(bytesRead);
            // System.out.println("got " + bytesRead + " bytes");

            if (bytesRead < 0)
                break;

            if (bytesRead > (lengthOffset + lengthFieldLength) && pduLength == 0)
            {
                byte[] lengthArray = new byte[lengthFieldLength];
                System.arraycopy(readBuffer, lengthOffset, lengthArray, 0, lengthFieldLength);
                pduLength = new Length(lengthArray, lengthFieldLength, new String[0]).toLong() + this.delta;
            }

            if (pduLength > 0 && bytesRead >= pduLength)
            {
                byte[] message = new byte[(int) pduLength];
                System.arraycopy(readBuffer, 0, message, 0, (int) pduLength);

                if (bytesRead > pduLength)
                {
                    // cut off first message
                    // go back to the beginning
                    inputStream.reset();

                    // reread first message
                    inputStream.read(readBuffer, 0, (int) pduLength);

                    // mark after first message
                    inputStream.mark(MAX_SIZE);
                }

                // FutureTask<T> task = new FutureTask<T>(() ->
                // PDU.decode(message, pduClass, 0));
                // es.submit(task);
                // return task;

                // long before = System.currentTimeMillis();
                T pdu = PDU.decode(message, pduClass, 0);
                // System.err.println("Decoded message in: " +
                // (System.currentTimeMillis() - before) + "ms");
                return pdu;
            }

        }
        return null;
    }
}
