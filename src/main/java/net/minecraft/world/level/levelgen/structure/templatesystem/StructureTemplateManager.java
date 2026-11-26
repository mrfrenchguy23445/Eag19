package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureTemplateManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String STRUCTURE_DIRECTORY_NAME = "structures";
   private static final String TEST_STRUCTURES_DIR = "gameteststructures";
   private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
   private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
   private final Map<ResourceLocation, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
   private final DataFixer fixerUpper;
   private ResourceManager resourceManager;
   private final Path generatedDir;
   private final List<StructureTemplateManager.Source> sources;
   private final HolderGetter<Block> blockLookup;
   private static final FileToIdConverter LISTER = new FileToIdConverter("structures", ".nbt");

   public StructureTemplateManager(ResourceManager param0, LevelStorageSource.LevelStorageAccess param1, DataFixer param2, HolderGetter<Block> param3) {
      this.resourceManager = param0;
      this.fixerUpper = param2;
      this.generatedDir = param1.getLevelPath(LevelResource.GENERATED_DIR).normalize();
      this.blockLookup = param3;
      Builder<StructureTemplateManager.Source> var5 = ImmutableList.builder();
      var5.add(new StructureTemplateManager.Source(this::loadFromGenerated, this::listGenerated));
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         var5.add(new StructureTemplateManager.Source(this::loadFromTestStructures, this::listTestStructures));
      }

      var5.add(new StructureTemplateManager.Source(this::loadFromResource, this::listResources));
      this.sources = var5.build();
   }

   public StructureTemplate getOrCreate(ResourceLocation param0) {
      Optional<StructureTemplate> var2 = this.get(param0);
      if (var2.isPresent()) {
         return var2.get();
      }

      StructureTemplate var3 = new StructureTemplate();
      this.structureRepository.put(param0, Optional.of(var3));
      return var3;
   }

   public Optional<StructureTemplate> get(ResourceLocation param0) {
      return this.structureRepository.computeIfAbsent(param0, this::tryLoad);
   }

   public Stream<ResourceLocation> listTemplates() {
      return this.sources.stream().flatMap(l_param0 -> l_param0.lister().get()).distinct();
   }

   private Optional<StructureTemplate> tryLoad(ResourceLocation param0) {
      for (StructureTemplateManager.Source var3 : this.sources) {
         try {
            Optional<StructureTemplate> var4 = var3.loader().apply(param0);
            if (var4.isPresent()) {
               return var4;
            }
         } catch (Exception var5) {
         }
      }

      return Optional.empty();
   }

   public void onResourceManagerReload(ResourceManager param0) {
      this.resourceManager = param0;
      this.structureRepository.clear();
   }

   private Optional<StructureTemplate> loadFromResource(ResourceLocation param0) {
      ResourceLocation var2 = LISTER.idToFile(param0);
      return this.load(() -> this.resourceManager.open(var2), l_param1 -> LOGGER.error("Couldn't load structure {}", param0, l_param1));
   }

   private Stream<ResourceLocation> listResources() {
      return LISTER.listMatchingResources(this.resourceManager).keySet().stream().map(LISTER::fileToId);
   }

   private Optional<StructureTemplate> loadFromTestStructures(ResourceLocation param0) {
      return this.loadFromSnbt(param0, Paths.get("gameteststructures"));
   }

   private Stream<ResourceLocation> listTestStructures() {
      return this.listFolderContents(Paths.get("gameteststructures"), "minecraft", ".snbt");
   }

   private Optional<StructureTemplate> loadFromGenerated(ResourceLocation param0) {
      if (!Files.isDirectory(this.generatedDir)) {
         return Optional.empty();
      }

      Path var2 = createAndValidatePathToStructure(this.generatedDir, param0, ".nbt");
      return this.load(() -> new FileInputStream(var2.toFile()), l_param1 -> LOGGER.error("Couldn't load structure from {}", var2, l_param1));
   }

   private Stream<ResourceLocation> listGenerated() {
      if (!Files.isDirectory(this.generatedDir)) {
         return Stream.empty();
      }

      try {
         return Files.list(this.generatedDir).filter(l_param0 -> Files.isDirectory(l_param0)).flatMap(l_param0 -> this.listGeneratedInNamespace(l_param0));
      } catch (IOException var1) {
         return Stream.empty();
      }
   }

   private Stream<ResourceLocation> listGeneratedInNamespace(Path param0) {
      Path var2 = param0.resolve("structures");
      return this.listFolderContents(var2, param0.getFileName().toString(), ".nbt");
   }

   private Stream<ResourceLocation> listFolderContents(Path param0, String param1, String param2) {
      if (!Files.isDirectory(param0)) {
         return Stream.empty();
      }

      int var4 = param2.length();
      Function<String, String> var5 = l_param1 -> l_param1.substring(0, l_param1.length() - var4);

      try {
         return Files.walk(param0).filter(l_param1 -> l_param1.toString().endsWith(param2)).mapMulti((l_param3, l_param4) -> {
            try {
               l_param4.accept(new ResourceLocation(param1, var5.apply(this.relativize(param0, l_param3))));
            } catch (ResourceLocationException l_var6) {
               LOGGER.error("Invalid location while listing pack contents", l_var6);
            }
         });
      } catch (IOException var6) {
         LOGGER.error("Failed to list folder contents", var6);
         return Stream.empty();
      }
   }

   private String relativize(Path param0, Path param1) {
      return param0.relativize(param1).toString().replace(File.separator, "/");
   }

   private Optional<StructureTemplate> loadFromSnbt(ResourceLocation param0, Path param1) {
      if (!Files.isDirectory(param1)) {
         return Optional.empty();
      }

      Path var3 = FileUtil.createPathToResource(param1, param0.getPath(), ".snbt");

      try (BufferedReader var4 = Files.newBufferedReader(var3)) {
         String var5 = IOUtils.toString(var4);
         return Optional.of(this.readStructure(NbtUtils.snbtToStructure(var5)));
      } catch (NoSuchFileException var4) {
         return Optional.empty();
      } catch (IOException | CommandSyntaxException var4) {
         LOGGER.error("Couldn't load structure from {}", var3, var4);
         return Optional.empty();
      }
   }

   private Optional<StructureTemplate> load(StructureTemplateManager.InputStreamOpener param0, Consumer<Throwable> param1) {
      try (InputStream var3 = param0.open()) {
         return Optional.of(this.readStructure(var3));
      } catch (FileNotFoundException var3) {
         return Optional.empty();
      } catch (Throwable var3) {
         param1.accept(var3);
         return Optional.empty();
      }
   }

   private StructureTemplate readStructure(InputStream param0) throws IOException {
      CompoundTag var2 = NbtIo.readCompressed(param0);
      return this.readStructure(var2);
   }

   public StructureTemplate readStructure(CompoundTag param0) {
      StructureTemplate var2 = new StructureTemplate();
      int var3 = NbtUtils.getDataVersion(param0, 500);
      var2.load(this.blockLookup, DataFixTypes.STRUCTURE.updateToCurrentVersion(this.fixerUpper, param0, var3));
      return var2;
   }

   public boolean save(ResourceLocation param0) {
      Optional<StructureTemplate> var2 = this.structureRepository.get(param0);
      if (!var2.isPresent()) {
         return false;
      }

      StructureTemplate var3 = var2.get();
      Path var4 = createAndValidatePathToStructure(this.generatedDir, param0, ".nbt");
      Path var5 = var4.getParent();
      if (var5 == null) {
         return false;
      }

      try {
         Files.createDirectories(Files.exists(var5) ? var5.toRealPath() : var5);
      } catch (IOException var6) {
         LOGGER.error("Failed to create parent directory: {}", var5);
         return false;
      }

      CompoundTag var6 = var3.save(new CompoundTag());

      try (OutputStream var7 = new FileOutputStream(var4.toFile())) {
         NbtIo.writeCompressed(var6, var7);
         return true;
      } catch (Throwable var7) {
         return false;
      }
   }

   public Path getPathToGeneratedStructure(ResourceLocation param0, String param1) {
      return createPathToStructure(this.generatedDir, param0, param1);
   }

   public static Path createPathToStructure(Path param0, ResourceLocation param1, String param2) {
      try {
         Path var3 = param0.resolve(param1.getNamespace());
         Path var4 = var3.resolve("structures");
         return FileUtil.createPathToResource(var4, param1.getPath(), param2);
      } catch (InvalidPathException var3) {
         throw new ResourceLocationException("Invalid resource path: " + param1, var3);
      }
   }

   private static Path createAndValidatePathToStructure(Path param0, ResourceLocation param1, String param2) {
      if (param1.getPath().contains("//")) {
         throw new ResourceLocationException("Invalid resource path: " + param1);
      } else {
         Path var3 = createPathToStructure(param0, param1, param2);
         if (var3.startsWith(param0) && FileUtil.isPathNormalized(var3) && FileUtil.isPathPortable(var3)) {
            return var3;
         } else {
            throw new ResourceLocationException("Invalid resource path: " + var3);
         }
      }
   }

   public void remove(ResourceLocation param0) {
      this.structureRepository.remove(param0);
   }

   @FunctionalInterface
   interface InputStreamOpener {
      InputStream open() throws IOException;
   }

   record Source(Function<ResourceLocation, Optional<StructureTemplate>> loader, Supplier<Stream<ResourceLocation>> lister) {
   }
}
