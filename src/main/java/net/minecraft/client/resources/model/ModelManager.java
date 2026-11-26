package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public class ModelManager implements PreparableReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceLocation, ResourceLocation> VANILLA_ATLASES = Map.of(
      Sheets.BANNER_SHEET,
      new ResourceLocation("banner_patterns"),
      Sheets.BED_SHEET,
      new ResourceLocation("beds"),
      Sheets.CHEST_SHEET,
      new ResourceLocation("chests"),
      Sheets.SHIELD_SHEET,
      new ResourceLocation("shield_patterns"),
      Sheets.SIGN_SHEET,
      new ResourceLocation("signs"),
      Sheets.SHULKER_SHEET,
      new ResourceLocation("shulker_boxes"),
      Sheets.ARMOR_TRIMS_SHEET,
      new ResourceLocation("armor_trims"),
      Sheets.DECORATED_POT_SHEET,
      new ResourceLocation("decorated_pot"),
      TextureAtlas.LOCATION_BLOCKS,
      new ResourceLocation("blocks")
   );
   private Map<ResourceLocation, BakedModel> bakedRegistry;
   private final AtlasSet atlases;
   private final BlockModelShaper blockModelShaper;
   private final BlockColors blockColors;
   private int maxMipmapLevels;
   private BakedModel missingModel;
   private Object2IntMap<BlockState> modelGroups;

   public ModelManager(TextureManager param0, BlockColors param1, int param2) {
      this.blockColors = param1;
      this.maxMipmapLevels = param2;
      this.blockModelShaper = new BlockModelShaper(this);
      this.atlases = new AtlasSet(VANILLA_ATLASES, param0);
   }

   public BakedModel getModel(ModelResourceLocation param0) {
      return this.bakedRegistry.getOrDefault(param0, this.missingModel);
   }

   public BakedModel getMissingModel() {
      return this.missingModel;
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   @Override
   public final CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier param0,
      ResourceManager param1,
      ProfilerFiller param2,
      ProfilerFiller param3,
      Executor param4,
      Executor param5
   ) {
      param2.startTick();
      CompletableFuture<Map<ResourceLocation, BlockModel>> var7 = loadBlockModels(param1, param4);
      CompletableFuture<Map<ResourceLocation, List<ModelBakery.LoadedJson>>> var8 = loadBlockStates(param1, param4);
      CompletableFuture<ModelBakery> var9 = var7.thenCombineAsync(
         var8,
         (l_param1, l_param2) -> new ModelBakery(
            this.blockColors, param2, (Map<ResourceLocation, BlockModel>)l_param1, (Map<ResourceLocation, List<ModelBakery.LoadedJson>>)l_param2
         ),
         param4
      );
      Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> var10 = this.atlases.scheduleLoad(param1, this.maxMipmapLevels, param4);
      return CompletableFuture.allOf(Stream.concat(var10.values().stream(), Stream.of(var9)).toArray(CompletableFuture[]::new))
         .thenApplyAsync(
            l_param3 -> this.loadModels(
               param2, var10.entrySet().stream().collect(Collectors.toMap(Entry::getKey, l2_param0 -> l2_param0.getValue().join())), var9.join()
            ),
            param4
         )
         .thenCompose(l_param0 -> l_param0.readyForUpload.thenApply(l2_param1 -> (ModelManager.ReloadState)l_param0))
         .thenCompose(param0::wait)
         .thenAcceptAsync(l_param1 -> this.apply(l_param1, param3), param5);
   }

   private static CompletableFuture<Map<ResourceLocation, BlockModel>> loadBlockModels(ResourceManager param0, Executor param1) {
      return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> ModelBakery.MODEL_LISTER.listMatchingResources(param0), param1)
         .thenCompose(
            l_param1 -> {
               List<CompletableFuture<Pair<ResourceLocation, BlockModel>>> l_var2 = new ArrayList<>(l_param1.size());

               for (Entry<ResourceLocation, Resource> l_var4 : l_param1.entrySet()) {
                  l_var2.add(CompletableFuture.supplyAsync(() -> {
                     try (Reader l2_var1 = l_var4.getValue().openAsReader()) {
                        return Pair.of(l_var4.getKey(), BlockModel.fromStream(l2_var1));
                     } catch (Exception l2_var1) {
                        LOGGER.error("Failed to load model {}", l_var4.getKey(), l2_var1);
                        return null;
                     }
                  }, param1));
               }

               return Util.sequence(l_var2)
                  .thenApply(l2_param0 -> l2_param0.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
            }
         );
   }

   private static CompletableFuture<Map<ResourceLocation, List<ModelBakery.LoadedJson>>> loadBlockStates(ResourceManager param0, Executor param1) {
      return CompletableFuture.<Map<ResourceLocation, List<Resource>>>supplyAsync(
            () -> ModelBakery.BLOCKSTATE_LISTER.listMatchingResourceStacks(param0), param1
         )
         .thenCompose(
            l_param1 -> {
               List<CompletableFuture<Pair<ResourceLocation, List<ModelBakery.LoadedJson>>>> l_var2 = new ArrayList<>(l_param1.size());

               for (Entry<ResourceLocation, List<Resource>> l_var4 : l_param1.entrySet()) {
                  l_var2.add(CompletableFuture.supplyAsync(() -> {
                     List<Resource> l2_var1 = l_var4.getValue();
                     List<ModelBakery.LoadedJson> l2_var2 = new ArrayList<>(l2_var1.size());

                     for (Resource l2_var4 : l2_var1) {
                        try (Reader l2_var5 = l2_var4.openAsReader()) {
                           JsonObject l2_var6 = GsonHelper.parse(l2_var5);
                           l2_var2.add(new ModelBakery.LoadedJson(l2_var4.sourcePackId(), l2_var6));
                        } catch (Exception l2_var5) {
                           LOGGER.error("Failed to load blockstate {} from pack {}", l_var4.getKey(), l2_var4.sourcePackId(), l2_var5);
                        }
                     }

                     return Pair.of(l_var4.getKey(), l2_var2);
                  }, param1));
               }

               return Util.sequence(l_var2)
                  .thenApply(l2_param0 -> l2_param0.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
            }
         );
   }

   private ModelManager.ReloadState loadModels(ProfilerFiller param0, Map<ResourceLocation, AtlasSet.StitchResult> param1, ModelBakery param2) {
      param0.push("load");
      param0.popPush("baking");
      Multimap<ResourceLocation, Material> var4 = HashMultimap.create();
      param2.bakeModels((l_param2, l_param3) -> {
         AtlasSet.StitchResult l_var4 = param1.get(l_param3.atlasLocation());
         TextureAtlasSprite l_var5 = l_var4.getSprite(l_param3.texture());
         if (l_var5 != null) {
            return l_var5;
         }

         var4.put(l_param2, l_param3);
         return l_var4.missing();
      });
      var4.asMap()
         .forEach(
            (l_param0, l_param1) -> LOGGER.warn(
               "Missing textures in model {}:\n{}",
               l_param0,
               l_param1.stream()
                  .sorted(Material.COMPARATOR)
                  .map(l2_param0 -> "    " + l2_param0.atlasLocation() + ":" + l2_param0.texture())
                  .collect(Collectors.joining("\n"))
            )
         );
      param0.popPush("dispatch");
      Map<ResourceLocation, BakedModel> var5 = param2.getBakedTopLevelModels();
      BakedModel var6 = var5.get(ModelBakery.MISSING_MODEL_LOCATION);
      Map<BlockState, BakedModel> var7 = new IdentityHashMap<>();

      for (Block var9 : BuiltInRegistries.BLOCK) {
         var9.getStateDefinition().getPossibleStates().forEach(l_param3 -> {
            ResourceLocation l_var4 = l_param3.getBlock().builtInRegistryHolder().key().location();
            BakedModel l_var5 = var5.getOrDefault(BlockModelShaper.stateToModelLocation(l_var4, l_param3), var6);
            var7.put(l_param3, l_var5);
         });
      }

      CompletableFuture<Void> var8 = CompletableFuture.allOf(
         param1.values().stream().map(AtlasSet.StitchResult::readyForUpload).toArray(CompletableFuture[]::new)
      );
      param0.pop();
      param0.endTick();
      return new ModelManager.ReloadState(param2, var6, var7, param1, var8);
   }

   private void apply(ModelManager.ReloadState param0, ProfilerFiller param1) {
      param1.startTick();
      param1.push("upload");
      param0.atlasPreparations.values().forEach(AtlasSet.StitchResult::upload);
      ModelBakery var3 = param0.modelBakery;
      this.bakedRegistry = var3.getBakedTopLevelModels();
      this.modelGroups = var3.getModelGroups();
      this.missingModel = param0.missingModel;
      param1.popPush("cache");
      this.blockModelShaper.replaceCache(param0.modelCache);
      param1.pop();
      param1.endTick();
   }

   public boolean requiresRender(BlockState param0, BlockState param1) {
      if (param0 == param1) {
         return false;
      }

      int var3 = this.modelGroups.getInt(param0);
      if (var3 != -1) {
         int var4 = this.modelGroups.getInt(param1);
         if (var3 == var4) {
            FluidState var5 = param0.getFluidState();
            FluidState var6 = param1.getFluidState();
            return var5 != var6;
         }
      }

      return true;
   }

   public TextureAtlas getAtlas(ResourceLocation param0) {
      return this.atlases.getAtlas(param0);
   }

   @Override
   public void close() {
      this.atlases.close();
   }

   public void updateMaxMipLevel(int param0) {
      this.maxMipmapLevels = param0;
   }

   record ReloadState(
      ModelBakery modelBakery,
      BakedModel missingModel,
      Map<BlockState, BakedModel> modelCache,
      Map<ResourceLocation, AtlasSet.StitchResult> atlasPreparations,
      CompletableFuture<Void> readyForUpload
   ) {
   }
}
