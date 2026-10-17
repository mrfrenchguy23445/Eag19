package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument implements ArgumentType<ItemPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
   private final HolderLookup<Item> items;

   public ItemPredicateArgument(CommandBuildContext param0) {
      this.items = param0.holderLookup(Registries.ITEM);
   }

   public static ItemPredicateArgument itemPredicate(CommandBuildContext param0) {
      return new ItemPredicateArgument(param0);
   }

   public ItemPredicateArgument.Result parse(StringReader param0) throws CommandSyntaxException {
      Either<ItemParser.ItemResult, ItemParser.TagResult> var2 = ItemParser.parseForTesting(this.items, param0);
      return var2.map(
         l_param0 -> createResult(l2_param1 -> l2_param1 == l_param0.item(), l_param0.nbt()),
         l_param0 -> createResult(l_param0.tag()::contains, l_param0.nbt())
      );
   }

   public static Predicate<ItemStack> getItemPredicate(CommandContext<CommandSourceStack> param0, String param1) {
      return param0.getArgument(param1, ItemPredicateArgument.Result.class);
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> param0, SuggestionsBuilder param1) {
      return ItemParser.fillSuggestions(this.items, param1, true);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static ItemPredicateArgument.Result createResult(Predicate<Holder<Item>> param0, @Nullable CompoundTag param1) {
      return param1 != null ? l_param2 -> l_param2.is(param0) && NbtUtils.compareNbt(param1, l_param2.getTag(), true) : l_param1 -> l_param1.is(param0);
   }

   public interface Result extends Predicate<ItemStack> {
   }
}
