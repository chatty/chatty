package com.pngencoder;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class PngEncoderLogic {
    // In hex: 89 50 4E 47 0D 0A 1A 0A
    // This is the "file beginning" aka "header" aka "signature" aka "magicnumber".
    // https://en.wikipedia.org/wiki/Portable_Network_Graphics#File_header
    // http://www.libpng.org/pub/png/book/chapter08.html#png.ch08.div.2
    // All PNGs start this way and it does not include any pixel format info.
    static final byte[] FILE_BEGINNING = {-119, 80, 78, 71, 13, 10, 26, 10};

    // In hex: 00 00 00 00 49 45 4E 44 AE 42 60 82
    // This is the "file ending"
    static final byte[] FILE_ENDING = {0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126};

    static final byte IHDR_BIT_DEPTH = 8;
    static final byte IHDR_COLOR_TYPE_RGB = 2;
    static final byte IHDR_COLOR_TYPE_RGBA = 6;
    static final byte IHDR_COMPRESSION_METHOD = 0;
    static final byte IHDR_FILTER_METHOD = 0;
    static final byte IHDR_INTERLACE_METHOD = 0;

    // Default values for the gAMA and cHRM chunks when an sRGB chunk is used,
    // as specified at http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html#C.sRGB
    //   "An application that writes the sRGB chunk should also write a gAMA chunk (and perhaps a cHRM chunk)
    //    for compatibility with applications that do not use the sRGB chunk.
    //    In this situation, only the following values may be used:
    //    ..."
    public static final byte[] GAMA_SRGB_VALUE = ByteBuffer.allocate(4).putInt(45455).array();
    public static final byte[] CHRM_SRGB_VALUE = ByteBuffer.allocate(8 * 4)
            .putInt(31270)
            .putInt(32900)
            .putInt(64000)
            .putInt(33000)
            .putInt(30000)
            .putInt(60000)
            .putInt(15000)
            .putInt(6000)
            .array();

    private PngEncoderLogic() {
    }

    static int encode(BufferedImage bufferedImage, OutputStream outputStream, int compressionLevel, boolean multiThreadedCompressionEnabled, PngEncoderSrgbRenderingIntent srgbRenderingIntent, PngEncoderPhysicalPixelDimensions physicalPixelDimensions) throws IOException {
        Objects.requireNonNull(bufferedImage, "bufferedImage");
        Objects.requireNonNull(outputStream, "outputStream");

        final boolean alpha = bufferedImage.getTransparency() != Transparency.OPAQUE;
        final int width = bufferedImage.getWidth();
        final int height = bufferedImage.getHeight();
        final PngEncoderCountingOutputStream countingOutputStream = new PngEncoderCountingOutputStream(outputStream);

        countingOutputStream.write(FILE_BEGINNING);

        final byte[] ihdr = getIhdrHeader(width, height, alpha);
        final byte[] ihdrChunk = asChunk("IHDR", ihdr);
        countingOutputStream.write(ihdrChunk);

        if (srgbRenderingIntent != null) {
            outputStream.write(asChunk("sRGB", new byte[]{ srgbRenderingIntent.getValue() }));
            outputStream.write(asChunk("gAMA", GAMA_SRGB_VALUE));
            outputStream.write(asChunk("cHRM", CHRM_SRGB_VALUE));
        }

        if (physicalPixelDimensions != null) {
            outputStream.write(asChunk("pHYs", getPhysicalPixelDimensions(physicalPixelDimensions)));
        }

        PngEncoderIdatChunksOutputStream idatChunksOutputStream = new PngEncoderIdatChunksOutputStream(countingOutputStream);
        final byte[] scanlineBytes = PngEncoderScanlineUtil.get(bufferedImage);

        final int segmentMaxLengthOriginal = PngEncoderDeflaterOutputStream.getSegmentMaxLengthOriginal(scanlineBytes.length);

        if (scanlineBytes.length <= segmentMaxLengthOriginal || !multiThreadedCompressionEnabled) {
            Deflater deflater = new Deflater(compressionLevel);
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(idatChunksOutputStream, deflater);
            deflaterOutputStream.write(scanlineBytes);
            deflaterOutputStream.finish();
            deflaterOutputStream.flush();
        } else {
            PngEncoderDeflaterOutputStream deflaterOutputStream = new PngEncoderDeflaterOutputStream(idatChunksOutputStream, compressionLevel, segmentMaxLengthOriginal);
            deflaterOutputStream.write(scanlineBytes);
            deflaterOutputStream.finish();
        }

        countingOutputStream.write(FILE_ENDING);

        countingOutputStream.flush();

        return countingOutputStream.getCount();
    }

    static byte[] getIhdrHeader(int width, int height, boolean alpha) {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put(IHDR_BIT_DEPTH);
        buffer.put(alpha ? IHDR_COLOR_TYPE_RGBA : IHDR_COLOR_TYPE_RGB);
        buffer.put(IHDR_COMPRESSION_METHOD);
        buffer.put(IHDR_FILTER_METHOD);
        buffer.put(IHDR_INTERLACE_METHOD);
        return buffer.array();
    }

    static byte[] getPhysicalPixelDimensions(PngEncoderPhysicalPixelDimensions physicalPixelDimensions) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(physicalPixelDimensions.getPixelsPerUnitX());
        buffer.putInt(physicalPixelDimensions.getPixelsPerUnitY());
        buffer.put(physicalPixelDimensions.getUnit().getValue());
        return buffer.array();
    }

    static byte[] asChunk(String type, byte[] data) {
        PngEncoderVerificationUtil.verifyChunkType(type);
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length + 12);
        byteBuffer.putInt(data.length);
        ByteBuffer byteBufferForCrc = byteBuffer.slice().asReadOnlyBuffer();
        byteBufferForCrc.limit(4 + data.length);
        byteBuffer.put(type.getBytes(StandardCharsets.US_ASCII));
        byteBuffer.put(data);
        byteBuffer.putInt(getCrc32(byteBufferForCrc));
        return byteBuffer.array();
    }

    static int getCrc32(ByteBuffer byteBuffer) {
        CRC32 crc = new CRC32();
        crc.update(byteBuffer);
        return (int) crc.getValue();
    }
}
