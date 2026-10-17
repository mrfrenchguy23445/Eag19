package net.minecraft.data.models;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelProvider implements DataProvider {
   private final PackOutput.PathProvider blockStatePathProvider;
   private final PackOutput.PathProvider modelPathProvider;

   public ModelProvider(PackOutput param0) {
      this.blockStatePathProvider = param0.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
      this.modelPathProvider = param0.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      Map<Block, BlockStateGenerator> var2 = Maps.newHashMap();
      Consumer<BlockStateGenerator> var3 = l_param1 -> {
         Block l_var2 = l_param1.getBlock();
         BlockStateGenerator l_var3 = var2.put(l_var2, l_param1);
         if (l_var3 != null) {
            throw new IllegalStateException("Duplicate blockstate definition for " + l_var2);
         }
      };
      Map<ResourceLocation, Supplier<JsonElement>> var4 = Maps.newHashMap();
      Set<Item> var5 = Sets.newHashSet();
      BiConsumer<ResourceLocation, Supplier<JsonElement>> var6 = (l_param1, l_param2) -> {
         Supplier<JsonElement> l_var3 = var4.put(l_param1, l_param2);
         if (l_var3 != null) {
            throw new IllegalStateException("Duplicate model definition for " + l_param1);
         }
      };
      Consumer<Item> var7 = var5::add;
      new BlockModelGenerators(var3, var6, var7).run();
      new ItemModelGenerators(var6).run();
      List<Block> var8 = BuiltInRegistries.BLOCK.stream().filter(l_param1 -> !var2.containsKey(l_param1)).toList();
      if (!var8.isEmpty()) {
         throw new IllegalStateException("Missing blockstate definitions for: " + var8);
      }

      BuiltInRegistries.BLOCK.forEach(l_param2 -> {
         Item l_var3 = Item.BY_BLOCK.get(l_param2);
         if (l_var3 != null) {
            if (var5.contains(l_var3)) {
               return;
            }

            ResourceLocation l_var4 = ModelLocationUtils.getModelLocation(l_var3);
            if (!var4.containsKey(l_var4)) {
               var4.put(l_var4, new DelegatedModel(ModelLocationUtils.getModelLocation(l_param2)));
            }
         }
      });
      return CompletableFuture.allOf(
         this.saveCollection(param0, var2, l_param0 -> this.blockStatePathProvider.json(l_param0.builtInRegistryHolder().key().location())),
         this.saveCollection(param0, var4, this.modelPathProvider::json)
      );
   }

   private <T> CompletableFuture<?> saveCollection(CachedOutput param0, Map<T, ? extends Supplier<JsonElement>> param1, Function<T, Path> param2) {
      return CompletableFuture.allOf(param1.entrySet().stream().map(l_param2 -> {
         Path l_var3 = param2.apply(l_param2.getKey());
         JsonElement l_var4 = l_param2.getValue().get();
         return DataProvider.saveStable(param0, l_var4, l_var3);
      }).toArray(CompletableFuture[]::new));
   }

   @Override
   public final String getName() {
      return "Model Definitions";
   }
}
