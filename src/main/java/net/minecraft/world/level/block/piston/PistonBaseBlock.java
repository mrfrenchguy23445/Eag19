package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonBaseBlock extends DirectionalBlock {
   public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
   public static final int TRIGGER_EXTEND = 0;
   public static final int TRIGGER_CONTRACT = 1;
   public static final int TRIGGER_DROP = 2;
   public static final float PLATFORM_THICKNESS = 4.0F;
   protected static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   protected static final VoxelShape WEST_AABB = Block.box(4.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 12.0);
   protected static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape UP_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
   protected static final VoxelShape DOWN_AABB = Block.box(0.0, 4.0, 0.0, 16.0, 16.0, 16.0);
   private final boolean isSticky;

   public PistonBaseBlock(boolean param0, BlockBehaviour.Properties param1) {
      super(param1);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXTENDED, false));
      this.isSticky = param0;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      if (param0.getValue(EXTENDED)) {
         switch ((Direction)param0.getValue(FACING)) {
            case DOWN:
               return DOWN_AABB;
            case UP:
            default:
               return UP_AABB;
            case NORTH:
               return NORTH_AABB;
            case SOUTH:
               return SOUTH_AABB;
            case WEST:
               return WEST_AABB;
            case EAST:
               return EAST_AABB;
         }
      } else {
         return Shapes.block();
      }
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, LivingEntity param3, ItemStack param4) {
      if (!param0.isClientSide) {
         this.checkIfExtend(param0, param1, param2);
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (!param1.isClientSide) {
         this.checkIfExtend(param1, param2, param0);
      }
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param3.is(param0.getBlock())) {
         if (!param1.isClientSide && param1.getBlockEntity(param2) == null) {
            this.checkIfExtend(param1, param2, param0);
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.defaultBlockState().setValue(FACING, param0.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, false);
   }

   private void checkIfExtend(Level param0, BlockPos param1, BlockState param2) {
      Direction var4 = param2.getValue(FACING);
      boolean var5 = this.getNeighborSignal(param0, param1, var4);
      if (var5 && !param2.getValue(EXTENDED)) {
         if (new PistonStructureResolver(param0, param1, var4, true).resolve()) {
            param0.blockEvent(param1, this, 0, var4.get3DDataValue());
         }
      } else if (!var5 && param2.getValue(EXTENDED)) {
         BlockPos var6 = param1.relative(var4, 2);
         BlockState var7 = param0.getBlockState(var6);
         int var8 = 1;
         if (var7.is(Blocks.MOVING_PISTON)
            && var7.getValue(FACING) == var4
            && param0.getBlockEntity(var6) instanceof PistonMovingBlockEntity var10
            && var10.isExtending()
            && (var10.getProgress(0.0F) < 0.5F || param0.getGameTime() == var10.getLastTicked() || ((ServerLevel)param0).isHandlingTick())) {
            var8 = 2;
         }

         param0.blockEvent(param1, this, var8, var4.get3DDataValue());
      }
   }

   private boolean getNeighborSignal(Level param0, BlockPos param1, Direction param2) {
      for (Direction var7 : Direction.values()) {
         if (var7 != param2 && param0.hasSignal(param1.relative(var7), var7)) {
            return true;
         }
      }

      if (param0.hasSignal(param1, Direction.DOWN)) {
         return true;
      }

      BlockPos var4 = param1.above();

      for (Direction var8 : Direction.values()) {
         if (var8 != Direction.DOWN && param0.hasSignal(var4.relative(var8), var8)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean triggerEvent(BlockState param0, Level param1, BlockPos param2, int param3, int param4) {
      Direction var6 = param0.getValue(FACING);
      if (!param1.isClientSide) {
         boolean var7 = this.getNeighborSignal(param1, param2, var6);
         if (var7 && (param3 == 1 || param3 == 2)) {
            param1.setBlock(param2, param0.setValue(EXTENDED, true), 2);
            return false;
         }

         if (!var7 && param3 == 0) {
            return false;
         }
      }

      if (param3 == 0) {
         if (!this.moveBlocks(param1, param2, var6, true)) {
            return false;
         }

         param1.setBlock(param2, param0.setValue(EXTENDED, true), 67);
         param1.playSound((Player)null, param2, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, param1.random.nextFloat() * 0.25F + 0.6F);
         param1.gameEvent((Entity)null, GameEvent.PISTON_EXTEND, param2);
      } else if (param3 == 1 || param3 == 2) {
         BlockEntity var7 = param1.getBlockEntity(param2.relative(var6));
         if (var7 instanceof PistonMovingBlockEntity) {
            ((PistonMovingBlockEntity)var7).finalTick();
         }

         BlockState var8 = Blocks.MOVING_PISTON
            .defaultBlockState()
            .setValue(MovingPistonBlock.FACING, var6)
            .setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
         param1.setBlock(param2, var8, 20);
         param1.setBlockEntity(
            MovingPistonBlock.newMovingBlockEntity(
               param2, var8, this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(param4 & 7)), var6, false, true
            )
         );
         param1.blockUpdated(param2, var8.getBlock());
         var8.updateNeighbourShapes(param1, param2, 2);
         if (this.isSticky) {
            BlockPos var9 = param2.offset(var6.getStepX() * 2, var6.getStepY() * 2, var6.getStepZ() * 2);
            BlockState var10 = param1.getBlockState(var9);
            boolean var11 = false;
            if (var10.is(Blocks.MOVING_PISTON)
               && param1.getBlockEntity(var9) instanceof PistonMovingBlockEntity var13
               && var13.getDirection() == var6
               && var13.isExtending()) {
               var13.finalTick();
               var11 = true;
            }

            if (!var11) {
               if (param3 != 1
                  || var10.isAir()
                  || !isPushable(var10, param1, var9, var6.getOpposite(), false, var6)
                  || var10.getPistonPushReaction() != PushReaction.NORMAL && !var10.is(Blocks.PISTON) && !var10.is(Blocks.STICKY_PISTON)) {
                  param1.removeBlock(param2.relative(var6), false);
               } else {
                  this.moveBlocks(param1, param2, var6, false);
               }
            }
         } else {
            param1.removeBlock(param2.relative(var6), false);
         }

         param1.playSound((Player)null, param2, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, param1.random.nextFloat() * 0.15F + 0.6F);
         param1.gameEvent((Entity)null, GameEvent.PISTON_CONTRACT, param2);
      }

      return true;
   }

   public static boolean isPushable(BlockState param0, Level param1, BlockPos param2, Direction param3, boolean param4, Direction param5) {
      if (param2.getY() < param1.getMinBuildHeight() || param2.getY() > param1.getMaxBuildHeight() - 1 || !param1.getWorldBorder().isWithinBounds(param2)) {
         return false;
      }

      if (param0.isAir()) {
         return true;
      }

      if (param0.is(Blocks.OBSIDIAN) || param0.is(Blocks.CRYING_OBSIDIAN) || param0.is(Blocks.RESPAWN_ANCHOR) || param0.is(Blocks.REINFORCED_DEEPSLATE)) {
         return false;
      }

      if (param3 == Direction.DOWN && param2.getY() == param1.getMinBuildHeight()) {
         return false;
      }

      if (param3 == Direction.UP && param2.getY() == param1.getMaxBuildHeight() - 1) {
         return false;
      }

      if (!param0.is(Blocks.PISTON) && !param0.is(Blocks.STICKY_PISTON)) {
         if (param0.getDestroySpeed(param1, param2) == -1.0F) {
            return false;
         }

         switch (param0.getPistonPushReaction()) {
            case BLOCK:
               return false;
            case DESTROY:
               return param4;
            case PUSH_ONLY:
               return param3 == param5;
         }
      } else if (param0.getValue(EXTENDED)) {
         return false;
      }

      return !param0.hasBlockEntity();
   }

   private boolean moveBlocks(Level param0, BlockPos param1, Direction param2, boolean param3) {
      BlockPos var5 = param1.relative(param2);
      if (!param3 && param0.getBlockState(var5).is(Blocks.PISTON_HEAD)) {
         param0.setBlock(var5, Blocks.AIR.defaultBlockState(), 20);
      }

      PistonStructureResolver var6 = new PistonStructureResolver(param0, param1, param2, param3);
      if (!var6.resolve()) {
         return false;
      }

      Map<BlockPos, BlockState> var7 = Maps.newHashMap();
      List<BlockPos> var8 = var6.getToPush();
      List<BlockState> var9 = Lists.newArrayList();

      for (int var10 = 0; var10 < var8.size(); var10++) {
         BlockPos var11 = var8.get(var10);
         BlockState var12 = param0.getBlockState(var11);
         var9.add(var12);
         var7.put(var11, var12);
      }

      List<BlockPos> var10 = var6.getToDestroy();
      BlockState[] var11 = new BlockState[var8.size() + var10.size()];
      Direction var12 = param3 ? param2 : param2.getOpposite();
      int var13 = 0;

      for (int var14 = var10.size() - 1; var14 >= 0; var14--) {
         BlockPos var15 = var10.get(var14);
         BlockState var16 = param0.getBlockState(var15);
         BlockEntity var17 = var16.hasBlockEntity() ? param0.getBlockEntity(var15) : null;
         dropResources(var16, param0, var15, var17);
         param0.setBlock(var15, Blocks.AIR.defaultBlockState(), 18);
         param0.gameEvent(GameEvent.BLOCK_DESTROY, var15, GameEvent.Context.of(var16));
         if (!var16.is(BlockTags.FIRE)) {
            param0.addDestroyBlockEffect(var15, var16);
         }

         var11[var13++] = var16;
      }

      for (int var14 = var8.size() - 1; var14 >= 0; var14--) {
         BlockPos var15 = var8.get(var14);
         BlockState var16 = param0.getBlockState(var15);
         var15 = var15.relative(var12);
         var7.remove(var15);
         BlockState var17 = Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, param2);
         param0.setBlock(var15, var17, 68);
         param0.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(var15, var17, var9.get(var14), param2, param3, false));
         var11[var13++] = var16;
      }

      if (param3) {
         PistonType var14 = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
         BlockState var15 = Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, param2).setValue(PistonHeadBlock.TYPE, var14);
         BlockState var16 = Blocks.MOVING_PISTON
            .defaultBlockState()
            .setValue(MovingPistonBlock.FACING, param2)
            .setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
         var7.remove(var5);
         param0.setBlock(var5, var16, 68);
         param0.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(var5, var16, var15, param2, true, true));
      }

      BlockState var14 = Blocks.AIR.defaultBlockState();

      for (BlockPos var16 : var7.keySet()) {
         param0.setBlock(var16, var14, 82);
      }

      for (Entry<BlockPos, BlockState> var16 : var7.entrySet()) {
         BlockPos var17 = var16.getKey();
         BlockState var18 = var16.getValue();
         var18.updateIndirectNeighbourShapes(param0, var17, 2);
         var14.updateNeighbourShapes(param0, var17, 2);
         var14.updateIndirectNeighbourShapes(param0, var17, 2);
      }

      var13 = 0;

      for (int var15 = var10.size() - 1; var15 >= 0; var15--) {
         BlockState var16 = var11[var13++];
         BlockPos var17 = var10.get(var15);
         var16.updateIndirectNeighbourShapes(param0, var17, 2);
         param0.updateNeighborsAt(var17, var16.getBlock());
      }

      for (int var15 = var8.size() - 1; var15 >= 0; var15--) {
         param0.updateNeighborsAt(var8.get(var15), var11[var13++].getBlock());
      }

      if (param3) {
         param0.updateNeighborsAt(var5, Blocks.PISTON_HEAD);
      }

      return true;
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      return param0.setValue(FACING, param1.rotate(param0.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      return param0.rotate(param1.getRotation(param0.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, EXTENDED);
   }

   @Override
   public boolean useShapeForLightOcclusion(BlockState param0) {
      return param0.getValue(EXTENDED);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
