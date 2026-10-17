package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CaveVines {
   VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
   BooleanProperty BERRIES = BlockStateProperties.BERRIES;

   static InteractionResult use(@Nullable Entity param0, BlockState param1, Level param2, BlockPos param3) {
      if (param1.getValue(BERRIES)) {
         Block.popResource(param2, param3, new ItemStack(Items.GLOW_BERRIES, 1));
         float var4 = Mth.randomBetween(param2.random, 0.8F, 1.2F);
         param2.playSound((Player)null, param3, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, var4);
         BlockState var5 = param1.setValue(BERRIES, false);
         param2.setBlock(param3, var5, 2);
         param2.gameEvent(GameEvent.BLOCK_CHANGE, param3, GameEvent.Context.of(param0, var5));
         return InteractionResult.sidedSuccess(param2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   static boolean hasGlowBerries(BlockState param0) {
      return param0.hasProperty(BERRIES) && param0.getValue(BERRIES);
   }

   static ToIntFunction<BlockState> emission(int param0) {
      return l_param1 -> l_param1.getValue(BlockStateProperties.BERRIES) ? param0 : 0;
   }
}
