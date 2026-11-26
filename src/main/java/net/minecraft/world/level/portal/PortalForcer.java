package net.minecraft.world.level.portal;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class PortalForcer {
   private static final int TICKET_RADIUS = 3;
   private static final int SEARCH_RADIUS = 128;
   private static final int CREATE_RADIUS = 16;
   private static final int FRAME_HEIGHT = 5;
   private static final int FRAME_WIDTH = 4;
   private static final int FRAME_BOX = 3;
   private static final int FRAME_HEIGHT_START = -1;
   private static final int FRAME_HEIGHT_END = 4;
   private static final int FRAME_WIDTH_START = -1;
   private static final int FRAME_WIDTH_END = 3;
   private static final int FRAME_BOX_START = -1;
   private static final int FRAME_BOX_END = 2;
   private static final int NOTHING_FOUND = -1;
   private final ServerLevel level;

   public PortalForcer(ServerLevel param0) {
      this.level = param0;
   }

   public Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos param0, boolean param1, WorldBorder param2) {
      PoiManager var4 = this.level.getPoiManager();
      int var5 = param1 ? 16 : 128;
      var4.ensureLoadedAndValid(this.level, param0, var5);
      Optional<PoiRecord> var6 = var4.getInSquare(l_param0 -> l_param0.is(PoiTypes.NETHER_PORTAL), param0, var5, PoiManager.Occupancy.ANY)
         .filter(l_param1 -> param2.isWithinBounds(l_param1.getPos()))
         .sorted(Comparator.<PoiRecord>comparingDouble(l_param1 -> l_param1.getPos().distSqr(param0)).thenComparingInt(l_param0 -> l_param0.getPos().getY()))
         .filter(l_param0 -> this.level.getBlockState(l_param0.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
         .findFirst();
      return var6.map(
         l_param0 -> {
            BlockPos l_var2 = l_param0.getPos();
            this.level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(l_var2), 3, l_var2);
            BlockState l_var3 = this.level.getBlockState(l_var2);
            return BlockUtil.getLargestRectangleAround(
               l_var2,
               l_var3.getValue(BlockStateProperties.HORIZONTAL_AXIS),
               21,
               Direction.Axis.Y,
               21,
               l2_param1 -> this.level.getBlockState(l2_param1) == l_var3
            );
         }
      );
   }

   public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos param0, Direction.Axis param1) {
      Direction var3 = Direction.get(Direction.AxisDirection.POSITIVE, param1);
      double var4 = -1.0;
      BlockPos var6 = null;
      double var7 = -1.0;
      BlockPos var9 = null;
      WorldBorder var10 = this.level.getWorldBorder();
      int var11 = Math.min(this.level.getMaxBuildHeight(), this.level.getMinBuildHeight() + this.level.getLogicalHeight()) - 1;
      BlockPos.MutableBlockPos var12 = param0.mutable();

      for (BlockPos.MutableBlockPos var14 : BlockPos.spiralAround(param0, 16, Direction.EAST, Direction.SOUTH)) {
         int var15 = Math.min(var11, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, var14.getX(), var14.getZ()));
         int var16 = 1;
         if (var10.isWithinBounds(var14) && var10.isWithinBounds(var14.move(var3, 1))) {
            var14.move(var3.getOpposite(), 1);

            for (int var17 = var15; var17 >= this.level.getMinBuildHeight(); var17--) {
               var14.setY(var17);
               if (this.canPortalReplaceBlock(var14)) {
                  int var18 = var17;

                  while (var17 > this.level.getMinBuildHeight() && this.canPortalReplaceBlock(var14.move(Direction.DOWN))) {
                     var17--;
                  }

                  if (var17 + 4 <= var11) {
                     int var19 = var18 - var17;
                     if (var19 <= 0 || var19 >= 3) {
                        var14.setY(var17);
                        if (this.canHostFrame(var14, var12, var3, 0)) {
                           double var20 = param0.distSqr(var14);
                           if (this.canHostFrame(var14, var12, var3, -1) && this.canHostFrame(var14, var12, var3, 1) && (var4 == -1.0 || var4 > var20)) {
                              var4 = var20;
                              var6 = var14.immutable();
                           }

                           if (var4 == -1.0 && (var7 == -1.0 || var7 > var20)) {
                              var7 = var20;
                              var9 = var14.immutable();
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (var4 == -1.0 && var7 != -1.0) {
         var6 = var9;
         var4 = var7;
      }

      if (var4 == -1.0) {
         int var13 = Math.max(this.level.getMinBuildHeight() - -1, 70);
         int var14 = var11 - 9;
         if (var14 < var13) {
            return Optional.empty();
         }

         var6 = new BlockPos(param0.getX(), Mth.clamp(param0.getY(), var13, var14), param0.getZ()).immutable();
         Direction var15 = var3.getClockWise();
         if (!var10.isWithinBounds(var6)) {
            return Optional.empty();
         }

         for (int var16 = -1; var16 < 2; var16++) {
            for (int var17 = 0; var17 < 2; var17++) {
               for (int var18 = -1; var18 < 3; var18++) {
                  BlockState var19 = var18 < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                  var12.setWithOffset(var6, var17 * var3.getStepX() + var16 * var15.getStepX(), var18, var17 * var3.getStepZ() + var16 * var15.getStepZ());
                  this.level.setBlockAndUpdate(var12, var19);
               }
            }
         }
      }

      for (int var13 = -1; var13 < 3; var13++) {
         for (int var14 = -1; var14 < 4; var14++) {
            if (var13 == -1 || var13 == 2 || var14 == -1 || var14 == 3) {
               var12.setWithOffset(var6, var13 * var3.getStepX(), var14, var13 * var3.getStepZ());
               this.level.setBlock(var12, Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
         }
      }

      BlockState var13 = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, param1);

      for (int var14 = 0; var14 < 2; var14++) {
         for (int var15 = 0; var15 < 3; var15++) {
            var12.setWithOffset(var6, var14 * var3.getStepX(), var15, var14 * var3.getStepZ());
            this.level.setBlock(var12, var13, 18);
         }
      }

      return Optional.of(new BlockUtil.FoundRectangle(var6.immutable(), 2, 3));
   }

   private boolean canPortalReplaceBlock(BlockPos.MutableBlockPos param0) {
      BlockState var2 = this.level.getBlockState(param0);
      return var2.canBeReplaced() && var2.getFluidState().isEmpty();
   }

   private boolean canHostFrame(BlockPos param0, BlockPos.MutableBlockPos param1, Direction param2, int param3) {
      Direction var5 = param2.getClockWise();

      for (int var6 = -1; var6 < 3; var6++) {
         for (int var7 = -1; var7 < 4; var7++) {
            param1.setWithOffset(param0, param2.getStepX() * var6 + var5.getStepX() * param3, var7, param2.getStepZ() * var6 + var5.getStepZ() * param3);
            if (var7 < 0 && !this.level.getBlockState(param1).getMaterial().isSolid()) {
               return false;
            }

            if (var7 >= 0 && !this.canPortalReplaceBlock(param1)) {
               return false;
            }
         }
      }

      return true;
   }
}
