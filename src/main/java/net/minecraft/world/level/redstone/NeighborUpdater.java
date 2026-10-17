package net.minecraft.world.level.redstone;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface NeighborUpdater {
   Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

   void shapeUpdate(Direction var1, BlockState var2, BlockPos var3, BlockPos var4, int var5, int var6);

   void neighborChanged(BlockPos var1, Block var2, BlockPos var3);

   void neighborChanged(BlockState var1, BlockPos var2, Block var3, BlockPos var4, boolean var5);

   default void updateNeighborsAtExceptFromFacing(BlockPos param0, Block param1, @Nullable Direction param2) {
      for (Direction var7 : UPDATE_ORDER) {
         if (var7 != param2) {
            this.neighborChanged(param0.relative(var7), param1, param0);
         }
      }
   }

   static void executeShapeUpdate(LevelAccessor param0, Direction param1, BlockState param2, BlockPos param3, BlockPos param4, int param5, int param6) {
      BlockState var7 = param0.getBlockState(param3);
      BlockState var8 = var7.updateShape(param1, param2, param0, param3, param4);
      Block.updateOrDestroy(var7, var8, param0, param3, param5, param6);
   }

   static void executeUpdate(Level param0, BlockState param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      try {
         param1.neighborChanged(param0, param2, param3, param4, param5);
      } catch (Throwable var6) {
         CrashReport var7 = CrashReport.forThrowable(var6, "Exception while updating neighbours");
         CrashReportCategory var8 = var7.addCategory("Block being updated");
         var8.setDetail(
            "Source block type",
            () -> {
               try {
                  return String.format(
                     Locale.ROOT, "ID #%s (%s // %s)", BuiltInRegistries.BLOCK.getKey(param3), param3.getDescriptionId(), param3.getClass().getCanonicalName()
                  );
               } catch (Throwable l_var1) {
                  return "ID #" + BuiltInRegistries.BLOCK.getKey(param3);
               }
            }
         );
         CrashReportCategory.populateBlockDetails(var8, param0, param2, param1);
         throw new ReportedException(var7);
      }
   }
}
