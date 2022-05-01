package com.pngencoder;

import java.awt.image.BufferedImage;

public enum PngEncoderBufferedImageType {
    TYPE_CUSTOM,
    TYPE_INT_RGB,
    TYPE_INT_ARGB,
    TYPE_INT_ARGB_PRE,
    TYPE_INT_BGR,
    TYPE_3BYTE_BGR,
    TYPE_4BYTE_ABGR,
    TYPE_4BYTE_ABGR_PRE,
    TYPE_USHORT_565_RGB,
    TYPE_USHORT_555_RGB,
    TYPE_BYTE_GRAY,
    TYPE_USHORT_GRAY,
    TYPE_BYTE_BINARY,
    TYPE_BYTE_INDEXED;

    public static PngEncoderBufferedImageType valueOf(int bufferedImageTypeOrdinal) {
        return values()[bufferedImageTypeOrdinal];
    }

    public static PngEncoderBufferedImageType valueOf(BufferedImage bufferedImage) {
        return valueOf(bufferedImage.getType());
    }

    @Override
    public String toString() {
        return name() + "#" + ordinal();
    }
}
