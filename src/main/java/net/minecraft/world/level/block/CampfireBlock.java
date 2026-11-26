package net.minecraft.world.level.block;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CampfireBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final BooleanProperty SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape VIRTUAL_FENCE_POST = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
   private static final int SMOKE_DISTANCE = 5;
   private final boolean spawnParticles;
   private final int fireDamage;

   public CampfireBlock(boolean param0, int param1, BlockBehaviour.Properties param2) {
      super(param2);
      this.spawnParticles = param0;
      this.fireDamage = param1;
      this.registerDefaultState(
         this.stateDefinition.any().setValue(LIT, true).setValue(SIGNAL_FIRE, false).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH)
      );
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.getBlockEntity(param2) instanceof CampfireBlockEntity var8) {
         ItemStack var9 = param3.getItemInHand(param4);
         Optional<CampfireCookingRecipe> var10 = var8.getCookableRecipe(var9);
         if (var10.isPresent()) {
            if (!param1.isClientSide && var8.placeFood(param3, param3.getAbilities().instabuild ? var9.copy() : var9, var10.get().getCookingTime())) {
               param3.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
               return InteractionResult.SUCCESS;
            }

            return InteractionResult.CONSUME;
         }
      }

      return InteractionResult.PASS;
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (param0.getValue(LIT) && param3 instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity)param3)) {
         param3.hurt(param1.damageSources().inFire(), this.fireDamage);
      }

      super.entityInside(param0, param1, param2, param3);
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param0.is(param3.getBlock())) {
         BlockEntity var6 = param1.getBlockEntity(param2);
         if (var6 instanceof CampfireBlockEntity) {
            Containers.dropContents(param1, param2, ((CampfireBlockEntity)var6).getItems());
         }

         super.onRemove(param0, param1, param2, param3, param4);
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      LevelAccessor var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      boolean var4 = var2.getFluidState(var3).getType() == Fluids.WATER;
      return this.defaultBlockState()
         .setValue(WATERLOGGED, var4)
         .setValue(SIGNAL_FIRE, this.isSmokeSource(var2.getBlockState(var3.below())))
         .setValue(LIT, !var4)
         .setValue(FACING, param0.getHorizontalDirection());
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return param1 == Direction.DOWN
         ? param0.setValue(SIGNAL_FIRE, this.isSmokeSource(param2))
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   private boolean isSmokeSource(BlockState param0) {
      return param0.is(Blocks.HAY_BLOCK);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LIT)) {
         if (param3.nextInt(10) == 0) {
            param1.playLocalSound(
               param2.getX() + 0.5,
               param2.getY() + 0.5,
               param2.getZ() + 0.5,
               SoundEvents.CAMPFIRE_CRACKLE,
               SoundSource.BLOCKS,
               0.5F + param3.nextFloat(),
               param3.nextFloat() * 0.7F + 0.6F,
               false
            );
         }

         if (this.spawnParticles && param3.nextInt(5) == 0) {
            for (int var5 = 0; var5 < param3.nextInt(1) + 1; var5++) {
               param1.addParticle(
                  ParticleTypes.LAVA,
                  param2.getX() + 0.5,
                  param2.getY() + 0.5,
                  param2.getZ() + 0.5,
                  param3.nextFloat() / 2.0F,
                  5.0E-5,
                  param3.nextFloat() / 2.0F
               );
            }
         }
      }
   }

   public static void dowse(@Nullable Entity param0, LevelAccessor param1, BlockPos param2, BlockState param3) {
      if (param1.isClientSide()) {
         for (int var4 = 0; var4 < 20; var4++) {
            makeParticles((Level)param1, param2, param3.getValue(SIGNAL_FIRE), true);
         }
      }

      BlockEntity var4 = param1.getBlockEntity(param2);
      if (var4 instanceof CampfireBlockEntity) {
         ((CampfireBlockEntity)var4).dowse();
      }

      param1.gameEvent(param0, GameEvent.BLOCK_CHANGE, param2);
   }

   @Override
   public boolean placeLiquid(LevelAccessor param0, BlockPos param1, BlockState param2, FluidState param3) {
      if (!param2.getValue(BlockStateProperties.WATERLOGGED) && param3.getType() == Fluids.WATER) {
         boolean var5 = param2.getValue(LIT);
         if (var5) {
            if (!param0.isClientSide()) {
               param0.playSound(null, param1, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            dowse(null, param0, param1, param2);
         }

         param0.setBlock(param1, param2.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
         param0.scheduleTick(param1, param3.getType(), param3.getType().getTickDelay(param0));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void onProjectileHit(Level param0, BlockState param1, BlockHitResult param2, Projectile param3) {
      BlockPos var5 = param2.getBlockPos();
      if (!param0.isClientSide && param3.isOnFire() && param3.mayInteract(param0, var5) && !param1.getValue(LIT) && !param1.getValue(WATERLOGGED)) {
         param0.setBlock(var5, param1.setValue(BlockStateProperties.LIT, true), 11);
      }
   }

   public static void makeParticles(Level param0, BlockPos param1, boolean param2, boolean param3) {
      RandomSource var4 = param0.getRandom();
      SimpleParticleType var5 = param2 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
      param0.addAlwaysVisibleParticle(
         var5,
         true,
         param1.getX() + 0.5 + var4.nextDouble() / 3.0 * (var4.nextBoolean() ? 1 : -1),
         param1.getY() + var4.nextDouble() + var4.nextDouble(),
         param1.getZ() + 0.5 + var4.nextDouble() / 3.0 * (var4.nextBoolean() ? 1 : -1),
         0.0,
         0.07,
         0.0
      );
      if (param3) {
         param0.addParticle(
            ParticleTypes.SMOKE,
            param1.getX() + 0.5 + var4.nextDouble() / 4.0 * (var4.nextBoolean() ? 1 : -1),
            param1.getY() + 0.4,
            param1.getZ() + 0.5 + var4.nextDouble() / 4.0 * (var4.nextBoolean() ? 1 : -1),
            0.0,
            0.005,
            0.0
         );
      }
   }

   public static boolean isSmokeyPos(Level param0, BlockPos param1) {
      for (int var2 = 1; var2 <= 5; var2++) {
         BlockPos var3 = param1.below(var2);
         BlockState var4 = param0.getBlockState(var3);
         if (isLitCampfire(var4)) {
            return true;
         }

         boolean var5 = Shapes.joinIsNotEmpty(VIRTUAL_FENCE_POST, var4.getCollisionShape(param0, param1, CollisionContext.empty()), BooleanOp.AND);
         if (var5) {
            BlockState var6 = param0.getBlockState(var3.below());
            return isLitCampfire(var6);
         }
      }

      return false;
   }

   public static boolean isLitCampfire(BlockState param0) {
      return param0.hasProperty(LIT) && param0.is(BlockTags.CAMPFIRES) && param0.getValue(LIT);
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
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
      param0.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new CampfireBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      if (param0.isClientSide) {
         return param1.getValue(LIT) ? createTickerHelper(param2, BlockEntityType.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
      } else {
         return param1.getValue(LIT)
            ? createTickerHelper(param2, BlockEntityType.CAMPFIRE, CampfireBlockEntity::cookTick)
            : createTickerHelper(param2, BlockEntityType.CAMPFIRE, CampfireBlockEntity::cooldownTick);
      }
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   public static boolean canLight(BlockState param0) {
      return param0.is(BlockTags.CAMPFIRES, l_param0 -> l_param0.hasProperty(WATERLOGGED) && l_param0.hasProperty(LIT))
         && !param0.getValue(WATERLOGGED)
         && !param0.getValue(LIT);
   }
}
