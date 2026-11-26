package net.minecraft.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;

public class LayeredRegistryAccess<T> {
   private final List<T> keys;
   private final List<RegistryAccess.Frozen> values;
   private final RegistryAccess.Frozen composite;

   public LayeredRegistryAccess(List<T> param0) {
      this(param0, Util.make(() -> {
         RegistryAccess.Frozen[] l_var1 = new RegistryAccess.Frozen[param0.size()];
         Arrays.fill(l_var1, RegistryAccess.EMPTY);
         return Arrays.asList(l_var1);
      }));
   }

   private LayeredRegistryAccess(List<T> param0, List<RegistryAccess.Frozen> param1) {
      this.keys = List.copyOf(param0);
      this.values = List.copyOf(param1);
      this.composite = new RegistryAccess.ImmutableRegistryAccess(collectRegistries(param1.stream())).freeze();
   }

   private int getLayerIndexOrThrow(T param0) {
      int var2 = this.keys.indexOf(param0);
      if (var2 == -1) {
         throw new IllegalStateException("Can't find " + param0 + " inside " + this.keys);
      } else {
         return var2;
      }
   }

   public RegistryAccess.Frozen getLayer(T param0) {
      int var2 = this.getLayerIndexOrThrow(param0);
      return this.values.get(var2);
   }

   public RegistryAccess.Frozen getAccessForLoading(T param0) {
      int var2 = this.getLayerIndexOrThrow(param0);
      return this.getCompositeAccessForLayers(0, var2);
   }

   public RegistryAccess.Frozen getAccessFrom(T param0) {
      int var2 = this.getLayerIndexOrThrow(param0);
      return this.getCompositeAccessForLayers(var2, this.values.size());
   }

   private RegistryAccess.Frozen getCompositeAccessForLayers(int param0, int param1) {
      return new RegistryAccess.ImmutableRegistryAccess(collectRegistries(this.values.subList(param0, param1).stream())).freeze();
   }

   public LayeredRegistryAccess<T> replaceFrom(T param0, RegistryAccess.Frozen... param1) {
      return this.replaceFrom(param0, Arrays.asList(param1));
   }

   public LayeredRegistryAccess<T> replaceFrom(T param0, List<RegistryAccess.Frozen> param1) {
      int var3 = this.getLayerIndexOrThrow(param0);
      if (param1.size() > this.values.size() - var3) {
         throw new IllegalStateException("Too many values to replace");
      }

      List<RegistryAccess.Frozen> var4 = new ArrayList<>();

      for (int var5 = 0; var5 < var3; var5++) {
         var4.add(this.values.get(var5));
      }

      var4.addAll(param1);

      while (var4.size() < this.values.size()) {
         var4.add(RegistryAccess.EMPTY);
      }

      return new LayeredRegistryAccess<>(this.keys, var4);
   }

   public RegistryAccess.Frozen compositeAccess() {
      return this.composite;
   }

   private static Map<ResourceKey<? extends Registry<?>>, Registry<?>> collectRegistries(Stream<? extends RegistryAccess> param0) {
      Map<ResourceKey<? extends Registry<?>>, Registry<?>> var1 = new HashMap<>();
      param0.forEach(l_param1 -> l_param1.registries().forEach(l2_param1 -> {
         if (var1.put(l2_param1.key(), l2_param1.value()) != null) {
            throw new IllegalStateException("Duplicated registry " + l2_param1.key());
         }
      }));
      return var1;
   }
}
