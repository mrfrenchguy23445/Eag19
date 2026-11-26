package net.minecraft.client.model.geom;

import net.minecraft.resources.ResourceLocation;

public final class ModelLayerLocation {
   private final ResourceLocation model;
   private final String layer;

   public ModelLayerLocation(ResourceLocation param0, String param1) {
      this.model = param0;
      this.layer = param1;
   }

   public ResourceLocation getModel() {
      return this.model;
   }

   public String getLayer() {
      return this.layer;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof ModelLayerLocation var2) ? false : this.model.equals(var2.model) && this.layer.equals(var2.layer);
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.model.hashCode();
      return 31 * var1 + this.layer.hashCode();
   }

   @Override
   public String toString() {
      return this.model + "#" + this.layer;
   }
}
