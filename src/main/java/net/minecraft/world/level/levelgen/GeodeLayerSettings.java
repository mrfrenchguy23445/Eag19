package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class GeodeLayerSettings {
   private static final Codec<Double> LAYER_RANGE = Codec.doubleRange(0.01, 50.0);
   public static final Codec<GeodeLayerSettings> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            LAYER_RANGE.fieldOf("filling").orElse(1.7).forGetter(l_param0 -> l_param0.filling),
            LAYER_RANGE.fieldOf("inner_layer").orElse(2.2).forGetter(l_param0 -> l_param0.innerLayer),
            LAYER_RANGE.fieldOf("middle_layer").orElse(3.2).forGetter(l_param0 -> l_param0.middleLayer),
            LAYER_RANGE.fieldOf("outer_layer").orElse(4.2).forGetter(l_param0 -> l_param0.outerLayer)
         )
         .apply(param0, GeodeLayerSettings::new)
   );
   public final double filling;
   public final double innerLayer;
   public final double middleLayer;
   public final double outerLayer;

   public GeodeLayerSettings(double param0, double param2, double param4, double param6) {
      this.filling = param0;
      this.innerLayer = param2;
      this.middleLayer = param4;
      this.outerLayer = param6;
   }
}
