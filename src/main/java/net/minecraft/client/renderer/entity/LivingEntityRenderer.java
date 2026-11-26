package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float EYE_BED_OFFSET = 0.1F;
   protected M model;
   protected final List<RenderLayer<T, M>> layers = Lists.newArrayList();

   public LivingEntityRenderer(EntityRendererProvider.Context param0, M param1, float param2) {
      super(param0);
      this.model = param1;
      this.shadowRadius = param2;
   }

   protected final boolean addLayer(RenderLayer<T, M> param0) {
      return this.layers.add(param0);
   }

   @Override
   public M getModel() {
      return this.model;
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      this.model.attackTime = this.getAttackAnim(param0, param2);
      this.model.riding = param0.isPassenger();
      this.model.young = param0.isBaby();
      float var7 = Mth.rotLerp(param2, param0.yBodyRotO, param0.yBodyRot);
      float var8 = Mth.rotLerp(param2, param0.yHeadRotO, param0.yHeadRot);
      float var9 = var8 - var7;
      if (param0.isPassenger() && param0.getVehicle() instanceof LivingEntity) {
         LivingEntity var10 = (LivingEntity)param0.getVehicle();
         var7 = Mth.rotLerp(param2, var10.yBodyRotO, var10.yBodyRot);
         var9 = var8 - var7;
         float var11 = Mth.wrapDegrees(var9);
         if (var11 < -85.0F) {
            var11 = -85.0F;
         }

         if (var11 >= 85.0F) {
            var11 = 85.0F;
         }

         var7 = var8 - var11;
         if (var11 * var11 > 2500.0F) {
            var7 += var11 * 0.2F;
         }

         var9 = var8 - var7;
      }

      float var10 = Mth.lerp(param2, param0.xRotO, param0.getXRot());
      if (isEntityUpsideDown(param0)) {
         var10 *= -1.0F;
         var9 *= -1.0F;
      }

      if (param0.hasPose(Pose.SLEEPING)) {
         Direction var11 = param0.getBedOrientation();
         if (var11 != null) {
            float var12 = param0.getEyeHeight(Pose.STANDING) - 0.1F;
            param3.translate(-var11.getStepX() * var12, 0.0F, -var11.getStepZ() * var12);
         }
      }

      float var11 = this.getBob(param0, param2);
      this.setupRotations(param0, param3, var11, var7, param2);
      param3.scale(-1.0F, -1.0F, 1.0F);
      this.scale(param0, param3, param2);
      param3.translate(0.0F, -1.501F, 0.0F);
      float var12 = 0.0F;
      float var13 = 0.0F;
      if (!param0.isPassenger() && param0.isAlive()) {
         var12 = param0.walkAnimation.speed(param2);
         var13 = param0.walkAnimation.position(param2);
         if (param0.isBaby()) {
            var13 *= 3.0F;
         }

         if (var12 > 1.0F) {
            var12 = 1.0F;
         }
      }

      this.model.prepareMobModel(param0, var13, var12, param2);
      this.model.setupAnim(param0, var13, var12, var11, var9, var10);
      Minecraft var14 = Minecraft.getInstance();
      boolean var15 = this.isBodyVisible(param0);
      boolean var16 = !var15 && !param0.isInvisibleTo(var14.player);
      boolean var17 = var14.shouldEntityAppearGlowing(param0);
      RenderType var18 = this.getRenderType(param0, var15, var16, var17);
      if (var18 != null) {
         VertexConsumer var19 = param4.getBuffer(var18);
         int var20 = getOverlayCoords(param0, this.getWhiteOverlayProgress(param0, param2));
         this.model.renderToBuffer(param3, var19, param5, var20, 1.0F, 1.0F, 1.0F, var16 ? 0.15F : 1.0F);
      }

      if (!param0.isSpectator()) {
         for (RenderLayer<T, M> var20 : this.layers) {
            var20.render(param3, param4, param5, param0, var13, var12, param2, var11, var9, var10);
         }
      }

      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   @Nullable
   protected RenderType getRenderType(T param0, boolean param1, boolean param2, boolean param3) {
      ResourceLocation var5 = this.getTextureLocation(param0);
      if (param2) {
         return RenderType.itemEntityTranslucentCull(var5);
      } else if (param1) {
         return this.model.renderType(var5);
      } else {
         return param3 ? RenderType.outline(var5) : null;
      }
   }

   public static int getOverlayCoords(LivingEntity param0, float param1) {
      return OverlayTexture.pack(OverlayTexture.u(param1), OverlayTexture.v(param0.hurtTime > 0 || param0.deathTime > 0));
   }

   protected boolean isBodyVisible(T param0) {
      return !param0.isInvisible();
   }

   private static float sleepDirectionToRotation(Direction param0) {
      switch (param0) {
         case SOUTH:
            return 90.0F;
         case WEST:
            return 0.0F;
         case NORTH:
            return 270.0F;
         case EAST:
            return 180.0F;
         default:
            return 0.0F;
      }
   }

   protected boolean isShaking(T param0) {
      return param0.isFullyFrozen();
   }

   protected void setupRotations(T param0, PoseStack param1, float param2, float param3, float param4) {
      if (this.isShaking(param0)) {
         param3 += (float)(Math.cos(param0.tickCount * 3.25) * Math.PI * 0.4F);
      }

      if (!param0.hasPose(Pose.SLEEPING)) {
         param1.mulPose(Axis.YP.rotationDegrees(180.0F - param3));
      }

      if (param0.deathTime > 0) {
         float var6 = (param0.deathTime + param4 - 1.0F) / 20.0F * 1.6F;
         var6 = Mth.sqrt(var6);
         if (var6 > 1.0F) {
            var6 = 1.0F;
         }

         param1.mulPose(Axis.ZP.rotationDegrees(var6 * this.getFlipDegrees(param0)));
      } else if (param0.isAutoSpinAttack()) {
         param1.mulPose(Axis.XP.rotationDegrees(-90.0F - param0.getXRot()));
         param1.mulPose(Axis.YP.rotationDegrees((param0.tickCount + param4) * -75.0F));
      } else if (param0.hasPose(Pose.SLEEPING)) {
         Direction var6 = param0.getBedOrientation();
         float var7 = var6 != null ? sleepDirectionToRotation(var6) : param3;
         param1.mulPose(Axis.YP.rotationDegrees(var7));
         param1.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(param0)));
         param1.mulPose(Axis.YP.rotationDegrees(270.0F));
      } else if (isEntityUpsideDown(param0)) {
         param1.translate(0.0F, param0.getBbHeight() + 0.1F, 0.0F);
         param1.mulPose(Axis.ZP.rotationDegrees(180.0F));
      }
   }

   protected float getAttackAnim(T param0, float param1) {
      return param0.getAttackAnim(param1);
   }

   protected float getBob(T param0, float param1) {
      return param0.tickCount + param1;
   }

   protected float getFlipDegrees(T param0) {
      return 90.0F;
   }

   protected float getWhiteOverlayProgress(T param0, float param1) {
      return 0.0F;
   }

   protected void scale(T param0, PoseStack param1, float param2) {
   }

   protected boolean shouldShowName(T param0) {
      double var2 = this.entityRenderDispatcher.distanceToSqr(param0);
      float var4 = param0.isDiscrete() ? 32.0F : 64.0F;
      if (var2 >= var4 * var4) {
         return false;
      }

      Minecraft var5 = Minecraft.getInstance();
      LocalPlayer var6 = var5.player;
      boolean var7 = !param0.isInvisibleTo(var6);
      if (param0 != var6) {
         Team var8 = param0.getTeam();
         Team var9 = var6.getTeam();
         if (var8 != null) {
            Team.Visibility var10 = var8.getNameTagVisibility();
            switch (var10) {
               case ALWAYS:
                  return var7;
               case NEVER:
                  return false;
               case HIDE_FOR_OTHER_TEAMS:
                  return var9 == null ? var7 : var8.isAlliedTo(var9) && (var8.canSeeFriendlyInvisibles() || var7);
               case HIDE_FOR_OWN_TEAM:
                  return var9 == null ? var7 : !var8.isAlliedTo(var9) && var7;
               default:
                  return true;
            }
         }
      }

      return Minecraft.renderNames() && param0 != var5.getCameraEntity() && var7 && !param0.isVehicle();
   }

   public static boolean isEntityUpsideDown(LivingEntity param0) {
      if (param0 instanceof Player || param0.hasCustomName()) {
         String var1 = ChatFormatting.stripFormatting(param0.getName().getString());
         if ("Dinnerbone".equals(var1) || "Grumm".equals(var1)) {
            return !(param0 instanceof Player) || ((Player)param0).isModelPartShown(PlayerModelPart.CAPE);
         }
      }

      return false;
   }
}
