package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public class RailState {
   private final Level level;
   private final BlockPos pos;
   private final BaseRailBlock block;
   private BlockState state;
   private final boolean isStraight;
   private final List<BlockPos> connections = Lists.newArrayList();

   public RailState(Level param0, BlockPos param1, BlockState param2) {
      this.level = param0;
      this.pos = param1;
      this.state = param2;
      this.block = (BaseRailBlock)param2.getBlock();
      RailShape var4 = param2.getValue(this.block.getShapeProperty());
      this.isStraight = this.block.isStraight();
      this.updateConnections(var4);
   }

   public List<BlockPos> getConnections() {
      return this.connections;
   }

   private void updateConnections(RailShape param0) {
      this.connections.clear();
      switch (param0) {
         case NORTH_SOUTH:
            this.connections.add(this.pos.north());
            this.connections.add(this.pos.south());
            break;
         case EAST_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.east());
            break;
         case ASCENDING_EAST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.east().above());
            break;
         case ASCENDING_WEST:
            this.connections.add(this.pos.west().above());
            this.connections.add(this.pos.east());
            break;
         case ASCENDING_NORTH:
            this.connections.add(this.pos.north().above());
            this.connections.add(this.pos.south());
            break;
         case ASCENDING_SOUTH:
            this.connections.add(this.pos.north());
            this.connections.add(this.pos.south().above());
            break;
         case SOUTH_EAST:
            this.connections.add(this.pos.east());
            this.connections.add(this.pos.south());
            break;
         case SOUTH_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.south());
            break;
         case NORTH_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.north());
            break;
         case NORTH_EAST:
            this.connections.add(this.pos.east());
            this.connections.add(this.pos.north());
      }
   }

   private void removeSoftConnections() {
      for (int var1 = 0; var1 < this.connections.size(); var1++) {
         RailState var2 = this.getRail(this.connections.get(var1));
         if (var2 != null && var2.connectsTo(this)) {
            this.connections.set(var1, var2.pos);
         } else {
            this.connections.remove(var1--);
         }
      }
   }

   private boolean hasRail(BlockPos param0) {
      return BaseRailBlock.isRail(this.level, param0) || BaseRailBlock.isRail(this.level, param0.above()) || BaseRailBlock.isRail(this.level, param0.below());
   }

   @Nullable
   private RailState getRail(BlockPos param0) {
      BlockPos var2 = param0;
      BlockState var3 = this.level.getBlockState(var2);
      if (BaseRailBlock.isRail(var3)) {
         return new RailState(this.level, var2, var3);
      }

      var2 = param0.above();
      var3 = this.level.getBlockState(var2);
      if (BaseRailBlock.isRail(var3)) {
         return new RailState(this.level, var2, var3);
      }

      var2 = param0.below();
      var3 = this.level.getBlockState(var2);
      return BaseRailBlock.isRail(var3) ? new RailState(this.level, var2, var3) : null;
   }

   private boolean connectsTo(RailState param0) {
      return this.hasConnection(param0.pos);
   }

   private boolean hasConnection(BlockPos param0) {
      for (int var2 = 0; var2 < this.connections.size(); var2++) {
         BlockPos var3 = this.connections.get(var2);
         if (var3.getX() == param0.getX() && var3.getZ() == param0.getZ()) {
            return true;
         }
      }

      return false;
   }

   protected int countPotentialConnections() {
      int var1 = 0;

      for (Direction var3 : Direction.Plane.HORIZONTAL) {
         if (this.hasRail(this.pos.relative(var3))) {
            var1++;
         }
      }

      return var1;
   }

   private boolean canConnectTo(RailState param0) {
      return this.connectsTo(param0) || this.connections.size() != 2;
   }

   private void connectTo(RailState param0) {
      this.connections.add(param0.pos);
      BlockPos var2 = this.pos.north();
      BlockPos var3 = this.pos.south();
      BlockPos var4 = this.pos.west();
      BlockPos var5 = this.pos.east();
      boolean var6 = this.hasConnection(var2);
      boolean var7 = this.hasConnection(var3);
      boolean var8 = this.hasConnection(var4);
      boolean var9 = this.hasConnection(var5);
      RailShape var10 = null;
      if (var6 || var7) {
         var10 = RailShape.NORTH_SOUTH;
      }

      if (var8 || var9) {
         var10 = RailShape.EAST_WEST;
      }

      if (!this.isStraight) {
         if (var7 && var9 && !var6 && !var8) {
            var10 = RailShape.SOUTH_EAST;
         }

         if (var7 && var8 && !var6 && !var9) {
            var10 = RailShape.SOUTH_WEST;
         }

         if (var6 && var8 && !var7 && !var9) {
            var10 = RailShape.NORTH_WEST;
         }

         if (var6 && var9 && !var7 && !var8) {
            var10 = RailShape.NORTH_EAST;
         }
      }

      if (var10 == RailShape.NORTH_SOUTH) {
         if (BaseRailBlock.isRail(this.level, var2.above())) {
            var10 = RailShape.ASCENDING_NORTH;
         }

         if (BaseRailBlock.isRail(this.level, var3.above())) {
            var10 = RailShape.ASCENDING_SOUTH;
         }
      }

      if (var10 == RailShape.EAST_WEST) {
         if (BaseRailBlock.isRail(this.level, var5.above())) {
            var10 = RailShape.ASCENDING_EAST;
         }

         if (BaseRailBlock.isRail(this.level, var4.above())) {
            var10 = RailShape.ASCENDING_WEST;
         }
      }

      if (var10 == null) {
         var10 = RailShape.NORTH_SOUTH;
      }

      this.state = this.state.setValue(this.block.getShapeProperty(), var10);
      this.level.setBlock(this.pos, this.state, 3);
   }

   private boolean hasNeighborRail(BlockPos param0) {
      RailState var2 = this.getRail(param0);
      if (var2 == null) {
         return false;
      }

      var2.removeSoftConnections();
      return var2.canConnectTo(this);
   }

   public RailState place(boolean param0, boolean param1, RailShape param2) {
      BlockPos var4 = this.pos.north();
      BlockPos var5 = this.pos.south();
      BlockPos var6 = this.pos.west();
      BlockPos var7 = this.pos.east();
      boolean var8 = this.hasNeighborRail(var4);
      boolean var9 = this.hasNeighborRail(var5);
      boolean var10 = this.hasNeighborRail(var6);
      boolean var11 = this.hasNeighborRail(var7);
      RailShape var12 = null;
      boolean var13 = var8 || var9;
      boolean var14 = var10 || var11;
      if (var13 && !var14) {
         var12 = RailShape.NORTH_SOUTH;
      }

      if (var14 && !var13) {
         var12 = RailShape.EAST_WEST;
      }

      boolean var15 = var9 && var11;
      boolean var16 = var9 && var10;
      boolean var17 = var8 && var11;
      boolean var18 = var8 && var10;
      if (!this.isStraight) {
         if (var15 && !var8 && !var10) {
            var12 = RailShape.SOUTH_EAST;
         }

         if (var16 && !var8 && !var11) {
            var12 = RailShape.SOUTH_WEST;
         }

         if (var18 && !var9 && !var11) {
            var12 = RailShape.NORTH_WEST;
         }

         if (var17 && !var9 && !var10) {
            var12 = RailShape.NORTH_EAST;
         }
      }

      if (var12 == null) {
         if (var13 && var14) {
            var12 = param2;
         } else if (var13) {
            var12 = RailShape.NORTH_SOUTH;
         } else if (var14) {
            var12 = RailShape.EAST_WEST;
         }

         if (!this.isStraight) {
            if (param0) {
               if (var15) {
                  var12 = RailShape.SOUTH_EAST;
               }

               if (var16) {
                  var12 = RailShape.SOUTH_WEST;
               }

               if (var17) {
                  var12 = RailShape.NORTH_EAST;
               }

               if (var18) {
                  var12 = RailShape.NORTH_WEST;
               }
            } else {
               if (var18) {
                  var12 = RailShape.NORTH_WEST;
               }

               if (var17) {
                  var12 = RailShape.NORTH_EAST;
               }

               if (var16) {
                  var12 = RailShape.SOUTH_WEST;
               }

               if (var15) {
                  var12 = RailShape.SOUTH_EAST;
               }
            }
         }
      }

      if (var12 == RailShape.NORTH_SOUTH) {
         if (BaseRailBlock.isRail(this.level, var4.above())) {
            var12 = RailShape.ASCENDING_NORTH;
         }

         if (BaseRailBlock.isRail(this.level, var5.above())) {
            var12 = RailShape.ASCENDING_SOUTH;
         }
      }

      if (var12 == RailShape.EAST_WEST) {
         if (BaseRailBlock.isRail(this.level, var7.above())) {
            var12 = RailShape.ASCENDING_EAST;
         }

         if (BaseRailBlock.isRail(this.level, var6.above())) {
            var12 = RailShape.ASCENDING_WEST;
         }
      }

      if (var12 == null) {
         var12 = param2;
      }

      this.updateConnections(var12);
      this.state = this.state.setValue(this.block.getShapeProperty(), var12);
      if (param1 || this.level.getBlockState(this.pos) != this.state) {
         this.level.setBlock(this.pos, this.state, 3);

         for (int var19 = 0; var19 < this.connections.size(); var19++) {
            RailState var20 = this.getRail(this.connections.get(var19));
            if (var20 != null) {
               var20.removeSoftConnections();
               if (var20.canConnectTo(this)) {
                  var20.connectTo(this);
               }
            }
         }
      }

      return this;
   }

   public BlockState getState() {
      return this.state;
   }
}
