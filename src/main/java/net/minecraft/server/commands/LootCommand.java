package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootCommand {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_LOOT_TABLE = (param0, param1) -> {
      LootTables var2 = param0.getSource().getServer().getLootTables();
      return SharedSuggestionProvider.suggestResource(var2.getIds(), param1);
   };
   private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.drop.no_held_items", param0)
   );
   private static final DynamicCommandExceptionType ERROR_NO_LOOT_TABLE = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.drop.no_loot_table", param0)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         addTargets(
            Commands.literal("loot").requires(l_param0 -> l_param0.hasPermission(2)),
            (l_param1, l_param2) -> l_param1.then(
                  Commands.literal("fish")
                     .then(
                        Commands.argument("loot_table", ResourceLocationArgument.id())
                           .suggests(SUGGEST_LOOT_TABLE)
                           .then(
                              Commands.argument("pos", BlockPosArgument.blockPos())
                                 .executes(
                                    l2_param1 -> dropFishingLoot(
                                       l2_param1,
                                       ResourceLocationArgument.getId(l2_param1, "loot_table"),
                                       BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                       ItemStack.EMPTY,
                                       l_param2
                                    )
                                 )
                                 .then(
                                    Commands.argument("tool", ItemArgument.item(param1))
                                       .executes(
                                          l2_param1 -> dropFishingLoot(
                                             l2_param1,
                                             ResourceLocationArgument.getId(l2_param1, "loot_table"),
                                             BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                             ItemArgument.getItem(l2_param1, "tool").createItemStack(1, false),
                                             l_param2
                                          )
                                       )
                                 )
                                 .then(
                                    Commands.literal("mainhand")
                                       .executes(
                                          l2_param1 -> dropFishingLoot(
                                             l2_param1,
                                             ResourceLocationArgument.getId(l2_param1, "loot_table"),
                                             BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                             getSourceHandItem(l2_param1.getSource(), EquipmentSlot.MAINHAND),
                                             l_param2
                                          )
                                       )
                                 )
                                 .then(
                                    Commands.literal("offhand")
                                       .executes(
                                          l2_param1 -> dropFishingLoot(
                                             l2_param1,
                                             ResourceLocationArgument.getId(l2_param1, "loot_table"),
                                             BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                             getSourceHandItem(l2_param1.getSource(), EquipmentSlot.OFFHAND),
                                             l_param2
                                          )
                                       )
                                 )
                           )
                     )
               )
               .then(
                  Commands.literal("loot")
                     .then(
                        Commands.argument("loot_table", ResourceLocationArgument.id())
                           .suggests(SUGGEST_LOOT_TABLE)
                           .executes(l2_param1 -> dropChestLoot(l2_param1, ResourceLocationArgument.getId(l2_param1, "loot_table"), l_param2))
                     )
               )
               .then(
                  Commands.literal("kill")
                     .then(
                        Commands.argument("target", EntityArgument.entity())
                           .executes(l2_param1 -> dropKillLoot(l2_param1, EntityArgument.getEntity(l2_param1, "target"), l_param2))
                     )
               )
               .then(
                  Commands.literal("mine")
                     .then(
                        Commands.argument("pos", BlockPosArgument.blockPos())
                           .executes(l2_param1 -> dropBlockLoot(l2_param1, BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"), ItemStack.EMPTY, l_param2))
                           .then(
                              Commands.argument("tool", ItemArgument.item(param1))
                                 .executes(
                                    l2_param1 -> dropBlockLoot(
                                       l2_param1,
                                       BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                       ItemArgument.getItem(l2_param1, "tool").createItemStack(1, false),
                                       l_param2
                                    )
                                 )
                           )
                           .then(
                              Commands.literal("mainhand")
                                 .executes(
                                    l2_param1 -> dropBlockLoot(
                                       l2_param1,
                                       BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                       getSourceHandItem(l2_param1.getSource(), EquipmentSlot.MAINHAND),
                                       l_param2
                                    )
                                 )
                           )
                           .then(
                              Commands.literal("offhand")
                                 .executes(
                                    l2_param1 -> dropBlockLoot(
                                       l2_param1,
                                       BlockPosArgument.getLoadedBlockPos(l2_param1, "pos"),
                                       getSourceHandItem(l2_param1.getSource(), EquipmentSlot.OFFHAND),
                                       l_param2
                                    )
                                 )
                           )
                     )
               )
         )
      );
   }

   private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addTargets(T param0, LootCommand.TailProvider param1) {
      return param0.then(
            Commands.literal("replace")
               .then(
                  Commands.literal("entity")
                     .then(
                        Commands.argument("entities", EntityArgument.entities())
                           .then(
                              param1.construct(
                                    Commands.argument("slot", SlotArgument.slot()),
                                    (l_param0, l_param1, l_param2) -> entityReplace(
                                       EntityArgument.getEntities(l_param0, "entities"),
                                       SlotArgument.getSlot(l_param0, "slot"),
                                       l_param1.size(),
                                       l_param1,
                                       l_param2
                                    )
                                 )
                                 .then(
                                    param1.construct(
                                       Commands.argument("count", IntegerArgumentType.integer(0)),
                                       (l_param0, l_param1, l_param2) -> entityReplace(
                                          EntityArgument.getEntities(l_param0, "entities"),
                                          SlotArgument.getSlot(l_param0, "slot"),
                                          IntegerArgumentType.getInteger(l_param0, "count"),
                                          l_param1,
                                          l_param2
                                       )
                                    )
                                 )
                           )
                     )
               )
               .then(
                  Commands.literal("block")
                     .then(
                        Commands.argument("targetPos", BlockPosArgument.blockPos())
                           .then(
                              param1.construct(
                                    Commands.argument("slot", SlotArgument.slot()),
                                    (l_param0, l_param1, l_param2) -> blockReplace(
                                       l_param0.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(l_param0, "targetPos"),
                                       SlotArgument.getSlot(l_param0, "slot"),
                                       l_param1.size(),
                                       l_param1,
                                       l_param2
                                    )
                                 )
                                 .then(
                                    param1.construct(
                                       Commands.argument("count", IntegerArgumentType.integer(0)),
                                       (l_param0, l_param1, l_param2) -> blockReplace(
                                          l_param0.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(l_param0, "targetPos"),
                                          IntegerArgumentType.getInteger(l_param0, "slot"),
                                          IntegerArgumentType.getInteger(l_param0, "count"),
                                          l_param1,
                                          l_param2
                                       )
                                    )
                                 )
                           )
                     )
               )
         )
         .then(
            Commands.literal("insert")
               .then(
                  param1.construct(
                     Commands.argument("targetPos", BlockPosArgument.blockPos()),
                     (l_param0, l_param1, l_param2) -> blockDistribute(
                        l_param0.getSource(), BlockPosArgument.getLoadedBlockPos(l_param0, "targetPos"), l_param1, l_param2
                     )
                  )
               )
         )
         .then(
            Commands.literal("give")
               .then(
                  param1.construct(
                     Commands.argument("players", EntityArgument.players()),
                     (l_param0, l_param1, l_param2) -> playerGive(EntityArgument.getPlayers(l_param0, "players"), l_param1, l_param2)
                  )
               )
         )
         .then(
            Commands.literal("spawn")
               .then(
                  param1.construct(
                     Commands.argument("targetPos", Vec3Argument.vec3()),
                     (l_param0, l_param1, l_param2) -> dropInWorld(l_param0.getSource(), Vec3Argument.getVec3(l_param0, "targetPos"), l_param1, l_param2)
                  )
               )
         );
   }

   private static Container getContainer(CommandSourceStack param0, BlockPos param1) throws CommandSyntaxException {
      BlockEntity var2 = param0.getLevel().getBlockEntity(param1);
      if (!(var2 instanceof Container)) {
         throw ItemCommands.ERROR_TARGET_NOT_A_CONTAINER.create(param1.getX(), param1.getY(), param1.getZ());
      } else {
         return (Container)var2;
      }
   }

   private static int blockDistribute(CommandSourceStack param0, BlockPos param1, List<ItemStack> param2, LootCommand.Callback param3) throws CommandSyntaxException {
      Container var4 = getContainer(param0, param1);
      List<ItemStack> var5 = Lists.newArrayListWithCapacity(param2.size());

      for (ItemStack var7 : param2) {
         if (distributeToContainer(var4, var7.copy())) {
            var4.setChanged();
            var5.add(var7);
         }
      }

      param3.accept(var5);
      return var5.size();
   }

   private static boolean distributeToContainer(Container param0, ItemStack param1) {
      boolean var2 = false;

      for (int var3 = 0; var3 < param0.getContainerSize() && !param1.isEmpty(); var3++) {
         ItemStack var4 = param0.getItem(var3);
         if (param0.canPlaceItem(var3, param1)) {
            if (var4.isEmpty()) {
               param0.setItem(var3, param1);
               var2 = true;
               break;
            }

            if (canMergeItems(var4, param1)) {
               int var5 = param1.getMaxStackSize() - var4.getCount();
               int var6 = Math.min(param1.getCount(), var5);
               param1.shrink(var6);
               var4.grow(var6);
               var2 = true;
            }
         }
      }

      return var2;
   }

   private static int blockReplace(CommandSourceStack param0, BlockPos param1, int param2, int param3, List<ItemStack> param4, LootCommand.Callback param5) throws CommandSyntaxException {
      Container var6 = getContainer(param0, param1);
      int var7 = var6.getContainerSize();
      if (param2 >= 0 && param2 < var7) {
         List<ItemStack> var8 = Lists.newArrayListWithCapacity(param4.size());

         for (int var9 = 0; var9 < param3; var9++) {
            int var10 = param2 + var9;
            ItemStack var11 = var9 < param4.size() ? param4.get(var9) : ItemStack.EMPTY;
            if (var6.canPlaceItem(var10, var11)) {
               var6.setItem(var10, var11);
               var8.add(var11);
            }
         }

         param5.accept(var8);
         return var8.size();
      } else {
         throw ItemCommands.ERROR_TARGET_INAPPLICABLE_SLOT.create(param2);
      }
   }

   private static boolean canMergeItems(ItemStack param0, ItemStack param1) {
      return param0.is(param1.getItem())
         && param0.getDamageValue() == param1.getDamageValue()
         && param0.getCount() <= param0.getMaxStackSize()
         && Objects.equals(param0.getTag(), param1.getTag());
   }

   private static int playerGive(Collection<ServerPlayer> param0, List<ItemStack> param1, LootCommand.Callback param2) throws CommandSyntaxException {
      List<ItemStack> var3 = Lists.newArrayListWithCapacity(param1.size());

      for (ItemStack var5 : param1) {
         for (ServerPlayer var7 : param0) {
            if (var7.getInventory().add(var5.copy())) {
               var3.add(var5);
            }
         }
      }

      param2.accept(var3);
      return var3.size();
   }

   private static void setSlots(Entity param0, List<ItemStack> param1, int param2, int param3, List<ItemStack> param4) {
      for (int var5 = 0; var5 < param3; var5++) {
         ItemStack var6 = var5 < param1.size() ? param1.get(var5) : ItemStack.EMPTY;
         SlotAccess var7 = param0.getSlot(param2 + var5);
         if (var7 != SlotAccess.NULL && var7.set(var6.copy())) {
            param4.add(var6);
         }
      }
   }

   private static int entityReplace(Collection<? extends Entity> param0, int param1, int param2, List<ItemStack> param3, LootCommand.Callback param4) throws CommandSyntaxException {
      List<ItemStack> var5 = Lists.newArrayListWithCapacity(param3.size());

      for (Entity var7 : param0) {
         if (var7 instanceof ServerPlayer var8) {
            setSlots(var7, param3, param1, param2, var5);
            var8.containerMenu.broadcastChanges();
         } else {
            setSlots(var7, param3, param1, param2, var5);
         }
      }

      param4.accept(var5);
      return var5.size();
   }

   private static int dropInWorld(CommandSourceStack param0, Vec3 param1, List<ItemStack> param2, LootCommand.Callback param3) throws CommandSyntaxException {
      ServerLevel var4 = param0.getLevel();
      param2.forEach(l_param2 -> {
         ItemEntity l_var3 = new ItemEntity(var4, param1.x, param1.y, param1.z, l_param2.copy());
         l_var3.setDefaultPickUpDelay();
         var4.addFreshEntity(l_var3);
      });
      param3.accept(param2);
      return param2.size();
   }

   private static void callback(CommandSourceStack param0, List<ItemStack> param1) {
      if (param1.size() == 1) {
         ItemStack var2 = param1.get(0);
         param0.sendSuccess(Component.translatable("commands.drop.success.single", var2.getCount(), var2.getDisplayName()), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.drop.success.multiple", param1.size()), false);
      }
   }

   private static void callback(CommandSourceStack param0, List<ItemStack> param1, ResourceLocation param2) {
      if (param1.size() == 1) {
         ItemStack var3 = param1.get(0);
         param0.sendSuccess(Component.translatable("commands.drop.success.single_with_table", var3.getCount(), var3.getDisplayName(), param2), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.drop.success.multiple_with_table", param1.size(), param2), false);
      }
   }

   private static ItemStack getSourceHandItem(CommandSourceStack param0, EquipmentSlot param1) throws CommandSyntaxException {
      Entity var2 = param0.getEntityOrException();
      if (var2 instanceof LivingEntity) {
         return ((LivingEntity)var2).getItemBySlot(param1);
      } else {
         throw ERROR_NO_HELD_ITEMS.create(var2.getDisplayName());
      }
   }

   private static int dropBlockLoot(CommandContext<CommandSourceStack> param0, BlockPos param1, ItemStack param2, LootCommand.DropConsumer param3) throws CommandSyntaxException {
      CommandSourceStack var4 = param0.getSource();
      ServerLevel var5 = var4.getLevel();
      BlockState var6 = var5.getBlockState(param1);
      BlockEntity var7 = var5.getBlockEntity(param1);
      LootContext.Builder var8 = new LootContext.Builder(var5)
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(param1))
         .withParameter(LootContextParams.BLOCK_STATE, var6)
         .withOptionalParameter(LootContextParams.BLOCK_ENTITY, var7)
         .withOptionalParameter(LootContextParams.THIS_ENTITY, var4.getEntity())
         .withParameter(LootContextParams.TOOL, param2);
      List<ItemStack> var9 = var6.getDrops(var8);
      return param3.accept(param0, var9, l_param2 -> callback(var4, l_param2, var6.getBlock().getLootTable()));
   }

   private static int dropKillLoot(CommandContext<CommandSourceStack> param0, Entity param1, LootCommand.DropConsumer param2) throws CommandSyntaxException {
      if (!(param1 instanceof LivingEntity)) {
         throw ERROR_NO_LOOT_TABLE.create(param1.getDisplayName());
      }

      ResourceLocation var3 = ((LivingEntity)param1).getLootTable();
      CommandSourceStack var4 = param0.getSource();
      LootContext.Builder var5 = new LootContext.Builder(var4.getLevel());
      Entity var6 = var4.getEntity();
      if (var6 instanceof Player) {
         var5.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, (Player)var6);
      }

      var5.withParameter(LootContextParams.DAMAGE_SOURCE, param1.damageSources().magic());
      var5.withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, var6);
      var5.withOptionalParameter(LootContextParams.KILLER_ENTITY, var6);
      var5.withParameter(LootContextParams.THIS_ENTITY, param1);
      var5.withParameter(LootContextParams.ORIGIN, var4.getPosition());
      LootTable var7 = var4.getServer().getLootTables().get(var3);
      List<ItemStack> var8 = var7.getRandomItems(var5.create(LootContextParamSets.ENTITY));
      return param2.accept(param0, var8, l_param2 -> callback(var4, l_param2, var3));
   }

   private static int dropChestLoot(CommandContext<CommandSourceStack> param0, ResourceLocation param1, LootCommand.DropConsumer param2) throws CommandSyntaxException {
      CommandSourceStack var3 = param0.getSource();
      LootContext.Builder var4 = new LootContext.Builder(var3.getLevel())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, var3.getEntity())
         .withParameter(LootContextParams.ORIGIN, var3.getPosition());
      return drop(param0, param1, var4.create(LootContextParamSets.CHEST), param2);
   }

   private static int dropFishingLoot(
      CommandContext<CommandSourceStack> param0, ResourceLocation param1, BlockPos param2, ItemStack param3, LootCommand.DropConsumer param4
   ) throws CommandSyntaxException {
      CommandSourceStack var5 = param0.getSource();
      LootContext var6 = new LootContext.Builder(var5.getLevel())
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(param2))
         .withParameter(LootContextParams.TOOL, param3)
         .withOptionalParameter(LootContextParams.THIS_ENTITY, var5.getEntity())
         .create(LootContextParamSets.FISHING);
      return drop(param0, param1, var6, param4);
   }

   private static int drop(CommandContext<CommandSourceStack> param0, ResourceLocation param1, LootContext param2, LootCommand.DropConsumer param3) throws CommandSyntaxException {
      CommandSourceStack var4 = param0.getSource();
      LootTable var5 = var4.getServer().getLootTables().get(param1);
      List<ItemStack> var6 = var5.getRandomItems(param2);
      return param3.accept(param0, var6, l_param1 -> callback(var4, l_param1));
   }

   @FunctionalInterface
   interface Callback {
      void accept(List<ItemStack> var1) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface DropConsumer {
      int accept(CommandContext<CommandSourceStack> var1, List<ItemStack> var2, LootCommand.Callback var3) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface TailProvider {
      ArgumentBuilder<CommandSourceStack, ?> construct(ArgumentBuilder<CommandSourceStack, ?> var1, LootCommand.DropConsumer var2);
   }
}
