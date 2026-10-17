package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NetherFossilStructure extends Structure {
   public static final Codec<NetherFossilStructure> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(settingsCodec(param0), HeightProvider.CODEC.fieldOf("height").forGetter(l_param0 -> l_param0.height))
         .apply(param0, NetherFossilStructure::new)
   );
   public final HeightProvider height;

   public NetherFossilStructure(Structure.StructureSettings param0, HeightProvider param1) {
      super(param0);
      this.height = param1;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      WorldgenRandom var2 = param0.random();
      int var3 = param0.chunkPos().getMinBlockX() + var2.nextInt(16);
      int var4 = param0.chunkPos().getMinBlockZ() + var2.nextInt(16);
      int var5 = param0.chunkGenerator().getSeaLevel();
      WorldGenerationContext var6 = new WorldGenerationContext(param0.chunkGenerator(), param0.heightAccessor());
      int var7 = this.height.sample(var2, var6);
      NoiseColumn var8 = param0.chunkGenerator().getBaseColumn(var3, var4, param0.heightAccessor(), param0.randomState());
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos(var3, var7, var4);

      while (var7 > var5) {
         BlockState var10 = var8.getBlock(var7);
         BlockState var11 = var8.getBlock(--var7);
         if (var10.isAir() && (var11.is(Blocks.SOUL_SAND) || var11.isFaceSturdy(EmptyBlockGetter.INSTANCE, var9.setY(var7), Direction.UP))) {
            break;
         }
      }

      if (var7 <= var5) {
         return Optional.empty();
      }

      BlockPos var10 = new BlockPos(var3, var7, var4);
      return Optional.of(
         new Structure.GenerationStub(var10, l_param3 -> NetherFossilPieces.addPieces(param0.structureTemplateManager(), l_param3, var2, var10))
      );
   }

   @Override
   public StructureType<?> type() {
      return StructureType.NETHER_FOSSIL;
   }
}
