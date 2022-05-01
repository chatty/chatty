package com.pngencoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.Adler32;

class PngEncoderDeflaterBuffer {
    final PngEncoderDeflaterBufferPool pool;
    final byte[] bytes;
    int length;

    PngEncoderDeflaterBuffer(PngEncoderDeflaterBufferPool pool, int maxLength) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.bytes = new byte[maxLength];
        this.length = 0;
    }

    void giveBack() {
        pool.giveBack(this);
    }

    long calculateAdler32() {
        Adler32 adler32 = new Adler32();
        adler32.update(bytes, 0, length);
        return adler32.getValue();
    }

    void write(OutputStream outputStream) throws IOException {
        outputStream.write(bytes, 0, length);
    }
}
