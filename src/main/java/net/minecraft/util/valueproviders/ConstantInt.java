package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public class ConstantInt extends IntProvider {
   public static final ConstantInt ZERO = new ConstantInt(0);
   public static final Codec<ConstantInt> CODEC = Codec.INT.fieldOf("value").xmap(ConstantInt::new, ConstantInt::getValue).codec();
   private final int value;

   public static ConstantInt of(int param0) {
      return param0 == 0 ? ZERO : new ConstantInt(param0);
   }

   private ConstantInt(int param0) {
      this.value = param0;
   }

   public int getValue() {
      return this.value;
   }

   @Override
   public int sample(RandomSource param0) {
      return this.value;
   }

   @Override
   public int getMinValue() {
      return this.value;
   }

   @Override
   public int getMaxValue() {
      return this.value;
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.CONSTANT;
   }

   @Override
   public String toString() {
      return Integer.toString(this.value);
   }
}
