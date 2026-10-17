package net.minecraft.world.entity;

import java.util.EnumSet;
import java.util.Set;

public enum RelativeMovement {
   X(0),
   Y(1),
   Z(2),
   Y_ROT(3),
   X_ROT(4);

   public static final Set<RelativeMovement> ALL = Set.of(values());
   public static final Set<RelativeMovement> ROTATION = Set.of(X_ROT, Y_ROT);
   private final int bit;

   RelativeMovement(int param2) {
      this.bit = param2;
   }

   private int getMask() {
      return 1 << this.bit;
   }

   private boolean isSet(int param0) {
      return (param0 & this.getMask()) == this.getMask();
   }

   public static Set<RelativeMovement> unpack(int param0) {
      Set<RelativeMovement> var1 = EnumSet.noneOf(RelativeMovement.class);

      for (RelativeMovement var5 : values()) {
         if (var5.isSet(param0)) {
            var1.add(var5);
         }
      }

      return var1;
   }

   public static int pack(Set<RelativeMovement> param0) {
      int var1 = 0;

      for (RelativeMovement var3 : param0) {
         var1 |= var3.getMask();
      }

      return var1;
   }
}
