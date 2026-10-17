package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class LeadItem extends Item {
   public LeadItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      if (var4.is(BlockTags.FENCES)) {
         Player var5 = param0.getPlayer();
         if (!var2.isClientSide && var5 != null) {
            bindPlayerMobs(var5, var2, var3);
         }

         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static InteractionResult bindPlayerMobs(Player param0, Level param1, BlockPos param2) {
      LeashFenceKnotEntity var3 = null;
      boolean var4 = false;
      double var5 = 7.0;
      int var7 = param2.getX();
      int var8 = param2.getY();
      int var9 = param2.getZ();

      for (Mob var12 : param1.getEntitiesOfClass(Mob.class, new AABB(var7 - 7.0, var8 - 7.0, var9 - 7.0, var7 + 7.0, var8 + 7.0, var9 + 7.0))) {
         if (var12.getLeashHolder() == param0) {
            if (var3 == null) {
               var3 = LeashFenceKnotEntity.getOrCreateKnot(param1, param2);
               var3.playPlacementSound();
            }

            var12.setLeashedTo(var3, true);
            var4 = true;
         }
      }

      if (var4) {
         param1.gameEvent(GameEvent.BLOCK_ATTACH, param2, GameEvent.Context.of(param0));
      }

      return var4 ? InteractionResult.SUCCESS : InteractionResult.PASS;
   }
}
