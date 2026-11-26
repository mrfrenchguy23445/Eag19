package net.minecraft.world.level.material;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public abstract class LavaFluid extends FlowingFluid {
   public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

   @Override
   public Fluid getFlowing() {
      return Fluids.FLOWING_LAVA;
   }

   @Override
   public Fluid getSource() {
      return Fluids.LAVA;
   }

   @Override
   public Item getBucket() {
      return Items.LAVA_BUCKET;
   }

   @Override
   public void animateTick(Level param0, BlockPos param1, FluidState param2, RandomSource param3) {
      BlockPos var5 = param1.above();
      if (param0.getBlockState(var5).isAir() && !param0.getBlockState(var5).isSolidRender(param0, var5)) {
         if (param3.nextInt(100) == 0) {
            double var6 = param1.getX() + param3.nextDouble();
            double var8 = param1.getY() + 1.0;
            double var10 = param1.getZ() + param3.nextDouble();
            param0.addParticle(ParticleTypes.LAVA, var6, var8, var10, 0.0, 0.0, 0.0);
            param0.playLocalSound(
               var6, var8, var10, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + param3.nextFloat() * 0.2F, 0.9F + param3.nextFloat() * 0.15F, false
            );
         }

         if (param3.nextInt(200) == 0) {
            param0.playLocalSound(
               param1.getX(),
               param1.getY(),
               param1.getZ(),
               SoundEvents.LAVA_AMBIENT,
               SoundSource.BLOCKS,
               0.2F + param3.nextFloat() * 0.2F,
               0.9F + param3.nextFloat() * 0.15F,
               false
            );
         }
      }
   }

   @Override
   public void randomTick(Level param0, BlockPos param1, FluidState param2, RandomSource param3) {
      if (param0.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
         int var5 = param3.nextInt(3);
         if (var5 > 0) {
            BlockPos var6 = param1;

            for (int var7 = 0; var7 < var5; var7++) {
               var6 = var6.offset(param3.nextInt(3) - 1, 1, param3.nextInt(3) - 1);
               if (!param0.isLoaded(var6)) {
                  return;
               }

               BlockState var8 = param0.getBlockState(var6);
               if (var8.isAir()) {
                  if (this.hasFlammableNeighbours(param0, var6)) {
                     param0.setBlockAndUpdate(var6, BaseFireBlock.getState(param0, var6));
                     return;
                  }
               } else if (var8.getMaterial().blocksMotion()) {
                  return;
               }
            }
         } else {
            for (int var6 = 0; var6 < 3; var6++) {
               BlockPos var7 = param1.offset(param3.nextInt(3) - 1, 0, param3.nextInt(3) - 1);
               if (!param0.isLoaded(var7)) {
                  return;
               }

               if (param0.isEmptyBlock(var7.above()) && this.isFlammable(param0, var7)) {
                  param0.setBlockAndUpdate(var7.above(), BaseFireBlock.getState(param0, var7));
               }
            }
         }
      }
   }

   private boolean hasFlammableNeighbours(LevelReader param0, BlockPos param1) {
      for (Direction var6 : Direction.values()) {
         if (this.isFlammable(param0, param1.relative(var6))) {
            return true;
         }
      }

      return false;
   }

   private boolean isFlammable(LevelReader param0, BlockPos param1) {
      return param1.getY() >= param0.getMinBuildHeight() && param1.getY() < param0.getMaxBuildHeight() && !param0.hasChunkAt(param1)
         ? false
         : param0.getBlockState(param1).getMaterial().isFlammable();
   }

   @Nullable
   @Override
   public ParticleOptions getDripParticle() {
      return ParticleTypes.DRIPPING_LAVA;
   }

   @Override
   protected void beforeDestroyingBlock(LevelAccessor param0, BlockPos param1, BlockState param2) {
      this.fizz(param0, param1);
   }

   @Override
   public int getSlopeFindDistance(LevelReader param0) {
      return param0.dimensionType().ultraWarm() ? 4 : 2;
   }

   @Override
   public BlockState createLegacyBlock(FluidState param0) {
      return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(param0));
   }

   @Override
   public boolean isSame(Fluid param0) {
      return param0 == Fluids.LAVA || param0 == Fluids.FLOWING_LAVA;
   }

   @Override
   public int getDropOff(LevelReader param0) {
      return param0.dimensionType().ultraWarm() ? 1 : 2;
   }

   @Override
   public boolean canBeReplacedWith(FluidState param0, BlockGetter param1, BlockPos param2, Fluid param3, Direction param4) {
      return param0.getHeight(param1, param2) >= 0.44444445F && param3.is(FluidTags.WATER);
   }

   @Override
   public int getTickDelay(LevelReader param0) {
      return param0.dimensionType().ultraWarm() ? 10 : 30;
   }

   @Override
   public int getSpreadDelay(Level param0, BlockPos param1, FluidState param2, FluidState param3) {
      int var5 = this.getTickDelay(param0);
      if (!param2.isEmpty()
         && !param3.isEmpty()
         && !param2.getValue(FALLING)
         && !param3.getValue(FALLING)
         && param3.getHeight(param0, param1) > param2.getHeight(param0, param1)
         && param0.getRandom().nextInt(4) != 0) {
         var5 *= 4;
      }

      return var5;
   }

   private void fizz(LevelAccessor param0, BlockPos param1) {
      param0.levelEvent(1501, param1, 0);
   }

   @Override
   protected boolean canConvertToSource(Level param0) {
      return param0.getGameRules().getBoolean(GameRules.RULE_LAVA_SOURCE_CONVERSION);
   }

   @Override
   protected void spreadTo(LevelAccessor param0, BlockPos param1, BlockState param2, Direction param3, FluidState param4) {
      if (param3 == Direction.DOWN) {
         FluidState var6 = param0.getFluidState(param1);
         if (this.is(FluidTags.LAVA) && var6.is(FluidTags.WATER)) {
            if (param2.getBlock() instanceof LiquidBlock) {
               param0.setBlock(param1, Blocks.STONE.defaultBlockState(), 3);
            }

            this.fizz(param0, param1);
            return;
         }
      }

      super.spreadTo(param0, param1, param2, param3, param4);
   }

   @Override
   protected boolean isRandomlyTicking() {
      return true;
   }

   @Override
   protected float getExplosionResistance() {
      return 100.0F;
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
   }

   public static class Flowing extends LavaFluid {
      @Override
      protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> param0) {
         super.createFluidStateDefinition(param0);
         param0.add(LEVEL);
      }

      @Override
      public int getAmount(FluidState param0) {
         return param0.getValue(LEVEL);
      }

      @Override
      public boolean isSource(FluidState param0) {
         return false;
      }
   }

   public static class Source extends LavaFluid {
      @Override
      public int getAmount(FluidState param0) {
         return 8;
      }

      @Override
      public boolean isSource(FluidState param0) {
         return true;
      }
   }
}
