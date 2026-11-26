package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class StrongholdStructure extends Structure {
   public static final Codec<StrongholdStructure> CODEC = simpleCodec(StrongholdStructure::new);

   public StrongholdStructure(Structure.StructureSettings param0) {
      super(param0);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      return Optional.of(new Structure.GenerationStub(param0.chunkPos().getWorldPosition(), l_param1 -> generatePieces(l_param1, param0)));
   }

   private static void generatePieces(StructurePiecesBuilder param0, Structure.GenerationContext param1) {
      int var2 = 0;

      StrongholdPieces.StartPiece var3;
      do {
         param0.clear();
         param1.random().setLargeFeatureSeed(param1.seed() + var2++, param1.chunkPos().x, param1.chunkPos().z);
         StrongholdPieces.resetPieces();
         var3 = new StrongholdPieces.StartPiece(param1.random(), param1.chunkPos().getBlockX(2), param1.chunkPos().getBlockZ(2));
         param0.addPiece(var3);
         var3.addChildren(var3, param0, param1.random());
         List<StructurePiece> var4 = var3.pendingChildren;

         while (!var4.isEmpty()) {
            int var5 = param1.random().nextInt(var4.size());
            StructurePiece var6 = var4.remove(var5);
            var6.addChildren(var3, param0, param1.random());
         }

         param0.moveBelowSeaLevel(param1.chunkGenerator().getSeaLevel(), param1.chunkGenerator().getMinY(), param1.random(), 10);
      } while (param0.isEmpty() || var3.portalRoomPiece == null);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.STRONGHOLD;
   }
}
