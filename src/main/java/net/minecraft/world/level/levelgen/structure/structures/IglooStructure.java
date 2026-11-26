package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class IglooStructure extends Structure {
   public static final Codec<IglooStructure> CODEC = simpleCodec(IglooStructure::new);

   public IglooStructure(Structure.StructureSettings param0) {
      super(param0);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      return onTopOfChunkCenter(param0, Heightmap.Types.WORLD_SURFACE_WG, l_param1 -> this.generatePieces(l_param1, param0));
   }

   private void generatePieces(StructurePiecesBuilder param0, Structure.GenerationContext param1) {
      ChunkPos var3 = param1.chunkPos();
      WorldgenRandom var4 = param1.random();
      BlockPos var5 = new BlockPos(var3.getMinBlockX(), 90, var3.getMinBlockZ());
      Rotation var6 = Rotation.getRandom(var4);
      IglooPieces.addPieces(param1.structureTemplateManager(), var5, var6, param0, var4);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.IGLOO;
   }
}
