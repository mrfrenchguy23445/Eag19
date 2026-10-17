package net.minecraft.commands.arguments.blocks;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockPredicateArgument implements ArgumentType<BlockPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
   private final HolderLookup<Block> blocks;

   public BlockPredicateArgument(CommandBuildContext param0) {
      this.blocks = param0.holderLookup(Registries.BLOCK);
   }

   public static BlockPredicateArgument blockPredicate(CommandBuildContext param0) {
      return new BlockPredicateArgument(param0);
   }

   public BlockPredicateArgument.Result parse(StringReader param0) throws CommandSyntaxException {
      return parse(this.blocks, param0);
   }

   public static BlockPredicateArgument.Result parse(HolderLookup<Block> param0, StringReader param1) throws CommandSyntaxException {
      return BlockStateParser.parseForTesting(param0, param1, true)
         .map(
            l_param0 -> new BlockPredicateArgument.BlockPredicate(l_param0.blockState(), l_param0.properties().keySet(), l_param0.nbt()),
            l_param0 -> new BlockPredicateArgument.TagPredicate(l_param0.tag(), l_param0.vagueProperties(), l_param0.nbt())
         );
   }

   public static Predicate<BlockInWorld> getBlockPredicate(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return param0.getArgument(param1, BlockPredicateArgument.Result.class);
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> param0, SuggestionsBuilder param1) {
      return BlockStateParser.fillSuggestions(this.blocks, param1, true, true);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   static class BlockPredicate implements BlockPredicateArgument.Result {
      private final BlockState state;
      private final Set<Property<?>> properties;
      @Nullable
      private final CompoundTag nbt;

      public BlockPredicate(BlockState param0, Set<Property<?>> param1, @Nullable CompoundTag param2) {
         this.state = param0;
         this.properties = param1;
         this.nbt = param2;
      }

      public boolean test(BlockInWorld param0) {
         BlockState var2 = param0.getState();
         if (!var2.is(this.state.getBlock())) {
            return false;
         }

         for (Property<?> var4 : this.properties) {
            if (var2.getValue(var4) != this.state.getValue(var4)) {
               return false;
            }
         }

         if (this.nbt == null) {
            return true;
         }

         BlockEntity var3 = param0.getEntity();
         return var3 != null && NbtUtils.compareNbt(this.nbt, var3.saveWithFullMetadata(), true);
      }

      @Override
      public boolean requiresNbt() {
         return this.nbt != null;
      }
   }

   public interface Result extends Predicate<BlockInWorld> {
      boolean requiresNbt();
   }

   static class TagPredicate implements BlockPredicateArgument.Result {
      private final HolderSet<Block> tag;
      @Nullable
      private final CompoundTag nbt;
      private final Map<String, String> vagueProperties;

      TagPredicate(HolderSet<Block> param0, Map<String, String> param1, @Nullable CompoundTag param2) {
         this.tag = param0;
         this.vagueProperties = param1;
         this.nbt = param2;
      }

      public boolean test(BlockInWorld param0) {
         BlockState var2 = param0.getState();
         if (!var2.is(this.tag)) {
            return false;
         }

         for (Entry<String, String> var4 : this.vagueProperties.entrySet()) {
            Property<?> var5 = var2.getBlock().getStateDefinition().getProperty(var4.getKey());
            if (var5 == null) {
               return false;
            }

            Comparable<?> var6 = (Comparable<?>)var5.getValue(var4.getValue()).orElse(null);
            if (var6 == null) {
               return false;
            }

            if (var2.getValue(var5) != var6) {
               return false;
            }
         }

         if (this.nbt == null) {
            return true;
         }

         BlockEntity var3 = param0.getEntity();
         return var3 != null && NbtUtils.compareNbt(this.nbt, var3.saveWithFullMetadata(), true);
      }

      @Override
      public boolean requiresNbt() {
         return this.nbt != null;
      }
   }
}
