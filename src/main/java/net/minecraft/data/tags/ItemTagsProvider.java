package net.minecraft.data.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class ItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
   private final CompletableFuture<TagsProvider.TagLookup<Block>> blockTags;
   private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy = new HashMap<>();

   public ItemTagsProvider(PackOutput param0, CompletableFuture<HolderLookup.Provider> param1, CompletableFuture<TagsProvider.TagLookup<Block>> param2) {
      super(param0, Registries.ITEM, param1, l_param0 -> l_param0.builtInRegistryHolder().key());
      this.blockTags = param2;
   }

   public ItemTagsProvider(
      PackOutput param0,
      CompletableFuture<HolderLookup.Provider> param1,
      CompletableFuture<TagsProvider.TagLookup<Item>> param2,
      CompletableFuture<TagsProvider.TagLookup<Block>> param3
   ) {
      super(param0, Registries.ITEM, param1, param2, l_param0 -> l_param0.builtInRegistryHolder().key());
      this.blockTags = param3;
   }

   protected void copy(TagKey<Block> param0, TagKey<Item> param1) {
      this.tagsToCopy.put(param0, param1);
   }

   @Override
   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return super.createContentsProvider().thenCombineAsync(this.blockTags, (l_param0, l_param1) -> {
         this.tagsToCopy.forEach((l2_param1, l2_param2) -> {
            TagBuilder l2_var4 = this.getOrCreateRawBuilder((TagKey<Item>)l2_param2);
            Optional<TagBuilder> l2_var5 = l_param1.apply((TagKey<Block>)(TagKey<?>)l2_param2);
            l2_var5.orElseThrow(() -> new IllegalStateException("Missing block tag " + l2_param2.location())).build().forEach(l2_var4::add);
         });
         return (HolderLookup.Provider)l_param0;
      });
   }
}
