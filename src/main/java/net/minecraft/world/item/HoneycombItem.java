package net.minecraft.world.item;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HoneycombItem extends Item {
   public static final Supplier<BiMap<Block, Block>> WAXABLES = Suppliers.<BiMap<Block, Block>>memoize(
      () -> ImmutableBiMap.<Block, Block>builder()
         .put(Blocks.COPPER_BLOCK, Blocks.WAXED_COPPER_BLOCK)
         .put(Blocks.EXPOSED_COPPER, Blocks.WAXED_EXPOSED_COPPER)
         .put(Blocks.WEATHERED_COPPER, Blocks.WAXED_WEATHERED_COPPER)
         .put(Blocks.OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_COPPER)
         .put(Blocks.CUT_COPPER, Blocks.WAXED_CUT_COPPER)
         .put(Blocks.EXPOSED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER)
         .put(Blocks.WEATHERED_CUT_COPPER, Blocks.WAXED_WEATHERED_CUT_COPPER)
         .put(Blocks.OXIDIZED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER)
         .put(Blocks.CUT_COPPER_SLAB, Blocks.WAXED_CUT_COPPER_SLAB)
         .put(Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB)
         .put(Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB)
         .put(Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB)
         .put(Blocks.CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER_STAIRS)
         .put(Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS)
         .put(Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS)
         .put(Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS)
         .build()
   );
   public static final Supplier<BiMap<Block, Block>> WAX_OFF_BY_BLOCK = Suppliers.<BiMap<Block, Block>>memoize(() -> WAXABLES.get().inverse());

   public HoneycombItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      return getWaxed(var4).map(l_param3 -> {
         Player l_var4 = param0.getPlayer();
         ItemStack l_var5 = param0.getItemInHand();
         if (l_var4 instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)l_var4, var3, l_var5);
         }

         l_var5.shrink(1);
         var2.setBlock(var3, l_param3, 11);
         var2.gameEvent(GameEvent.BLOCK_CHANGE, var3, GameEvent.Context.of(l_var4, l_param3));
         var2.levelEvent(l_var4, 3003, var3, 0);
         return InteractionResult.sidedSuccess(var2.isClientSide);
      }).orElse(InteractionResult.PASS);
   }

   public static Optional<BlockState> getWaxed(BlockState param0) {
      return Optional.ofNullable(WAXABLES.get().get(param0.getBlock())).map(l_param1 -> l_param1.withPropertiesOf(param0));
   }
}
