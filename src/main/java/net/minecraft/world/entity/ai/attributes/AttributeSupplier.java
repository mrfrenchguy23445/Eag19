package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;

public class AttributeSupplier {
   private final Map<Attribute, AttributeInstance> instances;

   public AttributeSupplier(Map<Attribute, AttributeInstance> param0) {
      this.instances = ImmutableMap.copyOf(param0);
   }

   private AttributeInstance getAttributeInstance(Attribute param0) {
      AttributeInstance var2 = this.instances.get(param0);
      if (var2 == null) {
         throw new IllegalArgumentException("Can't find attribute " + BuiltInRegistries.ATTRIBUTE.getKey(param0));
      } else {
         return var2;
      }
   }

   public double getValue(Attribute param0) {
      return this.getAttributeInstance(param0).getValue();
   }

   public double getBaseValue(Attribute param0) {
      return this.getAttributeInstance(param0).getBaseValue();
   }

   public double getModifierValue(Attribute param0, UUID param1) {
      AttributeModifier var3 = this.getAttributeInstance(param0).getModifier(param1);
      if (var3 == null) {
         throw new IllegalArgumentException("Can't find modifier " + param1 + " on attribute " + BuiltInRegistries.ATTRIBUTE.getKey(param0));
      } else {
         return var3.getAmount();
      }
   }

   @Nullable
   public AttributeInstance createInstance(Consumer<AttributeInstance> param0, Attribute param1) {
      AttributeInstance var3 = this.instances.get(param1);
      if (var3 == null) {
         return null;
      }

      AttributeInstance var4 = new AttributeInstance(param1, param0);
      var4.replaceFrom(var3);
      return var4;
   }

   public static AttributeSupplier.Builder builder() {
      return new AttributeSupplier.Builder();
   }

   public boolean hasAttribute(Attribute param0) {
      return this.instances.containsKey(param0);
   }

   public boolean hasModifier(Attribute param0, UUID param1) {
      AttributeInstance var3 = this.instances.get(param0);
      return var3 != null && var3.getModifier(param1) != null;
   }

   public static class Builder {
      private final Map<Attribute, AttributeInstance> builder = Maps.newHashMap();
      private boolean instanceFrozen;

      private AttributeInstance create(Attribute param0) {
         AttributeInstance var2 = new AttributeInstance(param0, l_param1 -> {
            if (this.instanceFrozen) {
               throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + BuiltInRegistries.ATTRIBUTE.getKey(param0));
            }
         });
         this.builder.put(param0, var2);
         return var2;
      }

      public AttributeSupplier.Builder add(Attribute param0) {
         this.create(param0);
         return this;
      }

      public AttributeSupplier.Builder add(Attribute param0, double param1) {
         AttributeInstance var4 = this.create(param0);
         var4.setBaseValue(param1);
         return this;
      }

      public AttributeSupplier build() {
         this.instanceFrozen = true;
         return new AttributeSupplier(this.builder);
      }
   }
}
