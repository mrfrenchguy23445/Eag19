package net.minecraft.world.entity.ai.sensing;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public class Sensing {
   private final Mob mob;
   private final IntSet seen = new IntOpenHashSet();
   private final IntSet unseen = new IntOpenHashSet();

   public Sensing(Mob param0) {
      this.mob = param0;
   }

   public void tick() {
      this.seen.clear();
      this.unseen.clear();
   }

   public boolean hasLineOfSight(Entity param0) {
      int var2 = param0.getId();
      if (this.seen.contains(var2)) {
         return true;
      }

      if (this.unseen.contains(var2)) {
         return false;
      }

      this.mob.level.getProfiler().push("hasLineOfSight");
      boolean var3 = this.mob.hasLineOfSight(param0);
      this.mob.level.getProfiler().pop();
      if (var3) {
         this.seen.add(var2);
      } else {
         this.unseen.add(var2);
      }

      return var3;
   }
}
