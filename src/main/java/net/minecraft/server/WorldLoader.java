package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.WorldDataConfiguration;
import org.slf4j.Logger;

public class WorldLoader {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static <D, R> CompletableFuture<R> load(
      WorldLoader.InitConfig param0, WorldLoader.WorldDataSupplier<D> param1, WorldLoader.ResultFactory<D, R> param2, Executor param3, Executor param4
   ) {
      try {
         Pair<WorldDataConfiguration, CloseableResourceManager> var5 = param0.packConfig.createResourceManager();
         CloseableResourceManager var6 = var5.getSecond();
         LayeredRegistryAccess<RegistryLayer> var7 = RegistryLayer.createRegistryAccess();
         LayeredRegistryAccess<RegistryLayer> var8 = loadAndReplaceLayer(var6, var7, RegistryLayer.WORLDGEN, RegistryDataLoader.WORLDGEN_REGISTRIES);
         RegistryAccess.Frozen var9 = var8.getAccessForLoading(RegistryLayer.DIMENSIONS);
         RegistryAccess.Frozen var10 = RegistryDataLoader.load(var6, var9, RegistryDataLoader.DIMENSION_REGISTRIES);
         WorldDataConfiguration var11 = var5.getFirst();
         WorldLoader.DataLoadOutput<D> var12 = param1.get(new WorldLoader.DataLoadContext(var6, var11, var9, var10));
         LayeredRegistryAccess<RegistryLayer> var13 = var8.replaceFrom(RegistryLayer.DIMENSIONS, var12.finalDimensions);
         RegistryAccess.Frozen var14 = var13.getAccessForLoading(RegistryLayer.RELOADABLE);
         return ReloadableServerResources.loadResources(
               var6, var14, var11.enabledFeatures(), param0.commandSelection(), param0.functionCompilationLevel(), param3, param4
            )
            .whenComplete((l_param1, l_param2) -> {
               if (l_param2 != null) {
                  var6.close();
               }
            })
            .thenApplyAsync(l_param5 -> {
               l_param5.updateRegistryTags(var14);
               return param2.create(var6, l_param5, var13, var12.cookie);
            }, param4);
      } catch (Exception var5) {
         return CompletableFuture.failedFuture(var5);
      }
   }

   private static RegistryAccess.Frozen loadLayer(
      ResourceManager param0, LayeredRegistryAccess<RegistryLayer> param1, RegistryLayer param2, List<RegistryDataLoader.RegistryData<?>> param3
   ) {
      RegistryAccess.Frozen var4 = param1.getAccessForLoading(param2);
      return RegistryDataLoader.load(param0, var4, param3);
   }

   private static LayeredRegistryAccess<RegistryLayer> loadAndReplaceLayer(
      ResourceManager param0, LayeredRegistryAccess<RegistryLayer> param1, RegistryLayer param2, List<RegistryDataLoader.RegistryData<?>> param3
   ) {
      RegistryAccess.Frozen var4 = loadLayer(param0, param1, param2, param3);
      return param1.replaceFrom(param2, var4);
   }

   public record DataLoadContext(
      ResourceManager resources, WorldDataConfiguration dataConfiguration, RegistryAccess.Frozen datapackWorldgen, RegistryAccess.Frozen datapackDimensions
   ) {
   }

   public record DataLoadOutput<D>(D cookie, RegistryAccess.Frozen finalDimensions) {
   }

   public record InitConfig(WorldLoader.PackConfig packConfig, Commands.CommandSelection commandSelection, int functionCompilationLevel) {
   }

   public record PackConfig(PackRepository packRepository, WorldDataConfiguration initialDataConfig, boolean safeMode, boolean initMode) {
      public Pair<WorldDataConfiguration, CloseableResourceManager> createResourceManager() {
         FeatureFlagSet var1 = this.initMode ? FeatureFlags.REGISTRY.allFlags() : this.initialDataConfig.enabledFeatures();
         WorldDataConfiguration var2 = MinecraftServer.configurePackRepository(this.packRepository, this.initialDataConfig.dataPacks(), this.safeMode, var1);
         if (!this.initMode) {
            var2 = var2.expandFeatures(this.initialDataConfig.enabledFeatures());
         }

         List<PackResources> var3 = this.packRepository.openAllSelected();
         CloseableResourceManager var4 = new MultiPackResourceManager(PackType.SERVER_DATA, var3);
         return Pair.of(var2, var4);
      }
   }

   @FunctionalInterface
   public interface ResultFactory<D, R> {
      R create(CloseableResourceManager var1, ReloadableServerResources var2, LayeredRegistryAccess<RegistryLayer> var3, D var4);
   }

   @FunctionalInterface
   public interface WorldDataSupplier<D> {
      WorldLoader.DataLoadOutput<D> get(WorldLoader.DataLoadContext var1);
   }
}
