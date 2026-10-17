package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class JigsawReplacementProcessor extends StructureProcessor {
   public static final Codec<JigsawReplacementProcessor> CODEC = Codec.unit(() -> JigsawReplacementProcessor.INSTANCE);
   public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

   private JigsawReplacementProcessor() {
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader param0,
      BlockPos param1,
      BlockPos param2,
      StructureTemplate.StructureBlockInfo param3,
      StructureTemplate.StructureBlockInfo param4,
      StructurePlaceSettings param5
   ) {
      BlockState var7 = param4.state;
      if (var7.is(Blocks.JIGSAW)) {
         String var8 = param4.nbt.getString("final_state");

         BlockState var9;
         try {
            BlockStateParser.BlockResult var10 = BlockStateParser.parseForBlock(param0.holderLookup(Registries.BLOCK), var8, true);
            var9 = var10.blockState();
         } catch (CommandSyntaxException var10) {
            throw new RuntimeException(var10);
         }

         return var9.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(param4.pos, var9, null);
      } else {
         return param4;
      }
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.JIGSAW_REPLACEMENT;
   }
}
