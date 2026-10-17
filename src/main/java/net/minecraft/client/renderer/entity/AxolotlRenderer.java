package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

public class AxolotlRenderer extends MobRenderer<Axolotl, AxolotlModel<Axolotl>> {
   private static final Map<Axolotl.Variant, ResourceLocation> TEXTURE_BY_TYPE = Util.make(Maps.newHashMap(), param0 -> {
      for (Axolotl.Variant var4 : Axolotl.Variant.values()) {
         param0.put(var4, new ResourceLocation(String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s.png", var4.getName())));
      }
   });

   public AxolotlRenderer(EntityRendererProvider.Context param0) {
      super(param0, new AxolotlModel<>(param0.bakeLayer(ModelLayers.AXOLOTL)), 0.5F);
   }

   public ResourceLocation getTextureLocation(Axolotl param0) {
      return TEXTURE_BY_TYPE.get(param0.getVariant());
   }
}
