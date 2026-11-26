package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

public class FullTextSearchTree<T> extends IdSearchTree<T> {
   private final List<T> contents;
   private final Function<T, Stream<String>> filler;
   private PlainTextSearchTree<T> plainTextSearchTree = PlainTextSearchTree.empty();

   public FullTextSearchTree(Function<T, Stream<String>> param0, Function<T, Stream<ResourceLocation>> param1, List<T> param2) {
      super(param1, param2);
      this.contents = param2;
      this.filler = param0;
   }

   @Override
   public void refresh() {
      super.refresh();
      this.plainTextSearchTree = PlainTextSearchTree.create(this.contents, this.filler);
   }

   @Override
   protected List<T> searchPlainText(String param0) {
      return this.plainTextSearchTree.search(param0);
   }

   @Override
   protected List<T> searchResourceLocation(String param0, String param1) {
      List<T> var3 = this.resourceLocationSearchTree.searchNamespace(param0);
      List<T> var4 = this.resourceLocationSearchTree.searchPath(param1);
      List<T> var5 = this.plainTextSearchTree.search(param1);
      Iterator<T> var6 = new MergingUniqueIterator<>(var4.iterator(), var5.iterator(), this.additionOrder);
      return ImmutableList.copyOf(new IntersectionIterator<>(var3.iterator(), var6, this.additionOrder));
   }
}
