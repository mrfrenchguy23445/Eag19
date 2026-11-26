package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class AxisAlignedLinearPosTest extends PosRuleTest {
   public static final Codec<AxisAlignedLinearPosTest> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter(l_param0 -> l_param0.minChance),
            Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter(l_param0 -> l_param0.maxChance),
            Codec.INT.fieldOf("min_dist").orElse(0).forGetter(l_param0 -> l_param0.minDist),
            Codec.INT.fieldOf("max_dist").orElse(0).forGetter(l_param0 -> l_param0.maxDist),
            Direction.Axis.CODEC.fieldOf("axis").orElse(Direction.Axis.Y).forGetter(l_param0 -> l_param0.axis)
         )
         .apply(param0, AxisAlignedLinearPosTest::new)
   );
   private final float minChance;
   private final float maxChance;
   private final int minDist;
   private final int maxDist;
   private final Direction.Axis axis;

   public AxisAlignedLinearPosTest(float param0, float param1, int param2, int param3, Direction.Axis param4) {
      if (param2 >= param3) {
         throw new IllegalArgumentException("Invalid range: [" + param2 + "," + param3 + "]");
      }

      this.minChance = param0;
      this.maxChance = param1;
      this.minDist = param2;
      this.maxDist = param3;
      this.axis = param4;
   }

   @Override
   public boolean test(BlockPos param0, BlockPos param1, BlockPos param2, RandomSource param3) {
      Direction var5 = Direction.get(Direction.AxisDirection.POSITIVE, this.axis);
      float var6 = Math.abs((param1.getX() - param2.getX()) * var5.getStepX());
      float var7 = Math.abs((param1.getY() - param2.getY()) * var5.getStepY());
      float var8 = Math.abs((param1.getZ() - param2.getZ()) * var5.getStepZ());
      int var9 = (int)(var6 + var7 + var8);
      float var10 = param3.nextFloat();
      return var10 <= Mth.clampedLerp(this.minChance, this.maxChance, Mth.inverseLerp(var9, this.minDist, this.maxDist));
   }

   @Override
   protected PosRuleTestType<?> getType() {
      return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
   }
}
