package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
   static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
      (param0, param1, param2) -> Component.translatable("commands.item.target.not_a_container", param0, param1, param2)
   );
   private static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
      (param0, param1, param2) -> Component.translatable("commands.item.source.not_a_container", param0, param1, param2)
   );
   static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.item.target.no_such_slot", param0)
   );
   private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.item.source.no_such_slot", param0)
   );
   private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.item.target.no_changes", param0)
   );
   private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.item.target.no_changed.known_item", param0, param1)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_MODIFIER = (param0, param1) -> {
      ItemModifierManager var2 = param0.getSource().getServer().getItemModifierManager();
      return SharedSuggestionProvider.suggestResource(var2.getKeys(), param1);
   };

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("item")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("replace")
                  .then(
                     Commands.literal("block")
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .then(
                                 Commands.argument("slot", SlotArgument.slot())
                                    .then(
                                       Commands.literal("with")
                                          .then(
                                             Commands.argument("item", ItemArgument.item(param1))
                                                .executes(
                                                   l_param0 -> setBlockItem(
                                                      l_param0.getSource(),
                                                      BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                      SlotArgument.getSlot(l_param0, "slot"),
                                                      ItemArgument.getItem(l_param0, "item").createItemStack(1, false)
                                                   )
                                                )
                                                .then(
                                                   Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                      .executes(
                                                         l_param0 -> setBlockItem(
                                                            l_param0.getSource(),
                                                            BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                            SlotArgument.getSlot(l_param0, "slot"),
                                                            ItemArgument.getItem(l_param0, "item")
                                                               .createItemStack(IntegerArgumentType.getInteger(l_param0, "count"), true)
                                                         )
                                                      )
                                                )
                                          )
                                    )
                                    .then(
                                       Commands.literal("from")
                                          .then(
                                             Commands.literal("block")
                                                .then(
                                                   Commands.argument("source", BlockPosArgument.blockPos())
                                                      .then(
                                                         Commands.argument("sourceSlot", SlotArgument.slot())
                                                            .executes(
                                                               l_param0 -> blockToBlock(
                                                                  l_param0.getSource(),
                                                                  BlockPosArgument.getLoadedBlockPos(l_param0, "source"),
                                                                  SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                  BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                                  SlotArgument.getSlot(l_param0, "slot")
                                                               )
                                                            )
                                                            .then(
                                                               Commands.argument("modifier", ResourceLocationArgument.id())
                                                                  .suggests(SUGGEST_MODIFIER)
                                                                  .executes(
                                                                     l_param0 -> blockToBlock(
                                                                        l_param0.getSource(),
                                                                        BlockPosArgument.getLoadedBlockPos(l_param0, "source"),
                                                                        SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                        BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                                        SlotArgument.getSlot(l_param0, "slot"),
                                                                        ResourceLocationArgument.getItemModifier(l_param0, "modifier")
                                                                     )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                          .then(
                                             Commands.literal("entity")
                                                .then(
                                                   Commands.argument("source", EntityArgument.entity())
                                                      .then(
                                                         Commands.argument("sourceSlot", SlotArgument.slot())
                                                            .executes(
                                                               l_param0 -> entityToBlock(
                                                                  l_param0.getSource(),
                                                                  EntityArgument.getEntity(l_param0, "source"),
                                                                  SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                  BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                                  SlotArgument.getSlot(l_param0, "slot")
                                                               )
                                                            )
                                                            .then(
                                                               Commands.argument("modifier", ResourceLocationArgument.id())
                                                                  .suggests(SUGGEST_MODIFIER)
                                                                  .executes(
                                                                     l_param0 -> entityToBlock(
                                                                        l_param0.getSource(),
                                                                        EntityArgument.getEntity(l_param0, "source"),
                                                                        SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                        BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                                        SlotArgument.getSlot(l_param0, "slot"),
                                                                        ResourceLocationArgument.getItemModifier(l_param0, "modifier")
                                                                     )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("targets", EntityArgument.entities())
                              .then(
                                 Commands.argument("slot", SlotArgument.slot())
                                    .then(
                                       Commands.literal("with")
                                          .then(
                                             Commands.argument("item", ItemArgument.item(param1))
                                                .executes(
                                                   l_param0 -> setEntityItem(
                                                      l_param0.getSource(),
                                                      EntityArgument.getEntities(l_param0, "targets"),
                                                      SlotArgument.getSlot(l_param0, "slot"),
                                                      ItemArgument.getItem(l_param0, "item").createItemStack(1, false)
                                                   )
                                                )
                                                .then(
                                                   Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                      .executes(
                                                         l_param0 -> setEntityItem(
                                                            l_param0.getSource(),
                                                            EntityArgument.getEntities(l_param0, "targets"),
                                                            SlotArgument.getSlot(l_param0, "slot"),
                                                            ItemArgument.getItem(l_param0, "item")
                                                               .createItemStack(IntegerArgumentType.getInteger(l_param0, "count"), true)
                                                         )
                                                      )
                                                )
                                          )
                                    )
                                    .then(
                                       Commands.literal("from")
                                          .then(
                                             Commands.literal("block")
                                                .then(
                                                   Commands.argument("source", BlockPosArgument.blockPos())
                                                      .then(
                                                         Commands.argument("sourceSlot", SlotArgument.slot())
                                                            .executes(
                                                               l_param0 -> blockToEntities(
                                                                  l_param0.getSource(),
                                                                  BlockPosArgument.getLoadedBlockPos(l_param0, "source"),
                                                                  SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                  EntityArgument.getEntities(l_param0, "targets"),
                                                                  SlotArgument.getSlot(l_param0, "slot")
                                                               )
                                                            )
                                                            .then(
                                                               Commands.argument("modifier", ResourceLocationArgument.id())
                                                                  .suggests(SUGGEST_MODIFIER)
                                                                  .executes(
                                                                     l_param0 -> blockToEntities(
                                                                        l_param0.getSource(),
                                                                        BlockPosArgument.getLoadedBlockPos(l_param0, "source"),
                                                                        SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                        EntityArgument.getEntities(l_param0, "targets"),
                                                                        SlotArgument.getSlot(l_param0, "slot"),
                                                                        ResourceLocationArgument.getItemModifier(l_param0, "modifier")
                                                                     )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                          .then(
                                             Commands.literal("entity")
                                                .then(
                                                   Commands.argument("source", EntityArgument.entity())
                                                      .then(
                                                         Commands.argument("sourceSlot", SlotArgument.slot())
                                                            .executes(
                                                               l_param0 -> entityToEntities(
                                                                  l_param0.getSource(),
                                                                  EntityArgument.getEntity(l_param0, "source"),
                                                                  SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                  EntityArgument.getEntities(l_param0, "targets"),
                                                                  SlotArgument.getSlot(l_param0, "slot")
                                                               )
                                                            )
                                                            .then(
                                                               Commands.argument("modifier", ResourceLocationArgument.id())
                                                                  .suggests(SUGGEST_MODIFIER)
                                                                  .executes(
                                                                     l_param0 -> entityToEntities(
                                                                        l_param0.getSource(),
                                                                        EntityArgument.getEntity(l_param0, "source"),
                                                                        SlotArgument.getSlot(l_param0, "sourceSlot"),
                                                                        EntityArgument.getEntities(l_param0, "targets"),
                                                                        SlotArgument.getSlot(l_param0, "slot"),
                                                                        ResourceLocationArgument.getItemModifier(l_param0, "modifier")
                                                                     )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("modify")
                  .then(
                     Commands.literal("block")
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .then(
                                 Commands.argument("slot", SlotArgument.slot())
                                    .then(
                                       Commands.argument("modifier", ResourceLocationArgument.id())
                                          .suggests(SUGGEST_MODIFIER)
                                          .executes(
                                             l_param0 -> modifyBlockItem(
                                                l_param0.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                SlotArgument.getSlot(l_param0, "slot"),
                                                ResourceLocationArgument.getItemModifier(l_param0, "modifier")
                                             )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("targets", EntityArgument.entities())
                              .then(
                                 Commands.argument("slot", SlotArgument.slot())
                                    .then(
                                       Commands.argument("modifier", ResourceLocationArgument.id())
                                          .suggests(SUGGEST_MODIFIER)
                                          .executes(
                                             l_param0 -> modifyEntityItem(
                                                l_param0.getSource(),
                                                EntityArgument.getEntities(l_param0, "targets"),
                                                SlotArgument.getSlot(l_param0, "slot"),
                                                ResourceLocationArgument.getItemModifier(l_param0, "modifier")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int modifyBlockItem(CommandSourceStack param0, BlockPos param1, int param2, LootItemFunction param3) throws CommandSyntaxException {
      Container var4 = getContainer(param0, param1, ERROR_TARGET_NOT_A_CONTAINER);
      if (param2 >= 0 && param2 < var4.getContainerSize()) {
         ItemStack var5 = applyModifier(param0, param3, var4.getItem(param2));
         var4.setItem(param2, var5);
         param0.sendSuccess(Component.translatable("commands.item.block.set.success", param1.getX(), param1.getY(), param1.getZ(), var5.getDisplayName()), true);
         return 1;
      } else {
         throw ERROR_TARGET_INAPPLICABLE_SLOT.create(param2);
      }
   }

   private static int modifyEntityItem(CommandSourceStack param0, Collection<? extends Entity> param1, int param2, LootItemFunction param3) throws CommandSyntaxException {
      Map<Entity, ItemStack> var4 = Maps.newHashMapWithExpectedSize(param1.size());

      for (Entity var6 : param1) {
         SlotAccess var7 = var6.getSlot(param2);
         if (var7 != SlotAccess.NULL) {
            ItemStack var8 = applyModifier(param0, param3, var7.get().copy());
            if (var7.set(var8)) {
               var4.put(var6, var8);
               if (var6 instanceof ServerPlayer) {
                  ((ServerPlayer)var6).containerMenu.broadcastChanges();
               }
            }
         }
      }

      if (var4.isEmpty()) {
         throw ERROR_TARGET_NO_CHANGES.create(param2);
      }

      if (var4.size() == 1) {
         Entry<Entity, ItemStack> var5 = var4.entrySet().iterator().next();
         param0.sendSuccess(
            Component.translatable("commands.item.entity.set.success.single", var5.getKey().getDisplayName(), var5.getValue().getDisplayName()), true
         );
      } else {
         param0.sendSuccess(Component.translatable("commands.item.entity.set.success.multiple", var4.size()), true);
      }

      return var4.size();
   }

   private static int setBlockItem(CommandSourceStack param0, BlockPos param1, int param2, ItemStack param3) throws CommandSyntaxException {
      Container var4 = getContainer(param0, param1, ERROR_TARGET_NOT_A_CONTAINER);
      if (param2 >= 0 && param2 < var4.getContainerSize()) {
         var4.setItem(param2, param3);
         param0.sendSuccess(
            Component.translatable("commands.item.block.set.success", param1.getX(), param1.getY(), param1.getZ(), param3.getDisplayName()), true
         );
         return 1;
      } else {
         throw ERROR_TARGET_INAPPLICABLE_SLOT.create(param2);
      }
   }

   private static Container getContainer(CommandSourceStack param0, BlockPos param1, Dynamic3CommandExceptionType param2) throws CommandSyntaxException {
      BlockEntity var3 = param0.getLevel().getBlockEntity(param1);
      if (!(var3 instanceof Container)) {
         throw param2.create(param1.getX(), param1.getY(), param1.getZ());
      } else {
         return (Container)var3;
      }
   }

   private static int setEntityItem(CommandSourceStack param0, Collection<? extends Entity> param1, int param2, ItemStack param3) throws CommandSyntaxException {
      List<Entity> var4 = Lists.newArrayListWithCapacity(param1.size());

      for (Entity var6 : param1) {
         SlotAccess var7 = var6.getSlot(param2);
         if (var7 != SlotAccess.NULL && var7.set(param3.copy())) {
            var4.add(var6);
            if (var6 instanceof ServerPlayer) {
               ((ServerPlayer)var6).containerMenu.broadcastChanges();
            }
         }
      }

      if (var4.isEmpty()) {
         throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(param3.getDisplayName(), param2);
      }

      if (var4.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.item.entity.set.success.single", var4.iterator().next().getDisplayName(), param3.getDisplayName()), true
         );
      } else {
         param0.sendSuccess(Component.translatable("commands.item.entity.set.success.multiple", var4.size(), param3.getDisplayName()), true);
      }

      return var4.size();
   }

   private static int blockToEntities(CommandSourceStack param0, BlockPos param1, int param2, Collection<? extends Entity> param3, int param4) throws CommandSyntaxException {
      return setEntityItem(param0, param3, param4, getBlockItem(param0, param1, param2));
   }

   private static int blockToEntities(
      CommandSourceStack param0, BlockPos param1, int param2, Collection<? extends Entity> param3, int param4, LootItemFunction param5
   ) throws CommandSyntaxException {
      return setEntityItem(param0, param3, param4, applyModifier(param0, param5, getBlockItem(param0, param1, param2)));
   }

   private static int blockToBlock(CommandSourceStack param0, BlockPos param1, int param2, BlockPos param3, int param4) throws CommandSyntaxException {
      return setBlockItem(param0, param3, param4, getBlockItem(param0, param1, param2));
   }

   private static int blockToBlock(CommandSourceStack param0, BlockPos param1, int param2, BlockPos param3, int param4, LootItemFunction param5) throws CommandSyntaxException {
      return setBlockItem(param0, param3, param4, applyModifier(param0, param5, getBlockItem(param0, param1, param2)));
   }

   private static int entityToBlock(CommandSourceStack param0, Entity param1, int param2, BlockPos param3, int param4) throws CommandSyntaxException {
      return setBlockItem(param0, param3, param4, getEntityItem(param1, param2));
   }

   private static int entityToBlock(CommandSourceStack param0, Entity param1, int param2, BlockPos param3, int param4, LootItemFunction param5) throws CommandSyntaxException {
      return setBlockItem(param0, param3, param4, applyModifier(param0, param5, getEntityItem(param1, param2)));
   }

   private static int entityToEntities(CommandSourceStack param0, Entity param1, int param2, Collection<? extends Entity> param3, int param4) throws CommandSyntaxException {
      return setEntityItem(param0, param3, param4, getEntityItem(param1, param2));
   }

   private static int entityToEntities(
      CommandSourceStack param0, Entity param1, int param2, Collection<? extends Entity> param3, int param4, LootItemFunction param5
   ) throws CommandSyntaxException {
      return setEntityItem(param0, param3, param4, applyModifier(param0, param5, getEntityItem(param1, param2)));
   }

   private static ItemStack applyModifier(CommandSourceStack param0, LootItemFunction param1, ItemStack param2) {
      ServerLevel var3 = param0.getLevel();
      LootContext.Builder var4 = new LootContext.Builder(var3)
         .withParameter(LootContextParams.ORIGIN, param0.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, param0.getEntity());
      return param1.apply(param2, var4.create(LootContextParamSets.COMMAND));
   }

   private static ItemStack getEntityItem(Entity param0, int param1) throws CommandSyntaxException {
      SlotAccess var2 = param0.getSlot(param1);
      if (var2 == SlotAccess.NULL) {
         throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(param1);
      } else {
         return var2.get().copy();
      }
   }

   private static ItemStack getBlockItem(CommandSourceStack param0, BlockPos param1, int param2) throws CommandSyntaxException {
      Container var3 = getContainer(param0, param1, ERROR_SOURCE_NOT_A_CONTAINER);
      if (param2 >= 0 && param2 < var3.getContainerSize()) {
         return var3.getItem(param2).copy();
      } else {
         throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(param2);
      }
   }
}
