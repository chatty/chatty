package com.pngencoder;

import java.util.Objects;

class PngEncoderDeflaterSegmentResult {
    private final PngEncoderDeflaterBuffer originalSegment;
    private final PngEncoderDeflaterBuffer deflatedSegment;
    private final long originalSegmentAdler32;
    private final int originalSegmentLength;

    PngEncoderDeflaterSegmentResult(
            PngEncoderDeflaterBuffer originalSegment,
            PngEncoderDeflaterBuffer deflatedSegment,
            long originalSegmentAdler32,
            int originalSegmentLength) {
        this.originalSegment = Objects.requireNonNull(originalSegment, "originalSegment");
        this.deflatedSegment = Objects.requireNonNull(deflatedSegment, "deflatedSegment");
        this.originalSegmentAdler32 = originalSegmentAdler32;
        this.originalSegmentLength = originalSegmentLength;
    }

    public PngEncoderDeflaterBuffer getOriginalSegment() {
        return originalSegment;
    }

    public PngEncoderDeflaterBuffer getDeflatedSegment() {
        return deflatedSegment;
    }

    long getUpdatedAdler32(long originalAdler32) {
        return combine(originalAdler32, originalSegmentAdler32, originalSegmentLength);
    }

    // https://github.com/madler/zlib/blob/master/adler32.c#L143
    static long combine(long adler1, long adler2, long len2) {
        long BASEL = 65521;
        long sum1;
        long sum2;
        long rem;

        rem = len2 % BASEL;
        sum1 = adler1 & 0xffffL;
        sum2 = rem * sum1;
        sum2 %= BASEL;
        sum1 += (adler2 & 0xffffL) + BASEL - 1;
        sum2 += ((adler1 >> 16) & 0xffffL) + ((adler2 >> 16) & 0xffffL) + BASEL - rem;
        if (sum1 >= BASEL) {
            sum1 -= BASEL;
        }
        if (sum1 >= BASEL) {
            sum1 -= BASEL;
        }
        if (sum2 >= (BASEL << 1)) {
            sum2 -= (BASEL << 1);
        }
        if (sum2 >= BASEL) {
            sum2 -= BASEL;
        }
        return sum1 | (sum2 << 16);
    }
}
