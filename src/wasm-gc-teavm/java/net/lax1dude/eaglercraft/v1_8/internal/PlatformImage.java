/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.lax1dude.eaglercraft.v1_8.internal;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * WASM-GC implementation of the PlatformImage seam. See the platform-api copy for
 * why it exists.
 *
 * Pure Java throughout: the classlib models java.util.zip.{Inflater,Deflater,CRC32}
 * but there is no javax.imageio and no stb_image, so PNG is decoded and encoded here
 * from the spec. Every texture Minecraft loads -- the block and item atlases above
 * all -- goes through {@link #decodePng}, so this is on the critical path to the
 * title screen.
 *
 * Scope, chosen to match what Mojang actually ships:
 *   decode: bit depths 1/2/4/8/16; colour types 0/2/3/4/6; all five scanline
 *           filters; tRNS for greyscale, truecolour and palette; multiple IDAT
 *           chunks. Interlacing is REFUSED rather than implemented -- nothing in
 *           the vanilla assets is interlaced, and a garbled atlas is worse than a
 *           clear error.
 *   encode: 8 bits per component, colour type chosen from the component count.
 *   resize: a triangle (tent) filter, separable. STB's stbir is also a triangle
 *           filter but with a different support rule, so this is not
 *           bit-identical; it is used only for the auto-screenshot thumbnail,
 *           where that does not matter. The contract matches vanilla
 *           NativeImage.resizeSubRectTo: source rectangle (srcX, srcY, srcW,
 *           srcH) is cropped out of src and scaled to the destination image's
 *           own width and height.
 */
public class PlatformImage {

	private static final byte[] SIG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	// PNG chunk tags, compared as big-endian ints. A string switch would work but
	// this is on the per-file path and this way needs no interning.
	private static final int TAG_IHDR = 0x49484452;
	private static final int TAG_PLTE = 0x504C5445;
	private static final int TAG_tRNS = 0x74524E53;
	private static final int TAG_IDAT = 0x49444154;
	private static final int TAG_IEND = 0x49454E44;

	private static String failure = "no decode attempted";

	public static String decodeFailure() {
		return failure;
	}

	// ------------------------------------------------------------------ decode

	public static byte[] decodePng(byte[] data, int[] outDims, int requiredComponents) {
		failure = "unknown";
		if (data == null) {
			failure = "null input";
			return null;
		}

		if (requiredComponents != 0 && (requiredComponents < 1 || requiredComponents > 4)) {
			failure = "requiredComponents must be 0 or 1..4, got " + requiredComponents;
			return null;
		}

		try {
			return decode0(data, outDims, requiredComponents);
		} catch (ArrayIndexOutOfBoundsException | NegativeArraySizeException | ArithmeticException e) {
			// A malformed file is attacker-controlled data, not a bug: report it the
			// same way stb_image does instead of letting the exception escape into
			// NativeImage.read, whose callers only check for a null return.
			failure = "malformed PNG (" + e.getClass().getSimpleName() + ")";
			return null;
		}
	}

	private static byte[] decode0(byte[] d, int[] outDims, int requiredComponents) {
		if (d.length < 8) {
			failure = "file too short to be a PNG";
			return null;
		}

		for (int i = 0; i < 8; ++i) {
			if (d[i] != SIG[i]) {
				failure = "not a PNG (bad signature)";
				return null;
			}
		}

		int width = -1;
		int height = -1;
		int bitDepth = -1;
		int colourType = -1;
		byte[] palette = null;
		byte[] trns = null;
		byte[] idat = null;

		int pos = 8;
		while (pos + 8 <= d.length) {
			int len = readInt(d, pos);
			int tag = readInt(d, pos + 4);
			int start = pos + 8;
			if (len < 0 || start + len + 4 > d.length) {
				failure = "chunk at offset " + pos + " overruns the file";
				return null;
			}

			if (tag == TAG_IHDR) {
				if (len < 13) {
					failure = "IHDR too short";
					return null;
				}

				width = readInt(d, start);
				height = readInt(d, start + 4);
				bitDepth = d[start + 8] & 255;
				colourType = d[start + 9] & 255;
				int compression = d[start + 10] & 255;
				int filterMethod = d[start + 11] & 255;
				int interlace = d[start + 12] & 255;

				if (compression != 0) {
					failure = "unsupported compression method " + compression;
					return null;
				}

				if (filterMethod != 0) {
					failure = "unsupported filter method " + filterMethod;
					return null;
				}

				if (interlace != 0) {
					failure = "interlaced PNGs are not supported";
					return null;
				}

				if (width <= 0 || height <= 0) {
					failure = "bad dimensions " + width + "x" + height;
					return null;
				}

				// Refuse before allocating rather than after. 2^28 pixels is far more
				// than any Minecraft texture, and the multiplication is done in long
				// because width*height*channels*depth can exceed int range.
				if ((long) width * (long) height > 268435456L) {
					failure = "image too large (" + width + "x" + height + ")";
					return null;
				}
			} else if (tag == TAG_PLTE) {
				palette = new byte[len];
				System.arraycopy(d, start, palette, 0, len);
			} else if (tag == TAG_tRNS) {
				trns = new byte[len];
				System.arraycopy(d, start, trns, 0, len);
			} else if (tag == TAG_IDAT) {
				idat = concat(idat, d, start, len);
			} else if (tag == TAG_IEND) {
				break;
			}

			pos = start + len + 4;
		}

		if (width < 0) {
			failure = "no IHDR chunk";
			return null;
		}

		int channels = channelsFor(colourType);
		if (channels == 0) {
			failure = "unsupported colour type " + colourType;
			return null;
		}

		if (!depthValid(colourType, bitDepth)) {
			failure = "bit depth " + bitDepth + " is not valid for colour type " + colourType;
			return null;
		}

		if (colourType == 3 && palette == null) {
			failure = "palette image with no PLTE chunk";
			return null;
		}

		if (idat == null) {
			failure = "no IDAT chunks";
			return null;
		}

		int outComp = requiredComponents == 0 ? channels : requiredComponents;
		byte[] out = expand(d, width, height, bitDepth, colourType, channels, palette, trns, idat, outComp);
		if (out == null) {
			return null;
		}

		outDims[0] = width;
		outDims[1] = height;
		failure = null;
		return out;
	}

	/**
	 * Inflates the IDAT payload, reverses the per-scanline filters, and expands each
	 * sample out to outComp channels. Returns null with {@link #failure} set.
	 */
	private static byte[] expand(byte[] d, int width, int height, int bitDepth, int colourType, int channels,
			byte[] palette, byte[] trns, byte[] idat, int outComp) {
		int bpp = Math.max(1, channels * bitDepth / 8);
		int stride = (width * channels * bitDepth + 7) / 8;
		int rowBytes = stride + 1;

		byte[] raw = new byte[height * rowBytes];
		Inflater inf = new Inflater();
		int off = 0;
		try {
			inf.setInput(idat);
			while (off < raw.length) {
				int n;
				try {
					n = inf.inflate(raw, off, raw.length - off);
				} catch (DataFormatException e) {
					failure = "corrupt deflate stream: " + e.getMessage();
					return null;
				}

				if (n == 0) {
					if (inf.finished() || inf.needsInput()) {
						break;
					}

					// Neither finished nor starved and produced nothing: cannot make
					// progress, so stop rather than spin.
					break;
				}

				off += n;
			}
		} finally {
			inf.end();
		}

		if (off < raw.length) {
			failure = "truncated image data (inflated " + off + " of " + raw.length + " bytes)";
			return null;
		}

		unfilter(raw, height, stride, rowBytes, bpp);

		int maxVal = (1 << bitDepth) - 1;
		int trnsGrey = -1;
		int trnsR = -1;
		int trnsG = -1;
		int trnsB = -1;
		if (trns != null) {
			if (colourType == 0 && trns.length >= 2) {
				trnsGrey = readU16(trns, 0);
			} else if (colourType == 2 && trns.length >= 6) {
				trnsR = readU16(trns, 0);
				trnsG = readU16(trns, 2);
				trnsB = readU16(trns, 4);
			}
		}

		byte[] out = new byte[width * height * outComp];
		int o = 0;

		for (int y = 0; y < height; ++y) {
			int rowBase = y * rowBytes + 1;
			for (int x = 0; x < width; ++x) {
				int s0;
				int s1 = 0;
				int s2 = 0;
				int s3 = 0;

				if (bitDepth == 16) {
					int p = rowBase + x * channels * 2;
					s0 = raw[p] & 255;
					if (channels > 1) {
						s1 = raw[p + 2] & 255;
					}

					if (channels > 2) {
						s2 = raw[p + 4] & 255;
					}

					if (channels > 3) {
						s3 = raw[p + 6] & 255;
					}
				} else if (bitDepth == 8) {
					int p = rowBase + x * channels;
					s0 = raw[p] & 255;
					if (channels > 1) {
						s1 = raw[p + 1] & 255;
					}

					if (channels > 2) {
						s2 = raw[p + 2] & 255;
					}

					if (channels > 3) {
						s3 = raw[p + 3] & 255;
					}
				} else {
					// 1, 2 or 4 bits. Only colour types 0 and 3 can reach here, and both
					// have exactly one channel, so the sample is a single packed value.
					int bitPos = x * bitDepth;
					int byteIdx = rowBase + (bitPos >> 3);
					int shift = 8 - bitDepth - (bitPos & 7);
					s0 = (raw[byteIdx] >> shift) & maxVal;
				}

				int r;
				int g;
				int b;
				int a = 255;

				if (colourType == 0) {
					r = g = b = scale(s0, bitDepth, maxVal);
					// tRNS is a 16-bit grey level; compare against the sample widened to
					// 16 bits so one comparison covers every depth.
					if (trnsGrey >= 0 && s0 * 65535 / maxVal == trnsGrey) {
						a = 0;
					}
				} else if (colourType == 2) {
					r = scale(s0, bitDepth, maxVal);
					g = scale(s1, bitDepth, maxVal);
					b = scale(s2, bitDepth, maxVal);
					if (trnsR >= 0 && s0 * 65535 / maxVal == trnsR && s1 * 65535 / maxVal == trnsG
							&& s2 * 65535 / maxVal == trnsB) {
						a = 0;
					}
				} else if (colourType == 3) {
					int pi = s0 * 3;
					if (pi + 2 >= palette.length) {
						failure = "palette index " + s0 + " outside PLTE";
						return null;
					}

					r = palette[pi] & 255;
					g = palette[pi + 1] & 255;
					b = palette[pi + 2] & 255;
					// For a palette, tRNS is one alpha per index, not a colour.
					if (trns != null && s0 < trns.length) {
						a = trns[s0] & 255;
					}
				} else if (colourType == 4) {
					r = g = b = scale(s0, bitDepth, maxVal);
					a = scale(s1, bitDepth, maxVal);
				} else {
					r = scale(s0, bitDepth, maxVal);
					g = scale(s1, bitDepth, maxVal);
					b = scale(s2, bitDepth, maxVal);
					a = scale(s3, bitDepth, maxVal);
				}

				// Emit in the requested channel count, using stb_image's luminance
				// weights so a forced grey conversion matches what NativeImage used to
				// produce.
				switch (outComp) {
					case 1:
						out[o++] = (byte)luma(r, g, b);
						break;
					case 2:
						out[o++] = (byte)luma(r, g, b);
						out[o++] = (byte)a;
						break;
					case 3:
						out[o++] = (byte)r;
						out[o++] = (byte)g;
						out[o++] = (byte)b;
						break;
					default:
						out[o++] = (byte)r;
						out[o++] = (byte)g;
						out[o++] = (byte)b;
						out[o++] = (byte)a;
						break;
				}
			}
		}

		return out;
	}

	/** Reverses the five PNG scanline filters, in place. */
	private static void unfilter(byte[] raw, int height, int stride, int rowBytes, int bpp) {
		for (int y = 0; y < height; ++y) {
			int ro = y * rowBytes;
			int filter = raw[ro] & 255;
			int off = ro + 1;
			int prev = off - rowBytes;

			switch (filter) {
				case 0:
					break;
				case 1:
					for (int x = bpp; x < stride; ++x) {
						raw[off + x] += raw[off + x - bpp];
					}
					break;
				case 2:
					if (y > 0) {
						for (int x = 0; x < stride; ++x) {
							raw[off + x] += raw[prev + x];
						}
					}
					break;
				case 3:
					for (int x = 0; x < stride; ++x) {
						int a = x >= bpp ? raw[off + x - bpp] & 255 : 0;
						int b = y > 0 ? raw[prev + x] & 255 : 0;
						raw[off + x] += (byte)((a + b) / 2);
					}
					break;
				case 4:
					for (int x = 0; x < stride; ++x) {
						int a = x >= bpp ? raw[off + x - bpp] & 255 : 0;
						int b = y > 0 ? raw[prev + x] & 255 : 0;
						int c = x >= bpp && y > 0 ? raw[prev + x - bpp] & 255 : 0;
						raw[off + x] += (byte)paeth(a, b, c);
					}
					break;
				default:
					// Unknown filter type: leave the row as-is. A decoder that guesses
					// here produces plausible garbage; the caller's CRC check upstream
					// means this only happens on a genuinely broken file.
					break;
			}
		}
	}

	private static int paeth(int a, int b, int c) {
		int p = a + b - c;
		int pa = p > a ? p - a : a - p;
		int pb = p > b ? p - b : b - p;
		int pc = p > c ? p - c : c - p;
		if (pa <= pb && pa <= pc) {
			return a;
		}

		return pb <= pc ? b : c;
	}

	/** Widens a sample of the given bit depth to 0..255. */
	private static int scale(int sample, int bitDepth, int maxVal) {
		if (bitDepth == 8) {
			return sample;
		}

		if (bitDepth == 16) {
			return sample;
		}

		return sample * 255 / maxVal;
	}

	/** stb_image's stbi__compute_y, so a forced greyscale matches the old output. */
	private static int luma(int r, int g, int b) {
		return (r * 77 + g * 150 + b * 29) >> 8;
	}

	private static int channelsFor(int colourType) {
		switch (colourType) {
			case 0:
				return 1;
			case 2:
				return 3;
			case 3:
				return 1;
			case 4:
				return 2;
			case 6:
				return 4;
			default:
				return 0;
		}
	}

	private static boolean depthValid(int colourType, int bitDepth) {
		switch (colourType) {
			case 0:
				return bitDepth == 1 || bitDepth == 2 || bitDepth == 4 || bitDepth == 8 || bitDepth == 16;
			case 3:
				// Palette indices are never 16 bit.
				return bitDepth == 1 || bitDepth == 2 || bitDepth == 4 || bitDepth == 8;
			case 2:
			case 4:
			case 6:
				return bitDepth == 8 || bitDepth == 16;
			default:
				return false;
		}
	}

	// ------------------------------------------------------------------ encode

	public static byte[] encodePng(byte[] pixels, int width, int height, int components) {
		if (pixels == null || width <= 0 || height <= 0) {
			return null;
		}

		int colourType;
		switch (components) {
			case 1:
				colourType = 0;
				break;
			case 2:
				colourType = 4;
				break;
			case 3:
				colourType = 2;
				break;
			case 4:
				colourType = 6;
				break;
			default:
				return null;
		}

		if ((long) width * height * components > pixels.length) {
			return null;
		}

		int rowBytes = width * components;
		byte[] raw = new byte[height * (rowBytes + 1)];
		for (int y = 0; y < height; ++y) {
			// Filter 0 (None). The encoders here run for the odd screenshot, where a
			// larger file is a non-issue; choosing per-row filters adaptively would buy
			// compression at the cost of a much harder-to-verify code path.
			raw[y * (rowBytes + 1)] = 0;
			System.arraycopy(pixels, y * rowBytes, raw, y * (rowBytes + 1) + 1, rowBytes);
		}

		byte[] deflated;
		Deflater def = new Deflater();
		try {
			def.setInput(raw);
			def.finish();
			byte[] buf = new byte[Math.max(1024, raw.length / 2)];
			byte[] acc = new byte[0];
			while (!def.finished()) {
				int n = def.deflate(buf);
				if (n == 0) {
					break;
				}

				acc = concat(acc, buf, 0, n);
			}

			deflated = acc;
		} finally {
			def.end();
		}

		byte[] ihdr = new byte[13];
		putInt(ihdr, 0, width);
		putInt(ihdr, 4, height);
		ihdr[8] = 8;
		ihdr[9] = (byte)colourType;
		ihdr[10] = 0;
		ihdr[11] = 0;
		ihdr[12] = 0;

		int total = 8 + (12 + 13) + (12 + deflated.length) + 12;
		byte[] out = new byte[total];
		int o = 0;
		System.arraycopy(SIG, 0, out, o, 8);
		o += 8;
		o = chunk(out, o, TAG_IHDR, ihdr);
		o = chunk(out, o, TAG_IDAT, deflated);
		o = chunk(out, o, TAG_IEND, new byte[0]);
		return out;
	}

	private static int chunk(byte[] out, int o, int tag, byte[] data) {
		putInt(out, o, data.length);
		putInt(out, o + 4, tag);
		System.arraycopy(data, 0, out, o + 8, data.length);

		CRC32 crc = new CRC32();
		crc.update(out, o + 4, 4 + data.length);
		putInt(out, o + 8 + data.length, (int)crc.getValue());
		return o + 12 + data.length;
	}

	// ------------------------------------------------------------------ resize

	// Contract mirrors vanilla NativeImage.resizeSubRectTo: the rectangle
	// (srcX, srcY, srcW, srcH) is cropped out of the source plane and scaled to
	// (dstW, dstH) with a separable tent filter. srcStride is the FULL source
	// row stride; the crop anchors are honoured in both passes. Only the
	// auto-screenshot thumbnail uses this, so allocation churn is acceptable.
	public static void resize(byte[] src, int srcX, int srcY, int srcW, int srcH, int srcStride, int components,
			byte[] dst, int dstW, int dstH) {
		if (dst == null || src == null || components < 1 || components > 4) {
			return;
		}

		if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0 || srcX < 0 || srcY < 0) {
			return;
		}

		if (srcX + srcW > srcStride / components || srcY + srcH > src.length / srcStride) {
			// Crop rectangle overruns the source plane - refuse rather than
			// reading out of bounds (throwing here would kill the IO task that
			// saves the world thumbnail and make the caller retry forever).
			return;
		}

		// Horizontal pass first: srcW x srcH -> dstW x srcH. That intermediate
		// is dstW*srcH*comps, the smaller of the two orderings whenever the
		// destination is a thumbnail -- the only caller.
		int midW = dstW;
		int midH = srcH;
		int[] mid = new int[midW * midH * components];

		Weights wx = weights(dstW, srcW);
		for (int y = 0; y < midH; ++y) {
			int srcRow = (srcY + y) * srcStride + srcX * components;
			int midRow = y * midW * components;
			for (int x = 0; x < midW; ++x) {
				int first = wx.start[x];
				int n = wx.count[x];
				for (int c = 0; c < components; ++c) {
					int acc = 0;
					for (int k = 0; k < n; ++k) {
						int si = clamp(first + k, srcW);
						acc += Math.round((src[srcRow + si * components + c] & 255) * wx.val[wx.offset[x] + k]);
					}

					mid[midRow + x * components + c] = acc;
				}
			}
		}

		// Vertical pass: dstW x srcH -> dstW x dstH, into the destination's own
		// full-image coordinate space (the caller passes a whole-image dst).
		int dstStride = dstW * components;
		Weights wy = weights(dstH, srcH);
		for (int y = 0; y < dstH; ++y) {
			int first = wy.start[y];
			int n = wy.count[y];
			int outRow = y * dstStride;
			for (int x = 0; x < dstW; ++x) {
				for (int c = 0; c < components; ++c) {
					int acc = 0;
					for (int k = 0; k < n; ++k) {
						int si = clamp(first + k, midH);
						acc += Math.round(mid[si * midW * components + x * components + c]
								* wy.val[wy.offset[y] + k]);
					}

					dst[outRow + x * components + c] = (byte)Math.min(255, Math.max(0, acc));
				}
			}
		}
	}

	private static int clamp(int i, int limit) {
		return i < 0 ? 0 : i >= limit ? limit - 1 : i;
	}

	/**
	 * Triangle (tent) filter taps for each output sample. The tap count varies per
	 * output sample -- support widens when minifying -- so the weights live in one
	 * flat array with a per-sample offset, rather than in a fixed-stride pair.
	 */
	private static final class Weights {

		private final int[] start;
		private final int[] count;
		private final int[] offset;
		private final float[] val;

		private Weights(int[] start, int[] count, int[] offset, float[] val) {
			this.start = start;
			this.count = count;
			this.offset = offset;
			this.val = val;
		}
	}

	/**
	 * Support widens when minifying, which is what stops a downscale from aliasing:
	 * a fixed one-pixel support would sample far fewer input pixels than the area
	 * being collapsed and would drop detail outright. Weights are normalised so the
	 * kernel has unit gain, and edge taps are clamped (not dropped) so a pixel at the
	 * border keeps its correct brightness.
	 */
	private static Weights weights(int dstSize, int srcSize) {
		double scale = (double)dstSize / (double)srcSize;
		double support = scale < 1.0 ? 1.0 / scale : 1.0;

		int[] start = new int[dstSize];
		int[] count = new int[dstSize];
		int[] offset = new int[dstSize];

		int total = 0;
		for (int i = 0; i < dstSize; ++i) {
			double centre = (i + 0.5) / scale - 0.5;
			int left = (int)Math.ceil(centre - support);
			int right = (int)Math.floor(centre + support);
			if (right < left) {
				right = left;
			}

			start[i] = left;
			count[i] = right - left + 1;
			offset[i] = total;
			total += count[i];
		}

		float[] val = new float[total];
		for (int i = 0; i < dstSize; ++i) {
			double centre = (i + 0.5) / scale - 0.5;
			float sum = 0.0f;
			for (int k = 0; k < count[i]; ++k) {
				double t = support - Math.abs(start[i] + k - centre);
				if (t < 0.0) {
					t = 0.0;
				}

				sum += (float)t;
			}

			if (sum <= 0.0f) {
				// Degenerate: every tap zeroed. Fall back to nearest-neighbour so the
				// caller still gets a defined image instead of an unnormalised one.
				val[offset[i]] = 1.0f;
				continue;
			}

			for (int k = 0; k < count[i]; ++k) {
				double t = support - Math.abs(start[i] + k - centre);
				if (t < 0.0) {
					t = 0.0;
				}

				val[offset[i] + k] = (float)(t / sum);
			}
		}

		return new Weights(start, count, offset, val);
	}

	// ------------------------------------------------------------------ helpers

	private static int readInt(byte[] b, int o) {
		return ((b[o] & 255) << 24) | ((b[o + 1] & 255) << 16) | ((b[o + 2] & 255) << 8) | (b[o + 3] & 255);
	}

	private static int readU16(byte[] b, int o) {
		return ((b[o] & 255) << 8) | (b[o + 1] & 255);
	}

	private static void putInt(byte[] b, int o, int v) {
		b[o] = (byte)(v >>> 24);
		b[o + 1] = (byte)(v >>> 16);
		b[o + 2] = (byte)(v >>> 8);
		b[o + 3] = (byte)v;
	}

	private static byte[] concat(byte[] a, byte[] b, int off, int len) {
		if (a == null) {
			byte[] r = new byte[len];
			System.arraycopy(b, off, r, 0, len);
			return r;
		}

		byte[] r = new byte[a.length + len];
		System.arraycopy(a, 0, r, 0, a.length);
		System.arraycopy(b, off, r, a.length, len);
		return r;
	}
}