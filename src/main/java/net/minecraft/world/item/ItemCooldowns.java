package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.Mth;

public class ItemCooldowns {
   private final Map<Item, ItemCooldowns.CooldownInstance> cooldowns = Maps.newHashMap();
   private int tickCount;

   public boolean isOnCooldown(Item param0) {
      return this.getCooldownPercent(param0, 0.0F) > 0.0F;
   }

   public float getCooldownPercent(Item param0, float param1) {
      ItemCooldowns.CooldownInstance var3 = this.cooldowns.get(param0);
      if (var3 != null) {
         float var4 = var3.endTime - var3.startTime;
         float var5 = var3.endTime - (this.tickCount + param1);
         return Mth.clamp(var5 / var4, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public void tick() {
      this.tickCount++;
      if (!this.cooldowns.isEmpty()) {
         Iterator<Entry<Item, ItemCooldowns.CooldownInstance>> var1 = this.cooldowns.entrySet().iterator();

         while (var1.hasNext()) {
            Entry<Item, ItemCooldowns.CooldownInstance> var2 = var1.next();
            if (var2.getValue().endTime <= this.tickCount) {
               var1.remove();
               this.onCooldownEnded(var2.getKey());
            }
         }
      }
   }

   public void addCooldown(Item param0, int param1) {
      this.cooldowns.put(param0, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + param1));
      this.onCooldownStarted(param0, param1);
   }

   public void removeCooldown(Item param0) {
      this.cooldowns.remove(param0);
      this.onCooldownEnded(param0);
   }

   protected void onCooldownStarted(Item param0, int param1) {
   }

   protected void onCooldownEnded(Item param0) {
   }

   static class CooldownInstance {
      final int startTime;
      final int endTime;

      CooldownInstance(int param0, int param1) {
         this.startTime = param0;
         this.endTime = param1;
      }
   }
}
