package net.minecraft.network.syncher;

public class EntityDataAccessor<T> {
   private final int declaredId;
   private int id;
   private boolean assigned;
   private final EntityDataSerializer<T> serializer;

   public EntityDataAccessor(int param0, EntityDataSerializer<T> param1) {
      this.declaredId = param0;
      this.id = param0;
      this.serializer = param1;
   }

   // Accessors built by defineId() carry no id yet: see SynchedEntityData.define(), which
   // hands out the real one when the entity's constructor chain defines them in order.
   public boolean isAssigned() {
      return this.assigned || this.declaredId >= 0;
   }

   public void assignId(int param0) {
      this.id = param0;
      this.assigned = true;
   }

   public int getId() {
      return this.id;
   }

   public EntityDataSerializer<T> getSerializer() {
      return this.serializer;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 != null && this.getClass() == param0.getClass()) {
         EntityDataAccessor<?> var2 = (EntityDataAccessor<?>)param0;
         return this.id == var2.id;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id;
   }

   @Override
   public String toString() {
      return "<entity data: " + this.id + ">";
   }
}
