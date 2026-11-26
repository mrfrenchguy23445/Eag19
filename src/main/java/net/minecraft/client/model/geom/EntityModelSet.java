package net.minecraft.client.model.geom;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class EntityModelSet implements ResourceManagerReloadListener {
   private Map<ModelLayerLocation, LayerDefinition> roots = ImmutableMap.of();

   public ModelPart bakeLayer(ModelLayerLocation param0) {
      LayerDefinition var2 = this.roots.get(param0);
      if (var2 == null) {
         throw new IllegalArgumentException("No model for layer " + param0);
      } else {
         return var2.bakeRoot();
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      this.roots = ImmutableMap.copyOf(LayerDefinitions.createRoots());
   }
}
