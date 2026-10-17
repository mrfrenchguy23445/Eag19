package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HoeItem extends DiggerItem {
   protected static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = Maps.newHashMap(
      ImmutableMap.of(
         Blocks.GRASS_BLOCK,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
         Blocks.DIRT_PATH,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
         Blocks.DIRT,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
         Blocks.COARSE_DIRT,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.DIRT.defaultBlockState())),
         Blocks.ROOTED_DIRT,
         Pair.of(param0 -> true, changeIntoStateAndDropItem(Blocks.DIRT.defaultBlockState(), Items.HANGING_ROOTS))
      )
   );

   protected HoeItem(Tier param0, int param1, float param2, Item.Properties param3) {
      super(param1, param2, param0, BlockTags.MINEABLE_WITH_HOE, param3);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> var4 = TILLABLES.get(var2.getBlockState(var3).getBlock());
      if (var4 == null) {
         return InteractionResult.PASS;
      }

      Predicate<UseOnContext> var5 = var4.getFirst();
      Consumer<UseOnContext> var6 = var4.getSecond();
      if (var5.test(param0)) {
         Player var7 = param0.getPlayer();
         var2.playSound(var7, var3, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!var2.isClientSide) {
            var6.accept(param0);
            if (var7 != null) {
               param0.getItemInHand().hurtAndBreak(1, var7, l_param1 -> l_param1.broadcastBreakEvent(param0.getHand()));
            }
         }

         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static Consumer<UseOnContext> changeIntoState(BlockState param0) {
      return l_param1 -> {
         l_param1.getLevel().setBlock(l_param1.getClickedPos(), param0, 11);
         l_param1.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, l_param1.getClickedPos(), GameEvent.Context.of(l_param1.getPlayer(), param0));
      };
   }

   public static Consumer<UseOnContext> changeIntoStateAndDropItem(BlockState param0, ItemLike param1) {
      return l_param2 -> {
         l_param2.getLevel().setBlock(l_param2.getClickedPos(), param0, 11);
         l_param2.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, l_param2.getClickedPos(), GameEvent.Context.of(l_param2.getPlayer(), param0));
         Block.popResourceFromFace(l_param2.getLevel(), l_param2.getClickedPos(), l_param2.getClickedFace(), new ItemStack(param1));
      };
   }

   public static boolean onlyIfAirAbove(UseOnContext param0) {
      return param0.getClickedFace() != Direction.DOWN && param0.getLevel().getBlockState(param0.getClickedPos().above()).isAir();
   }
}
