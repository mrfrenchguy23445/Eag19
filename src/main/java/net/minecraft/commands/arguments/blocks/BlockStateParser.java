package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockStateParser {
   public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.block.tag.disallowed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.block.id.invalid", param0)
   );
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("argument.block.property.unknown", param0, param1)
   );
   public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("argument.block.property.duplicate", param1, param0)
   );
   public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType(
      (param0, param1, param2) -> Component.translatable("argument.block.property.invalid", param0, param2, param1)
   );
   public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("argument.block.property.novalue", param0, param1)
   );
   public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(
      Component.translatable("argument.block.property.unclosed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      param0 -> Component.translatable("arguments.block.tag.unknown", param0)
   );
   private static final char SYNTAX_START_PROPERTIES = '[';
   private static final char SYNTAX_START_NBT = '{';
   private static final char SYNTAX_END_PROPERTIES = ']';
   private static final char SYNTAX_EQUALS = '=';
   private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
   private static final char SYNTAX_TAG = '#';
   private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   private final HolderLookup<Block> blocks;
   private final StringReader reader;
   private final boolean forTesting;
   private final boolean allowNbt;
   private final Map<Property<?>, Comparable<?>> properties = Maps.newHashMap();
   private final Map<String, String> vagueProperties = Maps.newHashMap();
   private ResourceLocation id = new ResourceLocation("");
   @Nullable
   private StateDefinition<Block, BlockState> definition;
   @Nullable
   private BlockState state;
   @Nullable
   private CompoundTag nbt;
   @Nullable
   private HolderSet<Block> tag;
   private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

   private BlockStateParser(HolderLookup<Block> param0, StringReader param1, boolean param2, boolean param3) {
      this.blocks = param0;
      this.reader = param1;
      this.forTesting = param2;
      this.allowNbt = param3;
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> param0, String param1, boolean param2) throws CommandSyntaxException {
      return parseForBlock(param0, new StringReader(param1), param2);
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> param0, StringReader param1, boolean param2) throws CommandSyntaxException {
      int var3 = param1.getCursor();

      try {
         BlockStateParser var4 = new BlockStateParser(param0, param1, false, param2);
         var4.parse();
         return new BlockStateParser.BlockResult(var4.state, var4.properties, var4.nbt);
      } catch (CommandSyntaxException var4) {
         param1.setCursor(var3);
         throw var4;
      }
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(HolderLookup<Block> param0, String param1, boolean param2) throws CommandSyntaxException {
      return parseForTesting(param0, new StringReader(param1), param2);
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      HolderLookup<Block> param0, StringReader param1, boolean param2
   ) throws CommandSyntaxException {
      int var3 = param1.getCursor();

      try {
         BlockStateParser var4 = new BlockStateParser(param0, param1, true, param2);
         var4.parse();
         return var4.tag != null
            ? Either.right(new BlockStateParser.TagResult(var4.tag, var4.vagueProperties, var4.nbt))
            : Either.left(new BlockStateParser.BlockResult(var4.state, var4.properties, var4.nbt));
      } catch (CommandSyntaxException var4) {
         param1.setCursor(var3);
         throw var4;
      }
   }

   public static CompletableFuture<Suggestions> fillSuggestions(HolderLookup<Block> param0, SuggestionsBuilder param1, boolean param2, boolean param3) {
      StringReader var4 = new StringReader(param1.getInput());
      var4.setCursor(param1.getStart());
      BlockStateParser var5 = new BlockStateParser(param0, var4, param2, param3);

      try {
         var5.parse();
      } catch (CommandSyntaxException var7) {
      }

      return var5.suggestions.apply(param1.createOffset(var4.getCursor()));
   }

   private void parse() throws CommandSyntaxException {
      if (this.forTesting) {
         this.suggestions = this::suggestBlockIdOrTag;
      } else {
         this.suggestions = this::suggestItem;
      }

      if (this.reader.canRead() && this.reader.peek() == '#') {
         this.readTag();
         this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readVagueProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      } else {
         this.readBlock();
         this.suggestions = this::suggestOpenPropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      }

      if (this.allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
         this.suggestions = SUGGEST_NOTHING;
         this.readNbt();
      }
   }

   private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty()) {
         param0.suggest(String.valueOf(']'));
      }

      return this.suggestPropertyName(param0);
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty()) {
         param0.suggest(String.valueOf(']'));
      }

      return this.suggestVaguePropertyName(param0);
   }

   private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder param0) {
      String var2 = param0.getRemaining().toLowerCase(Locale.ROOT);

      for (Property<?> var4 : this.state.getProperties()) {
         if (!this.properties.containsKey(var4) && var4.getName().startsWith(var2)) {
            param0.suggest(var4.getName() + "=");
         }
      }

      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder param0) {
      String var2 = param0.getRemaining().toLowerCase(Locale.ROOT);
      if (this.tag != null) {
         for (Holder<Block> var4 : this.tag) {
            for (Property<?> var6 : var4.value().getStateDefinition().getProperties()) {
               if (!this.vagueProperties.containsKey(var6.getName()) && var6.getName().startsWith(var2)) {
                  param0.suggest(var6.getName() + "=");
               }
            }
         }
      }

      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty() && this.hasBlockEntity()) {
         param0.suggest(String.valueOf('{'));
      }

      return param0.buildFuture();
   }

   private boolean hasBlockEntity() {
      if (this.state != null) {
         return this.state.hasBlockEntity();
      }

      if (this.tag != null) {
         for (Holder<Block> var2 : this.tag) {
            if (var2.value().defaultBlockState().hasBlockEntity()) {
               return true;
            }
         }
      }

      return false;
   }

   private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty()) {
         param0.suggest(String.valueOf('='));
      }

      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty()) {
         param0.suggest(String.valueOf(']'));
      }

      if (param0.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
         param0.suggest(String.valueOf(','));
      }

      return param0.buildFuture();
   }

   private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder param0, Property<T> param1) {
      for (T var3 : param1.getPossibleValues()) {
         if (var3 instanceof Integer var4) {
            param0.suggest(var4);
         } else {
            param0.suggest(param1.getName(var3));
         }
      }

      return param0;
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder param0, String param1) {
      boolean var3 = false;
      if (this.tag != null) {
         for (Holder<Block> var5 : this.tag) {
            Block var6 = var5.value();
            Property<?> var7 = var6.getStateDefinition().getProperty(param1);
            if (var7 != null) {
               addSuggestions(param0, var7);
            }

            if (!var3) {
               for (Property<?> var9 : var6.getStateDefinition().getProperties()) {
                  if (!this.vagueProperties.containsKey(var9.getName())) {
                     var3 = true;
                     break;
                  }
               }
            }
         }
      }

      if (var3) {
         param0.suggest(String.valueOf(','));
      }

      param0.suggest(String.valueOf(']'));
      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty() && this.tag != null) {
         boolean var2 = false;
         boolean var3 = false;

         for (Holder<Block> var5 : this.tag) {
            Block var6 = var5.value();
            var2 |= !var6.getStateDefinition().getProperties().isEmpty();
            var3 |= var6.defaultBlockState().hasBlockEntity();
            if (var2 && var3) {
               break;
            }
         }

         if (var2) {
            param0.suggest(String.valueOf('['));
         }

         if (var3) {
            param0.suggest(String.valueOf('{'));
         }
      }

      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder param0) {
      if (param0.getRemaining().isEmpty()) {
         if (!this.definition.getProperties().isEmpty()) {
            param0.suggest(String.valueOf('['));
         }

         if (this.state.hasBlockEntity()) {
            param0.suggest(String.valueOf('{'));
         }
      }

      return param0.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder param0) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listTagIds().map(TagKey::location), param0, String.valueOf('#'));
   }

   private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder param0) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listElementIds().map(ResourceKey::location), param0);
   }

   private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder param0) {
      this.suggestTag(param0);
      this.suggestItem(param0);
      return param0.buildFuture();
   }

   private void readBlock() throws CommandSyntaxException {
      int var1 = this.reader.getCursor();
      this.id = ResourceLocation.read(this.reader);
      Block var2 = this.blocks.get(ResourceKey.create(Registries.BLOCK, this.id)).orElseThrow(() -> {
         this.reader.setCursor(var1);
         return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
      }).value();
      this.definition = var2.getStateDefinition();
      this.state = var2.defaultBlockState();
   }

   private void readTag() throws CommandSyntaxException {
      if (!this.forTesting) {
         throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
      }

      int var1 = this.reader.getCursor();
      this.reader.expect('#');
      this.suggestions = this::suggestTag;
      ResourceLocation var2 = ResourceLocation.read(this.reader);
      this.tag = this.blocks.get(TagKey.create(Registries.BLOCK, var2)).orElseThrow(() -> {
         this.reader.setCursor(var1);
         return ERROR_UNKNOWN_TAG.createWithContext(this.reader, var2.toString());
      });
   }

   private void readProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestPropertyNameOrEnd;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int var1 = this.reader.getCursor();
         String var2 = this.reader.readString();
         Property<?> var3 = this.definition.getProperty(var2);
         if (var3 == null) {
            this.reader.setCursor(var1);
            throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), var2);
         }

         if (this.properties.containsKey(var3)) {
            this.reader.setCursor(var1);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), var2);
         }

         this.reader.skipWhitespace();
         this.suggestions = this::suggestEquals;
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), var2);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = l_param1 -> addSuggestions(l_param1, var3).buildFuture();
         int var4 = this.reader.getCursor();
         this.setValue(var3, this.reader.readString(), var4);
         this.suggestions = this::suggestNextPropertyOrEnd;
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestPropertyName;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readVagueProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestVaguePropertyNameOrEnd;
      int var1 = -1;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int var2 = this.reader.getCursor();
         String var3 = this.reader.readString();
         if (this.vagueProperties.containsKey(var3)) {
            this.reader.setCursor(var2);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), var3);
         }

         this.reader.skipWhitespace();
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            this.reader.setCursor(var2);
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), var3);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = l_param1 -> this.suggestVaguePropertyValue(l_param1, var3);
         var1 = this.reader.getCursor();
         String var4 = this.reader.readString();
         this.vagueProperties.put(var3, var4);
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            var1 = -1;
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestVaguePropertyName;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         if (var1 >= 0) {
            this.reader.setCursor(var1);
         }

         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readNbt() throws CommandSyntaxException {
      this.nbt = new TagParser(this.reader).readStruct();
   }

   private <T extends Comparable<T>> void setValue(Property<T> param0, String param1, int param2) throws CommandSyntaxException {
      Optional<T> var4 = param0.getValue(param1);
      if (var4.isPresent()) {
         this.state = this.state.setValue(param0, var4.get());
         this.properties.put(param0, var4.get());
      } else {
         this.reader.setCursor(param2);
         throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), param0.getName(), param1);
      }
   }

   public static String serialize(BlockState param0) {
      StringBuilder var1 = new StringBuilder(param0.getBlockHolder().unwrapKey().map(l_param0 -> l_param0.location().toString()).orElse("air"));
      if (!param0.getProperties().isEmpty()) {
         var1.append('[');
         boolean var2 = false;

         for (Entry<Property<?>, Comparable<?>> var4 : param0.getValues().entrySet()) {
            if (var2) {
               var1.append(',');
            }

            appendProperty(var1, var4.getKey(), var4.getValue());
            var2 = true;
         }

         var1.append(']');
      }

      return var1.toString();
   }

   private static <T extends Comparable<T>> void appendProperty(StringBuilder param0, Property<T> param1, Comparable<?> param2) {
      param0.append(param1.getName());
      param0.append('=');
      param0.append(param1.getName((T)param2));
   }

   public record BlockResult(BlockState blockState, Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag nbt) {
   }

   public record TagResult(HolderSet<Block> tag, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
   }
}
