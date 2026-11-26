package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class FontTexture extends AbstractTexture {
   private static final int SIZE = 256;
   private final ResourceLocation name;
   private final RenderType normalType;
   private final RenderType seeThroughType;
   private final RenderType polygonOffsetType;
   private final boolean colored;
   private final FontTexture.Node root;

   public FontTexture(ResourceLocation param0, boolean param1) {
      this.name = param0;
      this.colored = param1;
      this.root = new FontTexture.Node(0, 0, 256, 256);
      TextureUtil.prepareImage(param1 ? NativeImage.InternalGlFormat.RGBA : NativeImage.InternalGlFormat.RED, this.getId(), 256, 256);
      this.normalType = param1 ? RenderType.text(param0) : RenderType.textIntensity(param0);
      this.seeThroughType = param1 ? RenderType.textSeeThrough(param0) : RenderType.textIntensitySeeThrough(param0);
      this.polygonOffsetType = param1 ? RenderType.textPolygonOffset(param0) : RenderType.textIntensityPolygonOffset(param0);
   }

   @Override
   public void load(ResourceManager param0) {
   }

   @Override
   public void close() {
      this.releaseId();
   }

   @Nullable
   public BakedGlyph add(SheetGlyphInfo param0) {
      if (param0.isColored() != this.colored) {
         return null;
      } else {
         FontTexture.Node var2 = this.root.insert(param0);
         if (var2 != null) {
            this.bind();
            param0.upload(var2.x, var2.y);
            float var3 = 256.0F;
            float var4 = 256.0F;
            float var5 = 0.01F;
            return new BakedGlyph(
               this.normalType,
               this.seeThroughType,
               this.polygonOffsetType,
               (var2.x + 0.01F) / 256.0F,
               (var2.x - 0.01F + param0.getPixelWidth()) / 256.0F,
               (var2.y + 0.01F) / 256.0F,
               (var2.y - 0.01F + param0.getPixelHeight()) / 256.0F,
               param0.getLeft(),
               param0.getRight(),
               param0.getUp(),
               param0.getDown()
            );
         } else {
            return null;
         }
      }
   }

   public ResourceLocation getName() {
      return this.name;
   }

   static class Node {
      final int x;
      final int y;
      private final int width;
      private final int height;
      @Nullable
      private FontTexture.Node left;
      @Nullable
      private FontTexture.Node right;
      private boolean occupied;

      Node(int param0, int param1, int param2, int param3) {
         this.x = param0;
         this.y = param1;
         this.width = param2;
         this.height = param3;
      }

      @Nullable
      FontTexture.Node insert(SheetGlyphInfo param0) {
         if (this.left != null && this.right != null) {
            FontTexture.Node var2 = this.left.insert(param0);
            if (var2 == null) {
               var2 = this.right.insert(param0);
            }

            return var2;
         } else {
            if (this.occupied) {
               return null;
            }

            int var2 = param0.getPixelWidth();
            int var3 = param0.getPixelHeight();
            if (var2 > this.width || var3 > this.height) {
               return null;
            }

            if (var2 == this.width && var3 == this.height) {
               this.occupied = true;
               return this;
            }

            int var4 = this.width - var2;
            int var5 = this.height - var3;
            if (var4 > var5) {
               this.left = new FontTexture.Node(this.x, this.y, var2, this.height);
               this.right = new FontTexture.Node(this.x + var2 + 1, this.y, this.width - var2 - 1, this.height);
            } else {
               this.left = new FontTexture.Node(this.x, this.y, this.width, var3);
               this.right = new FontTexture.Node(this.x, this.y + var3 + 1, this.width, this.height - var3 - 1);
            }

            return this.left.insert(param0);
         }
      }
   }
}
