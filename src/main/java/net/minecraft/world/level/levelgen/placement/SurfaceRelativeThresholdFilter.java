package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceRelativeThresholdFilter extends PlacementFilter {
   public static final Codec<SurfaceRelativeThresholdFilter> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(l_param0 -> l_param0.heightmap),
            Codec.INT.optionalFieldOf("min_inclusive", Integer.MIN_VALUE).forGetter(l_param0 -> l_param0.minInclusive),
            Codec.INT.optionalFieldOf("max_inclusive", Integer.MAX_VALUE).forGetter(l_param0 -> l_param0.maxInclusive)
         )
         .apply(param0, SurfaceRelativeThresholdFilter::new)
   );
   private final Heightmap.Types heightmap;
   private final int minInclusive;
   private final int maxInclusive;

   private SurfaceRelativeThresholdFilter(Heightmap.Types param0, int param1, int param2) {
      this.heightmap = param0;
      this.minInclusive = param1;
      this.maxInclusive = param2;
   }

   public static SurfaceRelativeThresholdFilter of(Heightmap.Types param0, int param1, int param2) {
      return new SurfaceRelativeThresholdFilter(param0, param1, param2);
   }

   @Override
   protected boolean shouldPlace(PlacementContext param0, RandomSource param1, BlockPos param2) {
      long var4 = param0.getHeight(this.heightmap, param2.getX(), param2.getZ());
      long var6 = var4 + this.minInclusive;
      long var8 = var4 + this.maxInclusive;
      return var6 <= param2.getY() && param2.getY() <= var8;
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.SURFACE_RELATIVE_THRESHOLD_FILTER;
   }
}
