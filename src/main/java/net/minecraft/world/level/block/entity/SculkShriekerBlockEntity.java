package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class SculkShriekerBlockEntity extends BlockEntity implements VibrationListener.VibrationListenerConfig {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int LISTENER_RADIUS = 8;
   private static final int WARNING_SOUND_RADIUS = 10;
   private static final int WARDEN_SPAWN_ATTEMPTS = 20;
   private static final int WARDEN_SPAWN_RANGE_XZ = 5;
   private static final int WARDEN_SPAWN_RANGE_Y = 6;
   private static final int DARKNESS_RADIUS = 40;
   private static final Int2ObjectMap<SoundEvent> SOUND_BY_LEVEL = Util.make(new Int2ObjectOpenHashMap<>(), param0 -> {
      param0.put(1, SoundEvents.WARDEN_NEARBY_CLOSE);
      param0.put(2, SoundEvents.WARDEN_NEARBY_CLOSER);
      param0.put(3, SoundEvents.WARDEN_NEARBY_CLOSEST);
      param0.put(4, SoundEvents.WARDEN_LISTENING_ANGRY);
   });
   private static final int SHRIEKING_TICKS = 90;
   private int warningLevel;
   private VibrationListener listener = new VibrationListener(new BlockPositionSource(this.worldPosition), 8, this);

   public SculkShriekerBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.SCULK_SHRIEKER, param0, param1);
   }

   public VibrationListener getListener() {
      return this.listener;
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      if (param0.contains("warning_level", 99)) {
         this.warningLevel = param0.getInt("warning_level");
      }

      if (param0.contains("listener", 10)) {
         VibrationListener.codec(this)
            .parse(new Dynamic<>(NbtOps.INSTANCE, param0.getCompound("listener")))
            .resultOrPartial(LOGGER::error)
            .ifPresent(l_param0 -> this.listener = l_param0);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.putInt("warning_level", this.warningLevel);
      VibrationListener.codec(this)
         .encodeStart(NbtOps.INSTANCE, this.listener)
         .resultOrPartial(LOGGER::error)
         .ifPresent(l_param1 -> param0.put("listener", l_param1));
   }

   @Override
   public TagKey<GameEvent> getListenableEvents() {
      return GameEventTags.SHRIEKER_CAN_LISTEN;
   }

   @Override
   public boolean shouldListen(ServerLevel param0, GameEventListener param1, BlockPos param2, GameEvent param3, GameEvent.Context param4) {
      return !this.getBlockState().getValue(SculkShriekerBlock.SHRIEKING) && tryGetPlayer(param4.sourceEntity()) != null;
   }

   @Nullable
   public static ServerPlayer tryGetPlayer(@Nullable Entity param0) {
      if (param0 instanceof ServerPlayer var1) {
         return var1;
      } else if (param0 != null && param0.getControllingPassenger() instanceof ServerPlayer var1) {
         return var1;
      } else if (param0 instanceof Projectile var1 && var1.getOwner() instanceof ServerPlayer var2) {
         return var2;
      } else {
         return param0 instanceof ItemEntity var1 && var1.getOwner() instanceof ServerPlayer var2 ? var2 : null;
      }
   }

   @Override
   public void onSignalReceive(
      ServerLevel param0, GameEventListener param1, BlockPos param2, GameEvent param3, @Nullable Entity param4, @Nullable Entity param5, float param6
   ) {
      this.tryShriek(param0, tryGetPlayer(param5 != null ? param5 : param4));
   }

   public void tryShriek(ServerLevel param0, @Nullable ServerPlayer param1) {
      if (param1 != null) {
         BlockState var3 = this.getBlockState();
         if (!var3.getValue(SculkShriekerBlock.SHRIEKING)) {
            this.warningLevel = 0;
            if (!this.canRespond(param0) || this.tryToWarn(param0, param1)) {
               this.shriek(param0, param1);
            }
         }
      }
   }

   private boolean tryToWarn(ServerLevel param0, ServerPlayer param1) {
      OptionalInt var3 = WardenSpawnTracker.tryWarn(param0, this.getBlockPos(), param1);
      var3.ifPresent(l_param0 -> this.warningLevel = l_param0);
      return var3.isPresent();
   }

   private void shriek(ServerLevel param0, @Nullable Entity param1) {
      BlockPos var3 = this.getBlockPos();
      BlockState var4 = this.getBlockState();
      param0.setBlock(var3, var4.setValue(SculkShriekerBlock.SHRIEKING, true), 2);
      param0.scheduleTick(var3, var4.getBlock(), 90);
      param0.levelEvent(3007, var3, 0);
      param0.gameEvent(GameEvent.SHRIEK, var3, GameEvent.Context.of(param1));
   }

   private boolean canRespond(ServerLevel param0) {
      return this.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON)
         && param0.getDifficulty() != Difficulty.PEACEFUL
         && param0.getGameRules().getBoolean(GameRules.RULE_DO_WARDEN_SPAWNING);
   }

   public void tryRespond(ServerLevel param0) {
      if (this.canRespond(param0) && this.warningLevel > 0) {
         if (!this.trySummonWarden(param0)) {
            this.playWardenReplySound();
         }

         Warden.applyDarknessAround(param0, Vec3.atCenterOf(this.getBlockPos()), null, 40);
      }
   }

   private void playWardenReplySound() {
      SoundEvent var1 = SOUND_BY_LEVEL.get(this.warningLevel);
      if (var1 != null) {
         BlockPos var2 = this.getBlockPos();
         int var3 = var2.getX() + Mth.randomBetweenInclusive(this.level.random, -10, 10);
         int var4 = var2.getY() + Mth.randomBetweenInclusive(this.level.random, -10, 10);
         int var5 = var2.getZ() + Mth.randomBetweenInclusive(this.level.random, -10, 10);
         this.level.playSound(null, var3, var4, var5, var1, SoundSource.HOSTILE, 5.0F, 1.0F);
      }
   }

   private boolean trySummonWarden(ServerLevel param0) {
      return this.warningLevel < 4
         ? false
         : SpawnUtil.trySpawnMob(EntityType.WARDEN, MobSpawnType.TRIGGERED, param0, this.getBlockPos(), 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER)
            .isPresent();
   }

   @Override
   public void onSignalSchedule() {
      this.setChanged();
   }
}
