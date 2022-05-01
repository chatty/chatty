package com.pngencoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.Deflater;

/**
 * Main class, containing the interface for PngEncoder.
 * PngEncoder is a really fast encoder for PNG images in Java.
 */
public class PngEncoder {
    /**
     * Compression level 9 is the default.
     * It produces images with a size comparable to ImageIO.
     */
    public static int DEFAULT_COMPRESSION_LEVEL = Deflater.BEST_COMPRESSION;

    private final BufferedImage bufferedImage;
    private final int compressionLevel;
    private final boolean multiThreadedCompressionEnabled;
    private final PngEncoderSrgbRenderingIntent srgbRenderingIntent;
    private final PngEncoderPhysicalPixelDimensions physicalPixelDimensions;

    private PngEncoder(
            BufferedImage bufferedImage,
            int compressionLevel,
            boolean multiThreadedCompressionEnabled,
            PngEncoderSrgbRenderingIntent srgbRenderingIntent,
            PngEncoderPhysicalPixelDimensions physicalPixelDimensions) {
        this.bufferedImage = bufferedImage;
        this.compressionLevel = PngEncoderVerificationUtil.verifyCompressionLevel(compressionLevel);
        this.multiThreadedCompressionEnabled = multiThreadedCompressionEnabled;
        this.srgbRenderingIntent = srgbRenderingIntent;
        this.physicalPixelDimensions = physicalPixelDimensions;
    }

    /**
     * Constructs an empty PngEncoder. Usually combined with methods named with*.
     */
    public PngEncoder() {
        this(
                null,
                DEFAULT_COMPRESSION_LEVEL,
                true,
                null,
                null);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code bufferedImage}.
     * The new PngEncoder will use the provided {@code bufferedImage}.
     *
     * @param bufferedImage input image
     * @return a new PngEncoder
     */
    public PngEncoder withBufferedImage(BufferedImage bufferedImage) {
        return new PngEncoder(bufferedImage, compressionLevel, multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code compressionLevel}.
     * The new PngEncoder will use the provided {@code compressionLevel}.
     *
     * @param compressionLevel input image (must be between -1 and 9 inclusive)
     * @return a new PngEncoder
     */
    public PngEncoder withCompressionLevel(int compressionLevel) {
        return new PngEncoder(bufferedImage, compressionLevel, multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code multiThreadedCompressionEnabled}.
     * The new PngEncoder will use the provided {@code multiThreadedCompressionEnabled}.
     *
     * @param multiThreadedCompressionEnabled when {@code true}, multithreaded compression will be used
     * @return a new PngEncoder
     */
    public PngEncoder withMultiThreadedCompressionEnabled(boolean multiThreadedCompressionEnabled) {
        return new PngEncoder(bufferedImage, compressionLevel, multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code srgbRenderingIntent}.
     * The new PngEncoder will add an sRGB chunk to the encoded PNG and use the provided {@code srgbRenderingIntent}.
     *
     * @param srgbRenderingIntent the rendering intent that should be used when displaying the image
     * @return a new PngEncoder
     */
    public PngEncoder withSrgbRenderingIntent(PngEncoderSrgbRenderingIntent srgbRenderingIntent) {
        return new PngEncoder(bufferedImage, compressionLevel, multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions);
    }

    public PngEncoder withPhysicalPixelDimensions(PngEncoderPhysicalPixelDimensions physicalPixelDimensions) {
        return new PngEncoder(bufferedImage, compressionLevel, multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions);
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public boolean isMultiThreadedCompressionEnabled() {
        return multiThreadedCompressionEnabled;
    }

    public PngEncoderSrgbRenderingIntent getSrgbRenderingIntent() {
        return srgbRenderingIntent;
    }

    /**
     * Encodes the image to outputStream.
     * @param outputStream destination of the encoded data
     * @throws NullPointerException if the image has not been set.
     * @return number of bytes written
     */
    public int toStream(OutputStream outputStream) {
        try {
            return PngEncoderLogic.encode(bufferedImage, outputStream, compressionLevel, multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Encodes the image and saves data into {@code filePath}.
     * @param filePath destination file where the encoded data will be written
     * @throws NullPointerException if the image has not been set.
     * @throws UncheckedIOException instead of IOException
     * @return number of bytes written
     */
    public int toFile(Path filePath) {
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            return toStream(outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Encodes the image and saves data into {@code file}.
     * @param file destination file where the encoded data will be written
     * @throws NullPointerException if the image has not been set.
     * @throws UncheckedIOException instead of IOException
     * @return number of bytes written
     */
    public int toFile(File file) {
        return toFile(file.toPath());
    }

    /**
     * Encodes the image and saves data into {@code fileName}.
     * @param fileName destination file where the encoded data will be written
     * @throws NullPointerException if the image has not been set.
     * @throws UncheckedIOException instead of IOException
     * @return number of bytes written
     */
    public int toFile(String fileName) {
        return toFile(Paths.get(fileName));
    }

    /**
     * Encodes the image and returns data as {@code byte[]}.
     * @throws NullPointerException if the image has not been set.
     * @return encoded data
     */
    public byte[] toBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64 * 1024);
        toStream(outputStream);
        return outputStream.toByteArray();
    }
}
