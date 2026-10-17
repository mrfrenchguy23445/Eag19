package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

public class ItemModelGenerator {
   public static final List<String> LAYERS = Lists.newArrayList("layer0", "layer1", "layer2", "layer3", "layer4");
   private static final float MIN_Z = 7.5F;
   private static final float MAX_Z = 8.5F;

   public BlockModel generateBlockModel(Function<Material, TextureAtlasSprite> param0, BlockModel param1) {
      Map<String, Either<Material, String>> var3 = Maps.newHashMap();
      List<BlockElement> var4 = Lists.newArrayList();

      for (int var5 = 0; var5 < LAYERS.size(); var5++) {
         String var6 = LAYERS.get(var5);
         if (!param1.hasTexture(var6)) {
            break;
         }

         Material var7 = param1.getMaterial(var6);
         var3.put(var6, Either.left(var7));
         SpriteContents var8 = param0.apply(var7).contents();
         var4.addAll(this.processFrames(var5, var6, var8));
      }

      var3.put("particle", param1.hasTexture("particle") ? Either.left(param1.getMaterial("particle")) : var3.get("layer0"));
      BlockModel var5 = new BlockModel(null, var4, var3, false, param1.getGuiLight(), param1.getTransforms(), param1.getOverrides());
      var5.name = param1.name;
      return var5;
   }

   private List<BlockElement> processFrames(int param0, String param1, SpriteContents param2) {
      Map<Direction, BlockElementFace> var4 = Maps.newHashMap();
      var4.put(Direction.SOUTH, new BlockElementFace(null, param0, param1, new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)));
      var4.put(Direction.NORTH, new BlockElementFace(null, param0, param1, new BlockFaceUV(new float[]{16.0F, 0.0F, 0.0F, 16.0F}, 0)));
      List<BlockElement> var5 = Lists.newArrayList();
      var5.add(new BlockElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), var4, null, true));
      var5.addAll(this.createSideElements(param2, param1, param0));
      return var5;
   }

   private List<BlockElement> createSideElements(SpriteContents param0, String param1, int param2) {
      float var4 = param0.width();
      float var5 = param0.height();
      List<BlockElement> var6 = Lists.newArrayList();

      for (ItemModelGenerator.Span var8 : this.getSpans(param0)) {
         float var9 = 0.0F;
         float var10 = 0.0F;
         float var11 = 0.0F;
         float var12 = 0.0F;
         float var13 = 0.0F;
         float var14 = 0.0F;
         float var15 = 0.0F;
         float var16 = 0.0F;
         float var17 = 16.0F / var4;
         float var18 = 16.0F / var5;
         float var19 = var8.getMin();
         float var20 = var8.getMax();
         float var21 = var8.getAnchor();
         ItemModelGenerator.SpanFacing var22 = var8.getFacing();
         switch (var22) {
            case UP:
               var13 = var19;
               var9 = var19;
               var11 = var14 = var20 + 1.0F;
               var15 = var21;
               var10 = var21;
               var12 = var21;
               var16 = var21 + 1.0F;
               break;
            case DOWN:
               var15 = var21;
               var16 = var21 + 1.0F;
               var13 = var19;
               var9 = var19;
               var11 = var14 = var20 + 1.0F;
               var10 = var21 + 1.0F;
               var12 = var21 + 1.0F;
               break;
            case LEFT:
               var13 = var21;
               var9 = var21;
               var11 = var21;
               var14 = var21 + 1.0F;
               var16 = var19;
               var10 = var19;
               var12 = var15 = var20 + 1.0F;
               break;
            case RIGHT:
               var13 = var21;
               var14 = var21 + 1.0F;
               var9 = var21 + 1.0F;
               var11 = var21 + 1.0F;
               var16 = var19;
               var10 = var19;
               var12 = var15 = var20 + 1.0F;
         }

         var9 *= var17;
         var11 *= var17;
         var10 *= var18;
         var12 *= var18;
         var10 = 16.0F - var10;
         var12 = 16.0F - var12;
         var13 *= var17;
         var14 *= var17;
         var15 *= var18;
         var16 *= var18;
         Map<Direction, BlockElementFace> var23 = Maps.newHashMap();
         var23.put(var22.getDirection(), new BlockElementFace(null, param2, param1, new BlockFaceUV(new float[]{var13, var15, var14, var16}, 0)));
         switch (var22) {
            case UP:
               var6.add(new BlockElement(new Vector3f(var9, var10, 7.5F), new Vector3f(var11, var10, 8.5F), var23, null, true));
               break;
            case DOWN:
               var6.add(new BlockElement(new Vector3f(var9, var12, 7.5F), new Vector3f(var11, var12, 8.5F), var23, null, true));
               break;
            case LEFT:
               var6.add(new BlockElement(new Vector3f(var9, var10, 7.5F), new Vector3f(var9, var12, 8.5F), var23, null, true));
               break;
            case RIGHT:
               var6.add(new BlockElement(new Vector3f(var11, var10, 7.5F), new Vector3f(var11, var12, 8.5F), var23, null, true));
         }
      }

      return var6;
   }

   private List<ItemModelGenerator.Span> getSpans(SpriteContents param0) {
      int var2 = param0.width();
      int var3 = param0.height();
      List<ItemModelGenerator.Span> var4 = Lists.newArrayList();
      param0.getUniqueFrames().forEach(l_param4 -> {
         for (int l_var6 = 0; l_var6 < var3; l_var6++) {
            for (int l_var7 = 0; l_var7 < var2; l_var7++) {
               boolean l_var8 = !this.isTransparent(param0, l_param4, l_var7, l_var6, var2, var3);
               this.checkTransition(ItemModelGenerator.SpanFacing.UP, var4, param0, l_param4, l_var7, l_var6, var2, var3, l_var8);
               this.checkTransition(ItemModelGenerator.SpanFacing.DOWN, var4, param0, l_param4, l_var7, l_var6, var2, var3, l_var8);
               this.checkTransition(ItemModelGenerator.SpanFacing.LEFT, var4, param0, l_param4, l_var7, l_var6, var2, var3, l_var8);
               this.checkTransition(ItemModelGenerator.SpanFacing.RIGHT, var4, param0, l_param4, l_var7, l_var6, var2, var3, l_var8);
            }
         }
      });
      return var4;
   }

   private void checkTransition(
      ItemModelGenerator.SpanFacing param0,
      List<ItemModelGenerator.Span> param1,
      SpriteContents param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      boolean param8
   ) {
      boolean var10 = this.isTransparent(param2, param3, param4 + param0.getXOffset(), param5 + param0.getYOffset(), param6, param7) && param8;
      if (var10) {
         this.createOrExpandSpan(param1, param0, param4, param5);
      }
   }

   private void createOrExpandSpan(List<ItemModelGenerator.Span> param0, ItemModelGenerator.SpanFacing param1, int param2, int param3) {
      ItemModelGenerator.Span var5 = null;

      for (ItemModelGenerator.Span var7 : param0) {
         if (var7.getFacing() == param1) {
            int var8 = param1.isHorizontal() ? param3 : param2;
            if (var7.getAnchor() == var8) {
               var5 = var7;
               break;
            }
         }
      }

      int var6 = param1.isHorizontal() ? param3 : param2;
      int var7 = param1.isHorizontal() ? param2 : param3;
      if (var5 == null) {
         param0.add(new ItemModelGenerator.Span(param1, var7, var6));
      } else {
         var5.expand(var7);
      }
   }

   private boolean isTransparent(SpriteContents param0, int param1, int param2, int param3, int param4, int param5) {
      return param2 >= 0 && param3 >= 0 && param2 < param4 && param3 < param5 ? param0.isTransparent(param1, param2, param3) : true;
   }

   static class Span {
      private final ItemModelGenerator.SpanFacing facing;
      private int min;
      private int max;
      private final int anchor;

      public Span(ItemModelGenerator.SpanFacing param0, int param1, int param2) {
         this.facing = param0;
         this.min = param1;
         this.max = param1;
         this.anchor = param2;
      }

      public void expand(int param0) {
         if (param0 < this.min) {
            this.min = param0;
         } else if (param0 > this.max) {
            this.max = param0;
         }
      }

      public ItemModelGenerator.SpanFacing getFacing() {
         return this.facing;
      }

      public int getMin() {
         return this.min;
      }

      public int getMax() {
         return this.max;
      }

      public int getAnchor() {
         return this.anchor;
      }
   }

   enum SpanFacing {
      UP(Direction.UP, 0, -1),
      DOWN(Direction.DOWN, 0, 1),
      LEFT(Direction.EAST, -1, 0),
      RIGHT(Direction.WEST, 1, 0);

      private final Direction direction;
      private final int xOffset;
      private final int yOffset;

      SpanFacing(Direction param2, int param3, int param4) {
         this.direction = param2;
         this.xOffset = param3;
         this.yOffset = param4;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public int getXOffset() {
         return this.xOffset;
      }

      public int getYOffset() {
         return this.yOffset;
      }

      boolean isHorizontal() {
         return this == DOWN || this == UP;
      }
   }
}
