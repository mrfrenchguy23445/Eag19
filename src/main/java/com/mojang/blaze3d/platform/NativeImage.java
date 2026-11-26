package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.FastColor;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformImage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

/**
 * Port note (LWJGL removal): pixels live in a Java {@code byte[]} instead of a native
 * {@code long} address.
 *
 * <p>Vanilla held {@code private long pixels} from {@code MemoryUtil.nmemAlloc} and used
 * LWJGL's pointer arithmetic for every access. All of that is gone, so every
 * {@code MemoryUtil.mem*} call in this class became an array index and the four stb_image
 * entry points became {@link PlatformImage}, whose implementation decodes and encodes PNG
 * in pure Java (there is no {@code javax.imageio} and no stb_image in the TeaVM classlib).
 *
 * <p>Byte order is the one thing that must be reproduced exactly rather than tidied:
 * {@code getPixelRGBA}/{@code setPixelRGBA} exchange a 32-bit value that callers read with
 * {@code FastColor.ABGR32}, i.e. alpha in bits 24-31. {@code memGetInt} read that in the
 * platform's native order, which is little-endian on every target this port runs on, so
 * the helpers below assemble and disassemble little-endian by hand.
 *
 * <p>Deleted, with justification, rather than ported:
 * <ul>
 *   <li>{@code downloadDepthBuffer}, {@code drawPixels}, {@code asByteArray} -- no callers.</li>
 *   <li>{@code copyFromFont} -- only TrueTypeGlyphProvider called it, and that provider is
 *       refused outright (no TrueType rasteriser).</li>
 *   <li>{@code WriteCallback} -- existed only to hand stb_image a WritableByteChannel.
 *       {@link #writeToFile} now encodes to a byte[] directly.</li>
 * </ul>
 */
public final class NativeImage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(
      StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   );
   private final NativeImage.Format format;
   private final int width;
   private final int height;
   private byte[] pixels;

   public NativeImage(int param0, int param1, boolean param2) {
      this(NativeImage.Format.RGBA, param0, param1, param2);
   }

   public NativeImage(NativeImage.Format param0, int param1, int param2, boolean param3) {
      if (param1 > 0 && param2 > 0) {
         this.format = param0;
         this.width = param1;
         this.height = param2;
         // param3 asked stb for a zeroed buffer (nmemCalloc) rather than nmemAlloc. Java
         // always hands back zeroed arrays, so the distinction is gone; the flag stays on
         // the signature because call sites pass it.
         this.pixels = new byte[(int)((long)param1 * param2 * param0.components())];
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + param1 + "x" + param2);
      }
   }

   private NativeImage(NativeImage.Format param0, int param1, int param2, byte[] param3) {
      if (param1 > 0 && param2 > 0) {
         this.format = param0;
         this.width = param1;
         this.height = param2;
         this.pixels = param3;
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + param1 + "x" + param2);
      }
   }

   @Override
   public String toString() {
      return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels.length + "]";
   }

   private boolean isOutsideBounds(int param0, int param1) {
      return param0 < 0 || param0 >= this.width || param1 < 0 || param1 >= this.height;
   }

   public static NativeImage read(InputStream param0) throws IOException {
      return read(NativeImage.Format.RGBA, param0);
   }

   public static NativeImage read(@Nullable NativeImage.Format param0, InputStream param1) throws IOException {
      try {
         ByteBuffer var2 = TextureUtil.readResource(param1);
         var2.rewind();
         return read(param0, var2);
      } finally {
         IOUtils.closeQuietly(param1);
      }
   }

   public static NativeImage read(ByteBuffer param0) throws IOException {
      return read(NativeImage.Format.RGBA, param0);
   }

   public static NativeImage read(byte[] param0) throws IOException {
      return read(NativeImage.Format.RGBA, param0);
   }

   public static NativeImage read(@Nullable NativeImage.Format param0, byte[] param1) throws IOException {
      int[] var1 = new int[2];
      // requiredComponents mirrors stb_image's req_comp: 0 means "keep whatever the PNG
      // naturally has", which is what makes Format.getStbFormat meaningful below.
      byte[] var2 = PlatformImage.decodePng(param1, var1, param0 == null ? 0 : param0.components);
      if (var2 == null) {
         throw new IOException("Could not load image: " + PlatformImage.decodeFailure());
      } else {
         return new NativeImage(
            param0 == null ? NativeImage.Format.getStbFormat(var2.length / (var1[0] * var1[1])) : param0,
            var1[0], var1[1], var2
         );
      }
   }

   public static NativeImage read(@Nullable NativeImage.Format param0, ByteBuffer param1) throws IOException {
      if (param0 != null && !param0.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to read format " + param0);
      }

      if (param1 == null) {
         throw new IllegalArgumentException("Invalid buffer");
      }

      return read(param0, remainingBytes(param1));
   }

   /**
    * Copies a buffer's readable remainder out as a byte array.
    *
    * <p>Vanilla handed the direct buffer straight to {@code stbi_load_from_memory}, which
    * reads it in place. The pure-Java decoder wants an array, and it must not disturb the
    * caller's position, so this works from {@code duplicate()} rather than the buffer
    * itself.
    */
   private static byte[] remainingBytes(ByteBuffer param0) {
      ByteBuffer var1 = param0.duplicate();
      var1.rewind();
      byte[] var2 = new byte[var1.remaining()];
      var1.get(var2);
      return var2;
   }

   private static void setFilter(boolean param0, boolean param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      if (param0) {
         GlStateManager._texParameter(3553, 10241, param1 ? 9987 : 9729);
         GlStateManager._texParameter(3553, 10240, 9729);
      } else {
         GlStateManager._texParameter(3553, 10241, param1 ? 9986 : 9728);
         GlStateManager._texParameter(3553, 10240, 9728);
      }
   }

   private void checkAllocated() {
      if (this.pixels == null) {
         throw new IllegalStateException("Image is not allocated.");
      }
   }

   @Override
   public void close() {
      this.pixels = null;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public NativeImage.Format format() {
      return this.format;
   }

   public int getPixelRGBA(int param0, int param1) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      this.checkAllocated();
      int var3 = (param0 + param1 * this.width) * 4;
      return (this.pixels[var3] & 255)
         | ((this.pixels[var3 + 1] & 255) << 8)
         | ((this.pixels[var3 + 2] & 255) << 16)
         | ((this.pixels[var3 + 3] & 255) << 24);
   }

   public void setPixelRGBA(int param0, int param1, int param2) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      this.checkAllocated();
      int var3 = (param0 + param1 * this.width) * 4;
      this.pixels[var3] = (byte)param2;
      this.pixels[var3 + 1] = (byte)(param2 >>> 8);
      this.pixels[var3 + 2] = (byte)(param2 >>> 16);
      this.pixels[var3 + 3] = (byte)(param2 >>> 24);
   }

   public NativeImage mappedCopy(IntUnaryOperator param0) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      }

      this.checkAllocated();
      NativeImage var2 = new NativeImage(this.width, this.height, false);
      int var3 = this.width * this.height * 4;

      for (int var4 = 0; var4 < var3; var4 += 4) {
         int var5 = (this.pixels[var4] & 255)
            | ((this.pixels[var4 + 1] & 255) << 8)
            | ((this.pixels[var4 + 2] & 255) << 16)
            | ((this.pixels[var4 + 3] & 255) << 24);
         int var6 = param0.applyAsInt(var5);
         var2.pixels[var4] = (byte)var6;
         var2.pixels[var4 + 1] = (byte)(var6 >>> 8);
         var2.pixels[var4 + 2] = (byte)(var6 >>> 16);
         var2.pixels[var4 + 3] = (byte)(var6 >>> 24);
      }

      return var2;
   }

   public int[] getPixelsRGBA() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      }

      this.checkAllocated();
      int var1 = this.width * this.height;
      int[] var2 = new int[var1];

      for (int var3 = 0; var3 < var1; ++var3) {
         int var4 = var3 * 4;
         var2[var3] = (this.pixels[var4] & 255)
            | ((this.pixels[var4 + 1] & 255) << 8)
            | ((this.pixels[var4 + 2] & 255) << 16)
            | ((this.pixels[var4 + 3] & 255) << 24);
      }

      return var2;
   }

   public void setPixelLuminance(int param0, int param1, byte param2) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminance()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelLuminance only works on image with luminance; have %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      this.checkAllocated();
      this.pixels[(param0 + param1 * this.width) * this.format.components() + this.format.luminanceOffset() / 8] = param2;
   }

   public byte getRedOrLuminance(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrRed()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no red or luminance in %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      return this.pixels[(param0 + param1 * this.width) * this.format.components() + this.format.luminanceOrRedOffset() / 8];
   }

   public byte getGreenOrLuminance(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrGreen()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no green or luminance in %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      return this.pixels[(param0 + param1 * this.width) * this.format.components() + this.format.luminanceOrGreenOffset() / 8];
   }

   public byte getBlueOrLuminance(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrBlue()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no blue or luminance in %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      return this.pixels[(param0 + param1 * this.width) * this.format.components() + this.format.luminanceOrBlueOffset() / 8];
   }

   public byte getLuminanceOrAlpha(int param0, int param1) {
      if (!this.format.hasLuminanceOrAlpha()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
      }

      if (this.isOutsideBounds(param0, param1)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", param0, param1, this.width, this.height));
      }

      return this.pixels[(param0 + param1 * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8];
   }

   public void blendPixel(int param0, int param1, int param2) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("Can only call blendPixel with RGBA format");
      }

      int var4 = this.getPixelRGBA(param0, param1);
      float var5 = FastColor.ABGR32.alpha(param2) / 255.0F;
      float var6 = FastColor.ABGR32.blue(param2) / 255.0F;
      float var7 = FastColor.ABGR32.green(param2) / 255.0F;
      float var8 = FastColor.ABGR32.red(param2) / 255.0F;
      float var9 = FastColor.ABGR32.alpha(var4) / 255.0F;
      float var10 = FastColor.ABGR32.blue(var4) / 255.0F;
      float var11 = FastColor.ABGR32.green(var4) / 255.0F;
      float var12 = FastColor.ABGR32.red(var4) / 255.0F;
      float var13 = var5;
      float var14 = 1.0F - var5;
      float var15 = var5 * var13 + var9 * var14;
      float var16 = var6 * var13 + var10 * var14;
      float var17 = var7 * var13 + var11 * var14;
      float var18 = var8 * var13 + var12 * var14;
      if (var15 > 1.0F) {
         var15 = 1.0F;
      }

      if (var16 > 1.0F) {
         var16 = 1.0F;
      }

      if (var17 > 1.0F) {
         var17 = 1.0F;
      }

      if (var18 > 1.0F) {
         var18 = 1.0F;
      }

      int var19 = (int)(var15 * 255.0F);
      int var20 = (int)(var16 * 255.0F);
      int var21 = (int)(var17 * 255.0F);
      int var22 = (int)(var18 * 255.0F);
      this.setPixelRGBA(param0, param1, FastColor.ABGR32.color(var19, var20, var21, var22));
   }

   @Deprecated
   public int[] makePixelArray() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
      }

      this.checkAllocated();
      int[] var1 = new int[this.getWidth() * this.getHeight()];

      for (int var2 = 0; var2 < this.getHeight(); var2++) {
         for (int var3 = 0; var3 < this.getWidth(); var3++) {
            int var4 = this.getPixelRGBA(var3, var2);
            var1[var3 + var2 * this.getWidth()] = FastColor.ARGB32.color(
               FastColor.ABGR32.alpha(var4), FastColor.ABGR32.red(var4), FastColor.ABGR32.green(var4), FastColor.ABGR32.blue(var4)
            );
         }
      }

      return var1;
   }

   public void upload(int param0, int param1, int param2, boolean param3) {
      this.upload(param0, param1, param2, 0, 0, this.width, this.height, false, param3);
   }

   public void upload(int param0, int param1, int param2, int param3, int param4, int param5, int param6, boolean param7, boolean param8) {
      this.upload(param0, param1, param2, param3, param4, param5, param6, false, false, param7, param8);
   }

   public void upload(
      int param0, int param1, int param2, int param3, int param4, int param5, int param6, boolean param7, boolean param8, boolean param9, boolean param10
   ) {
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> this._upload(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10));
      } else {
         this._upload(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
      }
   }

   private void _upload(
      int param0, int param1, int param2, int param3, int param4, int param5, int param6, boolean param7, boolean param8, boolean param9, boolean param10
   ) {
      RenderSystem.assertOnRenderThreadOrInit();
      this.checkAllocated();
      setFilter(param7, param9);
      if (param5 == this.getWidth()) {
         GlStateManager._pixelStore(3314, 0);
      } else {
         GlStateManager._pixelStore(3314, this.getWidth());
      }

      // PORT NOTE (Eag19 / TeaVM WASM-GC): sliceForUpload() already begins the view at
      // the SKIP_PIXELS/SKIP_ROWS source offset, so the WebGL UNPACK_SKIP_* state must
      // stay zero - setting it here as well double-applies the offset. That made the
      // view shorter than the sub-rectangle WebGL then tried to read, so every upload
      // with a non-zero source offset failed with "texSubImage2D: ArrayBufferView not
      // big enough for request". It hit all font-glyph uploads (BitmapProvider passes
      // each glyph's offsetX/offsetY into this call) - i.e. the title-screen text.
      GlStateManager._pixelStore(3316, 0);
      GlStateManager._pixelStore(3315, 0);
      this.format.setUnpackPixelStoreState();
      // The ByteBuffer overload, not the pointer one: WebGL has no client memory, and the
      // whole point of holding pixels in a byte[] is that this call now has something
      // WebGL can view. The slice starts at the source offset the SKIP_PIXELS/SKIP_ROWS
      // state describes, which is how the sub-rect uploads in TextureAtlasSprite work.
      ByteBuffer var12 = sliceForUpload(param3, param4);
      GlStateManager._texSubImage2D(3553, param0, param1, param2, param5, param6, this.format.glFormat(), 5121, var12);
      if (param8) {
         GlStateManager._texParameter(3553, 10242, 33071);
         GlStateManager._texParameter(3553, 10243, 33071);
      }

      if (param10) {
         this.close();
      }
   }

   /**
    * Positions a read-only view of the pixel buffer for the requested upload.
    *
    * <p>Vanilla pointed GL at the whole allocation and expressed the sub-rectangle purely
    * as UNPACK_SKIP_PIXELS/SKIP_ROWS/SKIP_ROW_LENGTH. WebGL honours those the same way, so
    * the honest translation is to hand it a slice starting exactly where the skip state
    * says the data begins -- setting the skip state too would double-apply the offset.
    */
   private ByteBuffer sliceForUpload(int skipPixels, int skipRows) {
      int var1 = this.format.components();
      int var2 = skipRows * this.width * var1 + skipPixels * var1;
      int var3 = (this.height - skipRows) * this.width * var1;
      if (var3 <= 0) {
         return ByteBuffer.wrap(this.pixels);
      }

      if (var2 <= 0) {
         return ByteBuffer.wrap(this.pixels, 0, var3);
      }

      if (var2 >= this.pixels.length) {
         return ByteBuffer.wrap(new byte[0]);
      }

      return ByteBuffer.wrap(this.pixels, var2, Math.min(var3, this.pixels.length - var2));
   }

   /**
    * Reads the currently bound framebuffer into this image.
    *
    * <p>Replaces {@code downloadTexture}, which used {@code glGetTexImage}. WebGL cannot
    * read texture storage back at all -- {@code PlatformOpenGL._glGetTexImage} throws -- so
    * the equivalent is {@code glReadPixels} off whatever framebuffer is bound. That is also
    * what the two call sites actually want:
    *
    * <ul>
    *   <li>{@code Screenshot.takeScreenshot} now binds its render target for writing
    *       first, so the read hits exactly the texture it used to read back. Vanilla bound
    *       the texture and read it; here the texture is only ever reachable through its
    *       framebuffer.</li>
    *   <li>{@code TextureUtil.writeAsPNG} (the texture-atlas debug dump) reads whatever is
    *       bound. That output was never a faithful copy on this target anyway, and it is
    *       debug-only behind a config flag.</li>
    * </ul>
    *
    * <p>Row order matches GL: bottom-up. {@code flipY()} is what turns it into an
    * image, which the screenshot path already calls.
    */
   public void downloadTexture(int param0, boolean param1) {
      RenderSystem.assertOnRenderThread();
      this.checkAllocated();
      this.format.setPackPixelStoreState();
      GlStateManager._pixelStore(3332, this.width * this.format.components());
      GlStateManager._readPixels(0, 0, this.width, this.height, this.format.glFormat(), 5121, ByteBuffer.wrap(this.pixels));
      if (param1 && this.format.hasAlpha()) {
         for (int var3 = 0; var3 < this.getHeight(); var3++) {
            for (int var4 = 0; var4 < this.getWidth(); var4++) {
               this.setPixelRGBA(var4, var3, this.getPixelRGBA(var4, var3) | 255 << this.format.alphaOffset());
            }
         }
      }
   }

   public void writeToFile(File param0) throws IOException {
      this.writeToFile(Util.toPath(param0));
   }

   public void writeToFile(Path param0) throws IOException {
      if (!this.format.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to write format " + this.format);
      }

      this.checkAllocated();
      byte[] var1 = encodePng();

      try {
         Files.write(param0, var1);
      } catch (IOException var2) {
         throw new IOException("Could not write image to the PNG file \"" + param0.toAbsolutePath() + "\": " + var2.getMessage(), var2);
      }
   }

   /**
    * Encodes this image as PNG bytes.
    *
    * <p>Vanilla streamed straight into the channel through stb's write callback. Producing
    * the whole file in memory instead costs one extra copy of the compressed output, which
    * is small next to the pixel data already resident, and it removes the callback, the
    * channel and the "did stb report a reason" error path entirely.
    */
   private byte[] encodePng() throws IOException {
      byte[] var1 = PlatformImage.encodePng(this.pixels, this.width, this.height, this.format.components());
      if (var1 == null) {
         throw new IOException("Could not encode " + this.width + "x" + this.height + " " + this.format + " image as PNG");
      }

      return var1;
   }

   public void copyFrom(NativeImage param0) {
      if (param0.format() != this.format) {
         throw new UnsupportedOperationException("Image formats don't match.");
      }

      int var2 = this.format.components();
      this.checkAllocated();
      param0.checkAllocated();
      if (this.width == param0.width) {
         System.arraycopy(param0.pixels, 0, this.pixels, 0, Math.min(this.pixels.length, param0.pixels.length));
      } else {
         int var3 = Math.min(this.getWidth(), param0.getWidth()) * var2;
         int var4 = Math.min(this.getHeight(), param0.getHeight());

         for (int var5 = 0; var5 < var4; var5++) {
            int var6 = var5 * param0.getWidth() * var2;
            int var7 = var5 * this.getWidth() * var2;
            System.arraycopy(param0.pixels, var6, this.pixels, var7, var3);
         }
      }
   }

   public void fillRect(int param0, int param1, int param2, int param3, int param4) {
      for (int var6 = param1; var6 < param1 + param3; var6++) {
         for (int var7 = param0; var7 < param0 + param2; var7++) {
            this.setPixelRGBA(var7, var6, param4);
         }
      }
   }

   public void copyRect(int param0, int param1, int param2, int param3, int param4, int param5, boolean param6, boolean param7) {
      this.copyRect(this, param0, param1, param0 + param2, param1 + param3, param4, param5, param6, param7);
   }

   public void copyRect(NativeImage param0, int param1, int param2, int param3, int param4, int param5, int param6, boolean param7, boolean param8) {
      // The rectangle's extent, split by axis: var5 spans x (param1..param3) and var6 spans
      // y (param2..param4). Both are read through the flip flags below -- param7 mirrors
      // horizontally, so it bounds the x index; param8 mirrors vertically, so it bounds the
      // y one.
      int var5 = param3 - param1;
      int var6 = param4 - param2;

      for (int var10 = 0; var10 < var6; var10++) {
         for (int var11 = 0; var11 < var5; var11++) {
            int var12 = param7 ? var5 - 1 - var11 : var11;
            int var13 = param8 ? var6 - 1 - var10 : var10;
            int var14 = this.getPixelRGBA(param1 + var11, param2 + var10);
            param0.setPixelRGBA(param3 + var12, param4 + var13, var14);
         }
      }
   }

   public void flipY() {
      this.checkAllocated();
      int var1 = this.format.components();
      int var2 = this.getWidth() * var1;
      byte[] var3 = new byte[var2];

      for (int var6 = 0; var6 < this.getHeight() / 2; var6++) {
         int var7 = var6 * var2;
         int var8 = (this.getHeight() - 1 - var6) * var2;
         System.arraycopy(this.pixels, var7, var3, 0, var2);
         System.arraycopy(this.pixels, var8, this.pixels, var7, var2);
         System.arraycopy(var3, 0, this.pixels, var8, var2);
      }
   }

   public void resizeSubRectTo(int param0, int param1, int param2, int param3, NativeImage param4) {
      this.checkAllocated();
      if (param4.format() != this.format) {
         throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
      }

      int var6 = this.format.components();
      // Vanilla contract: crop (param0, param1, param2, param3) out of this image
      // and scale it into param4's own dimensions (e.g. the 64x64 world icon).
      PlatformImage.resize(
         this.pixels, param0, param1, param2, param3, this.getWidth() * var6, var6,
         param4.pixels, param4.getWidth(), param4.getHeight()
      );
   }

   public void untrack() {
      // Was DebugMemoryUntracker.untrack(this.pixels): telling LWJGL's debug allocator to
      // stop accounting one native allocation. There is no native allocation and no
      // allocator, so there is nothing to untrack. Kept because SpecialGlyphs and
      // MissingTextureAtlasSprite call it.
   }

   public enum Format {
      RGBA(4, 6408, true, true, true, false, true, 0, 8, 16, 255, 24, true),
      RGB(3, 6407, true, true, true, false, false, 0, 8, 16, 255, 255, true),
      LUMINANCE_ALPHA(2, 33319, false, false, false, true, true, 255, 255, 255, 0, 8, true),
      LUMINANCE(1, 6403, false, false, false, true, false, 0, 0, 0, 0, 255, true);

      final int components;
      private final int glFormat;
      private final boolean hasRed;
      private final boolean hasGreen;
      private final boolean hasBlue;
      private final boolean hasLuminance;
      private final boolean hasAlpha;
      private final int redOffset;
      private final int greenOffset;
      private final int blueOffset;
      private final int luminanceOffset;
      private final int alphaOffset;
      private final boolean supportedByStb;

      Format(
         int param2,
         int param3,
         boolean param4,
         boolean param5,
         boolean param6,
         boolean param7,
         boolean param8,
         int param9,
         int param10,
         int param11,
         int param12,
         int param13,
         boolean param14
      ) {
         this.components = param2;
         this.glFormat = param3;
         this.hasRed = param4;
         this.hasGreen = param5;
         this.hasBlue = param6;
         this.hasLuminance = param7;
         this.hasAlpha = param8;
         this.redOffset = param9;
         this.greenOffset = param10;
         this.blueOffset = param11;
         this.luminanceOffset = param12;
         this.alphaOffset = param13;
         this.supportedByStb = param14;
      }

      public int components() {
         return this.components;
      }

      public void setPackPixelStoreState() {
         RenderSystem.assertOnRenderThread();
         GlStateManager._pixelStore(3333, this.components());
      }

      public void setUnpackPixelStoreState() {
         RenderSystem.assertOnRenderThreadOrInit();
         GlStateManager._pixelStore(3317, this.components());
      }

      public int glFormat() {
         return this.glFormat;
      }

      public boolean hasRed() {
         return this.hasRed;
      }

      public boolean hasGreen() {
         return this.hasGreen;
      }

      public boolean hasBlue() {
         return this.hasBlue;
      }

      public boolean hasLuminance() {
         return this.hasLuminance;
      }

      public boolean hasAlpha() {
         return this.hasAlpha;
      }

      public int redOffset() {
         return this.redOffset;
      }

      public int greenOffset() {
         return this.greenOffset;
      }

      public int blueOffset() {
         return this.blueOffset;
      }

      public int luminanceOffset() {
         return this.luminanceOffset;
      }

      public int alphaOffset() {
         return this.alphaOffset;
      }

      public boolean hasLuminanceOrRed() {
         return this.hasLuminance || this.hasRed;
      }

      public boolean hasLuminanceOrGreen() {
         return this.hasLuminance || this.hasGreen;
      }

      public boolean hasLuminanceOrBlue() {
         return this.hasLuminance || this.hasBlue;
      }

      public boolean hasLuminanceOrAlpha() {
         return this.hasLuminance || this.hasAlpha;
      }

      public int luminanceOrRedOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.redOffset;
      }

      public int luminanceOrGreenOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
      }

      public int luminanceOrBlueOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
      }

      public int luminanceOrAlphaOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
      }

      public boolean supportedByStb() {
         return this.supportedByStb;
      }

      static NativeImage.Format getStbFormat(int param0) {
         switch (param0) {
            case 1:
               return LUMINANCE;
            case 2:
               return LUMINANCE_ALPHA;
            case 3:
               return RGB;
            case 4:
            default:
               return RGBA;
         }
      }
   }

   public enum InternalGlFormat {
      RGBA(6408),
      RGB(6407),
      RG(33319),
      RED(6403);

      private final int glFormat;

      InternalGlFormat(int param2) {
         this.glFormat = param2;
      }

      public int glFormat() {
         return this.glFormat;
      }
   }
}
