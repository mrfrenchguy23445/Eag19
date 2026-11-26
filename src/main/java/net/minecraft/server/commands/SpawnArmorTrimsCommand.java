package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.Level;

public class SpawnArmorTrimsCommand {
   private static final Map<Pair<ArmorMaterial, EquipmentSlot>, Item> MATERIAL_AND_SLOT_TO_ITEM = Util.make(Maps.newHashMap(), param0 -> {
      param0.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.HEAD), Items.CHAINMAIL_HELMET);
      param0.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.CHEST), Items.CHAINMAIL_CHESTPLATE);
      param0.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.LEGS), Items.CHAINMAIL_LEGGINGS);
      param0.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.FEET), Items.CHAINMAIL_BOOTS);
      param0.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.HEAD), Items.IRON_HELMET);
      param0.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.CHEST), Items.IRON_CHESTPLATE);
      param0.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.LEGS), Items.IRON_LEGGINGS);
      param0.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.FEET), Items.IRON_BOOTS);
      param0.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.HEAD), Items.GOLDEN_HELMET);
      param0.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.CHEST), Items.GOLDEN_CHESTPLATE);
      param0.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.LEGS), Items.GOLDEN_LEGGINGS);
      param0.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.FEET), Items.GOLDEN_BOOTS);
      param0.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.HEAD), Items.NETHERITE_HELMET);
      param0.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.CHEST), Items.NETHERITE_CHESTPLATE);
      param0.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.LEGS), Items.NETHERITE_LEGGINGS);
      param0.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.FEET), Items.NETHERITE_BOOTS);
      param0.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.HEAD), Items.DIAMOND_HELMET);
      param0.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.CHEST), Items.DIAMOND_CHESTPLATE);
      param0.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.LEGS), Items.DIAMOND_LEGGINGS);
      param0.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.FEET), Items.DIAMOND_BOOTS);
      param0.put(Pair.of(ArmorMaterials.TURTLE, EquipmentSlot.HEAD), Items.TURTLE_HELMET);
   });
   private static final List<ResourceKey<TrimPattern>> VANILLA_TRIM_PATTERNS = List.of(
      TrimPatterns.SENTRY,
      TrimPatterns.DUNE,
      TrimPatterns.COAST,
      TrimPatterns.WILD,
      TrimPatterns.WARD,
      TrimPatterns.EYE,
      TrimPatterns.VEX,
      TrimPatterns.TIDE,
      TrimPatterns.SNOUT,
      TrimPatterns.RIB,
      TrimPatterns.SPIRE
   );
   private static final List<ResourceKey<TrimMaterial>> VANILLA_TRIM_MATERIALS = List.of(
      TrimMaterials.QUARTZ,
      TrimMaterials.IRON,
      TrimMaterials.NETHERITE,
      TrimMaterials.REDSTONE,
      TrimMaterials.COPPER,
      TrimMaterials.GOLD,
      TrimMaterials.EMERALD,
      TrimMaterials.DIAMOND,
      TrimMaterials.LAPIS,
      TrimMaterials.AMETHYST
   );
   private static final ToIntFunction<ResourceKey<TrimPattern>> TRIM_PATTERN_ORDER = Util.createIndexLookup(VANILLA_TRIM_PATTERNS);
   private static final ToIntFunction<ResourceKey<TrimMaterial>> TRIM_MATERIAL_ORDER = Util.createIndexLookup(VANILLA_TRIM_MATERIALS);

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("spawn_armor_trims")
            .requires(l_param0 -> l_param0.hasPermission(2) && l_param0.getLevel().enabledFeatures().contains(FeatureFlags.UPDATE_1_20))
            .executes(l_param0 -> spawnArmorTrims(l_param0.getSource(), l_param0.getSource().getPlayerOrException()))
      );
   }

   private static int spawnArmorTrims(CommandSourceStack param0, Player param1) {
      Level var2 = param1.getLevel();
      NonNullList<ArmorTrim> var3 = NonNullList.create();
      Registry<TrimPattern> var4 = var2.registryAccess().registryOrThrow(Registries.TRIM_PATTERN);
      Registry<TrimMaterial> var5 = var2.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL);
      var4.stream()
         .sorted(Comparator.comparing(l_param1 -> TRIM_PATTERN_ORDER.applyAsInt(var4.getResourceKey(l_param1).orElse(null))))
         .forEachOrdered(
            l_param3 -> var5.stream()
               .sorted(Comparator.comparing(l2_param1 -> TRIM_MATERIAL_ORDER.applyAsInt(var5.getResourceKey(l2_param1).orElse(null))))
               .forEachOrdered(l2_param4 -> var3.add(new ArmorTrim(var5.wrapAsHolder(l2_param4), var4.wrapAsHolder(l_param3))))
         );
      BlockPos var6 = param1.blockPosition().relative(param1.getDirection(), 5);
      int var7 = ArmorMaterials.values().length - 1;
      double var8 = 3.0;
      int var10 = 0;
      int var11 = 0;

      for (ArmorTrim var13 : var3) {
         for (ArmorMaterial var17 : ArmorMaterials.values()) {
            if (var17 != ArmorMaterials.LEATHER) {
               double var18 = var6.getX() + 0.5 - var10 % var5.size() * 3.0;
               double var20 = var6.getY() + 0.5 + var11 % var7 * 3.0;
               double var22 = var6.getZ() + 0.5 + var10 / var5.size() * 10;
               ArmorStand var24 = new ArmorStand(var2, var18, var20, var22);
               var24.setYRot(180.0F);
               var24.setNoGravity(true);

               for (EquipmentSlot var28 : EquipmentSlot.values()) {
                  Item var29 = MATERIAL_AND_SLOT_TO_ITEM.get(Pair.of(var17, var28));
                  if (var29 != null) {
                     ItemStack var30 = new ItemStack(var29);
                     ArmorTrim.setTrim(var2.registryAccess(), var30, var13);
                     var24.setItemSlot(var28, var30);
                     if (var29 instanceof ArmorItem var31 && var31.getMaterial() == ArmorMaterials.TURTLE) {
                        var24.setCustomName(
                           var13.pattern().value().copyWithStyle(var13.material()).copy().append(" ").append(var13.material().value().description())
                        );
                        var24.setCustomNameVisible(true);
                     } else {
                        var24.setInvisible(true);
                     }
                  }
               }

               var2.addFreshEntity(var24);
               var11++;
            }
         }

         var10++;
      }

      param0.sendSuccess(Component.literal("Armorstands with trimmed armor spawned around you"), true);
      return 1;
   }
}
