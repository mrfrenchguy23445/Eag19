package net.minecraft.world.entity.animal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

public abstract class ShoulderRidingEntity extends TamableAnimal {
   private static final int RIDE_COOLDOWN = 100;
   private int rideCooldownCounter;

   protected ShoulderRidingEntity(EntityType<? extends ShoulderRidingEntity> param0, Level param1) {
      super(param0, param1);
   }

   public boolean setEntityOnShoulder(ServerPlayer param0) {
      CompoundTag var2 = new CompoundTag();
      var2.putString("id", this.getEncodeId());
      this.saveWithoutId(var2);
      if (param0.setEntityOnShoulder(var2)) {
         this.discard();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      this.rideCooldownCounter++;
      super.tick();
   }

   public boolean canSitOnShoulder() {
      return this.rideCooldownCounter > 100;
   }
}
