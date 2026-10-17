package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class RecipeManager extends SimpleJsonResourceReloadListener {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final Logger LOGGER = LogUtils.getLogger();
   private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = ImmutableMap.of();
   private Map<ResourceLocation, Recipe<?>> byName = ImmutableMap.of();
   private boolean hasErrors;

   public RecipeManager() {
      super(GSON, "recipes");
   }

   protected void apply(Map<ResourceLocation, JsonElement> param0, ResourceManager param1, ProfilerFiller param2) {
      this.hasErrors = false;
      Map<RecipeType<?>, Builder<ResourceLocation, Recipe<?>>> var4 = Maps.newHashMap();
      Builder<ResourceLocation, Recipe<?>> var5 = ImmutableMap.builder();

      for (Entry<ResourceLocation, JsonElement> var7 : param0.entrySet()) {
         ResourceLocation var8 = var7.getKey();

         try {
            Recipe<?> var9 = fromJson(var8, GsonHelper.convertToJsonObject(var7.getValue(), "top element"));
            var4.computeIfAbsent(var9.getType(), l_param0 -> ImmutableMap.builder()).put(var8, var9);
            var5.put(var8, var9);
         } catch (JsonParseException | IllegalArgumentException var9) {
            LOGGER.error("Parsing error loading recipe {}", var8, var9);
         }
      }

      this.recipes = var4.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, l_param0 -> ((Builder)l_param0.getValue()).build()));
      this.byName = var5.build();
      LOGGER.info("Loaded {} recipes", var4.size());
   }

   public boolean hadErrorsLoading() {
      return this.hasErrors;
   }

   public <C extends Container, T extends Recipe<C>> Optional<T> getRecipeFor(RecipeType<T> param0, C param1, Level param2) {
      return this.byType(param0).values().stream().filter(l_param2 -> l_param2.matches(param1, param2)).findFirst();
   }

   public <C extends Container, T extends Recipe<C>> Optional<Pair<ResourceLocation, T>> getRecipeFor(
      RecipeType<T> param0, C param1, Level param2, @Nullable ResourceLocation param3
   ) {
      Map<ResourceLocation, T> var5 = this.byType(param0);
      if (param3 != null) {
         T var6 = (T)var5.get(param3);
         if (var6 != null && var6.matches(param1, param2)) {
            return Optional.of(Pair.of(param3, var6));
         }
      }

      return var5.entrySet()
         .stream()
         .filter(l_param2 -> l_param2.getValue().matches(param1, param2))
         .findFirst()
         .map(l_param0 -> Pair.of(l_param0.getKey(), l_param0.getValue()));
   }

   public <C extends Container, T extends Recipe<C>> List<T> getAllRecipesFor(RecipeType<T> param0) {
      return List.copyOf(this.byType(param0).values());
   }

   public <C extends Container, T extends Recipe<C>> List<T> getRecipesFor(RecipeType<T> param0, C param1, Level param2) {
      return this.byType(param0)
         .values()
         .stream()
         .filter(l_param2 -> l_param2.matches(param1, param2))
         .sorted(Comparator.comparing(l_param1 -> l_param1.getResultItem(param2.registryAccess()).getDescriptionId()))
         .collect(Collectors.toList());
   }

   private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, T> byType(RecipeType<T> param0) {
      return (Map<ResourceLocation, T>)this.recipes.getOrDefault(param0, Collections.emptyMap());
   }

   public <C extends Container, T extends Recipe<C>> NonNullList<ItemStack> getRemainingItemsFor(RecipeType<T> param0, C param1, Level param2) {
      Optional<T> var4 = this.getRecipeFor(param0, param1, param2);
      if (var4.isPresent()) {
         return var4.get().getRemainingItems(param1);
      }

      NonNullList<ItemStack> var5 = NonNullList.withSize(param1.getContainerSize(), ItemStack.EMPTY);

      for (int var6 = 0; var6 < var5.size(); var6++) {
         var5.set(var6, param1.getItem(var6));
      }

      return var5;
   }

   public Optional<? extends Recipe<?>> byKey(ResourceLocation param0) {
      return Optional.ofNullable(this.byName.get(param0));
   }

   public Collection<Recipe<?>> getRecipes() {
      return this.recipes.values().stream().flatMap(l_param0 -> l_param0.values().stream()).collect(Collectors.toSet());
   }

   public Stream<ResourceLocation> getRecipeIds() {
      return this.recipes.values().stream().flatMap(l_param0 -> l_param0.keySet().stream());
   }

   public static Recipe<?> fromJson(ResourceLocation param0, JsonObject param1) {
      String var2 = GsonHelper.getAsString(param1, "type");
      return BuiltInRegistries.RECIPE_SERIALIZER
         .getOptional(new ResourceLocation(var2))
         .orElseThrow(() -> new JsonSyntaxException("Invalid or unsupported recipe type '" + var2 + "'"))
         .fromJson(param0, param1);
   }

   public void replaceRecipes(Iterable<Recipe<?>> param0) {
      this.hasErrors = false;
      Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> var2 = Maps.newHashMap();
      Builder<ResourceLocation, Recipe<?>> var3 = ImmutableMap.builder();
      param0.forEach(l_param2 -> {
         Map<ResourceLocation, Recipe<?>> l_var3 = var2.computeIfAbsent(l_param2.getType(), l2_param0 -> Maps.newHashMap());
         ResourceLocation l_var4 = l_param2.getId();
         Recipe<?> l_var5 = l_var3.put(l_var4, (Recipe<?>)l_param2);
         var3.put(l_var4, (Recipe<?>)l_param2);
         if (l_var5 != null) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + l_var4);
         }
      });
      this.recipes = ImmutableMap.copyOf(var2);
      this.byName = var3.build();
   }

   public static <C extends Container, T extends Recipe<C>> RecipeManager.CachedCheck<C, T> createCheck(final RecipeType<T> param0) {
      return new RecipeManager.CachedCheck<C, T>() {
         @Nullable
         private ResourceLocation lastRecipe;

         @Override
         public Optional<T> getRecipeFor(C l_param0, Level l_param1) {
            RecipeManager l_var3 = l_param1.getRecipeManager();
            Optional<Pair<ResourceLocation, T>> l_var4 = l_var3.getRecipeFor(param0, l_param0, l_param1, this.lastRecipe);
            if (l_var4.isPresent()) {
               Pair<ResourceLocation, T> l_var5 = l_var4.get();
               this.lastRecipe = l_var5.getFirst();
               return Optional.of(l_var5.getSecond());
            } else {
               return Optional.empty();
            }
         }
      };
   }

   public interface CachedCheck<C extends Container, T extends Recipe<C>> {
      Optional<T> getRecipeFor(C var1, Level var2);
   }
}
