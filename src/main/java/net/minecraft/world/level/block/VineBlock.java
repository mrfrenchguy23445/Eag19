package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VineBlock extends Block {
   public static final BooleanProperty UP = PipeBlock.UP;
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
      .entrySet()
      .stream()
      .filter(param0 -> param0.getKey() != Direction.DOWN)
      .collect(Util.toMap());
   protected static final float AABB_OFFSET = 1.0F;
   private static final VoxelShape UP_AABB = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape WEST_AABB = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
   private static final VoxelShape EAST_AABB = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
   private static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
   private final Map<BlockState, VoxelShape> shapesCache;

   public VineBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(UP, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
      );
      this.shapesCache = ImmutableMap.copyOf(
         this.stateDefinition.getPossibleStates().stream().collect(Collectors.toMap(Function.identity(), VineBlock::calculateShape))
      );
   }

   private static VoxelShape calculateShape(BlockState param0) {
      VoxelShape var1 = Shapes.empty();
      if (param0.getValue(UP)) {
         var1 = UP_AABB;
      }

      if (param0.getValue(NORTH)) {
         var1 = Shapes.or(var1, NORTH_AABB);
      }

      if (param0.getValue(SOUTH)) {
         var1 = Shapes.or(var1, SOUTH_AABB);
      }

      if (param0.getValue(EAST)) {
         var1 = Shapes.or(var1, EAST_AABB);
      }

      if (param0.getValue(WEST)) {
         var1 = Shapes.or(var1, WEST_AABB);
      }

      return var1.isEmpty() ? Shapes.block() : var1;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.shapesCache.get(param0);
   }

   @Override
   public boolean propagatesSkylightDown(BlockState param0, BlockGetter param1, BlockPos param2) {
      return true;
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return this.hasFaces(this.getUpdatedState(param0, param1, param2));
   }

   private boolean hasFaces(BlockState param0) {
      return this.countFaces(param0) > 0;
   }

   private int countFaces(BlockState param0) {
      int var2 = 0;

      for (BooleanProperty var4 : PROPERTY_BY_DIRECTION.values()) {
         if (param0.getValue(var4)) {
            var2++;
         }
      }

      return var2;
   }

   private boolean canSupportAtFace(BlockGetter param0, BlockPos param1, Direction param2) {
      if (param2 == Direction.DOWN) {
         return false;
      }

      BlockPos var4 = param1.relative(param2);
      if (isAcceptableNeighbour(param0, var4, param2)) {
         return true;
      }

      if (param2.getAxis() == Direction.Axis.Y) {
         return false;
      }

      BooleanProperty var5 = PROPERTY_BY_DIRECTION.get(param2);
      BlockState var6 = param0.getBlockState(param1.above());
      return var6.is(this) && var6.getValue(var5);
   }

   public static boolean isAcceptableNeighbour(BlockGetter param0, BlockPos param1, Direction param2) {
      return MultifaceBlock.canAttachTo(param0, param2, param1, param0.getBlockState(param1));
   }

   private BlockState getUpdatedState(BlockState param0, BlockGetter param1, BlockPos param2) {
      BlockPos var4 = param2.above();
      if (param0.getValue(UP)) {
         param0 = param0.setValue(UP, isAcceptableNeighbour(param1, var4, Direction.DOWN));
      }

      BlockState var5 = null;

      for (Direction var7 : Direction.Plane.HORIZONTAL) {
         BooleanProperty var8 = getPropertyForFace(var7);
         if (param0.getValue(var8)) {
            boolean var9 = this.canSupportAtFace(param1, param2, var7);
            if (!var9) {
               if (var5 == null) {
                  var5 = param1.getBlockState(var4);
               }

               var9 = var5.is(this) && var5.getValue(var8);
            }

            param0 = param0.setValue(var8, var9);
         }
      }

      return param0;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param1 == Direction.DOWN) {
         return super.updateShape(param0, param1, param2, param3, param4, param5);
      }

      BlockState var7 = this.getUpdatedState(param0, param3, param4);
      return !this.hasFaces(var7) ? Blocks.AIR.defaultBlockState() : var7;
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.getGameRules().getBoolean(GameRules.RULE_DO_VINES_SPREAD)) {
         if (param3.nextInt(4) == 0) {
            Direction var5 = Direction.getRandom(param3);
            BlockPos var6 = param2.above();
            if (var5.getAxis().isHorizontal() && !param0.getValue(getPropertyForFace(var5))) {
               if (this.canSpread(param1, param2)) {
                  BlockPos var7 = param2.relative(var5);
                  BlockState var8 = param1.getBlockState(var7);
                  if (var8.isAir()) {
                     Direction var9 = var5.getClockWise();
                     Direction var10 = var5.getCounterClockWise();
                     boolean var11 = param0.getValue(getPropertyForFace(var9));
                     boolean var12 = param0.getValue(getPropertyForFace(var10));
                     BlockPos var13 = var7.relative(var9);
                     BlockPos var14 = var7.relative(var10);
                     if (var11 && isAcceptableNeighbour(param1, var13, var9)) {
                        param1.setBlock(var7, this.defaultBlockState().setValue(getPropertyForFace(var9), true), 2);
                     } else if (var12 && isAcceptableNeighbour(param1, var14, var10)) {
                        param1.setBlock(var7, this.defaultBlockState().setValue(getPropertyForFace(var10), true), 2);
                     } else {
                        Direction var15 = var5.getOpposite();
                        if (var11 && param1.isEmptyBlock(var13) && isAcceptableNeighbour(param1, param2.relative(var9), var15)) {
                           param1.setBlock(var13, this.defaultBlockState().setValue(getPropertyForFace(var15), true), 2);
                        } else if (var12 && param1.isEmptyBlock(var14) && isAcceptableNeighbour(param1, param2.relative(var10), var15)) {
                           param1.setBlock(var14, this.defaultBlockState().setValue(getPropertyForFace(var15), true), 2);
                        } else if (param3.nextFloat() < 0.05 && isAcceptableNeighbour(param1, var7.above(), Direction.UP)) {
                           param1.setBlock(var7, this.defaultBlockState().setValue(UP, true), 2);
                        }
                     }
                  } else if (isAcceptableNeighbour(param1, var7, var5)) {
                     param1.setBlock(param2, param0.setValue(getPropertyForFace(var5), true), 2);
                  }
               }
            } else {
               if (var5 == Direction.UP && param2.getY() < param1.getMaxBuildHeight() - 1) {
                  if (this.canSupportAtFace(param1, param2, var5)) {
                     param1.setBlock(param2, param0.setValue(UP, true), 2);
                     return;
                  }

                  if (param1.isEmptyBlock(var6)) {
                     if (!this.canSpread(param1, param2)) {
                        return;
                     }

                     BlockState var7 = param0;

                     for (Direction var9 : Direction.Plane.HORIZONTAL) {
                        if (param3.nextBoolean() || !isAcceptableNeighbour(param1, var6.relative(var9), var9)) {
                           var7 = var7.setValue(getPropertyForFace(var9), false);
                        }
                     }

                     if (this.hasHorizontalConnection(var7)) {
                        param1.setBlock(var6, var7, 2);
                     }

                     return;
                  }
               }

               if (param2.getY() > param1.getMinBuildHeight()) {
                  BlockPos var7 = param2.below();
                  BlockState var8 = param1.getBlockState(var7);
                  if (var8.isAir() || var8.is(this)) {
                     BlockState var9 = var8.isAir() ? this.defaultBlockState() : var8;
                     BlockState var10 = this.copyRandomFaces(param0, var9, param3);
                     if (var9 != var10 && this.hasHorizontalConnection(var10)) {
                        param1.setBlock(var7, var10, 2);
                     }
                  }
               }
            }
         }
      }
   }

   private BlockState copyRandomFaces(BlockState param0, BlockState param1, RandomSource param2) {
      for (Direction var5 : Direction.Plane.HORIZONTAL) {
         if (param2.nextBoolean()) {
            BooleanProperty var6 = getPropertyForFace(var5);
            if (param0.getValue(var6)) {
               param1 = param1.setValue(var6, true);
            }
         }
      }

      return param1;
   }

   private boolean hasHorizontalConnection(BlockState param0) {
      return param0.getValue(NORTH) || param0.getValue(EAST) || param0.getValue(SOUTH) || param0.getValue(WEST);
   }

   private boolean canSpread(BlockGetter param0, BlockPos param1) {
      int var3 = 4;
      Iterable<BlockPos> var4 = BlockPos.betweenClosed(
         param1.getX() - 4, param1.getY() - 1, param1.getZ() - 4, param1.getX() + 4, param1.getY() + 1, param1.getZ() + 4
      );
      int var5 = 5;

      for (BlockPos var7 : var4) {
         if (param0.getBlockState(var7).is(this)) {
            if (--var5 <= 0) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   public boolean canBeReplaced(BlockState param0, BlockPlaceContext param1) {
      BlockState var3 = param1.getLevel().getBlockState(param1.getClickedPos());
      return var3.is(this) ? this.countFaces(var3) < PROPERTY_BY_DIRECTION.size() : super.canBeReplaced(param0, param1);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockState var2 = param0.getLevel().getBlockState(param0.getClickedPos());
      boolean var3 = var2.is(this);
      BlockState var4 = var3 ? var2 : this.defaultBlockState();

      for (Direction var8 : param0.getNearestLookingDirections()) {
         if (var8 != Direction.DOWN) {
            BooleanProperty var9 = getPropertyForFace(var8);
            boolean var10 = var3 && var2.getValue(var9);
            if (!var10 && this.canSupportAtFace(param0.getLevel(), param0.getClickedPos(), var8)) {
               return var4.setValue(var9, true);
            }
         }
      }

      return var3 ? var4 : null;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(UP, NORTH, EAST, SOUTH, WEST);
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

   public static BooleanProperty getPropertyForFace(Direction param0) {
      return PROPERTY_BY_DIRECTION.get(param0);
   }
}
