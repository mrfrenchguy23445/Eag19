package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureManager {
   private final LevelAccessor level;
   private final WorldOptions worldOptions;
   private final StructureCheck structureCheck;

   public StructureManager(LevelAccessor param0, WorldOptions param1, StructureCheck param2) {
      this.level = param0;
      this.worldOptions = param1;
      this.structureCheck = param2;
   }

   public StructureManager forWorldGenRegion(WorldGenRegion param0) {
      if (param0.getLevel() != this.level) {
         throw new IllegalStateException("Using invalid structure manager (source level: " + param0.getLevel() + ", region: " + param0);
      } else {
         return new StructureManager(param0, this.worldOptions, this.structureCheck);
      }
   }

   public List<StructureStart> startsForStructure(ChunkPos param0, Predicate<Structure> param1) {
      Map<Structure, LongSet> var3 = this.level.getChunk(param0.x, param0.z, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
      Builder<StructureStart> var4 = ImmutableList.builder();

      for (Entry<Structure, LongSet> var6 : var3.entrySet()) {
         Structure var7 = var6.getKey();
         if (param1.test(var7)) {
            this.fillStartsForStructure(var7, var6.getValue(), var4::add);
         }
      }

      return var4.build();
   }

   public List<StructureStart> startsForStructure(SectionPos param0, Structure param1) {
      LongSet var3 = this.level.getChunk(param0.x(), param0.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(param1);
      Builder<StructureStart> var4 = ImmutableList.builder();
      this.fillStartsForStructure(param1, var3, var4::add);
      return var4.build();
   }

   public void fillStartsForStructure(Structure param0, LongSet param1, Consumer<StructureStart> param2) {
      for (long var5 : param1) {
         SectionPos var7 = SectionPos.of(new ChunkPos(var5), this.level.getMinSection());
         StructureStart var8 = this.getStartForStructure(var7, param0, this.level.getChunk(var7.x(), var7.z(), ChunkStatus.STRUCTURE_STARTS));
         if (var8 != null && var8.isValid()) {
            param2.accept(var8);
         }
      }
   }

   @Nullable
   public StructureStart getStartForStructure(SectionPos param0, Structure param1, StructureAccess param2) {
      return param2.getStartForStructure(param1);
   }

   public void setStartForStructure(SectionPos param0, Structure param1, StructureStart param2, StructureAccess param3) {
      param3.setStartForStructure(param1, param2);
   }

   public void addReferenceForStructure(SectionPos param0, Structure param1, long param2, StructureAccess param4) {
      param4.addReferenceForStructure(param1, param2);
   }

   public boolean shouldGenerateStructures() {
      return this.worldOptions.generateStructures();
   }

   public StructureStart getStructureAt(BlockPos param0, Structure param1) {
      for (StructureStart var4 : this.startsForStructure(SectionPos.of(param0), param1)) {
         if (var4.getBoundingBox().isInside(param0)) {
            return var4;
         }
      }

      return StructureStart.INVALID_START;
   }

   public StructureStart getStructureWithPieceAt(BlockPos param0, ResourceKey<Structure> param1) {
      Structure var3 = this.registryAccess().registryOrThrow(Registries.STRUCTURE).get(param1);
      return var3 == null ? StructureStart.INVALID_START : this.getStructureWithPieceAt(param0, var3);
   }

   public StructureStart getStructureWithPieceAt(BlockPos param0, TagKey<Structure> param1) {
      Registry<Structure> var3 = this.registryAccess().registryOrThrow(Registries.STRUCTURE);

      for (StructureStart var5 : this.startsForStructure(
         new ChunkPos(param0), l_param2 -> var3.getHolder(var3.getId(l_param2)).map(l2_param1 -> l2_param1.is(param1)).orElse(false)
      )) {
         if (this.structureHasPieceAt(param0, var5)) {
            return var5;
         }
      }

      return StructureStart.INVALID_START;
   }

   public StructureStart getStructureWithPieceAt(BlockPos param0, Structure param1) {
      for (StructureStart var4 : this.startsForStructure(SectionPos.of(param0), param1)) {
         if (this.structureHasPieceAt(param0, var4)) {
            return var4;
         }
      }

      return StructureStart.INVALID_START;
   }

   public boolean structureHasPieceAt(BlockPos param0, StructureStart param1) {
      for (StructurePiece var4 : param1.getPieces()) {
         if (var4.getBoundingBox().isInside(param0)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasAnyStructureAt(BlockPos param0) {
      SectionPos var2 = SectionPos.of(param0);
      return this.level.getChunk(var2.x(), var2.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
   }

   public Map<Structure, LongSet> getAllStructuresAt(BlockPos param0) {
      SectionPos var2 = SectionPos.of(param0);
      return this.level.getChunk(var2.x(), var2.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
   }

   public StructureCheckResult checkStructurePresence(ChunkPos param0, Structure param1, boolean param2) {
      return this.structureCheck.checkStart(param0, param1, param2);
   }

   public void addReference(StructureStart param0) {
      param0.addReference();
      this.structureCheck.incrementReference(param0.getChunkPos(), param0.getStructure());
   }

   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }
}
