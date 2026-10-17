package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum Half implements StringRepresentable {
   TOP("top"),
   BOTTOM("bottom");

   private final String name;

   Half(String param2) {
      this.name = param2;
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
