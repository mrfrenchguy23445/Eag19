package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import org.slf4j.Logger;

public class ChunkSerializer {
   private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
      Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState()
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_UPGRADE_DATA = "UpgradeData";
   private static final String BLOCK_TICKS_TAG = "block_ticks";
   private static final String FLUID_TICKS_TAG = "fluid_ticks";
   public static final String X_POS_TAG = "xPos";
   public static final String Z_POS_TAG = "zPos";
   public static final String HEIGHTMAPS_TAG = "Heightmaps";
   public static final String IS_LIGHT_ON_TAG = "isLightOn";
   public static final String SECTIONS_TAG = "sections";
   public static final String BLOCK_LIGHT_TAG = "BlockLight";
   public static final String SKY_LIGHT_TAG = "SkyLight";

   public static ProtoChunk read(ServerLevel param0, PoiManager param1, ChunkPos param2, CompoundTag param3) {
      ChunkPos var4 = new ChunkPos(param3.getInt("xPos"), param3.getInt("zPos"));
      if (!Objects.equals(param2, var4)) {
         LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", param2, param2, var4);
      }

      UpgradeData var5 = param3.contains("UpgradeData", 10) ? new UpgradeData(param3.getCompound("UpgradeData"), param0) : UpgradeData.EMPTY;
      boolean var6 = param3.getBoolean("isLightOn");
      ListTag var7 = param3.getList("sections", 10);
      int var8 = param0.getSectionsCount();
      LevelChunkSection[] var9 = new LevelChunkSection[var8];
      boolean var10 = param0.dimensionType().hasSkyLight();
      ChunkSource var11 = param0.getChunkSource();
      LevelLightEngine var12 = var11.getLightEngine();
      Registry<Biome> var13 = param0.registryAccess().registryOrThrow(Registries.BIOME);
      Codec<PalettedContainerRO<Holder<Biome>>> var14 = makeBiomeCodec(var13);
      boolean var15 = false;

      for (int var16 = 0; var16 < var7.size(); var16++) {
         CompoundTag var17 = var7.getCompound(var16);
         int var18 = var17.getByte("Y");
         int var19 = param0.getSectionIndexFromSectionY(var18);
         if (var19 >= 0 && var19 < var9.length) {
            PalettedContainer<BlockState> var20;
            if (var17.contains("block_states", 10)) {
               var20 = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, var17.getCompound("block_states"))
                  .promotePartial(l_param2 -> logErrors(param2, var18, l_param2))
                  .getOrThrow(false, LOGGER::error);
            } else {
               var20 = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
            }

            PalettedContainerRO<Holder<Biome>> var21;
            if (var17.contains("biomes", 10)) {
               var21 = var14.parse(NbtOps.INSTANCE, var17.getCompound("biomes"))
                  .promotePartial(l_param2 -> logErrors(param2, var18, l_param2))
                  .getOrThrow(false, LOGGER::error);
            } else {
               var21 = new PalettedContainer<>(var13.asHolderIdMap(), var13.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
            }

            LevelChunkSection var22 = new LevelChunkSection(var18, var20, var21);
            var9[var19] = var22;
            param1.checkConsistencyWithBlocks(param2, var22);
         }

         boolean var20 = var17.contains("BlockLight", 7);
         boolean var21 = var10 && var17.contains("SkyLight", 7);
         if (var20 || var21) {
            if (!var15) {
               var12.retainData(param2, true);
               var15 = true;
            }

            if (var20) {
               var12.queueSectionData(LightLayer.BLOCK, SectionPos.of(param2, var18), new DataLayer(var17.getByteArray("BlockLight")), true);
            }

            if (var21) {
               var12.queueSectionData(LightLayer.SKY, SectionPos.of(param2, var18), new DataLayer(var17.getByteArray("SkyLight")), true);
            }
         }
      }

      long var16 = param3.getLong("InhabitedTime");
      ChunkStatus.ChunkType var18 = getChunkTypeFromTag(param3);
      BlendingData var19;
      if (param3.contains("blending_data", 10)) {
         var19 = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, param3.getCompound("blending_data"))).resultOrPartial(LOGGER::error).orElse(null);
      } else {
         var19 = null;
      }

      ChunkAccess var20;
      if (var18 == ChunkStatus.ChunkType.LEVELCHUNK) {
         LevelChunkTicks<Block> var21 = LevelChunkTicks.load(
            param3.getList("block_ticks", 10), l_param0 -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(l_param0)), param2
         );
         LevelChunkTicks<Fluid> var22 = LevelChunkTicks.load(
            param3.getList("fluid_ticks", 10), l_param0 -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(l_param0)), param2
         );
         var20 = new LevelChunk(param0.getLevel(), param2, var5, var21, var22, var16, var9, postLoadChunk(param0, param3), var19);
      } else {
         ProtoChunkTicks<Block> var21 = ProtoChunkTicks.load(
            param3.getList("block_ticks", 10), l_param0 -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(l_param0)), param2
         );
         ProtoChunkTicks<Fluid> var22 = ProtoChunkTicks.load(
            param3.getList("fluid_ticks", 10), l_param0 -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(l_param0)), param2
         );
         ProtoChunk var23 = new ProtoChunk(param2, var5, var9, var21, var22, param0, var13, var19);
         var20 = var23;
         var20.setInhabitedTime(var16);
         if (param3.contains("below_zero_retrogen", 10)) {
            BelowZeroRetrogen.CODEC
               .parse(new Dynamic<>(NbtOps.INSTANCE, param3.getCompound("below_zero_retrogen")))
               .resultOrPartial(LOGGER::error)
               .ifPresent(var23::setBelowZeroRetrogen);
         }

         ChunkStatus var24 = ChunkStatus.byName(param3.getString("Status"));
         var23.setStatus(var24);
         if (var24.isOrAfter(ChunkStatus.FEATURES)) {
            var23.setLightEngine(var12);
         }

         BelowZeroRetrogen var25 = var23.getBelowZeroRetrogen();
         boolean var26 = var24.isOrAfter(ChunkStatus.LIGHT) || var25 != null && var25.targetStatus().isOrAfter(ChunkStatus.LIGHT);
         if (!var6 && var26) {
            for (BlockPos var28 : BlockPos.betweenClosed(
               param2.getMinBlockX(),
               param0.getMinBuildHeight(),
               param2.getMinBlockZ(),
               param2.getMaxBlockX(),
               param0.getMaxBuildHeight() - 1,
               param2.getMaxBlockZ()
            )) {
               if (var20.getBlockState(var28).getLightEmission() != 0) {
                  var23.addLight(var28);
               }
            }
         }
      }

      var20.setLightCorrect(var6);
      CompoundTag var21 = param3.getCompound("Heightmaps");
      EnumSet<Heightmap.Types> var22 = EnumSet.noneOf(Heightmap.Types.class);

      for (Heightmap.Types var24 : var20.getStatus().heightmapsAfter()) {
         String var25 = var24.getSerializationKey();
         if (var21.contains(var25, 12)) {
            var20.setHeightmap(var24, var21.getLongArray(var25));
         } else {
            var22.add(var24);
         }
      }

      Heightmap.primeHeightmaps(var20, var22);
      CompoundTag var23 = param3.getCompound("structures");
      var20.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(param0), var23, param0.getSeed()));
      var20.setAllReferences(unpackStructureReferences(param0.registryAccess(), param2, var23));
      if (param3.getBoolean("shouldSave")) {
         var20.setUnsaved(true);
      }

      ListTag var24 = param3.getList("PostProcessing", 9);

      for (int var25 = 0; var25 < var24.size(); var25++) {
         ListTag var26 = var24.getList(var25);

         for (int var27 = 0; var27 < var26.size(); var27++) {
            var20.addPackedPostProcess(var26.getShort(var27), var25);
         }
      }

      if (var18 == ChunkStatus.ChunkType.LEVELCHUNK) {
         return new ImposterProtoChunk((LevelChunk)var20, false);
      }

      ProtoChunk var25 = (ProtoChunk)var20;
      ListTag var26 = param3.getList("entities", 10);

      for (int var27 = 0; var27 < var26.size(); var27++) {
         var25.addEntity(var26.getCompound(var27));
      }

      ListTag var27 = param3.getList("block_entities", 10);

      for (int var28 = 0; var28 < var27.size(); var28++) {
         CompoundTag var29 = var27.getCompound(var28);
         var20.setBlockEntityNbt(var29);
      }

      ListTag var28 = param3.getList("Lights", 9);

      for (int var29 = 0; var29 < var28.size(); var29++) {
         LevelChunkSection var30 = var9[var29];
         if (var30 != null && !var30.hasOnlyAir()) {
            ListTag var31 = var28.getList(var29);

            for (int var32 = 0; var32 < var31.size(); var32++) {
               var25.addLight(var31.getShort(var32), var29);
            }
         }
      }

      CompoundTag var29 = param3.getCompound("CarvingMasks");

      for (String var31 : var29.getAllKeys()) {
         GenerationStep.Carving var32 = GenerationStep.Carving.valueOf(var31);
         var25.setCarvingMask(var32, new CarvingMask(var29.getLongArray(var31), var20.getMinBuildHeight()));
      }

      return var25;
   }

   private static void logErrors(ChunkPos param0, int param1, String param2) {
      LOGGER.error("Recoverable errors when loading section [" + param0.x + ", " + param1 + ", " + param0.z + "]: " + param2);
   }

   private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> param0) {
      return PalettedContainer.codecRO(
         param0.asHolderIdMap(), param0.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, param0.getHolderOrThrow(Biomes.PLAINS)
      );
   }

   public static CompoundTag write(ServerLevel param0, ChunkAccess param1) {
      ChunkPos var2 = param1.getPos();
      CompoundTag var3 = NbtUtils.addCurrentDataVersion(new CompoundTag());
      var3.putInt("xPos", var2.x);
      var3.putInt("yPos", param1.getMinSection());
      var3.putInt("zPos", var2.z);
      var3.putLong("LastUpdate", param0.getGameTime());
      var3.putLong("InhabitedTime", param1.getInhabitedTime());
      var3.putString("Status", param1.getStatus().getName());
      BlendingData var4 = param1.getBlendingData();
      if (var4 != null) {
         BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, var4).resultOrPartial(LOGGER::error).ifPresent(l_param1 -> var3.put("blending_data", l_param1));
      }

      BelowZeroRetrogen var5 = param1.getBelowZeroRetrogen();
      if (var5 != null) {
         BelowZeroRetrogen.CODEC
            .encodeStart(NbtOps.INSTANCE, var5)
            .resultOrPartial(LOGGER::error)
            .ifPresent(l_param1 -> var3.put("below_zero_retrogen", l_param1));
      }

      UpgradeData var6 = param1.getUpgradeData();
      if (!var6.isEmpty()) {
         var3.put("UpgradeData", var6.write());
      }

      LevelChunkSection[] var7 = param1.getSections();
      ListTag var8 = new ListTag();
      LevelLightEngine var9 = param0.getChunkSource().getLightEngine();
      Registry<Biome> var10 = param0.registryAccess().registryOrThrow(Registries.BIOME);
      Codec<PalettedContainerRO<Holder<Biome>>> var11 = makeBiomeCodec(var10);
      boolean var12 = param1.isLightCorrect();

      for (int var13 = var9.getMinLightSection(); var13 < var9.getMaxLightSection(); var13++) {
         int var14 = param1.getSectionIndexFromSectionY(var13);
         boolean var15 = var14 >= 0 && var14 < var7.length;
         DataLayer var16 = var9.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(var2, var13));
         DataLayer var17 = var9.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(var2, var13));
         if (var15 || var16 != null || var17 != null) {
            CompoundTag var18 = new CompoundTag();
            if (var15) {
               LevelChunkSection var19 = var7[var14];
               var18.put("block_states", BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, var19.getStates()).getOrThrow(false, LOGGER::error));
               var18.put("biomes", var11.encodeStart(NbtOps.INSTANCE, var19.getBiomes()).getOrThrow(false, LOGGER::error));
            }

            if (var16 != null && !var16.isEmpty()) {
               var18.putByteArray("BlockLight", var16.getData());
            }

            if (var17 != null && !var17.isEmpty()) {
               var18.putByteArray("SkyLight", var17.getData());
            }

            if (!var18.isEmpty()) {
               var18.putByte("Y", (byte)var13);
               var8.add(var18);
            }
         }
      }

      var3.put("sections", var8);
      if (var12) {
         var3.putBoolean("isLightOn", true);
      }

      ListTag var13 = new ListTag();

      for (BlockPos var15 : param1.getBlockEntitiesPos()) {
         CompoundTag var16 = param1.getBlockEntityNbtForSaving(var15);
         if (var16 != null) {
            var13.add(var16);
         }
      }

      var3.put("block_entities", var13);
      if (param1.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
         ProtoChunk var14 = (ProtoChunk)param1;
         ListTag var15 = new ListTag();
         var15.addAll(var14.getEntities());
         var3.put("entities", var15);
         var3.put("Lights", packOffsets(var14.getPackedLights()));
         CompoundTag var16 = new CompoundTag();

         for (GenerationStep.Carving var20 : GenerationStep.Carving.values()) {
            CarvingMask var21 = var14.getCarvingMask(var20);
            if (var21 != null) {
               var16.putLongArray(var20.toString(), var21.toArray());
            }
         }

         var3.put("CarvingMasks", var16);
      }

      saveTicks(param0, var3, param1.getTicksForSerialization());
      var3.put("PostProcessing", packOffsets(param1.getPostProcessing()));
      CompoundTag var14 = new CompoundTag();

      for (Entry<Heightmap.Types, Heightmap> var16 : param1.getHeightmaps()) {
         if (param1.getStatus().heightmapsAfter().contains(var16.getKey())) {
            var14.put(var16.getKey().getSerializationKey(), new LongArrayTag(var16.getValue().getRawData()));
         }
      }

      var3.put("Heightmaps", var14);
      var3.put("structures", packStructureData(StructurePieceSerializationContext.fromLevel(param0), var2, param1.getAllStarts(), param1.getAllReferences()));
      return var3;
   }

   private static void saveTicks(ServerLevel param0, CompoundTag param1, ChunkAccess.TicksToSave param2) {
      long var3 = param0.getLevelData().getGameTime();
      param1.put("block_ticks", param2.blocks().save(var3, l_param0 -> BuiltInRegistries.BLOCK.getKey(l_param0).toString()));
      param1.put("fluid_ticks", param2.fluids().save(var3, l_param0 -> BuiltInRegistries.FLUID.getKey(l_param0).toString()));
   }

   public static ChunkStatus.ChunkType getChunkTypeFromTag(@Nullable CompoundTag param0) {
      return param0 != null ? ChunkStatus.byName(param0.getString("Status")).getChunkType() : ChunkStatus.ChunkType.PROTOCHUNK;
   }

   @Nullable
   private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel param0, CompoundTag param1) {
      ListTag var2 = getListOfCompoundsOrNull(param1, "entities");
      ListTag var3 = getListOfCompoundsOrNull(param1, "block_entities");
      return var2 == null && var3 == null ? null : l_param3 -> {
         if (var2 != null) {
            param0.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(var2, param0));
         }

         if (var3 != null) {
            for (int l_var4 = 0; l_var4 < var3.size(); l_var4++) {
               CompoundTag l_var5 = var3.getCompound(l_var4);
               boolean l_var6 = l_var5.getBoolean("keepPacked");
               if (l_var6) {
                  l_param3.setBlockEntityNbt(l_var5);
               } else {
                  BlockPos l_var7 = BlockEntity.getPosFromTag(l_var5);
                  BlockEntity l_var8 = BlockEntity.loadStatic(l_var7, l_param3.getBlockState(l_var7), l_var5);
                  if (l_var8 != null) {
                     l_param3.setBlockEntity(l_var8);
                  }
               }
            }
         }
      };
   }

   @Nullable
   private static ListTag getListOfCompoundsOrNull(CompoundTag param0, String param1) {
      ListTag var2 = param0.getList(param1, 10);
      return var2.isEmpty() ? null : var2;
   }

   private static CompoundTag packStructureData(
      StructurePieceSerializationContext param0, ChunkPos param1, Map<Structure, StructureStart> param2, Map<Structure, LongSet> param3
   ) {
      CompoundTag var4 = new CompoundTag();
      CompoundTag var5 = new CompoundTag();
      Registry<Structure> var6 = param0.registryAccess().registryOrThrow(Registries.STRUCTURE);

      for (Entry<Structure, StructureStart> var8 : param2.entrySet()) {
         ResourceLocation var9 = var6.getKey(var8.getKey());
         var5.put(var9.toString(), var8.getValue().createTag(param0, param1));
      }

      var4.put("starts", var5);
      CompoundTag var7 = new CompoundTag();

      for (Entry<Structure, LongSet> var9 : param3.entrySet()) {
         if (!var9.getValue().isEmpty()) {
            ResourceLocation var10 = var6.getKey(var9.getKey());
            var7.put(var10.toString(), new LongArrayTag(var9.getValue()));
         }
      }

      var4.put("References", var7);
      return var4;
   }

   private static Map<Structure, StructureStart> unpackStructureStart(StructurePieceSerializationContext param0, CompoundTag param1, long param2) {
      Map<Structure, StructureStart> var4 = Maps.newHashMap();
      Registry<Structure> var5 = param0.registryAccess().registryOrThrow(Registries.STRUCTURE);
      CompoundTag var6 = param1.getCompound("starts");

      for (String var8 : var6.getAllKeys()) {
         ResourceLocation var9 = ResourceLocation.tryParse(var8);
         Structure var10 = var5.get(var9);
         if (var10 == null) {
            LOGGER.error("Unknown structure start: {}", var9);
         } else {
            StructureStart var11 = StructureStart.loadStaticStart(param0, var6.getCompound(var8), param2);
            if (var11 != null) {
               var4.put(var10, var11);
            }
         }
      }

      return var4;
   }

   private static Map<Structure, LongSet> unpackStructureReferences(RegistryAccess param0, ChunkPos param1, CompoundTag param2) {
      Map<Structure, LongSet> var3 = Maps.newHashMap();
      Registry<Structure> var4 = param0.registryOrThrow(Registries.STRUCTURE);
      CompoundTag var5 = param2.getCompound("References");

      for (String var7 : var5.getAllKeys()) {
         ResourceLocation var8 = ResourceLocation.tryParse(var7);
         Structure var9 = var4.get(var8);
         if (var9 == null) {
            LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", var8, param1);
         } else {
            long[] var10 = var5.getLongArray(var7);
            if (var10.length != 0) {
               var3.put(var9, new LongOpenHashSet(Arrays.stream(var10).filter(l_param2 -> {
                  ChunkPos l_var4 = new ChunkPos(l_param2);
                  if (l_var4.getChessboardDistance(param1) > 8) {
                     LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", var8, l_var4, param1);
                     return false;
                  } else {
                     return true;
                  }
               }).toArray()));
            }
         }
      }

      return var3;
   }

   public static ListTag packOffsets(ShortList[] param0) {
      ListTag var1 = new ListTag();

      for (ShortList var5 : param0) {
         ListTag var6 = new ListTag();
         if (var5 != null) {
            for (Short var8 : var5) {
               var6.add(ShortTag.valueOf(var8));
            }
         }

         var1.add(var6);
      }

      return var1;
   }
}
