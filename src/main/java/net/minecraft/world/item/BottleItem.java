package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BottleItem extends Item {
   public BottleItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      List<AreaEffectCloud> var4 = param0.getEntitiesOfClass(
         AreaEffectCloud.class,
         param1.getBoundingBox().inflate(2.0),
         l_param0 -> l_param0 != null && l_param0.isAlive() && l_param0.getOwner() instanceof EnderDragon
      );
      ItemStack var5 = param1.getItemInHand(param2);
      if (!var4.isEmpty()) {
         AreaEffectCloud var6 = var4.get(0);
         var6.setRadius(var6.getRadius() - 0.5F);
         param0.playSound(null, param1.getX(), param1.getY(), param1.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
         param0.gameEvent(param1, GameEvent.FLUID_PICKUP, param1.position());
         if (param1 instanceof ServerPlayer var7) {
            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(var7, var5, var6);
         }

         return InteractionResultHolder.sidedSuccess(this.turnBottleIntoItem(var5, param1, new ItemStack(Items.DRAGON_BREATH)), param0.isClientSide());
      } else {
         HitResult var6 = getPlayerPOVHitResult(param0, param1, ClipContext.Fluid.SOURCE_ONLY);
         if (var6.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(var5);
         }

         if (var6.getType() == HitResult.Type.BLOCK) {
            BlockPos var7 = ((BlockHitResult)var6).getBlockPos();
            if (!param0.mayInteract(param1, var7)) {
               return InteractionResultHolder.pass(var5);
            }

            if (param0.getFluidState(var7).is(FluidTags.WATER)) {
               param0.playSound(param1, param1.getX(), param1.getY(), param1.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
               param0.gameEvent(param1, GameEvent.FLUID_PICKUP, var7);
               return InteractionResultHolder.sidedSuccess(
                  this.turnBottleIntoItem(var5, param1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), param0.isClientSide()
               );
            }
         }

         return InteractionResultHolder.pass(var5);
      }
   }

   protected ItemStack turnBottleIntoItem(ItemStack param0, Player param1, ItemStack param2) {
      param1.awardStat(Stats.ITEM_USED.get(this));
      return ItemUtils.createFilledResult(param0, param1, param2);
   }
}
