package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FogRenderer {
   private static final int WATER_FOG_DISTANCE = 96;
   private static final List<FogRenderer.MobEffectFogFunction> MOB_EFFECT_FOG = Lists.newArrayList(
      new FogRenderer.BlindnessFogFunction(), new FogRenderer.DarknessFogFunction()
   );
   public static final float BIOME_FOG_TRANSITION_TIME = 5000.0F;
   private static float fogRed;
   private static float fogGreen;
   private static float fogBlue;
   private static int targetBiomeFog = -1;
   private static int previousBiomeFog = -1;
   private static long biomeChangedTime = -1L;

   public static void setupColor(Camera param0, float param1, ClientLevel param2, int param3, float param4) {
      FogType var5 = param0.getFluidInCamera();
      Entity var6 = param0.getEntity();
      if (var5 == FogType.WATER) {
         long var7 = Util.getMillis();
         int var9 = param2.getBiome(BlockPos.containing(param0.getPosition())).value().getWaterFogColor();
         if (biomeChangedTime < 0L) {
            targetBiomeFog = var9;
            previousBiomeFog = var9;
            biomeChangedTime = var7;
         }

         int var10 = targetBiomeFog >> 16 & 0xFF;
         int var11 = targetBiomeFog >> 8 & 0xFF;
         int var12 = targetBiomeFog & 0xFF;
         int var13 = previousBiomeFog >> 16 & 0xFF;
         int var14 = previousBiomeFog >> 8 & 0xFF;
         int var15 = previousBiomeFog & 0xFF;
         float var16 = Mth.clamp((float)(var7 - biomeChangedTime) / 5000.0F, 0.0F, 1.0F);
         float var17 = Mth.lerp(var16, var13, var10);
         float var18 = Mth.lerp(var16, var14, var11);
         float var19 = Mth.lerp(var16, var15, var12);
         fogRed = var17 / 255.0F;
         fogGreen = var18 / 255.0F;
         fogBlue = var19 / 255.0F;
         if (targetBiomeFog != var9) {
            targetBiomeFog = var9;
            previousBiomeFog = Mth.floor(var17) << 16 | Mth.floor(var18) << 8 | Mth.floor(var19);
            biomeChangedTime = var7;
         }
      } else if (var5 == FogType.LAVA) {
         fogRed = 0.6F;
         fogGreen = 0.1F;
         fogBlue = 0.0F;
         biomeChangedTime = -1L;
      } else if (var5 == FogType.POWDER_SNOW) {
         fogRed = 0.623F;
         fogGreen = 0.734F;
         fogBlue = 0.785F;
         biomeChangedTime = -1L;
         RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
      } else {
         float var7 = 0.25F + 0.75F * param3 / 32.0F;
         var7 = 1.0F - (float)Math.pow(var7, 0.25);
         Vec3 var8 = param2.getSkyColor(param0.getPosition(), param1);
         float var9 = (float)var8.x;
         float var10 = (float)var8.y;
         float var11 = (float)var8.z;
         float var12 = Mth.clamp(Mth.cos(param2.getTimeOfDay(param1) * (float) (Math.PI * 2)) * 2.0F + 0.5F, 0.0F, 1.0F);
         BiomeManager var13 = param2.getBiomeManager();
         Vec3 var14 = param0.getPosition().subtract(2.0, 2.0, 2.0).scale(0.25);
         Vec3 var15 = CubicSampler.gaussianSampleVec3(
            var14,
            (l_param3, l_param4, l_param5) -> param2.effects()
               .getBrightnessDependentFogColor(Vec3.fromRGB24(var13.getNoiseBiomeAtQuart(l_param3, l_param4, l_param5).value().getFogColor()), var12)
         );
         fogRed = (float)var15.x();
         fogGreen = (float)var15.y();
         fogBlue = (float)var15.z();
         if (param3 >= 4) {
            float var16 = Mth.sin(param2.getSunAngle(param1)) > 0.0F ? -1.0F : 1.0F;
            Vector3f var17 = new Vector3f(var16, 0.0F, 0.0F);
            float var18 = param0.getLookVector().dot(var17);
            if (var18 < 0.0F) {
               var18 = 0.0F;
            }

            if (var18 > 0.0F) {
               float[] var19 = param2.effects().getSunriseColor(param2.getTimeOfDay(param1), param1);
               if (var19 != null) {
                  var18 *= var19[3];
                  fogRed = fogRed * (1.0F - var18) + var19[0] * var18;
                  fogGreen = fogGreen * (1.0F - var18) + var19[1] * var18;
                  fogBlue = fogBlue * (1.0F - var18) + var19[2] * var18;
               }
            }
         }

         fogRed = fogRed + (var9 - fogRed) * var7;
         fogGreen = fogGreen + (var10 - fogGreen) * var7;
         fogBlue = fogBlue + (var11 - fogBlue) * var7;
         float var16 = param2.getRainLevel(param1);
         if (var16 > 0.0F) {
            float var17 = 1.0F - var16 * 0.5F;
            float var18 = 1.0F - var16 * 0.4F;
            fogRed *= var17;
            fogGreen *= var17;
            fogBlue *= var18;
         }

         float var17 = param2.getThunderLevel(param1);
         if (var17 > 0.0F) {
            float var18 = 1.0F - var17 * 0.5F;
            fogRed *= var18;
            fogGreen *= var18;
            fogBlue *= var18;
         }

         biomeChangedTime = -1L;
      }

      float var7 = ((float)param0.getPosition().y - param2.getMinBuildHeight()) * param2.getLevelData().getClearColorScale();
      FogRenderer.MobEffectFogFunction var8 = getPriorityFogFunction(var6, param1);
      if (var8 != null) {
         LivingEntity var9 = (LivingEntity)var6;
         var7 = var8.getModifiedVoidDarkness(var9, var9.getEffect(var8.getMobEffect()), var7, param1);
      }

      if (var7 < 1.0F && var5 != FogType.LAVA && var5 != FogType.POWDER_SNOW) {
         if (var7 < 0.0F) {
            var7 = 0.0F;
         }

         var7 *= var7;
         fogRed *= var7;
         fogGreen *= var7;
         fogBlue *= var7;
      }

      if (param4 > 0.0F) {
         fogRed = fogRed * (1.0F - param4) + fogRed * 0.7F * param4;
         fogGreen = fogGreen * (1.0F - param4) + fogGreen * 0.6F * param4;
         fogBlue = fogBlue * (1.0F - param4) + fogBlue * 0.6F * param4;
      }

      float var9;
      if (var5 == FogType.WATER) {
         if (var6 instanceof LocalPlayer) {
            var9 = ((LocalPlayer)var6).getWaterVision();
         } else {
            var9 = 1.0F;
         }
      } else if (var6 instanceof LivingEntity var10 && var10.hasEffect(MobEffects.NIGHT_VISION) && !var10.hasEffect(MobEffects.DARKNESS)) {
         var9 = GameRenderer.getNightVisionScale(var10, param1);
      } else {
         var9 = 0.0F;
      }

      if (fogRed != 0.0F && fogGreen != 0.0F && fogBlue != 0.0F) {
         float var10 = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
         fogRed = fogRed * (1.0F - var9) + fogRed * var10 * var9;
         fogGreen = fogGreen * (1.0F - var9) + fogGreen * var10 * var9;
         fogBlue = fogBlue * (1.0F - var9) + fogBlue * var10 * var9;
      }

      RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
   }

   public static void setupNoFog() {
      RenderSystem.setShaderFogStart(Float.MAX_VALUE);
   }

   @Nullable
   private static FogRenderer.MobEffectFogFunction getPriorityFogFunction(Entity param0, float param1) {
      return param0 instanceof LivingEntity var2 ? MOB_EFFECT_FOG.stream().filter(l_param2 -> l_param2.isEnabled(var2, param1)).findFirst().orElse(null) : null;
   }

   public static void setupFog(Camera param0, FogRenderer.FogMode param1, float param2, boolean param3, float param4) {
      FogType var5 = param0.getFluidInCamera();
      Entity var6 = param0.getEntity();
      FogRenderer.FogData var7 = new FogRenderer.FogData(param1);
      FogRenderer.MobEffectFogFunction var8 = getPriorityFogFunction(var6, param4);
      if (var5 == FogType.LAVA) {
         if (var6.isSpectator()) {
            var7.start = -8.0F;
            var7.end = param2 * 0.5F;
         } else if (var6 instanceof LivingEntity && ((LivingEntity)var6).hasEffect(MobEffects.FIRE_RESISTANCE)) {
            var7.start = 0.0F;
            var7.end = 3.0F;
         } else {
            var7.start = 0.25F;
            var7.end = 1.0F;
         }
      } else if (var5 == FogType.POWDER_SNOW) {
         if (var6.isSpectator()) {
            var7.start = -8.0F;
            var7.end = param2 * 0.5F;
         } else {
            var7.start = 0.0F;
            var7.end = 2.0F;
         }
      } else if (var8 != null) {
         LivingEntity var9 = (LivingEntity)var6;
         MobEffectInstance var10 = var9.getEffect(var8.getMobEffect());
         if (var10 != null) {
            var8.setupFog(var7, var9, var10, param2, param4);
         }
      } else if (var5 == FogType.WATER) {
         var7.start = -8.0F;
         var7.end = 96.0F;
         if (var6 instanceof LocalPlayer var9) {
            var7.end = var7.end * Math.max(0.25F, var9.getWaterVision());
            Holder<Biome> var10 = var9.level.getBiome(var9.blockPosition());
            if (var10.is(BiomeTags.HAS_CLOSER_WATER_FOG)) {
               var7.end *= 0.85F;
            }
         }

         if (var7.end > param2) {
            var7.end = param2;
            var7.shape = FogShape.CYLINDER;
         }
      } else if (param3) {
         var7.start = param2 * 0.05F;
         var7.end = Math.min(param2, 192.0F) * 0.5F;
      } else if (param1 == FogRenderer.FogMode.FOG_SKY) {
         var7.start = 0.0F;
         var7.end = param2;
         var7.shape = FogShape.CYLINDER;
      } else {
         float var9 = Mth.clamp(param2 / 10.0F, 4.0F, 64.0F);
         var7.start = param2 - var9;
         var7.end = param2;
         var7.shape = FogShape.CYLINDER;
      }

      RenderSystem.setShaderFogStart(var7.start);
      RenderSystem.setShaderFogEnd(var7.end);
      RenderSystem.setShaderFogShape(var7.shape);
   }

   public static void levelFogColor() {
      RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue);
   }

   static class BlindnessFogFunction implements FogRenderer.MobEffectFogFunction {
      @Override
      public MobEffect getMobEffect() {
         return MobEffects.BLINDNESS;
      }

      @Override
      public void setupFog(FogRenderer.FogData param0, LivingEntity param1, MobEffectInstance param2, float param3, float param4) {
         float var6 = param2.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, param2.getDuration() / 20.0F), param3, 5.0F);
         if (param0.mode == FogRenderer.FogMode.FOG_SKY) {
            param0.start = 0.0F;
            param0.end = var6 * 0.8F;
         } else {
            param0.start = var6 * 0.25F;
            param0.end = var6;
         }
      }
   }

   static class DarknessFogFunction implements FogRenderer.MobEffectFogFunction {
      @Override
      public MobEffect getMobEffect() {
         return MobEffects.DARKNESS;
      }

      @Override
      public void setupFog(FogRenderer.FogData param0, LivingEntity param1, MobEffectInstance param2, float param3, float param4) {
         if (!param2.getFactorData().isEmpty()) {
            float var6 = Mth.lerp(param2.getFactorData().get().getFactor(param1, param4), param3, 15.0F);
            param0.start = param0.mode == FogRenderer.FogMode.FOG_SKY ? 0.0F : var6 * 0.75F;
            param0.end = var6;
         }
      }

      @Override
      public float getModifiedVoidDarkness(LivingEntity param0, MobEffectInstance param1, float param2, float param3) {
         return param1.getFactorData().isEmpty() ? 0.0F : 1.0F - param1.getFactorData().get().getFactor(param0, param3);
      }
   }

   static class FogData {
      public final FogRenderer.FogMode mode;
      public float start;
      public float end;
      public FogShape shape = FogShape.SPHERE;

      public FogData(FogRenderer.FogMode param0) {
         this.mode = param0;
      }
   }

   public enum FogMode {
      FOG_SKY,
      FOG_TERRAIN;
   }

   interface MobEffectFogFunction {
      MobEffect getMobEffect();

      void setupFog(FogRenderer.FogData var1, LivingEntity var2, MobEffectInstance var3, float var4, float var5);

      default boolean isEnabled(LivingEntity param0, float param1) {
         return param0.hasEffect(this.getMobEffect());
      }

      default float getModifiedVoidDarkness(LivingEntity param0, MobEffectInstance param1, float param2, float param3) {
         MobEffectInstance var5 = param0.getEffect(this.getMobEffect());
         if (var5 != null) {
            if (var5.endsWithin(19)) {
               param2 = 1.0F - var5.getDuration() / 20.0F;
            } else {
               param2 = 0.0F;
            }
         }

         return param2;
      }
   }
}
