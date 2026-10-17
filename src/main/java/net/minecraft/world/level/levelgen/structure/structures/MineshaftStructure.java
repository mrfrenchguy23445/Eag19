package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class MineshaftStructure extends Structure {
   public static final Codec<MineshaftStructure> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(settingsCodec(param0), MineshaftStructure.Type.CODEC.fieldOf("mineshaft_type").forGetter(l_param0 -> l_param0.type))
         .apply(param0, MineshaftStructure::new)
   );
   private final MineshaftStructure.Type type;

   public MineshaftStructure(Structure.StructureSettings param0, MineshaftStructure.Type param1) {
      super(param0);
      this.type = param1;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      param0.random().nextDouble();
      ChunkPos var2 = param0.chunkPos();
      BlockPos var3 = new BlockPos(var2.getMiddleBlockX(), 50, var2.getMinBlockZ());
      StructurePiecesBuilder var4 = new StructurePiecesBuilder();
      int var5 = this.generatePiecesAndAdjust(var4, param0);
      return Optional.of(new Structure.GenerationStub(var3.offset(0, var5, 0), Either.right(var4)));
   }

   private int generatePiecesAndAdjust(StructurePiecesBuilder param0, Structure.GenerationContext param1) {
      ChunkPos var3 = param1.chunkPos();
      WorldgenRandom var4 = param1.random();
      ChunkGenerator var5 = param1.chunkGenerator();
      MineshaftPieces.MineShaftRoom var6 = new MineshaftPieces.MineShaftRoom(0, var4, var3.getBlockX(2), var3.getBlockZ(2), this.type);
      param0.addPiece(var6);
      var6.addChildren(var6, param0, var4);
      int var7 = var5.getSeaLevel();
      if (this.type == MineshaftStructure.Type.MESA) {
         BlockPos var8 = param0.getBoundingBox().getCenter();
         int var9 = var5.getBaseHeight(var8.getX(), var8.getZ(), Heightmap.Types.WORLD_SURFACE_WG, param1.heightAccessor(), param1.randomState());
         int var10 = var9 <= var7 ? var7 : Mth.randomBetweenInclusive(var4, var7, var9);
         int var11 = var10 - var8.getY();
         param0.offsetPiecesVertically(var11);
         return var11;
      } else {
         return param0.moveBelowSeaLevel(var7, var5.getMinY(), var4, 10);
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.MINESHAFT;
   }

   public enum Type implements StringRepresentable {
      NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
      MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

      public static final Codec<MineshaftStructure.Type> CODEC = StringRepresentable.fromEnum(MineshaftStructure.Type::values);
      private static final IntFunction<MineshaftStructure.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      private final String name;
      private final BlockState woodState;
      private final BlockState planksState;
      private final BlockState fenceState;

      Type(String param2, Block param3, Block param4, Block param5) {
         this.name = param2;
         this.woodState = param3.defaultBlockState();
         this.planksState = param4.defaultBlockState();
         this.fenceState = param5.defaultBlockState();
      }

      public String getName() {
         return this.name;
      }

      public static MineshaftStructure.Type byId(int param0) {
         return BY_ID.apply(param0);
      }

      public BlockState getWoodState() {
         return this.woodState;
      }

      public BlockState getPlanksState() {
         return this.planksState;
      }

      public BlockState getFenceState() {
         return this.fenceState;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
