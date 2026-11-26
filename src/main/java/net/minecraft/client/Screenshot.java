package net.minecraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class Screenshot {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String SCREENSHOT_DIR = "screenshots";
   private int rowHeight;
   private final DataOutputStream outputStream;
   private final byte[] bytes;
   private final int width;
   private final int height;
   private File file;

   public static void grab(File param0, RenderTarget param1, Consumer<Component> param2) {
      grab(param0, null, param1, param2);
   }

   public static void grab(File param0, @Nullable String param1, RenderTarget param2, Consumer<Component> param3) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> _grab(param0, param1, param2, param3));
      } else {
         _grab(param0, param1, param2, param3);
      }
   }

   private static void _grab(File param0, @Nullable String param1, RenderTarget param2, Consumer<Component> param3) {
      NativeImage var4 = takeScreenshot(param2);
      File var5 = new File(param0, "screenshots");
      var5.mkdir();
      File var6;
      if (param1 == null) {
         var6 = getFile(var5);
      } else {
         var6 = new File(var5, param1);
      }

      Util.ioPool()
         .execute(
            () -> {
               try {
                  var4.writeToFile(var6);
                  Component l_var3 = Component.literal(var6.getName())
                     .withStyle(ChatFormatting.UNDERLINE)
                     .withStyle(l2_param1 -> l2_param1.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var6.getAbsolutePath())));
                  param3.accept(Component.translatable("screenshot.success", l_var3));
               } catch (Exception l_var3) {
                  LOGGER.warn("Couldn't save screenshot", l_var3);
                  param3.accept(Component.translatable("screenshot.failure", l_var3.getMessage()));
               } finally {
                  var4.close();
               }
            }
         );
   }

   public static NativeImage takeScreenshot(RenderTarget param0) {
      int var1 = param0.width;
      int var2 = param0.height;
      NativeImage var3 = new NativeImage(var1, var2, false);
      // Port note (LWJGL removal): vanilla bound the colour texture and read it back with
      // glGetTexImage. WebGL cannot read texture storage at all, so NativeImage
      // .downloadTexture is now a glReadPixels off the bound framebuffer instead -- which
      // means the framebuffer has to be the render target's. It cannot be assumed: the
      // caller (GameRenderer.tryTakeScreenshotIfNeeded) runs this before it re-binds the
      // main render target, and the post-effect chain may have left anything bound.
      // bindWrite(false) binds the FBO without touching the viewport or clearing.
      param0.bindWrite(false);
      var3.downloadTexture(0, true);
      var3.flipY();
      return var3;
   }

   private static File getFile(File param0) {
      String var1 = Util.getFilenameFormattedDateTime();
      int var2 = 1;

      while (true) {
         File var3 = new File(param0, var1 + (var2 == 1 ? "" : "_" + var2) + ".png");
         if (!var3.exists()) {
            return var3;
         }

         var2++;
      }
   }

   public Screenshot(File param0, int param1, int param2, int param3) throws IOException {
      this.width = param1;
      this.height = param2;
      this.rowHeight = param3;
      File var5 = new File(param0, "screenshots");
      var5.mkdir();
      String var6 = "huge_" + Util.getFilenameFormattedDateTime();
      int var7 = 1;

      while ((this.file = new File(var5, var6 + (var7 == 1 ? "" : "_" + var7) + ".tga")).exists()) {
         var7++;
      }

      byte[] var8 = new byte[18];
      var8[2] = 2;
      var8[12] = (byte)(param1 % 256);
      var8[13] = (byte)(param1 / 256);
      var8[14] = (byte)(param2 % 256);
      var8[15] = (byte)(param2 / 256);
      var8[16] = 24;
      this.bytes = new byte[param1 * param3 * 3];
      this.outputStream = new DataOutputStream(new FileOutputStream(this.file));
      this.outputStream.write(var8);
   }

   public void addRegion(ByteBuffer param0, int param1, int param2, int param3, int param4) {
      int var6 = param3;
      int var7 = param4;
      if (var6 > this.width - param1) {
         var6 = this.width - param1;
      }

      if (var7 > this.height - param2) {
         var7 = this.height - param2;
      }

      this.rowHeight = var7;

      for (int var8 = 0; var8 < var7; var8++) {
         param0.position((param4 - var7) * param3 * 3 + var8 * param3 * 3);
         int var9 = (param1 + var8 * this.width) * 3;
         param0.get(this.bytes, var9, var6 * 3);
      }
   }

   public void saveRow() throws IOException {
      this.outputStream.write(this.bytes, 0, this.width * 3 * this.rowHeight);
   }

   public File close() throws IOException {
      this.outputStream.close();
      return this.file;
   }
}
