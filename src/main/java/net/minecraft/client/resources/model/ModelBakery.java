package net.minecraft.client.resources.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

public class ModelBakery {
   public static final Material FIRE_0 = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/fire_0"));
   public static final Material FIRE_1 = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/fire_1"));
   public static final Material LAVA_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/lava_flow"));
   public static final Material WATER_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/water_flow"));
   public static final Material WATER_OVERLAY = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/water_overlay"));
   public static final Material BANNER_BASE = new Material(Sheets.BANNER_SHEET, new ResourceLocation("entity/banner_base"));
   public static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, new ResourceLocation("entity/shield_base"));
   public static final Material NO_PATTERN_SHIELD = new Material(Sheets.SHIELD_SHEET, new ResourceLocation("entity/shield_base_nopattern"));
   public static final int DESTROY_STAGE_COUNT = 10;
   public static final List<ResourceLocation> DESTROY_STAGES = IntStream.range(0, 10)
      .mapToObj(param0 -> new ResourceLocation("block/destroy_stage_" + param0))
      .collect(Collectors.toList());
   public static final List<ResourceLocation> BREAKING_LOCATIONS = DESTROY_STAGES.stream()
      .map(param0 -> new ResourceLocation("textures/" + param0.getPath() + ".png"))
      .collect(Collectors.toList());
   public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderType::crumbling).collect(Collectors.toList());
   static final int SINGLETON_MODEL_GROUP = -1;
   private static final int INVISIBLE_MODEL_GROUP = 0;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String BUILTIN_SLASH = "builtin/";
   private static final String BUILTIN_SLASH_GENERATED = "builtin/generated";
   private static final String BUILTIN_BLOCK_ENTITY = "builtin/entity";
   private static final String MISSING_MODEL_NAME = "missing";
   public static final ModelResourceLocation MISSING_MODEL_LOCATION = ModelResourceLocation.vanilla("builtin/missing", "missing");
   public static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");
   public static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
   @VisibleForTesting
   public static final String MISSING_MODEL_MESH = ("{    'textures': {       'particle': '"
         + MissingTextureAtlasSprite.getLocation().getPath()
         + "',       'missingno': '"
         + MissingTextureAtlasSprite.getLocation().getPath()
         + "'    },    'elements': [         {  'from': [ 0, 0, 0 ],            'to': [ 16, 16, 16 ],            'faces': {                'down':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'down',  'texture': '#missingno' },                'up':    { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'up',    'texture': '#missingno' },                'north': { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'north', 'texture': '#missingno' },                'south': { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'south', 'texture': '#missingno' },                'west':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'west',  'texture': '#missingno' },                'east':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'east',  'texture': '#missingno' }            }        }    ]}")
      .replace('\'', '"');
   private static final Map<String, String> BUILTIN_MODELS = Maps.newHashMap(ImmutableMap.of("missing", MISSING_MODEL_MESH));
   private static final Splitter COMMA_SPLITTER = Splitter.on(',');
   private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);
   public static final BlockModel GENERATION_MARKER = Util.make(
      BlockModel.fromString("{\"gui_light\": \"front\"}"), param0 -> param0.name = "generation marker"
   );
   public static final BlockModel BLOCK_ENTITY_MARKER = Util.make(
      BlockModel.fromString("{\"gui_light\": \"side\"}"), param0 -> param0.name = "block entity marker"
   );
   private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
      .add(BooleanProperty.create("map"))
      .create(Block::defaultBlockState, BlockState::new);
   static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
   private static final Map<ResourceLocation, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = ImmutableMap.of(
      new ResourceLocation("item_frame"), ITEM_FRAME_FAKE_DEFINITION, new ResourceLocation("glow_item_frame"), ITEM_FRAME_FAKE_DEFINITION
   );
   private final BlockColors blockColors;
   private final Map<ResourceLocation, BlockModel> modelResources;
   private final Map<ResourceLocation, List<ModelBakery.LoadedJson>> blockStateResources;
   private final Set<ResourceLocation> loadingStack = Sets.newHashSet();
   private final BlockModelDefinition.Context context = new BlockModelDefinition.Context();
   private final Map<ResourceLocation, UnbakedModel> unbakedCache = Maps.newHashMap();
   final Map<ModelBakery.BakedCacheKey, BakedModel> bakedCache = Maps.newHashMap();
   private final Map<ResourceLocation, UnbakedModel> topLevelModels = Maps.newHashMap();
   private final Map<ResourceLocation, BakedModel> bakedTopLevelModels = Maps.newHashMap();
   private int nextModelGroup = 1;
   private final Object2IntMap<BlockState> modelGroups = Util.make(new Object2IntOpenHashMap<>(), l_param0 -> l_param0.defaultReturnValue(-1));

   public ModelBakery(
      BlockColors param0, ProfilerFiller param1, Map<ResourceLocation, BlockModel> param2, Map<ResourceLocation, List<ModelBakery.LoadedJson>> param3
   ) {
      this.blockColors = param0;
      this.modelResources = param2;
      this.blockStateResources = param3;
      param1.push("missing_model");

      try {
         this.unbakedCache.put(MISSING_MODEL_LOCATION, this.loadBlockModel(MISSING_MODEL_LOCATION));
         this.loadTopLevel(MISSING_MODEL_LOCATION);
      } catch (IOException var5) {
         LOGGER.error("Error loading missing model, should never happen :(", var5);
         throw new RuntimeException(var5);
      }

      param1.popPush("static_definitions");
      STATIC_DEFINITIONS.forEach(
         (l_param0, l_param1) -> l_param1.getPossibleStates()
            .forEach(l2_param1 -> this.loadTopLevel(BlockModelShaper.stateToModelLocation(l_param0, l2_param1)))
      );
      param1.popPush("blocks");

      for (Block var6 : BuiltInRegistries.BLOCK) {
         var6.getStateDefinition().getPossibleStates().forEach(l_param0 -> this.loadTopLevel(BlockModelShaper.stateToModelLocation(l_param0)));
      }

      param1.popPush("items");

      for (ResourceLocation var6 : BuiltInRegistries.ITEM.keySet()) {
         this.loadTopLevel(new ModelResourceLocation(var6, "inventory"));
      }

      param1.popPush("special");
      this.loadTopLevel(ItemRenderer.TRIDENT_IN_HAND_MODEL);
      this.loadTopLevel(ItemRenderer.SPYGLASS_IN_HAND_MODEL);
      this.topLevelModels.values().forEach(l_param0 -> l_param0.resolveParents(this::getModel));
      param1.pop();
   }

   public void bakeModels(BiFunction<ResourceLocation, Material, TextureAtlasSprite> param0) {
      this.topLevelModels.keySet().forEach(l_param1 -> {
         BakedModel l_var3 = null;

         try {
            l_var3 = new ModelBakery.ModelBakerImpl(param0, l_param1).bake(l_param1, BlockModelRotation.X0_Y0);
         } catch (Exception l_var4) {
            LOGGER.warn("Unable to bake model: '{}': {}", l_param1, l_var4);
         }

         if (l_var3 != null) {
            this.bakedTopLevelModels.put(l_param1, l_var3);
         }
      });
   }

   private static Predicate<BlockState> predicate(StateDefinition<Block, BlockState> param0, String param1) {
      Map<Property<?>, Comparable<?>> var2 = Maps.newHashMap();

      for (String var4 : COMMA_SPLITTER.split(param1)) {
         Iterator<String> var5 = EQUAL_SPLITTER.split(var4).iterator();
         if (var5.hasNext()) {
            String var6 = var5.next();
            Property<?> var7 = param0.getProperty(var6);
            if (var7 != null && var5.hasNext()) {
               String var8 = var5.next();
               Comparable<?> var9 = getValueHelper((Property)var7, var8);
               if (var9 == null) {
                  throw new RuntimeException("Unknown value: '" + var8 + "' for blockstate property: '" + var6 + "' " + var7.getPossibleValues());
               }

               var2.put(var7, var9);
            } else if (!var6.isEmpty()) {
               throw new RuntimeException("Unknown blockstate property: '" + var6 + "'");
            }
         }
      }

      Block var3 = param0.getOwner();
      return l_param2 -> {
         if (l_param2 != null && l_param2.is(var3)) {
            for (Entry<Property<?>, Comparable<?>> l_var4 : var2.entrySet()) {
               if (!Objects.equals(l_param2.getValue(l_var4.getKey()), l_var4.getValue())) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      };
   }

   @Nullable
   static <T extends Comparable<T>> T getValueHelper(Property<T> param0, String param1) {
      return param0.getValue(param1).orElse(null);
   }

   public UnbakedModel getModel(ResourceLocation param0) {
      if (this.unbakedCache.containsKey(param0)) {
         return this.unbakedCache.get(param0);
      }

      if (this.loadingStack.contains(param0)) {
         throw new IllegalStateException("Circular reference while loading " + param0);
      }

      this.loadingStack.add(param0);
      UnbakedModel var2 = this.unbakedCache.get(MISSING_MODEL_LOCATION);

      while (!this.loadingStack.isEmpty()) {
         ResourceLocation var3 = this.loadingStack.iterator().next();

         try {
            if (!this.unbakedCache.containsKey(var3)) {
               this.loadModel(var3);
            }
         } catch (ModelBakery.BlockStateDefinitionException var4) {
            LOGGER.warn(var4.getMessage());
            this.unbakedCache.put(var3, var2);
         } catch (Exception var4) {
            LOGGER.warn("Unable to load model: '{}' referenced from: {}: {}", var3, param0, var4);
            this.unbakedCache.put(var3, var2);
         } finally {
            this.loadingStack.remove(var3);
         }
      }

      return this.unbakedCache.getOrDefault(param0, var2);
   }

   private void loadModel(ResourceLocation param0) throws Exception {
      if (!(param0 instanceof ModelResourceLocation var2)) {
         this.cacheAndQueueDependencies(param0, this.loadBlockModel(param0));
      } else {
         if (Objects.equals(var2.getVariant(), "inventory")) {
            ResourceLocation var3 = param0.withPrefix("item/");
            BlockModel var4 = this.loadBlockModel(var3);
            this.cacheAndQueueDependencies(var2, var4);
            this.unbakedCache.put(var3, var4);
         } else {
            ResourceLocation var3 = new ResourceLocation(param0.getNamespace(), param0.getPath());
            StateDefinition<Block, BlockState> var4 = Optional.ofNullable(STATIC_DEFINITIONS.get(var3))
               .orElseGet(() -> BuiltInRegistries.BLOCK.get(var3).getStateDefinition());
            this.context.setDefinition(var4);
            List<Property<?>> var5 = ImmutableList.copyOf(this.blockColors.getColoringProperties(var4.getOwner()));
            ImmutableList<BlockState> var6 = var4.getPossibleStates();
            Map<ModelResourceLocation, BlockState> var7 = Maps.newHashMap();
            var6.forEach(l_param2 -> var7.put(BlockModelShaper.stateToModelLocation(var3, l_param2), l_param2));
            Map<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>> var8 = Maps.newHashMap();
            ResourceLocation var9 = BLOCKSTATE_LISTER.idToFile(param0);
            UnbakedModel var10 = this.unbakedCache.get(MISSING_MODEL_LOCATION);
            ModelBakery.ModelGroupKey var11 = new ModelBakery.ModelGroupKey(ImmutableList.of(var10), ImmutableList.of());
            Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> var12 = Pair.of(var10, () -> var11);

            try {
               for (Pair<String, BlockModelDefinition> var15 : this.blockStateResources
                  .getOrDefault(var9, List.of())
                  .stream()
                  .map(
                     l_param1 -> {
                        try {
                           return Pair.of(l_param1.source, BlockModelDefinition.fromJsonElement(this.context, l_param1.data));
                        } catch (Exception l_var3) {
                           throw new ModelBakery.BlockStateDefinitionException(
                              String.format(
                                 Locale.ROOT,
                                 "Exception loading blockstate definition: '%s' in resourcepack: '%s': %s",
                                 var9,
                                 l_param1.source,
                                 l_var3.getMessage()
                              )
                           );
                        }
                     }
                  )
                  .toList()) {
                  BlockModelDefinition var16 = var15.getSecond();
                  Map<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>> var17 = Maps.newIdentityHashMap();
                  MultiPart var18;
                  if (var16.isMultiPart()) {
                     var18 = var16.getMultiPart();
                     var6.forEach(l_param3 -> var17.put(l_param3, Pair.of(var18, () -> ModelBakery.ModelGroupKey.create(l_param3, var18, var5))));
                  } else {
                     var18 = null;
                  }

                  var16.getVariants()
                     .forEach(
                        (l_param9, l_param10) -> {
                           try {
                              var6.stream()
                                 .filter(predicate(var4, l_param9))
                                 .forEach(
                                    l2_param6 -> {
                                       Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> l2_var7 = var17.put(
                                          l2_param6, Pair.of(l_param10, () -> ModelBakery.ModelGroupKey.create(l2_param6, l_param10, var5))
                                       );
                                       if (l2_var7 != null && l2_var7.getFirst() != var18) {
                                          var17.put(l2_param6, var12);
                                          throw new RuntimeException(
                                             "Overlapping definition with: "
                                                + var16.getVariants()
                                                   .entrySet()
                                                   .stream()
                                                   .filter(l3_param1 -> l3_param1.getValue() == l2_var7.getFirst())
                                                   .findFirst()
                                                   .get()
                                                   .getKey()
                                          );
                                       }
                                    }
                                 );
                           } catch (Exception l_var11) {
                              LOGGER.warn(
                                 "Exception loading blockstate definition: '{}' in resourcepack: '{}' for variant: '{}': {}",
                                 var9,
                                 var15.getFirst(),
                                 l_param9,
                                 l_var11.getMessage()
                              );
                           }
                        }
                     );
                  var8.putAll(var17);
               }
            } catch (ModelBakery.BlockStateDefinitionException var13) {
               throw var13;
            } catch (Exception var13) {
               throw new ModelBakery.BlockStateDefinitionException(String.format(Locale.ROOT, "Exception loading blockstate definition: '%s': %s", var9, var13));
            } finally {
               Map<ModelBakery.ModelGroupKey, Set<BlockState>> var20 = Maps.newHashMap();
               var7.forEach((l_param4, l_param5) -> {
                  Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> l_var7 = var8.get(l_param5);
                  if (l_var7 == null) {
                     LOGGER.warn("Exception loading blockstate definition: '{}' missing model for variant: '{}'", var9, l_param4);
                     l_var7 = var12;
                  }

                  this.cacheAndQueueDependencies(l_param4, l_var7.getFirst());

                  try {
                     ModelBakery.ModelGroupKey l_var8 = l_var7.getSecond().get();
                     var20.computeIfAbsent(l_var8, l2_param0 -> Sets.newIdentityHashSet()).add(l_param5);
                  } catch (Exception l_var8) {
                     LOGGER.warn("Exception evaluating model definition: '{}'", l_param4, l_var8);
                  }
               });
               var20.forEach((l_param0, l_param1) -> {
                  Iterator<BlockState> l_var3 = l_param1.iterator();

                  while (l_var3.hasNext()) {
                     BlockState l_var4 = l_var3.next();
                     if (l_var4.getRenderShape() != RenderShape.MODEL) {
                        l_var3.remove();
                        this.modelGroups.put(l_var4, 0);
                     }
                  }

                  if (l_param1.size() > 1) {
                     this.registerModelGroup(l_param1);
                  }
               });
            }
         }
      }
   }

   private void cacheAndQueueDependencies(ResourceLocation param0, UnbakedModel param1) {
      this.unbakedCache.put(param0, param1);
      this.loadingStack.addAll(param1.getDependencies());
   }

   private void loadTopLevel(ModelResourceLocation param0) {
      UnbakedModel var2 = this.getModel(param0);
      this.unbakedCache.put(param0, var2);
      this.topLevelModels.put(param0, var2);
   }

   private void registerModelGroup(Iterable<BlockState> param0) {
      int var2 = this.nextModelGroup++;
      param0.forEach(l_param1 -> this.modelGroups.put(l_param1, var2));
   }

   private BlockModel loadBlockModel(ResourceLocation param0) throws IOException {
      String var2 = param0.getPath();
      if ("builtin/generated".equals(var2)) {
         return GENERATION_MARKER;
      }

      if ("builtin/entity".equals(var2)) {
         return BLOCK_ENTITY_MARKER;
      }

      if (var2.startsWith("builtin/")) {
         String var3 = var2.substring("builtin/".length());
         String var4 = BUILTIN_MODELS.get(var3);
         if (var4 == null) {
            throw new FileNotFoundException(param0.toString());
         }

         Reader var5 = new StringReader(var4);
         BlockModel var6 = BlockModel.fromStream(var5);
         var6.name = param0.toString();
         return var6;
      } else {
         ResourceLocation var3 = MODEL_LISTER.idToFile(param0);
         BlockModel var4 = this.modelResources.get(var3);
         if (var4 == null) {
            throw new FileNotFoundException(var3.toString());
         }

         var4.name = param0.toString();
         return var4;
      }
   }

   public Map<ResourceLocation, BakedModel> getBakedTopLevelModels() {
      return this.bakedTopLevelModels;
   }

   public Object2IntMap<BlockState> getModelGroups() {
      return this.modelGroups;
   }

   record BakedCacheKey(ResourceLocation id, Transformation transformation, boolean isUvLocked) {
   }

   static class BlockStateDefinitionException extends RuntimeException {
      public BlockStateDefinitionException(String param0) {
         super(param0);
      }
   }

   public record LoadedJson(String source, JsonElement data) {
   }

   class ModelBakerImpl implements ModelBaker {
      private final Function<Material, TextureAtlasSprite> modelTextureGetter;

      ModelBakerImpl(BiFunction<ResourceLocation, Material, TextureAtlasSprite> param1, ResourceLocation param2) {
         this.modelTextureGetter = l_param2 -> param1.apply(param2, l_param2);
      }

      @Override
      public UnbakedModel getModel(ResourceLocation param0) {
         return ModelBakery.this.getModel(param0);
      }

      @Override
      public BakedModel bake(ResourceLocation param0, ModelState param1) {
         ModelBakery.BakedCacheKey var3 = new ModelBakery.BakedCacheKey(param0, param1.getRotation(), param1.isUvLocked());
         BakedModel var4 = ModelBakery.this.bakedCache.get(var3);
         if (var4 != null) {
            return var4;
         } else {
            UnbakedModel var5 = this.getModel(param0);
            if (var5 instanceof BlockModel var6 && var6.getRootModel() == ModelBakery.GENERATION_MARKER) {
               return ModelBakery.ITEM_MODEL_GENERATOR
                  .generateBlockModel(this.modelTextureGetter, var6)
                  .bake(this, var6, this.modelTextureGetter, param1, param0, false);
            } else {
               BakedModel var6 = var5.bake(this, this.modelTextureGetter, param1, param0);
               ModelBakery.this.bakedCache.put(var3, var6);
               return var6;
            }
         }
      }
   }

   static class ModelGroupKey {
      private final List<UnbakedModel> models;
      private final List<Object> coloringValues;

      public ModelGroupKey(List<UnbakedModel> param0, List<Object> param1) {
         this.models = param0;
         this.coloringValues = param1;
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else {
            return !(param0 instanceof ModelBakery.ModelGroupKey var2)
               ? false
               : Objects.equals(this.models, var2.models) && Objects.equals(this.coloringValues, var2.coloringValues);
         }
      }

      @Override
      public int hashCode() {
         return 31 * this.models.hashCode() + this.coloringValues.hashCode();
      }

      public static ModelBakery.ModelGroupKey create(BlockState param0, MultiPart param1, Collection<Property<?>> param2) {
         StateDefinition<Block, BlockState> var3 = param0.getBlock().getStateDefinition();
         List<UnbakedModel> var4 = param1.getSelectors()
            .stream()
            .filter(l_param2 -> l_param2.getPredicate(var3).test(param0))
            .map(Selector::getVariant)
            .collect(ImmutableList.toImmutableList());
         List<Object> var5 = getColoringValues(param0, param2);
         return new ModelBakery.ModelGroupKey(var4, var5);
      }

      public static ModelBakery.ModelGroupKey create(BlockState param0, UnbakedModel param1, Collection<Property<?>> param2) {
         List<Object> var3 = getColoringValues(param0, param2);
         return new ModelBakery.ModelGroupKey(ImmutableList.of(param1), var3);
      }

      private static List<Object> getColoringValues(BlockState param0, Collection<Property<?>> param1) {
         return param1.stream().map(param0::getValue).collect(ImmutableList.toImmutableList());
      }
   }
}
