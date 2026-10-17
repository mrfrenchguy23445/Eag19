package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedStoneWireBlock extends Block {
   public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
   public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
   public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
   public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(
      ImmutableMap.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST)
   );
   protected static final int H = 1;
   protected static final int W = 3;
   protected static final int E = 13;
   protected static final int N = 3;
   protected static final int S = 13;
   private static final VoxelShape SHAPE_DOT = Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
   private static final Map<Direction, VoxelShape> SHAPES_FLOOR = Maps.newEnumMap(
      ImmutableMap.of(
         Direction.NORTH,
         Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
         Direction.SOUTH,
         Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
         Direction.EAST,
         Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
         Direction.WEST,
         Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0)
      )
   );
   private static final Map<Direction, VoxelShape> SHAPES_UP = Maps.newEnumMap(
      ImmutableMap.of(
         Direction.NORTH,
         Shapes.or(SHAPES_FLOOR.get(Direction.NORTH), Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0)),
         Direction.SOUTH,
         Shapes.or(SHAPES_FLOOR.get(Direction.SOUTH), Block.box(3.0, 0.0, 15.0, 13.0, 16.0, 16.0)),
         Direction.EAST,
         Shapes.or(SHAPES_FLOOR.get(Direction.EAST), Block.box(15.0, 0.0, 3.0, 16.0, 16.0, 13.0)),
         Direction.WEST,
         Shapes.or(SHAPES_FLOOR.get(Direction.WEST), Block.box(0.0, 0.0, 3.0, 1.0, 16.0, 13.0))
      )
   );
   private static final Map<BlockState, VoxelShape> SHAPES_CACHE = Maps.newHashMap();
   private static final Vec3[] COLORS = Util.make(new Vec3[16], param0 -> {
      for (int var1 = 0; var1 <= 15; var1++) {
         float var2 = var1 / 15.0F;
         float var3 = var2 * 0.6F + (var2 > 0.0F ? 0.4F : 0.3F);
         float var4 = Mth.clamp(var2 * var2 * 0.7F - 0.5F, 0.0F, 1.0F);
         float var5 = Mth.clamp(var2 * var2 * 0.6F - 0.7F, 0.0F, 1.0F);
         param0[var1] = new Vec3(var3, var4, var5);
      }
   });
   private static final float PARTICLE_DENSITY = 0.2F;
   private final BlockState crossState;
   private boolean shouldSignal = true;

   public RedStoneWireBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, RedstoneSide.NONE)
            .setValue(EAST, RedstoneSide.NONE)
            .setValue(SOUTH, RedstoneSide.NONE)
            .setValue(WEST, RedstoneSide.NONE)
            .setValue(POWER, 0)
      );
      this.crossState = this.defaultBlockState()
         .setValue(NORTH, RedstoneSide.SIDE)
         .setValue(EAST, RedstoneSide.SIDE)
         .setValue(SOUTH, RedstoneSide.SIDE)
         .setValue(WEST, RedstoneSide.SIDE);

      for (BlockState var3 : this.getStateDefinition().getPossibleStates()) {
         if (var3.getValue(POWER) == 0) {
            SHAPES_CACHE.put(var3, this.calculateShape(var3));
         }
      }
   }

   private VoxelShape calculateShape(BlockState param0) {
      VoxelShape var2 = SHAPE_DOT;

      for (Direction var4 : Direction.Plane.HORIZONTAL) {
         RedstoneSide var5 = param0.getValue(PROPERTY_BY_DIRECTION.get(var4));
         if (var5 == RedstoneSide.SIDE) {
            var2 = Shapes.or(var2, SHAPES_FLOOR.get(var4));
         } else if (var5 == RedstoneSide.UP) {
            var2 = Shapes.or(var2, SHAPES_UP.get(var4));
         }
      }

      return var2;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPES_CACHE.get(param0.setValue(POWER, 0));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.getConnectionState(param0.getLevel(), this.crossState, param0.getClickedPos());
   }

   private BlockState getConnectionState(BlockGetter param0, BlockState param1, BlockPos param2) {
      boolean var4 = isDot(param1);
      param1 = this.getMissingConnections(param0, this.defaultBlockState().setValue(POWER, param1.getValue(POWER)), param2);
      if (var4 && isDot(param1)) {
         return param1;
      }

      boolean var5 = param1.getValue(NORTH).isConnected();
      boolean var6 = param1.getValue(SOUTH).isConnected();
      boolean var7 = param1.getValue(EAST).isConnected();
      boolean var8 = param1.getValue(WEST).isConnected();
      boolean var9 = !var5 && !var6;
      boolean var10 = !var7 && !var8;
      if (!var8 && var9) {
         param1 = param1.setValue(WEST, RedstoneSide.SIDE);
      }

      if (!var7 && var9) {
         param1 = param1.setValue(EAST, RedstoneSide.SIDE);
      }

      if (!var5 && var10) {
         param1 = param1.setValue(NORTH, RedstoneSide.SIDE);
      }

      if (!var6 && var10) {
         param1 = param1.setValue(SOUTH, RedstoneSide.SIDE);
      }

      return param1;
   }

   private BlockState getMissingConnections(BlockGetter param0, BlockState param1, BlockPos param2) {
      boolean var4 = !param0.getBlockState(param2.above()).isRedstoneConductor(param0, param2);

      for (Direction var6 : Direction.Plane.HORIZONTAL) {
         if (!param1.getValue(PROPERTY_BY_DIRECTION.get(var6)).isConnected()) {
            RedstoneSide var7 = this.getConnectingSide(param0, param2, var6, var4);
            param1 = param1.setValue(PROPERTY_BY_DIRECTION.get(var6), var7);
         }
      }

      return param1;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param1 == Direction.DOWN) {
         return param0;
      }

      if (param1 == Direction.UP) {
         return this.getConnectionState(param3, param0, param4);
      }

      RedstoneSide var7 = this.getConnectingSide(param3, param4, param1);
      return var7.isConnected() == param0.getValue(PROPERTY_BY_DIRECTION.get(param1)).isConnected() && !isCross(param0)
         ? param0.setValue(PROPERTY_BY_DIRECTION.get(param1), var7)
         : this.getConnectionState(param3, this.crossState.setValue(POWER, param0.getValue(POWER)).setValue(PROPERTY_BY_DIRECTION.get(param1), var7), param4);
   }

   private static boolean isCross(BlockState param0) {
      return param0.getValue(NORTH).isConnected()
         && param0.getValue(SOUTH).isConnected()
         && param0.getValue(EAST).isConnected()
         && param0.getValue(WEST).isConnected();
   }

   private static boolean isDot(BlockState param0) {
      return !param0.getValue(NORTH).isConnected()
         && !param0.getValue(SOUTH).isConnected()
         && !param0.getValue(EAST).isConnected()
         && !param0.getValue(WEST).isConnected();
   }

   @Override
   public void updateIndirectNeighbourShapes(BlockState param0, LevelAccessor param1, BlockPos param2, int param3, int param4) {
      BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

      for (Direction var8 : Direction.Plane.HORIZONTAL) {
         RedstoneSide var9 = param0.getValue(PROPERTY_BY_DIRECTION.get(var8));
         if (var9 != RedstoneSide.NONE && !param1.getBlockState(var6.setWithOffset(param2, var8)).is(this)) {
            var6.move(Direction.DOWN);
            BlockState var10 = param1.getBlockState(var6);
            if (var10.is(this)) {
               BlockPos var11 = var6.relative(var8.getOpposite());
               param1.neighborShapeChanged(var8.getOpposite(), param1.getBlockState(var11), var6, var11, param3, param4);
            }

            var6.setWithOffset(param2, var8).move(Direction.UP);
            BlockState var11 = param1.getBlockState(var6);
            if (var11.is(this)) {
               BlockPos var12 = var6.relative(var8.getOpposite());
               param1.neighborShapeChanged(var8.getOpposite(), param1.getBlockState(var12), var6, var12, param3, param4);
            }
         }
      }
   }

   private RedstoneSide getConnectingSide(BlockGetter param0, BlockPos param1, Direction param2) {
      return this.getConnectingSide(param0, param1, param2, !param0.getBlockState(param1.above()).isRedstoneConductor(param0, param1));
   }

   private RedstoneSide getConnectingSide(BlockGetter param0, BlockPos param1, Direction param2, boolean param3) {
      BlockPos var5 = param1.relative(param2);
      BlockState var6 = param0.getBlockState(var5);
      if (param3) {
         boolean var7 = this.canSurviveOn(param0, var5, var6);
         if (var7 && shouldConnectTo(param0.getBlockState(var5.above()))) {
            if (var6.isFaceSturdy(param0, var5, param2.getOpposite())) {
               return RedstoneSide.UP;
            }

            return RedstoneSide.SIDE;
         }
      }

      return !shouldConnectTo(var6, param2) && (var6.isRedstoneConductor(param0, var5) || !shouldConnectTo(param0.getBlockState(var5.below())))
         ? RedstoneSide.NONE
         : RedstoneSide.SIDE;
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockPos var4 = param2.below();
      BlockState var5 = param1.getBlockState(var4);
      return this.canSurviveOn(param1, var4, var5);
   }

   private boolean canSurviveOn(BlockGetter param0, BlockPos param1, BlockState param2) {
      return param2.isFaceSturdy(param0, param1, Direction.UP) || param2.is(Blocks.HOPPER);
   }

   private void updatePowerStrength(Level param0, BlockPos param1, BlockState param2) {
      int var4 = this.calculateTargetStrength(param0, param1);
      if (param2.getValue(POWER) != var4) {
         if (param0.getBlockState(param1) == param2) {
            param0.setBlock(param1, param2.setValue(POWER, var4), 2);
         }

         Set<BlockPos> var5 = Sets.newHashSet();
         var5.add(param1);

         for (Direction var9 : Direction.values()) {
            var5.add(param1.relative(var9));
         }

         for (BlockPos var7 : var5) {
            param0.updateNeighborsAt(var7, this);
         }
      }
   }

   private int calculateTargetStrength(Level param0, BlockPos param1) {
      this.shouldSignal = false;
      int var3 = param0.getBestNeighborSignal(param1);
      this.shouldSignal = true;
      int var4 = 0;
      if (var3 < 15) {
         for (Direction var6 : Direction.Plane.HORIZONTAL) {
            BlockPos var7 = param1.relative(var6);
            BlockState var8 = param0.getBlockState(var7);
            var4 = Math.max(var4, this.getWireSignal(var8));
            BlockPos var9 = param1.above();
            if (var8.isRedstoneConductor(param0, var7) && !param0.getBlockState(var9).isRedstoneConductor(param0, var9)) {
               var4 = Math.max(var4, this.getWireSignal(param0.getBlockState(var7.above())));
            } else if (!var8.isRedstoneConductor(param0, var7)) {
               var4 = Math.max(var4, this.getWireSignal(param0.getBlockState(var7.below())));
            }
         }
      }

      return Math.max(var3, var4 - 1);
   }

   private int getWireSignal(BlockState param0) {
      return param0.is(this) ? param0.getValue(POWER) : 0;
   }

   private void checkCornerChangeAt(Level param0, BlockPos param1) {
      if (param0.getBlockState(param1).is(this)) {
         param0.updateNeighborsAt(param1, this);

         for (Direction var6 : Direction.values()) {
            param0.updateNeighborsAt(param1.relative(var6), this);
         }
      }
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param3.is(param0.getBlock()) && !param1.isClientSide) {
         this.updatePowerStrength(param1, param2, param0);

         for (Direction var7 : Direction.Plane.VERTICAL) {
            param1.updateNeighborsAt(param2.relative(var7), this);
         }

         this.updateNeighborsOfNeighboringWires(param1, param2);
      }
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param4 && !param0.is(param3.getBlock())) {
         super.onRemove(param0, param1, param2, param3, param4);
         if (!param1.isClientSide) {
            for (Direction var9 : Direction.values()) {
               param1.updateNeighborsAt(param2.relative(var9), this);
            }

            this.updatePowerStrength(param1, param2, param0);
            this.updateNeighborsOfNeighboringWires(param1, param2);
         }
      }
   }

   private void updateNeighborsOfNeighboringWires(Level param0, BlockPos param1) {
      for (Direction var4 : Direction.Plane.HORIZONTAL) {
         this.checkCornerChangeAt(param0, param1.relative(var4));
      }

      for (Direction var4 : Direction.Plane.HORIZONTAL) {
         BlockPos var5 = param1.relative(var4);
         if (param0.getBlockState(var5).isRedstoneConductor(param0, var5)) {
            this.checkCornerChangeAt(param0, var5.above());
         } else {
            this.checkCornerChangeAt(param0, var5.below());
         }
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (!param1.isClientSide) {
         if (param0.canSurvive(param1, param2)) {
            this.updatePowerStrength(param1, param2, param0);
         } else {
            dropResources(param0, param1, param2);
            param1.removeBlock(param2, false);
         }
      }
   }

   @Override
   public int getDirectSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return !this.shouldSignal ? 0 : param0.getSignal(param1, param2, param3);
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      if (this.shouldSignal && param3 != Direction.DOWN) {
         int var5 = param0.getValue(POWER);
         if (var5 == 0) {
            return 0;
         } else {
            return param3 != Direction.UP
                  && !this.getConnectionState(param1, param0, param2).getValue(PROPERTY_BY_DIRECTION.get(param3.getOpposite())).isConnected()
               ? 0
               : var5;
         }
      } else {
         return 0;
      }
   }

   protected static boolean shouldConnectTo(BlockState param0) {
      return shouldConnectTo(param0, null);
   }

   protected static boolean shouldConnectTo(BlockState param0, @Nullable Direction param1) {
      if (param0.is(Blocks.REDSTONE_WIRE)) {
         return true;
      } else if (param0.is(Blocks.REPEATER)) {
         Direction var2 = param0.getValue(RepeaterBlock.FACING);
         return var2 == param1 || var2.getOpposite() == param1;
      } else {
         return param0.is(Blocks.OBSERVER) ? param1 == param0.getValue(ObserverBlock.FACING) : param0.isSignalSource() && param1 != null;
      }
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return this.shouldSignal;
   }

   public static int getColorForPower(int param0) {
      Vec3 var1 = COLORS[param0];
      return Mth.color((float)var1.x(), (float)var1.y(), (float)var1.z());
   }

   private void spawnParticlesAlongLine(
      Level param0, RandomSource param1, BlockPos param2, Vec3 param3, Direction param4, Direction param5, float param6, float param7
   ) {
      float var9 = param7 - param6;
      if (!(param1.nextFloat() >= 0.2F * var9)) {
         float var10 = 0.4375F;
         float var11 = param6 + var9 * param1.nextFloat();
         double var12 = 0.5 + 0.4375F * param4.getStepX() + var11 * param5.getStepX();
         double var14 = 0.5 + 0.4375F * param4.getStepY() + var11 * param5.getStepY();
         double var16 = 0.5 + 0.4375F * param4.getStepZ() + var11 * param5.getStepZ();
         param0.addParticle(
            new DustParticleOptions(param3.toVector3f(), 1.0F), param2.getX() + var12, param2.getY() + var14, param2.getZ() + var16, 0.0, 0.0, 0.0
         );
      }
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      int var5 = param0.getValue(POWER);
      if (var5 != 0) {
         for (Direction var7 : Direction.Plane.HORIZONTAL) {
            RedstoneSide var8 = param0.getValue(PROPERTY_BY_DIRECTION.get(var7));
            switch (var8) {
               case UP:
                  this.spawnParticlesAlongLine(param1, param3, param2, COLORS[var5], var7, Direction.UP, -0.5F, 0.5F);
               case SIDE:
                  this.spawnParticlesAlongLine(param1, param3, param2, COLORS[var5], Direction.DOWN, var7, 0.0F, 0.5F);
                  break;
               case NONE:
               default:
                  this.spawnParticlesAlongLine(param1, param3, param2, COLORS[var5], Direction.DOWN, var7, 0.0F, 0.3F);
            }
         }
      }
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      switch (param1) {
         case CLOCKWISE_180:
            return param0.setValue(NORTH, param0.getValue(SOUTH))
               .setValue(EAST, param0.getValue(WEST))
               .setValue(SOUTH, param0.getValue(NORTH))
               .setValue(WEST, param0.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return param0.setValue(NORTH, param0.getValue(EAST))
               .setValue(EAST, param0.getValue(SOUTH))
               .setValue(SOUTH, param0.getValue(WEST))
               .setValue(WEST, param0.getValue(NORTH));
         case CLOCKWISE_90:
            return param0.setValue(NORTH, param0.getValue(WEST))
               .setValue(EAST, param0.getValue(NORTH))
               .setValue(SOUTH, param0.getValue(EAST))
               .setValue(WEST, param0.getValue(SOUTH));
         default:
            return param0;
      }
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      switch (param1) {
         case LEFT_RIGHT:
            return param0.setValue(NORTH, param0.getValue(SOUTH)).setValue(SOUTH, param0.getValue(NORTH));
         case FRONT_BACK:
            return param0.setValue(EAST, param0.getValue(WEST)).setValue(WEST, param0.getValue(EAST));
         default:
            return super.mirror(param0, param1);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(NORTH, EAST, SOUTH, WEST, POWER);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (!param3.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      }

      if (isCross(param0) || isDot(param0)) {
         BlockState var7 = isCross(param0) ? this.defaultBlockState() : this.crossState;
         var7 = var7.setValue(POWER, param0.getValue(POWER));
         var7 = this.getConnectionState(param1, var7, param2);
         if (var7 != param0) {
            param1.setBlock(param2, var7, 3);
            this.updatesOnShapeChange(param1, param2, param0, var7);
            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   private void updatesOnShapeChange(Level param0, BlockPos param1, BlockState param2, BlockState param3) {
      for (Direction var6 : Direction.Plane.HORIZONTAL) {
         BlockPos var7 = param1.relative(var6);
         if (param2.getValue(PROPERTY_BY_DIRECTION.get(var6)).isConnected() != param3.getValue(PROPERTY_BY_DIRECTION.get(var6)).isConnected()
            && param0.getBlockState(var7).isRedstoneConductor(param0, var7)) {
            param0.updateNeighborsAtExceptFromFacing(var7, param3.getBlock(), var6.getOpposite());
         }
      }
   }
}
