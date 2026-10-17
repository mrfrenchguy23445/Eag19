package net.minecraft.world.level.levelgen.structure;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.slf4j.Logger;

public final class StructureStart {
   public static final String INVALID_START_ID = "INVALID";
   public static final StructureStart INVALID_START = new StructureStart(null, new ChunkPos(0, 0), 0, new PiecesContainer(List.of()));
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Structure structure;
   private final PiecesContainer pieceContainer;
   private final ChunkPos chunkPos;
   private int references;
   @Nullable
   private volatile BoundingBox cachedBoundingBox;

   public StructureStart(Structure param0, ChunkPos param1, int param2, PiecesContainer param3) {
      this.structure = param0;
      this.chunkPos = param1;
      this.references = param2;
      this.pieceContainer = param3;
   }

   @Nullable
   public static StructureStart loadStaticStart(StructurePieceSerializationContext param0, CompoundTag param1, long param2) {
      String var4 = param1.getString("id");
      if ("INVALID".equals(var4)) {
         return INVALID_START;
      }

      Registry<Structure> var5 = param0.registryAccess().registryOrThrow(Registries.STRUCTURE);
      Structure var6 = var5.get(new ResourceLocation(var4));
      if (var6 == null) {
         LOGGER.error("Unknown stucture id: {}", var4);
         return null;
      }

      ChunkPos var7 = new ChunkPos(param1.getInt("ChunkX"), param1.getInt("ChunkZ"));
      int var8 = param1.getInt("references");
      ListTag var9 = param1.getList("Children", 10);

      try {
         PiecesContainer var10 = PiecesContainer.load(var9, param0);
         if (var6 instanceof OceanMonumentStructure) {
            var10 = OceanMonumentStructure.regeneratePiecesAfterLoad(var7, param2, var10);
         }

         return new StructureStart(var6, var7, var8, var10);
      } catch (Exception var10) {
         LOGGER.error("Failed Start with id {}", var4, var10);
         return null;
      }
   }

   public BoundingBox getBoundingBox() {
      BoundingBox var1 = this.cachedBoundingBox;
      if (var1 == null) {
         var1 = this.structure.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
         this.cachedBoundingBox = var1;
      }

      return var1;
   }

   public void placeInChunk(WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5) {
      List<StructurePiece> var7 = this.pieceContainer.pieces();
      if (!var7.isEmpty()) {
         BoundingBox var8 = var7.get(0).boundingBox;
         BlockPos var9 = var8.getCenter();
         BlockPos var10 = new BlockPos(var9.getX(), var8.minY(), var9.getZ());

         for (StructurePiece var12 : var7) {
            if (var12.getBoundingBox().intersects(param4)) {
               var12.postProcess(param0, param1, param2, param3, param4, param5, var10);
            }
         }

         this.structure.afterPlace(param0, param1, param2, param3, param4, param5, this.pieceContainer);
      }
   }

   public CompoundTag createTag(StructurePieceSerializationContext param0, ChunkPos param1) {
      CompoundTag var3 = new CompoundTag();
      if (this.isValid()) {
         var3.putString("id", param0.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(this.structure).toString());
         var3.putInt("ChunkX", param1.x);
         var3.putInt("ChunkZ", param1.z);
         var3.putInt("references", this.references);
         var3.put("Children", this.pieceContainer.save(param0));
         return var3;
      } else {
         var3.putString("id", "INVALID");
         return var3;
      }
   }

   public boolean isValid() {
      return !this.pieceContainer.isEmpty();
   }

   public ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   public boolean canBeReferenced() {
      return this.references < this.getMaxReferences();
   }

   public void addReference() {
      this.references++;
   }

   public int getReferences() {
      return this.references;
   }

   protected int getMaxReferences() {
      return 1;
   }

   public Structure getStructure() {
      return this.structure;
   }

   public List<StructurePiece> getPieces() {
      return this.pieceContainer.pieces();
   }
}
