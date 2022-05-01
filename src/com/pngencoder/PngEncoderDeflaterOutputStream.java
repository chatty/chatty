package com.pngencoder;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

// https://tools.ietf.org/html/rfc1950
// https://stackoverflow.com/questions/9050260/what-does-a-zlib-header-look-like
// https://www.euccas.me/zlib/
// https://stackoverflow.com/questions/13132136/java-multithreaded-compression-with-deflater
class PngEncoderDeflaterOutputStream extends FilterOutputStream {
    // The maximum amount of queued tasks.
    // Multiplied because some segments compress faster than others.
    // A value of 3 seems to keep all threads busy.
    static final int COUNT_MAX_QUEUED_TASKS = PngEncoderDeflaterExecutorService.NUM_THREADS_IS_AVAILABLE_PROCESSORS * 3;

    // Enforces writing to underlying stream in main thread.
    // Multiplied so that not all work is finished before flush to underlying stream.
    static final int COUNT_MAX_TOTAL_SEGMENTS = COUNT_MAX_QUEUED_TASKS * 3;

    // The maximum dictionary size according to the deflate specification.
    // A segment max length lower than this would not allow for future use of dictionary.
    // Used for unit test sanity checking.
    static final int SEGMENT_MAX_LENGTH_DICTIONARY = 32 * 1024;

    // Our minimum segment length.
    // Corresponds to about 2% size overhead.
    // A lower value would better parallelize images but increase the size overhead.
    static final int SEGMENT_MAX_LENGTH_ORIGINAL_MIN = 128 * 1024;

    public static int getSegmentMaxLengthOriginal(int totalOriginalBytesLength) {
        return Math.max(totalOriginalBytesLength / COUNT_MAX_TOTAL_SEGMENTS, SEGMENT_MAX_LENGTH_ORIGINAL_MIN);
    }

    public static int getSegmentMaxLengthDeflated(int segmentMaxLengthOriginal) {
        return segmentMaxLengthOriginal + (segmentMaxLengthOriginal >> 3);
    }

    private final PngEncoderDeflaterBufferPool pool;
    private final byte[] singleByte;
    private final int compressionLevel;
    private final int segmentMaxLengthOriginal;
    private final ConcurrentLinkedQueue<CompletableFuture<PngEncoderDeflaterSegmentResult>> resultQueue;
    private PngEncoderDeflaterBuffer originalSegment;
    private long adler32;
    private boolean finished;
    private boolean closed;

    PngEncoderDeflaterOutputStream(OutputStream out, int compressionLevel, int segmentMaxLengthOriginal, PngEncoderDeflaterBufferPool pool) throws IOException {
        super(Objects.requireNonNull(out, "out"));
        this.pool = Objects.requireNonNull(pool, "pool");
        this.singleByte = new byte[1];
        this.compressionLevel = compressionLevel;
        this.segmentMaxLengthOriginal = segmentMaxLengthOriginal;
        this.resultQueue = new ConcurrentLinkedQueue<>();
        this.originalSegment = pool.borrow();
        this.adler32 = 1;
        this.finished = false;
        this.closed = false;
        if (pool.getBufferMaxLength() != getSegmentMaxLengthDeflated(segmentMaxLengthOriginal)) {
            throw new IllegalArgumentException("Mismatch between segmentMaxLengthOriginal and pool.");
        }
        writeDeflateHeader(out, compressionLevel);
    }

    PngEncoderDeflaterOutputStream(OutputStream out, int compressionLevel, int segmentMaxLengthOriginal) throws IOException {
        this(out, compressionLevel, segmentMaxLengthOriginal, new PngEncoderDeflaterBufferPool(getSegmentMaxLengthDeflated(segmentMaxLengthOriginal)));
    }

    @Override
    public void write(int b) throws IOException {
        singleByte[0] = (byte)(b & 0xff);
        write(singleByte, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (finished) {
            throw new IOException("write beyond end of stream");
        }
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        while (len > 0) {
            int freeBufCount = segmentMaxLengthOriginal - originalSegment.length;
            if (freeBufCount == 0) {
                // Submit task if the buffer is full and there still is more to write.
                joinUntilMaximumQueueSize(COUNT_MAX_QUEUED_TASKS - 1);
                submitTask(false);
            } else {
                int toCopyCount = Math.min(len, freeBufCount);
                System.arraycopy(b, off, originalSegment.bytes, originalSegment.length, toCopyCount);
                originalSegment.length += toCopyCount;
                off += toCopyCount;
                len -= toCopyCount;
            }
        }
    }

    public void finish() throws IOException {
        if (this.finished) {
            return;
        }
        this.finished = true;
        try {
            submitTask(true);
            joinUntilMaximumQueueSize(0);
            out.write(ByteBuffer.allocate(4).putInt((int) adler32).array());
            out.flush();
        } finally {
            originalSegment.giveBack();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        finish();
        super.close();
    }

    void submitTask(boolean lastSegment) {
        final PngEncoderDeflaterBuffer deflatedSegment = pool.borrow();
        final PngEncoderDeflaterSegmentTask task = new PngEncoderDeflaterSegmentTask(originalSegment, deflatedSegment, compressionLevel, lastSegment);
        submitTask(task);
        originalSegment = pool.borrow();
    }

    void submitTask(PngEncoderDeflaterSegmentTask task) {
        CompletableFuture<PngEncoderDeflaterSegmentResult> future = CompletableFuture.supplyAsync(task, PngEncoderDeflaterExecutorService.getInstance());
        resultQueue.offer(future);
    }

    void joinOne() throws IOException {
        CompletableFuture<PngEncoderDeflaterSegmentResult> resultFuture = resultQueue.poll();
        if (resultFuture != null) {
            final PngEncoderDeflaterSegmentResult result;
            try {
                result = resultFuture.join();
            } catch (RuntimeException e) {
                throw new IOException("An async segment task failed.", e);
            }
            try {
                adler32 = result.getUpdatedAdler32(adler32);
                result.getDeflatedSegment().write(out);
            } finally {
                result.getOriginalSegment().giveBack();
                result.getDeflatedSegment().giveBack();
            }
        }
    }

    void joinUntilMaximumQueueSize(int maximumResultQueueSize) throws IOException {
        while (resultQueue.size() > maximumResultQueueSize) {
            joinOne();
        }
    }

    static void writeDeflateHeader(OutputStream outputStream, int compressionLevel) throws IOException {
        // Write "CMF"
        // " ... In practice, this means the first byte is almost always 78 (hex) ..."
        outputStream.write(0x78);

        // Write "FLG"
        byte flg = getFlg(compressionLevel);
        outputStream.write(flg);
    }

    static byte getFlg(int compressionLevel) {
        if (compressionLevel == -1 || compressionLevel == 6) {
            return (byte) 0x9C;
        }

        if (compressionLevel >= 0 && compressionLevel <= 1) {
            return (byte) 0x01;
        }

        if (compressionLevel >= 2 && compressionLevel <= 5) {
            return (byte) 0x5E;
        }

        if (compressionLevel >= 7 && compressionLevel <= 9) {
            return (byte) 0xDA;
        }

        throw new IllegalArgumentException("Invalid compressionLevel: " + compressionLevel);
    }
}
