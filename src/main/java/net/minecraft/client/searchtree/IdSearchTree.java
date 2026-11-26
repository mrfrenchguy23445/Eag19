package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public class IdSearchTree<T> implements RefreshableSearchTree<T> {
   protected final Comparator<T> additionOrder;
   protected final ResourceLocationSearchTree<T> resourceLocationSearchTree;

   public IdSearchTree(Function<T, Stream<ResourceLocation>> param0, List<T> param1) {
      ToIntFunction<T> var3 = Util.createIndexLookup(param1);
      this.additionOrder = Comparator.comparingInt(var3);
      this.resourceLocationSearchTree = ResourceLocationSearchTree.create(param1, param0);
   }

   @Override
   public List<T> search(String param0) {
      int var2 = param0.indexOf(58);
      return var2 == -1 ? this.searchPlainText(param0) : this.searchResourceLocation(param0.substring(0, var2).trim(), param0.substring(var2 + 1).trim());
   }

   protected List<T> searchPlainText(String param0) {
      return this.resourceLocationSearchTree.searchPath(param0);
   }

   protected List<T> searchResourceLocation(String param0, String param1) {
      List<T> var3 = this.resourceLocationSearchTree.searchNamespace(param0);
      List<T> var4 = this.resourceLocationSearchTree.searchPath(param1);
      return ImmutableList.copyOf(new IntersectionIterator<>(var3.iterator(), var4.iterator(), this.additionOrder));
   }
}
