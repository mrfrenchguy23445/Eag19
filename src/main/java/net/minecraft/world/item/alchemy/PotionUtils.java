package net.minecraft.world.item.alchemy;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class PotionUtils {
   public static final String TAG_CUSTOM_POTION_EFFECTS = "CustomPotionEffects";
   public static final String TAG_CUSTOM_POTION_COLOR = "CustomPotionColor";
   public static final String TAG_POTION = "Potion";
   private static final int EMPTY_COLOR = 16253176;
   private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);

   public static List<MobEffectInstance> getMobEffects(ItemStack param0) {
      return getAllEffects(param0.getTag());
   }

   public static List<MobEffectInstance> getAllEffects(Potion param0, Collection<MobEffectInstance> param1) {
      List<MobEffectInstance> var2 = Lists.newArrayList();
      var2.addAll(param0.getEffects());
      var2.addAll(param1);
      return var2;
   }

   public static List<MobEffectInstance> getAllEffects(@Nullable CompoundTag param0) {
      List<MobEffectInstance> var1 = Lists.newArrayList();
      var1.addAll(getPotion(param0).getEffects());
      getCustomEffects(param0, var1);
      return var1;
   }

   public static List<MobEffectInstance> getCustomEffects(ItemStack param0) {
      return getCustomEffects(param0.getTag());
   }

   public static List<MobEffectInstance> getCustomEffects(@Nullable CompoundTag param0) {
      List<MobEffectInstance> var1 = Lists.newArrayList();
      getCustomEffects(param0, var1);
      return var1;
   }

   public static void getCustomEffects(@Nullable CompoundTag param0, List<MobEffectInstance> param1) {
      if (param0 != null && param0.contains("CustomPotionEffects", 9)) {
         ListTag var2 = param0.getList("CustomPotionEffects", 10);

         for (int var3 = 0; var3 < var2.size(); var3++) {
            CompoundTag var4 = var2.getCompound(var3);
            MobEffectInstance var5 = MobEffectInstance.load(var4);
            if (var5 != null) {
               param1.add(var5);
            }
         }
      }
   }

   public static int getColor(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      if (var1 != null && var1.contains("CustomPotionColor", 99)) {
         return var1.getInt("CustomPotionColor");
      } else {
         return getPotion(param0) == Potions.EMPTY ? 16253176 : getColor(getMobEffects(param0));
      }
   }

   public static int getColor(Potion param0) {
      return param0 == Potions.EMPTY ? 16253176 : getColor(param0.getEffects());
   }

   public static int getColor(Collection<MobEffectInstance> param0) {
      int var1 = 3694022;
      if (param0.isEmpty()) {
         return 3694022;
      }

      float var2 = 0.0F;
      float var3 = 0.0F;
      float var4 = 0.0F;
      int var5 = 0;

      for (MobEffectInstance var7 : param0) {
         if (var7.isVisible()) {
            int var8 = var7.getEffect().getColor();
            int var9 = var7.getAmplifier() + 1;
            var2 += var9 * (var8 >> 16 & 0xFF) / 255.0F;
            var3 += var9 * (var8 >> 8 & 0xFF) / 255.0F;
            var4 += var9 * (var8 >> 0 & 0xFF) / 255.0F;
            var5 += var9;
         }
      }

      if (var5 == 0) {
         return 0;
      }

      var2 = var2 / var5 * 255.0F;
      var3 = var3 / var5 * 255.0F;
      var4 = var4 / var5 * 255.0F;
      return (int)var2 << 16 | (int)var3 << 8 | (int)var4;
   }

   public static Potion getPotion(ItemStack param0) {
      return getPotion(param0.getTag());
   }

   public static Potion getPotion(@Nullable CompoundTag param0) {
      return param0 == null ? Potions.EMPTY : Potion.byName(param0.getString("Potion"));
   }

   public static ItemStack setPotion(ItemStack param0, Potion param1) {
      ResourceLocation var2 = BuiltInRegistries.POTION.getKey(param1);
      if (param1 == Potions.EMPTY) {
         param0.removeTagKey("Potion");
      } else {
         param0.getOrCreateTag().putString("Potion", var2.toString());
      }

      return param0;
   }

   public static ItemStack setCustomEffects(ItemStack param0, Collection<MobEffectInstance> param1) {
      if (param1.isEmpty()) {
         return param0;
      }

      CompoundTag var2 = param0.getOrCreateTag();
      ListTag var3 = var2.getList("CustomPotionEffects", 9);

      for (MobEffectInstance var5 : param1) {
         var3.add(var5.save(new CompoundTag()));
      }

      var2.put("CustomPotionEffects", var3);
      return param0;
   }

   public static void addPotionTooltip(ItemStack param0, List<Component> param1, float param2) {
      addPotionTooltip(getMobEffects(param0), param1, param2);
   }

   public static void addPotionTooltip(List<MobEffectInstance> param0, List<Component> param1, float param2) {
      List<Pair<Attribute, AttributeModifier>> var3 = Lists.newArrayList();
      if (param0.isEmpty()) {
         param1.add(NO_EFFECT);
      } else {
         for (MobEffectInstance var5 : param0) {
            MutableComponent var6 = Component.translatable(var5.getDescriptionId());
            MobEffect var7 = var5.getEffect();
            Map<Attribute, AttributeModifier> var8 = var7.getAttributeModifiers();
            if (!var8.isEmpty()) {
               for (Entry<Attribute, AttributeModifier> var10 : var8.entrySet()) {
                  AttributeModifier var11 = var10.getValue();
                  AttributeModifier var12 = new AttributeModifier(
                     var11.getName(), var7.getAttributeModifierValue(var5.getAmplifier(), var11), var11.getOperation()
                  );
                  var3.add(new Pair<>(var10.getKey(), var12));
               }
            }

            if (var5.getAmplifier() > 0) {
               var6 = Component.translatable("potion.withAmplifier", var6, Component.translatable("potion.potency." + var5.getAmplifier()));
            }

            if (!var5.endsWithin(20)) {
               var6 = Component.translatable("potion.withDuration", var6, MobEffectUtil.formatDuration(var5, param2));
            }

            param1.add(var6.withStyle(var7.getCategory().getTooltipFormatting()));
         }
      }

      if (!var3.isEmpty()) {
         param1.add(CommonComponents.EMPTY);
         param1.add(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

         for (Pair<Attribute, AttributeModifier> var5 : var3) {
            AttributeModifier var6 = var5.getSecond();
            double var7 = var6.getAmount();
            double var9;
            if (var6.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && var6.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
               var9 = var6.getAmount();
            } else {
               var9 = var6.getAmount() * 100.0;
            }

            if (var7 > 0.0) {
               param1.add(
                  Component.translatable(
                        "attribute.modifier.plus." + var6.getOperation().toValue(),
                        ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(var9),
                        Component.translatable(var5.getFirst().getDescriptionId())
                     )
                     .withStyle(ChatFormatting.BLUE)
               );
            } else if (var7 < 0.0) {
               var9 *= -1.0;
               param1.add(
                  Component.translatable(
                        "attribute.modifier.take." + var6.getOperation().toValue(),
                        ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(var9),
                        Component.translatable(var5.getFirst().getDescriptionId())
                     )
                     .withStyle(ChatFormatting.RED)
               );
            }
         }
      }
   }
}
