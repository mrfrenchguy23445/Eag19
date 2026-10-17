package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum DripstoneThickness implements StringRepresentable {
   TIP_MERGE("tip_merge"),
   TIP("tip"),
   FRUSTUM("frustum"),
   MIDDLE("middle"),
   BASE("base");

   private final String name;

   DripstoneThickness(String param2) {
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
