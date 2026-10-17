package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

public interface ResourceLocationSearchTree<T> {
   static <T> ResourceLocationSearchTree<T> empty() {
      return new ResourceLocationSearchTree<T>() {
         @Override
         public List<T> searchNamespace(String l_param0) {
            return List.of();
         }

         @Override
         public List<T> searchPath(String l_param0) {
            return List.of();
         }
      };
   }

   static <T> ResourceLocationSearchTree<T> create(List<T> param0, Function<T, Stream<ResourceLocation>> param1) {
      if (param0.isEmpty()) {
         return empty();
      }

      final SuffixArray<T> var2 = new SuffixArray<>();
      final SuffixArray<T> var3 = new SuffixArray<>();

      for (T var5 : param0) {
         param1.apply(var5).forEach(l_param3 -> {
            var2.add(var5, l_param3.getNamespace().toLowerCase(Locale.ROOT));
            var3.add(var5, l_param3.getPath().toLowerCase(Locale.ROOT));
         });
      }

      var2.generate();
      var3.generate();
      return new ResourceLocationSearchTree<T>() {
         @Override
         public List<T> searchNamespace(String l_param0) {
            return var2.search(l_param0);
         }

         @Override
         public List<T> searchPath(String l_param0) {
            return var3.search(l_param0);
         }
      };
   }

   List<T> searchNamespace(String var1);

   List<T> searchPath(String var1);
}
