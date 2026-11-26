package net.minecraft.client.searchtree;

import java.util.List;

public interface RefreshableSearchTree<T> extends SearchTree<T> {
   static <T> RefreshableSearchTree<T> empty() {
      return l_param0 -> List.of();
   }

   default void refresh() {
   }
}
