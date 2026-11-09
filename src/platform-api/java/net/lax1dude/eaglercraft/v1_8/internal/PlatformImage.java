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

/**
 * Replaces the four org.lwjgl.stb entry points NativeImage used, for targets that
 * have no LWJGL at all. Each method here has a real implementation in the target's
 * own source tree, the same seam pattern PlatformOpenGL / PlatformAudio use.
 *
 * Why this exists: NativeImage stored its pixels in a native `long` address and
 * delegated every codec operation to stb_image. There is no native memory and no
 * stb_image here, so the storage became a Java byte[] (see NativeImage) and these
 * four operations became plain byte[] work. PNG is not an accident: every texture
 * Minecraft loads is a PNG, and there is no javax.imageio in the classlib, so the
 * codec is written out by hand against java.util.zip.
 *
 * Only what Minecraft actually needs is implemented. Notably the decoder refuses
 * interlaced (Adam7) PNGs rather than implementing them -- nothing Mojang ships is
 * interlaced, and silently producing a garbled atlas would be far worse than a
 * clear failure.
 */
public class PlatformImage {

	/**
	 * Decodes a PNG.
	 *
	 * @param data                the complete file contents.
	 * @param outDims             written with {width, height} on success; untouched on failure.
	 * @param requiredComponents  0 for the image's natural channel count, or 1 (grey),
	 *                            2 (grey+alpha), 3 (RGB) or 4 (RGBA) to force a conversion.
	 *                            This mirrors stbi_load_from_memory's req_comp, because
	 *                            NativeImage.read passes NativeImage.Format.components.
	 * @return width * height * requiredComponents bytes, row-major from the top-left,
	 *         or null if the data is not a PNG this decoder accepts. Ask
	 *         {@link #decodeFailure()} for the reason -- it stands in for
	 *         STBImage.stbi_failure_reason().
	 */
	public static native byte[] decodePng(byte[] data, int[] outDims, int requiredComponents);

	/** Why the last {@link #decodePng} returned null. Never null. */
	public static native String decodeFailure();

	/**
	 * Encodes to PNG. Inverse of {@link #decodePng} for 8-bit-per-component input.
	 *
	 * @param pixels width * height * components bytes, row-major from the top-left.
	 * @return the file contents, or null if the arguments are unusable.
	 */
	public static native byte[] encodePng(byte[] pixels, int width, int height, int components);

	/**
	 * Crops the source rectangle (srcX, srcY, srcW, srcH) out of src and resamples
	 * it to the destination image's own width and height. Stands in for
	 * STBImageResize.nstbir_resize_uint8. Matches the vanilla
	 * NativeImage.resizeSubRectTo contract, which the auto-screenshot thumbnail
	 * uses.
	 *
	 * @param src        source buffer; srcX + srcW rows of srcStride bytes, the
	 *                   full source width times components.
	 * @param srcX       left edge of the source rectangle.
	 * @param srcY       top edge of the source rectangle.
	 * @param srcW       width of the source rectangle.
	 * @param srcH       height of the source rectangle.
	 * @param srcStride  bytes per row of src, i.e. the full source width times
	 *                   components -- not srcW.
	 * @param components 1, 2, 3 or 4, matching on both sides.
	 * @param dst        destination buffer, dstW * dstH * components bytes.
	 * @param dstW       destination width.
	 * @param dstH       destination height.
	 */
	public static native void resize(byte[] src, int srcX, int srcY, int srcW, int srcH, int srcStride, int components,
			byte[] dst, int dstW, int dstH);
}