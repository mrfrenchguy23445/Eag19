package com.mojang.blaze3d.shaders;

public enum FogShape {
   SPHERE(0),
   CYLINDER(1);

   private final int index;

   FogShape(int param2) {
      this.index = param2;
   }

   public int getIndex() {
      return this.index;
   }
}
