package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnEggItem extends Item {
   private static final Map<EntityType<? extends Mob>, SpawnEggItem> BY_ID = Maps.newIdentityHashMap();
   private final int backgroundColor;
   private final int highlightColor;
   private final EntityType<?> defaultType;

   public SpawnEggItem(EntityType<? extends Mob> param0, int param1, int param2, Item.Properties param3) {
      super(param3);
      this.defaultType = param0;
      this.backgroundColor = param1;
      this.highlightColor = param2;
      BY_ID.put(param0, this);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      if (!(var2 instanceof ServerLevel)) {
         return InteractionResult.SUCCESS;
      }

      ItemStack var3 = param0.getItemInHand();
      BlockPos var4 = param0.getClickedPos();
      Direction var5 = param0.getClickedFace();
      BlockState var6 = var2.getBlockState(var4);
      if (var6.is(Blocks.SPAWNER)) {
         BlockEntity var7 = var2.getBlockEntity(var4);
         if (var7 instanceof SpawnerBlockEntity var8) {
            EntityType<?> var9 = this.getType(var3.getTag());
            var8.setEntityId(var9, var2.getRandom());
            var7.setChanged();
            var2.sendBlockUpdated(var4, var6, var6, 3);
            var2.gameEvent(param0.getPlayer(), GameEvent.BLOCK_CHANGE, var4);
            var3.shrink(1);
            return InteractionResult.CONSUME;
         }
      }

      BlockPos var7;
      if (var6.getCollisionShape(var2, var4).isEmpty()) {
         var7 = var4;
      } else {
         var7 = var4.relative(var5);
      }

      EntityType<?> var8 = this.getType(var3.getTag());
      if (var8.spawn((ServerLevel)var2, var3, param0.getPlayer(), var7, MobSpawnType.SPAWN_EGG, true, !Objects.equals(var4, var7) && var5 == Direction.UP)
         != null) {
         var3.shrink(1);
         var2.gameEvent(param0.getPlayer(), GameEvent.ENTITY_PLACE, var4);
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      HitResult var5 = getPlayerPOVHitResult(param0, param1, ClipContext.Fluid.SOURCE_ONLY);
      if (var5.getType() != HitResult.Type.BLOCK) {
         return InteractionResultHolder.pass(var4);
      }

      if (!(param0 instanceof ServerLevel)) {
         return InteractionResultHolder.success(var4);
      }

      BlockHitResult var6 = (BlockHitResult)var5;
      BlockPos var7 = var6.getBlockPos();
      if (!(param0.getBlockState(var7).getBlock() instanceof LiquidBlock)) {
         return InteractionResultHolder.pass(var4);
      }

      if (param0.mayInteract(param1, var7) && param1.mayUseItemAt(var7, var6.getDirection(), var4)) {
         EntityType<?> var8 = this.getType(var4.getTag());
         Entity var9 = var8.spawn((ServerLevel)param0, var4, param1, var7, MobSpawnType.SPAWN_EGG, false, false);
         if (var9 == null) {
            return InteractionResultHolder.pass(var4);
         }

         if (!param1.getAbilities().instabuild) {
            var4.shrink(1);
         }

         param1.awardStat(Stats.ITEM_USED.get(this));
         param0.gameEvent(param1, GameEvent.ENTITY_PLACE, var9.position());
         return InteractionResultHolder.consume(var4);
      } else {
         return InteractionResultHolder.fail(var4);
      }
   }

   public boolean spawnsEntity(@Nullable CompoundTag param0, EntityType<?> param1) {
      return Objects.equals(this.getType(param0), param1);
   }

   public int getColor(int param0) {
      return param0 == 0 ? this.backgroundColor : this.highlightColor;
   }

   @Nullable
   public static SpawnEggItem byId(@Nullable EntityType<?> param0) {
      return BY_ID.get(param0);
   }

   public static Iterable<SpawnEggItem> eggs() {
      return Iterables.unmodifiableIterable(BY_ID.values());
   }

   public EntityType<?> getType(@Nullable CompoundTag param0) {
      if (param0 != null && param0.contains("EntityTag", 10)) {
         CompoundTag var2 = param0.getCompound("EntityTag");
         if (var2.contains("id", 8)) {
            return EntityType.byString(var2.getString("id")).orElse(this.defaultType);
         }
      }

      return this.defaultType;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.defaultType.requiredFeatures();
   }

   public Optional<Mob> spawnOffspringFromSpawnEgg(
      Player param0, Mob param1, EntityType<? extends Mob> param2, ServerLevel param3, Vec3 param4, ItemStack param5
   ) {
      if (!this.spawnsEntity(param5.getTag(), param2)) {
         return Optional.empty();
      }

      Mob var7;
      if (param1 instanceof AgeableMob) {
         var7 = ((AgeableMob)param1).getBreedOffspring(param3, (AgeableMob)param1);
      } else {
         var7 = param2.create(param3);
      }

      if (var7 == null) {
         return Optional.empty();
      }

      var7.setBaby(true);
      if (!var7.isBaby()) {
         return Optional.empty();
      }

      var7.moveTo(param4.x(), param4.y(), param4.z(), 0.0F, 0.0F);
      param3.addFreshEntityWithPassengers(var7);
      if (param5.hasCustomHoverName()) {
         var7.setCustomName(param5.getHoverName());
      }

      if (!param0.getAbilities().instabuild) {
         param5.shrink(1);
      }

      return Optional.of(var7);
   }
}
