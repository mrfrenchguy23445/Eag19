package net.minecraft.client.color.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MaterialColor;

public class BlockColors {
   private static final int DEFAULT = -1;
   private final IdMapper<BlockColor> blockColors = new IdMapper<>(32);
   private final Map<Block, Set<Property<?>>> coloringStates = Maps.newHashMap();

   public static BlockColors createDefault() {
      BlockColors var0 = new BlockColors();
      var0.register(
         (l_param0, l_param1, l_param2, l_param3) -> l_param1 != null && l_param2 != null
            ? BiomeColors.getAverageGrassColor(l_param1, l_param0.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? l_param2.below() : l_param2)
            : GrassColor.getDefaultColor(),
         Blocks.LARGE_FERN,
         Blocks.TALL_GRASS
      );
      var0.addColoringState(DoublePlantBlock.HALF, Blocks.LARGE_FERN, Blocks.TALL_GRASS);
      var0.register(
         (l_param0, l_param1, l_param2, l_param3) -> l_param1 != null && l_param2 != null
            ? BiomeColors.getAverageGrassColor(l_param1, l_param2)
            : GrassColor.getDefaultColor(),
         Blocks.GRASS_BLOCK,
         Blocks.FERN,
         Blocks.GRASS,
         Blocks.POTTED_FERN
      );
      var0.register((l_param0, l_param1, l_param2, l_param3) -> {
         if (l_param3 != 0) {
            return l_param1 != null && l_param2 != null ? BiomeColors.getAverageGrassColor(l_param1, l_param2) : GrassColor.getDefaultColor();
         } else {
            return -1;
         }
      }, Blocks.PINK_PETALS);
      var0.register((l_param0, l_param1, l_param2, l_param3) -> FoliageColor.getEvergreenColor(), Blocks.SPRUCE_LEAVES);
      var0.register((l_param0, l_param1, l_param2, l_param3) -> FoliageColor.getBirchColor(), Blocks.BIRCH_LEAVES);
      var0.register(
         (l_param0, l_param1, l_param2, l_param3) -> l_param1 != null && l_param2 != null
            ? BiomeColors.getAverageFoliageColor(l_param1, l_param2)
            : FoliageColor.getDefaultColor(),
         Blocks.OAK_LEAVES,
         Blocks.JUNGLE_LEAVES,
         Blocks.ACACIA_LEAVES,
         Blocks.DARK_OAK_LEAVES,
         Blocks.VINE,
         Blocks.MANGROVE_LEAVES
      );
      var0.register(
         (l_param0, l_param1, l_param2, l_param3) -> l_param1 != null && l_param2 != null ? BiomeColors.getAverageWaterColor(l_param1, l_param2) : -1,
         Blocks.WATER,
         Blocks.BUBBLE_COLUMN,
         Blocks.WATER_CAULDRON
      );
      var0.register(
         (l_param0, l_param1, l_param2, l_param3) -> RedStoneWireBlock.getColorForPower(l_param0.getValue(RedStoneWireBlock.POWER)), Blocks.REDSTONE_WIRE
      );
      var0.addColoringState(RedStoneWireBlock.POWER, Blocks.REDSTONE_WIRE);
      var0.register(
         (l_param0, l_param1, l_param2, l_param3) -> l_param1 != null && l_param2 != null ? BiomeColors.getAverageGrassColor(l_param1, l_param2) : -1,
         Blocks.SUGAR_CANE
      );
      var0.register((l_param0, l_param1, l_param2, l_param3) -> 14731036, Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
      var0.register((l_param0, l_param1, l_param2, l_param3) -> {
         int l_var4 = l_param0.getValue(StemBlock.AGE);
         int l_var5 = l_var4 * 32;
         int l_var6 = 255 - l_var4 * 8;
         int l_var7 = l_var4 * 4;
         return l_var5 << 16 | l_var6 << 8 | l_var7;
      }, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
      var0.addColoringState(StemBlock.AGE, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
      var0.register((l_param0, l_param1, l_param2, l_param3) -> l_param1 != null && l_param2 != null ? 2129968 : 7455580, Blocks.LILY_PAD);
      return var0;
   }

   public int getColor(BlockState param0, Level param1, BlockPos param2) {
      BlockColor var4 = this.blockColors.byId(BuiltInRegistries.BLOCK.getId(param0.getBlock()));
      if (var4 != null) {
         return var4.getColor(param0, null, null, 0);
      }

      MaterialColor var5 = param0.getMapColor(param1, param2);
      return var5 != null ? var5.col : -1;
   }

   public int getColor(BlockState param0, @Nullable BlockAndTintGetter param1, @Nullable BlockPos param2, int param3) {
      BlockColor var5 = this.blockColors.byId(BuiltInRegistries.BLOCK.getId(param0.getBlock()));
      return var5 == null ? -1 : var5.getColor(param0, param1, param2, param3);
   }

   public void register(BlockColor param0, Block... param1) {
      for (Block var6 : param1) {
         this.blockColors.addMapping(param0, BuiltInRegistries.BLOCK.getId(var6));
      }
   }

   private void addColoringStates(Set<Property<?>> param0, Block... param1) {
      for (Block var6 : param1) {
         this.coloringStates.put(var6, param0);
      }
   }

   private void addColoringState(Property<?> param0, Block... param1) {
      this.addColoringStates(ImmutableSet.of(param0), param1);
   }

   public Set<Property<?>> getColoringProperties(Block param0) {
      return this.coloringStates.getOrDefault(param0, ImmutableSet.of());
   }
}
