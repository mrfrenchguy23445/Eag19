package net.minecraft.world.entity;

import net.minecraft.util.OptionEnum;

public enum HumanoidArm implements OptionEnum {
   LEFT(0, "options.mainHand.left"),
   RIGHT(1, "options.mainHand.right");

   private final int id;
   private final String name;

   HumanoidArm(int param2, String param3) {
      this.id = param2;
      this.name = param3;
   }

   public HumanoidArm getOpposite() {
      return this == LEFT ? RIGHT : LEFT;
   }

   @Override
   public int getId() {
      return this.id;
   }

   @Override
   public String getKey() {
      return this.name;
   }
}
