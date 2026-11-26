package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LiquidBlockRenderer {
   private static final float MAX_FLUID_HEIGHT = 0.8888889F;
   private final TextureAtlasSprite[] lavaIcons = new TextureAtlasSprite[2];
   private final TextureAtlasSprite[] waterIcons = new TextureAtlasSprite[2];
   private TextureAtlasSprite waterOverlay;

   protected void setupSprites() {
      this.lavaIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
      this.lavaIcons[1] = ModelBakery.LAVA_FLOW.sprite();
      this.waterIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
      this.waterIcons[1] = ModelBakery.WATER_FLOW.sprite();
      this.waterOverlay = ModelBakery.WATER_OVERLAY.sprite();
   }

   private static boolean isNeighborSameFluid(FluidState param0, FluidState param1) {
      return param1.getType().isSame(param0.getType());
   }

   private static boolean isFaceOccludedByState(BlockGetter param0, Direction param1, float param2, BlockPos param3, BlockState param4) {
      if (param4.canOcclude()) {
         VoxelShape var5 = Shapes.box(0.0, 0.0, 0.0, 1.0, param2, 1.0);
         VoxelShape var6 = param4.getOcclusionShape(param0, param3);
         return Shapes.blockOccudes(var5, var6, param1);
      } else {
         return false;
      }
   }

   private static boolean isFaceOccludedByNeighbor(BlockGetter param0, BlockPos param1, Direction param2, float param3, BlockState param4) {
      return isFaceOccludedByState(param0, param2, param3, param1.relative(param2), param4);
   }

   private static boolean isFaceOccludedBySelf(BlockGetter param0, BlockPos param1, BlockState param2, Direction param3) {
      return isFaceOccludedByState(param0, param3.getOpposite(), 1.0F, param1, param2);
   }

   public static boolean shouldRenderFace(BlockAndTintGetter param0, BlockPos param1, FluidState param2, BlockState param3, Direction param4, FluidState param5) {
      return !isFaceOccludedBySelf(param0, param1, param3, param4) && !isNeighborSameFluid(param2, param5);
   }

   public void tesselate(BlockAndTintGetter param0, BlockPos param1, VertexConsumer param2, BlockState param3, FluidState param4) {
      boolean var6 = param4.is(FluidTags.LAVA);
      TextureAtlasSprite[] var7 = var6 ? this.lavaIcons : this.waterIcons;
      int var8 = var6 ? 16777215 : BiomeColors.getAverageWaterColor(param0, param1);
      float var9 = (var8 >> 16 & 0xFF) / 255.0F;
      float var10 = (var8 >> 8 & 0xFF) / 255.0F;
      float var11 = (var8 & 0xFF) / 255.0F;
      BlockState var12 = param0.getBlockState(param1.relative(Direction.DOWN));
      FluidState var13 = var12.getFluidState();
      BlockState var14 = param0.getBlockState(param1.relative(Direction.UP));
      FluidState var15 = var14.getFluidState();
      BlockState var16 = param0.getBlockState(param1.relative(Direction.NORTH));
      FluidState var17 = var16.getFluidState();
      BlockState var18 = param0.getBlockState(param1.relative(Direction.SOUTH));
      FluidState var19 = var18.getFluidState();
      BlockState var20 = param0.getBlockState(param1.relative(Direction.WEST));
      FluidState var21 = var20.getFluidState();
      BlockState var22 = param0.getBlockState(param1.relative(Direction.EAST));
      FluidState var23 = var22.getFluidState();
      boolean var24 = !isNeighborSameFluid(param4, var15);
      boolean var25 = shouldRenderFace(param0, param1, param4, param3, Direction.DOWN, var13)
         && !isFaceOccludedByNeighbor(param0, param1, Direction.DOWN, 0.8888889F, var12);
      boolean var26 = shouldRenderFace(param0, param1, param4, param3, Direction.NORTH, var17);
      boolean var27 = shouldRenderFace(param0, param1, param4, param3, Direction.SOUTH, var19);
      boolean var28 = shouldRenderFace(param0, param1, param4, param3, Direction.WEST, var21);
      boolean var29 = shouldRenderFace(param0, param1, param4, param3, Direction.EAST, var23);
      if (var24 || var25 || var29 || var28 || var26 || var27) {
         float var30 = param0.getShade(Direction.DOWN, true);
         float var31 = param0.getShade(Direction.UP, true);
         float var32 = param0.getShade(Direction.NORTH, true);
         float var33 = param0.getShade(Direction.WEST, true);
         Fluid var34 = param4.getType();
         float var39 = this.getHeight(param0, var34, param1, param3, param4);
         float var35;
         float var36;
         float var37;
         float var38;
         if (var39 >= 1.0F) {
            var35 = 1.0F;
            var36 = 1.0F;
            var37 = 1.0F;
            var38 = 1.0F;
         } else {
            float var40 = this.getHeight(param0, var34, param1.north(), var16, var17);
            float var41 = this.getHeight(param0, var34, param1.south(), var18, var19);
            float var42 = this.getHeight(param0, var34, param1.east(), var22, var23);
            float var43 = this.getHeight(param0, var34, param1.west(), var20, var21);
            var35 = this.calculateAverageHeight(param0, var34, var39, var40, var42, param1.relative(Direction.NORTH).relative(Direction.EAST));
            var36 = this.calculateAverageHeight(param0, var34, var39, var40, var43, param1.relative(Direction.NORTH).relative(Direction.WEST));
            var37 = this.calculateAverageHeight(param0, var34, var39, var41, var42, param1.relative(Direction.SOUTH).relative(Direction.EAST));
            var38 = this.calculateAverageHeight(param0, var34, var39, var41, var43, param1.relative(Direction.SOUTH).relative(Direction.WEST));
         }

         double var40 = param1.getX() & 15;
         double var42 = param1.getY() & 15;
         double var44 = param1.getZ() & 15;
         float var46 = 0.001F;
         float var47 = var25 ? 0.001F : 0.0F;
         if (var24 && !isFaceOccludedByNeighbor(param0, param1, Direction.UP, Math.min(Math.min(var36, var38), Math.min(var37, var35)), var14)) {
            var36 -= 0.001F;
            var38 -= 0.001F;
            var37 -= 0.001F;
            var35 -= 0.001F;
            Vec3 var56 = param4.getFlow(param0, param1);
            float var48;
            float var49;
            float var50;
            float var51;
            float var52;
            float var53;
            float var54;
            float var55;
            if (var56.x == 0.0 && var56.z == 0.0) {
               TextureAtlasSprite var57 = var7[0];
               var48 = var57.getU(0.0);
               var52 = var57.getV(0.0);
               var49 = var48;
               var53 = var57.getV(16.0);
               var50 = var57.getU(16.0);
               var54 = var53;
               var51 = var50;
               var55 = var52;
            } else {
               TextureAtlasSprite var57 = var7[1];
               float var58 = (float)Mth.atan2(var56.z, var56.x) - (float) (Math.PI / 2);
               float var59 = Mth.sin(var58) * 0.25F;
               float var60 = Mth.cos(var58) * 0.25F;
               float var61 = 8.0F;
               var48 = var57.getU(8.0F + (-var60 - var59) * 16.0F);
               var52 = var57.getV(8.0F + (-var60 + var59) * 16.0F);
               var49 = var57.getU(8.0F + (-var60 + var59) * 16.0F);
               var53 = var57.getV(8.0F + (var60 + var59) * 16.0F);
               var50 = var57.getU(8.0F + (var60 + var59) * 16.0F);
               var54 = var57.getV(8.0F + (var60 - var59) * 16.0F);
               var51 = var57.getU(8.0F + (var60 - var59) * 16.0F);
               var55 = var57.getV(8.0F + (-var60 - var59) * 16.0F);
            }

            float var57 = (var48 + var49 + var50 + var51) / 4.0F;
            float var58 = (var52 + var53 + var54 + var55) / 4.0F;
            float var59 = var7[0].uvShrinkRatio();
            var48 = Mth.lerp(var59, var48, var57);
            var49 = Mth.lerp(var59, var49, var57);
            var50 = Mth.lerp(var59, var50, var57);
            var51 = Mth.lerp(var59, var51, var57);
            var52 = Mth.lerp(var59, var52, var58);
            var53 = Mth.lerp(var59, var53, var58);
            var54 = Mth.lerp(var59, var54, var58);
            var55 = Mth.lerp(var59, var55, var58);
            int var60 = this.getLightColor(param0, param1);
            float var61 = var31 * var9;
            float var62 = var31 * var10;
            float var63 = var31 * var11;
            this.vertex(param2, var40 + 0.0, var42 + var36, var44 + 0.0, var61, var62, var63, var48, var52, var60);
            this.vertex(param2, var40 + 0.0, var42 + var38, var44 + 1.0, var61, var62, var63, var49, var53, var60);
            this.vertex(param2, var40 + 1.0, var42 + var37, var44 + 1.0, var61, var62, var63, var50, var54, var60);
            this.vertex(param2, var40 + 1.0, var42 + var35, var44 + 0.0, var61, var62, var63, var51, var55, var60);
            if (param4.shouldRenderBackwardUpFace(param0, param1.above())) {
               this.vertex(param2, var40 + 0.0, var42 + var36, var44 + 0.0, var61, var62, var63, var48, var52, var60);
               this.vertex(param2, var40 + 1.0, var42 + var35, var44 + 0.0, var61, var62, var63, var51, var55, var60);
               this.vertex(param2, var40 + 1.0, var42 + var37, var44 + 1.0, var61, var62, var63, var50, var54, var60);
               this.vertex(param2, var40 + 0.0, var42 + var38, var44 + 1.0, var61, var62, var63, var49, var53, var60);
            }
         }

         if (var25) {
            float var48 = var7[0].getU0();
            float var49 = var7[0].getU1();
            float var50 = var7[0].getV0();
            float var51 = var7[0].getV1();
            int var52 = this.getLightColor(param0, param1.below());
            float var53 = var30 * var9;
            float var54 = var30 * var10;
            float var55 = var30 * var11;
            this.vertex(param2, var40, var42 + var47, var44 + 1.0, var53, var54, var55, var48, var51, var52);
            this.vertex(param2, var40, var42 + var47, var44, var53, var54, var55, var48, var50, var52);
            this.vertex(param2, var40 + 1.0, var42 + var47, var44, var53, var54, var55, var49, var50, var52);
            this.vertex(param2, var40 + 1.0, var42 + var47, var44 + 1.0, var53, var54, var55, var49, var51, var52);
         }

         int var48 = this.getLightColor(param0, param1);

         for (Direction var50 : Direction.Plane.HORIZONTAL) {
            float var51;
            float var52;
            double var53;
            double var55;
            double var57;
            double var59;
            boolean var61;
            switch (var50) {
               case NORTH:
                  var51 = var36;
                  var52 = var35;
                  var53 = var40;
                  var57 = var40 + 1.0;
                  var55 = var44 + 0.001F;
                  var59 = var44 + 0.001F;
                  var61 = var26;
                  break;
               case SOUTH:
                  var51 = var37;
                  var52 = var38;
                  var53 = var40 + 1.0;
                  var57 = var40;
                  var55 = var44 + 1.0 - 0.001F;
                  var59 = var44 + 1.0 - 0.001F;
                  var61 = var27;
                  break;
               case WEST:
                  var51 = var38;
                  var52 = var36;
                  var53 = var40 + 0.001F;
                  var57 = var40 + 0.001F;
                  var55 = var44 + 1.0;
                  var59 = var44;
                  var61 = var28;
                  break;
               default:
                  var51 = var35;
                  var52 = var37;
                  var53 = var40 + 1.0 - 0.001F;
                  var57 = var40 + 1.0 - 0.001F;
                  var55 = var44;
                  var59 = var44 + 1.0;
                  var61 = var29;
            }

            if (var61 && !isFaceOccludedByNeighbor(param0, param1, var50, Math.max(var51, var52), param0.getBlockState(param1.relative(var50)))) {
               BlockPos var62 = param1.relative(var50);
               TextureAtlasSprite var63 = var7[1];
               if (!var6) {
                  Block var64 = param0.getBlockState(var62).getBlock();
                  if (var64 instanceof HalfTransparentBlock || var64 instanceof LeavesBlock) {
                     var63 = this.waterOverlay;
                  }
               }

               float var64 = var63.getU(0.0);
               float var65 = var63.getU(8.0);
               float var66 = var63.getV((1.0F - var51) * 16.0F * 0.5F);
               float var67 = var63.getV((1.0F - var52) * 16.0F * 0.5F);
               float var68 = var63.getV(8.0);
               float var69 = var50.getAxis() == Direction.Axis.Z ? var32 : var33;
               float var70 = var31 * var69 * var9;
               float var71 = var31 * var69 * var10;
               float var72 = var31 * var69 * var11;
               this.vertex(param2, var53, var42 + var51, var55, var70, var71, var72, var64, var66, var48);
               this.vertex(param2, var57, var42 + var52, var59, var70, var71, var72, var65, var67, var48);
               this.vertex(param2, var57, var42 + var47, var59, var70, var71, var72, var65, var68, var48);
               this.vertex(param2, var53, var42 + var47, var55, var70, var71, var72, var64, var68, var48);
               if (var63 != this.waterOverlay) {
                  this.vertex(param2, var53, var42 + var47, var55, var70, var71, var72, var64, var68, var48);
                  this.vertex(param2, var57, var42 + var47, var59, var70, var71, var72, var65, var68, var48);
                  this.vertex(param2, var57, var42 + var52, var59, var70, var71, var72, var65, var67, var48);
                  this.vertex(param2, var53, var42 + var51, var55, var70, var71, var72, var64, var66, var48);
               }
            }
         }
      }
   }

   private float calculateAverageHeight(BlockAndTintGetter param0, Fluid param1, float param2, float param3, float param4, BlockPos param5) {
      if (!(param4 >= 1.0F) && !(param3 >= 1.0F)) {
         float[] var7 = new float[2];
         if (param4 > 0.0F || param3 > 0.0F) {
            float var8 = this.getHeight(param0, param1, param5);
            if (var8 >= 1.0F) {
               return 1.0F;
            }

            this.addWeightedHeight(var7, var8);
         }

         this.addWeightedHeight(var7, param2);
         this.addWeightedHeight(var7, param4);
         this.addWeightedHeight(var7, param3);
         return var7[0] / var7[1];
      } else {
         return 1.0F;
      }
   }

   private void addWeightedHeight(float[] param0, float param1) {
      if (param1 >= 0.8F) {
         param0[0] += param1 * 10.0F;
         param0[1] += 10.0F;
      } else if (param1 >= 0.0F) {
         param0[0] += param1;
         param0[1]++;
      }
   }

   private float getHeight(BlockAndTintGetter param0, Fluid param1, BlockPos param2) {
      BlockState var4 = param0.getBlockState(param2);
      return this.getHeight(param0, param1, param2, var4, var4.getFluidState());
   }

   private float getHeight(BlockAndTintGetter param0, Fluid param1, BlockPos param2, BlockState param3, FluidState param4) {
      if (param1.isSame(param4.getType())) {
         BlockState var6 = param0.getBlockState(param2.above());
         return param1.isSame(var6.getFluidState().getType()) ? 1.0F : param4.getOwnHeight();
      } else {
         return !param3.getMaterial().isSolid() ? 0.0F : -1.0F;
      }
   }

   private void vertex(
      VertexConsumer param0, double param1, double param3, double param5, float param7, float param8, float param9, float param10, float param11, int param12
   ) {
      param0.vertex(param1, param3, param5).color(param7, param8, param9, 1.0F).uv(param10, param11).uv2(param12).normal(0.0F, 1.0F, 0.0F).endVertex();
   }

   private int getLightColor(BlockAndTintGetter param0, BlockPos param1) {
      int var3 = LevelRenderer.getLightColor(param0, param1);
      int var4 = LevelRenderer.getLightColor(param0, param1.above());
      int var5 = var3 & 0xFF;
      int var6 = var4 & 0xFF;
      int var7 = var3 >> 16 & 0xFF;
      int var8 = var4 >> 16 & 0xFF;
      return (var5 > var6 ? var5 : var6) | (var7 > var8 ? var7 : var8) << 16;
   }
}
