package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;

public class RemoveBlockGoal extends MoveToBlockGoal {
   private final Block blockToRemove;
   private final Mob removerMob;
   private int ticksSinceReachedGoal;
   private static final int WAIT_AFTER_BLOCK_FOUND = 20;

   public RemoveBlockGoal(Block param0, PathfinderMob param1, double param2, int param4) {
      super(param1, param2, 24, param4);
      this.blockToRemove = param0;
      this.removerMob = param1;
   }

   @Override
   public boolean canUse() {
      if (!this.removerMob.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
         return false;
      } else if (this.nextStartTick > 0) {
         this.nextStartTick--;
         return false;
      } else if (this.findNearestBlock()) {
         this.nextStartTick = reducedTickDelay(20);
         return true;
      } else {
         this.nextStartTick = this.nextStartTick(this.mob);
         return false;
      }
   }

   @Override
   public void stop() {
      super.stop();
      this.removerMob.fallDistance = 1.0F;
   }

   @Override
   public void start() {
      super.start();
      this.ticksSinceReachedGoal = 0;
   }

   public void playDestroyProgressSound(LevelAccessor param0, BlockPos param1) {
   }

   public void playBreakSound(Level param0, BlockPos param1) {
   }

   @Override
   public void tick() {
      super.tick();
      Level var1 = this.removerMob.level;
      BlockPos var2 = this.removerMob.blockPosition();
      BlockPos var3 = this.getPosWithBlock(var2, var1);
      RandomSource var4 = this.removerMob.getRandom();
      if (this.isReachedTarget() && var3 != null) {
         if (this.ticksSinceReachedGoal > 0) {
            Vec3 var5 = this.removerMob.getDeltaMovement();
            this.removerMob.setDeltaMovement(var5.x, 0.3, var5.z);
            if (!var1.isClientSide) {
               double var6 = 0.08;
               ((ServerLevel)var1)
                  .sendParticles(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EGG)),
                     var3.getX() + 0.5,
                     var3.getY() + 0.7,
                     var3.getZ() + 0.5,
                     3,
                     (var4.nextFloat() - 0.5) * 0.08,
                     (var4.nextFloat() - 0.5) * 0.08,
                     (var4.nextFloat() - 0.5) * 0.08,
                     0.15F
                  );
            }
         }

         if (this.ticksSinceReachedGoal % 2 == 0) {
            Vec3 var5 = this.removerMob.getDeltaMovement();
            this.removerMob.setDeltaMovement(var5.x, -0.3, var5.z);
            if (this.ticksSinceReachedGoal % 6 == 0) {
               this.playDestroyProgressSound(var1, this.blockPos);
            }
         }

         if (this.ticksSinceReachedGoal > 60) {
            var1.removeBlock(var3, false);
            if (!var1.isClientSide) {
               for (int var5 = 0; var5 < 20; var5++) {
                  double var6 = var4.nextGaussian() * 0.02;
                  double var8 = var4.nextGaussian() * 0.02;
                  double var10 = var4.nextGaussian() * 0.02;
                  ((ServerLevel)var1).sendParticles(ParticleTypes.POOF, var3.getX() + 0.5, var3.getY(), var3.getZ() + 0.5, 1, var6, var8, var10, 0.15F);
               }

               this.playBreakSound(var1, var3);
            }
         }

         this.ticksSinceReachedGoal++;
      }
   }

   @Nullable
   private BlockPos getPosWithBlock(BlockPos param0, BlockGetter param1) {
      if (param1.getBlockState(param0).is(this.blockToRemove)) {
         return param0;
      }

      BlockPos[] var3 = new BlockPos[]{param0.below(), param0.west(), param0.east(), param0.north(), param0.south(), param0.below().below()};

      for (BlockPos var7 : var3) {
         if (param1.getBlockState(var7).is(this.blockToRemove)) {
            return var7;
         }
      }

      return null;
   }

   @Override
   protected boolean isValidTarget(LevelReader param0, BlockPos param1) {
      ChunkAccess var3 = param0.getChunk(SectionPos.blockToSectionCoord(param1.getX()), SectionPos.blockToSectionCoord(param1.getZ()), ChunkStatus.FULL, false);
      return var3 == null
         ? false
         : var3.getBlockState(param1).is(this.blockToRemove) && var3.getBlockState(param1.above()).isAir() && var3.getBlockState(param1.above(2)).isAir();
   }
}
