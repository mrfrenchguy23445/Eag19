package net.minecraft.world.entity.animal;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public interface Bucketable {
   boolean fromBucket();

   void setFromBucket(boolean var1);

   void saveToBucketTag(ItemStack var1);

   void loadFromBucketTag(CompoundTag var1);

   ItemStack getBucketItemStack();

   SoundEvent getPickupSound();

   @Deprecated
   static void saveDefaultDataToBucketTag(Mob param0, ItemStack param1) {
      CompoundTag var2 = param1.getOrCreateTag();
      if (param0.hasCustomName()) {
         param1.setHoverName(param0.getCustomName());
      }

      if (param0.isNoAi()) {
         var2.putBoolean("NoAI", param0.isNoAi());
      }

      if (param0.isSilent()) {
         var2.putBoolean("Silent", param0.isSilent());
      }

      if (param0.isNoGravity()) {
         var2.putBoolean("NoGravity", param0.isNoGravity());
      }

      if (param0.hasGlowingTag()) {
         var2.putBoolean("Glowing", param0.hasGlowingTag());
      }

      if (param0.isInvulnerable()) {
         var2.putBoolean("Invulnerable", param0.isInvulnerable());
      }

      var2.putFloat("Health", param0.getHealth());
   }

   @Deprecated
   static void loadDefaultDataFromBucketTag(Mob param0, CompoundTag param1) {
      if (param1.contains("NoAI")) {
         param0.setNoAi(param1.getBoolean("NoAI"));
      }

      if (param1.contains("Silent")) {
         param0.setSilent(param1.getBoolean("Silent"));
      }

      if (param1.contains("NoGravity")) {
         param0.setNoGravity(param1.getBoolean("NoGravity"));
      }

      if (param1.contains("Glowing")) {
         param0.setGlowingTag(param1.getBoolean("Glowing"));
      }

      if (param1.contains("Invulnerable")) {
         param0.setInvulnerable(param1.getBoolean("Invulnerable"));
      }

      if (param1.contains("Health", 99)) {
         param0.setHealth(param1.getFloat("Health"));
      }
   }

   static <T extends LivingEntity & Bucketable> Optional<InteractionResult> bucketMobPickup(Player param0, InteractionHand param1, T param2) {
      ItemStack var3 = param0.getItemInHand(param1);
      if (var3.getItem() == Items.WATER_BUCKET && param2.isAlive()) {
         param2.playSound(param2.getPickupSound(), 1.0F, 1.0F);
         ItemStack var4 = param2.getBucketItemStack();
         param2.saveToBucketTag(var4);
         ItemStack var5 = ItemUtils.createFilledResult(var3, param0, var4, false);
         param0.setItemInHand(param1, var5);
         Level var6 = param2.level;
         if (!var6.isClientSide) {
            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)param0, var4);
         }

         param2.discard();
         return Optional.of(InteractionResult.sidedSuccess(var6.isClientSide));
      } else {
         return Optional.empty();
      }
   }
}
