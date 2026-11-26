package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

public class ItemPickupParticle extends Particle {
   private static final int LIFE_TIME = 3;
   private final RenderBuffers renderBuffers;
   private final Entity itemEntity;
   private final Entity target;
   private int life;
   private final EntityRenderDispatcher entityRenderDispatcher;

   public ItemPickupParticle(EntityRenderDispatcher param0, RenderBuffers param1, ClientLevel param2, Entity param3, Entity param4) {
      this(param0, param1, param2, param3, param4, param3.getDeltaMovement());
   }

   private ItemPickupParticle(EntityRenderDispatcher param0, RenderBuffers param1, ClientLevel param2, Entity param3, Entity param4, Vec3 param5) {
      super(param2, param3.getX(), param3.getY(), param3.getZ(), param5.x, param5.y, param5.z);
      this.renderBuffers = param1;
      this.itemEntity = this.getSafeCopy(param3);
      this.target = param4;
      this.entityRenderDispatcher = param0;
   }

   private Entity getSafeCopy(Entity param0) {
      return !(param0 instanceof ItemEntity) ? param0 : ((ItemEntity)param0).copy();
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.CUSTOM;
   }

   @Override
   public void render(VertexConsumer param0, Camera param1, float param2) {
      float var4 = (this.life + param2) / 3.0F;
      var4 *= var4;
      double var5 = Mth.lerp(param2, this.target.xOld, this.target.getX());
      double var7 = Mth.lerp(param2, this.target.yOld, (this.target.getY() + this.target.getEyeY()) / 2.0);
      double var9 = Mth.lerp(param2, this.target.zOld, this.target.getZ());
      double var11 = Mth.lerp(var4, this.itemEntity.getX(), var5);
      double var13 = Mth.lerp(var4, this.itemEntity.getY(), var7);
      double var15 = Mth.lerp(var4, this.itemEntity.getZ(), var9);
      MultiBufferSource.BufferSource var17 = this.renderBuffers.bufferSource();
      Vec3 var18 = param1.getPosition();
      this.entityRenderDispatcher
         .render(
            this.itemEntity,
            var11 - var18.x(),
            var13 - var18.y(),
            var15 - var18.z(),
            this.itemEntity.getYRot(),
            param2,
            new PoseStack(),
            var17,
            this.entityRenderDispatcher.getPackedLightCoords(this.itemEntity, param2)
         );
      var17.endBatch();
   }

   @Override
   public void tick() {
      this.life++;
      if (this.life == 3) {
         this.remove();
      }
   }
}
