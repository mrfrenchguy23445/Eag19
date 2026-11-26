package net.minecraft.client.resources.metadata.animation;

import com.google.common.collect.Lists;
import java.util.List;

public class AnimationMetadataSection {
   public static final AnimationMetadataSectionSerializer SERIALIZER = new AnimationMetadataSectionSerializer();
   public static final String SECTION_NAME = "animation";
   public static final int DEFAULT_FRAME_TIME = 1;
   public static final int UNKNOWN_SIZE = -1;
   public static final AnimationMetadataSection EMPTY = new AnimationMetadataSection(Lists.newArrayList(), -1, -1, 1, false) {
      @Override
      public FrameSize calculateFrameSize(int param0, int param1) {
         return new FrameSize(param0, param1);
      }
   };
   private final List<AnimationFrame> frames;
   private final int frameWidth;
   private final int frameHeight;
   private final int defaultFrameTime;
   private final boolean interpolatedFrames;

   public AnimationMetadataSection(List<AnimationFrame> param0, int param1, int param2, int param3, boolean param4) {
      this.frames = param0;
      this.frameWidth = param1;
      this.frameHeight = param2;
      this.defaultFrameTime = param3;
      this.interpolatedFrames = param4;
   }

   public FrameSize calculateFrameSize(int param0, int param1) {
      if (this.frameWidth != -1) {
         return this.frameHeight != -1 ? new FrameSize(this.frameWidth, this.frameHeight) : new FrameSize(this.frameWidth, param1);
      }

      if (this.frameHeight != -1) {
         return new FrameSize(param0, this.frameHeight);
      }

      int var3 = Math.min(param0, param1);
      return new FrameSize(var3, var3);
   }

   public int getDefaultFrameTime() {
      return this.defaultFrameTime;
   }

   public boolean isInterpolatedFrames() {
      return this.interpolatedFrames;
   }

   public void forEachFrame(AnimationMetadataSection.FrameOutput param0) {
      for (AnimationFrame var3 : this.frames) {
         param0.accept(var3.getIndex(), var3.getTime(this.defaultFrameTime));
      }
   }

   @FunctionalInterface
   public interface FrameOutput {
      void accept(int var1, int var2);
   }
}
