package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.UUID;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.attribute.failed.entity", param0)
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.attribute.failed.no_attribute", param0, param1)
   );
   private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
      (param0, param1, param2) -> Component.translatable("commands.attribute.failed.no_modifier", param1, param0, param2)
   );
   private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
      (param0, param1, param2) -> Component.translatable("commands.attribute.failed.modifier_already_present", param2, param1, param0)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("attribute")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     Commands.argument("attribute", ResourceArgument.resource(param1, Registries.ATTRIBUTE))
                        .then(
                           Commands.literal("get")
                              .executes(
                                 l_param0 -> getAttributeValue(
                                    l_param0.getSource(),
                                    EntityArgument.getEntity(l_param0, "target"),
                                    ResourceArgument.getAttribute(l_param0, "attribute"),
                                    1.0
                                 )
                              )
                              .then(
                                 Commands.argument("scale", DoubleArgumentType.doubleArg())
                                    .executes(
                                       l_param0 -> getAttributeValue(
                                          l_param0.getSource(),
                                          EntityArgument.getEntity(l_param0, "target"),
                                          ResourceArgument.getAttribute(l_param0, "attribute"),
                                          DoubleArgumentType.getDouble(l_param0, "scale")
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("base")
                              .then(
                                 Commands.literal("set")
                                    .then(
                                       Commands.argument("value", DoubleArgumentType.doubleArg())
                                          .executes(
                                             l_param0 -> setAttributeBase(
                                                l_param0.getSource(),
                                                EntityArgument.getEntity(l_param0, "target"),
                                                ResourceArgument.getAttribute(l_param0, "attribute"),
                                                DoubleArgumentType.getDouble(l_param0, "value")
                                             )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("get")
                                    .executes(
                                       l_param0 -> getAttributeBase(
                                          l_param0.getSource(),
                                          EntityArgument.getEntity(l_param0, "target"),
                                          ResourceArgument.getAttribute(l_param0, "attribute"),
                                          1.0
                                       )
                                    )
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .executes(
                                             l_param0 -> getAttributeBase(
                                                l_param0.getSource(),
                                                EntityArgument.getEntity(l_param0, "target"),
                                                ResourceArgument.getAttribute(l_param0, "attribute"),
                                                DoubleArgumentType.getDouble(l_param0, "scale")
                                             )
                                          )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("modifier")
                              .then(
                                 Commands.literal("add")
                                    .then(
                                       Commands.argument("uuid", UuidArgument.uuid())
                                          .then(
                                             Commands.argument("name", StringArgumentType.string())
                                                .then(
                                                   Commands.argument("value", DoubleArgumentType.doubleArg())
                                                      .then(
                                                         Commands.literal("add")
                                                            .executes(
                                                               l_param0 -> addModifier(
                                                                  l_param0.getSource(),
                                                                  EntityArgument.getEntity(l_param0, "target"),
                                                                  ResourceArgument.getAttribute(l_param0, "attribute"),
                                                                  UuidArgument.getUuid(l_param0, "uuid"),
                                                                  StringArgumentType.getString(l_param0, "name"),
                                                                  DoubleArgumentType.getDouble(l_param0, "value"),
                                                                  AttributeModifier.Operation.ADDITION
                                                               )
                                                            )
                                                      )
                                                      .then(
                                                         Commands.literal("multiply")
                                                            .executes(
                                                               l_param0 -> addModifier(
                                                                  l_param0.getSource(),
                                                                  EntityArgument.getEntity(l_param0, "target"),
                                                                  ResourceArgument.getAttribute(l_param0, "attribute"),
                                                                  UuidArgument.getUuid(l_param0, "uuid"),
                                                                  StringArgumentType.getString(l_param0, "name"),
                                                                  DoubleArgumentType.getDouble(l_param0, "value"),
                                                                  AttributeModifier.Operation.MULTIPLY_TOTAL
                                                               )
                                                            )
                                                      )
                                                      .then(
                                                         Commands.literal("multiply_base")
                                                            .executes(
                                                               l_param0 -> addModifier(
                                                                  l_param0.getSource(),
                                                                  EntityArgument.getEntity(l_param0, "target"),
                                                                  ResourceArgument.getAttribute(l_param0, "attribute"),
                                                                  UuidArgument.getUuid(l_param0, "uuid"),
                                                                  StringArgumentType.getString(l_param0, "name"),
                                                                  DoubleArgumentType.getDouble(l_param0, "value"),
                                                                  AttributeModifier.Operation.MULTIPLY_BASE
                                                               )
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("remove")
                                    .then(
                                       Commands.argument("uuid", UuidArgument.uuid())
                                          .executes(
                                             l_param0 -> removeModifier(
                                                l_param0.getSource(),
                                                EntityArgument.getEntity(l_param0, "target"),
                                                ResourceArgument.getAttribute(l_param0, "attribute"),
                                                UuidArgument.getUuid(l_param0, "uuid")
                                             )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("value")
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             Commands.argument("uuid", UuidArgument.uuid())
                                                .executes(
                                                   l_param0 -> getAttributeModifier(
                                                      l_param0.getSource(),
                                                      EntityArgument.getEntity(l_param0, "target"),
                                                      ResourceArgument.getAttribute(l_param0, "attribute"),
                                                      UuidArgument.getUuid(l_param0, "uuid"),
                                                      1.0
                                                   )
                                                )
                                                .then(
                                                   Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                      .executes(
                                                         l_param0 -> getAttributeModifier(
                                                            l_param0.getSource(),
                                                            EntityArgument.getEntity(l_param0, "target"),
                                                            ResourceArgument.getAttribute(l_param0, "attribute"),
                                                            UuidArgument.getUuid(l_param0, "uuid"),
                                                            DoubleArgumentType.getDouble(l_param0, "scale")
                                                         )
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

   private static AttributeInstance getAttributeInstance(Entity param0, Holder<Attribute> param1) throws CommandSyntaxException {
      AttributeInstance var2 = getLivingEntity(param0).getAttributes().getInstance(param1);
      if (var2 == null) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(param0.getName(), getAttributeDescription(param1));
      } else {
         return var2;
      }
   }

   private static LivingEntity getLivingEntity(Entity param0) throws CommandSyntaxException {
      if (!(param0 instanceof LivingEntity)) {
         throw ERROR_NOT_LIVING_ENTITY.create(param0.getName());
      } else {
         return (LivingEntity)param0;
      }
   }

   private static LivingEntity getEntityWithAttribute(Entity param0, Holder<Attribute> param1) throws CommandSyntaxException {
      LivingEntity var2 = getLivingEntity(param0);
      if (!var2.getAttributes().hasAttribute(param1)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(param0.getName(), getAttributeDescription(param1));
      } else {
         return var2;
      }
   }

   private static int getAttributeValue(CommandSourceStack param0, Entity param1, Holder<Attribute> param2, double param3) throws CommandSyntaxException {
      LivingEntity var5 = getEntityWithAttribute(param1, param2);
      double var6 = var5.getAttributeValue(param2);
      param0.sendSuccess(Component.translatable("commands.attribute.value.get.success", getAttributeDescription(param2), param1.getName(), var6), false);
      return (int)(var6 * param3);
   }

   private static int getAttributeBase(CommandSourceStack param0, Entity param1, Holder<Attribute> param2, double param3) throws CommandSyntaxException {
      LivingEntity var5 = getEntityWithAttribute(param1, param2);
      double var6 = var5.getAttributeBaseValue(param2);
      param0.sendSuccess(Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(param2), param1.getName(), var6), false);
      return (int)(var6 * param3);
   }

   private static int getAttributeModifier(CommandSourceStack param0, Entity param1, Holder<Attribute> param2, UUID param3, double param4) throws CommandSyntaxException {
      LivingEntity var6 = getEntityWithAttribute(param1, param2);
      AttributeMap var7 = var6.getAttributes();
      if (!var7.hasModifier(param2, param3)) {
         throw ERROR_NO_SUCH_MODIFIER.create(param1.getName(), getAttributeDescription(param2), param3);
      }

      double var8 = var7.getModifierValue(param2, param3);
      param0.sendSuccess(
         Component.translatable("commands.attribute.modifier.value.get.success", param3, getAttributeDescription(param2), param1.getName(), var8), false
      );
      return (int)(var8 * param4);
   }

   private static int setAttributeBase(CommandSourceStack param0, Entity param1, Holder<Attribute> param2, double param3) throws CommandSyntaxException {
      getAttributeInstance(param1, param2).setBaseValue(param3);
      param0.sendSuccess(Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(param2), param1.getName(), param3), false);
      return 1;
   }

   private static int addModifier(
      CommandSourceStack param0, Entity param1, Holder<Attribute> param2, UUID param3, String param4, double param5, AttributeModifier.Operation param7
   ) throws CommandSyntaxException {
      AttributeInstance var8 = getAttributeInstance(param1, param2);
      AttributeModifier var9 = new AttributeModifier(param3, param4, param5, param7);
      if (var8.hasModifier(var9)) {
         throw ERROR_MODIFIER_ALREADY_PRESENT.create(param1.getName(), getAttributeDescription(param2), param3);
      }

      var8.addPermanentModifier(var9);
      param0.sendSuccess(Component.translatable("commands.attribute.modifier.add.success", param3, getAttributeDescription(param2), param1.getName()), false);
      return 1;
   }

   private static int removeModifier(CommandSourceStack param0, Entity param1, Holder<Attribute> param2, UUID param3) throws CommandSyntaxException {
      AttributeInstance var4 = getAttributeInstance(param1, param2);
      if (var4.removePermanentModifier(param3)) {
         param0.sendSuccess(
            Component.translatable("commands.attribute.modifier.remove.success", param3, getAttributeDescription(param2), param1.getName()), false
         );
         return 1;
      } else {
         throw ERROR_NO_SUCH_MODIFIER.create(param1.getName(), getAttributeDescription(param2), param3);
      }
   }

   private static Component getAttributeDescription(Holder<Attribute> param0) {
      return Component.translatable(param0.value().getDescriptionId());
   }
}
