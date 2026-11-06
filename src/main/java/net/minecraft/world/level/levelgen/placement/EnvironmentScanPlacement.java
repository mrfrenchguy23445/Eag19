package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class EnvironmentScanPlacement extends PlacementModifier {
   private final Direction directionOfSearch;
   private final BlockPredicate targetCondition;
   private final BlockPredicate allowedSearchCondition;
   private final int maxSteps;
   public static final Codec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Direction.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter(l_param0 -> l_param0.directionOfSearch),
            BlockPredicate.CODEC.fieldOf("target_condition").forGetter(l_param0 -> l_param0.targetCondition),
            BlockPredicate.CODEC
               .optionalFieldOf("allowed_search_condition", BlockPredicate.alwaysTrue())
               .forGetter(l_param0 -> l_param0.allowedSearchCondition),
            Codec.intRange(1, 32).fieldOf("max_steps").forGetter(l_param0 -> l_param0.maxSteps)
         )
         .apply(param0, EnvironmentScanPlacement::new)
   );

   private EnvironmentScanPlacement(Direction param0, BlockPredicate param1, BlockPredicate param2, int param3) {
      this.directionOfSearch = param0;
      this.targetCondition = param1;
      this.allowedSearchCondition = param2;
      this.maxSteps = param3;
   }

   public static EnvironmentScanPlacement scanningFor(Direction param0, BlockPredicate param1, BlockPredicate param2, int param3) {
      return new EnvironmentScanPlacement(param0, param1, param2, param3);
   }

   public static EnvironmentScanPlacement scanningFor(Direction param0, BlockPredicate param1, int param2) {
      return scanningFor(param0, param1, BlockPredicate.alwaysTrue(), param2);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext param0, RandomSource param1, BlockPos param2) {
      BlockPos.MutableBlockPos var4 = param2.mutable();
      WorldGenLevel var5 = param0.getLevel();
      if (!this.allowedSearchCondition.test(var5, var4)) {
         return Stream.of();
      }

      for (int var6 = 0; var6 < this.maxSteps; var6++) {
         if (this.targetCondition.test(var5, var4)) {
            return Stream.of(var4);
         }

         var4.move(this.directionOfSearch);
         if (var5.isOutsideBuildHeight(var4.getY())) {
            return Stream.of();
         }

         if (!this.allowedSearchCondition.test(var5, var4)) {
            break;
         }
      }

      return this.targetCondition.test(var5, var4) ? Stream.of(var4) : Stream.of();
   }

   @Override
   public void appendPositions(PlacementContext param0, RandomSource param1, BlockPos param2, List<BlockPos> param3) {
      // Cooperative-port (TeaVM wasm-gc): stream-free scan; consumes the same RNG-free
      // path. Results copied immutably (vanilla hands out the mutable instance as the
      // terminal stream element - safe because it stops moving on return).
      BlockPos.MutableBlockPos var4 = param2.mutable();
      WorldGenLevel var5 = param0.getLevel();
      if (!this.allowedSearchCondition.test(var5, var4)) {
         return;
      }

      for (int var6 = 0; var6 < this.maxSteps; var6++) {
         if (this.targetCondition.test(var5, var4)) {
            param3.add(new BlockPos(var4));
            return;
         }

         var4.move(this.directionOfSearch);
         if (var5.isOutsideBuildHeight(var4.getY())) {
            return;
         }

         if (!this.allowedSearchCondition.test(var5, var4)) {
            break;
         }
      }

      if (this.targetCondition.test(var5, var4)) {
         param3.add(new BlockPos(var4));
      }
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.ENVIRONMENT_SCAN;
   }
}
