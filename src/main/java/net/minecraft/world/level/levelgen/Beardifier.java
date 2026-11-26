package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Beardifier implements DensityFunctions.BeardifierOrMarker {
   public static final int BEARD_KERNEL_RADIUS = 12;
   private static final int BEARD_KERNEL_SIZE = 24;
   private static final float[] BEARD_KERNEL = Util.make(new float[13824], param0 -> {
      for (int var1 = 0; var1 < 24; var1++) {
         for (int var2 = 0; var2 < 24; var2++) {
            for (int var3 = 0; var3 < 24; var3++) {
               param0[var1 * 24 * 24 + var2 * 24 + var3] = (float)computeBeardContribution(var2 - 12, var3 - 12, var1 - 12);
            }
         }
      }
   });
   private final ObjectListIterator<Beardifier.Rigid> pieceIterator;
   private final ObjectListIterator<JigsawJunction> junctionIterator;

   public static Beardifier forStructuresInChunk(StructureManager param0, ChunkPos param1) {
      int var2 = param1.getMinBlockX();
      int var3 = param1.getMinBlockZ();
      ObjectList<Beardifier.Rigid> var4 = new ObjectArrayList<>(10);
      ObjectList<JigsawJunction> var5 = new ObjectArrayList<>(32);
      param0.startsForStructure(param1, l_param0 -> l_param0.terrainAdaptation() != TerrainAdjustment.NONE).forEach(l_param5 -> {
         TerrainAdjustment l_var6 = l_param5.getStructure().terrainAdaptation();

         for (StructurePiece l_var8 : l_param5.getPieces()) {
            if (l_var8.isCloseToChunk(param1, 12)) {
               if (l_var8 instanceof PoolElementStructurePiece l_var9) {
                  StructureTemplatePool.Projection l_var10 = l_var9.getElement().getProjection();
                  if (l_var10 == StructureTemplatePool.Projection.RIGID) {
                     var4.add(new Beardifier.Rigid(l_var9.getBoundingBox(), l_var6, l_var9.getGroundLevelDelta()));
                  }

                  for (JigsawJunction l_var12 : l_var9.getJunctions()) {
                     int l_var13 = l_var12.getSourceX();
                     int l_var14 = l_var12.getSourceZ();
                     if (l_var13 > var2 - 12 && l_var14 > var3 - 12 && l_var13 < var2 + 15 + 12 && l_var14 < var3 + 15 + 12) {
                        var5.add(l_var12);
                     }
                  }
               } else {
                  var4.add(new Beardifier.Rigid(l_var8.getBoundingBox(), l_var6, 0));
               }
            }
         }
      });
      return new Beardifier(var4.iterator(), var5.iterator());
   }

   @VisibleForTesting
   public Beardifier(ObjectListIterator<Beardifier.Rigid> param0, ObjectListIterator<JigsawJunction> param1) {
      this.pieceIterator = param0;
      this.junctionIterator = param1;
   }

   @Override
   public double compute(DensityFunction.FunctionContext param0) {
      int var2 = param0.blockX();
      int var3 = param0.blockY();
      int var4 = param0.blockZ();
      double var5 = 0.0;

      while (this.pieceIterator.hasNext()) {
         Beardifier.Rigid var7 = this.pieceIterator.next();
         BoundingBox var8 = var7.box();
         int var9 = var7.groundLevelDelta();
         int var10 = Math.max(0, Math.max(var8.minX() - var2, var2 - var8.maxX()));
         int var11 = Math.max(0, Math.max(var8.minZ() - var4, var4 - var8.maxZ()));
         int var12 = var8.minY() + var9;
         int var13 = var3 - var12;

         int var14 = switch (var7.terrainAdjustment()) {
            case NONE -> 0;
            case BURY, BEARD_THIN -> var13;
            case BEARD_BOX -> Math.max(0, Math.max(var12 - var3, var3 - var8.maxY()));
         };

         var5 += switch (var7.terrainAdjustment()) {
            case NONE -> 0.0;
            case BURY -> getBuryContribution(var10, var14, var11);
            case BEARD_THIN, BEARD_BOX -> getBeardContribution(var10, var14, var11, var13) * 0.8;
         };
      }

      this.pieceIterator.back(Integer.MAX_VALUE);

      while (this.junctionIterator.hasNext()) {
         JigsawJunction var7 = this.junctionIterator.next();
         int var8 = var2 - var7.getSourceX();
         int var9 = var3 - var7.getSourceGroundY();
         int var10 = var4 - var7.getSourceZ();
         var5 += getBeardContribution(var8, var9, var10, var9) * 0.4;
      }

      this.junctionIterator.back(Integer.MAX_VALUE);
      return var5;
   }

   @Override
   public double minValue() {
      return Double.NEGATIVE_INFINITY;
   }

   @Override
   public double maxValue() {
      return Double.POSITIVE_INFINITY;
   }

   private static double getBuryContribution(int param0, int param1, int param2) {
      double var3 = Mth.length(param0, param1 / 2.0, param2);
      return Mth.clampedMap(var3, 0.0, 6.0, 1.0, 0.0);
   }

   private static double getBeardContribution(int param0, int param1, int param2, int param3) {
      int var4 = param0 + 12;
      int var5 = param1 + 12;
      int var6 = param2 + 12;
      if (isInKernelRange(var4) && isInKernelRange(var5) && isInKernelRange(var6)) {
         double var7 = param3 + 0.5;
         double var9 = Mth.lengthSquared(param0, var7, param2);
         double var11 = -var7 * Mth.fastInvSqrt(var9 / 2.0) / 2.0;
         return var11 * BEARD_KERNEL[var6 * 24 * 24 + var4 * 24 + var5];
      } else {
         return 0.0;
      }
   }

   private static boolean isInKernelRange(int param0) {
      return param0 >= 0 && param0 < 24;
   }

   private static double computeBeardContribution(int param0, int param1, int param2) {
      return computeBeardContribution(param0, param1 + 0.5, param2);
   }

   private static double computeBeardContribution(int param0, double param1, int param3) {
      double var4 = Mth.lengthSquared(param0, param1, param3);
      return Math.pow(Math.E, -var4 / 16.0);
   }

   @VisibleForTesting
   public record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
   }
}
