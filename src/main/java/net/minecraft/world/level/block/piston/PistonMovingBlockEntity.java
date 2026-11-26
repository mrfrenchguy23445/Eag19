package net.minecraft.world.level.block.piston;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonMovingBlockEntity extends BlockEntity {
   private static final int TICKS_TO_EXTEND = 2;
   private static final double PUSH_OFFSET = 0.01;
   public static final double TICK_MOVEMENT = 0.51;
   private BlockState movedState = Blocks.AIR.defaultBlockState();
   private Direction direction;
   private boolean extending;
   private boolean isSourcePiston;
   private static final ThreadLocal<Direction> NOCLIP = ThreadLocal.withInitial(() -> null);
   private float progress;
   private float progressO;
   private long lastTicked;
   private int deathTicks;

   public PistonMovingBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.PISTON, param0, param1);
   }

   public PistonMovingBlockEntity(BlockPos param0, BlockState param1, BlockState param2, Direction param3, boolean param4, boolean param5) {
      this(param0, param1);
      this.movedState = param2;
      this.direction = param3;
      this.extending = param4;
      this.isSourcePiston = param5;
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public boolean isExtending() {
      return this.extending;
   }

   public Direction getDirection() {
      return this.direction;
   }

   public boolean isSourcePiston() {
      return this.isSourcePiston;
   }

   public float getProgress(float param0) {
      if (param0 > 1.0F) {
         param0 = 1.0F;
      }

      return Mth.lerp(param0, this.progressO, this.progress);
   }

   public float getXOff(float param0) {
      return this.direction.getStepX() * this.getExtendedProgress(this.getProgress(param0));
   }

   public float getYOff(float param0) {
      return this.direction.getStepY() * this.getExtendedProgress(this.getProgress(param0));
   }

   public float getZOff(float param0) {
      return this.direction.getStepZ() * this.getExtendedProgress(this.getProgress(param0));
   }

   private float getExtendedProgress(float param0) {
      return this.extending ? param0 - 1.0F : 1.0F - param0;
   }

   private BlockState getCollisionRelatedBlockState() {
      return !this.isExtending() && this.isSourcePiston() && this.movedState.getBlock() instanceof PistonBaseBlock
         ? Blocks.PISTON_HEAD
            .defaultBlockState()
            .setValue(PistonHeadBlock.SHORT, this.progress > 0.25F)
            .setValue(PistonHeadBlock.TYPE, this.movedState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT)
            .setValue(PistonHeadBlock.FACING, this.movedState.getValue(PistonBaseBlock.FACING))
         : this.movedState;
   }

   private static void moveCollidedEntities(Level param0, BlockPos param1, float param2, PistonMovingBlockEntity param3) {
      Direction var4 = param3.getMovementDirection();
      double var5 = param2 - param3.progress;
      VoxelShape var7 = param3.getCollisionRelatedBlockState().getCollisionShape(param0, param1);
      if (!var7.isEmpty()) {
         AABB var8 = moveByPositionAndProgress(param1, var7.bounds(), param3);
         List<Entity> var9 = param0.getEntities(null, PistonMath.getMovementArea(var8, var4, var5).minmax(var8));
         if (!var9.isEmpty()) {
            List<AABB> var10 = var7.toAabbs();
            boolean var11 = param3.movedState.is(Blocks.SLIME_BLOCK);
            Iterator var12 = var9.iterator();

            while (true) {
               Entity var13;
               while (true) {
                  if (!var12.hasNext()) {
                     return;
                  }

                  var13 = (Entity)var12.next();
                  if (var13.getPistonPushReaction() != PushReaction.IGNORE) {
                     if (!var11) {
                        break;
                     }

                     if (!(var13 instanceof ServerPlayer)) {
                        Vec3 var14 = var13.getDeltaMovement();
                        double var15 = var14.x;
                        double var17 = var14.y;
                        double var19 = var14.z;
                        switch (var4.getAxis()) {
                           case X:
                              var15 = var4.getStepX();
                              break;
                           case Y:
                              var17 = var4.getStepY();
                              break;
                           case Z:
                              var19 = var4.getStepZ();
                        }

                        var13.setDeltaMovement(var15, var17, var19);
                        break;
                     }
                  }
               }

               double var14 = 0.0;

               for (AABB var17 : var10) {
                  AABB var18 = PistonMath.getMovementArea(moveByPositionAndProgress(param1, var17, param3), var4, var5);
                  AABB var19 = var13.getBoundingBox();
                  if (var18.intersects(var19)) {
                     var14 = Math.max(var14, getMovement(var18, var4, var19));
                     if (var14 >= var5) {
                        break;
                     }
                  }
               }

               if (!(var14 <= 0.0)) {
                  var14 = Math.min(var14, var5) + 0.01;
                  moveEntityByPiston(var4, var13, var14, var4);
                  if (!param3.extending && param3.isSourcePiston) {
                     fixEntityWithinPistonBase(param1, var13, var4, var5);
                  }
               }
            }
         }
      }
   }

   private static void moveEntityByPiston(Direction param0, Entity param1, double param2, Direction param4) {
      NOCLIP.set(param0);
      param1.move(MoverType.PISTON, new Vec3(param2 * param4.getStepX(), param2 * param4.getStepY(), param2 * param4.getStepZ()));
      NOCLIP.set(null);
   }

   private static void moveStuckEntities(Level param0, BlockPos param1, float param2, PistonMovingBlockEntity param3) {
      if (param3.isStickyForEntities()) {
         Direction var4 = param3.getMovementDirection();
         if (var4.getAxis().isHorizontal()) {
            double var5 = param3.movedState.getCollisionShape(param0, param1).max(Direction.Axis.Y);
            AABB var7 = moveByPositionAndProgress(param1, new AABB(0.0, var5, 0.0, 1.0, 1.5000000999999998, 1.0), param3);
            double var8 = param2 - param3.progress;

            for (Entity var12 : param0.getEntities((Entity)null, var7, l_param1 -> matchesStickyCritera(var7, l_param1))) {
               moveEntityByPiston(var4, var12, var8, var4);
            }
         }
      }
   }

   private static boolean matchesStickyCritera(AABB param0, Entity param1) {
      return param1.getPistonPushReaction() == PushReaction.NORMAL
         && param1.isOnGround()
         && param1.getX() >= param0.minX
         && param1.getX() <= param0.maxX
         && param1.getZ() >= param0.minZ
         && param1.getZ() <= param0.maxZ;
   }

   private boolean isStickyForEntities() {
      return this.movedState.is(Blocks.HONEY_BLOCK);
   }

   public Direction getMovementDirection() {
      return this.extending ? this.direction : this.direction.getOpposite();
   }

   private static double getMovement(AABB param0, Direction param1, AABB param2) {
      switch (param1) {
         case EAST:
            return param0.maxX - param2.minX;
         case WEST:
            return param2.maxX - param0.minX;
         case UP:
         default:
            return param0.maxY - param2.minY;
         case DOWN:
            return param2.maxY - param0.minY;
         case SOUTH:
            return param0.maxZ - param2.minZ;
         case NORTH:
            return param2.maxZ - param0.minZ;
      }
   }

   private static AABB moveByPositionAndProgress(BlockPos param0, AABB param1, PistonMovingBlockEntity param2) {
      double var3 = param2.getExtendedProgress(param2.progress);
      return param1.move(
         param0.getX() + var3 * param2.direction.getStepX(),
         param0.getY() + var3 * param2.direction.getStepY(),
         param0.getZ() + var3 * param2.direction.getStepZ()
      );
   }

   private static void fixEntityWithinPistonBase(BlockPos param0, Entity param1, Direction param2, double param3) {
      AABB var5 = param1.getBoundingBox();
      AABB var6 = Shapes.block().bounds().move(param0);
      if (var5.intersects(var6)) {
         Direction var7 = param2.getOpposite();
         double var8 = getMovement(var6, var7, var5) + 0.01;
         double var10 = getMovement(var6, var7, var5.intersect(var6)) + 0.01;
         if (Math.abs(var8 - var10) < 0.01) {
            var8 = Math.min(var8, param3) + 0.01;
            moveEntityByPiston(param2, param1, var8, var7);
         }
      }
   }

   public BlockState getMovedState() {
      return this.movedState;
   }

   public void finalTick() {
      if (this.level != null && (this.progressO < 1.0F || this.level.isClientSide)) {
         this.progress = 1.0F;
         this.progressO = this.progress;
         this.level.removeBlockEntity(this.worldPosition);
         this.setRemoved();
         if (this.level.getBlockState(this.worldPosition).is(Blocks.MOVING_PISTON)) {
            BlockState var1;
            if (this.isSourcePiston) {
               var1 = Blocks.AIR.defaultBlockState();
            } else {
               var1 = Block.updateFromNeighbourShapes(this.movedState, this.level, this.worldPosition);
            }

            this.level.setBlock(this.worldPosition, var1, 3);
            this.level.neighborChanged(this.worldPosition, var1.getBlock(), this.worldPosition);
         }
      }
   }

   public static void tick(Level param0, BlockPos param1, BlockState param2, PistonMovingBlockEntity param3) {
      param3.lastTicked = param0.getGameTime();
      param3.progressO = param3.progress;
      if (param3.progressO >= 1.0F) {
         if (param0.isClientSide && param3.deathTicks < 5) {
            param3.deathTicks++;
         } else {
            param0.removeBlockEntity(param1);
            param3.setRemoved();
            if (param0.getBlockState(param1).is(Blocks.MOVING_PISTON)) {
               BlockState var4 = Block.updateFromNeighbourShapes(param3.movedState, param0, param1);
               if (var4.isAir()) {
                  param0.setBlock(param1, param3.movedState, 84);
                  Block.updateOrDestroy(param3.movedState, var4, param0, param1, 3);
               } else {
                  if (var4.hasProperty(BlockStateProperties.WATERLOGGED) && var4.getValue(BlockStateProperties.WATERLOGGED)) {
                     var4 = var4.setValue(BlockStateProperties.WATERLOGGED, false);
                  }

                  param0.setBlock(param1, var4, 67);
                  param0.neighborChanged(param1, var4.getBlock(), param1);
               }
            }
         }
      } else {
         float var4 = param3.progress + 0.5F;
         moveCollidedEntities(param0, param1, var4, param3);
         moveStuckEntities(param0, param1, var4, param3);
         param3.progress = var4;
         if (param3.progress >= 1.0F) {
            param3.progress = 1.0F;
         }
      }
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      HolderGetter<Block> var2 = this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup();
      this.movedState = NbtUtils.readBlockState(var2, param0.getCompound("blockState"));
      this.direction = Direction.from3DDataValue(param0.getInt("facing"));
      this.progress = param0.getFloat("progress");
      this.progressO = this.progress;
      this.extending = param0.getBoolean("extending");
      this.isSourcePiston = param0.getBoolean("source");
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.put("blockState", NbtUtils.writeBlockState(this.movedState));
      param0.putInt("facing", this.direction.get3DDataValue());
      param0.putFloat("progress", this.progressO);
      param0.putBoolean("extending", this.extending);
      param0.putBoolean("source", this.isSourcePiston);
   }

   public VoxelShape getCollisionShape(BlockGetter param0, BlockPos param1) {
      VoxelShape var3;
      if (!this.extending && this.isSourcePiston && this.movedState.getBlock() instanceof PistonBaseBlock) {
         var3 = this.movedState.setValue(PistonBaseBlock.EXTENDED, true).getCollisionShape(param0, param1);
      } else {
         var3 = Shapes.empty();
      }

      Direction var4 = NOCLIP.get();
      if (this.progress < 1.0 && var4 == this.getMovementDirection()) {
         return var3;
      }

      BlockState var5;
      if (this.isSourcePiston()) {
         var5 = Blocks.PISTON_HEAD
            .defaultBlockState()
            .setValue(PistonHeadBlock.FACING, this.direction)
            .setValue(PistonHeadBlock.SHORT, this.extending != 1.0F - this.progress < 0.25F);
      } else {
         var5 = this.movedState;
      }

      float var6 = this.getExtendedProgress(this.progress);
      double var7 = this.direction.getStepX() * var6;
      double var9 = this.direction.getStepY() * var6;
      double var11 = this.direction.getStepZ() * var6;
      return Shapes.or(var3, var5.getCollisionShape(param0, param1).move(var7, var9, var11));
   }

   public long getLastTicked() {
      return this.lastTicked;
   }

   @Override
   public void setLevel(Level param0) {
      super.setLevel(param0);
      if (param0.holderLookup(Registries.BLOCK).get(this.movedState.getBlock().builtInRegistryHolder().key()).isEmpty()) {
         this.movedState = Blocks.AIR.defaultBlockState();
      }
   }
}
