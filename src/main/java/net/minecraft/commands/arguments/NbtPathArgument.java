package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NbtPathArgument implements ArgumentType<NbtPathArgument.NbtPath> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
   public static final SimpleCommandExceptionType ERROR_INVALID_NODE = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.node.invalid"));
   public static final SimpleCommandExceptionType ERROR_DATA_TOO_DEEP = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.too_deep"));
   public static final DynamicCommandExceptionType ERROR_NOTHING_FOUND = new DynamicCommandExceptionType(
      param0 -> Component.translatable("arguments.nbtpath.nothing_found", param0)
   );
   static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.data.modify.expected_list", param0)
   );
   static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.data.modify.invalid_index", param0)
   );
   private static final char INDEX_MATCH_START = '[';
   private static final char INDEX_MATCH_END = ']';
   private static final char KEY_MATCH_START = '{';
   private static final char KEY_MATCH_END = '}';
   private static final char QUOTED_KEY_START = '"';

   public static NbtPathArgument nbtPath() {
      return new NbtPathArgument();
   }

   public static NbtPathArgument.NbtPath getPath(CommandContext<CommandSourceStack> param0, String param1) {
      return param0.getArgument(param1, NbtPathArgument.NbtPath.class);
   }

   public NbtPathArgument.NbtPath parse(StringReader param0) throws CommandSyntaxException {
      List<NbtPathArgument.Node> var2 = Lists.newArrayList();
      int var3 = param0.getCursor();
      Object2IntMap<NbtPathArgument.Node> var4 = new Object2IntOpenHashMap<>();
      boolean var5 = true;

      while (param0.canRead() && param0.peek() != ' ') {
         NbtPathArgument.Node var6 = parseNode(param0, var5);
         var2.add(var6);
         var4.put(var6, param0.getCursor() - var3);
         var5 = false;
         if (param0.canRead()) {
            char var7 = param0.peek();
            if (var7 != ' ' && var7 != '[' && var7 != '{') {
               param0.expect('.');
            }
         }
      }

      return new NbtPathArgument.NbtPath(param0.getString().substring(var3, param0.getCursor()), var2.toArray(new NbtPathArgument.Node[0]), var4);
   }

   private static NbtPathArgument.Node parseNode(StringReader param0, boolean param1) throws CommandSyntaxException {
      switch (param0.peek()) {
         case '"': {
            String var2 = param0.readString();
            return readObjectNode(param0, var2);
         }
         case '[': {
            param0.skip();
            int var2 = param0.peek();
            if (var2 == 123) {
               CompoundTag var3 = new TagParser(param0).readStruct();
               param0.expect(']');
               return new NbtPathArgument.MatchElementNode(var3);
            } else {
               if (var2 == 93) {
                  param0.skip();
                  return NbtPathArgument.AllElementsNode.INSTANCE;
               }

               int var3 = param0.readInt();
               param0.expect(']');
               return new NbtPathArgument.IndexedElementNode(var3);
            }
         }
         case '{': {
            if (!param1) {
               throw ERROR_INVALID_NODE.createWithContext(param0);
            }

            CompoundTag var2 = new TagParser(param0).readStruct();
            return new NbtPathArgument.MatchRootObjectNode(var2);
         }
         default: {
            String var2 = readUnquotedName(param0);
            return readObjectNode(param0, var2);
         }
      }
   }

   private static NbtPathArgument.Node readObjectNode(StringReader param0, String param1) throws CommandSyntaxException {
      if (param0.canRead() && param0.peek() == '{') {
         CompoundTag var2 = new TagParser(param0).readStruct();
         return new NbtPathArgument.MatchObjectNode(param1, var2);
      } else {
         return new NbtPathArgument.CompoundChildNode(param1);
      }
   }

   private static String readUnquotedName(StringReader param0) throws CommandSyntaxException {
      int var1 = param0.getCursor();

      while (param0.canRead() && isAllowedInUnquotedName(param0.peek())) {
         param0.skip();
      }

      if (param0.getCursor() == var1) {
         throw ERROR_INVALID_NODE.createWithContext(param0);
      } else {
         return param0.getString().substring(var1, param0.getCursor());
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static boolean isAllowedInUnquotedName(char param0) {
      return param0 != ' ' && param0 != '"' && param0 != '[' && param0 != ']' && param0 != '.' && param0 != '{' && param0 != '}';
   }

   static Predicate<Tag> createTagPredicate(CompoundTag param0) {
      return l_param1 -> NbtUtils.compareNbt(param0, l_param1, true);
   }

   static class AllElementsNode implements NbtPathArgument.Node {
      public static final NbtPathArgument.AllElementsNode INSTANCE = new NbtPathArgument.AllElementsNode();

      private AllElementsNode() {
      }

      @Override
      public void getTag(Tag param0, List<Tag> param1) {
         if (param0 instanceof CollectionTag) {
            param1.addAll((CollectionTag)param0);
         }
      }

      @Override
      public void getOrCreateTag(Tag param0, Supplier<Tag> param1, List<Tag> param2) {
         if (param0 instanceof CollectionTag<?> var4) {
            if (var4.isEmpty()) {
               Tag var5 = param1.get();
               if (var4.addTag(0, var5)) {
                  param2.add(var5);
               }
            } else {
               param2.addAll((Collection<? extends Tag>)var4);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag param0, Supplier<Tag> param1) {
         if (!(param0 instanceof CollectionTag<?> var3)) {
            return 0;
         } else {
            int var4 = var3.size();
            if (var4 == 0) {
               var3.addTag(0, param1.get());
               return 1;
            }

            Tag var5 = param1.get();
            int var6 = var4 - (int)var3.stream().filter(var5::equals).count();
            if (var6 == 0) {
               return 0;
            }

            var3.clear();
            if (!var3.addTag(0, var5)) {
               return 0;
            }

            for (int var7 = 1; var7 < var4; var7++) {
               var3.addTag(var7, param1.get());
            }

            return var6;
         }
      }

      @Override
      public int removeTag(Tag param0) {
         if (param0 instanceof CollectionTag<?> var2) {
            int var3 = var2.size();
            if (var3 > 0) {
               var2.clear();
               return var3;
            }
         }

         return 0;
      }
   }

   static class CompoundChildNode implements NbtPathArgument.Node {
      private final String name;

      public CompoundChildNode(String param0) {
         this.name = param0;
      }

      @Override
      public void getTag(Tag param0, List<Tag> param1) {
         if (param0 instanceof CompoundTag) {
            Tag var3 = ((CompoundTag)param0).get(this.name);
            if (var3 != null) {
               param1.add(var3);
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag param0, Supplier<Tag> param1, List<Tag> param2) {
         if (param0 instanceof CompoundTag var4) {
            Tag var5;
            if (var4.contains(this.name)) {
               var5 = var4.get(this.name);
            } else {
               var5 = param1.get();
               var4.put(this.name, var5);
            }

            param2.add(var5);
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag param0, Supplier<Tag> param1) {
         if (param0 instanceof CompoundTag var3) {
            Tag var4 = param1.get();
            Tag var5 = var3.put(this.name, var4);
            if (!var4.equals(var5)) {
               return 1;
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag param0) {
         if (param0 instanceof CompoundTag var2 && var2.contains(this.name)) {
            var2.remove(this.name);
            return 1;
         } else {
            return 0;
         }
      }
   }

   static class IndexedElementNode implements NbtPathArgument.Node {
      private final int index;

      public IndexedElementNode(int param0) {
         this.index = param0;
      }

      @Override
      public void getTag(Tag param0, List<Tag> param1) {
         if (param0 instanceof CollectionTag<?> var3) {
            int var4 = var3.size();
            int var5 = this.index < 0 ? var4 + this.index : this.index;
            if (0 <= var5 && var5 < var4) {
               param1.add(var3.get(var5));
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag param0, Supplier<Tag> param1, List<Tag> param2) {
         this.getTag(param0, param2);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag param0, Supplier<Tag> param1) {
         if (param0 instanceof CollectionTag<?> var3) {
            int var4 = var3.size();
            int var5 = this.index < 0 ? var4 + this.index : this.index;
            if (0 <= var5 && var5 < var4) {
               Tag var6 = var3.get(var5);
               Tag var7 = param1.get();
               if (!var7.equals(var6) && var3.setTag(var5, var7)) {
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag param0) {
         if (param0 instanceof CollectionTag<?> var2) {
            int var3 = var2.size();
            int var4 = this.index < 0 ? var3 + this.index : this.index;
            if (0 <= var4 && var4 < var3) {
               var2.remove(var4);
               return 1;
            }
         }

         return 0;
      }
   }

   static class MatchElementNode implements NbtPathArgument.Node {
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchElementNode(CompoundTag param0) {
         this.pattern = param0;
         this.predicate = NbtPathArgument.createTagPredicate(param0);
      }

      @Override
      public void getTag(Tag param0, List<Tag> param1) {
         if (param0 instanceof ListTag var3) {
            var3.stream().filter(this.predicate).forEach(param1::add);
         }
      }

      @Override
      public void getOrCreateTag(Tag param0, Supplier<Tag> param1, List<Tag> param2) {
         MutableBoolean var4 = new MutableBoolean();
         if (param0 instanceof ListTag var5) {
            var5.stream().filter(this.predicate).forEach(l_param2 -> {
               param2.add(l_param2);
               var4.setTrue();
            });
            if (var4.isFalse()) {
               CompoundTag var6 = this.pattern.copy();
               var5.add(var6);
               param2.add(var6);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag param0, Supplier<Tag> param1) {
         int var3 = 0;
         if (param0 instanceof ListTag var4) {
            int var5 = var4.size();
            if (var5 == 0) {
               var4.add(param1.get());
               var3++;
            } else {
               for (int var6 = 0; var6 < var5; var6++) {
                  Tag var7 = var4.get(var6);
                  if (this.predicate.test(var7)) {
                     Tag var8 = param1.get();
                     if (!var8.equals(var7) && var4.setTag(var6, var8)) {
                        var3++;
                     }
                  }
               }
            }
         }

         return var3;
      }

      @Override
      public int removeTag(Tag param0) {
         int var2 = 0;
         if (param0 instanceof ListTag var3) {
            for (int var4 = var3.size() - 1; var4 >= 0; var4--) {
               if (this.predicate.test(var3.get(var4))) {
                  var3.remove(var4);
                  var2++;
               }
            }
         }

         return var2;
      }
   }

   static class MatchObjectNode implements NbtPathArgument.Node {
      private final String name;
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchObjectNode(String param0, CompoundTag param1) {
         this.name = param0;
         this.pattern = param1;
         this.predicate = NbtPathArgument.createTagPredicate(param1);
      }

      @Override
      public void getTag(Tag param0, List<Tag> param1) {
         if (param0 instanceof CompoundTag) {
            Tag var3 = ((CompoundTag)param0).get(this.name);
            if (this.predicate.test(var3)) {
               param1.add(var3);
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag param0, Supplier<Tag> param1, List<Tag> param2) {
         if (param0 instanceof CompoundTag var4) {
            Tag var5 = var4.get(this.name);
            if (var5 == null) {
               var5 = this.pattern.copy();
               var4.put(this.name, var5);
               param2.add(var5);
            } else if (this.predicate.test(var5)) {
               param2.add(var5);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag param0, Supplier<Tag> param1) {
         if (param0 instanceof CompoundTag var3) {
            Tag var4 = var3.get(this.name);
            if (this.predicate.test(var4)) {
               Tag var5 = param1.get();
               if (!var5.equals(var4)) {
                  var3.put(this.name, var5);
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag param0) {
         if (param0 instanceof CompoundTag var2) {
            Tag var3 = var2.get(this.name);
            if (this.predicate.test(var3)) {
               var2.remove(this.name);
               return 1;
            }
         }

         return 0;
      }
   }

   static class MatchRootObjectNode implements NbtPathArgument.Node {
      private final Predicate<Tag> predicate;

      public MatchRootObjectNode(CompoundTag param0) {
         this.predicate = NbtPathArgument.createTagPredicate(param0);
      }

      @Override
      public void getTag(Tag param0, List<Tag> param1) {
         if (param0 instanceof CompoundTag && this.predicate.test(param0)) {
            param1.add(param0);
         }
      }

      @Override
      public void getOrCreateTag(Tag param0, Supplier<Tag> param1, List<Tag> param2) {
         this.getTag(param0, param2);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag param0, Supplier<Tag> param1) {
         return 0;
      }

      @Override
      public int removeTag(Tag param0) {
         return 0;
      }
   }

   public static class NbtPath {
      private final String original;
      private final Object2IntMap<NbtPathArgument.Node> nodeToOriginalPosition;
      private final NbtPathArgument.Node[] nodes;

      public NbtPath(String param0, NbtPathArgument.Node[] param1, Object2IntMap<NbtPathArgument.Node> param2) {
         this.original = param0;
         this.nodes = param1;
         this.nodeToOriginalPosition = param2;
      }

      public List<Tag> get(Tag param0) throws CommandSyntaxException {
         List<Tag> var2 = Collections.singletonList(param0);

         for (NbtPathArgument.Node var6 : this.nodes) {
            var2 = var6.get(var2);
            if (var2.isEmpty()) {
               throw this.createNotFoundException(var6);
            }
         }

         return var2;
      }

      public int countMatching(Tag param0) {
         List<Tag> var2 = Collections.singletonList(param0);

         for (NbtPathArgument.Node var6 : this.nodes) {
            var2 = var6.get(var2);
            if (var2.isEmpty()) {
               return 0;
            }
         }

         return var2.size();
      }

      private List<Tag> getOrCreateParents(Tag param0) throws CommandSyntaxException {
         List<Tag> var2 = Collections.singletonList(param0);

         for (int var3 = 0; var3 < this.nodes.length - 1; var3++) {
            NbtPathArgument.Node var4 = this.nodes[var3];
            int var5 = var3 + 1;
            var2 = var4.getOrCreate(var2, this.nodes[var5]::createPreferredParentTag);
            if (var2.isEmpty()) {
               throw this.createNotFoundException(var4);
            }
         }

         return var2;
      }

      public List<Tag> getOrCreate(Tag param0, Supplier<Tag> param1) throws CommandSyntaxException {
         List<Tag> var3 = this.getOrCreateParents(param0);
         NbtPathArgument.Node var4 = this.nodes[this.nodes.length - 1];
         return var4.getOrCreate(var3, param1);
      }

      private static int apply(List<Tag> param0, Function<Tag, Integer> param1) {
         return param0.stream().map(param1).reduce(0, (l_param0, l_param1) -> l_param0 + l_param1);
      }

      public static boolean isTooDeep(Tag param0, int param1) {
         if (param1 >= 512) {
            return true;
         }

         if (param0 instanceof CompoundTag var2) {
            for (String var5 : var2.getAllKeys()) {
               Tag var6 = var2.get(var5);
               if (var6 != null && isTooDeep(var6, param1 + 1)) {
                  return true;
               }
            }
         } else if (param0 instanceof ListTag) {
            for (Tag var5 : (ListTag)param0) {
               if (isTooDeep(var5, param1 + 1)) {
                  return true;
               }
            }
         }

         return false;
      }

      public int set(Tag param0, Tag param1) throws CommandSyntaxException {
         if (isTooDeep(param1, this.estimatePathDepth())) {
            throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
         }

         Tag var3 = param1.copy();
         List<Tag> var4 = this.getOrCreateParents(param0);
         if (var4.isEmpty()) {
            return 0;
         }

         NbtPathArgument.Node var5 = this.nodes[this.nodes.length - 1];
         MutableBoolean var6 = new MutableBoolean(false);
         return apply(var4, l_param3 -> var5.setTag(l_param3, () -> {
            if (var6.isFalse()) {
               var6.setTrue();
               return var3;
            } else {
               return var3.copy();
            }
         }));
      }

      private int estimatePathDepth() {
         return this.nodes.length;
      }

      public int insert(int param0, CompoundTag param1, List<Tag> param2) throws CommandSyntaxException {
         List<Tag> var4 = new ArrayList<>(param2.size());

         for (Tag var6 : param2) {
            Tag var7 = var6.copy();
            var4.add(var7);
            if (isTooDeep(var7, this.estimatePathDepth())) {
               throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
            }
         }

         Collection<Tag> var5 = this.getOrCreate(param1, ListTag::new);
         int var6 = 0;
         boolean var7 = false;

         for (Tag var9 : var5) {
            if (!(var9 instanceof CollectionTag<?> var10)) {
               throw NbtPathArgument.ERROR_EXPECTED_LIST.create(var9);
            }

            boolean var11 = false;
            int var12 = param0 < 0 ? var10.size() + param0 + 1 : param0;

            for (Tag var14 : var4) {
               try {
                  if (var10.addTag(var12, var7 ? var14.copy() : var14)) {
                     var12++;
                     var11 = true;
                  }
               } catch (IndexOutOfBoundsException var15) {
                  throw NbtPathArgument.ERROR_INVALID_INDEX.create(var12);
               }
            }

            var7 = true;
            var6 += var11 ? 1 : 0;
         }

         return var6;
      }

      public int remove(Tag param0) {
         List<Tag> var2 = Collections.singletonList(param0);

         for (int var3 = 0; var3 < this.nodes.length - 1; var3++) {
            var2 = this.nodes[var3].get(var2);
         }

         NbtPathArgument.Node var3 = this.nodes[this.nodes.length - 1];
         return apply(var2, var3::removeTag);
      }

      private CommandSyntaxException createNotFoundException(NbtPathArgument.Node param0) {
         int var2 = this.nodeToOriginalPosition.getInt(param0);
         return NbtPathArgument.ERROR_NOTHING_FOUND.create(this.original.substring(0, var2));
      }

      @Override
      public String toString() {
         return this.original;
      }
   }

   interface Node {
      void getTag(Tag var1, List<Tag> var2);

      void getOrCreateTag(Tag var1, Supplier<Tag> var2, List<Tag> var3);

      Tag createPreferredParentTag();

      int setTag(Tag var1, Supplier<Tag> var2);

      int removeTag(Tag var1);

      default List<Tag> get(List<Tag> param0) {
         return this.collect(param0, this::getTag);
      }

      default List<Tag> getOrCreate(List<Tag> param0, Supplier<Tag> param1) {
         return this.collect(param0, (l_param1, l_param2) -> this.getOrCreateTag(l_param1, param1, l_param2));
      }

      default List<Tag> collect(List<Tag> param0, BiConsumer<Tag, List<Tag>> param1) {
         List<Tag> var3 = Lists.newArrayList();

         for (Tag var5 : param0) {
            param1.accept(var5, var3);
         }

         return var3;
      }
   }
}
