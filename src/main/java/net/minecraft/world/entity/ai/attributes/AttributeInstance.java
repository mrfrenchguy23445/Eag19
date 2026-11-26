package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class AttributeInstance {
   private final Attribute attribute;
   private final Map<AttributeModifier.Operation, Set<AttributeModifier>> modifiersByOperation = Maps.newEnumMap(AttributeModifier.Operation.class);
   private final Map<UUID, AttributeModifier> modifierById = new Object2ObjectArrayMap<>();
   private final Set<AttributeModifier> permanentModifiers = new ObjectArraySet<>();
   private double baseValue;
   private boolean dirty = true;
   private double cachedValue;
   private final Consumer<AttributeInstance> onDirty;

   public AttributeInstance(Attribute param0, Consumer<AttributeInstance> param1) {
      this.attribute = param0;
      this.onDirty = param1;
      this.baseValue = param0.getDefaultValue();
   }

   public Attribute getAttribute() {
      return this.attribute;
   }

   public double getBaseValue() {
      return this.baseValue;
   }

   public void setBaseValue(double param0) {
      if (param0 != this.baseValue) {
         this.baseValue = param0;
         this.setDirty();
      }
   }

   public Set<AttributeModifier> getModifiers(AttributeModifier.Operation param0) {
      return this.modifiersByOperation.computeIfAbsent(param0, l_param0 -> Sets.newHashSet());
   }

   public Set<AttributeModifier> getModifiers() {
      return ImmutableSet.copyOf(this.modifierById.values());
   }

   @Nullable
   public AttributeModifier getModifier(UUID param0) {
      return this.modifierById.get(param0);
   }

   public boolean hasModifier(AttributeModifier param0) {
      return this.modifierById.get(param0.getId()) != null;
   }

   private void addModifier(AttributeModifier param0) {
      AttributeModifier var2 = this.modifierById.putIfAbsent(param0.getId(), param0);
      if (var2 != null) {
         throw new IllegalArgumentException("Modifier is already applied on this attribute!");
      }

      this.getModifiers(param0.getOperation()).add(param0);
      this.setDirty();
   }

   public void addTransientModifier(AttributeModifier param0) {
      this.addModifier(param0);
   }

   public void addPermanentModifier(AttributeModifier param0) {
      this.addModifier(param0);
      this.permanentModifiers.add(param0);
   }

   protected void setDirty() {
      this.dirty = true;
      this.onDirty.accept(this);
   }

   public void removeModifier(AttributeModifier param0) {
      this.getModifiers(param0.getOperation()).remove(param0);
      this.modifierById.remove(param0.getId());
      this.permanentModifiers.remove(param0);
      this.setDirty();
   }

   public void removeModifier(UUID param0) {
      AttributeModifier var2 = this.getModifier(param0);
      if (var2 != null) {
         this.removeModifier(var2);
      }
   }

   public boolean removePermanentModifier(UUID param0) {
      AttributeModifier var2 = this.getModifier(param0);
      if (var2 != null && this.permanentModifiers.contains(var2)) {
         this.removeModifier(var2);
         return true;
      } else {
         return false;
      }
   }

   public void removeModifiers() {
      for (AttributeModifier var2 : this.getModifiers()) {
         this.removeModifier(var2);
      }
   }

   public double getValue() {
      if (this.dirty) {
         this.cachedValue = this.calculateValue();
         this.dirty = false;
      }

      return this.cachedValue;
   }

   private double calculateValue() {
      double var1 = this.getBaseValue();

      for (AttributeModifier var4 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADDITION)) {
         var1 += var4.getAmount();
      }

      double var3 = var1;

      for (AttributeModifier var6 : this.getModifiersOrEmpty(AttributeModifier.Operation.MULTIPLY_BASE)) {
         var3 += var1 * var6.getAmount();
      }

      for (AttributeModifier var6 : this.getModifiersOrEmpty(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
         var3 *= 1.0 + var6.getAmount();
      }

      return this.attribute.sanitizeValue(var3);
   }

   private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation param0) {
      return this.modifiersByOperation.getOrDefault(param0, Collections.emptySet());
   }

   public void replaceFrom(AttributeInstance param0) {
      this.baseValue = param0.baseValue;
      this.modifierById.clear();
      this.modifierById.putAll(param0.modifierById);
      this.permanentModifiers.clear();
      this.permanentModifiers.addAll(param0.permanentModifiers);
      this.modifiersByOperation.clear();
      param0.modifiersByOperation.forEach((l_param0, l_param1) -> this.getModifiers(l_param0).addAll((Collection<? extends AttributeModifier>)l_param1));
      this.setDirty();
   }

   public CompoundTag save() {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", BuiltInRegistries.ATTRIBUTE.getKey(this.attribute).toString());
      var1.putDouble("Base", this.baseValue);
      if (!this.permanentModifiers.isEmpty()) {
         ListTag var2 = new ListTag();

         for (AttributeModifier var4 : this.permanentModifiers) {
            var2.add(var4.save());
         }

         var1.put("Modifiers", var2);
      }

      return var1;
   }

   public void load(CompoundTag param0) {
      this.baseValue = param0.getDouble("Base");
      if (param0.contains("Modifiers", 9)) {
         ListTag var2 = param0.getList("Modifiers", 10);

         for (int var3 = 0; var3 < var2.size(); var3++) {
            AttributeModifier var4 = AttributeModifier.load(var2.getCompound(var3));
            if (var4 != null) {
               this.modifierById.put(var4.getId(), var4);
               this.getModifiers(var4.getOperation()).add(var4);
               this.permanentModifiers.add(var4);
            }
         }
      }

      this.setDirty();
   }
}
