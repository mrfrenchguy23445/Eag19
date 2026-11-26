package net.minecraft.world.level.gameevent.vibrations;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VibrationListener implements GameEventListener {
   @VisibleForTesting
   public static final Object2IntMap<GameEvent> VIBRATION_FREQUENCY_FOR_EVENT = Object2IntMaps.unmodifiable(
      Util.make(new Object2IntOpenHashMap<>(), param0 -> {
         param0.put(GameEvent.STEP, 1);
         param0.put(GameEvent.ITEM_INTERACT_FINISH, 2);
         param0.put(GameEvent.FLAP, 2);
         param0.put(GameEvent.SWIM, 3);
         param0.put(GameEvent.ELYTRA_GLIDE, 4);
         param0.put(GameEvent.HIT_GROUND, 5);
         param0.put(GameEvent.TELEPORT, 5);
         param0.put(GameEvent.SPLASH, 6);
         param0.put(GameEvent.ENTITY_SHAKE, 6);
         param0.put(GameEvent.BLOCK_CHANGE, 6);
         param0.put(GameEvent.NOTE_BLOCK_PLAY, 6);
         param0.put(GameEvent.ENTITY_DISMOUNT, 6);
         param0.put(GameEvent.PROJECTILE_SHOOT, 7);
         param0.put(GameEvent.DRINK, 7);
         param0.put(GameEvent.PRIME_FUSE, 7);
         param0.put(GameEvent.ENTITY_MOUNT, 7);
         param0.put(GameEvent.PROJECTILE_LAND, 8);
         param0.put(GameEvent.EAT, 8);
         param0.put(GameEvent.ENTITY_INTERACT, 8);
         param0.put(GameEvent.ENTITY_DAMAGE, 8);
         param0.put(GameEvent.EQUIP, 9);
         param0.put(GameEvent.SHEAR, 9);
         param0.put(GameEvent.ENTITY_ROAR, 9);
         param0.put(GameEvent.BLOCK_CLOSE, 10);
         param0.put(GameEvent.BLOCK_DEACTIVATE, 10);
         param0.put(GameEvent.BLOCK_DETACH, 10);
         param0.put(GameEvent.DISPENSE_FAIL, 10);
         param0.put(GameEvent.BLOCK_OPEN, 11);
         param0.put(GameEvent.BLOCK_ACTIVATE, 11);
         param0.put(GameEvent.BLOCK_ATTACH, 11);
         param0.put(GameEvent.ENTITY_PLACE, 12);
         param0.put(GameEvent.BLOCK_PLACE, 12);
         param0.put(GameEvent.FLUID_PLACE, 12);
         param0.put(GameEvent.ENTITY_DIE, 13);
         param0.put(GameEvent.BLOCK_DESTROY, 13);
         param0.put(GameEvent.FLUID_PICKUP, 13);
         param0.put(GameEvent.CONTAINER_CLOSE, 14);
         param0.put(GameEvent.PISTON_CONTRACT, 14);
         param0.put(GameEvent.PISTON_EXTEND, 15);
         param0.put(GameEvent.CONTAINER_OPEN, 15);
         param0.put(GameEvent.EXPLODE, 15);
         param0.put(GameEvent.LIGHTNING_STRIKE, 15);
         param0.put(GameEvent.INSTRUMENT_PLAY, 15);
      })
   );
   protected final PositionSource listenerSource;
   protected final int listenerRange;
   protected final VibrationListener.VibrationListenerConfig config;
   @Nullable
   protected VibrationInfo currentVibration;
   protected int travelTimeInTicks;
   private final VibrationSelector selectionStrategy;

   public static Codec<VibrationListener> codec(VibrationListener.VibrationListenerConfig param0) {
      return RecordCodecBuilder.create(
         l_param1 -> l_param1.group(
               PositionSource.CODEC.fieldOf("source").forGetter(l2_param0 -> l2_param0.listenerSource),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("range").forGetter(l2_param0 -> l2_param0.listenerRange),
               VibrationInfo.CODEC.optionalFieldOf("event").forGetter(l2_param0 -> Optional.ofNullable(l2_param0.currentVibration)),
               VibrationSelector.CODEC.fieldOf("selector").forGetter(l2_param0 -> l2_param0.selectionStrategy),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("event_delay").orElse(0).forGetter(l2_param0 -> l2_param0.travelTimeInTicks)
            )
            .apply(
               l_param1,
               (l2_param1, l2_param2, l2_param3, l2_param4, l2_param5) -> new VibrationListener(
                  l2_param1, l2_param2, param0, l2_param3.orElse(null), l2_param4, l2_param5
               )
            )
      );
   }

   private VibrationListener(
      PositionSource param0, int param1, VibrationListener.VibrationListenerConfig param2, @Nullable VibrationInfo param3, VibrationSelector param4, int param5
   ) {
      this.listenerSource = param0;
      this.listenerRange = param1;
      this.config = param2;
      this.currentVibration = param3;
      this.travelTimeInTicks = param5;
      this.selectionStrategy = param4;
   }

   public VibrationListener(PositionSource param0, int param1, VibrationListener.VibrationListenerConfig param2) {
      this(param0, param1, param2, null, new VibrationSelector(), 0);
   }

   public static int getGameEventFrequency(GameEvent param0) {
      return VIBRATION_FREQUENCY_FOR_EVENT.getOrDefault(param0, 0);
   }

   public void tick(Level param0) {
      if (param0 instanceof ServerLevel var2) {
         if (this.currentVibration == null) {
            this.selectionStrategy
               .chosenCandidate(var2.getGameTime())
               .ifPresent(
                  l_param1 -> {
                     this.currentVibration = l_param1;
                     Vec3 l_var3 = this.currentVibration.pos();
                     this.travelTimeInTicks = Mth.floor(this.currentVibration.distance());
                     var2.sendParticles(
                        new VibrationParticleOption(this.listenerSource, this.travelTimeInTicks), l_var3.x, l_var3.y, l_var3.z, 1, 0.0, 0.0, 0.0, 0.0
                     );
                     this.config.onSignalSchedule();
                     this.selectionStrategy.startOver();
                  }
               );
         }

         if (this.currentVibration != null) {
            this.travelTimeInTicks--;
            if (this.travelTimeInTicks <= 0) {
               this.travelTimeInTicks = 0;
               this.config
                  .onSignalReceive(
                     var2,
                     this,
                     BlockPos.containing(this.currentVibration.pos()),
                     this.currentVibration.gameEvent(),
                     this.currentVibration.getEntity(var2).orElse(null),
                     this.currentVibration.getProjectileOwner(var2).orElse(null),
                     this.currentVibration.distance()
                  );
               this.currentVibration = null;
            }
         }
      }
   }

   @Override
   public PositionSource getListenerSource() {
      return this.listenerSource;
   }

   @Override
   public int getListenerRadius() {
      return this.listenerRange;
   }

   @Override
   public boolean handleGameEvent(ServerLevel param0, GameEvent param1, GameEvent.Context param2, Vec3 param3) {
      if (this.currentVibration != null) {
         return false;
      }

      if (!this.config.isValidVibration(param1, param2)) {
         return false;
      }

      Optional<Vec3> var5 = this.listenerSource.getPosition(param0);
      if (var5.isEmpty()) {
         return false;
      }

      Vec3 var6 = var5.get();
      if (!this.config.shouldListen(param0, this, BlockPos.containing(param3), param1, param2)) {
         return false;
      }

      if (isOccluded(param0, param3, var6)) {
         return false;
      }

      this.scheduleVibration(param0, param1, param2, param3, var6);
      return true;
   }

   public void forceGameEvent(ServerLevel param0, GameEvent param1, GameEvent.Context param2, Vec3 param3) {
      this.listenerSource.getPosition(param0).ifPresent(l_param4 -> this.scheduleVibration(param0, param1, param2, param3, l_param4));
   }

   public void scheduleVibration(ServerLevel param0, GameEvent param1, GameEvent.Context param2, Vec3 param3, Vec3 param4) {
      this.selectionStrategy.addCandidate(new VibrationInfo(param1, (float)param3.distanceTo(param4), param3, param2.sourceEntity()), param0.getGameTime());
   }

   private static boolean isOccluded(Level param0, Vec3 param1, Vec3 param2) {
      Vec3 var3 = new Vec3(Mth.floor(param1.x) + 0.5, Mth.floor(param1.y) + 0.5, Mth.floor(param1.z) + 0.5);
      Vec3 var4 = new Vec3(Mth.floor(param2.x) + 0.5, Mth.floor(param2.y) + 0.5, Mth.floor(param2.z) + 0.5);

      for (Direction var8 : Direction.values()) {
         Vec3 var9 = var3.relative(var8, 1.0E-5F);
         if (param0.isBlockInLine(new ClipBlockStateContext(var9, var4, l_param0 -> l_param0.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType()
            != HitResult.Type.BLOCK) {
            return false;
         }
      }

      return true;
   }

   public interface VibrationListenerConfig {
      default TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.VIBRATIONS;
      }

      default boolean canTriggerAvoidVibration() {
         return false;
      }

      default boolean isValidVibration(GameEvent param0, GameEvent.Context param1) {
         if (!param0.is(this.getListenableEvents())) {
            return false;
         }

         Entity var3 = param1.sourceEntity();
         if (var3 != null) {
            if (var3.isSpectator()) {
               return false;
            }

            if (var3.isSteppingCarefully() && param0.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
               if (this.canTriggerAvoidVibration() && var3 instanceof ServerPlayer var4) {
                  CriteriaTriggers.AVOID_VIBRATION.trigger(var4);
               }

               return false;
            }

            if (var3.dampensVibrations()) {
               return false;
            }
         }

         return param1.affectedState() != null ? !param1.affectedState().is(BlockTags.DAMPENS_VIBRATIONS) : true;
      }

      boolean shouldListen(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, GameEvent.Context var5);

      void onSignalReceive(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, @Nullable Entity var5, @Nullable Entity var6, float var7);

      default void onSignalSchedule() {
      }
   }
}
