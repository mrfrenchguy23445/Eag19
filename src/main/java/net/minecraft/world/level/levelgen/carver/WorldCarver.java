package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldCarver<C extends CarverConfiguration> {
   public static final WorldCarver<CaveCarverConfiguration> CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
   public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
   public static final WorldCarver<CanyonCarverConfiguration> CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
   protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
   protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
   protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
   protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
   protected Set<Fluid> liquids = ImmutableSet.of(Fluids.WATER);
   private final Codec<ConfiguredWorldCarver<C>> configuredCodec;

   private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String param0, F param1) {
      return Registry.register(BuiltInRegistries.CARVER, param0, param1);
   }

   public WorldCarver(Codec<C> param0) {
      this.configuredCodec = param0.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config).codec();
   }

   public ConfiguredWorldCarver<C> configured(C param0) {
      return new ConfiguredWorldCarver<>(this, param0);
   }

   public Codec<ConfiguredWorldCarver<C>> configuredCodec() {
      return this.configuredCodec;
   }

   public int getRange() {
      return 4;
   }

   protected boolean carveEllipsoid(
      CarvingContext param0,
      C param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      Aquifer param4,
      double param5,
      double param7,
      double param9,
      double param11,
      double param13,
      CarvingMask param15,
      WorldCarver.CarveSkipChecker param16
   ) {
      ChunkPos var18 = param2.getPos();
      double var19 = var18.getMiddleBlockX();
      double var21 = var18.getMiddleBlockZ();
      double var23 = 16.0 + param11 * 2.0;
      if (!(Math.abs(param5 - var19) > var23) && !(Math.abs(param9 - var21) > var23)) {
         int var25 = var18.getMinBlockX();
         int var26 = var18.getMinBlockZ();
         int var27 = Math.max(Mth.floor(param5 - param11) - var25 - 1, 0);
         int var28 = Math.min(Mth.floor(param5 + param11) - var25, 15);
         int var29 = Math.max(Mth.floor(param7 - param13) - 1, param0.getMinGenY() + 1);
         int var30 = param2.isUpgrading() ? 0 : 7;
         int var31 = Math.min(Mth.floor(param7 + param13) + 1, param0.getMinGenY() + param0.getGenDepth() - 1 - var30);
         int var32 = Math.max(Mth.floor(param9 - param11) - var26 - 1, 0);
         int var33 = Math.min(Mth.floor(param9 + param11) - var26, 15);
         boolean var34 = false;
         BlockPos.MutableBlockPos var35 = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos var36 = new BlockPos.MutableBlockPos();

         for (int var37 = var27; var37 <= var28; var37++) {
            int var38 = var18.getBlockX(var37);
            double var39 = (var38 + 0.5 - param5) / param11;

            for (int var41 = var32; var41 <= var33; var41++) {
               int var42 = var18.getBlockZ(var41);
               double var43 = (var42 + 0.5 - param9) / param11;
               if (!(var39 * var39 + var43 * var43 >= 1.0)) {
                  MutableBoolean var45 = new MutableBoolean(false);

                  for (int var46 = var31; var46 > var29; var46--) {
                     double var47 = (var46 - 0.5 - param7) / param13;
                     if (!param16.shouldSkip(param0, var39, var47, var43, var46) && (!param15.get(var37, var46, var41) || isDebugEnabled(param1))) {
                        param15.set(var37, var46, var41);
                        var35.set(var38, var46, var42);
                        var34 |= this.carveBlock(param0, param1, param2, param3, param15, var35, var36, param4, var45);
                     }
                  }
               }
            }
         }

         return var34;
      } else {
         return false;
      }
   }

   protected boolean carveBlock(
      CarvingContext param0,
      C param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      CarvingMask param4,
      BlockPos.MutableBlockPos param5,
      BlockPos.MutableBlockPos param6,
      Aquifer param7,
      MutableBoolean param8
   ) {
      BlockState var10 = param2.getBlockState(param5);
      if (var10.is(Blocks.GRASS_BLOCK) || var10.is(Blocks.MYCELIUM)) {
         param8.setTrue();
      }

      if (!this.canReplaceBlock(param1, var10) && !isDebugEnabled(param1)) {
         return false;
      }

      BlockState var11 = this.getCarveState(param0, param1, param5, param7);
      if (var11 == null) {
         return false;
      }

      param2.setBlockState(param5, var11, false);
      if (param7.shouldScheduleFluidUpdate() && !var11.getFluidState().isEmpty()) {
         param2.markPosForPostprocessing(param5);
      }

      if (param8.isTrue()) {
         param6.setWithOffset(param5, Direction.DOWN);
         if (param2.getBlockState(param6).is(Blocks.DIRT)) {
            param0.topMaterial(param3, param2, param6, !var11.getFluidState().isEmpty()).ifPresent(l_param2 -> {
               param2.setBlockState(param6, l_param2, false);
               if (!l_param2.getFluidState().isEmpty()) {
                  param2.markPosForPostprocessing(param6);
               }
            });
         }
      }

      return true;
   }

   @Nullable
   private BlockState getCarveState(CarvingContext param0, C param1, BlockPos param2, Aquifer param3) {
      if (param2.getY() <= param1.lavaLevel.resolveY(param0)) {
         return LAVA.createLegacyBlock();
      } else {
         BlockState var5 = param3.computeSubstance(new DensityFunction.SinglePointContext(param2.getX(), param2.getY(), param2.getZ()), 0.0);
         if (var5 == null) {
            return isDebugEnabled(param1) ? param1.debugSettings.getBarrierState() : null;
         } else {
            return isDebugEnabled(param1) ? getDebugState(param1, var5) : var5;
         }
      }
   }

   private static BlockState getDebugState(CarverConfiguration param0, BlockState param1) {
      if (param1.is(Blocks.AIR)) {
         return param0.debugSettings.getAirState();
      } else if (param1.is(Blocks.WATER)) {
         BlockState var2 = param0.debugSettings.getWaterState();
         return var2.hasProperty(BlockStateProperties.WATERLOGGED) ? var2.setValue(BlockStateProperties.WATERLOGGED, true) : var2;
      } else {
         return param1.is(Blocks.LAVA) ? param0.debugSettings.getLavaState() : param1;
      }
   }

   public abstract boolean carve(
      CarvingContext var1, C var2, ChunkAccess var3, Function<BlockPos, Holder<Biome>> var4, RandomSource var5, Aquifer var6, ChunkPos var7, CarvingMask var8
   );

   public abstract boolean isStartChunk(C var1, RandomSource var2);

   protected boolean canReplaceBlock(C param0, BlockState param1) {
      return param1.is(param0.replaceable);
   }

   protected static boolean canReach(ChunkPos param0, double param1, double param3, int param5, int param6, float param7) {
      double var8 = param0.getMiddleBlockX();
      double var10 = param0.getMiddleBlockZ();
      double var12 = param1 - var8;
      double var14 = param3 - var10;
      double var16 = param6 - param5;
      double var18 = param7 + 2.0F + 16.0F;
      return var12 * var12 + var14 * var14 - var16 * var16 <= var18 * var18;
   }

   private static boolean isDebugEnabled(CarverConfiguration param0) {
      return param0.debugSettings.isDebugMode();
   }

   public interface CarveSkipChecker {
      boolean shouldSkip(CarvingContext var1, double var2, double var4, double var6, int var8);
   }
}
