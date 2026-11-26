package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class SpriteContents implements Stitcher.Entry, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation name;
   final int width;
   final int height;
   private final NativeImage originalImage;
   NativeImage[] byMipLevel;
   @Nullable
   private final SpriteContents.AnimatedTexture animatedTexture;

   public SpriteContents(ResourceLocation param0, FrameSize param1, NativeImage param2, AnimationMetadataSection param3) {
      this.name = param0;
      this.width = param1.width();
      this.height = param1.height();
      this.animatedTexture = this.createAnimatedTexture(param1, param2.getWidth(), param2.getHeight(), param3);
      this.originalImage = param2;
      this.byMipLevel = new NativeImage[]{this.originalImage};
   }

   public void increaseMipLevel(int param0) {
      try {
         this.byMipLevel = MipmapGenerator.generateMipLevels(this.byMipLevel, param0);
      } catch (Throwable var2) {
         CrashReport var3 = CrashReport.forThrowable(var2, "Generating mipmaps for frame");
         CrashReportCategory var4 = var3.addCategory("Sprite being mipmapped");
         var4.setDetail("First frame", () -> {
            StringBuilder l_var1 = new StringBuilder();
            if (l_var1.length() > 0) {
               l_var1.append(", ");
            }

            l_var1.append(this.originalImage.getWidth()).append("x").append(this.originalImage.getHeight());
            return l_var1.toString();
         });
         CrashReportCategory var5 = var3.addCategory("Frame being iterated");
         var5.setDetail("Sprite name", this.name);
         var5.setDetail("Sprite size", () -> this.width + " x " + this.height);
         var5.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
         var5.setDetail("Mipmap levels", param0);
         throw new ReportedException(var3);
      }
   }

   private int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   @Nullable
   private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize param0, int param1, int param2, AnimationMetadataSection param3) {
      int var5 = param1 / param0.width();
      int var6 = param2 / param0.height();
      int var7 = var5 * var6;
      List<SpriteContents.FrameInfo> var8 = new ArrayList<>();
      param3.forEachFrame((l_param1, l_param2) -> var8.add(new SpriteContents.FrameInfo(l_param1, l_param2)));
      if (var8.isEmpty()) {
         for (int var9 = 0; var9 < var7; var9++) {
            var8.add(new SpriteContents.FrameInfo(var9, param3.getDefaultFrameTime()));
         }
      } else {
         int var9 = 0;
         IntSet var10 = new IntOpenHashSet();

         for (Iterator<SpriteContents.FrameInfo> var11 = var8.iterator(); var11.hasNext(); var9++) {
            SpriteContents.FrameInfo var12 = var11.next();
            boolean var13 = true;
            if (var12.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", this.name, var9, var12.time);
               var13 = false;
            }

            if (var12.index < 0 || var12.index >= var7) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", this.name, var9, var12.index);
               var13 = false;
            }

            if (var13) {
               var10.add(var12.index);
            } else {
               var11.remove();
            }
         }

         int[] var12 = IntStream.range(0, var7).filter(l_param1 -> !var10.contains(l_param1)).toArray();
         if (var12.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(var12));
         }
      }

      return var8.size() <= 1 ? null : new SpriteContents.AnimatedTexture(ImmutableList.copyOf(var8), var5, param3.isInterpolatedFrames());
   }

   void upload(int param0, int param1, int param2, int param3, NativeImage[] param4) {
      for (int var6 = 0; var6 < this.byMipLevel.length; var6++) {
         param4[var6]
            .upload(
               var6, param0 >> var6, param1 >> var6, param2 >> var6, param3 >> var6, this.width >> var6, this.height >> var6, this.byMipLevel.length > 1, false
            );
      }
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   @Override
   public ResourceLocation name() {
      return this.name;
   }

   public IntStream getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
   }

   @Nullable
   public SpriteTicker createTicker() {
      return this.animatedTexture != null ? this.animatedTexture.createTicker() : null;
   }

   @Override
   public void close() {
      for (NativeImage var4 : this.byMipLevel) {
         var4.close();
      }
   }

   @Override
   public String toString() {
      return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isTransparent(int param0, int param1, int param2) {
      int var4 = param1;
      int var5 = param2;
      if (this.animatedTexture != null) {
         var4 += this.animatedTexture.getFrameX(param0) * this.width;
         var5 += this.animatedTexture.getFrameY(param0) * this.height;
      }

      return (this.originalImage.getPixelRGBA(var4, var5) >> 24 & 0xFF) == 0;
   }

   public void uploadFirstFrame(int param0, int param1) {
      if (this.animatedTexture != null) {
         this.animatedTexture.uploadFirstFrame(param0, param1);
      } else {
         this.upload(param0, param1, 0, 0, this.byMipLevel);
      }
   }

   class AnimatedTexture {
      final List<SpriteContents.FrameInfo> frames;
      private final int frameRowSize;
      private final boolean interpolateFrames;

      AnimatedTexture(List<SpriteContents.FrameInfo> param1, int param2, boolean param3) {
         this.frames = param1;
         this.frameRowSize = param2;
         this.interpolateFrames = param3;
      }

      int getFrameX(int param0) {
         return param0 % this.frameRowSize;
      }

      int getFrameY(int param0) {
         return param0 / this.frameRowSize;
      }

      void uploadFrame(int param0, int param1, int param2) {
         int var4 = this.getFrameX(param2) * SpriteContents.this.width;
         int var5 = this.getFrameY(param2) * SpriteContents.this.height;
         SpriteContents.this.upload(param0, param1, var4, var5, SpriteContents.this.byMipLevel);
      }

      public SpriteTicker createTicker() {
         return SpriteContents.this.new Ticker(this, this.interpolateFrames ? SpriteContents.this.new InterpolationData() : null);
      }

      public void uploadFirstFrame(int param0, int param1) {
         this.uploadFrame(param0, param1, this.frames.get(0).index);
      }

      public IntStream getUniqueFrames() {
         return this.frames.stream().mapToInt(l_param0 -> l_param0.index).distinct();
      }
   }

   static class FrameInfo {
      final int index;
      final int time;

      FrameInfo(int param0, int param1) {
         this.index = param0;
         this.time = param1;
      }
   }

   final class InterpolationData implements AutoCloseable {
      private final NativeImage[] activeFrame = new NativeImage[SpriteContents.this.byMipLevel.length];

      InterpolationData() {
         for (int var2 = 0; var2 < this.activeFrame.length; var2++) {
            int var3 = SpriteContents.this.width >> var2;
            int var4 = SpriteContents.this.height >> var2;
            this.activeFrame[var2] = new NativeImage(var3, var4, false);
         }
      }

      void uploadInterpolatedFrame(int param0, int param1, SpriteContents.Ticker param2) {
         SpriteContents.AnimatedTexture var4 = param2.animationInfo;
         List<SpriteContents.FrameInfo> var5 = var4.frames;
         SpriteContents.FrameInfo var6 = var5.get(param2.frame);
         double var7 = 1.0 - (double)param2.subFrame / var6.time;
         int var9 = var6.index;
         int var10 = var5.get((param2.frame + 1) % var5.size()).index;
         if (var9 != var10) {
            for (int var11 = 0; var11 < this.activeFrame.length; var11++) {
               int var12 = SpriteContents.this.width >> var11;
               int var13 = SpriteContents.this.height >> var11;

               for (int var14 = 0; var14 < var13; var14++) {
                  for (int var15 = 0; var15 < var12; var15++) {
                     int var16 = this.getPixel(var4, var9, var11, var15, var14);
                     int var17 = this.getPixel(var4, var10, var11, var15, var14);
                     int var18 = this.mix(var7, var16 >> 16 & 0xFF, var17 >> 16 & 0xFF);
                     int var19 = this.mix(var7, var16 >> 8 & 0xFF, var17 >> 8 & 0xFF);
                     int var20 = this.mix(var7, var16 & 0xFF, var17 & 0xFF);
                     this.activeFrame[var11].setPixelRGBA(var15, var14, var16 & 0xFF000000 | var18 << 16 | var19 << 8 | var20);
                  }
               }
            }

            SpriteContents.this.upload(param0, param1, 0, 0, this.activeFrame);
         }
      }

      private int getPixel(SpriteContents.AnimatedTexture param0, int param1, int param2, int param3, int param4) {
         return SpriteContents.this.byMipLevel[param2]
            .getPixelRGBA(
               param3 + (param0.getFrameX(param1) * SpriteContents.this.width >> param2),
               param4 + (param0.getFrameY(param1) * SpriteContents.this.height >> param2)
            );
      }

      private int mix(double param0, int param2, int param3) {
         return (int)(param0 * param2 + (1.0 - param0) * param3);
      }

      @Override
      public void close() {
         for (NativeImage var4 : this.activeFrame) {
            var4.close();
         }
      }
   }

   class Ticker implements SpriteTicker {
      int frame;
      int subFrame;
      final SpriteContents.AnimatedTexture animationInfo;
      @Nullable
      private final SpriteContents.InterpolationData interpolationData;

      Ticker(SpriteContents.AnimatedTexture param1, @Nullable SpriteContents.InterpolationData param2) {
         this.animationInfo = param1;
         this.interpolationData = param2;
      }

      @Override
      public void tickAndUpload(int param0, int param1) {
         this.subFrame++;
         SpriteContents.FrameInfo var3 = this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= var3.time) {
            int var4 = var3.index;
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
            int var5 = this.animationInfo.frames.get(this.frame).index;
            if (var4 != var5) {
               this.animationInfo.uploadFrame(param0, param1, var5);
            }
         } else if (this.interpolationData != null) {
            if (!RenderSystem.isOnRenderThread()) {
               RenderSystem.recordRenderCall(() -> this.interpolationData.uploadInterpolatedFrame(param0, param1, this));
            } else {
               this.interpolationData.uploadInterpolatedFrame(param0, param1, this);
            }
         }
      }

      @Override
      public void close() {
         if (this.interpolationData != null) {
            this.interpolationData.close();
         }
      }
   }
}
