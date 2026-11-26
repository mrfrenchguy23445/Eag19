package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import org.joml.Matrix4f;

public class TheEndPortalRenderer<T extends TheEndPortalBlockEntity> implements BlockEntityRenderer<T> {
   public static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
   public static final ResourceLocation END_PORTAL_LOCATION = new ResourceLocation("textures/entity/end_portal.png");

   public TheEndPortalRenderer(BlockEntityRendererProvider.Context param0) {
   }

   public void render(T param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      Matrix4f var7 = param2.last().pose();
      this.renderCube(param0, var7, param3.getBuffer(this.renderType()));
   }

   private void renderCube(T param0, Matrix4f param1, VertexConsumer param2) {
      float var4 = this.getOffsetDown();
      float var5 = this.getOffsetUp();
      this.renderFace(param0, param1, param2, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
      this.renderFace(param0, param1, param2, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
      this.renderFace(param0, param1, param2, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
      this.renderFace(param0, param1, param2, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
      this.renderFace(param0, param1, param2, 0.0F, 1.0F, var4, var4, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
      this.renderFace(param0, param1, param2, 0.0F, 1.0F, var5, var5, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
   }

   private void renderFace(
      T param0,
      Matrix4f param1,
      VertexConsumer param2,
      float param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9,
      float param10,
      Direction param11
   ) {
      if (param0.shouldRenderFace(param11)) {
         param2.vertex(param1, param3, param5, param7).endVertex();
         param2.vertex(param1, param4, param5, param8).endVertex();
         param2.vertex(param1, param4, param6, param9).endVertex();
         param2.vertex(param1, param3, param6, param10).endVertex();
      }
   }

   protected float getOffsetUp() {
      return 0.75F;
   }

   protected float getOffsetDown() {
      return 0.375F;
   }

   protected RenderType renderType() {
      return RenderType.endPortal();
   }
}
