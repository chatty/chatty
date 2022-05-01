package com.pngencoder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class PngEncoderBufferedImageConverter {
    private static final int[] BAND_MASKS_INT_ARGB = {
            0x00ff0000,
            0x0000ff00,
            0x000000ff,
            0xff000000};
    private static final ColorModel COLOR_MODEL_INT_ARGB = ColorModel.getRGBdefault();

    private static final int[] BAND_MASKS_INT_RGB = {
            0x00ff0000,
            0x0000ff00,
            0x000000ff};
    private static final ColorModel COLOR_MODEL_INT_RGB = new DirectColorModel(
            24,
            0x00ff0000,
            0x0000ff00,
            0x000000ff,
            0x0);

    private static final int[] BAND_MASKS_INT_ARGB_PRE = {
            0x00ff0000,
            0x0000ff00,
            0x000000ff,
            0xff000000};
    private static final ColorModel COLOR_MODEL_INT_ARGB_PRE = new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            32,
            0x00ff0000,
            0x0000ff00,
            0x000000ff,
            0xff000000,
            true,
            DataBuffer.TYPE_INT);

    private static final int[] BAND_MASKS_INT_BGR = {
            0x000000ff,
            0x0000ff00,
            0x00ff0000};
    private static final ColorModel COLOR_MODEL_INT_BGR = new DirectColorModel(
            24,
            0x000000ff,
            0x0000ff00,
            0x00ff0000);

    private static final int[] BAND_MASKS_USHORT_565_RGB = {
            0xf800,
            0x07E0,
            0x001F};

    private static final int[] BAND_MASKS_USHORT_555_RGB = {
            0x7C00,
            0x03E0,
            0x001F};

    private PngEncoderBufferedImageConverter() {
    }

    public static BufferedImage createFromIntArgb(int[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferInt(data, data.length);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_INT_ARGB, null);
        return new BufferedImage(COLOR_MODEL_INT_ARGB, raster, false, null);
    }

    public static BufferedImage createFromIntRgb(int[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferInt(data, data.length);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_INT_RGB, null);
        return new BufferedImage(COLOR_MODEL_INT_RGB, raster, false, null);
    }

    public static BufferedImage createFromIntArgbPre(int[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferInt(data, data.length);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_INT_ARGB_PRE, null);
        return new BufferedImage(COLOR_MODEL_INT_ARGB_PRE, raster, true, null);
    }

    public static BufferedImage createFromIntBgr(int[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferInt(data, data.length);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_INT_BGR, null);
        return new BufferedImage(COLOR_MODEL_INT_BGR, raster, false, null);
    }

    public static BufferedImage createFrom3ByteBgr(byte[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8};
        int[] bOffs = {2, 1, 0};
        ColorModel colorModel = new ComponentColorModel(colorSpace, nBits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width * 3, 3, bOffs, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static BufferedImage createFrom4ByteAbgr(byte[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8, 8};
        int[] bOffs = {3, 2, 1, 0};
        ColorModel colorModel = new ComponentColorModel(colorSpace, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width * 4, 4, bOffs, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static BufferedImage createFrom4ByteAbgrPre(byte[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8, 8};
        int[] bOffs = {3, 2, 1, 0};
        ColorModel colorModel = new ComponentColorModel(colorSpace, nBits, true, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width * 4, 4, bOffs, null);
        return new BufferedImage(colorModel, raster, true, null);
    }

    public static BufferedImage createFromUshort565Rgb(short[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferUShort(data, data.length);
        ColorModel colorModel = new DirectColorModel(16, BAND_MASKS_USHORT_565_RGB[0], BAND_MASKS_USHORT_565_RGB[1], BAND_MASKS_USHORT_565_RGB[2]);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_USHORT_565_RGB, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static BufferedImage createFromUshort555Rgb(short[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferUShort(data, data.length);
        ColorModel colorModel = new DirectColorModel(15, BAND_MASKS_USHORT_555_RGB[0], BAND_MASKS_USHORT_555_RGB[1], BAND_MASKS_USHORT_555_RGB[2]);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_USHORT_555_RGB, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static BufferedImage createFromByteGray(byte[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        int[] nBits = {8};
        ColorModel colorModel = new ComponentColorModel(colorSpace, nBits, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        int[] bandOffsets = {0};
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width, 1, bandOffsets, null);
        return new BufferedImage(colorModel, raster, true, null);
    }

    public static BufferedImage createFromUshortGray(short[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferUShort(data, data.length);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        int[] nBits = {16};
        ColorModel colorModel = new ComponentColorModel(colorSpace, nBits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        int[] bandOffsets = {0};
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width, 1, bandOffsets, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static BufferedImage createFromByteBinary(byte[] data, int width, int height) {
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        byte[] arr = {(byte)0, (byte)0xff};
        IndexColorModel colorModel = new IndexColorModel(1, 2, arr, arr, arr);
        WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, 1, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static DataBuffer getDataBuffer(BufferedImage bufferedImage) {
        return bufferedImage.getRaster().getDataBuffer();
    }

    public static DataBufferInt getDataBufferInt(BufferedImage bufferedImage) {
        return (DataBufferInt) getDataBuffer(bufferedImage);
    }

    public static DataBufferUShort getDataBufferUShort(BufferedImage bufferedImage) {
        return (DataBufferUShort) getDataBuffer(bufferedImage);
    }

    public static DataBufferByte getDataBufferByte(BufferedImage bufferedImage) {
        return (DataBufferByte) getDataBuffer(bufferedImage);
    }

    public static BufferedImage copyType(BufferedImage bufferedImage, PngEncoderBufferedImageType type) {
        final int width = bufferedImage.getWidth();
        final int height = bufferedImage.getHeight();
        final BufferedImage convertedBufferedImage = new BufferedImage(width, height, type.ordinal());
        final Graphics graphics = convertedBufferedImage.getGraphics();
        if (convertedBufferedImage.getTransparency() == Transparency.OPAQUE) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.drawImage(bufferedImage, 0, 0, null);
        graphics.dispose();
        return convertedBufferedImage;
    }

    public static BufferedImage ensureType(BufferedImage bufferedImage, PngEncoderBufferedImageType type) {
        if (PngEncoderBufferedImageType.valueOf(bufferedImage) == type) {
            return bufferedImage;
        } else {
            return copyType(bufferedImage, type);
        }
    }
}
