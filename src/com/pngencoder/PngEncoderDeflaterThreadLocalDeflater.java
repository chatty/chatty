package com.pngencoder;

import java.util.zip.Deflater;

/**
 * We save time by allocating and reusing some thread local state.
 *
 * Creating a new Deflater instance takes a surprising amount of time.
 * Resetting an existing Deflater instance is almost free though.
 */
class PngEncoderDeflaterThreadLocalDeflater {
    private static final ThreadLocal<PngEncoderDeflaterThreadLocalDeflater> THREAD_LOCAL = ThreadLocal.withInitial(PngEncoderDeflaterThreadLocalDeflater::new);

    static Deflater getInstance(int compressionLevel) {
        return THREAD_LOCAL.get().getDeflater(compressionLevel);
    }

    private final Deflater[] deflaters;

    private PngEncoderDeflaterThreadLocalDeflater() {
        this.deflaters = new Deflater[11];
        for (int compressionLevel = -1; compressionLevel <= 9; compressionLevel++) {
            boolean nowrap = true;
            this.deflaters[compressionLevel + 1] = new Deflater(compressionLevel, nowrap);
        }
    }

    private Deflater getDeflater(int compressionLevel) {
        Deflater deflater = this.deflaters[compressionLevel + 1];
        deflater.reset();
        return deflater;
    }
}
