package net.minecraft.world.item.enchantment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;

public class EnchantmentHelper {
   private static final String TAG_ENCH_ID = "id";
   private static final String TAG_ENCH_LEVEL = "lvl";
   private static final float SWIFT_SNEAK_EXTRA_FACTOR = 0.15F;

   public static CompoundTag storeEnchantment(@Nullable ResourceLocation param0, int param1) {
      CompoundTag var2 = new CompoundTag();
      var2.putString("id", String.valueOf(param0));
      var2.putShort("lvl", (short)param1);
      return var2;
   }

   public static void setEnchantmentLevel(CompoundTag param0, int param1) {
      param0.putShort("lvl", (short)param1);
   }

   public static int getEnchantmentLevel(CompoundTag param0) {
      return Mth.clamp(param0.getInt("lvl"), 0, 255);
   }

   @Nullable
   public static ResourceLocation getEnchantmentId(CompoundTag param0) {
      return ResourceLocation.tryParse(param0.getString("id"));
   }

   @Nullable
   public static ResourceLocation getEnchantmentId(Enchantment param0) {
      return BuiltInRegistries.ENCHANTMENT.getKey(param0);
   }

   public static int getItemEnchantmentLevel(Enchantment param0, ItemStack param1) {
      if (param1.isEmpty()) {
         return 0;
      }

      ResourceLocation var2 = getEnchantmentId(param0);
      ListTag var3 = param1.getEnchantmentTags();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         CompoundTag var5 = var3.getCompound(var4);
         ResourceLocation var6 = getEnchantmentId(var5);
         if (var6 != null && var6.equals(var2)) {
            return getEnchantmentLevel(var5);
         }
      }

      return 0;
   }

   public static Map<Enchantment, Integer> getEnchantments(ItemStack param0) {
      ListTag var1 = param0.is(Items.ENCHANTED_BOOK) ? EnchantedBookItem.getEnchantments(param0) : param0.getEnchantmentTags();
      return deserializeEnchantments(var1);
   }

   public static Map<Enchantment, Integer> deserializeEnchantments(ListTag param0) {
      Map<Enchantment, Integer> var1 = Maps.newLinkedHashMap();

      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         BuiltInRegistries.ENCHANTMENT.getOptional(getEnchantmentId(var3)).ifPresent(l_param2 -> var1.put(l_param2, getEnchantmentLevel(var3)));
      }

      return var1;
   }

   public static void setEnchantments(Map<Enchantment, Integer> param0, ItemStack param1) {
      ListTag var2 = new ListTag();

      for (Entry<Enchantment, Integer> var4 : param0.entrySet()) {
         Enchantment var5 = var4.getKey();
         if (var5 != null) {
            int var6 = var4.getValue();
            var2.add(storeEnchantment(getEnchantmentId(var5), var6));
            if (param1.is(Items.ENCHANTED_BOOK)) {
               EnchantedBookItem.addEnchantment(param1, new EnchantmentInstance(var5, var6));
            }
         }
      }

      if (var2.isEmpty()) {
         param1.removeTagKey("Enchantments");
      } else if (!param1.is(Items.ENCHANTED_BOOK)) {
         param1.addTagElement("Enchantments", var2);
      }
   }

   private static void runIterationOnItem(EnchantmentHelper.EnchantmentVisitor param0, ItemStack param1) {
      if (!param1.isEmpty()) {
         ListTag var2 = param1.getEnchantmentTags();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            CompoundTag var4 = var2.getCompound(var3);
            BuiltInRegistries.ENCHANTMENT.getOptional(getEnchantmentId(var4)).ifPresent(l_param2 -> param0.accept(l_param2, getEnchantmentLevel(var4)));
         }
      }
   }

   private static void runIterationOnInventory(EnchantmentHelper.EnchantmentVisitor param0, Iterable<ItemStack> param1) {
      for (ItemStack var3 : param1) {
         runIterationOnItem(param0, var3);
      }
   }

   public static int getDamageProtection(Iterable<ItemStack> param0, DamageSource param1) {
      MutableInt var2 = new MutableInt();
      runIterationOnInventory((l_param2, l_param3) -> var2.add(l_param2.getDamageProtection(l_param3, param1)), param0);
      return var2.intValue();
   }

   public static float getDamageBonus(ItemStack param0, MobType param1) {
      MutableFloat var2 = new MutableFloat();
      runIterationOnItem((l_param2, l_param3) -> var2.add(l_param2.getDamageBonus(l_param3, param1)), param0);
      return var2.floatValue();
   }

   public static float getSweepingDamageRatio(LivingEntity param0) {
      int var1 = getEnchantmentLevel(Enchantments.SWEEPING_EDGE, param0);
      return var1 > 0 ? SweepingEdgeEnchantment.getSweepingDamageRatio(var1) : 0.0F;
   }

   public static void doPostHurtEffects(LivingEntity param0, Entity param1) {
      EnchantmentHelper.EnchantmentVisitor var2 = (l_param2, l_param3) -> l_param2.doPostHurt(param0, param1, l_param3);
      if (param0 != null) {
         runIterationOnInventory(var2, param0.getAllSlots());
      }

      if (param1 instanceof Player) {
         runIterationOnItem(var2, param0.getMainHandItem());
      }
   }

   public static void doPostDamageEffects(LivingEntity param0, Entity param1) {
      EnchantmentHelper.EnchantmentVisitor var2 = (l_param2, l_param3) -> l_param2.doPostAttack(param0, param1, l_param3);
      if (param0 != null) {
         runIterationOnInventory(var2, param0.getAllSlots());
      }

      if (param0 instanceof Player) {
         runIterationOnItem(var2, param0.getMainHandItem());
      }
   }

   public static int getEnchantmentLevel(Enchantment param0, LivingEntity param1) {
      Iterable<ItemStack> var2 = param0.getSlotItems(param1).values();
      if (var2 == null) {
         return 0;
      }

      int var3 = 0;

      for (ItemStack var5 : var2) {
         int var6 = getItemEnchantmentLevel(param0, var5);
         if (var6 > var3) {
            var3 = var6;
         }
      }

      return var3;
   }

   public static float getSneakingSpeedBonus(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.SWIFT_SNEAK, param0) * 0.15F;
   }

   public static int getKnockbackBonus(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.KNOCKBACK, param0);
   }

   public static int getFireAspect(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.FIRE_ASPECT, param0);
   }

   public static int getRespiration(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.RESPIRATION, param0);
   }

   public static int getDepthStrider(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.DEPTH_STRIDER, param0);
   }

   public static int getBlockEfficiency(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, param0);
   }

   public static int getFishingLuckBonus(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.FISHING_LUCK, param0);
   }

   public static int getFishingSpeedBonus(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.FISHING_SPEED, param0);
   }

   public static int getMobLooting(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.MOB_LOOTING, param0);
   }

   public static boolean hasAquaAffinity(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.AQUA_AFFINITY, param0) > 0;
   }

   public static boolean hasFrostWalker(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.FROST_WALKER, param0) > 0;
   }

   public static boolean hasSoulSpeed(LivingEntity param0) {
      return getEnchantmentLevel(Enchantments.SOUL_SPEED, param0) > 0;
   }

   public static boolean hasBindingCurse(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.BINDING_CURSE, param0) > 0;
   }

   public static boolean hasVanishingCurse(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, param0) > 0;
   }

   public static boolean hasSilkTouch(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.SILK_TOUCH, param0) > 0;
   }

   public static int getLoyalty(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.LOYALTY, param0);
   }

   public static int getRiptide(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.RIPTIDE, param0);
   }

   public static boolean hasChanneling(ItemStack param0) {
      return getItemEnchantmentLevel(Enchantments.CHANNELING, param0) > 0;
   }

   @Nullable
   public static Entry<EquipmentSlot, ItemStack> getRandomItemWith(Enchantment param0, LivingEntity param1) {
      return getRandomItemWith(param0, param1, l_param0 -> true);
   }

   @Nullable
   public static Entry<EquipmentSlot, ItemStack> getRandomItemWith(Enchantment param0, LivingEntity param1, Predicate<ItemStack> param2) {
      Map<EquipmentSlot, ItemStack> var3 = param0.getSlotItems(param1);
      if (var3.isEmpty()) {
         return null;
      }

      List<Entry<EquipmentSlot, ItemStack>> var4 = Lists.newArrayList();

      for (Entry<EquipmentSlot, ItemStack> var6 : var3.entrySet()) {
         ItemStack var7 = var6.getValue();
         if (!var7.isEmpty() && getItemEnchantmentLevel(param0, var7) > 0 && param2.test(var7)) {
            var4.add(var6);
         }
      }

      return var4.isEmpty() ? null : var4.get(param1.getRandom().nextInt(var4.size()));
   }

   public static int getEnchantmentCost(RandomSource param0, int param1, int param2, ItemStack param3) {
      Item var4 = param3.getItem();
      int var5 = var4.getEnchantmentValue();
      if (var5 <= 0) {
         return 0;
      }

      if (param2 > 15) {
         param2 = 15;
      }

      int var6 = param0.nextInt(8) + 1 + (param2 >> 1) + param0.nextInt(param2 + 1);
      if (param1 == 0) {
         return Math.max(var6 / 3, 1);
      } else {
         return param1 == 1 ? var6 * 2 / 3 + 1 : Math.max(var6, param2 * 2);
      }
   }

   public static ItemStack enchantItem(RandomSource param0, ItemStack param1, int param2, boolean param3) {
      List<EnchantmentInstance> var4 = selectEnchantment(param0, param1, param2, param3);
      boolean var5 = param1.is(Items.BOOK);
      if (var5) {
         param1 = new ItemStack(Items.ENCHANTED_BOOK);
      }

      for (EnchantmentInstance var7 : var4) {
         if (var5) {
            EnchantedBookItem.addEnchantment(param1, var7);
         } else {
            param1.enchant(var7.enchantment, var7.level);
         }
      }

      return param1;
   }

   public static List<EnchantmentInstance> selectEnchantment(RandomSource param0, ItemStack param1, int param2, boolean param3) {
      List<EnchantmentInstance> var4 = Lists.newArrayList();
      Item var5 = param1.getItem();
      int var6 = var5.getEnchantmentValue();
      if (var6 <= 0) {
         return var4;
      }

      param2 += 1 + param0.nextInt(var6 / 4 + 1) + param0.nextInt(var6 / 4 + 1);
      float var7 = (param0.nextFloat() + param0.nextFloat() - 1.0F) * 0.15F;
      param2 = Mth.clamp(Math.round(param2 + param2 * var7), 1, Integer.MAX_VALUE);
      List<EnchantmentInstance> var8 = getAvailableEnchantmentResults(param2, param1, param3);
      if (!var8.isEmpty()) {
         WeightedRandom.getRandomItem(param0, var8).ifPresent(var4::add);

         while (param0.nextInt(50) <= param2) {
            if (!var4.isEmpty()) {
               filterCompatibleEnchantments(var8, Util.lastOf(var4));
            }

            if (var8.isEmpty()) {
               break;
            }

            WeightedRandom.getRandomItem(param0, var8).ifPresent(var4::add);
            param2 /= 2;
         }
      }

      return var4;
   }

   public static void filterCompatibleEnchantments(List<EnchantmentInstance> param0, EnchantmentInstance param1) {
      Iterator<EnchantmentInstance> var2 = param0.iterator();

      while (var2.hasNext()) {
         if (!param1.enchantment.isCompatibleWith(var2.next().enchantment)) {
            var2.remove();
         }
      }
   }

   public static boolean isEnchantmentCompatible(Collection<Enchantment> param0, Enchantment param1) {
      for (Enchantment var3 : param0) {
         if (!var3.isCompatibleWith(param1)) {
            return false;
         }
      }

      return true;
   }

   public static List<EnchantmentInstance> getAvailableEnchantmentResults(int param0, ItemStack param1, boolean param2) {
      List<EnchantmentInstance> var3 = Lists.newArrayList();
      Item var4 = param1.getItem();
      boolean var5 = param1.is(Items.BOOK);

      for (Enchantment var7 : BuiltInRegistries.ENCHANTMENT) {
         if ((!var7.isTreasureOnly() || param2) && var7.isDiscoverable() && (var7.category.canEnchant(var4) || var5)) {
            for (int var8 = var7.getMaxLevel(); var8 > var7.getMinLevel() - 1; var8--) {
               if (param0 >= var7.getMinCost(var8) && param0 <= var7.getMaxCost(var8)) {
                  var3.add(new EnchantmentInstance(var7, var8));
                  break;
               }
            }
         }
      }

      return var3;
   }

   @FunctionalInterface
   interface EnchantmentVisitor {
      void accept(Enchantment var1, int var2);
   }
}
