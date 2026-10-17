package net.minecraft.server.commands.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DataCommands {
   private static final SimpleCommandExceptionType ERROR_MERGE_UNCHANGED = new SimpleCommandExceptionType(Component.translatable("commands.data.merge.failed"));
   private static final DynamicCommandExceptionType ERROR_GET_NOT_NUMBER = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.data.get.invalid", param0)
   );
   private static final DynamicCommandExceptionType ERROR_GET_NON_EXISTENT = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.data.get.unknown", param0)
   );
   private static final SimpleCommandExceptionType ERROR_MULTIPLE_TAGS = new SimpleCommandExceptionType(Component.translatable("commands.data.get.multiple"));
   private static final DynamicCommandExceptionType ERROR_EXPECTED_OBJECT = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.data.modify.expected_object", param0)
   );
   private static final DynamicCommandExceptionType ERROR_EXPECTED_VALUE = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.data.modify.expected_value", param0)
   );
   public static final List<Function<String, DataCommands.DataProvider>> ALL_PROVIDERS = ImmutableList.of(
      EntityDataAccessor.PROVIDER, BlockDataAccessor.PROVIDER, StorageDataAccessor.PROVIDER
   );
   public static final List<DataCommands.DataProvider> TARGET_PROVIDERS = ALL_PROVIDERS.stream()
      .map(param0 -> param0.apply("target"))
      .collect(ImmutableList.toImmutableList());
   public static final List<DataCommands.DataProvider> SOURCE_PROVIDERS = ALL_PROVIDERS.stream()
      .map(param0 -> param0.apply("source"))
      .collect(ImmutableList.toImmutableList());

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      LiteralArgumentBuilder<CommandSourceStack> var1 = Commands.literal("data").requires(l_param0 -> l_param0.hasPermission(2));

      for (DataCommands.DataProvider var3 : TARGET_PROVIDERS) {
         var1.then(
               var3.wrap(
                  Commands.literal("merge"),
                  l_param1 -> l_param1.then(
                     Commands.argument("nbt", CompoundTagArgument.compoundTag())
                        .executes(l2_param1 -> mergeData(l2_param1.getSource(), var3.access(l2_param1), CompoundTagArgument.getCompoundTag(l2_param1, "nbt")))
                  )
               )
            )
            .then(
               var3.wrap(
                  Commands.literal("get"),
                  l_param1 -> l_param1.executes(l2_param1 -> getData(l2_param1.getSource(), var3.access(l2_param1)))
                     .then(
                        Commands.argument("path", NbtPathArgument.nbtPath())
                           .executes(l2_param1 -> getData(l2_param1.getSource(), var3.access(l2_param1), NbtPathArgument.getPath(l2_param1, "path")))
                           .then(
                              Commands.argument("scale", DoubleArgumentType.doubleArg())
                                 .executes(
                                    l2_param1 -> getNumeric(
                                       l2_param1.getSource(),
                                       var3.access(l2_param1),
                                       NbtPathArgument.getPath(l2_param1, "path"),
                                       DoubleArgumentType.getDouble(l2_param1, "scale")
                                    )
                                 )
                           )
                     )
               )
            )
            .then(
               var3.wrap(
                  Commands.literal("remove"),
                  l_param1 -> l_param1.then(
                     Commands.argument("path", NbtPathArgument.nbtPath())
                        .executes(l2_param1 -> removeData(l2_param1.getSource(), var3.access(l2_param1), NbtPathArgument.getPath(l2_param1, "path")))
                  )
               )
            )
            .then(
               decorateModification(
                  (l_param0, l_param1) -> l_param0.then(
                        Commands.literal("insert")
                           .then(
                              Commands.argument("index", IntegerArgumentType.integer())
                                 .then(
                                    l_param1.create(
                                       (l2_param0, l2_param1, l2_param2, l2_param3) -> l2_param2.insert(
                                          IntegerArgumentType.getInteger(l2_param0, "index"), l2_param1, l2_param3
                                       )
                                    )
                                 )
                           )
                     )
                     .then(
                        Commands.literal("prepend")
                           .then(l_param1.create((l2_param0, l2_param1, l2_param2, l2_param3) -> l2_param2.insert(0, l2_param1, l2_param3)))
                     )
                     .then(
                        Commands.literal("append")
                           .then(l_param1.create((l2_param0, l2_param1, l2_param2, l2_param3) -> l2_param2.insert(-1, l2_param1, l2_param3)))
                     )
                     .then(
                        Commands.literal("set")
                           .then(l_param1.create((l2_param0, l2_param1, l2_param2, l2_param3) -> l2_param2.set(l2_param1, Iterables.getLast(l2_param3))))
                     )
                     .then(Commands.literal("merge").then(l_param1.create((l2_param0, l2_param1, l2_param2, l2_param3) -> {
                        CompoundTag l2_var4 = new CompoundTag();

                        for (Tag l2_var6 : l2_param3) {
                           if (NbtPathArgument.NbtPath.isTooDeep(l2_var6, 0)) {
                              throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
                           }

                           if (!(l2_var6 instanceof CompoundTag l2_var7)) {
                              throw ERROR_EXPECTED_OBJECT.create(l2_var6);
                           }

                           l2_var4.merge(l2_var7);
                        }

                        Collection<Tag> l2_var5 = l2_param2.getOrCreate(l2_param1, CompoundTag::new);
                        int l2_var6 = 0;

                        for (Tag l2_var8 : l2_var5) {
                           if (!(l2_var8 instanceof CompoundTag l2_var9)) {
                              throw ERROR_EXPECTED_OBJECT.create(l2_var8);
                           }

                           CompoundTag l2_var10 = l2_var9.copy();
                           l2_var9.merge(l2_var4);
                           l2_var6 += l2_var10.equals(l2_var9) ? 0 : 1;
                        }

                        return l2_var6;
                     })))
               )
            );
      }

      param0.register(var1);
   }

   private static String getAsText(Tag param0) throws CommandSyntaxException {
      if (param0.getType().isValue()) {
         return param0.getAsString();
      } else {
         throw ERROR_EXPECTED_VALUE.create(param0);
      }
   }

   private static List<Tag> stringifyTagList(List<Tag> param0, Function<String, String> param1) throws CommandSyntaxException {
      List<Tag> var2 = new ArrayList<>(param0.size());

      for (Tag var4 : param0) {
         String var5 = getAsText(var4);
         var2.add(StringTag.valueOf(param1.apply(var5)));
      }

      return var2;
   }

   private static ArgumentBuilder<CommandSourceStack, ?> decorateModification(
      BiConsumer<ArgumentBuilder<CommandSourceStack, ?>, DataCommands.DataManipulatorDecorator> param0
   ) {
      LiteralArgumentBuilder<CommandSourceStack> var1 = Commands.literal("modify");

      for (DataCommands.DataProvider var3 : TARGET_PROVIDERS) {
         var3.wrap(
            var1,
            l_param2 -> {
               ArgumentBuilder<CommandSourceStack, ?> l_var3 = Commands.argument("targetPath", NbtPathArgument.nbtPath());

               for (DataCommands.DataProvider l_var5 : SOURCE_PROVIDERS) {
                  param0.accept(
                     l_var3,
                     l2_param2 -> l_var5.wrap(
                        Commands.literal("from"),
                        l3_param3 -> l3_param3.executes(l4_param3 -> manipulateData(l4_param3, var3, l2_param2, getSingletonSource(l4_param3, l_var5)))
                           .then(
                              Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                 .executes(l4_param3 -> manipulateData(l4_param3, var3, l2_param2, resolveSourcePath(l4_param3, l_var5)))
                           )
                     )
                  );
                  param0.accept(
                     l_var3,
                     l2_param2 -> l_var5.wrap(
                        Commands.literal("string"),
                        l3_param3 -> l3_param3.executes(
                              l4_param3 -> manipulateData(
                                 l4_param3, var3, l2_param2, stringifyTagList(getSingletonSource(l4_param3, l_var5), l5_param0 -> l5_param0)
                              )
                           )
                           .then(
                              Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                 .executes(
                                    l4_param3 -> manipulateData(
                                       l4_param3, var3, l2_param2, stringifyTagList(resolveSourcePath(l4_param3, l_var5), l5_param0 -> l5_param0)
                                    )
                                 )
                                 .then(
                                    Commands.argument("start", IntegerArgumentType.integer(0))
                                       .executes(
                                          l4_param3 -> manipulateData(
                                             l4_param3,
                                             var3,
                                             l2_param2,
                                             stringifyTagList(
                                                resolveSourcePath(l4_param3, l_var5),
                                                l5_param1 -> l5_param1.substring(IntegerArgumentType.getInteger(l4_param3, "start"))
                                             )
                                          )
                                       )
                                       .then(
                                          Commands.argument("end", IntegerArgumentType.integer(0))
                                             .executes(
                                                l4_param3 -> manipulateData(
                                                   l4_param3,
                                                   var3,
                                                   l2_param2,
                                                   stringifyTagList(
                                                      resolveSourcePath(l4_param3, l_var5),
                                                      l5_param1 -> l5_param1.substring(
                                                         IntegerArgumentType.getInteger(l4_param3, "start"), IntegerArgumentType.getInteger(l4_param3, "end")
                                                      )
                                                   )
                                                )
                                             )
                                       )
                                 )
                           )
                     )
                  );
               }

               param0.accept(l_var3, l2_param1 -> Commands.literal("value").then(Commands.argument("value", NbtTagArgument.nbtTag()).executes(l3_param2 -> {
                  List<Tag> l3_var3 = Collections.singletonList(NbtTagArgument.getNbtTag(l3_param2, "value"));
                  return manipulateData(l3_param2, var3, l2_param1, l3_var3);
               })));
               return l_param2.then(l_var3);
            }
         );
      }

      return var1;
   }

   private static List<Tag> getSingletonSource(CommandContext<CommandSourceStack> param0, DataCommands.DataProvider param1) throws CommandSyntaxException {
      DataAccessor var2 = param1.access(param0);
      return Collections.singletonList(var2.getData());
   }

   private static List<Tag> resolveSourcePath(CommandContext<CommandSourceStack> param0, DataCommands.DataProvider param1) throws CommandSyntaxException {
      DataAccessor var2 = param1.access(param0);
      NbtPathArgument.NbtPath var3 = NbtPathArgument.getPath(param0, "sourcePath");
      return var3.get(var2.getData());
   }

   private static int manipulateData(
      CommandContext<CommandSourceStack> param0, DataCommands.DataProvider param1, DataCommands.DataManipulator param2, List<Tag> param3
   ) throws CommandSyntaxException {
      DataAccessor var4 = param1.access(param0);
      NbtPathArgument.NbtPath var5 = NbtPathArgument.getPath(param0, "targetPath");
      CompoundTag var6 = var4.getData();
      int var7 = param2.modify(param0, var6, var5, param3);
      if (var7 == 0) {
         throw ERROR_MERGE_UNCHANGED.create();
      }

      var4.setData(var6);
      param0.getSource().sendSuccess(var4.getModifiedSuccess(), true);
      return var7;
   }

   private static int removeData(CommandSourceStack param0, DataAccessor param1, NbtPathArgument.NbtPath param2) throws CommandSyntaxException {
      CompoundTag var3 = param1.getData();
      int var4 = param2.remove(var3);
      if (var4 == 0) {
         throw ERROR_MERGE_UNCHANGED.create();
      }

      param1.setData(var3);
      param0.sendSuccess(param1.getModifiedSuccess(), true);
      return var4;
   }

   private static Tag getSingleTag(NbtPathArgument.NbtPath param0, DataAccessor param1) throws CommandSyntaxException {
      Collection<Tag> var2 = param0.get(param1.getData());
      Iterator<Tag> var3 = var2.iterator();
      Tag var4 = var3.next();
      if (var3.hasNext()) {
         throw ERROR_MULTIPLE_TAGS.create();
      } else {
         return var4;
      }
   }

   private static int getData(CommandSourceStack param0, DataAccessor param1, NbtPathArgument.NbtPath param2) throws CommandSyntaxException {
      Tag var3 = getSingleTag(param2, param1);
      int var4;
      if (var3 instanceof NumericTag) {
         var4 = Mth.floor(((NumericTag)var3).getAsDouble());
      } else if (var3 instanceof CollectionTag) {
         var4 = ((CollectionTag)var3).size();
      } else if (var3 instanceof CompoundTag) {
         var4 = ((CompoundTag)var3).size();
      } else {
         if (!(var3 instanceof StringTag)) {
            throw ERROR_GET_NON_EXISTENT.create(param2.toString());
         }

         var4 = var3.getAsString().length();
      }

      param0.sendSuccess(param1.getPrintSuccess(var3), false);
      return var4;
   }

   private static int getNumeric(CommandSourceStack param0, DataAccessor param1, NbtPathArgument.NbtPath param2, double param3) throws CommandSyntaxException {
      Tag var5 = getSingleTag(param2, param1);
      if (!(var5 instanceof NumericTag)) {
         throw ERROR_GET_NOT_NUMBER.create(param2.toString());
      }

      int var6 = Mth.floor(((NumericTag)var5).getAsDouble() * param3);
      param0.sendSuccess(param1.getPrintSuccess(param2, param3, var6), false);
      return var6;
   }

   private static int getData(CommandSourceStack param0, DataAccessor param1) throws CommandSyntaxException {
      param0.sendSuccess(param1.getPrintSuccess(param1.getData()), false);
      return 1;
   }

   private static int mergeData(CommandSourceStack param0, DataAccessor param1, CompoundTag param2) throws CommandSyntaxException {
      CompoundTag var3 = param1.getData();
      if (NbtPathArgument.NbtPath.isTooDeep(param2, 0)) {
         throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
      }

      CompoundTag var4 = var3.copy().merge(param2);
      if (var3.equals(var4)) {
         throw ERROR_MERGE_UNCHANGED.create();
      }

      param1.setData(var4);
      param0.sendSuccess(param1.getModifiedSuccess(), true);
      return 1;
   }

   interface DataManipulator {
      int modify(CommandContext<CommandSourceStack> var1, CompoundTag var2, NbtPathArgument.NbtPath var3, List<Tag> var4) throws CommandSyntaxException;
   }

   interface DataManipulatorDecorator {
      ArgumentBuilder<CommandSourceStack, ?> create(DataCommands.DataManipulator var1);
   }

   public interface DataProvider {
      DataAccessor access(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

      ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> var1, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> var2
      );
   }
}
