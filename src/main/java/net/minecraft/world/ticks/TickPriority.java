package net.minecraft.world.ticks;

public enum TickPriority {
   EXTREMELY_HIGH(-3),
   VERY_HIGH(-2),
   HIGH(-1),
   NORMAL(0),
   LOW(1),
   VERY_LOW(2),
   EXTREMELY_LOW(3);

   private final int value;

   TickPriority(int param2) {
      this.value = param2;
   }

   public static TickPriority byValue(int param0) {
      for (TickPriority var4 : values()) {
         if (var4.value == param0) {
            return var4;
         }
      }

      return param0 < EXTREMELY_HIGH.value ? EXTREMELY_HIGH : EXTREMELY_LOW;
   }

   public int getValue() {
      return this.value;
   }
}
