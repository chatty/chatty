package com.pngencoder;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.Deflater;

class PngEncoderDeflaterSegmentTask implements Supplier<PngEncoderDeflaterSegmentResult> {
    private final PngEncoderDeflaterBuffer originalSegment;
    private final PngEncoderDeflaterBuffer deflatedSegment;
    private final int compressionLevel;
    private final boolean lastSegment;

    public PngEncoderDeflaterSegmentTask(
            PngEncoderDeflaterBuffer originalSegment,
            PngEncoderDeflaterBuffer deflatedSegment,
            int compressionLevel,
            boolean lastSegment) {
        this.originalSegment = Objects.requireNonNull(originalSegment, "originalSegment");
        this.deflatedSegment = Objects.requireNonNull(deflatedSegment, "deflatedSegment");
        this.compressionLevel = compressionLevel;
        this.lastSegment = lastSegment;
    }

    @Override
    public PngEncoderDeflaterSegmentResult get() {
        final long originalSegmentAdler32 = originalSegment.calculateAdler32();
        final int originalSegmentLength = originalSegment.length;

        deflate(originalSegment, deflatedSegment, compressionLevel, lastSegment);

        return new PngEncoderDeflaterSegmentResult(originalSegment, deflatedSegment, originalSegmentAdler32, originalSegmentLength);
    }

    static void deflate(PngEncoderDeflaterBuffer originalSegment, PngEncoderDeflaterBuffer deflatedSegment, int compressionLevel, boolean lastSegment) {
        final Deflater deflater = PngEncoderDeflaterThreadLocalDeflater.getInstance(compressionLevel);
        deflater.setInput(originalSegment.bytes, 0, originalSegment.length);

        if (lastSegment) {
            deflater.finish();
        }

        deflatedSegment.length = deflater.deflate(deflatedSegment.bytes, 0, deflatedSegment.bytes.length, lastSegment ? Deflater.NO_FLUSH : Deflater.SYNC_FLUSH);
    }
}
