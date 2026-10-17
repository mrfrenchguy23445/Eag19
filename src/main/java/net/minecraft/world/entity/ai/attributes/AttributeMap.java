package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AttributeMap {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<Attribute, AttributeInstance> attributes = Maps.newHashMap();
   private final Set<AttributeInstance> dirtyAttributes = Sets.newHashSet();
   private final AttributeSupplier supplier;

   public AttributeMap(AttributeSupplier param0) {
      this.supplier = param0;
   }

   private void onAttributeModified(AttributeInstance param0) {
      if (param0.getAttribute().isClientSyncable()) {
         this.dirtyAttributes.add(param0);
      }
   }

   public Set<AttributeInstance> getDirtyAttributes() {
      return this.dirtyAttributes;
   }

   public Collection<AttributeInstance> getSyncableAttributes() {
      return this.attributes.values().stream().filter(l_param0 -> l_param0.getAttribute().isClientSyncable()).collect(Collectors.toList());
   }

   @Nullable
   public AttributeInstance getInstance(Attribute param0) {
      return this.attributes.computeIfAbsent(param0, l_param0 -> this.supplier.createInstance(this::onAttributeModified, l_param0));
   }

   @Nullable
   public AttributeInstance getInstance(Holder<Attribute> param0) {
      return this.getInstance(param0.value());
   }

   public boolean hasAttribute(Attribute param0) {
      return this.attributes.get(param0) != null || this.supplier.hasAttribute(param0);
   }

   public boolean hasAttribute(Holder<Attribute> param0) {
      return this.hasAttribute(param0.value());
   }

   public boolean hasModifier(Attribute param0, UUID param1) {
      AttributeInstance var3 = this.attributes.get(param0);
      return var3 != null ? var3.getModifier(param1) != null : this.supplier.hasModifier(param0, param1);
   }

   public boolean hasModifier(Holder<Attribute> param0, UUID param1) {
      return this.hasModifier(param0.value(), param1);
   }

   public double getValue(Attribute param0) {
      AttributeInstance var2 = this.attributes.get(param0);
      return var2 != null ? var2.getValue() : this.supplier.getValue(param0);
   }

   public double getBaseValue(Attribute param0) {
      AttributeInstance var2 = this.attributes.get(param0);
      return var2 != null ? var2.getBaseValue() : this.supplier.getBaseValue(param0);
   }

   public double getModifierValue(Attribute param0, UUID param1) {
      AttributeInstance var3 = this.attributes.get(param0);
      return var3 != null ? var3.getModifier(param1).getAmount() : this.supplier.getModifierValue(param0, param1);
   }

   public double getModifierValue(Holder<Attribute> param0, UUID param1) {
      return this.getModifierValue(param0.value(), param1);
   }

   public void removeAttributeModifiers(Multimap<Attribute, AttributeModifier> param0) {
      param0.asMap().forEach((l_param0, l_param1) -> {
         AttributeInstance l_var3 = this.attributes.get(l_param0);
         if (l_var3 != null) {
            l_param1.forEach(l_var3::removeModifier);
         }
      });
   }

   public void addTransientAttributeModifiers(Multimap<Attribute, AttributeModifier> param0) {
      param0.forEach((l_param0, l_param1) -> {
         AttributeInstance l_var3 = this.getInstance(l_param0);
         if (l_var3 != null) {
            l_var3.removeModifier(l_param1);
            l_var3.addTransientModifier(l_param1);
         }
      });
   }

   public void assignValues(AttributeMap param0) {
      param0.attributes.values().forEach(l_param0 -> {
         AttributeInstance l_var2 = this.getInstance(l_param0.getAttribute());
         if (l_var2 != null) {
            l_var2.replaceFrom(l_param0);
         }
      });
   }

   public ListTag save() {
      ListTag var1 = new ListTag();

      for (AttributeInstance var3 : this.attributes.values()) {
         var1.add(var3.save());
      }

      return var1;
   }

   public void load(ListTag param0) {
      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         String var4 = var3.getString("Name");
         Util.ifElse(BuiltInRegistries.ATTRIBUTE.getOptional(ResourceLocation.tryParse(var4)), l_param1 -> {
            AttributeInstance l_var3 = this.getInstance(l_param1);
            if (l_var3 != null) {
               l_var3.load(var3);
            }
         }, () -> LOGGER.warn("Ignoring unknown attribute '{}'", var4));
      }
   }
}
