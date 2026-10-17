package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ModelBlockRenderer {
   private static final int FACE_CUBIC = 0;
   private static final int FACE_PARTIAL = 1;
   static final Direction[] DIRECTIONS = Direction.values();
   private final BlockColors blockColors;
   private static final int CACHE_SIZE = 100;
   static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);

   public ModelBlockRenderer(BlockColors param0) {
      this.blockColors = param0;
   }

   public void tesselateBlock(
      BlockAndTintGetter param0,
      BakedModel param1,
      BlockState param2,
      BlockPos param3,
      PoseStack param4,
      VertexConsumer param5,
      boolean param6,
      RandomSource param7,
      long param8,
      int param10
   ) {
      boolean var12 = Minecraft.useAmbientOcclusion() && param2.getLightEmission() == 0 && param1.useAmbientOcclusion();
      Vec3 var13 = param2.getOffset(param0, param3);
      param4.translate(var13.x, var13.y, var13.z);

      try {
         if (var12) {
            this.tesselateWithAO(param0, param1, param2, param3, param4, param5, param6, param7, param8, param10);
         } else {
            this.tesselateWithoutAO(param0, param1, param2, param3, param4, param5, param6, param7, param8, param10);
         }
      } catch (Throwable var14) {
         CrashReport var15 = CrashReport.forThrowable(var14, "Tesselating block model");
         CrashReportCategory var16 = var15.addCategory("Block model being tesselated");
         CrashReportCategory.populateBlockDetails(var16, param0, param3, param2);
         var16.setDetail("Using AO", var12);
         throw new ReportedException(var15);
      }
   }

   public void tesselateWithAO(
      BlockAndTintGetter param0,
      BakedModel param1,
      BlockState param2,
      BlockPos param3,
      PoseStack param4,
      VertexConsumer param5,
      boolean param6,
      RandomSource param7,
      long param8,
      int param10
   ) {
      float[] var12 = new float[DIRECTIONS.length * 2];
      BitSet var13 = new BitSet(3);
      ModelBlockRenderer.AmbientOcclusionFace var14 = new ModelBlockRenderer.AmbientOcclusionFace();
      BlockPos.MutableBlockPos var15 = param3.mutable();

      for (Direction var19 : DIRECTIONS) {
         param7.setSeed(param8);
         List<BakedQuad> var20 = param1.getQuads(param2, var19, param7);
         if (!var20.isEmpty()) {
            var15.setWithOffset(param3, var19);
            if (!param6 || Block.shouldRenderFace(param2, param0, param3, var19, var15)) {
               this.renderModelFaceAO(param0, param2, param3, param4, param5, var20, var12, var13, var14, param10);
            }
         }
      }

      param7.setSeed(param8);
      List<BakedQuad> var16 = param1.getQuads(param2, null, param7);
      if (!var16.isEmpty()) {
         this.renderModelFaceAO(param0, param2, param3, param4, param5, var16, var12, var13, var14, param10);
      }
   }

   public void tesselateWithoutAO(
      BlockAndTintGetter param0,
      BakedModel param1,
      BlockState param2,
      BlockPos param3,
      PoseStack param4,
      VertexConsumer param5,
      boolean param6,
      RandomSource param7,
      long param8,
      int param10
   ) {
      BitSet var12 = new BitSet(3);
      BlockPos.MutableBlockPos var13 = param3.mutable();

      for (Direction var17 : DIRECTIONS) {
         param7.setSeed(param8);
         List<BakedQuad> var18 = param1.getQuads(param2, var17, param7);
         if (!var18.isEmpty()) {
            var13.setWithOffset(param3, var17);
            if (!param6 || Block.shouldRenderFace(param2, param0, param3, var17, var13)) {
               int var19 = LevelRenderer.getLightColor(param0, param2, var13);
               this.renderModelFaceFlat(param0, param2, param3, var19, param10, false, param4, param5, var18, var12);
            }
         }
      }

      param7.setSeed(param8);
      List<BakedQuad> var14 = param1.getQuads(param2, null, param7);
      if (!var14.isEmpty()) {
         this.renderModelFaceFlat(param0, param2, param3, -1, param10, true, param4, param5, var14, var12);
      }
   }

   private void renderModelFaceAO(
      BlockAndTintGetter param0,
      BlockState param1,
      BlockPos param2,
      PoseStack param3,
      VertexConsumer param4,
      List<BakedQuad> param5,
      float[] param6,
      BitSet param7,
      ModelBlockRenderer.AmbientOcclusionFace param8,
      int param9
   ) {
      for (BakedQuad var12 : param5) {
         this.calculateShape(param0, param1, param2, var12.getVertices(), var12.getDirection(), param6, param7);
         param8.calculate(param0, param1, param2, var12.getDirection(), param6, param7, var12.isShade());
         this.putQuadData(
            param0,
            param1,
            param2,
            param4,
            param3.last(),
            var12,
            param8.brightness[0],
            param8.brightness[1],
            param8.brightness[2],
            param8.brightness[3],
            param8.lightmap[0],
            param8.lightmap[1],
            param8.lightmap[2],
            param8.lightmap[3],
            param9
         );
      }
   }

   private void putQuadData(
      BlockAndTintGetter param0,
      BlockState param1,
      BlockPos param2,
      VertexConsumer param3,
      PoseStack.Pose param4,
      BakedQuad param5,
      float param6,
      float param7,
      float param8,
      float param9,
      int param10,
      int param11,
      int param12,
      int param13,
      int param14
   ) {
      float var16;
      float var17;
      float var18;
      if (param5.isTinted()) {
         int var19 = this.blockColors.getColor(param1, param0, param2, param5.getTintIndex());
         var16 = (var19 >> 16 & 0xFF) / 255.0F;
         var17 = (var19 >> 8 & 0xFF) / 255.0F;
         var18 = (var19 & 0xFF) / 255.0F;
      } else {
         var16 = 1.0F;
         var17 = 1.0F;
         var18 = 1.0F;
      }

      param3.putBulkData(
         param4, param5, new float[]{param6, param7, param8, param9}, var16, var17, var18, new int[]{param10, param11, param12, param13}, param14, true
      );
   }

   private void calculateShape(
      BlockAndTintGetter param0, BlockState param1, BlockPos param2, int[] param3, Direction param4, @Nullable float[] param5, BitSet param6
   ) {
      float var8 = 32.0F;
      float var9 = 32.0F;
      float var10 = 32.0F;
      float var11 = -32.0F;
      float var12 = -32.0F;
      float var13 = -32.0F;

      for (int var14 = 0; var14 < 4; var14++) {
         float var15 = Float.intBitsToFloat(param3[var14 * 8]);
         float var16 = Float.intBitsToFloat(param3[var14 * 8 + 1]);
         float var17 = Float.intBitsToFloat(param3[var14 * 8 + 2]);
         var8 = Math.min(var8, var15);
         var9 = Math.min(var9, var16);
         var10 = Math.min(var10, var17);
         var11 = Math.max(var11, var15);
         var12 = Math.max(var12, var16);
         var13 = Math.max(var13, var17);
      }

      if (param5 != null) {
         param5[Direction.WEST.get3DDataValue()] = var8;
         param5[Direction.EAST.get3DDataValue()] = var11;
         param5[Direction.DOWN.get3DDataValue()] = var9;
         param5[Direction.UP.get3DDataValue()] = var12;
         param5[Direction.NORTH.get3DDataValue()] = var10;
         param5[Direction.SOUTH.get3DDataValue()] = var13;
         int var14 = DIRECTIONS.length;
         param5[Direction.WEST.get3DDataValue() + var14] = 1.0F - var8;
         param5[Direction.EAST.get3DDataValue() + var14] = 1.0F - var11;
         param5[Direction.DOWN.get3DDataValue() + var14] = 1.0F - var9;
         param5[Direction.UP.get3DDataValue() + var14] = 1.0F - var12;
         param5[Direction.NORTH.get3DDataValue() + var14] = 1.0F - var10;
         param5[Direction.SOUTH.get3DDataValue() + var14] = 1.0F - var13;
      }

      float var14 = 1.0E-4F;
      float var15 = 0.9999F;
      switch (param4) {
         case DOWN:
            param6.set(1, var8 >= 1.0E-4F || var10 >= 1.0E-4F || var11 <= 0.9999F || var13 <= 0.9999F);
            param6.set(0, var9 == var12 && (var9 < 1.0E-4F || param1.isCollisionShapeFullBlock(param0, param2)));
            break;
         case UP:
            param6.set(1, var8 >= 1.0E-4F || var10 >= 1.0E-4F || var11 <= 0.9999F || var13 <= 0.9999F);
            param6.set(0, var9 == var12 && (var12 > 0.9999F || param1.isCollisionShapeFullBlock(param0, param2)));
            break;
         case NORTH:
            param6.set(1, var8 >= 1.0E-4F || var9 >= 1.0E-4F || var11 <= 0.9999F || var12 <= 0.9999F);
            param6.set(0, var10 == var13 && (var10 < 1.0E-4F || param1.isCollisionShapeFullBlock(param0, param2)));
            break;
         case SOUTH:
            param6.set(1, var8 >= 1.0E-4F || var9 >= 1.0E-4F || var11 <= 0.9999F || var12 <= 0.9999F);
            param6.set(0, var10 == var13 && (var13 > 0.9999F || param1.isCollisionShapeFullBlock(param0, param2)));
            break;
         case WEST:
            param6.set(1, var9 >= 1.0E-4F || var10 >= 1.0E-4F || var12 <= 0.9999F || var13 <= 0.9999F);
            param6.set(0, var8 == var11 && (var8 < 1.0E-4F || param1.isCollisionShapeFullBlock(param0, param2)));
            break;
         case EAST:
            param6.set(1, var9 >= 1.0E-4F || var10 >= 1.0E-4F || var12 <= 0.9999F || var13 <= 0.9999F);
            param6.set(0, var8 == var11 && (var11 > 0.9999F || param1.isCollisionShapeFullBlock(param0, param2)));
      }
   }

   private void renderModelFaceFlat(
      BlockAndTintGetter param0,
      BlockState param1,
      BlockPos param2,
      int param3,
      int param4,
      boolean param5,
      PoseStack param6,
      VertexConsumer param7,
      List<BakedQuad> param8,
      BitSet param9
   ) {
      for (BakedQuad var12 : param8) {
         if (param5) {
            this.calculateShape(param0, param1, param2, var12.getVertices(), var12.getDirection(), null, param9);
            BlockPos var13 = param9.get(0) ? param2.relative(var12.getDirection()) : param2;
            param3 = LevelRenderer.getLightColor(param0, param1, var13);
         }

         float var13 = param0.getShade(var12.getDirection(), var12.isShade());
         this.putQuadData(param0, param1, param2, param7, param6.last(), var12, var13, var13, var13, var13, param3, param3, param3, param3, param4);
      }
   }

   public void renderModel(
      PoseStack.Pose param0,
      VertexConsumer param1,
      @Nullable BlockState param2,
      BakedModel param3,
      float param4,
      float param5,
      float param6,
      int param7,
      int param8
   ) {
      RandomSource var10 = RandomSource.create();
      long var11 = 42L;

      for (Direction var16 : DIRECTIONS) {
         var10.setSeed(42L);
         renderQuadList(param0, param1, param4, param5, param6, param3.getQuads(param2, var16, var10), param7, param8);
      }

      var10.setSeed(42L);
      renderQuadList(param0, param1, param4, param5, param6, param3.getQuads(param2, null, var10), param7, param8);
   }

   private static void renderQuadList(
      PoseStack.Pose param0, VertexConsumer param1, float param2, float param3, float param4, List<BakedQuad> param5, int param6, int param7
   ) {
      for (BakedQuad var9 : param5) {
         float var10;
         float var11;
         float var12;
         if (var9.isTinted()) {
            var10 = Mth.clamp(param2, 0.0F, 1.0F);
            var11 = Mth.clamp(param3, 0.0F, 1.0F);
            var12 = Mth.clamp(param4, 0.0F, 1.0F);
         } else {
            var10 = 1.0F;
            var11 = 1.0F;
            var12 = 1.0F;
         }

         param1.putBulkData(param0, var9, var10, var11, var12, param6, param7);
      }
   }

   public static void enableCaching() {
      CACHE.get().enable();
   }

   public static void clearCache() {
      CACHE.get().disable();
   }

   protected enum AdjacencyInfo {
      DOWN(
         new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
         0.5F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      UP(
         new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
         1.0F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      NORTH(
         new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
         0.8F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST
         }
      ),
      SOUTH(
         new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
         0.8F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.EAST
         }
      ),
      WEST(
         new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
         0.6F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      EAST(
         new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
         0.6F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      );

      final Direction[] corners;
      final boolean doNonCubicWeight;
      final ModelBlockRenderer.SizeInfo[] vert0Weights;
      final ModelBlockRenderer.SizeInfo[] vert1Weights;
      final ModelBlockRenderer.SizeInfo[] vert2Weights;
      final ModelBlockRenderer.SizeInfo[] vert3Weights;
      private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], param0 -> {
         param0[Direction.DOWN.get3DDataValue()] = DOWN;
         param0[Direction.UP.get3DDataValue()] = UP;
         param0[Direction.NORTH.get3DDataValue()] = NORTH;
         param0[Direction.SOUTH.get3DDataValue()] = SOUTH;
         param0[Direction.WEST.get3DDataValue()] = WEST;
         param0[Direction.EAST.get3DDataValue()] = EAST;
      });

      AdjacencyInfo(
         Direction[] param2,
         float param3,
         boolean param4,
         ModelBlockRenderer.SizeInfo[] param5,
         ModelBlockRenderer.SizeInfo[] param6,
         ModelBlockRenderer.SizeInfo[] param7,
         ModelBlockRenderer.SizeInfo[] param8
      ) {
         this.corners = param2;
         this.doNonCubicWeight = param4;
         this.vert0Weights = param5;
         this.vert1Weights = param6;
         this.vert2Weights = param7;
         this.vert3Weights = param8;
      }

      public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction param0) {
         return BY_FACING[param0.get3DDataValue()];
      }
   }

   static class AmbientOcclusionFace {
      final float[] brightness = new float[4];
      final int[] lightmap = new int[4];

      public AmbientOcclusionFace() {
      }

      public void calculate(BlockAndTintGetter param0, BlockState param1, BlockPos param2, Direction param3, float[] param4, BitSet param5, boolean param6) {
         BlockPos var8 = param5.get(0) ? param2.relative(param3) : param2;
         ModelBlockRenderer.AdjacencyInfo var9 = ModelBlockRenderer.AdjacencyInfo.fromFacing(param3);
         BlockPos.MutableBlockPos var10 = new BlockPos.MutableBlockPos();
         ModelBlockRenderer.Cache var11 = ModelBlockRenderer.CACHE.get();
         var10.setWithOffset(var8, var9.corners[0]);
         BlockState var12 = param0.getBlockState(var10);
         int var13 = var11.getLightColor(var12, param0, var10);
         float var14 = var11.getShadeBrightness(var12, param0, var10);
         var10.setWithOffset(var8, var9.corners[1]);
         BlockState var15 = param0.getBlockState(var10);
         int var16 = var11.getLightColor(var15, param0, var10);
         float var17 = var11.getShadeBrightness(var15, param0, var10);
         var10.setWithOffset(var8, var9.corners[2]);
         BlockState var18 = param0.getBlockState(var10);
         int var19 = var11.getLightColor(var18, param0, var10);
         float var20 = var11.getShadeBrightness(var18, param0, var10);
         var10.setWithOffset(var8, var9.corners[3]);
         BlockState var21 = param0.getBlockState(var10);
         int var22 = var11.getLightColor(var21, param0, var10);
         float var23 = var11.getShadeBrightness(var21, param0, var10);
         BlockState var24 = param0.getBlockState(var10.setWithOffset(var8, var9.corners[0]).move(param3));
         boolean var25 = !var24.isViewBlocking(param0, var10) || var24.getLightBlock(param0, var10) == 0;
         BlockState var26 = param0.getBlockState(var10.setWithOffset(var8, var9.corners[1]).move(param3));
         boolean var27 = !var26.isViewBlocking(param0, var10) || var26.getLightBlock(param0, var10) == 0;
         BlockState var28 = param0.getBlockState(var10.setWithOffset(var8, var9.corners[2]).move(param3));
         boolean var29 = !var28.isViewBlocking(param0, var10) || var28.getLightBlock(param0, var10) == 0;
         BlockState var30 = param0.getBlockState(var10.setWithOffset(var8, var9.corners[3]).move(param3));
         boolean var31 = !var30.isViewBlocking(param0, var10) || var30.getLightBlock(param0, var10) == 0;
         float var32;
         int var36;
         if (!var29 && !var25) {
            var32 = var14;
            var36 = var13;
         } else {
            var10.setWithOffset(var8, var9.corners[0]).move(var9.corners[2]);
            BlockState var40 = param0.getBlockState(var10);
            var32 = var11.getShadeBrightness(var40, param0, var10);
            var36 = var11.getLightColor(var40, param0, var10);
         }

         float var33;
         int var37;
         if (!var31 && !var25) {
            var33 = var14;
            var37 = var13;
         } else {
            var10.setWithOffset(var8, var9.corners[0]).move(var9.corners[3]);
            BlockState var40 = param0.getBlockState(var10);
            var33 = var11.getShadeBrightness(var40, param0, var10);
            var37 = var11.getLightColor(var40, param0, var10);
         }

         float var34;
         int var38;
         if (!var29 && !var27) {
            var34 = var14;
            var38 = var13;
         } else {
            var10.setWithOffset(var8, var9.corners[1]).move(var9.corners[2]);
            BlockState var40 = param0.getBlockState(var10);
            var34 = var11.getShadeBrightness(var40, param0, var10);
            var38 = var11.getLightColor(var40, param0, var10);
         }

         float var35;
         int var39;
         if (!var31 && !var27) {
            var35 = var14;
            var39 = var13;
         } else {
            var10.setWithOffset(var8, var9.corners[1]).move(var9.corners[3]);
            BlockState var40 = param0.getBlockState(var10);
            var35 = var11.getShadeBrightness(var40, param0, var10);
            var39 = var11.getLightColor(var40, param0, var10);
         }

         int var40 = var11.getLightColor(param1, param0, param2);
         var10.setWithOffset(param2, param3);
         BlockState var41 = param0.getBlockState(var10);
         if (param5.get(0) || !var41.isSolidRender(param0, var10)) {
            var40 = var11.getLightColor(var41, param0, var10);
         }

         float var42 = param5.get(0)
            ? var11.getShadeBrightness(param0.getBlockState(var8), param0, var8)
            : var11.getShadeBrightness(param0.getBlockState(param2), param0, param2);
         ModelBlockRenderer.AmbientVertexRemap var43 = ModelBlockRenderer.AmbientVertexRemap.fromFacing(param3);
         if (param5.get(1) && var9.doNonCubicWeight) {
            float var44 = (var23 + var14 + var33 + var42) * 0.25F;
            float var45 = (var20 + var14 + var32 + var42) * 0.25F;
            float var46 = (var20 + var17 + var34 + var42) * 0.25F;
            float var47 = (var23 + var17 + var35 + var42) * 0.25F;
            float var48 = param4[var9.vert0Weights[0].shape] * param4[var9.vert0Weights[1].shape];
            float var49 = param4[var9.vert0Weights[2].shape] * param4[var9.vert0Weights[3].shape];
            float var50 = param4[var9.vert0Weights[4].shape] * param4[var9.vert0Weights[5].shape];
            float var51 = param4[var9.vert0Weights[6].shape] * param4[var9.vert0Weights[7].shape];
            float var52 = param4[var9.vert1Weights[0].shape] * param4[var9.vert1Weights[1].shape];
            float var53 = param4[var9.vert1Weights[2].shape] * param4[var9.vert1Weights[3].shape];
            float var54 = param4[var9.vert1Weights[4].shape] * param4[var9.vert1Weights[5].shape];
            float var55 = param4[var9.vert1Weights[6].shape] * param4[var9.vert1Weights[7].shape];
            float var56 = param4[var9.vert2Weights[0].shape] * param4[var9.vert2Weights[1].shape];
            float var57 = param4[var9.vert2Weights[2].shape] * param4[var9.vert2Weights[3].shape];
            float var58 = param4[var9.vert2Weights[4].shape] * param4[var9.vert2Weights[5].shape];
            float var59 = param4[var9.vert2Weights[6].shape] * param4[var9.vert2Weights[7].shape];
            float var60 = param4[var9.vert3Weights[0].shape] * param4[var9.vert3Weights[1].shape];
            float var61 = param4[var9.vert3Weights[2].shape] * param4[var9.vert3Weights[3].shape];
            float var62 = param4[var9.vert3Weights[4].shape] * param4[var9.vert3Weights[5].shape];
            float var63 = param4[var9.vert3Weights[6].shape] * param4[var9.vert3Weights[7].shape];
            this.brightness[var43.vert0] = var44 * var48 + var45 * var49 + var46 * var50 + var47 * var51;
            this.brightness[var43.vert1] = var44 * var52 + var45 * var53 + var46 * var54 + var47 * var55;
            this.brightness[var43.vert2] = var44 * var56 + var45 * var57 + var46 * var58 + var47 * var59;
            this.brightness[var43.vert3] = var44 * var60 + var45 * var61 + var46 * var62 + var47 * var63;
            int var64 = this.blend(var22, var13, var37, var40);
            int var65 = this.blend(var19, var13, var36, var40);
            int var66 = this.blend(var19, var16, var38, var40);
            int var67 = this.blend(var22, var16, var39, var40);
            this.lightmap[var43.vert0] = this.blend(var64, var65, var66, var67, var48, var49, var50, var51);
            this.lightmap[var43.vert1] = this.blend(var64, var65, var66, var67, var52, var53, var54, var55);
            this.lightmap[var43.vert2] = this.blend(var64, var65, var66, var67, var56, var57, var58, var59);
            this.lightmap[var43.vert3] = this.blend(var64, var65, var66, var67, var60, var61, var62, var63);
         } else {
            float var44 = (var23 + var14 + var33 + var42) * 0.25F;
            float var45 = (var20 + var14 + var32 + var42) * 0.25F;
            float var46 = (var20 + var17 + var34 + var42) * 0.25F;
            float var47 = (var23 + var17 + var35 + var42) * 0.25F;
            this.lightmap[var43.vert0] = this.blend(var22, var13, var37, var40);
            this.lightmap[var43.vert1] = this.blend(var19, var13, var36, var40);
            this.lightmap[var43.vert2] = this.blend(var19, var16, var38, var40);
            this.lightmap[var43.vert3] = this.blend(var22, var16, var39, var40);
            this.brightness[var43.vert0] = var44;
            this.brightness[var43.vert1] = var45;
            this.brightness[var43.vert2] = var46;
            this.brightness[var43.vert3] = var47;
         }

         float var44 = param0.getShade(param3, param6);

         for (int var45 = 0; var45 < this.brightness.length; var45++) {
            this.brightness[var45] = this.brightness[var45] * var44;
         }
      }

      private int blend(int param0, int param1, int param2, int param3) {
         if (param0 == 0) {
            param0 = param3;
         }

         if (param1 == 0) {
            param1 = param3;
         }

         if (param2 == 0) {
            param2 = param3;
         }

         return param0 + param1 + param2 + param3 >> 2 & 16711935;
      }

      private int blend(int param0, int param1, int param2, int param3, float param4, float param5, float param6, float param7) {
         int var9 = (int)((param0 >> 16 & 0xFF) * param4 + (param1 >> 16 & 0xFF) * param5 + (param2 >> 16 & 0xFF) * param6 + (param3 >> 16 & 0xFF) * param7)
            & 0xFF;
         int var10 = (int)((param0 & 0xFF) * param4 + (param1 & 0xFF) * param5 + (param2 & 0xFF) * param6 + (param3 & 0xFF) * param7) & 0xFF;
         return var9 << 16 | var10;
      }
   }

   enum AmbientVertexRemap {
      DOWN(0, 1, 2, 3),
      UP(2, 3, 0, 1),
      NORTH(3, 0, 1, 2),
      SOUTH(0, 1, 2, 3),
      WEST(3, 0, 1, 2),
      EAST(1, 2, 3, 0);

      final int vert0;
      final int vert1;
      final int vert2;
      final int vert3;
      private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], param0 -> {
         param0[Direction.DOWN.get3DDataValue()] = DOWN;
         param0[Direction.UP.get3DDataValue()] = UP;
         param0[Direction.NORTH.get3DDataValue()] = NORTH;
         param0[Direction.SOUTH.get3DDataValue()] = SOUTH;
         param0[Direction.WEST.get3DDataValue()] = WEST;
         param0[Direction.EAST.get3DDataValue()] = EAST;
      });

      AmbientVertexRemap(int param2, int param3, int param4, int param5) {
         this.vert0 = param2;
         this.vert1 = param3;
         this.vert2 = param4;
         this.vert3 = param5;
      }

      public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction param0) {
         return BY_FACING[param0.get3DDataValue()];
      }
   }

   static class Cache {
      private boolean enabled;
      private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
         Long2IntLinkedOpenHashMap l_var1 = new Long2IntLinkedOpenHashMap(100, 0.25F) {
            @Override
            protected void rehash(int l_param0) {
            }
         };
         l_var1.defaultReturnValue(Integer.MAX_VALUE);
         return l_var1;
      });
      private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
         Long2FloatLinkedOpenHashMap l_var1 = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
            @Override
            protected void rehash(int l_param0) {
            }
         };
         l_var1.defaultReturnValue(Float.NaN);
         return l_var1;
      });

      private Cache() {
      }

      public void enable() {
         this.enabled = true;
      }

      public void disable() {
         this.enabled = false;
         this.colorCache.clear();
         this.brightnessCache.clear();
      }

      public int getLightColor(BlockState param0, BlockAndTintGetter param1, BlockPos param2) {
         long var4 = param2.asLong();
         if (this.enabled) {
            int var6 = this.colorCache.get(var4);
            if (var6 != Integer.MAX_VALUE) {
               return var6;
            }
         }

         int var6 = LevelRenderer.getLightColor(param1, param0, param2);
         if (this.enabled) {
            if (this.colorCache.size() == 100) {
               this.colorCache.removeFirstInt();
            }

            this.colorCache.put(var4, var6);
         }

         return var6;
      }

      public float getShadeBrightness(BlockState param0, BlockAndTintGetter param1, BlockPos param2) {
         long var4 = param2.asLong();
         if (this.enabled) {
            float var6 = this.brightnessCache.get(var4);
            if (!Float.isNaN(var6)) {
               return var6;
            }
         }

         float var6 = param0.getShadeBrightness(param1, param2);
         if (this.enabled) {
            if (this.brightnessCache.size() == 100) {
               this.brightnessCache.removeFirstFloat();
            }

            this.brightnessCache.put(var4, var6);
         }

         return var6;
      }
   }

   protected enum SizeInfo {
      DOWN(Direction.DOWN, false),
      UP(Direction.UP, false),
      NORTH(Direction.NORTH, false),
      SOUTH(Direction.SOUTH, false),
      WEST(Direction.WEST, false),
      EAST(Direction.EAST, false),
      FLIP_DOWN(Direction.DOWN, true),
      FLIP_UP(Direction.UP, true),
      FLIP_NORTH(Direction.NORTH, true),
      FLIP_SOUTH(Direction.SOUTH, true),
      FLIP_WEST(Direction.WEST, true),
      FLIP_EAST(Direction.EAST, true);

      final int shape;

      SizeInfo(Direction param2, boolean param3) {
         this.shape = param2.get3DDataValue() + (param3 ? ModelBlockRenderer.DIRECTIONS.length : 0);
      }
   }
}
