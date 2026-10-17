package net.minecraft.world.entity.ai.attributes;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

public class AttributeModifier {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final double amount;
   private final AttributeModifier.Operation operation;
   private final Supplier<String> nameGetter;
   private final UUID id;

   public AttributeModifier(String param0, double param1, AttributeModifier.Operation param3) {
      this(Mth.createInsecureUUID(RandomSource.createNewThreadLocalInstance()), () -> param0, param1, param3);
   }

   public AttributeModifier(UUID param0, String param1, double param2, AttributeModifier.Operation param4) {
      this(param0, () -> param1, param2, param4);
   }

   public AttributeModifier(UUID param0, Supplier<String> param1, double param2, AttributeModifier.Operation param4) {
      this.id = param0;
      this.nameGetter = param1;
      this.amount = param2;
      this.operation = param4;
   }

   public UUID getId() {
      return this.id;
   }

   public String getName() {
      return this.nameGetter.get();
   }

   public AttributeModifier.Operation getOperation() {
      return this.operation;
   }

   public double getAmount() {
      return this.amount;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 != null && this.getClass() == param0.getClass()) {
         AttributeModifier var2 = (AttributeModifier)param0;
         return Objects.equals(this.id, var2.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return "AttributeModifier{amount=" + this.amount + ", operation=" + this.operation + ", name='" + this.nameGetter.get() + "', id=" + this.id + "}";
   }

   public CompoundTag save() {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", this.getName());
      var1.putDouble("Amount", this.amount);
      var1.putInt("Operation", this.operation.toValue());
      var1.putUUID("UUID", this.id);
      return var1;
   }

   @Nullable
   public static AttributeModifier load(CompoundTag param0) {
      try {
         UUID var1 = param0.getUUID("UUID");
         AttributeModifier.Operation var2 = AttributeModifier.Operation.fromValue(param0.getInt("Operation"));
         return new AttributeModifier(var1, param0.getString("Name"), param0.getDouble("Amount"), var2);
      } catch (Exception var1) {
         LOGGER.warn("Unable to create attribute: {}", var1.getMessage());
         return null;
      }
   }

   public enum Operation {
      ADDITION(0),
      MULTIPLY_BASE(1),
      MULTIPLY_TOTAL(2);

      private static final AttributeModifier.Operation[] OPERATIONS = new AttributeModifier.Operation[]{ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL};
      private final int value;

      Operation(int param2) {
         this.value = param2;
      }

      public int toValue() {
         return this.value;
      }

      public static AttributeModifier.Operation fromValue(int param0) {
         if (param0 >= 0 && param0 < OPERATIONS.length) {
            return OPERATIONS[param0];
         } else {
            throw new IllegalArgumentException("No operation with value " + param0);
         }
      }
   }
}
