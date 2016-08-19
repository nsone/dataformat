package de.sloc.dataformat;

import java.io.BufferedInputStream;
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
    protected static int MAX_SIZE = 100000000;

    protected Class<T> pduClass;
    protected BufferedInputStream inputStream;
    protected byte[] readBuffer;

    boolean isFixedLength = false;

    protected int lengthOffset;
    protected int lengthFieldLength;
    protected int delta;

    protected int fixedLength;

    protected ExecutorService es = Executors.newFixedThreadPool(3);

    public PDUInputStream(Class<T> pduClass, InputStream inputStream)
    {
        readBuffer = new byte[MAX_SIZE];
        this.pduClass = pduClass;
        this.inputStream = new BufferedInputStream(inputStream);

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

    public int waitForBytes() throws IOException
    {
        return inputStream.read(readBuffer, bytesReady, MAX_SIZE - bytesReady);
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

    public void close() throws IOException
    {
        es.shutdown();
        super.close();
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
        List<Future<T>> nextPDUs = new ArrayList<>();
        bytesReady = 0;
        int bytesRead = 0;
        boolean onePDURead = false;
        inputStream.mark(MAX_SIZE);

        loop: for (;;)
        {
            long pduLength = 0;
            offset = 0;

            bytesReady += waitForBytes();

            if (bytesReady < 0)
                return null;

            while (bytesReady > pduLength)
            {
                if (isFixedLength)
                {
                    pduLength = fixedLength;
                }
                else if (isEnoughLengthBytesReady())
                {
                    pduLength = readLength();
                }
                else if (!onePDURead)
                {
                    continue loop;
                }

                if (isEnoughBytesReady(pduLength))
                {
                    nextPDUs.add(createDecodeTask(pduLength));
                    onePDURead = true;
                }
                else if (!onePDURead)
                {
                    continue loop;
                }

                bytesReady -= pduLength;
                bytesRead += pduLength;
            }
            break loop;
        }

        inputStream.reset();
        inputStream.skip(bytesRead);

        return nextPDUs;
    }

    protected FutureTask<T> createDecodeTask(long pduLength)
    {
        byte[] message = new byte[(int) pduLength];
        System.arraycopy(readBuffer, offset, message, 0, (int) pduLength);
        offset += pduLength;
        FutureTask<T> task = new FutureTask<T>(() -> PDU.decode(message, pduClass, 0));
        es.submit(task);
        return task;
    }

    protected boolean isEnoughBytesReady(long length)
    {
        return length > 0 && bytesReady >= length;
    }

    protected boolean isEnoughLengthBytesReady()
    {
        return bytesReady >= (lengthOffset + lengthFieldLength);
    }

    protected long readLength()
    {
        long pduLength;
        byte[] lengthArray = new byte[lengthFieldLength];
        System.arraycopy(readBuffer, offset + lengthOffset, lengthArray, 0, lengthFieldLength);
        pduLength = new Length(lengthArray, lengthFieldLength, new String[0]).toLong() + this.delta;
        return pduLength;
    }

}
