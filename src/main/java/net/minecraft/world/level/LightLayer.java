package net.minecraft.world.level;

public enum LightLayer {
   SKY(15),
   BLOCK(0);

   public final int surrounding;

   LightLayer(int param2) {
      this.surrounding = param2;
   }
}
