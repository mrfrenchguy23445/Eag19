package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RedstoneTorchBlock extends TorchBlock {
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   private static final Map<BlockGetter, List<RedstoneTorchBlock.Toggle>> RECENT_TOGGLES = new WeakHashMap<>();
   public static final int RECENT_TOGGLE_TIMER = 60;
   public static final int MAX_RECENT_TOGGLES = 8;
   public static final int RESTART_DELAY = 160;
   private static final int TOGGLE_DELAY = 2;

   protected RedstoneTorchBlock(BlockBehaviour.Properties param0) {
      super(param0, DustParticleOptions.REDSTONE);
      this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      for (Direction var9 : Direction.values()) {
         param1.updateNeighborsAt(param2.relative(var9), this);
      }
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param4) {
         for (Direction var9 : Direction.values()) {
            param1.updateNeighborsAt(param2.relative(var9), this);
         }
      }
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param0.getValue(LIT) && Direction.UP != param3 ? 15 : 0;
   }

   protected boolean hasNeighborSignal(Level param0, BlockPos param1, BlockState param2) {
      return param0.hasSignal(param1.below(), Direction.DOWN);
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      boolean var5 = this.hasNeighborSignal(param1, param2, param0);
      List<RedstoneTorchBlock.Toggle> var6 = RECENT_TOGGLES.get(param1);

      while (var6 != null && !var6.isEmpty() && param1.getGameTime() - var6.get(0).when > 60L) {
         var6.remove(0);
      }

      if (param0.getValue(LIT)) {
         if (var5) {
            param1.setBlock(param2, param0.setValue(LIT, false), 3);
            if (isToggledTooFrequently(param1, param2, true)) {
               param1.levelEvent(1502, param2, 0);
               param1.scheduleTick(param2, param1.getBlockState(param2).getBlock(), 160);
            }
         }
      } else if (!var5 && !isToggledTooFrequently(param1, param2, false)) {
         param1.setBlock(param2, param0.setValue(LIT, true), 3);
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (param0.getValue(LIT) == this.hasNeighborSignal(param1, param2, param0) && !param1.getBlockTicks().willTickThisTick(param2, this)) {
         param1.scheduleTick(param2, this, 2);
      }
   }

   @Override
   public int getDirectSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param3 == Direction.DOWN ? param0.getSignal(param1, param2, param3) : 0;
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LIT)) {
         double var5 = param2.getX() + 0.5 + (param3.nextDouble() - 0.5) * 0.2;
         double var7 = param2.getY() + 0.7 + (param3.nextDouble() - 0.5) * 0.2;
         double var9 = param2.getZ() + 0.5 + (param3.nextDouble() - 0.5) * 0.2;
         param1.addParticle(this.flameParticle, var5, var7, var9, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(LIT);
   }

   private static boolean isToggledTooFrequently(Level param0, BlockPos param1, boolean param2) {
      List<RedstoneTorchBlock.Toggle> var3 = RECENT_TOGGLES.computeIfAbsent(param0, l_param0 -> Lists.newArrayList());
      if (param2) {
         var3.add(new RedstoneTorchBlock.Toggle(param1.immutable(), param0.getGameTime()));
      }

      int var4 = 0;

      for (int var5 = 0; var5 < var3.size(); var5++) {
         RedstoneTorchBlock.Toggle var6 = var3.get(var5);
         if (var6.pos.equals(param1)) {
            if (++var4 >= 8) {
               return true;
            }
         }
      }

      return false;
   }

   public static class Toggle {
      final BlockPos pos;
      final long when;

      public Toggle(BlockPos param0, long param1) {
         this.pos = param0;
         this.when = param1;
      }
   }
}
