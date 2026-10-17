package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.util.RandomSource;

public class TrapezoidFloat extends FloatProvider {
   public static final Codec<TrapezoidFloat> CODEC = RecordCodecBuilder.<TrapezoidFloat>create(
         param0 -> param0.group(
               Codec.FLOAT.fieldOf("min").forGetter(l_param0 -> l_param0.min),
               Codec.FLOAT.fieldOf("max").forGetter(l_param0 -> l_param0.max),
               Codec.FLOAT.fieldOf("plateau").forGetter(l_param0 -> l_param0.plateau)
            )
            .apply(param0, TrapezoidFloat::new)
      )
      .comapFlatMap(
         param0 -> {
            if (param0.max < param0.min) {
               return DataResult.error(() -> "Max must be larger than min: [" + param0.min + ", " + param0.max + "]");
            } else {
               return param0.plateau > param0.max - param0.min
                  ? DataResult.error(() -> "Plateau can at most be the full span: [" + param0.min + ", " + param0.max + "]")
                  : DataResult.success((TrapezoidFloat)param0);
            }
         },
         Function.identity()
      );
   private final float min;
   private final float max;
   private final float plateau;

   public static TrapezoidFloat of(float param0, float param1, float param2) {
      return new TrapezoidFloat(param0, param1, param2);
   }

   private TrapezoidFloat(float param0, float param1, float param2) {
      this.min = param0;
      this.max = param1;
      this.plateau = param2;
   }

   @Override
   public float sample(RandomSource param0) {
      float var2 = this.max - this.min;
      float var3 = (var2 - this.plateau) / 2.0F;
      float var4 = var2 - var3;
      return this.min + param0.nextFloat() * var4 + param0.nextFloat() * var3;
   }

   @Override
   public float getMinValue() {
      return this.min;
   }

   @Override
   public float getMaxValue() {
      return this.max;
   }

   @Override
   public FloatProviderType<?> getType() {
      return FloatProviderType.TRAPEZOID;
   }

   @Override
   public String toString() {
      return "trapezoid(" + this.plateau + ") in [" + this.min + "-" + this.max + "]";
   }
}
