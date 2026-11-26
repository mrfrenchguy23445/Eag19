package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;

public class ArrowLayer<T extends LivingEntity, M extends PlayerModel<T>> extends StuckInBodyLayer<T, M> {
   private final EntityRenderDispatcher dispatcher;

   public ArrowLayer(EntityRendererProvider.Context param0, LivingEntityRenderer<T, M> param1) {
      super(param1);
      this.dispatcher = param0.getEntityRenderDispatcher();
   }

   @Override
   protected int numStuck(T param0) {
      return param0.getArrowCount();
   }

   @Override
   protected void renderStuckItem(PoseStack param0, MultiBufferSource param1, int param2, Entity param3, float param4, float param5, float param6, float param7) {
      float var9 = Mth.sqrt(param4 * param4 + param6 * param6);
      Arrow var10 = new Arrow(param3.level, param3.getX(), param3.getY(), param3.getZ());
      var10.setYRot((float)(Math.atan2(param4, param6) * 180.0F / (float)Math.PI));
      var10.setXRot((float)(Math.atan2(param5, var9) * 180.0F / (float)Math.PI));
      var10.yRotO = var10.getYRot();
      var10.xRotO = var10.getXRot();
      this.dispatcher.render(var10, 0.0, 0.0, 0.0, 0.0F, param7, param0, param1, param2);
   }
}
