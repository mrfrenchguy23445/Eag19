package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public class DataPackCommand {
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_PACK = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.datapack.unknown", param0)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_ENABLED = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.datapack.enable.failed", param0)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_DISABLED = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.datapack.disable.failed", param0)
   );
   private static final Dynamic2CommandExceptionType ERROR_PACK_FEATURES_NOT_ENABLED = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.datapack.enable.failed.no_flags", param0, param1)
   );
   private static final SuggestionProvider<CommandSourceStack> SELECTED_PACKS = (param0, param1) -> SharedSuggestionProvider.suggest(
      param0.getSource().getServer().getPackRepository().getSelectedIds().stream().map(StringArgumentType::escapeIfRequired), param1
   );
   private static final SuggestionProvider<CommandSourceStack> UNSELECTED_PACKS = (param0, param1) -> {
      PackRepository var2 = param0.getSource().getServer().getPackRepository();
      Collection<String> var3 = var2.getSelectedIds();
      FeatureFlagSet var4 = param0.getSource().enabledFeatures();
      return SharedSuggestionProvider.suggest(
         var2.getAvailablePacks()
            .stream()
            .filter(l_param1 -> l_param1.getRequestedFeatures().isSubsetOf(var4))
            .map(Pack::getId)
            .filter(l_param1 -> !var3.contains(l_param1))
            .map(StringArgumentType::escapeIfRequired),
         param1
      );
   };

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("datapack")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("enable")
                  .then(
                     Commands.argument("name", StringArgumentType.string())
                        .suggests(UNSELECTED_PACKS)
                        .executes(
                           l_param0 -> enablePack(
                              l_param0.getSource(),
                              getPack(l_param0, "name", true),
                              (l2_param0, l2_param1) -> l2_param1.getDefaultPosition().insert(l2_param0, l2_param1, l3_param0 -> l3_param0, false)
                           )
                        )
                        .then(
                           Commands.literal("after")
                              .then(
                                 Commands.argument("existing", StringArgumentType.string())
                                    .suggests(SELECTED_PACKS)
                                    .executes(
                                       l_param0 -> enablePack(
                                          l_param0.getSource(),
                                          getPack(l_param0, "name", true),
                                          (l2_param1, l2_param2) -> l2_param1.add(l2_param1.indexOf(getPack(l_param0, "existing", false)) + 1, l2_param2)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("before")
                              .then(
                                 Commands.argument("existing", StringArgumentType.string())
                                    .suggests(SELECTED_PACKS)
                                    .executes(
                                       l_param0 -> enablePack(
                                          l_param0.getSource(),
                                          getPack(l_param0, "name", true),
                                          (l2_param1, l2_param2) -> l2_param1.add(l2_param1.indexOf(getPack(l_param0, "existing", false)), l2_param2)
                                       )
                                    )
                              )
                        )
                        .then(Commands.literal("last").executes(l_param0 -> enablePack(l_param0.getSource(), getPack(l_param0, "name", true), List::add)))
                        .then(
                           Commands.literal("first")
                              .executes(
                                 l_param0 -> enablePack(
                                    l_param0.getSource(), getPack(l_param0, "name", true), (l2_param0, l2_param1) -> l2_param0.add(0, l2_param1)
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("disable")
                  .then(
                     Commands.argument("name", StringArgumentType.string())
                        .suggests(SELECTED_PACKS)
                        .executes(l_param0 -> disablePack(l_param0.getSource(), getPack(l_param0, "name", false)))
                  )
            )
            .then(
               Commands.literal("list")
                  .executes(l_param0 -> listPacks(l_param0.getSource()))
                  .then(Commands.literal("available").executes(l_param0 -> listAvailablePacks(l_param0.getSource())))
                  .then(Commands.literal("enabled").executes(l_param0 -> listEnabledPacks(l_param0.getSource())))
            )
      );
   }

   private static int enablePack(CommandSourceStack param0, Pack param1, DataPackCommand.Inserter param2) throws CommandSyntaxException {
      PackRepository var3 = param0.getServer().getPackRepository();
      List<Pack> var4 = Lists.newArrayList(var3.getSelectedPacks());
      param2.apply(var4, param1);
      param0.sendSuccess(Component.translatable("commands.datapack.modify.enable", param1.getChatLink(true)), true);
      ReloadCommand.reloadPacks(var4.stream().map(Pack::getId).collect(Collectors.toList()), param0);
      return var4.size();
   }

   private static int disablePack(CommandSourceStack param0, Pack param1) {
      PackRepository var2 = param0.getServer().getPackRepository();
      List<Pack> var3 = Lists.newArrayList(var2.getSelectedPacks());
      var3.remove(param1);
      param0.sendSuccess(Component.translatable("commands.datapack.modify.disable", param1.getChatLink(true)), true);
      ReloadCommand.reloadPacks(var3.stream().map(Pack::getId).collect(Collectors.toList()), param0);
      return var3.size();
   }

   private static int listPacks(CommandSourceStack param0) {
      return listEnabledPacks(param0) + listAvailablePacks(param0);
   }

   private static int listAvailablePacks(CommandSourceStack param0) {
      PackRepository var1 = param0.getServer().getPackRepository();
      var1.reload();
      Collection<Pack> var2 = var1.getSelectedPacks();
      Collection<Pack> var3 = var1.getAvailablePacks();
      FeatureFlagSet var4 = param0.enabledFeatures();
      List<Pack> var5 = var3.stream().filter(l_param2 -> !var2.contains(l_param2) && l_param2.getRequestedFeatures().isSubsetOf(var4)).toList();
      if (var5.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.datapack.list.available.none"), false);
      } else {
         param0.sendSuccess(
            Component.translatable(
               "commands.datapack.list.available.success", var5.size(), ComponentUtils.formatList(var5, l_param0 -> l_param0.getChatLink(false))
            ),
            false
         );
      }

      return var5.size();
   }

   private static int listEnabledPacks(CommandSourceStack param0) {
      PackRepository var1 = param0.getServer().getPackRepository();
      var1.reload();
      Collection<? extends Pack> var2 = var1.getSelectedPacks();
      if (var2.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.datapack.list.enabled.none"), false);
      } else {
         param0.sendSuccess(
            Component.translatable(
               "commands.datapack.list.enabled.success", var2.size(), ComponentUtils.formatList(var2, l_param0 -> l_param0.getChatLink(true))
            ),
            false
         );
      }

      return var2.size();
   }

   private static Pack getPack(CommandContext<CommandSourceStack> param0, String param1, boolean param2) throws CommandSyntaxException {
      String var3 = StringArgumentType.getString(param0, param1);
      PackRepository var4 = param0.getSource().getServer().getPackRepository();
      Pack var5 = var4.getPack(var3);
      if (var5 == null) {
         throw ERROR_UNKNOWN_PACK.create(var3);
      } else {
         boolean var6 = var4.getSelectedPacks().contains(var5);
         if (param2 && var6) {
            throw ERROR_PACK_ALREADY_ENABLED.create(var3);
         } else if (!param2 && !var6) {
            throw ERROR_PACK_ALREADY_DISABLED.create(var3);
         } else {
            FeatureFlagSet var7 = param0.getSource().enabledFeatures();
            FeatureFlagSet var8 = var5.getRequestedFeatures();
            if (!var8.isSubsetOf(var7)) {
               throw ERROR_PACK_FEATURES_NOT_ENABLED.create(var3, FeatureFlags.printMissingFlags(var7, var8));
            } else {
               return var5;
            }
         }
      }
   }

   interface Inserter {
      void apply(List<Pack> var1, Pack var2) throws CommandSyntaxException;
   }
}
