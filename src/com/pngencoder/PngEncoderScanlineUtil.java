package com.pngencoder;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;

class PngEncoderScanlineUtil {
    private PngEncoderScanlineUtil() {
    }

    static byte[] get(BufferedImage bufferedImage) {
        final int width = bufferedImage.getWidth();
        final int height = bufferedImage.getHeight();

        final PngEncoderBufferedImageType type = PngEncoderBufferedImageType.valueOf(bufferedImage);

        if (type == PngEncoderBufferedImageType.TYPE_INT_RGB) {
            final int[] elements = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
            return getIntRgb(elements, width, height);
        }

        if (type == PngEncoderBufferedImageType.TYPE_INT_ARGB) {
            final int[] elements = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
            return getIntArgb(elements, width, height);
        }

        // TODO: TYPE_INT_ARGB_PRE

        if (type == PngEncoderBufferedImageType.TYPE_INT_BGR) {
            final int[] elements = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
            return getIntBgr(elements, width, height);
        }

        if (type == PngEncoderBufferedImageType.TYPE_3BYTE_BGR) {
            final byte[] elements = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
            return get3ByteBgr(elements, width, height);
        }

        if (type == PngEncoderBufferedImageType.TYPE_4BYTE_ABGR) {
            final byte[] elements = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
            return get4ByteAbgr(elements, width, height);
        }

        // TODO: TYPE_4BYTE_ABGR_PRE

        // TODO: TYPE_USHORT_565_RGB
        // TODO: TYPE_USHORT_555_RGB

        if (type == PngEncoderBufferedImageType.TYPE_BYTE_GRAY) {
            final byte[] elements = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
            return getByteGray(elements, width, height);
        }

        if (type == PngEncoderBufferedImageType.TYPE_USHORT_GRAY) {
            final short[] elements = ((DataBufferUShort) bufferedImage.getRaster().getDataBuffer()).getData();
            return getUshortGray(elements, width, height);
        }

        // Fallback for unsupported type.
        final int[] elements = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        if (bufferedImage.getTransparency() == Transparency.OPAQUE) {
            return getIntRgb(elements, width, height);
        } else {
            return getIntArgb(elements, width, height);
        }
    }

    static byte[] getIntRgb(int[] elements, int width, int height) {
        final int channels = 3;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int element = elements[y*width + x];
                bytes[y*rowByteSize + x*channels + 1] = (byte) (element >> 16); // R
                bytes[y*rowByteSize + x*channels + 2] = (byte) (element >> 8);  // G
                bytes[y*rowByteSize + x*channels + 3] = (byte) (element);       // B
            }
        }
        return bytes;
    }

    static byte[] getIntArgb(int[] elements, int width, int height) {
        final int channels = 4;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int element = elements[y*width + x];
                bytes[y*rowByteSize + x*channels + 1] = (byte) (element >> 16); // R
                bytes[y*rowByteSize + x*channels + 2] = (byte) (element >> 8);  // G
                bytes[y*rowByteSize + x*channels + 3] = (byte) (element);       // B
                bytes[y*rowByteSize + x*channels + 4] = (byte) (element >> 24); // A
            }
        }
        return bytes;
    }

    static byte[] getIntBgr(int[] elements, int width, int height) {
        final int channels = 3;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int element = elements[y*width + x];
                bytes[y*rowByteSize + x*channels + 1] = (byte) (element);       // R
                bytes[y*rowByteSize + x*channels + 2] = (byte) (element >> 8);  // G
                bytes[y*rowByteSize + x*channels + 3] = (byte) (element >> 16); // B
            }
        }
        return bytes;
    }

    static byte[] get3ByteBgr(byte[] elements, int width, int height) {
        final int channels = 3;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bytes[y*rowByteSize + x*channels + 1] = elements[(y*width + x)*3 + 2]; // R
                bytes[y*rowByteSize + x*channels + 2] = elements[(y*width + x)*3 + 1]; // G
                bytes[y*rowByteSize + x*channels + 3] = elements[(y*width + x)*3 + 0]; // B
            }
        }
        return bytes;
    }

    static byte[] get4ByteAbgr(byte[] elements, int width, int height) {
        final int channels = 4;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bytes[y*rowByteSize + x*channels + 1] = elements[(y*width + x)*4 + 3]; // R
                bytes[y*rowByteSize + x*channels + 2] = elements[(y*width + x)*4 + 2]; // G
                bytes[y*rowByteSize + x*channels + 3] = elements[(y*width + x)*4 + 1]; // B
                bytes[y*rowByteSize + x*channels + 4] = elements[(y*width + x)*4];     // A
            }
        }
        return bytes;
    }

    static byte[] getByteGray(byte[] elements, int width, int height) {
        final int channels = 3;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bytes[y*rowByteSize + x*channels + 1] = elements[(y*width + x)]; // R
                bytes[y*rowByteSize + x*channels + 2] = elements[(y*width + x)]; // G
                bytes[y*rowByteSize + x*channels + 3] = elements[(y*width + x)]; // B
            }
        }
        return bytes;
    }

    static byte[] getUshortGray(short[] elements, int width, int height) {
        final int channels = 3;
        final int rowByteSize = 1 + channels*width;
        final byte[] bytes = new byte[rowByteSize * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bytes[y*rowByteSize + x*channels + 1] = (byte) (elements[(y*width + x)] >> 8); // R
                bytes[y*rowByteSize + x*channels + 2] = (byte) (elements[(y*width + x)] >> 8); // G
                bytes[y*rowByteSize + x*channels + 3] = (byte) (elements[(y*width + x)] >> 8); // B
            }
        }
        return bytes;
    }
}
