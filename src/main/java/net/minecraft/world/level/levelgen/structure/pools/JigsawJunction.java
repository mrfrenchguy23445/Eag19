package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

public class JigsawJunction {
   private final int sourceX;
   private final int sourceGroundY;
   private final int sourceZ;
   private final int deltaY;
   private final StructureTemplatePool.Projection destProjection;

   public JigsawJunction(int param0, int param1, int param2, int param3, StructureTemplatePool.Projection param4) {
      this.sourceX = param0;
      this.sourceGroundY = param1;
      this.sourceZ = param2;
      this.deltaY = param3;
      this.destProjection = param4;
   }

   public int getSourceX() {
      return this.sourceX;
   }

   public int getSourceGroundY() {
      return this.sourceGroundY;
   }

   public int getSourceZ() {
      return this.sourceZ;
   }

   public int getDeltaY() {
      return this.deltaY;
   }

   public StructureTemplatePool.Projection getDestProjection() {
      return this.destProjection;
   }

   public <T> Dynamic<T> serialize(DynamicOps<T> param0) {
      Builder<T, T> var2 = ImmutableMap.builder();
      var2.put(param0.createString("source_x"), param0.createInt(this.sourceX))
         .put(param0.createString("source_ground_y"), param0.createInt(this.sourceGroundY))
         .put(param0.createString("source_z"), param0.createInt(this.sourceZ))
         .put(param0.createString("delta_y"), param0.createInt(this.deltaY))
         .put(param0.createString("dest_proj"), param0.createString(this.destProjection.getName()));
      return new Dynamic<>(param0, param0.createMap(var2.build()));
   }

   public static <T> JigsawJunction deserialize(Dynamic<T> param0) {
      return new JigsawJunction(
         param0.get("source_x").asInt(0),
         param0.get("source_ground_y").asInt(0),
         param0.get("source_z").asInt(0),
         param0.get("delta_y").asInt(0),
         StructureTemplatePool.Projection.byName(param0.get("dest_proj").asString(""))
      );
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      }

      if (param0 != null && this.getClass() == param0.getClass()) {
         JigsawJunction var2 = (JigsawJunction)param0;
         if (this.sourceX != var2.sourceX) {
            return false;
         } else if (this.sourceZ != var2.sourceZ) {
            return false;
         } else {
            return this.deltaY != var2.deltaY ? false : this.destProjection == var2.destProjection;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.sourceX;
      var1 = 31 * var1 + this.sourceGroundY;
      var1 = 31 * var1 + this.sourceZ;
      var1 = 31 * var1 + this.deltaY;
      return 31 * var1 + this.destProjection.hashCode();
   }

   @Override
   public String toString() {
      return "JigsawJunction{sourceX="
         + this.sourceX
         + ", sourceGroundY="
         + this.sourceGroundY
         + ", sourceZ="
         + this.sourceZ
         + ", deltaY="
         + this.deltaY
         + ", destProjection="
         + this.destProjection
         + "}";
   }
}
