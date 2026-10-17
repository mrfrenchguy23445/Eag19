package net.minecraft.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.WorldVersion;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.packs.UpdateOneTwentyVanillaAdvancementProvider;
import net.minecraft.data.advancements.packs.VanillaAdvancementProvider;
import net.minecraft.data.info.BiomeParametersDumpReport;
import net.minecraft.data.info.BlockListReport;
import net.minecraft.data.info.CommandsReport;
import net.minecraft.data.info.RegistryDumpReport;
import net.minecraft.data.loot.packs.UpdateOneTwentyLootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.models.ModelProvider;
import net.minecraft.data.recipes.packs.BundleRecipeProvider;
import net.minecraft.data.recipes.packs.UpdateOneTwentyRecipeProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.UpdateOneTwentyRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.SnbtToNbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.CatVariantTagsProvider;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.FlatLevelGeneratorPresetTagsProvider;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.GameEventTagsProvider;
import net.minecraft.data.tags.InstrumentTagsProvider;
import net.minecraft.data.tags.PaintingVariantTagsProvider;
import net.minecraft.data.tags.PoiTypeTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyBiomeTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyBlockTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyItemTagsProvider;
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.data.tags.VanillaItemTagsProvider;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class Main {
   @DontObfuscate
   public static void main(String[] param0) throws IOException {
      SharedConstants.tryDetectVersion();
      OptionParser var1 = new OptionParser();
      OptionSpec<Void> var2 = var1.accepts("help", "Show the help menu").forHelp();
      OptionSpec<Void> var3 = var1.accepts("server", "Include server generators");
      OptionSpec<Void> var4 = var1.accepts("client", "Include client generators");
      OptionSpec<Void> var5 = var1.accepts("dev", "Include development tools");
      OptionSpec<Void> var6 = var1.accepts("reports", "Include data reports");
      OptionSpec<Void> var7 = var1.accepts("validate", "Validate inputs");
      OptionSpec<Void> var8 = var1.accepts("all", "Include all generators");
      OptionSpec<String> var9 = var1.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
      OptionSpec<String> var10 = var1.accepts("input", "Input folder").withRequiredArg();
      OptionSet var11 = var1.parse(param0);
      if (!var11.has(var2) && var11.hasOptions()) {
         Path var12 = Paths.get(var9.value(var11));
         boolean var13 = var11.has(var8);
         boolean var14 = var13 || var11.has(var4);
         boolean var15 = var13 || var11.has(var3);
         boolean var16 = var13 || var11.has(var5);
         boolean var17 = var13 || var11.has(var6);
         boolean var18 = var13 || var11.has(var7);
         DataGenerator var19 = createStandardGenerator(
            var12,
            var11.valuesOf(var10).stream().map(l_param0 -> Paths.get(l_param0)).collect(Collectors.toList()),
            var14,
            var15,
            var16,
            var17,
            var18,
            SharedConstants.getCurrentVersion(),
            true
         );
         var19.run();
      } else {
         var1.printHelpOn(System.out);
      }
   }

   private static <T extends DataProvider> DataProvider.Factory<T> bindRegistries(
      BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> param0, CompletableFuture<HolderLookup.Provider> param1
   ) {
      return l_param2 -> param0.apply(l_param2, param1);
   }

   public static DataGenerator createStandardGenerator(
      Path param0, Collection<Path> param1, boolean param2, boolean param3, boolean param4, boolean param5, boolean param6, WorldVersion param7, boolean param8
   ) {
      DataGenerator var9 = new DataGenerator(param0, param7, param8);
      DataGenerator.PackGenerator var10 = var9.getVanillaPack(param2 || param3);
      var10.addProvider(l_param1 -> new SnbtToNbt(l_param1, param1).addFilter(new StructureUpdater()));
      CompletableFuture<HolderLookup.Provider> var10x = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
      DataGenerator.PackGenerator var11 = var9.getVanillaPack(param2);
      var11.addProvider(ModelProvider::new);
      DataGenerator.PackGenerator var13 = var9.getVanillaPack(param3);
      var13.addProvider(bindRegistries(RegistriesDatapackGenerator::new, var10x));
      var13.addProvider(bindRegistries(VanillaAdvancementProvider::create, var10x));
      var13.addProvider(VanillaLootTableProvider::create);
      var13.addProvider(VanillaRecipeProvider::new);
      TagsProvider<Block> var11x = var13.addProvider(bindRegistries(VanillaBlockTagsProvider::new, var10x));
      TagsProvider<Item> var12 = var13.addProvider(l_param2 -> new VanillaItemTagsProvider(l_param2, var10x, var11x.contentsGetter()));
      var13.addProvider(bindRegistries(BannerPatternTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(BiomeTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(CatVariantTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(DamageTypeTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(EntityTypeTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(FlatLevelGeneratorPresetTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(FluidTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(GameEventTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(InstrumentTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(PaintingVariantTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(PoiTypeTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(StructureTagsProvider::new, var10x));
      var13.addProvider(bindRegistries(WorldPresetTagsProvider::new, var10x));
      var13 = var9.getVanillaPack(param4);
      var13.addProvider(l_param1 -> new NbtToSnbt(l_param1, param1));
      var13 = var9.getVanillaPack(param5);
      var13.addProvider(bindRegistries(BiomeParametersDumpReport::new, var10x));
      var13.addProvider(BlockListReport::new);
      var13.addProvider(bindRegistries(CommandsReport::new, var10x));
      var13.addProvider(RegistryDumpReport::new);
      var13 = var9.getBuiltinDatapack(param3, "bundle");
      var13.addProvider(BundleRecipeProvider::new);
      var13.addProvider(
         l_param0 -> PackMetadataGenerator.forFeaturePack(
            l_param0, Component.translatable("dataPack.bundle.description"), FeatureFlagSet.of(FeatureFlags.BUNDLE)
         )
      );
      CompletableFuture<HolderLookup.Provider> var13x = UpdateOneTwentyRegistries.createLookup(var10x);
      DataGenerator.PackGenerator var14 = var9.getBuiltinDatapack(param3, "update_1_20");
      var14.addProvider(UpdateOneTwentyRecipeProvider::new);
      TagsProvider<Block> var15 = var14.addProvider(l_param2 -> new UpdateOneTwentyBlockTagsProvider(l_param2, var10x, var11x.contentsGetter()));
      var14.addProvider(l_param3 -> new UpdateOneTwentyItemTagsProvider(l_param3, var10x, var12.contentsGetter(), var15.contentsGetter()));
      var14.addProvider(bindRegistries(UpdateOneTwentyBiomeTagsProvider::new, var13x));
      var14.addProvider(UpdateOneTwentyLootTableProvider::create);
      var14.addProvider(bindRegistries(UpdateOneTwentyVanillaAdvancementProvider::create, var13x));
      var14.addProvider(bindRegistries(RegistriesDatapackGenerator::new, var13x));
      var14.addProvider(
         l_param0 -> PackMetadataGenerator.forFeaturePack(
            l_param0, Component.translatable("dataPack.update_1_20.description"), FeatureFlagSet.of(FeatureFlags.UPDATE_1_20)
         )
      );
      return var9;
   }
}
