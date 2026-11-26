package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class EnchantCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.enchant.failed.entity", param0)
   );
   private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.enchant.failed.itemless", param0)
   );
   private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.enchant.failed.incompatible", param0)
   );
   private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.enchant.failed.level", param0, param1)
   );
   private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED = new SimpleCommandExceptionType(Component.translatable("commands.enchant.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("enchant")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("targets", EntityArgument.entities())
                  .then(
                     Commands.argument("enchantment", ResourceArgument.resource(param1, Registries.ENCHANTMENT))
                        .executes(
                           l_param0 -> enchant(
                              l_param0.getSource(),
                              EntityArgument.getEntities(l_param0, "targets"),
                              ResourceArgument.getEnchantment(l_param0, "enchantment"),
                              1
                           )
                        )
                        .then(
                           Commands.argument("level", IntegerArgumentType.integer(0))
                              .executes(
                                 l_param0 -> enchant(
                                    l_param0.getSource(),
                                    EntityArgument.getEntities(l_param0, "targets"),
                                    ResourceArgument.getEnchantment(l_param0, "enchantment"),
                                    IntegerArgumentType.getInteger(l_param0, "level")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int enchant(CommandSourceStack param0, Collection<? extends Entity> param1, Holder<Enchantment> param2, int param3) throws CommandSyntaxException {
      Enchantment var4 = param2.value();
      if (param3 > var4.getMaxLevel()) {
         throw ERROR_LEVEL_TOO_HIGH.create(param3, var4.getMaxLevel());
      }

      int var5 = 0;

      for (Entity var7 : param1) {
         if (var7 instanceof LivingEntity var8) {
            ItemStack var9 = var8.getMainHandItem();
            if (!var9.isEmpty()) {
               if (var4.canEnchant(var9) && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantments(var9).keySet(), var4)) {
                  var9.enchant(var4, param3);
                  var5++;
               } else if (param1.size() == 1) {
                  throw ERROR_INCOMPATIBLE.create(var9.getItem().getName(var9).getString());
               }
            } else if (param1.size() == 1) {
               throw ERROR_NO_ITEM.create(var8.getName().getString());
            }
         } else if (param1.size() == 1) {
            throw ERROR_NOT_LIVING_ENTITY.create(var7.getName().getString());
         }
      }

      if (var5 == 0) {
         throw ERROR_NOTHING_HAPPENED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.enchant.success.single", var4.getFullname(param3), param1.iterator().next().getDisplayName()), true
         );
      } else {
         param0.sendSuccess(Component.translatable("commands.enchant.success.multiple", var4.getFullname(param3), param1.size()), true);
      }

      return var5;
   }
}
