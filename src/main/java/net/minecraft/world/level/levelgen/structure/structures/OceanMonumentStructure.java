package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class OceanMonumentStructure extends Structure {
   public static final Codec<OceanMonumentStructure> CODEC = simpleCodec(OceanMonumentStructure::new);

   public OceanMonumentStructure(Structure.StructureSettings param0) {
      super(param0);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      int var2 = param0.chunkPos().getBlockX(9);
      int var3 = param0.chunkPos().getBlockZ(9);

      for (Holder<Biome> var6 : param0.biomeSource().getBiomesWithin(var2, param0.chunkGenerator().getSeaLevel(), var3, 29, param0.randomState().sampler())) {
         if (!var6.is(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING)) {
            return Optional.empty();
         }
      }

      return onTopOfChunkCenter(param0, Heightmap.Types.OCEAN_FLOOR_WG, l_param1 -> generatePieces(l_param1, param0));
   }

   private static StructurePiece createTopPiece(ChunkPos param0, WorldgenRandom param1) {
      int var2 = param0.getMinBlockX() - 29;
      int var3 = param0.getMinBlockZ() - 29;
      Direction var4 = Direction.Plane.HORIZONTAL.getRandomDirection(param1);
      return new OceanMonumentPieces.MonumentBuilding(param1, var2, var3, var4);
   }

   private static void generatePieces(StructurePiecesBuilder param0, Structure.GenerationContext param1) {
      param0.addPiece(createTopPiece(param1.chunkPos(), param1.random()));
   }

   public static PiecesContainer regeneratePiecesAfterLoad(ChunkPos param0, long param1, PiecesContainer param3) {
      if (param3.isEmpty()) {
         return param3;
      }

      WorldgenRandom var4 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      var4.setLargeFeatureSeed(param1, param0.x, param0.z);
      StructurePiece var5 = param3.pieces().get(0);
      BoundingBox var6 = var5.getBoundingBox();
      int var7 = var6.minX();
      int var8 = var6.minZ();
      Direction var9 = Direction.Plane.HORIZONTAL.getRandomDirection(var4);
      Direction var10 = Objects.requireNonNullElse(var5.getOrientation(), var9);
      StructurePiece var11 = new OceanMonumentPieces.MonumentBuilding(var4, var7, var8, var10);
      StructurePiecesBuilder var12 = new StructurePiecesBuilder();
      var12.addPiece(var11);
      return var12.build();
   }

   @Override
   public StructureType<?> type() {
      return StructureType.OCEAN_MONUMENT;
   }
}
