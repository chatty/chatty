
package chatty.util.gif;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * With modifications by github.com/wsxjump
 * https://github.com/wsxjump/Open-Imaging/commit/f2dcc0b92eac8297c8156ddf068515f59f9d6c66#diff-9c60c8664ef5f4b3884d1c3af999c20f
 * 
 * And further modifications by tduva, not switching between img and prevImg
 * anymore, which failed for some images when not clearing the entire frame. I'm
 * not sure if this breaks things, but at least all tested GIFs seem to work the
 * same or better.
*/

/*
 * Copyright 2014 Dhyan Blum
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * <p>
 * A decoder capable of processing a GIF data stream to render the graphics
 * contained in it. This implementation follows the official <A
 * HREF="http://www.w3.org/Graphics/GIF/spec-gif89a.txt">GIF specification</A>.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <p>
 * 
 * <pre>
 * final GifImage gifImage = GifDecoder.read(int[] data);
 * final int width = gifImage.getWidth();
 * final int height = gifImage.getHeight();
 * final int frameCount = gifImage.getFrameCount();
 * for (int i = 0; i < frameCount; i++) {
 * 	final BufferedImage image = gifImage.getFrame(i);
 * 	final int delay = gif.getDelay(i);
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author Dhyan Blum
 * @version 1.07 October 2014
 * 
 */
public final class GifDecoder {
	final class BitReader {
		private int bitPos; // Next bit to read
		private byte[] in; // Data array

		// To avoid costly bounds checks, 'in' needs 2 more 0-bytes at the end
		private final void init(final byte[] in) {
			this.in = in;
			bitPos = 0;
		}

		private final int read(final int bits) {
			// Byte indices: (bitPos / 8), (bitPos / 8) + 1, (bitPos / 8) + 2
			int i = bitPos >>> 3; // Byte = bit / 8
			// Bits we'll shift to the right, AND 7 is the same as MODULO 8
			final int rBits = bitPos & 7;
			// Byte 0 to 2, AND to get their unsigned values
			final int b0 = in[i++] & 0xFF, b1 = in[i++] & 0xFF, b2 = in[i] & 0xFF;
			// Glue the bytes together, don't do more shifting than necessary
			final int buf = ((b2 << 8 | b1) << 8 | b0) >>> rBits;
			bitPos += bits;
			return buf & MASK[bits]; // Kill the unwanted higher bits
		}
	}

	final class CodeTable {
		private final int[][] tbl; // Maps codes to lists of colors
		private int initTableSize; // Number of colors +2 for CLEAR + EOI
		private int initCodeSize; // Initial code size
		private int initCodeLimit; // First code limit
		private int currCodeSize; // Current code size, maximum is 12 bits
		private int nextCode; // Next available code for a new entry
		private int nextCodeLimit; // Increase codeSize when nextCode == limit

		public CodeTable() {
			tbl = new int[4096][1];
		}

		private final int add(final int[] indices) {
			if (nextCode < 4096) {
				if (nextCode == nextCodeLimit && currCodeSize < 12) {
					currCodeSize++; // Max code size is 12
					nextCodeLimit = MASK[currCodeSize]; // 2^currCodeSize - 1
				}
				tbl[nextCode++] = indices;
			}
			return currCodeSize;
		}

		private final int clear() {
			currCodeSize = initCodeSize;
			nextCodeLimit = initCodeLimit;
			nextCode = initTableSize; // Don't recreate table, reset pointer
			return currCodeSize;
		}

		private final void init(final GifFrame fr, final int[] activeColTbl) {
			final int numColors = activeColTbl.length;
			initCodeSize = fr.firstCodeSize;
			initCodeLimit = MASK[initCodeSize]; // 2^initCodeSize - 1
			initTableSize = fr.endOfInfoCode + 1;
			nextCode = initTableSize;
			for (int c = numColors - 1; c >= 0; c--) {
				tbl[c][0] = activeColTbl[c]; // Translated color
			} // A gap may follow with no colors assigned if numCols < CLEAR
			tbl[fr.clearCode] = new int[] { fr.clearCode }; // CLEAR
			tbl[fr.endOfInfoCode] = new int[] { fr.endOfInfoCode }; // EOI
			// Locate transparent color in code table and set to 0
			if (fr.transpColFlag && fr.transpColIndex < numColors) {
				tbl[fr.transpColIndex][0] = 0;
			}
		}
	}

	final class GifFrame {
		// Graphic control extension (optional)
		// Disposal: 0=NO_ACTION, 1=NO_DISPOSAL, 2=RESTORE_BG, 3=RESTORE_PREV
		private int disposalMethod; // 0-3 as above, 4-7 undefined
		private boolean transpColFlag; // 1 Bit
		private int delay; // Unsigned, LSByte first, n * 1/100 * s
		private int transpColIndex; // 1 Byte
		// Image descriptor
		private int left; // Position on the canvas from the left
		private int top; // Position on the canvas from the top
		private int width; // May be smaller than the base image
		private int height; // May be smaller than the base image
		private boolean hasLocColTbl; // Has local color table? 1 Bit
		private boolean interlaceFlag; // Is an interlace image? 1 Bit
		@SuppressWarnings("unused")
		private boolean sortFlag; // True if local colors are sorted, 1 Bit
		private int sizeOfLocColTbl; // Size of the local color table, 3 Bits
		private int[] localColTbl; // Local color table (optional)
		// Image data
		private int firstCodeSize; // LZW minimum code size + 1 for CLEAR & EOI
		private int clearCode;
		private int endOfInfoCode;
		private byte[] data;
	}

	public final class GifImage {
		public String header; // Bytes 0-5, GIF87a or GIF89a
		private int width; // Unsigned 16 Bit, least significant byte first
		private int height; // Unsigned 16 Bit, least significant byte first
		private int wh; // width * height
		public boolean hasGlobColTbl; // 1 Bit
		public int colorResolution; // 3 Bits
		public boolean sortFlag; // True if global colors are sorted, 1 Bit
		public int sizeOfGlobColTbl; // 2^(val(3 Bits) + 1), see spec
		public int bgColIndex; // Background color index, 1 Byte
		public int pxAspectRatio; // Pixel aspect ratio, 1 Byte
		public int[] globalColTbl; // Global color table
		private final List<GifFrame> frames = new ArrayList<GifFrame>(48);
		public String appId = ""; // 8 Bytes at in[i+3], usually "NETSCAPE"
		public String appAuthCode = ""; // 3 Bytes at in[i+11], usually "2.0"
		public int repetitions = 0; // 0: infinite loop, N: number of loops
		private BufferedImage img = null; // Currently drawn frame
		private BufferedImage prevImg = null; // Last drawn frame
		private int prevIndex; // Index of the last drawn frame
		private int prevDisposal; // Disposal of the previous frame
		private int previmgwidth; // The real width of the previous frame
		private int previmgheight; // The real height of the previous frame
		private int previmgleft; // The left position of the previous frame
		private int previmgtop; // The top position of the previous frame
		private final BitReader in = new BitReader();
		private final CodeTable codes = new CodeTable();
		public int[] pxBuffer;

		private final int[] decode(final GifFrame fr, final int[] activeColTbl) {
			codes.init(fr, activeColTbl);
			in.init(fr.data); // Incoming codes
			final int clearCode = fr.clearCode, endCode = fr.endOfInfoCode;
			final int[] out = pxBuffer; // Target image pixel array
			final int[][] tbl = codes.tbl; // Code table
			int pxPos = 0; // Next pixel position in the output image array
			int currCodeSize = codes.clear(); // Init code table
			in.read(currCodeSize); // Skip leading clear code
			int code = in.read(currCodeSize); // Read first code
			int[] pixels = tbl[code]; // Output pixel for first code
			System.arraycopy(pixels, 0, out, pxPos, pixels.length);
			pxPos += pixels.length;
			try {
				while (true) {
					final int prevCode = code;
					code = in.read(currCodeSize); // Get next code in stream
					if (code == clearCode) { // After a CLEAR table, there is
						currCodeSize = codes.clear(); // No previous code, we
						code = in.read(currCodeSize); // need to read a new one
						pixels = tbl[code]; // Output pixels
						System.arraycopy(pixels, 0, out, pxPos, pixels.length);
						pxPos += pixels.length;
						continue; // Back to the loop with a valid previous code
					} else if (code == endCode) {
						break;
					}
					final int[] prevVals = tbl[prevCode];
					final int[] prevValsAndK = new int[prevVals.length + 1];
					System.arraycopy(prevVals, 0, prevValsAndK, 0,
							prevVals.length);
					if (code < codes.nextCode) { // Code table contains code
						pixels = tbl[code]; // Output pixels
						System.arraycopy(pixels, 0, out, pxPos, pixels.length);
						pxPos += pixels.length;
						prevValsAndK[prevVals.length] = tbl[code][0]; // K
					} else {
						prevValsAndK[prevVals.length] = prevVals[0]; // K
						System.arraycopy(prevValsAndK, 0, out, pxPos,
								prevValsAndK.length);
						pxPos += prevValsAndK.length;
					}
					currCodeSize = codes.add(prevValsAndK); // Previous indices
															// + K

				}
			} catch (final ArrayIndexOutOfBoundsException e) {
			}
			return out;
		}

		private final int[] deinterlace(final int[] pixels, final GifFrame fr) {
			final int w = fr.width, h = fr.height, group2, group3, group4;
			final int[] dest = new int[pixels.length];
			// Interlaced images are divided in 4 groups of pixel lines
			group2 = (h + 7) / 8; // Start index of group 2 = ceil(h/8.0)
			group3 = group2 + (h + 3) / 8; // Start index = ceil(h-4/8.0)
			group4 = group3 + (h + 1) / 4; // Start index = ceil(h-2/4.0)
			// Group 1 contains every 8th line starting from 0
			for (int y = 0; y < group2; y++) {
				final int destPos = w * y * 8;
				System.arraycopy(pixels, w * y, dest, destPos, w);
			} // Group 2 contains every 8th line starting from 4
			for (int y = group2; y < group3; y++) {
				final int destY = (y - group2) * 8 + 4, destPos = w * destY;
				System.arraycopy(pixels, w * y, dest, destPos, w);
			} // Group 3 contains every 4th line starting from 2
			for (int y = group3; y < group4; y++) {
				final int destY = (y - group3) * 4 + 2, destPos = w * destY;
				System.arraycopy(pixels, w * y, dest, destPos, w);
			} // Group 4 contains every 2nd line starting from 1 (biggest group)
			for (int y = group4; y < h; y++) {
				final int destY = (y - group4) * 2 + 1, destPos = w * destY;
				System.arraycopy(pixels, w * y, dest, destPos, w);
			}
			return dest; // All pixel lines have now been rearranged
		}

		private final void drawFrame(final GifFrame fr) {
			int bgCol = 0; // Current background color value
			final int[] activeColTbl; // Active color table
			if (fr.hasLocColTbl) {
				activeColTbl = fr.localColTbl;
			} else {
				activeColTbl = globalColTbl;
				if (!fr.transpColFlag) { // Only use background color if there
					bgCol = globalColTbl[bgColIndex]; // is no transparency
				}
			}
			// Handle disposal, prepare current BufferedImage for drawing
			switch (prevDisposal) {
                            case 2: // Next frame draws on background canvas
				final BufferedImage bgImage = img;
				final int[] px = getPixels(bgImage);
				//fill the prev frame area with background color,
				//NOT to fill the whole image or will cause bug 
				//when prev frame smaller than the whole image.
				final int bgimgwidth = bgImage.getWidth();
				int startpxindex=(previmgtop-1)*bgimgwidth+previmgleft;//minus 1 so that we can use "+=" in the loop
				for(int fillindex=0;fillindex<previmgheight;fillindex++){
					startpxindex+=bgimgwidth;
					Arrays.fill(px,startpxindex,startpxindex+previmgwidth,bgCol);
				}
				img = bgImage;
				break;
                            case 3: // Next frame draws on previous frame, so restore previous
				System.arraycopy(getPixels(prevImg), 0, getPixels(img), 0, wh);
				break;
                            default: // Next frame draws on current frame, so backup current
				System.arraycopy(getPixels(img), 0, getPixels(prevImg), 0, wh);
				break;
			}
			// Get pixels from data stream
			int[] pixels = decode(fr, activeColTbl);
			if (fr.interlaceFlag) {
				pixels = deinterlace(pixels, fr); // Rearrange pixel lines
			}
			// Draw pixels on top of current image
			final int w = fr.width, h = fr.height, numPixels = w * h;
			final BufferedImage frame = new BufferedImage(w, h, 2); // 2 = ARGB
			System.arraycopy(pixels, 0, getPixels(frame), 0, numPixels);
			final Graphics2D g = img.createGraphics();
			g.drawImage(frame, fr.left, fr.top, null);
			g.dispose();
		}

		/**
		 * Returns the background color of the first frame in this GIF image. If
		 * the frame has a local color table, the returned color will be from
		 * that table. If not, the color will be from the global color table.
		 * Returns 0 if there is neither a local nor a global color table.
		 * 
		 * @param index
		 *            Index of the current frame, 0 to N-1
		 * @return 32 bit ARGB color in the form 0xAARRGGBB
		 */
		public final int getBackgroundColor() {
			final GifFrame frame = frames.get(0);
			if (frame.hasLocColTbl) {
				return frame.localColTbl[bgColIndex];
			} else if (hasGlobColTbl) {
				return globalColTbl[bgColIndex];
			}
			return 0;
		}

		/**
		 * If not 0, the delay specifies how many hundredths (1/100) of a second
		 * to wait before displaying the frame <i>after</i> the current frame.
		 * 
		 * @param index
		 *            Index of the current frame, 0 to N-1
		 * @return Delay as number of hundredths (1/100) of a second
		 */
		public final int getDelay(final int index) {
			return frames.get(index).delay;
		}

		/**
		 * @param index
		 *            Index of the frame to return as image, starting from 0.
		 *            For indices greater than 0, it may be necessary to draw
		 *            the current frame on top of its previous frames. This
		 *            behavior depends on the disposal method encoded in the
		 *            image. This method's runtime therefore increases with
		 *            higher indices. However, the runtime increase will be
		 *            linear for calls with ascending indices. So don't request
		 *            frame i after requesting frame i+n, as all frames prior to
		 *            i might have to be redrawn. Use call sequences with
		 *            indices from 0 to N-1 instead.
		 * @return A BufferedImage for the specified frame.
		 */
		public final BufferedImage getFrame(final int index) {
			if (img == null || index < prevIndex) { // (Re)Init
				img = new BufferedImage(width, height, 2); // 2 = ARGB
				prevImg = new BufferedImage(width, height, 2);
				prevIndex = -1;
				prevDisposal = 2;
				previmgheight = height;
				previmgwidth = width ;
				previmgleft = 0;
				previmgtop = 0;
			}
			// Draw current frame on top of previous frames
			for (int i = prevIndex + 1; i <= index; i++) {
				final GifFrame fr = frames.get(i);
				drawFrame(fr);
				prevIndex = i;
				prevDisposal = fr.disposalMethod;
				previmgheight = fr.height;
				previmgwidth = fr.width ;
				previmgleft = fr.left;
				previmgtop = fr.top;
			}
			return img;
		}

		/**
		 * @return The number of frames contained in this GIF image
		 */
		public final int getFrameCount() {
			return frames.size();
		}

		/**
		 * @return The height of the GIF image
		 */
		public final int getHeight() {
			return height;
		}

		private final int[] getPixels(final BufferedImage img) {
			return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
		}

		/**
		 * @return The width of the GIF image
		 */
		public final int getWidth() {
			return width;
		}
	}

	// If used as a bitmask, the index tells how much lower bits remain.
	// May also be used to compute f(i) = (2^i) - 1.
	static final int[] MASK = new int[] { 0x00000000, 0x00000001, 0x00000003,
			0x00000007, 0x0000000F, 0x0000001F, 0x0000003F, 0x0000007F,
			0x000000FF, 0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF,
			0x00001FFF, 0x00003FFF, 0x00007FFF, 0x0000FFFF, 0x0001FFFF,
			0x0003FFFF, 0x0007FFFF, 0x000FFFFF, 0x001FFFFF, 0x003FFFFF,
			0x007FFFFF, 0x00FFFFFF, 0x01FFFFFF, 0x03FFFFFF, 0x07FFFFFF,
			0x0FFFFFFF, 0x1FFFFFFF, 0x3FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF };

	/**
	 * @param in
	 *            Raw image data as a byte[] array
	 * @return A GifImage object exposing the properties of the GIF image.
	 * @throws IOException
	 *             If the image violates the GIF specification or is truncated.
	 */
	public static final GifImage read(final byte[] in) throws IOException {
		final GifDecoder dec = new GifDecoder();
		final GifImage img = dec.new GifImage();
		final List<GifFrame> frames = img.frames; // Local access is faster
		GifFrame frame = null; // Currently open frame
		int pos = readHeader(in, img); // Read header, get next byte position
		pos = readLogicalScreenDescriptor(img, in, pos);
		if (img.hasGlobColTbl) {
			img.globalColTbl = new int[img.sizeOfGlobColTbl];
			pos = readColTbl(in, img.globalColTbl, pos);
		}
		while (pos < in.length) {
			final int block = in[pos] & 0xFF;
			switch (block) {
			case 0x21: // Extension introducer
				if (pos + 1 >= in.length) {
					throw new IOException("Unexpected end of file.");
				}
				switch (in[pos + 1] & 0xFF) {
				case 0xFE: // Comment extension
					pos = readTextExtension(in, pos);
					break;
				case 0xFF: // Application extension
					pos = readAppExt(img, in, pos);
					break;
				case 0x01: // Plain text extension
					frame = null; // End of current frame
					pos = readTextExtension(in, pos);
					break;
				case 0xF9: // Graphic control extension
					if (frame == null) {
						frame = dec.new GifFrame();
						frames.add(frame);
					}
					pos = readGraphicControlExt(frame, in, pos);
					break;
				default:
					throw new IOException("Unknown extension at " + pos);
				}
				break;
			case 0x2C: // Image descriptor
				if (frame == null) {
					frame = dec.new GifFrame();
					frames.add(frame);
				}
				pos = readImgDescr(frame, in, pos);
				if (frame.hasLocColTbl) {
					frame.localColTbl = new int[frame.sizeOfLocColTbl];
					pos = readColTbl(in, frame.localColTbl, pos);
				}
				pos = readImgData(frame, in, pos);
				frame = null; // End of current frame
				break;
			case 0x3B: // GIF Trailer
				return img; // Found trailer, finished reading.
			default:
				// Unknown block. The image is corrupted. Strategies: a) Skip
				// and wait for a valid block. Experience: It'll get worse. b)
				// Throw exception. c) Return gracefully if we are almost done
				// processing. The frames we have so far should be error-free.
				final double progress = 1.0 * pos / in.length;
				if (progress < 0.9) {
					throw new IOException("Unknown block at: " + pos);
				}
				pos = in.length; // Exit loop
			}
		}
		return img;
	}

	/**
	 * @param is
	 *            Image data as input stream. This method will read from the
	 *            input stream's current position. It will not reset the
	 *            position before reading and won't reset or close the stream
	 *            afterwards. Call these methods before and after calling this
	 *            method as needed.
	 * @return A GifImage object exposing the properties of the GIF image.
	 * @throws IOException
	 *             If an I/O error occurs, the image violates the GIF
	 *             specification or the GIF is truncated.
	 */
	public static final GifImage read(final InputStream is) throws IOException {
		final int numBytes = is.available();
		final byte[] data = new byte[numBytes];
		is.read(data, 0, numBytes);
		return read(data);
	}

	/**
	 * @param ext
	 *            Empty application extension object
	 * @param in
	 *            Raw data
	 * @param i
	 *            Index of the first byte of the application extension
	 * @return Index of the first byte after this extension
	 */
	static final int readAppExt(final GifImage img, final byte[] in, int i) {
		img.appId = new String(in, i + 3, 8); // should be "NETSCAPE"
		img.appAuthCode = new String(in, i + 11, 3); // should be "2.0"
		i += 14; // Go to sub-block size, it's value should be 3
		final int subBlockSize = in[i] & 0xFF;
		// The only app extension widely used is NETSCAPE, it's got 3 data bytes
		if (subBlockSize == 3) {
			// in[i+1] should have value 01, in[i+5] should be block terminator
			img.repetitions = in[i + 2] & 0xFF | in[i + 3] & 0xFF << 8; // Short
			return i + 5;
		} // Skip unknown application extensions
		while ((in[i] & 0xFF) != 0) { // While sub-block size != 0
			i += (in[i] & 0xFF) + 1; // Skip to next sub-block
		}
		return i + 1;
	}

	/**
	 * @param in
	 *            Raw data
	 * @param colors
	 *            Pre-initialized target array to store ARGB colors
	 * @param i
	 *            Index of the color table's first byte
	 * @return Index of the first byte after the color table
	 */
	static final int readColTbl(final byte[] in, final int[] colors, int i) {
		final int numColors = colors.length;
		for (int c = 0; c < numColors; c++) {
			final int a = 0xFF; // Alpha 255 (opaque)
			final int r = in[i++] & 0xFF; // 1st byte is red
			final int g = in[i++] & 0xFF; // 2nd byte is green
			final int b = in[i++] & 0xFF; // 3rd byte is blue
			colors[c] = ((a << 8 | r) << 8 | g) << 8 | b;
		}
		return i;
	}

	/**
	 * @param ext
	 *            Graphic control extension object
	 * @param in
	 *            Raw data
	 * @param i
	 *            Index of the extension introducer
	 * @return Index of the first byte after this block
	 */
	static final int readGraphicControlExt(final GifFrame fr, final byte[] in,
			final int i) {
		fr.disposalMethod = (in[i + 3] & 0b00011100) >>> 2; // Bits 4-2
		fr.transpColFlag = (in[i + 3] & 1) == 1; // Bit 0
		fr.delay = in[i + 4] & 0xFF | (in[i + 5] & 0xFF) << 8; // 16 bit LSB
		fr.transpColIndex = in[i + 6] & 0xFF; // Byte 6
		return i + 8; // Skipped byte 7 (blockTerminator), as it's always 0x00
	}

	/**
	 * @param in
	 *            Raw data
	 * @param img
	 *            The GifImage object that is currently read
	 * @return Index of the first byte after this block
	 * @throws IOException
	 *             If the GIF header/trailer is missing, incomplete or unknown
	 */
	static final int readHeader(final byte[] in, final GifImage img)
			throws IOException {
		if (in.length < 6) { // Check first 6 bytes
			throw new IOException("Image is truncated.");
		}
		img.header = new String(in, 0, 6);
		if (!img.header.equals("GIF87a") && !img.header.equals("GIF89a")) {
			throw new IOException("Invalid GIF header.");
		}
		return 6;
	}

	/**
	 * @param fr
	 *            The GIF frame to whom this image descriptor belongs
	 * @param in
	 *            Raw data
	 * @param i
	 *            Index of the first byte of this block, i.e. the minCodeSize
	 * @return
	 */
	static final int readImgData(final GifFrame fr, final byte[] in, int i) {
		final int fileSize = in.length;
		final int minCodeSize = in[i++] & 0xFF; // Read code size, go to block
		final int clearCode = 1 << minCodeSize; // CLEAR = 2^minCodeSize
		fr.firstCodeSize = minCodeSize + 1; // Add 1 bit for CLEAR and EOI
		fr.clearCode = clearCode;
		fr.endOfInfoCode = clearCode + 1;
		final int imgDataSize = readImgDataSize(in, i);
		final byte[] imgData = new byte[imgDataSize + 2];
		int imgDataPos = 0;
		int subBlockSize = in[i] & 0xFF;
		while (subBlockSize > 0) { // While block has data
			try { // Next line may throw exception if sub-block size is fake
				final int nextSubBlockSizePos = i + subBlockSize + 1;
				final int nextSubBlockSize = in[nextSubBlockSizePos] & 0xFF;
				System.arraycopy(in, i + 1, imgData, imgDataPos, subBlockSize);
				imgDataPos += subBlockSize; // Move output data position
				i = nextSubBlockSizePos; // Move to next sub-block size
				subBlockSize = nextSubBlockSize;
			} catch (final Exception e) {
				// Sub-block exceeds file end, only use remaining bytes
				subBlockSize = fileSize - i - 1; // Remaining bytes
				System.arraycopy(in, i + 1, imgData, imgDataPos, subBlockSize);
				imgDataPos += subBlockSize; // Move output data position
				i += subBlockSize + 1; // Move to next sub-block size
				break;
			}
		}
		fr.data = imgData; // Holds LZW encoded data
		i++; // Skip last sub-block size, should be 0
		return i;
	}

	static final int readImgDataSize(final byte[] in, int i) {
		final int fileSize = in.length;
		int imgDataPos = 0;
		int subBlockSize = in[i] & 0xFF;
		while (subBlockSize > 0) { // While block has data
			try { // Next line may throw exception if sub-block size is fake
				final int nextSubBlockSizePos = i + subBlockSize + 1;
				final int nextSubBlockSize = in[nextSubBlockSizePos] & 0xFF;
				imgDataPos += subBlockSize; // Move output data position
				i = nextSubBlockSizePos; // Move to next sub-block size
				subBlockSize = nextSubBlockSize;
			} catch (final Exception e) {
				// Sub-block exceeds file end, only use remaining bytes
				subBlockSize = fileSize - i - 1; // Remaining bytes
				imgDataPos += subBlockSize; // Move output data position
				break;
			}
		}
		return imgDataPos;
	}

	/**
	 * @param fr
	 *            The GIF frame to whom this image descriptor belongs
	 * @param in
	 *            Raw data
	 * @param i
	 *            Index of the image separator, i.e. the first block byte
	 * @return Index of the first byte after this block
	 */
	static final int readImgDescr(final GifFrame fr, final byte[] in, int i) {
		fr.left = in[++i] & 0xFF | (in[++i] & 0xFF) << 8; // Byte 1-2
		fr.top = in[++i] & 0xFF | (in[++i] & 0xFF) << 8; // Byte 3-4
		fr.width = in[++i] & 0xFF | (in[++i] & 0xFF) << 8; // Byte 5-6
		fr.height = in[++i] & 0xFF | (in[++i] & 0xFF) << 8; // Byte 7-8
		final byte b = in[++i]; // Byte 9 is a packed byte
		fr.hasLocColTbl = (b & 0b10000000) >>> 7 == 1; // Bit 7
		fr.interlaceFlag = (b & 0b01000000) >>> 6 == 1; // Bit 6
		fr.sortFlag = (b & 0b00100000) >>> 5 == 1; // Bit 5
		final int colTblSizePower = (b & 7) + 1; // Bits 2-0
		fr.sizeOfLocColTbl = 1 << colTblSizePower; // 2^(N+1), As per the spec
		return ++i;
	}

	/**
	 * @param img
	 * @param i
	 *            Start index of this block.
	 * @return Index of the first byte after this block.
	 */
	static final int readLogicalScreenDescriptor(final GifImage img,
			final byte[] in, final int i) {
		img.width = in[i] & 0xFF | (in[i + 1] & 0xFF) << 8; // 16 bit, LSB 1st
		img.height = in[i + 2] & 0xFF | (in[i + 3] & 0xFF) << 8; // 16 bit
		img.wh = img.width * img.height;
		img.pxBuffer = new int[img.wh]; // Output pixel buffer
		final byte b = in[i + 4]; // Byte 4 is a packed byte
		img.hasGlobColTbl = (b & 0b10000000) >>> 7 == 1; // Bit 7
		final int colResPower = ((b & 0b01110000) >>> 4) + 1; // Bits 6-4
		img.colorResolution = 1 << colResPower; // 2^(N+1), As per the spec
		img.sortFlag = (b & 0b00001000) >>> 3 == 1; // Bit 3
		final int globColTblSizePower = (b & 7) + 1; // Bits 0-2
		img.sizeOfGlobColTbl = 1 << globColTblSizePower; // 2^(N+1), see spec
		img.bgColIndex = in[i + 5] & 0xFF; // 1 Byte
		img.pxAspectRatio = in[i + 6] & 0xFF; // 1 Byte
		return i + 7;
	}

	/**
	 * @param in
	 *            Raw data
	 * @param pos
	 *            Index of the extension introducer
	 * @return Index of the first byte after this block
	 */
	static final int readTextExtension(final byte[] in, final int pos) {
		int i = pos + 2; // Skip extension introducer and label
		int subBlockSize = in[i++] & 0xFF;
		while (subBlockSize != 0 && i < in.length) {
			i += subBlockSize;
			subBlockSize = in[i++] & 0xFF;
		}
		return i;
	}
}