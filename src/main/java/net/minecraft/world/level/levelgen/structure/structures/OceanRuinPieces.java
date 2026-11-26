package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class OceanRuinPieces {
   private static final ResourceLocation[] WARM_RUINS = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/warm_1"),
      new ResourceLocation("underwater_ruin/warm_2"),
      new ResourceLocation("underwater_ruin/warm_3"),
      new ResourceLocation("underwater_ruin/warm_4"),
      new ResourceLocation("underwater_ruin/warm_5"),
      new ResourceLocation("underwater_ruin/warm_6"),
      new ResourceLocation("underwater_ruin/warm_7"),
      new ResourceLocation("underwater_ruin/warm_8")
   };
   private static final ResourceLocation[] RUINS_BRICK = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/brick_1"),
      new ResourceLocation("underwater_ruin/brick_2"),
      new ResourceLocation("underwater_ruin/brick_3"),
      new ResourceLocation("underwater_ruin/brick_4"),
      new ResourceLocation("underwater_ruin/brick_5"),
      new ResourceLocation("underwater_ruin/brick_6"),
      new ResourceLocation("underwater_ruin/brick_7"),
      new ResourceLocation("underwater_ruin/brick_8")
   };
   private static final ResourceLocation[] RUINS_CRACKED = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/cracked_1"),
      new ResourceLocation("underwater_ruin/cracked_2"),
      new ResourceLocation("underwater_ruin/cracked_3"),
      new ResourceLocation("underwater_ruin/cracked_4"),
      new ResourceLocation("underwater_ruin/cracked_5"),
      new ResourceLocation("underwater_ruin/cracked_6"),
      new ResourceLocation("underwater_ruin/cracked_7"),
      new ResourceLocation("underwater_ruin/cracked_8")
   };
   private static final ResourceLocation[] RUINS_MOSSY = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/mossy_1"),
      new ResourceLocation("underwater_ruin/mossy_2"),
      new ResourceLocation("underwater_ruin/mossy_3"),
      new ResourceLocation("underwater_ruin/mossy_4"),
      new ResourceLocation("underwater_ruin/mossy_5"),
      new ResourceLocation("underwater_ruin/mossy_6"),
      new ResourceLocation("underwater_ruin/mossy_7"),
      new ResourceLocation("underwater_ruin/mossy_8")
   };
   private static final ResourceLocation[] BIG_RUINS_BRICK = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/big_brick_1"),
      new ResourceLocation("underwater_ruin/big_brick_2"),
      new ResourceLocation("underwater_ruin/big_brick_3"),
      new ResourceLocation("underwater_ruin/big_brick_8")
   };
   private static final ResourceLocation[] BIG_RUINS_MOSSY = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/big_mossy_1"),
      new ResourceLocation("underwater_ruin/big_mossy_2"),
      new ResourceLocation("underwater_ruin/big_mossy_3"),
      new ResourceLocation("underwater_ruin/big_mossy_8")
   };
   private static final ResourceLocation[] BIG_RUINS_CRACKED = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/big_cracked_1"),
      new ResourceLocation("underwater_ruin/big_cracked_2"),
      new ResourceLocation("underwater_ruin/big_cracked_3"),
      new ResourceLocation("underwater_ruin/big_cracked_8")
   };
   private static final ResourceLocation[] BIG_WARM_RUINS = new ResourceLocation[]{
      new ResourceLocation("underwater_ruin/big_warm_4"),
      new ResourceLocation("underwater_ruin/big_warm_5"),
      new ResourceLocation("underwater_ruin/big_warm_6"),
      new ResourceLocation("underwater_ruin/big_warm_7")
   };

   private static ResourceLocation getSmallWarmRuin(RandomSource param0) {
      return Util.getRandom(WARM_RUINS, param0);
   }

   private static ResourceLocation getBigWarmRuin(RandomSource param0) {
      return Util.getRandom(BIG_WARM_RUINS, param0);
   }

   public static void addPieces(
      StructureTemplateManager param0, BlockPos param1, Rotation param2, StructurePieceAccessor param3, RandomSource param4, OceanRuinStructure param5
   ) {
      boolean var6 = param4.nextFloat() <= param5.largeProbability;
      float var7 = var6 ? 0.9F : 0.8F;
      addPiece(param0, param1, param2, param3, param4, param5, var6, var7);
      if (var6 && param4.nextFloat() <= param5.clusterProbability) {
         addClusterRuins(param0, param4, param2, param1, param5, param3);
      }
   }

   private static void addClusterRuins(
      StructureTemplateManager param0, RandomSource param1, Rotation param2, BlockPos param3, OceanRuinStructure param4, StructurePieceAccessor param5
   ) {
      BlockPos var6 = new BlockPos(param3.getX(), 90, param3.getZ());
      BlockPos var7 = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, param2, BlockPos.ZERO).offset(var6);
      BoundingBox var8 = BoundingBox.fromCorners(var6, var7);
      BlockPos var9 = new BlockPos(Math.min(var6.getX(), var7.getX()), var6.getY(), Math.min(var6.getZ(), var7.getZ()));
      List<BlockPos> var10 = allPositions(param1, var9);
      int var11 = Mth.nextInt(param1, 4, 8);

      for (int var12 = 0; var12 < var11; var12++) {
         if (!var10.isEmpty()) {
            int var13 = param1.nextInt(var10.size());
            BlockPos var14 = var10.remove(var13);
            Rotation var15 = Rotation.getRandom(param1);
            BlockPos var16 = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, var15, BlockPos.ZERO).offset(var14);
            BoundingBox var17 = BoundingBox.fromCorners(var14, var16);
            if (!var17.intersects(var8)) {
               addPiece(param0, var14, var15, param5, param1, param4, false, 0.8F);
            }
         }
      }
   }

   private static List<BlockPos> allPositions(RandomSource param0, BlockPos param1) {
      List<BlockPos> var2 = Lists.newArrayList();
      var2.add(param1.offset(-16 + Mth.nextInt(param0, 1, 8), 0, 16 + Mth.nextInt(param0, 1, 7)));
      var2.add(param1.offset(-16 + Mth.nextInt(param0, 1, 8), 0, Mth.nextInt(param0, 1, 7)));
      var2.add(param1.offset(-16 + Mth.nextInt(param0, 1, 8), 0, -16 + Mth.nextInt(param0, 4, 8)));
      var2.add(param1.offset(Mth.nextInt(param0, 1, 7), 0, 16 + Mth.nextInt(param0, 1, 7)));
      var2.add(param1.offset(Mth.nextInt(param0, 1, 7), 0, -16 + Mth.nextInt(param0, 4, 6)));
      var2.add(param1.offset(16 + Mth.nextInt(param0, 1, 7), 0, 16 + Mth.nextInt(param0, 3, 8)));
      var2.add(param1.offset(16 + Mth.nextInt(param0, 1, 7), 0, Mth.nextInt(param0, 1, 7)));
      var2.add(param1.offset(16 + Mth.nextInt(param0, 1, 7), 0, -16 + Mth.nextInt(param0, 4, 8)));
      return var2;
   }

   private static void addPiece(
      StructureTemplateManager param0,
      BlockPos param1,
      Rotation param2,
      StructurePieceAccessor param3,
      RandomSource param4,
      OceanRuinStructure param5,
      boolean param6,
      float param7
   ) {
      switch (param5.biomeTemp) {
         case WARM:
         default:
            ResourceLocation var8 = param6 ? getBigWarmRuin(param4) : getSmallWarmRuin(param4);
            param3.addPiece(new OceanRuinPieces.OceanRuinPiece(param0, var8, param1, param2, param7, param5.biomeTemp, param6));
            break;
         case COLD:
            ResourceLocation[] var9 = param6 ? BIG_RUINS_BRICK : RUINS_BRICK;
            ResourceLocation[] var10 = param6 ? BIG_RUINS_CRACKED : RUINS_CRACKED;
            ResourceLocation[] var11 = param6 ? BIG_RUINS_MOSSY : RUINS_MOSSY;
            int var12 = param4.nextInt(var9.length);
            param3.addPiece(new OceanRuinPieces.OceanRuinPiece(param0, var9[var12], param1, param2, param7, param5.biomeTemp, param6));
            param3.addPiece(new OceanRuinPieces.OceanRuinPiece(param0, var10[var12], param1, param2, 0.7F, param5.biomeTemp, param6));
            param3.addPiece(new OceanRuinPieces.OceanRuinPiece(param0, var11[var12], param1, param2, 0.5F, param5.biomeTemp, param6));
      }
   }

   public static class OceanRuinPiece extends TemplateStructurePiece {
      private final OceanRuinStructure.Type biomeType;
      private final float integrity;
      private final boolean isLarge;

      public OceanRuinPiece(
         StructureTemplateManager param0,
         ResourceLocation param1,
         BlockPos param2,
         Rotation param3,
         float param4,
         OceanRuinStructure.Type param5,
         boolean param6
      ) {
         super(StructurePieceType.OCEAN_RUIN, 0, param0, param1, param1.toString(), makeSettings(param3), param2);
         this.integrity = param4;
         this.biomeType = param5;
         this.isLarge = param6;
      }

      public OceanRuinPiece(StructureTemplateManager param0, CompoundTag param1) {
         super(StructurePieceType.OCEAN_RUIN, param1, param0, l_param1 -> makeSettings(Rotation.valueOf(param1.getString("Rot"))));
         this.integrity = param1.getFloat("Integrity");
         this.biomeType = OceanRuinStructure.Type.valueOf(param1.getString("BiomeType"));
         this.isLarge = param1.getBoolean("IsLarge");
      }

      private static StructurePlaceSettings makeSettings(Rotation param0) {
         return new StructurePlaceSettings().setRotation(param0).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putString("Rot", this.placeSettings.getRotation().name());
         param1.putFloat("Integrity", this.integrity);
         param1.putString("BiomeType", this.biomeType.toString());
         param1.putBoolean("IsLarge", this.isLarge);
      }

      @Override
      protected void handleDataMarker(String param0, BlockPos param1, ServerLevelAccessor param2, RandomSource param3, BoundingBox param4) {
         if ("chest".equals(param0)) {
            param2.setBlock(param1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, param2.getFluidState(param1).is(FluidTags.WATER)), 2);
            BlockEntity var6 = param2.getBlockEntity(param1);
            if (var6 instanceof ChestBlockEntity) {
               ((ChestBlockEntity)var6)
                  .setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, param3.nextLong());
            }
         } else if ("drowned".equals(param0)) {
            Drowned var6 = EntityType.DROWNED.create(param2.getLevel());
            if (var6 != null) {
               var6.setPersistenceRequired();
               var6.moveTo(param1, 0.0F, 0.0F);
               var6.finalizeSpawn(param2, param2.getCurrentDifficultyAt(param1), MobSpawnType.STRUCTURE, null, null);
               param2.addFreshEntityWithPassengers(var6);
               if (param1.getY() > param2.getSeaLevel()) {
                  param2.setBlock(param1, Blocks.AIR.defaultBlockState(), 2);
               } else {
                  param2.setBlock(param1, Blocks.WATER.defaultBlockState(), 2);
               }
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.placeSettings.clearProcessors().addProcessor(new BlockRotProcessor(this.integrity)).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
         int var8 = param0.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
         this.templatePosition = new BlockPos(this.templatePosition.getX(), var8, this.templatePosition.getZ());
         BlockPos var9 = StructureTemplate.transform(
               new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1),
               Mirror.NONE,
               this.placeSettings.getRotation(),
               BlockPos.ZERO
            )
            .offset(this.templatePosition);
         this.templatePosition = new BlockPos(this.templatePosition.getX(), this.getHeight(this.templatePosition, param0, var9), this.templatePosition.getZ());
         super.postProcess(param0, param1, param2, param3, param4, param5, param6);
      }

      private int getHeight(BlockPos param0, BlockGetter param1, BlockPos param2) {
         int var4 = param0.getY();
         int var5 = 512;
         int var6 = var4 - 1;
         int var7 = 0;

         for (BlockPos var9 : BlockPos.betweenClosed(param0, param2)) {
            int var10 = var9.getX();
            int var11 = var9.getZ();
            int var12 = param0.getY() - 1;
            BlockPos.MutableBlockPos var13 = new BlockPos.MutableBlockPos(var10, var12, var11);
            BlockState var14 = param1.getBlockState(var13);

            for (FluidState var15 = param1.getFluidState(var13);
               (var14.isAir() || var15.is(FluidTags.WATER) || var14.is(BlockTags.ICE)) && var12 > param1.getMinBuildHeight() + 1;
               var15 = param1.getFluidState(var13)
            ) {
               var13.set(var10, --var12, var11);
               var14 = param1.getBlockState(var13);
            }

            var5 = Math.min(var5, var12);
            if (var12 < var6 - 2) {
               var7++;
            }
         }

         int var8 = Math.abs(param0.getX() - param2.getX());
         if (var6 - var5 > 2 && var7 > var8 - 2) {
            var4 = var5 + 1;
         }

         return var4;
      }
   }
}
