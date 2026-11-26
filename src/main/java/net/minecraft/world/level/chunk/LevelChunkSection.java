package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {
   public static final int SECTION_WIDTH = 16;
   public static final int SECTION_HEIGHT = 16;
   public static final int SECTION_SIZE = 4096;
   public static final int BIOME_CONTAINER_BITS = 2;
   private final int bottomBlockY;
   private short nonEmptyBlockCount;
   private short tickingBlockCount;
   private short tickingFluidCount;
   private final PalettedContainer<BlockState> states;
   private PalettedContainerRO<Holder<Biome>> biomes;

   public LevelChunkSection(int param0, PalettedContainer<BlockState> param1, PalettedContainerRO<Holder<Biome>> param2) {
      this.bottomBlockY = getBottomBlockY(param0);
      this.states = param1;
      this.biomes = param2;
      this.recalcBlockCounts();
   }

   public LevelChunkSection(int param0, Registry<Biome> param1) {
      this.bottomBlockY = getBottomBlockY(param0);
      this.states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
      this.biomes = new PalettedContainer<>(param1.asHolderIdMap(), param1.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
   }

   public static int getBottomBlockY(int param0) {
      return param0 << 4;
   }

   public BlockState getBlockState(int param0, int param1, int param2) {
      return this.states.get(param0, param1, param2);
   }

   public FluidState getFluidState(int param0, int param1, int param2) {
      return this.states.get(param0, param1, param2).getFluidState();
   }

   public void acquire() {
      this.states.acquire();
   }

   public void release() {
      this.states.release();
   }

   public BlockState setBlockState(int param0, int param1, int param2, BlockState param3) {
      return this.setBlockState(param0, param1, param2, param3, true);
   }

   public BlockState setBlockState(int param0, int param1, int param2, BlockState param3, boolean param4) {
      BlockState var6;
      if (param4) {
         var6 = this.states.getAndSet(param0, param1, param2, param3);
      } else {
         var6 = this.states.getAndSetUnchecked(param0, param1, param2, param3);
      }

      FluidState var7 = var6.getFluidState();
      FluidState var8 = param3.getFluidState();
      if (!var6.isAir()) {
         this.nonEmptyBlockCount--;
         if (var6.isRandomlyTicking()) {
            this.tickingBlockCount--;
         }
      }

      if (!var7.isEmpty()) {
         this.tickingFluidCount--;
      }

      if (!param3.isAir()) {
         this.nonEmptyBlockCount++;
         if (param3.isRandomlyTicking()) {
            this.tickingBlockCount++;
         }
      }

      if (!var8.isEmpty()) {
         this.tickingFluidCount++;
      }

      return var6;
   }

   public boolean hasOnlyAir() {
      return this.nonEmptyBlockCount == 0;
   }

   public boolean isRandomlyTicking() {
      return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
   }

   public boolean isRandomlyTickingBlocks() {
      return this.tickingBlockCount > 0;
   }

   public boolean isRandomlyTickingFluids() {
      return this.tickingFluidCount > 0;
   }

   public int bottomBlockY() {
      return this.bottomBlockY;
   }

   public void recalcBlockCounts() {
      class BlockCounter implements PalettedContainer.CountConsumer<BlockState> {
         public int nonEmptyBlockCount;
         public int tickingBlockCount;
         public int tickingFluidCount;

         public void accept(BlockState l_param0, int l_param1) {
            FluidState l_var3 = l_param0.getFluidState();
            if (!l_param0.isAir()) {
               this.nonEmptyBlockCount += l_param1;
               if (l_param0.isRandomlyTicking()) {
                  this.tickingBlockCount += l_param1;
               }
            }

            if (!l_var3.isEmpty()) {
               this.nonEmptyBlockCount += l_param1;
               if (l_var3.isRandomlyTicking()) {
                  this.tickingFluidCount += l_param1;
               }
            }
         }
      }

      BlockCounter var1 = new BlockCounter();
      this.states.count(var1);
      this.nonEmptyBlockCount = (short)var1.nonEmptyBlockCount;
      this.tickingBlockCount = (short)var1.tickingBlockCount;
      this.tickingFluidCount = (short)var1.tickingFluidCount;
   }

   public PalettedContainer<BlockState> getStates() {
      return this.states;
   }

   public PalettedContainerRO<Holder<Biome>> getBiomes() {
      return this.biomes;
   }

   public void read(FriendlyByteBuf param0) {
      this.nonEmptyBlockCount = param0.readShort();
      this.states.read(param0);
      PalettedContainer<Holder<Biome>> var2 = this.biomes.recreate();
      var2.read(param0);
      this.biomes = var2;
   }

   public void readBiomes(FriendlyByteBuf param0) {
      PalettedContainer<Holder<Biome>> var2 = this.biomes.recreate();
      var2.read(param0);
      this.biomes = var2;
   }

   public void write(FriendlyByteBuf param0) {
      param0.writeShort(this.nonEmptyBlockCount);
      this.states.write(param0);
      this.biomes.write(param0);
   }

   public int getSerializedSize() {
      return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
   }

   public boolean maybeHas(Predicate<BlockState> param0) {
      return this.states.maybeHas(param0);
   }

   public Holder<Biome> getNoiseBiome(int param0, int param1, int param2) {
      return this.biomes.get(param0, param1, param2);
   }

   public void fillBiomesFromNoise(BiomeResolver param0, Climate.Sampler param1, int param2, int param3) {
      PalettedContainer<Holder<Biome>> var5 = this.biomes.recreate();
      int var6 = QuartPos.fromBlock(this.bottomBlockY());
      int var7 = 4;

      for (int var8 = 0; var8 < 4; var8++) {
         for (int var9 = 0; var9 < 4; var9++) {
            for (int var10 = 0; var10 < 4; var10++) {
               var5.getAndSetUnchecked(var8, var9, var10, param0.getNoiseBiome(param2 + var8, var6 + var9, param3 + var10, param1));
            }
         }
      }

      this.biomes = var5;
   }
}
