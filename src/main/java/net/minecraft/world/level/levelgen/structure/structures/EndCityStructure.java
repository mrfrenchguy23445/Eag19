package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class EndCityStructure extends Structure {
   public static final Codec<EndCityStructure> CODEC = simpleCodec(EndCityStructure::new);

   public EndCityStructure(Structure.StructureSettings param0) {
      super(param0);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      Rotation var2 = Rotation.getRandom(param0.random());
      BlockPos var3 = this.getLowestYIn5by5BoxOffset7Blocks(param0, var2);
      return var3.getY() < 60
         ? Optional.empty()
         : Optional.of(new Structure.GenerationStub(var3, l_param3 -> this.generatePieces(l_param3, var3, var2, param0)));
   }

   private void generatePieces(StructurePiecesBuilder param0, BlockPos param1, Rotation param2, Structure.GenerationContext param3) {
      List<StructurePiece> var5 = Lists.newArrayList();
      EndCityPieces.startHouseTower(param3.structureTemplateManager(), param1, param2, var5, param3.random());
      var5.forEach(param0::addPiece);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.END_CITY;
   }
}
