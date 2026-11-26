package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import javax.annotation.Nullable;

public class BlendMode {
   @Nullable
   private static BlendMode lastApplied;
   private final int srcColorFactor;
   private final int srcAlphaFactor;
   private final int dstColorFactor;
   private final int dstAlphaFactor;
   private final int blendFunc;
   private final boolean separateBlend;
   private final boolean opaque;

   private BlendMode(boolean param0, boolean param1, int param2, int param3, int param4, int param5, int param6) {
      this.separateBlend = param0;
      this.srcColorFactor = param2;
      this.dstColorFactor = param3;
      this.srcAlphaFactor = param4;
      this.dstAlphaFactor = param5;
      this.opaque = param1;
      this.blendFunc = param6;
   }

   public BlendMode() {
      this(false, true, 1, 0, 1, 0, 32774);
   }

   public BlendMode(int param0, int param1, int param2) {
      this(false, false, param0, param1, param0, param1, param2);
   }

   public BlendMode(int param0, int param1, int param2, int param3, int param4) {
      this(true, false, param0, param1, param2, param3, param4);
   }

   public void apply() {
      if (!this.equals(lastApplied)) {
         if (lastApplied == null || this.opaque != lastApplied.isOpaque()) {
            lastApplied = this;
            if (this.opaque) {
               RenderSystem.disableBlend();
               return;
            }

            RenderSystem.enableBlend();
         }

         RenderSystem.blendEquation(this.blendFunc);
         if (this.separateBlend) {
            RenderSystem.blendFuncSeparate(this.srcColorFactor, this.dstColorFactor, this.srcAlphaFactor, this.dstAlphaFactor);
         } else {
            RenderSystem.blendFunc(this.srcColorFactor, this.dstColorFactor);
         }
      }
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (!(param0 instanceof BlendMode var2)) {
         return false;
      } else if (this.blendFunc != var2.blendFunc) {
         return false;
      } else if (this.dstAlphaFactor != var2.dstAlphaFactor) {
         return false;
      } else if (this.dstColorFactor != var2.dstColorFactor) {
         return false;
      } else if (this.opaque != var2.opaque) {
         return false;
      } else if (this.separateBlend != var2.separateBlend) {
         return false;
      } else {
         return this.srcAlphaFactor != var2.srcAlphaFactor ? false : this.srcColorFactor == var2.srcColorFactor;
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.srcColorFactor;
      var1 = 31 * var1 + this.srcAlphaFactor;
      var1 = 31 * var1 + this.dstColorFactor;
      var1 = 31 * var1 + this.dstAlphaFactor;
      var1 = 31 * var1 + this.blendFunc;
      var1 = 31 * var1 + (this.separateBlend ? 1 : 0);
      return 31 * var1 + (this.opaque ? 1 : 0);
   }

   public boolean isOpaque() {
      return this.opaque;
   }

   public static int stringToBlendFunc(String param0) {
      String var1 = param0.trim().toLowerCase(Locale.ROOT);
      if ("add".equals(var1)) {
         return 32774;
      } else if ("subtract".equals(var1)) {
         return 32778;
      } else if ("reversesubtract".equals(var1)) {
         return 32779;
      } else if ("reverse_subtract".equals(var1)) {
         return 32779;
      } else if ("min".equals(var1)) {
         return 32775;
      } else {
         return "max".equals(var1) ? 32776 : 32774;
      }
   }

   public static int stringToBlendFactor(String param0) {
      String var1 = param0.trim().toLowerCase(Locale.ROOT);
      var1 = var1.replaceAll("_", "");
      var1 = var1.replaceAll("one", "1");
      var1 = var1.replaceAll("zero", "0");
      var1 = var1.replaceAll("minus", "-");
      if ("0".equals(var1)) {
         return 0;
      } else if ("1".equals(var1)) {
         return 1;
      } else if ("srccolor".equals(var1)) {
         return 768;
      } else if ("1-srccolor".equals(var1)) {
         return 769;
      } else if ("dstcolor".equals(var1)) {
         return 774;
      } else if ("1-dstcolor".equals(var1)) {
         return 775;
      } else if ("srcalpha".equals(var1)) {
         return 770;
      } else if ("1-srcalpha".equals(var1)) {
         return 771;
      } else if ("dstalpha".equals(var1)) {
         return 772;
      } else {
         return "1-dstalpha".equals(var1) ? 773 : -1;
      }
   }
}
