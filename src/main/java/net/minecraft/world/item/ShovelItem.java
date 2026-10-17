package net.minecraft.world.item;

import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class ShovelItem extends DiggerItem {
   protected static final Map<Block, BlockState> FLATTENABLES = Maps.newHashMap(
      new Builder()
         .put(Blocks.GRASS_BLOCK, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.DIRT, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.PODZOL, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.COARSE_DIRT, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.MYCELIUM, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.ROOTED_DIRT, Blocks.DIRT_PATH.defaultBlockState())
         .build()
   );

   public ShovelItem(Tier param0, float param1, float param2, Item.Properties param3) {
      super(param1, param2, param0, BlockTags.MINEABLE_WITH_SHOVEL, param3);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      if (param0.getClickedFace() == Direction.DOWN) {
         return InteractionResult.PASS;
      }

      Player var5 = param0.getPlayer();
      BlockState var6 = FLATTENABLES.get(var4.getBlock());
      BlockState var7 = null;
      if (var6 != null && var2.getBlockState(var3.above()).isAir()) {
         var2.playSound(var5, var3, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
         var7 = var6;
      } else if (var4.getBlock() instanceof CampfireBlock && var4.getValue(CampfireBlock.LIT)) {
         if (!var2.isClientSide()) {
            var2.levelEvent(null, 1009, var3, 0);
         }

         CampfireBlock.dowse(param0.getPlayer(), var2, var3, var4);
         var7 = var4.setValue(CampfireBlock.LIT, false);
      }

      if (var7 != null) {
         if (!var2.isClientSide) {
            var2.setBlock(var3, var7, 11);
            var2.gameEvent(GameEvent.BLOCK_CHANGE, var3, GameEvent.Context.of(var5, var7));
            if (var5 != null) {
               param0.getItemInHand().hurtAndBreak(1, var5, l_param1 -> l_param1.broadcastBreakEvent(param0.getHand()));
            }
         }

         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }
}
