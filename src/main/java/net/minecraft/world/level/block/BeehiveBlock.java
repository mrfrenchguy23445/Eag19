package net.minecraft.world.level.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BeehiveBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final IntegerProperty HONEY_LEVEL = BlockStateProperties.LEVEL_HONEY;
   public static final int MAX_HONEY_LEVELS = 5;
   private static final int SHEARED_HONEYCOMB_COUNT = 3;

   public BeehiveBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(HONEY_LEVEL, 0).setValue(FACING, Direction.NORTH));
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      return param0.getValue(HONEY_LEVEL);
   }

   @Override
   public void playerDestroy(Level param0, Player param1, BlockPos param2, BlockState param3, @Nullable BlockEntity param4, ItemStack param5) {
      super.playerDestroy(param0, param1, param2, param3, param4, param5);
      if (!param0.isClientSide && param4 instanceof BeehiveBlockEntity var7) {
         if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, param5) == 0) {
            var7.emptyAllLivingFromHive(param1, param3, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            param0.updateNeighbourForOutputSignal(param2, this);
            this.angerNearbyBees(param0, param2);
         }

         CriteriaTriggers.BEE_NEST_DESTROYED.trigger((ServerPlayer)param1, param3, param5, var7.getOccupantCount());
      }
   }

   private void angerNearbyBees(Level param0, BlockPos param1) {
      List<Bee> var3 = param0.getEntitiesOfClass(Bee.class, new AABB(param1).inflate(8.0, 6.0, 8.0));
      if (!var3.isEmpty()) {
         List<Player> var4 = param0.getEntitiesOfClass(Player.class, new AABB(param1).inflate(8.0, 6.0, 8.0));
         int var5 = var4.size();

         for (Bee var7 : var3) {
            if (var7.getTarget() == null) {
               var7.setTarget(var4.get(param0.random.nextInt(var5)));
            }
         }
      }
   }

   public static void dropHoneycomb(Level param0, BlockPos param1) {
      popResource(param0, param1, new ItemStack(Items.HONEYCOMB, 3));
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      ItemStack var7 = param3.getItemInHand(param4);
      int var8 = param0.getValue(HONEY_LEVEL);
      boolean var9 = false;
      if (var8 >= 5) {
         Item var10 = var7.getItem();
         if (var7.is(Items.SHEARS)) {
            param1.playSound(param3, param3.getX(), param3.getY(), param3.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            dropHoneycomb(param1, param2);
            var7.hurtAndBreak(1, param3, l_param1 -> l_param1.broadcastBreakEvent(param4));
            var9 = true;
            param1.gameEvent(param3, GameEvent.SHEAR, param2);
         } else if (var7.is(Items.GLASS_BOTTLE)) {
            var7.shrink(1);
            param1.playSound(param3, param3.getX(), param3.getY(), param3.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (var7.isEmpty()) {
               param3.setItemInHand(param4, new ItemStack(Items.HONEY_BOTTLE));
            } else if (!param3.getInventory().add(new ItemStack(Items.HONEY_BOTTLE))) {
               param3.drop(new ItemStack(Items.HONEY_BOTTLE), false);
            }

            var9 = true;
            param1.gameEvent(param3, GameEvent.FLUID_PICKUP, param2);
         }

         if (!param1.isClientSide() && var9) {
            param3.awardStat(Stats.ITEM_USED.get(var10));
         }
      }

      if (var9) {
         if (!CampfireBlock.isSmokeyPos(param1, param2)) {
            if (this.hiveContainsBees(param1, param2)) {
               this.angerNearbyBees(param1, param2);
            }

            this.releaseBeesAndResetHoneyLevel(param1, param0, param2, param3, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
         } else {
            this.resetHoneyLevel(param1, param0, param2);
         }

         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else {
         return super.use(param0, param1, param2, param3, param4, param5);
      }
   }

   private boolean hiveContainsBees(Level param0, BlockPos param1) {
      return param0.getBlockEntity(param1) instanceof BeehiveBlockEntity var4 ? !var4.isEmpty() : false;
   }

   public void releaseBeesAndResetHoneyLevel(
      Level param0, BlockState param1, BlockPos param2, @Nullable Player param3, BeehiveBlockEntity.BeeReleaseStatus param4
   ) {
      this.resetHoneyLevel(param0, param1, param2);
      if (param0.getBlockEntity(param2) instanceof BeehiveBlockEntity var7) {
         var7.emptyAllLivingFromHive(param3, param1, param4);
      }
   }

   public void resetHoneyLevel(Level param0, BlockState param1, BlockPos param2) {
      param0.setBlock(param2, param1.setValue(HONEY_LEVEL, 0), 3);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(HONEY_LEVEL) >= 5) {
         for (int var5 = 0; var5 < param3.nextInt(1) + 1; var5++) {
            this.trySpawnDripParticles(param1, param2, param0);
         }
      }
   }

   private void trySpawnDripParticles(Level param0, BlockPos param1, BlockState param2) {
      if (param2.getFluidState().isEmpty() && !(param0.random.nextFloat() < 0.3F)) {
         VoxelShape var4 = param2.getCollisionShape(param0, param1);
         double var5 = var4.max(Direction.Axis.Y);
         if (var5 >= 1.0 && !param2.is(BlockTags.IMPERMEABLE)) {
            double var7 = var4.min(Direction.Axis.Y);
            if (var7 > 0.0) {
               this.spawnParticle(param0, param1, var4, param1.getY() + var7 - 0.05);
            } else {
               BlockPos var9 = param1.below();
               BlockState var10 = param0.getBlockState(var9);
               VoxelShape var11 = var10.getCollisionShape(param0, var9);
               double var12 = var11.max(Direction.Axis.Y);
               if ((var12 < 1.0 || !var10.isCollisionShapeFullBlock(param0, var9)) && var10.getFluidState().isEmpty()) {
                  this.spawnParticle(param0, param1, var4, param1.getY() - 0.05);
               }
            }
         }
      }
   }

   private void spawnParticle(Level param0, BlockPos param1, VoxelShape param2, double param3) {
      this.spawnFluidParticle(
         param0,
         param1.getX() + param2.min(Direction.Axis.X),
         param1.getX() + param2.max(Direction.Axis.X),
         param1.getZ() + param2.min(Direction.Axis.Z),
         param1.getZ() + param2.max(Direction.Axis.Z),
         param3
      );
   }

   private void spawnFluidParticle(Level param0, double param1, double param3, double param5, double param7, double param9) {
      param0.addParticle(
         ParticleTypes.DRIPPING_HONEY,
         Mth.lerp(param0.random.nextDouble(), param1, param3),
         param9,
         Mth.lerp(param0.random.nextDouble(), param5, param7),
         0.0,
         0.0,
         0.0
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.defaultBlockState().setValue(FACING, param0.getHorizontalDirection().getOpposite());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(HONEY_LEVEL, FACING);
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new BeehiveBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return param0.isClientSide ? null : createTickerHelper(param2, BlockEntityType.BEEHIVE, BeehiveBlockEntity::serverTick);
   }

   @Override
   public void playerWillDestroy(Level param0, BlockPos param1, BlockState param2, Player param3) {
      if (!param0.isClientSide
         && param3.isCreative()
         && param0.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
         && param0.getBlockEntity(param1) instanceof BeehiveBlockEntity var6) {
         ItemStack var7 = new ItemStack(this);
         int var8 = param2.getValue(HONEY_LEVEL);
         boolean var9 = !var6.isEmpty();
         if (var9 || var8 > 0) {
            if (var9) {
               CompoundTag var10 = new CompoundTag();
               var10.put("Bees", var6.writeBees());
               BlockItem.setBlockEntityData(var7, BlockEntityType.BEEHIVE, var10);
            }

            CompoundTag var10 = new CompoundTag();
            var10.putInt("honey_level", var8);
            var7.addTagElement("BlockStateTag", var10);
            ItemEntity var11 = new ItemEntity(param0, param1.getX(), param1.getY(), param1.getZ(), var7);
            var11.setDefaultPickUpDelay();
            param0.addFreshEntity(var11);
         }
      }

      super.playerWillDestroy(param0, param1, param2, param3);
   }

   @Override
   public List<ItemStack> getDrops(BlockState param0, LootContext.Builder param1) {
      Entity var3 = param1.getOptionalParameter(LootContextParams.THIS_ENTITY);
      if (var3 instanceof PrimedTnt || var3 instanceof Creeper || var3 instanceof WitherSkull || var3 instanceof WitherBoss || var3 instanceof MinecartTNT) {
         BlockEntity var4 = param1.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
         if (var4 instanceof BeehiveBlockEntity var5) {
            var5.emptyAllLivingFromHive(null, param0, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
         }
      }

      return super.getDrops(param0, param1);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param3.getBlockState(param5).getBlock() instanceof FireBlock && param3.getBlockEntity(param4) instanceof BeehiveBlockEntity var8) {
         var8.emptyAllLivingFromHive(null, param0, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }
}
