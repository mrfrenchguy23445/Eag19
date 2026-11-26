package net.minecraft.world.entity.ai.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;

public class LongJumpToPreferredBlock<E extends Mob> extends LongJumpToRandomPos<E> {
   private final TagKey<Block> preferredBlockTag;
   private final float preferredBlocksChance;
   private final List<LongJumpToRandomPos.PossibleJump> notPrefferedJumpCandidates = new ArrayList<>();
   private boolean currentlyWantingPreferredOnes;

   public LongJumpToPreferredBlock(
      UniformInt param0,
      int param1,
      int param2,
      float param3,
      Function<E, SoundEvent> param4,
      TagKey<Block> param5,
      float param6,
      BiPredicate<E, BlockPos> param7
   ) {
      super(param0, param1, param2, param3, param4, param7);
      this.preferredBlockTag = param5;
      this.preferredBlocksChance = param6;
   }

   @Override
   protected void start(ServerLevel param0, E param1, long param2) {
      super.start(param0, param1, param2);
      this.notPrefferedJumpCandidates.clear();
      this.currentlyWantingPreferredOnes = param1.getRandom().nextFloat() < this.preferredBlocksChance;
   }

   @Override
   protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel param0) {
      if (!this.currentlyWantingPreferredOnes) {
         return super.getJumpCandidate(param0);
      }

      BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();

      while (!this.jumpCandidates.isEmpty()) {
         Optional<LongJumpToRandomPos.PossibleJump> var3 = super.getJumpCandidate(param0);
         if (var3.isPresent()) {
            LongJumpToRandomPos.PossibleJump var4 = var3.get();
            if (param0.getBlockState(var2.setWithOffset(var4.getJumpTarget(), Direction.DOWN)).is(this.preferredBlockTag)) {
               return var3;
            }

            this.notPrefferedJumpCandidates.add(var4);
         }
      }

      return !this.notPrefferedJumpCandidates.isEmpty() ? Optional.of(this.notPrefferedJumpCandidates.remove(0)) : Optional.empty();
   }
}
