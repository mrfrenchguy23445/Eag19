package net.minecraft.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class ChorusFruitItem extends Item {
   public ChorusFruitItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public ItemStack finishUsingItem(ItemStack param0, Level param1, LivingEntity param2) {
      ItemStack var4 = super.finishUsingItem(param0, param1, param2);
      if (!param1.isClientSide) {
         double var5 = param2.getX();
         double var7 = param2.getY();
         double var9 = param2.getZ();

         for (int var11 = 0; var11 < 16; var11++) {
            double var12 = param2.getX() + (param2.getRandom().nextDouble() - 0.5) * 16.0;
            double var14 = Mth.clamp(
               param2.getY() + (param2.getRandom().nextInt(16) - 8),
               param1.getMinBuildHeight(),
               param1.getMinBuildHeight() + ((ServerLevel)param1).getLogicalHeight() - 1
            );
            double var16 = param2.getZ() + (param2.getRandom().nextDouble() - 0.5) * 16.0;
            if (param2.isPassenger()) {
               param2.stopRiding();
            }

            Vec3 var18 = param2.position();
            if (param2.randomTeleport(var12, var14, var16, true)) {
               param1.gameEvent(GameEvent.TELEPORT, var18, GameEvent.Context.of(param2));
               SoundEvent var19 = param2 instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
               param1.playSound(null, var5, var7, var9, var19, SoundSource.PLAYERS, 1.0F, 1.0F);
               param2.playSound(var19, 1.0F, 1.0F);
               break;
            }
         }

         if (param2 instanceof Player) {
            ((Player)param2).getCooldowns().addCooldown(this, 20);
         }
      }

      return var4;
   }
}
