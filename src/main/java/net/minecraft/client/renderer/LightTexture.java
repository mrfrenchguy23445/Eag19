package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;

public class LightTexture implements AutoCloseable {
   public static final int FULL_BRIGHT = 15728880;
   public static final int FULL_SKY = 15728640;
   public static final int FULL_BLOCK = 240;
   private final DynamicTexture lightTexture;
   private final NativeImage lightPixels;
   private final ResourceLocation lightTextureLocation;
   private boolean updateLightTexture;
   private float blockLightRedFlicker;
   private final GameRenderer renderer;
   private final Minecraft minecraft;

   public LightTexture(GameRenderer param0, Minecraft param1) {
      this.renderer = param0;
      this.minecraft = param1;
      this.lightTexture = new DynamicTexture(16, 16, false);
      this.lightTextureLocation = this.minecraft.getTextureManager().register("light_map", this.lightTexture);
      this.lightPixels = this.lightTexture.getPixels();

      for (int var3 = 0; var3 < 16; var3++) {
         for (int var4 = 0; var4 < 16; var4++) {
            this.lightPixels.setPixelRGBA(var4, var3, -1);
         }
      }

      this.lightTexture.upload();
   }

   @Override
   public void close() {
      this.lightTexture.close();
   }

   public void tick() {
      this.blockLightRedFlicker = this.blockLightRedFlicker + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
      this.blockLightRedFlicker *= 0.9F;
      this.updateLightTexture = true;
   }

   public void turnOffLightLayer() {
      RenderSystem.setShaderTexture(2, 0);
   }

   public void turnOnLightLayer() {
      RenderSystem.setShaderTexture(2, this.lightTextureLocation);
      this.minecraft.getTextureManager().bindForSetup(this.lightTextureLocation);
      RenderSystem.texParameter(3553, 10241, 9729);
      RenderSystem.texParameter(3553, 10240, 9729);
   }

   private float getDarknessGamma(float param0) {
      if (this.minecraft.player.hasEffect(MobEffects.DARKNESS)) {
         MobEffectInstance var2 = this.minecraft.player.getEffect(MobEffects.DARKNESS);
         if (var2 != null && var2.getFactorData().isPresent()) {
            return var2.getFactorData().get().getFactor(this.minecraft.player, param0);
         }
      }

      return 0.0F;
   }

   private float calculateDarknessScale(LivingEntity param0, float param1, float param2) {
      float var4 = 0.45F * param1;
      return Math.max(0.0F, Mth.cos((param0.tickCount - param2) * (float) Math.PI * 0.025F) * var4);
   }

   public void updateLightTexture(float param0) {
      if (this.updateLightTexture) {
         this.updateLightTexture = false;
         this.minecraft.getProfiler().push("lightTex");
         ClientLevel var2 = this.minecraft.level;
         if (var2 != null) {
            float var3 = var2.getSkyDarken(1.0F);
            float var4;
            if (var2.getSkyFlashTime() > 0) {
               var4 = 1.0F;
            } else {
               var4 = var3 * 0.95F + 0.05F;
            }

            float var5 = this.minecraft.options.darknessEffectScale().get().floatValue();
            float var6 = this.getDarknessGamma(param0) * var5;
            float var7 = this.calculateDarknessScale(this.minecraft.player, var6, param0) * var5;
            float var9 = this.minecraft.player.getWaterVision();
            float var8;
            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
               var8 = GameRenderer.getNightVisionScale(this.minecraft.player, param0);
            } else if (var9 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
               var8 = var9;
            } else {
               var8 = 0.0F;
            }

            Vector3f var10 = new Vector3f(var3, var3, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
            float var11 = this.blockLightRedFlicker + 1.5F;
            Vector3f var12 = new Vector3f();
            // Everything below is loop-invariant: hoist it out of the 256-pixel loop, and precompute
            // the per-level brightness tables instead of resolving dimensionType/effects/options per pixel.
            DimensionType var13 = var2.dimensionType();
            boolean var14 = var2.effects().forceBrightLightmap();
            float var15 = this.renderer.getDarkenWorldAmount(param0);
            float var16 = Math.max(0.0F, this.minecraft.options.gamma().get().floatValue() - var6);
            float[] var17 = new float[16];
            float[] var18 = new float[16];

            for (int var19 = 0; var19 < 16; var19++) {
               float var20 = getBrightness(var13, var19);
               var17[var19] = var20 * var4;
               var18[var19] = var20 * var11;
            }

            Vector3f var21 = new Vector3f();
            Vector3f var22 = new Vector3f();
            Vector3f var23 = new Vector3f(0.75F, 0.75F, 0.75F);
            Vector3f var24 = new Vector3f(0.99F, 1.12F, 1.0F);

            for (int var25 = 0; var25 < 16; var25++) {
               for (int var26 = 0; var26 < 16; var26++) {
                  float var27 = var18[var26];
                  float var28 = var27 * ((var27 * 0.6F + 0.4F) * 0.6F + 0.4F);
                  float var29 = var27 * (var27 * var27 * 0.6F + 0.4F);
                  var12.set(var27, var28, var29);
                  if (var14) {
                     var12.lerp(var24, 0.25F);
                     clampColor(var12);
                  } else {
                     var21.set(var10).mul(var17[var25]);
                     var12.add(var21);
                     var12.lerp(var23, 0.04F);
                     if (var15 > 0.0F) {
                        var21.set(var12).mul(0.7F, 0.6F, 0.6F);
                        var12.lerp(var21, var15);
                     }
                  }

                  if (var8 > 0.0F) {
                     float var30 = Math.max(var12.x(), Math.max(var12.y(), var12.z()));
                     if (var30 < 1.0F) {
                        var21.set(var12).mul(1.0F / var30);
                        var12.lerp(var21, var8);
                     }
                  }

                  if (!var14) {
                     if (var7 > 0.0F) {
                        var12.add(-var7, -var7, -var7);
                     }

                     clampColor(var12);
                  }

                  var21.set(this.notGamma(var12.x), this.notGamma(var12.y), this.notGamma(var12.z));
                  var12.lerp(var21, var16);
                  var12.lerp(var23, 0.04F);
                  clampColor(var12);
                  var12.mul(255.0F);
                  int var30 = (int)var12.x();
                  int var31 = (int)var12.y();
                  int var32 = (int)var12.z();
                  this.lightPixels.setPixelRGBA(var26, var25, 0xFF000000 | var32 << 16 | var31 << 8 | var30);
               }
            }

            this.lightTexture.upload();
            this.minecraft.getProfiler().pop();
         }
      }
   }

   private static void clampColor(Vector3f param0) {
      param0.set(Mth.clamp(param0.x, 0.0F, 1.0F), Mth.clamp(param0.y, 0.0F, 1.0F), Mth.clamp(param0.z, 0.0F, 1.0F));
   }

   private float notGamma(float param0) {
      float var2 = 1.0F - param0;
      return 1.0F - var2 * var2 * var2 * var2;
   }

   public static float getBrightness(DimensionType param0, int param1) {
      float var2 = param1 / 15.0F;
      float var3 = var2 / (4.0F - 3.0F * var2);
      return Mth.lerp(param0.ambientLight(), var3, 1.0F);
   }

   public static int pack(int param0, int param1) {
      return param0 << 4 | param1 << 20;
   }

   public static int block(int param0) {
      return param0 >> 4 & 65535;
   }

   public static int sky(int param0) {
      return param0 >> 20 & 65535;
   }
}
