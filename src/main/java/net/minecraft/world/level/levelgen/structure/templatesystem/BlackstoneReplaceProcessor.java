package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlackstoneReplaceProcessor extends StructureProcessor {
   public static final Codec<BlackstoneReplaceProcessor> CODEC = Codec.unit(() -> BlackstoneReplaceProcessor.INSTANCE);
   public static final BlackstoneReplaceProcessor INSTANCE = new BlackstoneReplaceProcessor();
   private final Map<Block, Block> replacements = Util.make(Maps.newHashMap(), l_param0 -> {
      l_param0.put(Blocks.COBBLESTONE, Blocks.BLACKSTONE);
      l_param0.put(Blocks.MOSSY_COBBLESTONE, Blocks.BLACKSTONE);
      l_param0.put(Blocks.STONE, Blocks.POLISHED_BLACKSTONE);
      l_param0.put(Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
      l_param0.put(Blocks.MOSSY_STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
      l_param0.put(Blocks.COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
      l_param0.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
      l_param0.put(Blocks.STONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS);
      l_param0.put(Blocks.STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
      l_param0.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
      l_param0.put(Blocks.COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
      l_param0.put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
      l_param0.put(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
      l_param0.put(Blocks.STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
      l_param0.put(Blocks.STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
      l_param0.put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
      l_param0.put(Blocks.STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
      l_param0.put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
      l_param0.put(Blocks.COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
      l_param0.put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
      l_param0.put(Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE);
      l_param0.put(Blocks.CRACKED_STONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
      l_param0.put(Blocks.IRON_BARS, Blocks.CHAIN);
   });

   private BlackstoneReplaceProcessor() {
   }

   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader param0,
      BlockPos param1,
      BlockPos param2,
      StructureTemplate.StructureBlockInfo param3,
      StructureTemplate.StructureBlockInfo param4,
      StructurePlaceSettings param5
   ) {
      Block var7 = this.replacements.get(param4.state.getBlock());
      if (var7 == null) {
         return param4;
      }

      BlockState var8 = param4.state;
      BlockState var9 = var7.defaultBlockState();
      if (var8.hasProperty(StairBlock.FACING)) {
         var9 = var9.setValue(StairBlock.FACING, var8.getValue(StairBlock.FACING));
      }

      if (var8.hasProperty(StairBlock.HALF)) {
         var9 = var9.setValue(StairBlock.HALF, var8.getValue(StairBlock.HALF));
      }

      if (var8.hasProperty(SlabBlock.TYPE)) {
         var9 = var9.setValue(SlabBlock.TYPE, var8.getValue(SlabBlock.TYPE));
      }

      return new StructureTemplate.StructureBlockInfo(param4.pos, var9, param4.nbt);
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.BLACKSTONE_REPLACE;
   }
}
