package net.minecraft.world.entity;

public enum EquipmentSlot {
   MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
   OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
   FEET(EquipmentSlot.Type.ARMOR, 0, 1, "feet"),
   LEGS(EquipmentSlot.Type.ARMOR, 1, 2, "legs"),
   CHEST(EquipmentSlot.Type.ARMOR, 2, 3, "chest"),
   HEAD(EquipmentSlot.Type.ARMOR, 3, 4, "head");

   private final EquipmentSlot.Type type;
   private final int index;
   private final int filterFlag;
   private final String name;

   EquipmentSlot(EquipmentSlot.Type param2, int param3, int param4, String param5) {
      this.type = param2;
      this.index = param3;
      this.filterFlag = param4;
      this.name = param5;
   }

   public EquipmentSlot.Type getType() {
      return this.type;
   }

   public int getIndex() {
      return this.index;
   }

   public int getIndex(int param0) {
      return param0 + this.index;
   }

   public int getFilterFlag() {
      return this.filterFlag;
   }

   public String getName() {
      return this.name;
   }

   public boolean isArmor() {
      return this.type == EquipmentSlot.Type.ARMOR;
   }

   public static EquipmentSlot byName(String param0) {
      for (EquipmentSlot var4 : values()) {
         if (var4.getName().equals(param0)) {
            return var4;
         }
      }

      throw new IllegalArgumentException("Invalid slot '" + param0 + "'");
   }

   public static EquipmentSlot byTypeAndIndex(EquipmentSlot.Type param0, int param1) {
      for (EquipmentSlot var5 : values()) {
         if (var5.getType() == param0 && var5.getIndex() == param1) {
            return var5;
         }
      }

      throw new IllegalArgumentException("Invalid slot '" + param0 + "': " + param1);
   }

   public enum Type {
      HAND,
      ARMOR;
   }
}
