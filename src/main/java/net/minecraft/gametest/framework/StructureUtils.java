package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
   public static String testStructuresDir = "gameteststructures";
   private static final int HOW_MANY_CHUNKS_TO_LOAD_IN_EACH_DIRECTION_OF_STRUCTURE = 4;

   public static Rotation getRotationForRotationSteps(int param0) {
      switch (param0) {
         case 0:
            return Rotation.NONE;
         case 1:
            return Rotation.CLOCKWISE_90;
         case 2:
            return Rotation.CLOCKWISE_180;
         case 3:
            return Rotation.COUNTERCLOCKWISE_90;
         default:
            throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + param0);
      }
   }

   public static int getRotationStepsForRotation(Rotation param0) {
      switch (param0) {
         case NONE:
            return 0;
         case CLOCKWISE_90:
            return 1;
         case CLOCKWISE_180:
            return 2;
         case COUNTERCLOCKWISE_90:
            return 3;
         default:
            throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + param0);
      }
   }

   public static void main(String[] param0) throws IOException {
      Bootstrap.bootStrap();
      Files.walk(Paths.get(testStructuresDir)).filter(l_param0 -> l_param0.toString().endsWith(".snbt")).forEach(l_param0 -> {
         try {
            String l_var1 = Files.readString(l_param0);
            CompoundTag l_var2 = NbtUtils.snbtToStructure(l_var1);
            CompoundTag l_var3 = StructureUpdater.update(l_param0.toString(), l_var2);
            NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, l_param0, NbtUtils.structureToSnbt(l_var3));
         } catch (CommandSyntaxException | IOException l_var1) {
            LOGGER.error("Something went wrong upgrading: {}", l_param0, l_var1);
         }
      });
   }

   public static AABB getStructureBounds(StructureBlockEntity param0) {
      BlockPos var1 = param0.getBlockPos();
      BlockPos var2 = var1.offset(param0.getStructureSize().offset(-1, -1, -1));
      BlockPos var3 = StructureTemplate.transform(var2, Mirror.NONE, param0.getRotation(), var1);
      return new AABB(var1, var3);
   }

   public static BoundingBox getStructureBoundingBox(StructureBlockEntity param0) {
      BlockPos var1 = param0.getBlockPos();
      BlockPos var2 = var1.offset(param0.getStructureSize().offset(-1, -1, -1));
      BlockPos var3 = StructureTemplate.transform(var2, Mirror.NONE, param0.getRotation(), var1);
      return BoundingBox.fromCorners(var1, var3);
   }

   public static void addCommandBlockAndButtonToStartTest(BlockPos param0, BlockPos param1, Rotation param2, ServerLevel param3) {
      BlockPos var4 = StructureTemplate.transform(param0.offset(param1), Mirror.NONE, param2, param0);
      param3.setBlockAndUpdate(var4, Blocks.COMMAND_BLOCK.defaultBlockState());
      CommandBlockEntity var5 = (CommandBlockEntity)param3.getBlockEntity(var4);
      var5.getCommandBlock().setCommand("test runthis");
      BlockPos var6 = StructureTemplate.transform(var4.offset(0, 0, -1), Mirror.NONE, param2, var4);
      param3.setBlockAndUpdate(var6, Blocks.STONE_BUTTON.defaultBlockState().rotate(param2));
   }

   public static void createNewEmptyStructureBlock(String param0, BlockPos param1, Vec3i param2, Rotation param3, ServerLevel param4) {
      BoundingBox var5 = getStructureBoundingBox(param1, param2, param3);
      clearSpaceForStructure(var5, param1.getY(), param4);
      param4.setBlockAndUpdate(param1, Blocks.STRUCTURE_BLOCK.defaultBlockState());
      StructureBlockEntity var6 = (StructureBlockEntity)param4.getBlockEntity(param1);
      var6.setIgnoreEntities(false);
      var6.setStructureName(new ResourceLocation(param0));
      var6.setStructureSize(param2);
      var6.setMode(StructureMode.SAVE);
      var6.setShowBoundingBox(true);
   }

   public static StructureBlockEntity spawnStructure(String param0, BlockPos param1, Rotation param2, int param3, ServerLevel param4, boolean param5) {
      Vec3i var6 = getStructureTemplate(param0, param4).getSize();
      BoundingBox var7 = getStructureBoundingBox(param1, var6, param2);
      BlockPos var8;
      if (param2 == Rotation.NONE) {
         var8 = param1;
      } else if (param2 == Rotation.CLOCKWISE_90) {
         var8 = param1.offset(var6.getZ() - 1, 0, 0);
      } else if (param2 == Rotation.CLOCKWISE_180) {
         var8 = param1.offset(var6.getX() - 1, 0, var6.getZ() - 1);
      } else {
         if (param2 != Rotation.COUNTERCLOCKWISE_90) {
            throw new IllegalArgumentException("Invalid rotation: " + param2);
         }

         var8 = param1.offset(0, 0, var6.getX() - 1);
      }

      forceLoadChunks(param1, param4);
      clearSpaceForStructure(var7, param1.getY(), param4);
      StructureBlockEntity var9 = createStructureBlock(param0, var8, param2, param4, param5);
      param4.getBlockTicks().clearArea(var7);
      param4.clearBlockEvents(var7);
      return var9;
   }

   private static void forceLoadChunks(BlockPos param0, ServerLevel param1) {
      ChunkPos var2 = new ChunkPos(param0);

      for (int var3 = -1; var3 < 4; var3++) {
         for (int var4 = -1; var4 < 4; var4++) {
            int var5 = var2.x + var3;
            int var6 = var2.z + var4;
            param1.setChunkForced(var5, var6, true);
         }
      }
   }

   public static void clearSpaceForStructure(BoundingBox param0, int param1, ServerLevel param2) {
      BoundingBox var3 = new BoundingBox(param0.minX() - 2, param0.minY() - 3, param0.minZ() - 3, param0.maxX() + 3, param0.maxY() + 20, param0.maxZ() + 3);
      BlockPos.betweenClosedStream(var3).forEach(l_param2 -> clearBlock(param1, l_param2, param2));
      param2.getBlockTicks().clearArea(var3);
      param2.clearBlockEvents(var3);
      AABB var4 = new AABB(var3.minX(), var3.minY(), var3.minZ(), var3.maxX(), var3.maxY(), var3.maxZ());
      List<Entity> var5 = param2.getEntitiesOfClass(Entity.class, var4, l_param0 -> !(l_param0 instanceof Player));
      var5.forEach(Entity::discard);
   }

   public static BoundingBox getStructureBoundingBox(BlockPos param0, Vec3i param1, Rotation param2) {
      BlockPos var3 = param0.offset(param1).offset(-1, -1, -1);
      BlockPos var4 = StructureTemplate.transform(var3, Mirror.NONE, param2, param0);
      BoundingBox var5 = BoundingBox.fromCorners(param0, var4);
      int var6 = Math.min(var5.minX(), var5.maxX());
      int var7 = Math.min(var5.minZ(), var5.maxZ());
      return var5.move(param0.getX() - var6, 0, param0.getZ() - var7);
   }

   public static Optional<BlockPos> findStructureBlockContainingPos(BlockPos param0, int param1, ServerLevel param2) {
      return findStructureBlocks(param0, param1, param2).stream().filter(l_param2 -> doesStructureContain(l_param2, param0, param2)).findFirst();
   }

   @Nullable
   public static BlockPos findNearestStructureBlock(BlockPos param0, int param1, ServerLevel param2) {
      Comparator<BlockPos> var3 = Comparator.comparingInt(l_param1 -> l_param1.distManhattan(param0));
      Collection<BlockPos> var4 = findStructureBlocks(param0, param1, param2);
      Optional<BlockPos> var5 = var4.stream().min(var3);
      return var5.orElse(null);
   }

   public static Collection<BlockPos> findStructureBlocks(BlockPos param0, int param1, ServerLevel param2) {
      Collection<BlockPos> var3 = Lists.newArrayList();
      AABB var4 = new AABB(param0);
      var4 = var4.inflate(param1);

      for (int var5 = (int)var4.minX; var5 <= (int)var4.maxX; var5++) {
         for (int var6 = (int)var4.minY; var6 <= (int)var4.maxY; var6++) {
            for (int var7 = (int)var4.minZ; var7 <= (int)var4.maxZ; var7++) {
               BlockPos var8 = new BlockPos(var5, var6, var7);
               BlockState var9 = param2.getBlockState(var8);
               if (var9.is(Blocks.STRUCTURE_BLOCK)) {
                  var3.add(var8);
               }
            }
         }
      }

      return var3;
   }

   private static StructureTemplate getStructureTemplate(String param0, ServerLevel param1) {
      StructureTemplateManager var2 = param1.getStructureManager();
      Optional<StructureTemplate> var3 = var2.get(new ResourceLocation(param0));
      if (var3.isPresent()) {
         return var3.get();
      } else {
         String var4 = param0 + ".snbt";
         Path var5 = Paths.get(testStructuresDir, var4);
         CompoundTag var6 = tryLoadStructure(var5);
         if (var6 == null) {
            throw new RuntimeException("Could not find structure file " + var5 + ", and the structure is not available in the world structures either.");
         } else {
            return var2.readStructure(var6);
         }
      }
   }

   private static StructureBlockEntity createStructureBlock(String param0, BlockPos param1, Rotation param2, ServerLevel param3, boolean param4) {
      param3.setBlockAndUpdate(param1, Blocks.STRUCTURE_BLOCK.defaultBlockState());
      StructureBlockEntity var5 = (StructureBlockEntity)param3.getBlockEntity(param1);
      var5.setMode(StructureMode.LOAD);
      var5.setRotation(param2);
      var5.setIgnoreEntities(false);
      var5.setStructureName(new ResourceLocation(param0));
      var5.loadStructure(param3, param4);
      if (var5.getStructureSize() != Vec3i.ZERO) {
         return var5;
      } else {
         StructureTemplate var6 = getStructureTemplate(param0, param3);
         var5.loadStructure(param3, param4, var6);
         if (var5.getStructureSize() == Vec3i.ZERO) {
            throw new RuntimeException("Failed to load structure " + param0);
         } else {
            return var5;
         }
      }
   }

   @Nullable
   private static CompoundTag tryLoadStructure(Path param0) {
      try {
         BufferedReader var1 = Files.newBufferedReader(param0);
         String var2 = IOUtils.toString(var1);
         return NbtUtils.snbtToStructure(var2);
      } catch (IOException var1) {
         return null;
      } catch (CommandSyntaxException var1) {
         throw new RuntimeException("Error while trying to load structure " + param0, var1);
      }
   }

   private static void clearBlock(int param0, BlockPos param1, ServerLevel param2) {
      BlockState var3 = null;
      RegistryAccess var4 = param2.registryAccess();
      FlatLevelGeneratorSettings var5 = FlatLevelGeneratorSettings.getDefault(
         var4.lookupOrThrow(Registries.BIOME), var4.lookupOrThrow(Registries.STRUCTURE_SET), var4.lookupOrThrow(Registries.PLACED_FEATURE)
      );
      List<BlockState> var6 = var5.getLayers();
      int var7 = param1.getY() - param2.getMinBuildHeight();
      if (param1.getY() < param0 && var7 > 0 && var7 <= var6.size()) {
         var3 = var6.get(var7 - 1);
      }

      if (var3 == null) {
         var3 = Blocks.AIR.defaultBlockState();
      }

      BlockInput var8 = new BlockInput(var3, Collections.emptySet(), null);
      var8.place(param2, param1, 2);
      param2.blockUpdated(param1, var3.getBlock());
   }

   private static boolean doesStructureContain(BlockPos param0, BlockPos param1, ServerLevel param2) {
      StructureBlockEntity var3 = (StructureBlockEntity)param2.getBlockEntity(param0);
      AABB var4 = getStructureBounds(var3).inflate(1.0);
      return var4.contains(Vec3.atCenterOf(param1));
   }
}
