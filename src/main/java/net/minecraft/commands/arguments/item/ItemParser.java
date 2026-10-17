package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ItemParser {
   private static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.item.tag.disallowed")
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.item.id.invalid", param0)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      param0 -> Component.translatable("arguments.item.tag.unknown", param0)
   );
   private static final char SYNTAX_START_NBT = '{';
   private static final char SYNTAX_TAG = '#';
   private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   private final HolderLookup<Item> items;
   private final StringReader reader;
   private final boolean allowTags;
   private Either<Holder<Item>, HolderSet<Item>> result;
   @Nullable
   private CompoundTag nbt;
   private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

   private ItemParser(HolderLookup<Item> param0, StringReader param1, boolean param2) {
      this.items = param0;
      this.reader = param1;
      this.allowTags = param2;
   }

   public static ItemParser.ItemResult parseForItem(HolderLookup<Item> param0, StringReader param1) throws CommandSyntaxException {
      int var2 = param1.getCursor();

      try {
         ItemParser var3 = new ItemParser(param0, param1, false);
         var3.parse();
         Holder<Item> var4 = var3.result.left().orElseThrow(() -> new IllegalStateException("Parser returned unexpected tag name"));
         return new ItemParser.ItemResult(var4, var3.nbt);
      } catch (CommandSyntaxException var3) {
         param1.setCursor(var2);
         throw var3;
      }
   }

   public static Either<ItemParser.ItemResult, ItemParser.TagResult> parseForTesting(HolderLookup<Item> param0, StringReader param1) throws CommandSyntaxException {
      int var2 = param1.getCursor();

      try {
         ItemParser var3 = new ItemParser(param0, param1, true);
         var3.parse();
         return var3.result
            .mapBoth(
               l_param1 -> new ItemParser.ItemResult((Holder<Item>)l_param1, var3.nbt),
               l_param1 -> new ItemParser.TagResult((HolderSet<Item>)l_param1, var3.nbt)
            );
      } catch (CommandSyntaxException var3) {
         param1.setCursor(var2);
         throw var3;
      }
   }

   public static CompletableFuture<Suggestions> fillSuggestions(HolderLookup<Item> param0, SuggestionsBuilder param1, boolean param2) {
      StringReader var3 = new StringReader(param1.getInput());
      var3.setCursor(param1.getStart());
      ItemParser var4 = new ItemParser(param0, var3, param2);

      try {
         var4.parse();
      } catch (CommandSyntaxException var6) {
      }

      return var4.suggestions.apply(param1.createOffset(var3.getCursor()));
   }

   private void readItem() throws CommandSyntaxException {
      int var1 = this.reader.getCursor();
      ResourceLocation var2 = ResourceLocation.read(this.reader);
      Optional<? extends Holder<Item>> var3 = this.items.get(ResourceKey.create(Registries.ITEM, var2));
      this.result = Either.left((Holder<Item>)var3.orElseThrow(() -> {
         this.reader.setCursor(var1);
         return ERROR_UNKNOWN_ITEM.createWithContext(this.reader, var2);
      }));
   }

   private void readTag() throws CommandSyntaxException {
      if (!this.allowTags) {
         throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
      }

      int var1 = this.reader.getCursor();
      this.reader.expect('#');
      this.suggestions = this::suggestTag;
      ResourceLocation var2 = ResourceLocation.read(this.reader);
      Optional<? extends HolderSet<Item>> var3 = this.items.get(TagKey.create(Registries.ITEM, var2));
      this.result = Either.right((HolderSet<Item>)var3.orElseThrow(() -> {
         this.reader.setCursor(var1);
         return ERROR_UNKNOWN_TAG.createWithContext(this.reader, var2);
      }));
   }

   private void readNbt() throws CommandSyntaxException {
      this.nbt = new TagParser(this.reader).readStruct();
   }

   private void parse() throws CommandSyntaxException {
      if (this.allowTags) {
         this.suggestions = this::suggestItemIdOrTag;
      } else {
         this.suggestions = this::suggestItem;
      }

      if (this.reader.canRead() && this.reader.peek() == '#') {
         this.readTag();
      } else {
         this.readItem();
      }

      this.suggestions = this::suggestOpenNbt;
      if (this.reader.canRead() && this.reader.peek() == '{') {
         this.suggestions = SUGGEST_NOTHING;
         this.readNbt();
      }
   }

   private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty()) {
         param0.suggest(String.valueOf('{'));
      }

      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder param0) {
      return SharedSuggestionProvider.suggestResource(this.items.listTagIds().map(TagKey::location), param0, String.valueOf('#'));
   }

   private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder param0) {
      return SharedSuggestionProvider.suggestResource(this.items.listElementIds().map(ResourceKey::location), param0);
   }

   private CompletableFuture<Suggestions> suggestItemIdOrTag(SuggestionsBuilder param0) {
      this.suggestTag(param0);
      return this.suggestItem(param0);
   }

   public record ItemResult(Holder<Item> item, @Nullable CompoundTag nbt) {
   }

   public record TagResult(HolderSet<Item> tag, @Nullable CompoundTag nbt) {
   }
}
