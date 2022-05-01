package com.pngencoder;

public enum PngEncoderSrgbRenderingIntent {
    PERCEPTUAL((byte) 0),
    RELATIVE_COLORIMETRIC((byte) 1),
    SATURATION((byte) 2),
    ABSOLUTE_COLORIMETRIC((byte) 3);

    private final byte value;

    PngEncoderSrgbRenderingIntent(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
