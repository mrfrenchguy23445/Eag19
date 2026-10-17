package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

public class IronGolemCrackinessLayer extends RenderLayer<IronGolem, IronGolemModel<IronGolem>> {
   private static final Map<IronGolem.Crackiness, ResourceLocation> resourceLocations = ImmutableMap.of(
      IronGolem.Crackiness.LOW,
      new ResourceLocation("textures/entity/iron_golem/iron_golem_crackiness_low.png"),
      IronGolem.Crackiness.MEDIUM,
      new ResourceLocation("textures/entity/iron_golem/iron_golem_crackiness_medium.png"),
      IronGolem.Crackiness.HIGH,
      new ResourceLocation("textures/entity/iron_golem/iron_golem_crackiness_high.png")
   );

   public IronGolemCrackinessLayer(RenderLayerParent<IronGolem, IronGolemModel<IronGolem>> param0) {
      super(param0);
   }

   public void render(
      PoseStack param0,
      MultiBufferSource param1,
      int param2,
      IronGolem param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9
   ) {
      if (!param3.isInvisible()) {
         IronGolem.Crackiness var11 = param3.getCrackiness();
         if (var11 != IronGolem.Crackiness.NONE) {
            ResourceLocation var12 = resourceLocations.get(var11);
            renderColoredCutoutModel(this.getParentModel(), var12, param0, param1, param2, param3, 1.0F, 1.0F, 1.0F);
         }
      }
   }
}
