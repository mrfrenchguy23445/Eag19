package net.minecraft.world.entity;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LightningBolt extends Entity {
   private static final int START_LIFE = 2;
   private static final double DAMAGE_RADIUS = 3.0;
   private static final double DETECTION_RADIUS = 15.0;
   private int life;
   public long seed;
   private int flashes;
   private boolean visualOnly;
   @Nullable
   private ServerPlayer cause;
   private final Set<Entity> hitEntities = Sets.newHashSet();
   private int blocksSetOnFire;

   public LightningBolt(EntityType<? extends LightningBolt> param0, Level param1) {
      super(param0, param1);
      this.noCulling = true;
      this.life = 2;
      this.seed = this.random.nextLong();
      this.flashes = this.random.nextInt(3) + 1;
   }

   public void setVisualOnly(boolean param0) {
      this.visualOnly = param0;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.WEATHER;
   }

   @Nullable
   public ServerPlayer getCause() {
      return this.cause;
   }

   public void setCause(@Nullable ServerPlayer param0) {
      this.cause = param0;
   }

   private void powerLightningRod() {
      BlockPos var1 = this.getStrikePosition();
      BlockState var2 = this.level.getBlockState(var1);
      if (var2.is(Blocks.LIGHTNING_ROD)) {
         ((LightningRodBlock)var2.getBlock()).onLightningStrike(var2, this.level, var1);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.life == 2) {
         if (this.level.isClientSide()) {
            this.level
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.LIGHTNING_BOLT_THUNDER,
                  SoundSource.WEATHER,
                  10000.0F,
                  0.8F + this.random.nextFloat() * 0.2F,
                  false
               );
            this.level
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.LIGHTNING_BOLT_IMPACT,
                  SoundSource.WEATHER,
                  2.0F,
                  0.5F + this.random.nextFloat() * 0.2F,
                  false
               );
         } else {
            Difficulty var1 = this.level.getDifficulty();
            if (var1 == Difficulty.NORMAL || var1 == Difficulty.HARD) {
               this.spawnFire(4);
            }

            this.powerLightningRod();
            clearCopperOnLightningStrike(this.level, this.getStrikePosition());
            this.gameEvent(GameEvent.LIGHTNING_STRIKE);
         }
      }

      this.life--;
      if (this.life < 0) {
         if (this.flashes == 0) {
            if (this.level instanceof ServerLevel) {
               List<Entity> var1 = this.level
                  .getEntities(
                     this,
                     new AABB(this.getX() - 15.0, this.getY() - 15.0, this.getZ() - 15.0, this.getX() + 15.0, this.getY() + 6.0 + 15.0, this.getZ() + 15.0),
                     l_param0 -> l_param0.isAlive() && !this.hitEntities.contains(l_param0)
                  );

               for (ServerPlayer var3 : ((ServerLevel)this.level).getPlayers(l_param0 -> l_param0.distanceTo(this) < 256.0F)) {
                  CriteriaTriggers.LIGHTNING_STRIKE.trigger(var3, this, var1);
               }
            }

            this.discard();
         } else if (this.life < -this.random.nextInt(10)) {
            this.flashes--;
            this.life = 1;
            this.seed = this.random.nextLong();
            this.spawnFire(0);
         }
      }

      if (this.life >= 0) {
         if (!(this.level instanceof ServerLevel)) {
            this.level.setSkyFlashTime(2);
         } else if (!this.visualOnly) {
            List<Entity> var1 = this.level
               .getEntities(
                  this,
                  new AABB(this.getX() - 3.0, this.getY() - 3.0, this.getZ() - 3.0, this.getX() + 3.0, this.getY() + 6.0 + 3.0, this.getZ() + 3.0),
                  Entity::isAlive
               );

            for (Entity var3 : var1) {
               var3.thunderHit((ServerLevel)this.level, this);
            }

            this.hitEntities.addAll(var1);
            if (this.cause != null) {
               CriteriaTriggers.CHANNELED_LIGHTNING.trigger(this.cause, var1);
            }
         }
      }
   }

   private BlockPos getStrikePosition() {
      Vec3 var1 = this.position();
      return BlockPos.containing(var1.x, var1.y - 1.0E-6, var1.z);
   }

   private void spawnFire(int param0) {
      if (!this.visualOnly && !this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
         BlockPos var2 = this.blockPosition();
         BlockState var3 = BaseFireBlock.getState(this.level, var2);
         if (this.level.getBlockState(var2).isAir() && var3.canSurvive(this.level, var2)) {
            this.level.setBlockAndUpdate(var2, var3);
            this.blocksSetOnFire++;
         }

         for (int var4 = 0; var4 < param0; var4++) {
            BlockPos var5 = var2.offset(this.random.nextInt(3) - 1, this.random.nextInt(3) - 1, this.random.nextInt(3) - 1);
            var3 = BaseFireBlock.getState(this.level, var5);
            if (this.level.getBlockState(var5).isAir() && var3.canSurvive(this.level, var5)) {
               this.level.setBlockAndUpdate(var5, var3);
               this.blocksSetOnFire++;
            }
         }
      }
   }

   private static void clearCopperOnLightningStrike(Level param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1);
      BlockPos var3;
      BlockState var4;
      if (var2.is(Blocks.LIGHTNING_ROD)) {
         var3 = param1.relative(var2.getValue(LightningRodBlock.FACING).getOpposite());
         var4 = param0.getBlockState(var3);
      } else {
         var3 = param1;
         var4 = var2;
      }

      if (var4.getBlock() instanceof WeatheringCopper) {
         param0.setBlockAndUpdate(var3, WeatheringCopper.getFirst(param0.getBlockState(var3)));
         BlockPos.MutableBlockPos var5 = param1.mutable();
         int var6 = param0.random.nextInt(3) + 3;

         for (int var7 = 0; var7 < var6; var7++) {
            int var8 = param0.random.nextInt(8) + 1;
            randomWalkCleaningCopper(param0, var3, var5, var8);
         }
      }
   }

   private static void randomWalkCleaningCopper(Level param0, BlockPos param1, BlockPos.MutableBlockPos param2, int param3) {
      param2.set(param1);

      for (int var4 = 0; var4 < param3; var4++) {
         Optional<BlockPos> var5 = randomStepCleaningCopper(param0, param2);
         if (!var5.isPresent()) {
            break;
         }

         param2.set(var5.get());
      }
   }

   private static Optional<BlockPos> randomStepCleaningCopper(Level param0, BlockPos param1) {
      for (BlockPos var3 : BlockPos.randomInCube(param0.random, 10, param1, 1)) {
         BlockState var4 = param0.getBlockState(var3);
         if (var4.getBlock() instanceof WeatheringCopper) {
            WeatheringCopper.getPrevious(var4).ifPresent(l_param2 -> param0.setBlockAndUpdate(var3, l_param2));
            param0.levelEvent(3002, var3, -1);
            return Optional.of(var3);
         }
      }

      return Optional.empty();
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double param0) {
      double var3 = 64.0 * getViewScale();
      return param0 < var3 * var3;
   }

   @Override
   protected void defineSynchedData() {
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
   }

   public int getBlocksSetOnFire() {
      return this.blocksSetOnFire;
   }

   public Stream<Entity> getHitEntities() {
      return this.hitEntities.stream().filter(Entity::isAlive);
   }
}
