package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AttachedToLeavesDecorator extends TreeDecorator {
   public static final Codec<AttachedToLeavesDecorator> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(l_param0 -> l_param0.probability),
            Codec.intRange(0, 16).fieldOf("exclusion_radius_xz").forGetter(l_param0 -> l_param0.exclusionRadiusXZ),
            Codec.intRange(0, 16).fieldOf("exclusion_radius_y").forGetter(l_param0 -> l_param0.exclusionRadiusY),
            BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(l_param0 -> l_param0.blockProvider),
            Codec.intRange(1, 16).fieldOf("required_empty_blocks").forGetter(l_param0 -> l_param0.requiredEmptyBlocks),
            ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions").forGetter(l_param0 -> l_param0.directions)
         )
         .apply(param0, AttachedToLeavesDecorator::new)
   );
   protected final float probability;
   protected final int exclusionRadiusXZ;
   protected final int exclusionRadiusY;
   protected final BlockStateProvider blockProvider;
   protected final int requiredEmptyBlocks;
   protected final List<Direction> directions;

   public AttachedToLeavesDecorator(float param0, int param1, int param2, BlockStateProvider param3, int param4, List<Direction> param5) {
      this.probability = param0;
      this.exclusionRadiusXZ = param1;
      this.exclusionRadiusY = param2;
      this.blockProvider = param3;
      this.requiredEmptyBlocks = param4;
      this.directions = param5;
   }

   @Override
   public void place(TreeDecorator.Context param0) {
      Set<BlockPos> var2 = new HashSet<>();
      RandomSource var3 = param0.random();

      for (BlockPos var5 : Util.shuffledCopy(param0.leaves(), var3)) {
         Direction var6 = Util.getRandom(this.directions, var3);
         BlockPos var7 = var5.relative(var6);
         if (!var2.contains(var7) && var3.nextFloat() < this.probability && this.hasRequiredEmptyBlocks(param0, var5, var6)) {
            BlockPos var8 = var7.offset(-this.exclusionRadiusXZ, -this.exclusionRadiusY, -this.exclusionRadiusXZ);
            BlockPos var9 = var7.offset(this.exclusionRadiusXZ, this.exclusionRadiusY, this.exclusionRadiusXZ);

            for (BlockPos var11 : BlockPos.betweenClosed(var8, var9)) {
               var2.add(var11.immutable());
            }

            param0.setBlock(var7, this.blockProvider.getState(var3, var7));
         }
      }
   }

   private boolean hasRequiredEmptyBlocks(TreeDecorator.Context param0, BlockPos param1, Direction param2) {
      for (int var4 = 1; var4 <= this.requiredEmptyBlocks; var4++) {
         BlockPos var5 = param1.relative(param2, var4);
         if (!param0.isAir(var5)) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ATTACHED_TO_LEAVES;
   }
}
