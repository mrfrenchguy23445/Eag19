package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class RuinedPortalStructure extends Structure {
   private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{
      "ruined_portal/portal_1",
      "ruined_portal/portal_2",
      "ruined_portal/portal_3",
      "ruined_portal/portal_4",
      "ruined_portal/portal_5",
      "ruined_portal/portal_6",
      "ruined_portal/portal_7",
      "ruined_portal/portal_8",
      "ruined_portal/portal_9",
      "ruined_portal/portal_10"
   };
   private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{
      "ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"
   };
   private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
   private static final int MIN_Y_INDEX = 15;
   private final List<RuinedPortalStructure.Setup> setups;
   public static final Codec<RuinedPortalStructure> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            settingsCodec(param0),
            ExtraCodecs.nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf()).fieldOf("setups").forGetter(l_param0 -> l_param0.setups)
         )
         .apply(param0, RuinedPortalStructure::new)
   );

   public RuinedPortalStructure(Structure.StructureSettings param0, List<RuinedPortalStructure.Setup> param1) {
      super(param0);
      this.setups = param1;
   }

   public RuinedPortalStructure(Structure.StructureSettings param0, RuinedPortalStructure.Setup param1) {
      this(param0, List.of(param1));
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      RuinedPortalPiece.Properties var2 = new RuinedPortalPiece.Properties();
      WorldgenRandom var3 = param0.random();
      RuinedPortalStructure.Setup var4 = null;
      if (this.setups.size() > 1) {
         float var5 = 0.0F;

         for (RuinedPortalStructure.Setup var7 : this.setups) {
            var5 += var7.weight();
         }

         float var6 = var3.nextFloat();

         for (RuinedPortalStructure.Setup var8 : this.setups) {
            var6 -= var8.weight() / var5;
            if (var6 < 0.0F) {
               var4 = var8;
               break;
            }
         }
      } else {
         var4 = this.setups.get(0);
      }

      if (var4 == null) {
         throw new IllegalStateException();
      }

      RuinedPortalStructure.Setup var5 = var4;
      var2.airPocket = sample(var3, var5.airPocketProbability());
      var2.mossiness = var5.mossiness();
      var2.overgrown = var5.overgrown();
      var2.vines = var5.vines();
      var2.replaceWithBlackstone = var5.replaceWithBlackstone();
      ResourceLocation var6;
      if (var3.nextFloat() < 0.05F) {
         var6 = new ResourceLocation(STRUCTURE_LOCATION_GIANT_PORTALS[var3.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
      } else {
         var6 = new ResourceLocation(STRUCTURE_LOCATION_PORTALS[var3.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
      }

      StructureTemplate var7 = param0.structureTemplateManager().getOrCreate(var6);
      Rotation var8 = Util.getRandom(Rotation.values(), var3);
      Mirror var9 = var3.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
      BlockPos var10 = new BlockPos(var7.getSize().getX() / 2, 0, var7.getSize().getZ() / 2);
      ChunkGenerator var11 = param0.chunkGenerator();
      LevelHeightAccessor var12 = param0.heightAccessor();
      RandomState var13 = param0.randomState();
      BlockPos var14 = param0.chunkPos().getWorldPosition();
      BoundingBox var15 = var7.getBoundingBox(var14, var8, var10, var9);
      BlockPos var16 = var15.getCenter();
      int var17 = var11.getBaseHeight(var16.getX(), var16.getZ(), RuinedPortalPiece.getHeightMapType(var5.placement()), var12, var13) - 1;
      int var18 = findSuitableY(var3, var11, var5.placement(), var2.airPocket, var17, var15.getYSpan(), var15, var12, var13);
      BlockPos var19 = new BlockPos(var14.getX(), var18, var14.getZ());
      return Optional.of(
         new Structure.GenerationStub(
            var19,
            l_param10 -> {
               if (var5.canBeCold()) {
                  var2.cold = isCold(
                     var19,
                     param0.chunkGenerator()
                        .getBiomeSource()
                        .getNoiseBiome(QuartPos.fromBlock(var19.getX()), QuartPos.fromBlock(var19.getY()), QuartPos.fromBlock(var19.getZ()), var13.sampler())
                  );
               }

               l_param10.addPiece(new RuinedPortalPiece(param0.structureTemplateManager(), var19, var5.placement(), var2, var6, var7, var8, var9, var10));
            }
         )
      );
   }

   private static boolean sample(WorldgenRandom param0, float param1) {
      if (param1 == 0.0F) {
         return false;
      } else {
         return param1 == 1.0F ? true : param0.nextFloat() < param1;
      }
   }

   private static boolean isCold(BlockPos param0, Holder<Biome> param1) {
      return param1.value().coldEnoughToSnow(param0);
   }

   private static int findSuitableY(
      RandomSource param0,
      ChunkGenerator param1,
      RuinedPortalPiece.VerticalPlacement param2,
      boolean param3,
      int param4,
      int param5,
      BoundingBox param6,
      LevelHeightAccessor param7,
      RandomState param8
   ) {
      int var10 = param7.getMinBuildHeight() + 15;
      int var9;
      if (param2 == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
         if (param3) {
            var9 = Mth.randomBetweenInclusive(param0, 32, 100);
         } else if (param0.nextFloat() < 0.5F) {
            var9 = Mth.randomBetweenInclusive(param0, 27, 29);
         } else {
            var9 = Mth.randomBetweenInclusive(param0, 29, 100);
         }
      } else if (param2 == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
         int var11 = param4 - param5;
         var9 = getRandomWithinInterval(param0, 70, var11);
      } else if (param2 == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
         int var11 = param4 - param5;
         var9 = getRandomWithinInterval(param0, var10, var11);
      } else if (param2 == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED) {
         var9 = param4 - param5 + Mth.randomBetweenInclusive(param0, 2, 8);
      } else {
         var9 = param4;
      }

      List<BlockPos> var11 = ImmutableList.of(
         new BlockPos(param6.minX(), 0, param6.minZ()),
         new BlockPos(param6.maxX(), 0, param6.minZ()),
         new BlockPos(param6.minX(), 0, param6.maxZ()),
         new BlockPos(param6.maxX(), 0, param6.maxZ())
      );
      List<NoiseColumn> var12 = var11.stream()
         .map(l_param3 -> param1.getBaseColumn(l_param3.getX(), l_param3.getZ(), param7, param8))
         .collect(Collectors.toList());
      Heightmap.Types var13 = param2 == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;

      int var14;
      for (var14 = var9; var14 > var10; var14--) {
         int var15 = 0;

         for (NoiseColumn var17 : var12) {
            BlockState var18 = var17.getBlock(var14);
            if (var13.isOpaque().test(var18)) {
               if (++var15 == 3) {
                  return var14;
               }
            }
         }
      }

      return var14;
   }

   private static int getRandomWithinInterval(RandomSource param0, int param1, int param2) {
      return param1 < param2 ? Mth.randomBetweenInclusive(param0, param1, param2) : param2;
   }

   @Override
   public StructureType<?> type() {
      return StructureType.RUINED_PORTAL;
   }

   public record Setup(
      RuinedPortalPiece.VerticalPlacement placement,
      float airPocketProbability,
      float mossiness,
      boolean overgrown,
      boolean vines,
      boolean canBeCold,
      boolean replaceWithBlackstone,
      float weight
   ) {
      public static final Codec<RuinedPortalStructure.Setup> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(RuinedPortalStructure.Setup::placement),
               Codec.floatRange(0.0F, 1.0F).fieldOf("air_pocket_probability").forGetter(RuinedPortalStructure.Setup::airPocketProbability),
               Codec.floatRange(0.0F, 1.0F).fieldOf("mossiness").forGetter(RuinedPortalStructure.Setup::mossiness),
               Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown),
               Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines),
               Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold),
               Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone),
               ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)
            )
            .apply(param0, RuinedPortalStructure.Setup::new)
      );
   }
}
