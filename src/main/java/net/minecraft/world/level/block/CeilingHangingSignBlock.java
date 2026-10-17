package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CeilingHangingSignBlock extends SignBlock {
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   protected static final float AABB_OFFSET = 5.0F;
   protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
   private static final Map<Integer, VoxelShape> AABBS = Maps.newHashMap(
      ImmutableMap.of(
         0,
         Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0),
         4,
         Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0),
         8,
         Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0),
         12,
         Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0)
      )
   );

   public CeilingHangingSignBlock(BlockBehaviour.Properties param0, WoodType param1) {
      super(param0.sound(param1.hangingSignSoundType()), param1);
      this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(ATTACHED, false).setValue(WATERLOGGED, false));
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.getBlockEntity(param2) instanceof SignBlockEntity var7) {
         ItemStack var8 = param3.getItemInHand(param4);
         if (!var7.hasAnyClickCommands(param3) && var8.getItem() instanceof BlockItem) {
            return InteractionResult.PASS;
         }
      }

      return super.use(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return param1.getBlockState(param2.above()).isFaceSturdy(param1, param2.above(), Direction.DOWN, SupportType.CENTER);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      Level var2 = param0.getLevel();
      FluidState var3 = var2.getFluidState(param0.getClickedPos());
      BlockPos var4 = param0.getClickedPos().above();
      BlockState var5 = var2.getBlockState(var4);
      boolean var6 = var5.is(BlockTags.ALL_HANGING_SIGNS);
      Direction var7 = Direction.fromYRot(param0.getRotation());
      boolean var8 = !Block.isFaceFull(var5.getCollisionShape(var2, var4), Direction.DOWN) || param0.isSecondaryUseActive();
      if (var6 && !param0.isSecondaryUseActive()) {
         if (var5.hasProperty(WallHangingSignBlock.FACING)) {
            Direction var9 = var5.getValue(WallHangingSignBlock.FACING);
            if (var9.getAxis().test(var7)) {
               var8 = false;
            }
         } else if (var5.hasProperty(ROTATION)) {
            Optional<Direction> var9 = RotationSegment.convertToDirection(var5.getValue(ROTATION));
            if (var9.isPresent() && var9.get().getAxis().test(var7)) {
               var8 = false;
            }
         }
      }

      int var9 = !var8 ? RotationSegment.convertToSegment(var7.getOpposite()) : RotationSegment.convertToSegment(param0.getRotation() + 180.0F);
      return this.defaultBlockState().setValue(ATTACHED, var8).setValue(ROTATION, var9).setValue(WATERLOGGED, var3.getType() == Fluids.WATER);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      VoxelShape var5 = AABBS.get(param0.getValue(ROTATION));
      return var5 == null ? SHAPE : var5;
   }

   @Override
   public VoxelShape getBlockSupportShape(BlockState param0, BlockGetter param1, BlockPos param2) {
      return this.getShape(param0, param1, param2, CollisionContext.empty());
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return param1 == Direction.UP && !this.canSurvive(param0, param3, param4)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      return param0.setValue(ROTATION, param1.rotate(param0.getValue(ROTATION), 16));
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      return param0.setValue(ROTATION, param1.mirror(param0.getValue(ROTATION), 16));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(ROTATION, ATTACHED, WATERLOGGED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new HangingSignBlockEntity(param0, param1);
   }
}
