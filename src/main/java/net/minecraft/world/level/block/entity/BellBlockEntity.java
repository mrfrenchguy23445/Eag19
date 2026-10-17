package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;

public class BellBlockEntity extends BlockEntity {
   private static final int DURATION = 50;
   private static final int GLOW_DURATION = 60;
   private static final int MIN_TICKS_BETWEEN_SEARCHES = 60;
   private static final int MAX_RESONATION_TICKS = 40;
   private static final int TICKS_BEFORE_RESONATION = 5;
   private static final int SEARCH_RADIUS = 48;
   private static final int HEAR_BELL_RADIUS = 32;
   private static final int HIGHLIGHT_RAIDERS_RADIUS = 48;
   private long lastRingTimestamp;
   public int ticks;
   public boolean shaking;
   public Direction clickDirection;
   private List<LivingEntity> nearbyEntities;
   private boolean resonating;
   private int resonationTicks;

   public BellBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.BELL, param0, param1);
   }

   @Override
   public boolean triggerEvent(int param0, int param1) {
      if (param0 == 1) {
         this.updateEntities();
         this.resonationTicks = 0;
         this.clickDirection = Direction.from3DDataValue(param1);
         this.ticks = 0;
         this.shaking = true;
         return true;
      } else {
         return super.triggerEvent(param0, param1);
      }
   }

   private static void tick(Level param0, BlockPos param1, BlockState param2, BellBlockEntity param3, BellBlockEntity.ResonationEndAction param4) {
      if (param3.shaking) {
         param3.ticks++;
      }

      if (param3.ticks >= 50) {
         param3.shaking = false;
         param3.ticks = 0;
      }

      if (param3.ticks >= 5 && param3.resonationTicks == 0 && areRaidersNearby(param1, param3.nearbyEntities)) {
         param3.resonating = true;
         param0.playSound((Player)null, param1, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      if (param3.resonating) {
         if (param3.resonationTicks < 40) {
            param3.resonationTicks++;
         } else {
            param4.run(param0, param1, param3.nearbyEntities);
            param3.resonating = false;
         }
      }
   }

   public static void clientTick(Level param0, BlockPos param1, BlockState param2, BellBlockEntity param3) {
      tick(param0, param1, param2, param3, BellBlockEntity::showBellParticles);
   }

   public static void serverTick(Level param0, BlockPos param1, BlockState param2, BellBlockEntity param3) {
      tick(param0, param1, param2, param3, BellBlockEntity::makeRaidersGlow);
   }

   public void onHit(Direction param0) {
      BlockPos var2 = this.getBlockPos();
      this.clickDirection = param0;
      if (this.shaking) {
         this.ticks = 0;
      } else {
         this.shaking = true;
      }

      this.level.blockEvent(var2, this.getBlockState().getBlock(), 1, param0.get3DDataValue());
   }

   private void updateEntities() {
      BlockPos var1 = this.getBlockPos();
      if (this.level.getGameTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
         this.lastRingTimestamp = this.level.getGameTime();
         AABB var2 = new AABB(var1).inflate(48.0);
         this.nearbyEntities = this.level.getEntitiesOfClass(LivingEntity.class, var2);
      }

      if (!this.level.isClientSide) {
         for (LivingEntity var3 : this.nearbyEntities) {
            if (var3.isAlive() && !var3.isRemoved() && var1.closerToCenterThan(var3.position(), 32.0)) {
               var3.getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, this.level.getGameTime());
            }
         }
      }
   }

   private static boolean areRaidersNearby(BlockPos param0, List<LivingEntity> param1) {
      for (LivingEntity var3 : param1) {
         if (var3.isAlive() && !var3.isRemoved() && param0.closerToCenterThan(var3.position(), 32.0) && var3.getType().is(EntityTypeTags.RAIDERS)) {
            return true;
         }
      }

      return false;
   }

   private static void makeRaidersGlow(Level param0, BlockPos param1, List<LivingEntity> param2) {
      param2.stream().filter(l_param1 -> isRaiderWithinRange(param1, l_param1)).forEach(BellBlockEntity::glow);
   }

   private static void showBellParticles(Level param0, BlockPos param1, List<LivingEntity> param2) {
      MutableInt var3 = new MutableInt(16700985);
      int var4 = (int)param2.stream().filter(l_param1 -> param1.closerToCenterThan(l_param1.position(), 48.0)).count();
      param2.stream()
         .filter(l_param1 -> isRaiderWithinRange(param1, l_param1))
         .forEach(
            l_param4 -> {
               float l_var5 = 1.0F;
               double l_var6 = Math.sqrt(
                  (l_param4.getX() - param1.getX()) * (l_param4.getX() - param1.getX()) + (l_param4.getZ() - param1.getZ()) * (l_param4.getZ() - param1.getZ())
               );
               double l_var8 = param1.getX() + 0.5F + 1.0 / l_var6 * (l_param4.getX() - param1.getX());
               double l_var10 = param1.getZ() + 0.5F + 1.0 / l_var6 * (l_param4.getZ() - param1.getZ());
               int l_var12 = Mth.clamp((var4 - 21) / -2, 3, 15);

               for (int l_var13 = 0; l_var13 < l_var12; l_var13++) {
                  int l_var14 = var3.addAndGet(5);
                  double l_var15 = FastColor.ARGB32.red(l_var14) / 255.0;
                  double l_var17 = FastColor.ARGB32.green(l_var14) / 255.0;
                  double l_var19 = FastColor.ARGB32.blue(l_var14) / 255.0;
                  param0.addParticle(ParticleTypes.ENTITY_EFFECT, l_var8, param1.getY() + 0.5F, l_var10, l_var15, l_var17, l_var19);
               }
            }
         );
   }

   private static boolean isRaiderWithinRange(BlockPos param0, LivingEntity param1) {
      return param1.isAlive() && !param1.isRemoved() && param0.closerToCenterThan(param1.position(), 48.0) && param1.getType().is(EntityTypeTags.RAIDERS);
   }

   private static void glow(LivingEntity param0) {
      param0.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60));
   }

   @FunctionalInterface
   interface ResonationEndAction {
      void run(Level var1, BlockPos var2, List<LivingEntity> var3);
   }
}
