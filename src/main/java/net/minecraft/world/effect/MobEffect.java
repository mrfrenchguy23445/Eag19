package net.minecraft.world.effect;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class MobEffect {
   private final Map<Attribute, AttributeModifier> attributeModifiers = Maps.newHashMap();
   private final MobEffectCategory category;
   private final int color;
   @Nullable
   private String descriptionId;
   private Supplier<MobEffectInstance.FactorData> factorDataFactory = () -> null;

   @Nullable
   public static MobEffect byId(int param0) {
      return BuiltInRegistries.MOB_EFFECT.byId(param0);
   }

   public static int getId(MobEffect param0) {
      return BuiltInRegistries.MOB_EFFECT.getId(param0);
   }

   public static int getIdFromNullable(@Nullable MobEffect param0) {
      return BuiltInRegistries.MOB_EFFECT.getId(param0);
   }

   protected MobEffect(MobEffectCategory param0, int param1) {
      this.category = param0;
      this.color = param1;
   }

   public Optional<MobEffectInstance.FactorData> createFactorData() {
      return Optional.ofNullable(this.factorDataFactory.get());
   }

   public void applyEffectTick(LivingEntity param0, int param1) {
      if (this == MobEffects.REGENERATION) {
         if (param0.getHealth() < param0.getMaxHealth()) {
            param0.heal(1.0F);
         }
      } else if (this == MobEffects.POISON) {
         if (param0.getHealth() > 1.0F) {
            param0.hurt(param0.damageSources().magic(), 1.0F);
         }
      } else if (this == MobEffects.WITHER) {
         param0.hurt(param0.damageSources().wither(), 1.0F);
      } else if (this == MobEffects.HUNGER && param0 instanceof Player) {
         ((Player)param0).causeFoodExhaustion(0.005F * (param1 + 1));
      } else if (this == MobEffects.SATURATION && param0 instanceof Player) {
         if (!param0.level.isClientSide) {
            ((Player)param0).getFoodData().eat(param1 + 1, 1.0F);
         }
      } else if ((this != MobEffects.HEAL || param0.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !param0.isInvertedHealAndHarm())) {
         if (this == MobEffects.HARM && !param0.isInvertedHealAndHarm() || this == MobEffects.HEAL && param0.isInvertedHealAndHarm()) {
            param0.hurt(param0.damageSources().magic(), 6 << param1);
         }
      } else {
         param0.heal(Math.max(4 << param1, 0));
      }
   }

   public void applyInstantenousEffect(@Nullable Entity param0, @Nullable Entity param1, LivingEntity param2, int param3, double param4) {
      if ((this != MobEffects.HEAL || param2.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !param2.isInvertedHealAndHarm())) {
         if (this == MobEffects.HARM && !param2.isInvertedHealAndHarm() || this == MobEffects.HEAL && param2.isInvertedHealAndHarm()) {
            int var7 = (int)(param4 * (6 << param3) + 0.5);
            if (param0 == null) {
               param2.hurt(param2.damageSources().magic(), var7);
            } else {
               param2.hurt(param2.damageSources().indirectMagic(param0, param1), var7);
            }
         } else {
            this.applyEffectTick(param2, param3);
         }
      } else {
         int var7 = (int)(param4 * (4 << param3) + 0.5);
         param2.heal(var7);
      }
   }

   public boolean isDurationEffectTick(int param0, int param1) {
      if (this == MobEffects.REGENERATION) {
         int var3 = 50 >> param1;
         return var3 > 0 ? param0 % var3 == 0 : true;
      } else if (this == MobEffects.POISON) {
         int var3 = 25 >> param1;
         return var3 > 0 ? param0 % var3 == 0 : true;
      } else if (this == MobEffects.WITHER) {
         int var3 = 40 >> param1;
         return var3 > 0 ? param0 % var3 == 0 : true;
      } else {
         return this == MobEffects.HUNGER;
      }
   }

   public boolean isInstantenous() {
      return false;
   }

   protected String getOrCreateDescriptionId() {
      if (this.descriptionId == null) {
         this.descriptionId = Util.makeDescriptionId("effect", BuiltInRegistries.MOB_EFFECT.getKey(this));
      }

      return this.descriptionId;
   }

   public String getDescriptionId() {
      return this.getOrCreateDescriptionId();
   }

   public Component getDisplayName() {
      return Component.translatable(this.getDescriptionId());
   }

   public MobEffectCategory getCategory() {
      return this.category;
   }

   public int getColor() {
      return this.color;
   }

   public MobEffect addAttributeModifier(Attribute param0, String param1, double param2, AttributeModifier.Operation param4) {
      AttributeModifier var6 = new AttributeModifier(UUID.fromString(param1), this::getDescriptionId, param2, param4);
      this.attributeModifiers.put(param0, var6);
      return this;
   }

   public MobEffect setFactorDataFactory(Supplier<MobEffectInstance.FactorData> param0) {
      this.factorDataFactory = param0;
      return this;
   }

   public Map<Attribute, AttributeModifier> getAttributeModifiers() {
      return this.attributeModifiers;
   }

   public void removeAttributeModifiers(LivingEntity param0, AttributeMap param1, int param2) {
      for (Entry<Attribute, AttributeModifier> var5 : this.attributeModifiers.entrySet()) {
         AttributeInstance var6 = param1.getInstance(var5.getKey());
         if (var6 != null) {
            var6.removeModifier(var5.getValue());
         }
      }
   }

   public void addAttributeModifiers(LivingEntity param0, AttributeMap param1, int param2) {
      for (Entry<Attribute, AttributeModifier> var5 : this.attributeModifiers.entrySet()) {
         AttributeInstance var6 = param1.getInstance(var5.getKey());
         if (var6 != null) {
            AttributeModifier var7 = var5.getValue();
            var6.removeModifier(var7);
            var6.addPermanentModifier(
               new AttributeModifier(var7.getId(), this.getDescriptionId() + " " + param2, this.getAttributeModifierValue(param2, var7), var7.getOperation())
            );
         }
      }
   }

   public double getAttributeModifierValue(int param0, AttributeModifier param1) {
      return param1.getAmount() * (param0 + 1);
   }

   public boolean isBeneficial() {
      return this.category == MobEffectCategory.BENEFICIAL;
   }
}
