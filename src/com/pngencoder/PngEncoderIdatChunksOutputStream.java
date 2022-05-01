package com.pngencoder;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

class PngEncoderIdatChunksOutputStream extends FilterOutputStream {
    static final byte[] IDAT_BYTES = "IDAT".getBytes(StandardCharsets.US_ASCII);

    // An IDAT chunk adds 12 bytes of overhead to the data within.
    // 12 / (32 * 1024) = 0.00037 meaning the size overhead is just 0.037% which should be negligible.
    static final int DEFAULT_BUFFER_LENGTH = 32 * 1024;

    private final CRC32 crc;
    private final byte[] buf;
    private int count;

    PngEncoderIdatChunksOutputStream(OutputStream out, int bufferLength) {
        super(out);
        this.crc = new CRC32();
        this.buf = new byte[bufferLength];
        this.count = 0;
    }

    PngEncoderIdatChunksOutputStream(OutputStream out) {
        this(out, DEFAULT_BUFFER_LENGTH);
    }

    @Override
    public void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte)b;
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len >= buf.length) {
            flushBuffer();
            writeIdatChunk(b, off, len);
            return;
        }
        if (len > buf.length - count) {
            flushBuffer();
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        super.flush();
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            writeIdatChunk(buf, 0, count);
            count = 0;
        }
    }

    private void writeIdatChunk(byte[] b, int off, int len) throws IOException {
        writeInt(len);
        out.write(IDAT_BYTES);
        out.write(b, off, len);
        crc.reset();
        crc.update(IDAT_BYTES);
        crc.update(b, off, len);
        writeInt((int) crc.getValue());
    }

    private void writeInt(int i) throws IOException {
        out.write((byte) (i >> 24) & 0xFF);
        out.write((byte) (i >> 16) & 0xFF);
        out.write((byte) (i >> 8) & 0xFF);
        out.write((byte) i & 0xFF);
    }
}
