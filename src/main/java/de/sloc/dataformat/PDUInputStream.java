package de.sloc.dataformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

public class PDUInputStream<T extends PDUSerializable> extends InputStream
{
    private final int MAX_SIZE = 10000000;

    protected Class<T> pduClass;
    protected InputStream inputStream;
    protected byte[] readBuffer = new byte[MAX_SIZE];

    boolean isFixedLength = false;

    protected int lengthOffset;
    protected int lengthFieldLength;
    protected int delta;

    protected int fixedLength;

    protected ExecutorService es = Executors.newFixedThreadPool(8);

    public PDUInputStream(Class<T> pduClass, InputStream inputStream)
    {
        this.pduClass = pduClass;
        this.inputStream = inputStream;

        int[] metadata = PDU.getLengthMetadata(pduClass);

        if (metadata.length == 3)
        {
            this.lengthOffset = metadata[0];
            this.lengthFieldLength = metadata[1];
            this.delta = metadata[2];
        }
        else if (metadata.length == 1)
        {
            fixedLength = metadata[0];
            isFixedLength = true;
        }
        else
        {
            throw new IllegalStateException("Unknown length metadata found");
        }

    }

    @Override
    public int read() throws IOException
    {
        return this.inputStream.read();
    }

    public void close() throws IOException
    {
        es.shutdown();
        super.close();
    }

    public int waitForBytes(int offset) throws IOException
    {
        return inputStream.read(readBuffer, offset, MAX_SIZE - offset);
    }

    private boolean enoughBytesForLengthField(int offset, int bytesReady)
    {
        return bytesReady > offset + lengthOffset + lengthFieldLength;
    }

    private boolean enoughBytesForPDU(int offset, long pduLength, int bytesReady)
    {
        return bytesReady >= offset + pduLength;
    }

    private int parsePDULength()
    {
        byte[] lengthArray = new byte[lengthFieldLength];
        System.arraycopy(readBuffer, lengthOffset, lengthArray, 0, lengthFieldLength);
        return new Length(lengthArray, lengthFieldLength, new String[0]).toInt() + this.delta;
    }

    private Future<T> createPDUTask(int offset, int pduLength) throws PDUException
    {
        byte[] pduBytes = new byte[pduLength];
        System.arraycopy(readBuffer, offset, pduBytes, 0, pduLength);
        FutureTask<T> futureTask = new FutureTask<>(() -> PDU.decode(pduBytes, pduClass, 0));
        es.submit(futureTask);
        return futureTask;
    }

    private boolean bufferOverlapsLengthField(int offset)
    {
        boolean result = offset + lengthOffset + lengthFieldLength > MAX_SIZE;
        // if (result)
        // System.out.println("buffer overlaps length field");
        return result;
    }

    private boolean bufferOverlapsPDU(int offset, int pduLength)
    {
        boolean result = offset + pduLength > MAX_SIZE;
        // if (result)
        // System.out.println("buffer overlaps pdu");
        return result;
    }

    private int moveRestBytes(int offset)
    {
        int restBytes = MAX_SIZE - offset;
        byte[] newReadBuffer = new byte[MAX_SIZE];
        System.arraycopy(readBuffer, offset, newReadBuffer, 0, restBytes);
        this.readBuffer = newReadBuffer;

        return restBytes;
    }

    public T readPDU() throws IOException
    {
        try
        {
            Future<T> futurePdu = nextPDU();
            return futurePdu == null ? null : futurePdu.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new IOException(e);
        }
    }

    private Queue<Future<T>> pduQueue = new LinkedBlockingQueue<>();

    public Future<T> nextPDU() throws IOException
    {
        if (pduQueue.isEmpty())
        {
            List<Future<T>> pdus = nextPDUs();
            if (pdus == null)
            {
                return null;
            }

            pduQueue.addAll(pdus);
        }

        return pduQueue.poll();
    }

    protected int bytesReady = 0, offset = 0;

    protected List<Future<T>> nextPDUs() throws IOException
    {
        int pduLength = fixedLength;

        List<Future<T>> pdus = new ArrayList<>();

        outer:
        do
        {
            if (!isFixedLength)
            {
                if (bufferOverlapsLengthField(offset))
                {
                    bytesReady = moveRestBytes(offset);
                    offset = 0;
                    break outer;
                }

                while (!enoughBytesForLengthField(offset, bytesReady))
                {
                    int newBytesReady = waitForBytes(bytesReady);
                    if (newBytesReady == -1)
                    {
                        break outer;
                    }
                    bytesReady += newBytesReady;
                }

                // got enough bytes for length field
                pduLength = parsePDULength();
            }

            if (bufferOverlapsPDU(offset, pduLength))
            {
                bytesReady = moveRestBytes(offset);
                offset = 0;
                break outer;
            }

            while (!enoughBytesForPDU(offset, pduLength, bytesReady))
            {
                int newBytesReady = waitForBytes(bytesReady);
                if (newBytesReady == -1)
                {
                    break outer;
                }
                bytesReady += newBytesReady;
            }
            // got enough bytes for PDU
            pdus.add(createPDUTask(offset, pduLength));

            offset += pduLength;

        }
        // there are more PDUs
        while (bytesReady > offset);

        if ((!isFixedLength && bufferOverlapsLengthField(offset)) || bufferOverlapsPDU(offset, pduLength))
        {
            bytesReady = moveRestBytes(offset);
            offset = 0;
        }

        if (pdus.size() == 0)
        {
            return null;
        }
        else
        {
            return pdus;
        }
    }

}
