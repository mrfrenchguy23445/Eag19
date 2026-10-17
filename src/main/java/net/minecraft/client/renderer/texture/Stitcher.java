package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Stitcher<T extends Stitcher.Entry> {
   private static final Comparator<Stitcher.Holder<?>> HOLDER_COMPARATOR = Comparator.<Stitcher.Holder<?>, Integer>comparing(param0 -> -param0.height)
      .thenComparing(param0 -> -param0.width)
      .thenComparing(param0 -> param0.entry.name());
   private final int mipLevel;
   private final List<Stitcher.Holder<T>> texturesToBeStitched = new ArrayList<>();
   private final List<Stitcher.Region<T>> storage = new ArrayList<>();
   private int storageX;
   private int storageY;
   private final int maxWidth;
   private final int maxHeight;

   public Stitcher(int param0, int param1, int param2) {
      this.mipLevel = param2;
      this.maxWidth = param0;
      this.maxHeight = param1;
   }

   public int getWidth() {
      return this.storageX;
   }

   public int getHeight() {
      return this.storageY;
   }

   public void registerSprite(T param0) {
      Stitcher.Holder<T> var2 = new Stitcher.Holder<>(param0, this.mipLevel);
      this.texturesToBeStitched.add(var2);
   }

   public void stitch() {
      List<Stitcher.Holder<T>> var1 = new ArrayList<>(this.texturesToBeStitched);
      var1.sort(HOLDER_COMPARATOR);

      for (Stitcher.Holder<T> var3 : var1) {
         if (!this.addToStorage(var3)) {
            throw new StitcherException(var3.entry, var1.stream().map(l_param0 -> l_param0.entry).collect(ImmutableList.toImmutableList()));
         }
      }
   }

   public void gatherSprites(Stitcher.SpriteLoader<T> param0) {
      for (Stitcher.Region<T> var3 : this.storage) {
         var3.walk(param0);
      }
   }

   static int smallestFittingMinTexel(int param0, int param1) {
      return (param0 >> param1) + ((param0 & (1 << param1) - 1) == 0 ? 0 : 1) << param1;
   }

   private boolean addToStorage(Stitcher.Holder<T> param0) {
      for (Stitcher.Region<T> var3 : this.storage) {
         if (var3.add(param0)) {
            return true;
         }
      }

      return this.expand(param0);
   }

   private boolean expand(Stitcher.Holder<T> param0) {
      int var3 = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      int var4 = Mth.smallestEncompassingPowerOfTwo(this.storageY);
      int var5 = Mth.smallestEncompassingPowerOfTwo(this.storageX + param0.width);
      int var6 = Mth.smallestEncompassingPowerOfTwo(this.storageY + param0.height);
      boolean var7 = var5 <= this.maxWidth;
      boolean var8 = var6 <= this.maxHeight;
      if (!var7 && !var8) {
         return false;
      }

      boolean var9 = var7 && var3 != var5;
      boolean var10 = var8 && var4 != var6;
      boolean var2;
      if (var9 ^ var10) {
         var2 = var9;
      } else {
         var2 = var7 && var3 <= var4;
      }

      Stitcher.Region<T> var11;
      if (var2) {
         if (this.storageY == 0) {
            this.storageY = var6;
         }

         var11 = new Stitcher.Region<>(this.storageX, 0, var5 - this.storageX, this.storageY);
         this.storageX = var5;
      } else {
         var11 = new Stitcher.Region<>(0, this.storageY, this.storageX, var6 - this.storageY);
         this.storageY = var6;
      }

      var11.add(param0);
      this.storage.add(var11);
      return true;
   }

   public interface Entry {
      int width();

      int height();

      ResourceLocation name();
   }

   record Holder<T extends Stitcher.Entry>(T entry, int width, int height) {

      public Holder(T param0, int param1) {
         this(param0, Stitcher.smallestFittingMinTexel(param0.width(), param1), Stitcher.smallestFittingMinTexel(param0.height(), param1));
      }
   }

   public static class Region<T extends Stitcher.Entry> {
      private final int originX;
      private final int originY;
      private final int width;
      private final int height;
      @Nullable
      private List<Stitcher.Region<T>> subSlots;
      @Nullable
      private Stitcher.Holder<T> holder;

      public Region(int param0, int param1, int param2, int param3) {
         this.originX = param0;
         this.originY = param1;
         this.width = param2;
         this.height = param3;
      }

      public int getX() {
         return this.originX;
      }

      public int getY() {
         return this.originY;
      }

      public boolean add(Stitcher.Holder<T> param0) {
         if (this.holder != null) {
            return false;
         }

         int var2 = param0.width;
         int var3 = param0.height;
         if (var2 <= this.width && var3 <= this.height) {
            if (var2 == this.width && var3 == this.height) {
               this.holder = param0;
               return true;
            }

            if (this.subSlots == null) {
               this.subSlots = new ArrayList<>(1);
               this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY, var2, var3));
               int var4 = this.width - var2;
               int var5 = this.height - var3;
               if (var5 > 0 && var4 > 0) {
                  int var6 = Math.max(this.height, var4);
                  int var7 = Math.max(this.width, var5);
                  if (var6 >= var7) {
                     this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + var3, var2, var5));
                     this.subSlots.add(new Stitcher.Region<>(this.originX + var2, this.originY, var4, this.height));
                  } else {
                     this.subSlots.add(new Stitcher.Region<>(this.originX + var2, this.originY, var4, var3));
                     this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + var3, this.width, var5));
                  }
               } else if (var4 == 0) {
                  this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + var3, var2, var5));
               } else if (var5 == 0) {
                  this.subSlots.add(new Stitcher.Region<>(this.originX + var2, this.originY, var4, var3));
               }
            }

            for (Stitcher.Region<T> var5 : this.subSlots) {
               if (var5.add(param0)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }

      public void walk(Stitcher.SpriteLoader<T> param0) {
         if (this.holder != null) {
            param0.load(this.holder.entry, this.getX(), this.getY());
         } else if (this.subSlots != null) {
            for (Stitcher.Region<T> var3 : this.subSlots) {
               var3.walk(param0);
            }
         }
      }

      @Override
      public String toString() {
         return "Slot{originX="
            + this.originX
            + ", originY="
            + this.originY
            + ", width="
            + this.width
            + ", height="
            + this.height
            + ", texture="
            + this.holder
            + ", subSlots="
            + this.subSlots
            + "}";
      }
   }

   public interface SpriteLoader<T extends Stitcher.Entry> {
      void load(T var1, int var2, int var3);
   }
}
