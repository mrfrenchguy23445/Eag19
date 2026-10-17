package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public abstract class BuiltInPackSource implements RepositorySource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String VANILLA_ID = "vanilla";
   private final PackType packType;
   private final VanillaPackResources vanillaPack;
   private final ResourceLocation packDir;

   public BuiltInPackSource(PackType param0, VanillaPackResources param1, ResourceLocation param2) {
      this.packType = param0;
      this.vanillaPack = param1;
      this.packDir = param2;
   }

   @Override
   public void loadPacks(Consumer<Pack> param0) {
      Pack var2 = this.createVanillaPack(this.vanillaPack);
      if (var2 != null) {
         param0.accept(var2);
      }

      this.listBundledPacks(param0);
   }

   @Nullable
   protected abstract Pack createVanillaPack(PackResources var1);

   protected abstract Component getPackTitle(String var1);

   public VanillaPackResources getVanillaPack() {
      return this.vanillaPack;
   }

   private void listBundledPacks(Consumer<Pack> param0) {
      Map<String, Function<String, Pack>> var2 = new HashMap<>();
      this.populatePackList(var2::put);
      var2.forEach((l_param1, l_param2) -> {
         Pack l_var3 = l_param2.apply(l_param1);
         if (l_var3 != null) {
            param0.accept(l_var3);
         }
      });
   }

   protected void populatePackList(BiConsumer<String, Function<String, Pack>> param0) {
      this.vanillaPack.listRawPaths(this.packType, this.packDir, l_param1 -> this.discoverPacksInPath(l_param1, param0));
   }

   protected void discoverPacksInPath(@Nullable Path param0, BiConsumer<String, Function<String, Pack>> param1) {
      if (param0 != null && Files.isDirectory(param0)) {
         try {
            FolderRepositorySource.discoverPacks(
               param0,
               true,
               (l_param1, l_param2) -> param1.accept(pathToId(l_param1), l2_param1 -> this.createBuiltinPack(l2_param1, l_param2, this.getPackTitle(l2_param1)))
            );
         } catch (IOException var3) {
            LOGGER.warn("Failed to discover packs in {}", param0, var3);
         }
      }
   }

   private static String pathToId(Path param0) {
      return StringUtils.removeEnd(param0.getFileName().toString(), ".zip");
   }

   @Nullable
   protected abstract Pack createBuiltinPack(String var1, Pack.ResourcesSupplier var2, Component var3);
}
