package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CelebrateVillagersSurvivedRaid extends Behavior<Villager> {
   @Nullable
   private Raid currentRaid;

   public CelebrateVillagersSurvivedRaid(int param0, int param1) {
      super(ImmutableMap.of(), param0, param1);
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      BlockPos var3 = param1.blockPosition();
      this.currentRaid = param0.getRaidAt(var3);
      return this.currentRaid != null && this.currentRaid.isVictory() && MoveToSkySeeingSpot.hasNoBlocksAbove(param0, param1, var3);
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      return this.currentRaid != null && !this.currentRaid.isStopped();
   }

   protected void stop(ServerLevel param0, Villager param1, long param2) {
      this.currentRaid = null;
      param1.getBrain().updateActivityFromSchedule(param0.getDayTime(), param0.getGameTime());
   }

   protected void tick(ServerLevel param0, Villager param1, long param2) {
      RandomSource var5 = param1.getRandom();
      if (var5.nextInt(100) == 0) {
         param1.playCelebrateSound();
      }

      if (var5.nextInt(200) == 0 && MoveToSkySeeingSpot.hasNoBlocksAbove(param0, param1, param1.blockPosition())) {
         DyeColor var6 = Util.getRandom(DyeColor.values(), var5);
         int var7 = var5.nextInt(3);
         ItemStack var8 = this.getFirework(var6, var7);
         FireworkRocketEntity var9 = new FireworkRocketEntity(param1.level, param1, param1.getX(), param1.getEyeY(), param1.getZ(), var8);
         param1.level.addFreshEntity(var9);
      }
   }

   private ItemStack getFirework(DyeColor param0, int param1) {
      ItemStack var3 = new ItemStack(Items.FIREWORK_ROCKET, 1);
      ItemStack var4 = new ItemStack(Items.FIREWORK_STAR);
      CompoundTag var5 = var4.getOrCreateTagElement("Explosion");
      List<Integer> var6 = Lists.newArrayList();
      var6.add(param0.getFireworkColor());
      var5.putIntArray("Colors", var6);
      var5.putByte("Type", (byte)FireworkRocketItem.Shape.BURST.getId());
      CompoundTag var7 = var3.getOrCreateTagElement("Fireworks");
      ListTag var8 = new ListTag();
      CompoundTag var9 = var4.getTagElement("Explosion");
      if (var9 != null) {
         var8.add(var9);
      }

      var7.putByte("Flight", (byte)param1);
      if (!var8.isEmpty()) {
         var7.put("Explosions", var8);
      }

      return var3;
   }
}
