package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

public interface PlainTextSearchTree<T> {
   static <T> PlainTextSearchTree<T> empty() {
      return l_param0 -> List.of();
   }

   static <T> PlainTextSearchTree<T> create(List<T> param0, Function<T, Stream<String>> param1) {
      if (param0.isEmpty()) {
         return empty();
      }

      SuffixArray<T> var2 = new SuffixArray<>();

      for (T var4 : param0) {
         param1.apply(var4).forEach(l_param2 -> var2.add(var4, l_param2.toLowerCase(Locale.ROOT)));
      }

      var2.generate();
      return var2::search;
   }

   List<T> search(String var1);
}
