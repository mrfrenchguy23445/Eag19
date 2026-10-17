package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.slf4j.Logger;

public class LevelStorageSource {
   static final Logger LOGGER = LogUtils.getLogger();
   static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
      .appendLiteral('-')
      .appendValue(ChronoField.MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.DAY_OF_MONTH, 2)
      .appendLiteral('_')
      .appendValue(ChronoField.HOUR_OF_DAY, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
      .toFormatter();
   private static final ImmutableList<String> OLD_SETTINGS_KEYS = ImmutableList.of(
      "RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest"
   );
   private static final String TAG_DATA = "Data";
   final Path baseDir;
   private final Path backupDir;
   final DataFixer fixerUpper;

   public LevelStorageSource(Path param0, Path param1, DataFixer param2) {
      this.fixerUpper = param2;

      try {
         FileUtil.createDirectoriesSafe(param0);
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }

      this.baseDir = param0;
      this.backupDir = param1;
   }

   public static LevelStorageSource createDefault(Path param0) {
      return new LevelStorageSource(param0, param0.resolve("../backups"), DataFixers.getDataFixer());
   }

   private static <T> DataResult<WorldGenSettings> readWorldGenSettings(Dynamic<T> param0, DataFixer param1, int param2) {
      Dynamic<T> var3 = param0.get("WorldGenSettings").orElseEmptyMap();

      for (String var5 : OLD_SETTINGS_KEYS) {
         Optional<Dynamic<T>> var6 = param0.get(var5).result();
         if (var6.isPresent()) {
            var3 = var3.set(var5, var6.get());
         }
      }

      Dynamic<T> var4 = DataFixTypes.WORLD_GEN_SETTINGS.updateToCurrentVersion(param1, var3, param2);
      return WorldGenSettings.CODEC.parse(var4);
   }

   private static WorldDataConfiguration readDataConfig(Dynamic<?> param0) {
      return WorldDataConfiguration.CODEC.parse(param0).resultOrPartial(LOGGER::error).orElse(WorldDataConfiguration.DEFAULT);
   }

   public String getName() {
      return "Anvil";
   }

   public LevelStorageSource.LevelCandidates findLevelCandidates() throws LevelStorageException {
      if (!Files.isDirectory(this.baseDir)) {
         throw new LevelStorageException(Component.translatable("selectWorld.load_folder_access"));
      }

      try {
         List<LevelStorageSource.LevelDirectory> var1 = Files.list(this.baseDir)
            .filter(l_param0 -> Files.isDirectory(l_param0))
            .map(LevelStorageSource.LevelDirectory::new)
            .filter(l_param0 -> Files.isRegularFile(l_param0.dataFile()) || Files.isRegularFile(l_param0.oldDataFile()))
            .toList();
         return new LevelStorageSource.LevelCandidates(var1);
      } catch (IOException var1) {
         throw new LevelStorageException(Component.translatable("selectWorld.load_folder_access"));
      }
   }

   public CompletableFuture<List<LevelSummary>> loadLevelSummaries(LevelStorageSource.LevelCandidates param0) {
      List<CompletableFuture<LevelSummary>> var2 = new ArrayList<>(param0.levels.size());

      for (LevelStorageSource.LevelDirectory var4 : param0.levels) {
         var2.add(
            CompletableFuture.supplyAsync(
               () -> {
                  boolean l_var2;
                  try {
                     l_var2 = DirectoryLock.isLocked(var4.path());
                  } catch (Exception l_var3) {
                     LOGGER.warn("Failed to read {} lock", var4.path(), l_var3);
                     return null;
                  }

                  try {
                     LevelSummary l_var3 = this.readLevelData(var4, this.levelSummaryReader(var4, l_var2));
                     return l_var3 != null ? l_var3 : null;
                  } catch (OutOfMemoryError l_var3) {
                     MemoryReserve.release();
                     System.gc();
                     LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of memory trying to read summary of {}", var4.directoryName());
                     throw l_var3;
                  } catch (StackOverflowError l_var3) {
                     LOGGER.error(
                        LogUtils.FATAL_MARKER,
                        "Ran out of stack trying to read summary of {}. Assuming corruption; attempting to restore from from level.dat_old.",
                        var4.directoryName()
                     );
                     Util.safeReplaceOrMoveFile(var4.dataFile(), var4.oldDataFile(), var4.corruptedDataFile(LocalDateTime.now()), true);
                     throw l_var3;
                  }
               },
               Util.backgroundExecutor()
            )
         );
      }

      return Util.sequenceFailFastAndCancel(var2).thenApply(l_param0 -> l_param0.stream().filter(Objects::nonNull).sorted().toList());
   }

   private int getStorageVersion() {
      return 19133;
   }

   @Nullable
   <T> T readLevelData(LevelStorageSource.LevelDirectory param0, BiFunction<Path, DataFixer, T> param1) {
      if (!Files.exists(param0.path())) {
         return null;
      }

      Path var3 = param0.dataFile();
      if (Files.exists(var3)) {
         T var4 = param1.apply(var3, this.fixerUpper);
         if (var4 != null) {
            return var4;
         }
      }

      var3 = param0.oldDataFile();
      return Files.exists(var3) ? param1.apply(var3, this.fixerUpper) : null;
   }

   @Nullable
   private static WorldDataConfiguration getDataConfiguration(Path param0, DataFixer param1) {
      try {
         if (readLightweightData(param0) instanceof CompoundTag var3) {
            CompoundTag var4 = var3.getCompound("Data");
            int var5 = NbtUtils.getDataVersion(var4, -1);
            Dynamic<?> var6 = DataFixTypes.LEVEL.updateToCurrentVersion(param1, new Dynamic<>(NbtOps.INSTANCE, var4), var5);
            return readDataConfig(var6);
         }
      } catch (Exception var2) {
         LOGGER.error("Exception reading {}", param0, var2);
      }

      return null;
   }

   static BiFunction<Path, DataFixer, Pair<WorldData, WorldDimensions.Complete>> getLevelData(
      DynamicOps<Tag> param0, WorldDataConfiguration param1, Registry<LevelStem> param2, Lifecycle param3
   ) {
      return (l_param4, l_param5) -> {
         CompoundTag l_var6;
         try {
            l_var6 = NbtIo.readCompressed(l_param4.toFile());
         } catch (IOException l_var7) {
            throw new UncheckedIOException(l_var7);
         }

         CompoundTag l_var7 = l_var6.getCompound("Data");
         CompoundTag l_var8 = l_var7.contains("Player", 10) ? l_var7.getCompound("Player") : null;
         l_var7.remove("Player");
         int l_var9 = NbtUtils.getDataVersion(l_var7, -1);
         Dynamic<?> l_var10 = DataFixTypes.LEVEL.updateToCurrentVersion(l_param5, new Dynamic<>(param0, l_var7), l_var9);
         WorldGenSettings l_var11 = readWorldGenSettings(l_var10, l_param5, l_var9).getOrThrow(false, Util.prefix("WorldGenSettings: ", LOGGER::error));
         LevelVersion l_var12 = LevelVersion.parse(l_var10);
         LevelSettings l_var13 = LevelSettings.parse(l_var10, param1);
         WorldDimensions.Complete l_var14 = l_var11.dimensions().bake(param2);
         Lifecycle l_var15 = l_var14.lifecycle().add(param3);
         PrimaryLevelData l_var16 = PrimaryLevelData.parse(
            l_var10, l_param5, l_var9, l_var8, l_var13, l_var12, l_var14.specialWorldProperty(), l_var11.options(), l_var15
         );
         return Pair.of(l_var16, l_var14);
      };
   }

   BiFunction<Path, DataFixer, LevelSummary> levelSummaryReader(LevelStorageSource.LevelDirectory param0, boolean param1) {
      return (l_param2, l_param3) -> {
         try {
            if (readLightweightData(l_param2) instanceof CompoundTag l_var6) {
               CompoundTag l_var7 = l_var6.getCompound("Data");
               int l_var8 = NbtUtils.getDataVersion(l_var7, -1);
               Dynamic<?> l_var9 = DataFixTypes.LEVEL.updateToCurrentVersion(l_param3, new Dynamic<>(NbtOps.INSTANCE, l_var7), l_var8);
               LevelVersion l_var10 = LevelVersion.parse(l_var9);
               int l_var11 = l_var10.levelDataVersion();
               if (l_var11 == 19132 || l_var11 == 19133) {
                  boolean l_var12 = l_var11 != this.getStorageVersion();
                  Path l_var13 = param0.iconFile();
                  WorldDataConfiguration l_var14 = readDataConfig(l_var9);
                  LevelSettings l_var15 = LevelSettings.parse(l_var9, l_var14);
                  FeatureFlagSet l_var16 = parseFeatureFlagsFromSummary(l_var9);
                  boolean l_var17 = FeatureFlags.isExperimental(l_var16);
                  return new LevelSummary(l_var15, l_var10, param0.directoryName(), l_var12, param1, l_var17, l_var13);
               }
            } else {
               LOGGER.warn("Invalid root tag in {}", l_param2);
            }

            return null;
         } catch (Exception l_var5) {
            LOGGER.error("Exception reading {}", l_param2, l_var5);
            return null;
         }
      };
   }

   private static FeatureFlagSet parseFeatureFlagsFromSummary(Dynamic<?> param0) {
      Set<ResourceLocation> var1 = param0.get("enabled_features")
         .asStream()
         .flatMap(l_param0 -> l_param0.asString().result().map(ResourceLocation::tryParse).stream())
         .collect(Collectors.toSet());
      return FeatureFlags.REGISTRY.fromNames(var1, l_param0 -> {});
   }

   @Nullable
   private static Tag readLightweightData(Path param0) throws IOException {
      SkipFields var1 = new SkipFields(new FieldSelector("Data", CompoundTag.TYPE, "Player"), new FieldSelector("Data", CompoundTag.TYPE, "WorldGenSettings"));
      NbtIo.parseCompressed(param0.toFile(), var1);
      return var1.getResult();
   }

   public boolean isNewLevelIdAcceptable(String param0) {
      try {
         Path var2 = this.baseDir.resolve(param0);
         Files.createDirectory(var2);
         Files.deleteIfExists(var2);
         return true;
      } catch (IOException var2) {
         return false;
      }
   }

   public boolean levelExists(String param0) {
      return Files.isDirectory(this.baseDir.resolve(param0));
   }

   public Path getBaseDir() {
      return this.baseDir;
   }

   public Path getBackupPath() {
      return this.backupDir;
   }

   public LevelStorageSource.LevelStorageAccess createAccess(String param0) throws IOException {
      return new LevelStorageSource.LevelStorageAccess(param0);
   }

   public record LevelCandidates(List<LevelStorageSource.LevelDirectory> levels) implements Iterable<LevelStorageSource.LevelDirectory> {

      public boolean isEmpty() {
         return this.levels.isEmpty();
      }

      @Override
      public Iterator<LevelStorageSource.LevelDirectory> iterator() {
         return this.levels.iterator();
      }
   }

   public record LevelDirectory(Path path) {
      public String directoryName() {
         return this.path.getFileName().toString();
      }

      public Path dataFile() {
         return this.resourcePath(LevelResource.LEVEL_DATA_FILE);
      }

      public Path oldDataFile() {
         return this.resourcePath(LevelResource.OLD_LEVEL_DATA_FILE);
      }

      public Path corruptedDataFile(LocalDateTime param0) {
         return this.path.resolve(LevelResource.LEVEL_DATA_FILE.getId() + "_corrupted_" + param0.format(LevelStorageSource.FORMATTER));
      }

      public Path iconFile() {
         return this.resourcePath(LevelResource.ICON_FILE);
      }

      public Path lockFile() {
         return this.resourcePath(LevelResource.LOCK_FILE);
      }

      public Path resourcePath(LevelResource param0) {
         return this.path.resolve(param0.getId());
      }
   }

   public class LevelStorageAccess implements AutoCloseable {
      final DirectoryLock lock;
      final LevelStorageSource.LevelDirectory levelDirectory;
      private final String levelId;
      private final Map<LevelResource, Path> resources = Maps.newHashMap();

      public LevelStorageAccess(String param1) throws IOException {
         this.levelId = param1;
         this.levelDirectory = new LevelStorageSource.LevelDirectory(LevelStorageSource.this.baseDir.resolve(param1));
         this.lock = DirectoryLock.create(this.levelDirectory.path());
      }

      public String getLevelId() {
         return this.levelId;
      }

      public Path getLevelPath(LevelResource param0) {
         return this.resources.computeIfAbsent(param0, this.levelDirectory::resourcePath);
      }

      public Path getDimensionPath(ResourceKey<Level> param0) {
         return DimensionType.getStorageFolder(param0, this.levelDirectory.path());
      }

      private void checkLock() {
         if (!this.lock.isValid()) {
            throw new IllegalStateException("Lock is no longer valid");
         }
      }

      public PlayerDataStorage createPlayerStorage() {
         this.checkLock();
         return new PlayerDataStorage(this, LevelStorageSource.this.fixerUpper);
      }

      @Nullable
      public LevelSummary getSummary() {
         this.checkLock();
         return LevelStorageSource.this.readLevelData(this.levelDirectory, LevelStorageSource.this.levelSummaryReader(this.levelDirectory, false));
      }

      @Nullable
      public Pair<WorldData, WorldDimensions.Complete> getDataTag(
         DynamicOps<Tag> param0, WorldDataConfiguration param1, Registry<LevelStem> param2, Lifecycle param3
      ) {
         this.checkLock();
         return LevelStorageSource.this.readLevelData(this.levelDirectory, LevelStorageSource.getLevelData(param0, param1, param2, param3));
      }

      @Nullable
      public WorldDataConfiguration getDataConfiguration() {
         this.checkLock();
         return LevelStorageSource.this.readLevelData(this.levelDirectory, LevelStorageSource::getDataConfiguration);
      }

      public void saveDataTag(RegistryAccess param0, WorldData param1) {
         this.saveDataTag(param0, param1, null);
      }

      public void saveDataTag(RegistryAccess param0, WorldData param1, @Nullable CompoundTag param2) {
         File var4 = this.levelDirectory.path().toFile();
         CompoundTag var5 = param1.createTag(param0, param2);
         CompoundTag var6 = new CompoundTag();
         var6.put("Data", var5);

         try {
            File var7 = File.createTempFile("level", ".dat", var4);
            NbtIo.writeCompressed(var6, var7);
            File var8 = this.levelDirectory.oldDataFile().toFile();
            File var9 = this.levelDirectory.dataFile().toFile();
            Util.safeReplaceFile(var9, var7, var8);
         } catch (Exception var7) {
            LevelStorageSource.LOGGER.error("Failed to save level {}", var4, var7);
         }
      }

      public Optional<Path> getIconFile() {
         return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelDirectory.iconFile());
      }

      public void deleteLevel() throws IOException {
         this.checkLock();
         final Path var1 = this.levelDirectory.lockFile();
         LevelStorageSource.LOGGER.info("Deleting level {}", this.levelId);

         for (int var2 = 1; var2 <= 5; var2++) {
            LevelStorageSource.LOGGER.info("Attempt {}...", var2);

            try {
               Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>() {
                  public FileVisitResult visitFile(Path l_param0, BasicFileAttributes l_param1) throws IOException {
                     if (!l_param0.equals(var1)) {
                        LevelStorageSource.LOGGER.debug("Deleting {}", l_param0);
                        Files.delete(l_param0);
                     }

                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult postVisitDirectory(Path l_param0, IOException l_param1) throws IOException {
                     if (l_param1 != null) {
                        throw l_param1;
                     }

                     if (l_param0.equals(LevelStorageAccess.this.levelDirectory.path())) {
                        LevelStorageAccess.this.lock.close();
                        Files.deleteIfExists(var1);
                     }

                     Files.delete(l_param0);
                     return FileVisitResult.CONTINUE;
                  }
               });
               break;
            } catch (IOException var3) {
               if (var2 >= 5) {
                  throw var3;
               }

               LevelStorageSource.LOGGER.warn("Failed to delete {}", this.levelDirectory.path(), var3);

               try {
                  Thread.sleep(500L);
               } catch (InterruptedException var5) {
               }
            }
         }
      }

      public void renameLevel(String param0) throws IOException {
         this.checkLock();
         Path var2 = this.levelDirectory.dataFile();
         if (Files.exists(var2)) {
            CompoundTag var3 = NbtIo.readCompressed(var2.toFile());
            CompoundTag var4 = var3.getCompound("Data");
            var4.putString("LevelName", param0);
            NbtIo.writeCompressed(var3, var2.toFile());
         }
      }

      public long makeWorldBackup() throws IOException {
         this.checkLock();
         String var1 = LocalDateTime.now().format(LevelStorageSource.FORMATTER) + "_" + this.levelId;
         Path var2 = LevelStorageSource.this.getBackupPath();

         try {
            FileUtil.createDirectoriesSafe(var2);
         } catch (IOException var3) {
            throw new RuntimeException(var3);
         }

         Path var3 = var2.resolve(FileUtil.findAvailableName(var2, var1, ".zip"));

         try (final ZipOutputStream var4 = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(var3)))) {
            final Path var5 = Paths.get(this.levelId);
            Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>() {
               public FileVisitResult visitFile(Path l_param0, BasicFileAttributes l_param1) throws IOException {
                  if (l_param0.endsWith("session.lock")) {
                     return FileVisitResult.CONTINUE;
                  }

                  String l_var3 = var5.resolve(LevelStorageAccess.this.levelDirectory.path().relativize(l_param0)).toString().replace('\\', '/');
                  ZipEntry l_var4 = new ZipEntry(l_var3);
                  var4.putNextEntry(l_var4);
                  com.google.common.io.Files.asByteSource(l_param0.toFile()).copyTo(var4);
                  var4.closeEntry();
                  return FileVisitResult.CONTINUE;
               }
            });
         }

         return Files.size(var3);
      }

      @Override
      public void close() throws IOException {
         this.lock.close();
      }
   }
}
