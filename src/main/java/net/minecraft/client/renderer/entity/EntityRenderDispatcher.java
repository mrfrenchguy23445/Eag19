package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class EntityRenderDispatcher implements ResourceManagerReloadListener {
   private static final RenderType SHADOW_RENDER_TYPE = RenderType.entityShadow(new ResourceLocation("textures/misc/shadow.png"));
   private Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
   private Map<String, EntityRenderer<? extends Player>> playerRenderers = ImmutableMap.of();
   public final TextureManager textureManager;
   private Level level;
   public Camera camera;
   private Quaternionf cameraOrientation;
   public Entity crosshairPickEntity;
   private final ItemRenderer itemRenderer;
   private final BlockRenderDispatcher blockRenderDispatcher;
   private final ItemInHandRenderer itemInHandRenderer;
   private final Font font;
   public final Options options;
   private final EntityModelSet entityModels;
   private boolean shouldRenderShadow = true;
   private boolean renderHitBoxes;

   public <E extends Entity> int getPackedLightCoords(E param0, float param1) {
      return this.getRenderer(param0).getPackedLightCoords(param0, param1);
   }

   public EntityRenderDispatcher(
      Minecraft param0, TextureManager param1, ItemRenderer param2, BlockRenderDispatcher param3, Font param4, Options param5, EntityModelSet param6
   ) {
      this.textureManager = param1;
      this.itemRenderer = param2;
      this.itemInHandRenderer = new ItemInHandRenderer(param0, this, param2);
      this.blockRenderDispatcher = param3;
      this.font = param4;
      this.options = param5;
      this.entityModels = param6;
   }

   public <T extends Entity> EntityRenderer<? super T> getRenderer(T param0) {
      if (param0 instanceof AbstractClientPlayer) {
         String var2 = ((AbstractClientPlayer)param0).getModelName();
         EntityRenderer<? extends Player> var3 = this.playerRenderers.get(var2);
         return (EntityRenderer<? super T>)(var3 != null ? var3 : this.playerRenderers.get("default"));
      } else {
         return (EntityRenderer<? super T>)this.renderers.get(param0.getType());
      }
   }

   public void prepare(Level param0, Camera param1, Entity param2) {
      this.level = param0;
      this.camera = param1;
      this.cameraOrientation = param1.rotation();
      this.crosshairPickEntity = param2;
   }

   public void overrideCameraOrientation(Quaternionf param0) {
      this.cameraOrientation = param0;
   }

   public void setRenderShadow(boolean param0) {
      this.shouldRenderShadow = param0;
   }

   public void setRenderHitBoxes(boolean param0) {
      this.renderHitBoxes = param0;
   }

   public boolean shouldRenderHitBoxes() {
      return this.renderHitBoxes;
   }

   public <E extends Entity> boolean shouldRender(E param0, Frustum param1, double param2, double param4, double param6) {
      EntityRenderer<? super E> var9 = this.getRenderer(param0);
      return var9.shouldRender(param0, param1, param2, param4, param6);
   }

   public <E extends Entity> void render(
      E param0, double param1, double param3, double param5, float param7, float param8, PoseStack param9, MultiBufferSource param10, int param11
   ) {
      EntityRenderer<? super E> var13 = this.getRenderer(param0);

      try {
         Vec3 var14 = var13.getRenderOffset(param0, param8);
         double var15 = param1 + var14.x();
         double var17 = param3 + var14.y();
         double var19 = param5 + var14.z();
         param9.pushPose();
         param9.translate(var15, var17, var19);
         var13.render(param0, param7, param8, param9, param10, param11);
         if (param0.displayFireAnimation()) {
            this.renderFlame(param9, param10, param0);
         }

         param9.translate(-var14.x(), -var14.y(), -var14.z());
         if (this.options.entityShadows().get() && this.shouldRenderShadow && var13.shadowRadius > 0.0F && !param0.isInvisible()) {
            double var21 = this.distanceToSqr(param0.getX(), param0.getY(), param0.getZ());
            float var23 = (float)((1.0 - var21 / 256.0) * var13.shadowStrength);
            if (var23 > 0.0F) {
               renderShadow(param9, param10, param0, var23, param8, this.level, var13.shadowRadius);
            }
         }

         if (this.renderHitBoxes && !param0.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo()) {
            renderHitbox(param9, param10.getBuffer(RenderType.lines()), param0, param8);
         }

         param9.popPose();
      } catch (Throwable var14) {
         CrashReport var15 = CrashReport.forThrowable(var14, "Rendering entity in world");
         CrashReportCategory var16 = var15.addCategory("Entity being rendered");
         param0.fillCrashReportCategory(var16);
         CrashReportCategory var17 = var15.addCategory("Renderer details");
         var17.setDetail("Assigned renderer", var13);
         var17.setDetail("Location", CrashReportCategory.formatLocation(this.level, param1, param3, param5));
         var17.setDetail("Rotation", param7);
         var17.setDetail("Delta", param8);
         throw new ReportedException(var15);
      }
   }

   private static void renderHitbox(PoseStack param0, VertexConsumer param1, Entity param2, float param3) {
      AABB var4 = param2.getBoundingBox().move(-param2.getX(), -param2.getY(), -param2.getZ());
      LevelRenderer.renderLineBox(param0, param1, var4, 1.0F, 1.0F, 1.0F, 1.0F);
      if (param2 instanceof EnderDragon) {
         double var5 = -Mth.lerp(param3, param2.xOld, param2.getX());
         double var7 = -Mth.lerp(param3, param2.yOld, param2.getY());
         double var9 = -Mth.lerp(param3, param2.zOld, param2.getZ());

         for (EnderDragonPart var14 : ((EnderDragon)param2).getSubEntities()) {
            param0.pushPose();
            double var15 = var5 + Mth.lerp(param3, var14.xOld, var14.getX());
            double var17 = var7 + Mth.lerp(param3, var14.yOld, var14.getY());
            double var19 = var9 + Mth.lerp(param3, var14.zOld, var14.getZ());
            param0.translate(var15, var17, var19);
            LevelRenderer.renderLineBox(param0, param1, var14.getBoundingBox().move(-var14.getX(), -var14.getY(), -var14.getZ()), 0.25F, 1.0F, 0.0F, 1.0F);
            param0.popPose();
         }
      }

      if (param2 instanceof LivingEntity) {
         float var5 = 0.01F;
         LevelRenderer.renderLineBox(
            param0, param1, var4.minX, param2.getEyeHeight() - 0.01F, var4.minZ, var4.maxX, param2.getEyeHeight() + 0.01F, var4.maxZ, 1.0F, 0.0F, 0.0F, 1.0F
         );
      }

      Vec3 var5 = param2.getViewVector(param3);
      Matrix4f var6 = param0.last().pose();
      Matrix3f var7 = param0.last().normal();
      param1.vertex(var6, 0.0F, param2.getEyeHeight(), 0.0F).color(0, 0, 255, 255).normal(var7, (float)var5.x, (float)var5.y, (float)var5.z).endVertex();
      param1.vertex(var6, (float)(var5.x * 2.0), (float)(param2.getEyeHeight() + var5.y * 2.0), (float)(var5.z * 2.0))
         .color(0, 0, 255, 255)
         .normal(var7, (float)var5.x, (float)var5.y, (float)var5.z)
         .endVertex();
   }

   private void renderFlame(PoseStack param0, MultiBufferSource param1, Entity param2) {
      TextureAtlasSprite var4 = ModelBakery.FIRE_0.sprite();
      TextureAtlasSprite var5 = ModelBakery.FIRE_1.sprite();
      param0.pushPose();
      float var6 = param2.getBbWidth() * 1.4F;
      param0.scale(var6, var6, var6);
      float var7 = 0.5F;
      float var8 = 0.0F;
      float var9 = param2.getBbHeight() / var6;
      float var10 = 0.0F;
      param0.mulPose(Axis.YP.rotationDegrees(-this.camera.getYRot()));
      param0.translate(0.0F, 0.0F, -0.3F + (int)var9 * 0.02F);
      float var11 = 0.0F;
      int var12 = 0;
      VertexConsumer var13 = param1.getBuffer(Sheets.cutoutBlockSheet());
      PoseStack.Pose var14 = param0.last();

      while (var9 > 0.0F) {
         TextureAtlasSprite var15 = var12 % 2 == 0 ? var4 : var5;
         float var16 = var15.getU0();
         float var17 = var15.getV0();
         float var18 = var15.getU1();
         float var19 = var15.getV1();
         if (var12 / 2 % 2 == 0) {
            float var20 = var18;
            var18 = var16;
            var16 = var20;
         }

         fireVertex(var14, var13, var7 - 0.0F, 0.0F - var10, var11, var18, var19);
         fireVertex(var14, var13, -var7 - 0.0F, 0.0F - var10, var11, var16, var19);
         fireVertex(var14, var13, -var7 - 0.0F, 1.4F - var10, var11, var16, var17);
         fireVertex(var14, var13, var7 - 0.0F, 1.4F - var10, var11, var18, var17);
         var9 -= 0.45F;
         var10 -= 0.45F;
         var7 *= 0.9F;
         var11 += 0.03F;
         var12++;
      }

      param0.popPose();
   }

   private static void fireVertex(PoseStack.Pose param0, VertexConsumer param1, float param2, float param3, float param4, float param5, float param6) {
      param1.vertex(param0.pose(), param2, param3, param4)
         .color(255, 255, 255, 255)
         .uv(param5, param6)
         .overlayCoords(0, 10)
         .uv2(240)
         .normal(param0.normal(), 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   private static void renderShadow(PoseStack param0, MultiBufferSource param1, Entity param2, float param3, float param4, LevelReader param5, float param6) {
      float var7 = param6;
      if (param2 instanceof Mob var8 && var8.isBaby()) {
         var7 *= 0.5F;
      }

      double var8 = Mth.lerp(param4, param2.xOld, param2.getX());
      double var10 = Mth.lerp(param4, param2.yOld, param2.getY());
      double var12 = Mth.lerp(param4, param2.zOld, param2.getZ());
      int var14 = Mth.floor(var8 - var7);
      int var15 = Mth.floor(var8 + var7);
      int var16 = Mth.floor(var10 - var7);
      int var17 = Mth.floor(var10);
      int var18 = Mth.floor(var12 - var7);
      int var19 = Mth.floor(var12 + var7);
      PoseStack.Pose var20 = param0.last();
      VertexConsumer var21 = param1.getBuffer(SHADOW_RENDER_TYPE);

      for (BlockPos var23 : BlockPos.betweenClosed(new BlockPos(var14, var16, var18), new BlockPos(var15, var17, var19))) {
         renderBlockShadow(var20, var21, param5, var23, var8, var10, var12, var7, param3);
      }
   }

   private static void renderBlockShadow(
      PoseStack.Pose param0,
      VertexConsumer param1,
      LevelReader param2,
      BlockPos param3,
      double param4,
      double param6,
      double param8,
      float param10,
      float param11
   ) {
      BlockPos var12 = param3.below();
      BlockState var13 = param2.getBlockState(var12);
      if (var13.getRenderShape() != RenderShape.INVISIBLE && param2.getMaxLocalRawBrightness(param3) > 3) {
         if (var13.isCollisionShapeFullBlock(param2, var12)) {
            VoxelShape var14 = var13.getShape(param2, param3.below());
            if (!var14.isEmpty()) {
               float var15 = LightTexture.getBrightness(param2.dimensionType(), param2.getMaxLocalRawBrightness(param3));
               float var16 = (float)((param11 - (param6 - param3.getY()) / 2.0) * 0.5 * var15);
               if (var16 >= 0.0F) {
                  if (var16 > 1.0F) {
                     var16 = 1.0F;
                  }

                  AABB var17 = var14.bounds();
                  double var18 = param3.getX() + var17.minX;
                  double var20 = param3.getX() + var17.maxX;
                  double var22 = param3.getY() + var17.minY;
                  double var24 = param3.getZ() + var17.minZ;
                  double var26 = param3.getZ() + var17.maxZ;
                  float var28 = (float)(var18 - param4);
                  float var29 = (float)(var20 - param4);
                  float var30 = (float)(var22 - param6);
                  float var31 = (float)(var24 - param8);
                  float var32 = (float)(var26 - param8);
                  float var33 = -var28 / 2.0F / param10 + 0.5F;
                  float var34 = -var29 / 2.0F / param10 + 0.5F;
                  float var35 = -var31 / 2.0F / param10 + 0.5F;
                  float var36 = -var32 / 2.0F / param10 + 0.5F;
                  shadowVertex(param0, param1, var16, var28, var30, var31, var33, var35);
                  shadowVertex(param0, param1, var16, var28, var30, var32, var33, var36);
                  shadowVertex(param0, param1, var16, var29, var30, var32, var34, var36);
                  shadowVertex(param0, param1, var16, var29, var30, var31, var34, var35);
               }
            }
         }
      }
   }

   private static void shadowVertex(
      PoseStack.Pose param0, VertexConsumer param1, float param2, float param3, float param4, float param5, float param6, float param7
   ) {
      param1.vertex(param0.pose(), param3, param4, param5)
         .color(1.0F, 1.0F, 1.0F, param2)
         .uv(param6, param7)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(param0.normal(), 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public void setLevel(@Nullable Level param0) {
      this.level = param0;
      if (param0 == null) {
         this.camera = null;
      }
   }

   public double distanceToSqr(Entity param0) {
      return this.camera.getPosition().distanceToSqr(param0.position());
   }

   public double distanceToSqr(double param0, double param2, double param4) {
      return this.camera.getPosition().distanceToSqr(param0, param2, param4);
   }

   public Quaternionf cameraOrientation() {
      return this.cameraOrientation;
   }

   public ItemInHandRenderer getItemInHandRenderer() {
      return this.itemInHandRenderer;
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      EntityRendererProvider.Context var2 = new EntityRendererProvider.Context(
         this, this.itemRenderer, this.blockRenderDispatcher, this.itemInHandRenderer, param0, this.entityModels, this.font
      );
      this.renderers = EntityRenderers.createEntityRenderers(var2);
      this.playerRenderers = EntityRenderers.createPlayerRenderers(var2);
   }
}
