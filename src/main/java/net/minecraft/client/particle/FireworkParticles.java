package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;

public class FireworkParticles {
   public static class FlashProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public FlashProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         FireworkParticles.OverlayParticle var15 = new FireworkParticles.OverlayParticle(param1, param2, param4, param6);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }

   public static class OverlayParticle extends TextureSheetParticle {
      OverlayParticle(ClientLevel param0, double param1, double param3, double param5) {
         super(param0, param1, param3, param5);
         this.lifetime = 4;
      }

      @Override
      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }

      @Override
      public void render(VertexConsumer param0, Camera param1, float param2) {
         this.setAlpha(0.6F - (this.age + param2 - 1.0F) * 0.25F * 0.5F);
         super.render(param0, param1, param2);
      }

      @Override
      public float getQuadSize(float param0) {
         return 7.1F * Mth.sin((this.age + param0 - 1.0F) * 0.25F * (float) Math.PI);
      }
   }

   static class SparkParticle extends SimpleAnimatedParticle {
      private boolean trail;
      private boolean flicker;
      private final ParticleEngine engine;
      private float fadeR;
      private float fadeG;
      private float fadeB;
      private boolean hasFade;

      SparkParticle(
         ClientLevel param0,
         double param1,
         double param3,
         double param5,
         double param7,
         double param9,
         double param11,
         ParticleEngine param13,
         SpriteSet param14
      ) {
         super(param0, param1, param3, param5, param14, 0.1F);
         this.xd = param7;
         this.yd = param9;
         this.zd = param11;
         this.engine = param13;
         this.quadSize *= 0.75F;
         this.lifetime = 48 + this.random.nextInt(12);
         this.setSpriteFromAge(param14);
      }

      public void setTrail(boolean param0) {
         this.trail = param0;
      }

      public void setFlicker(boolean param0) {
         this.flicker = param0;
      }

      @Override
      public void render(VertexConsumer param0, Camera param1, float param2) {
         if (!this.flicker || this.age < this.lifetime / 3 || (this.age + this.lifetime) / 3 % 2 == 0) {
            super.render(param0, param1, param2);
         }
      }

      @Override
      public void tick() {
         super.tick();
         if (this.trail && this.age < this.lifetime / 2 && (this.age + this.lifetime) % 2 == 0) {
            FireworkParticles.SparkParticle var1 = new FireworkParticles.SparkParticle(
               this.level, this.x, this.y, this.z, 0.0, 0.0, 0.0, this.engine, this.sprites
            );
            var1.setAlpha(0.99F);
            var1.setColor(this.rCol, this.gCol, this.bCol);
            var1.age = var1.lifetime / 2;
            if (this.hasFade) {
               var1.hasFade = true;
               var1.fadeR = this.fadeR;
               var1.fadeG = this.fadeG;
               var1.fadeB = this.fadeB;
            }

            var1.flicker = this.flicker;
            this.engine.add(var1);
         }
      }
   }

   public static class SparkProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SparkProvider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         FireworkParticles.SparkParticle var15 = new FireworkParticles.SparkParticle(
            param1, param2, param4, param6, param8, param10, param12, Minecraft.getInstance().particleEngine, this.sprites
         );
         var15.setAlpha(0.99F);
         return var15;
      }
   }

   public static class Starter extends NoRenderParticle {
      private int life;
      private final ParticleEngine engine;
      private ListTag explosions;
      private boolean twinkleDelay;

      public Starter(
         ClientLevel param0,
         double param1,
         double param3,
         double param5,
         double param7,
         double param9,
         double param11,
         ParticleEngine param13,
         @Nullable CompoundTag param14
      ) {
         super(param0, param1, param3, param5);
         this.xd = param7;
         this.yd = param9;
         this.zd = param11;
         this.engine = param13;
         this.lifetime = 8;
         if (param14 != null) {
            this.explosions = param14.getList("Explosions", 10);
            if (this.explosions.isEmpty()) {
               this.explosions = null;
            } else {
               this.lifetime = this.explosions.size() * 2 - 1;

               for (int var16 = 0; var16 < this.explosions.size(); var16++) {
                  CompoundTag var17 = this.explosions.getCompound(var16);
                  if (var17.getBoolean("Flicker")) {
                     this.twinkleDelay = true;
                     this.lifetime += 15;
                     break;
                  }
               }
            }
         }
      }

      @Override
      public void tick() {
         if (this.life == 0 && this.explosions != null) {
            boolean var1 = this.isFarAwayFromCamera();
            boolean var2 = false;
            if (this.explosions.size() >= 3) {
               var2 = true;
            } else {
               for (int var3 = 0; var3 < this.explosions.size(); var3++) {
                  CompoundTag var4 = this.explosions.getCompound(var3);
                  if (FireworkRocketItem.Shape.byId(var4.getByte("Type")) == FireworkRocketItem.Shape.LARGE_BALL) {
                     var2 = true;
                     break;
                  }
               }
            }

            SoundEvent var3;
            if (var2) {
               var3 = var1 ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST;
            } else {
               var3 = var1 ? SoundEvents.FIREWORK_ROCKET_BLAST_FAR : SoundEvents.FIREWORK_ROCKET_BLAST;
            }

            this.level.playLocalSound(this.x, this.y, this.z, var3, SoundSource.AMBIENT, 20.0F, 0.95F + this.random.nextFloat() * 0.1F, true);
         }

         if (this.life % 2 == 0 && this.explosions != null && this.life / 2 < this.explosions.size()) {
            int var1 = this.life / 2;
            CompoundTag var2 = this.explosions.getCompound(var1);
            FireworkRocketItem.Shape var3 = FireworkRocketItem.Shape.byId(var2.getByte("Type"));
            boolean var4 = var2.getBoolean("Trail");
            boolean var5 = var2.getBoolean("Flicker");
            int[] var6 = var2.getIntArray("Colors");
            int[] var7 = var2.getIntArray("FadeColors");
            if (var6.length == 0) {
               var6 = new int[]{DyeColor.BLACK.getFireworkColor()};
            }

            switch (var3) {
               case SMALL_BALL:
               default:
                  this.createParticleBall(0.25, 2, var6, var7, var4, var5);
                  break;
               case LARGE_BALL:
                  this.createParticleBall(0.5, 4, var6, var7, var4, var5);
                  break;
               case STAR:
                  this.createParticleShape(
                     0.5,
                     new double[][]{
                        {0.0, 1.0},
                        {0.3455, 0.309},
                        {0.9511, 0.309},
                        {0.3795918367346939, -0.12653061224489795},
                        {0.6122448979591837, -0.8040816326530612},
                        {0.0, -0.35918367346938773}
                     },
                     var6,
                     var7,
                     var4,
                     var5,
                     false
                  );
                  break;
               case CREEPER:
                  this.createParticleShape(
                     0.5,
                     new double[][]{
                        {0.0, 0.2},
                        {0.2, 0.2},
                        {0.2, 0.6},
                        {0.6, 0.6},
                        {0.6, 0.2},
                        {0.2, 0.2},
                        {0.2, 0.0},
                        {0.4, 0.0},
                        {0.4, -0.6},
                        {0.2, -0.6},
                        {0.2, -0.4},
                        {0.0, -0.4}
                     },
                     var6,
                     var7,
                     var4,
                     var5,
                     true
                  );
                  break;
               case BURST:
                  this.createParticleBurst(var6, var7, var4, var5);
            }

            int var8 = var6[0];
            float var9 = ((var8 & 0xFF0000) >> 16) / 255.0F;
            float var10 = ((var8 & 0xFF00) >> 8) / 255.0F;
            float var11 = ((var8 & 0xFF) >> 0) / 255.0F;
            Particle var12 = this.engine.createParticle(ParticleTypes.FLASH, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            var12.setColor(var9, var10, var11);
         }

         this.life++;
         if (this.life > this.lifetime) {
            if (this.twinkleDelay) {
               boolean var1 = this.isFarAwayFromCamera();
               SoundEvent var2 = var1 ? SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR : SoundEvents.FIREWORK_ROCKET_TWINKLE;
               this.level.playLocalSound(this.x, this.y, this.z, var2, SoundSource.AMBIENT, 20.0F, 0.9F + this.random.nextFloat() * 0.15F, true);
            }

            this.remove();
         }
      }

      private boolean isFarAwayFromCamera() {
         Minecraft var1 = Minecraft.getInstance();
         return var1.gameRenderer.getMainCamera().getPosition().distanceToSqr(this.x, this.y, this.z) >= 256.0;
      }

      private void createParticle(
         double param0,
         double param2,
         double param4,
         double param6,
         double param8,
         double param10,
         int[] param12,
         int[] param13,
         boolean param14,
         boolean param15
      ) {
         FireworkParticles.SparkParticle var17 = (FireworkParticles.SparkParticle)this.engine
            .createParticle(ParticleTypes.FIREWORK, param0, param2, param4, param6, param8, param10);
         var17.setTrail(param14);
         var17.setFlicker(param15);
         var17.setAlpha(0.99F);
         int var18 = this.random.nextInt(param12.length);
         var17.setColor(param12[var18]);
         if (param13.length > 0) {
            var17.setFadeColor(Util.getRandom(param13, this.random));
         }
      }

      private void createParticleBall(double param0, int param2, int[] param3, int[] param4, boolean param5, boolean param6) {
         double var8 = this.x;
         double var10 = this.y;
         double var12 = this.z;

         for (int var14 = -param2; var14 <= param2; var14++) {
            for (int var15 = -param2; var15 <= param2; var15++) {
               for (int var16 = -param2; var16 <= param2; var16++) {
                  double var17 = var15 + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
                  double var19 = var14 + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
                  double var21 = var16 + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
                  double var23 = Math.sqrt(var17 * var17 + var19 * var19 + var21 * var21) / param0 + this.random.nextGaussian() * 0.05;
                  this.createParticle(var8, var10, var12, var17 / var23, var19 / var23, var21 / var23, param3, param4, param5, param6);
                  if (var14 != -param2 && var14 != param2 && var15 != -param2 && var15 != param2) {
                     var16 += param2 * 2 - 1;
                  }
               }
            }
         }
      }

      private void createParticleShape(double param0, double[][] param2, int[] param3, int[] param4, boolean param5, boolean param6, boolean param7) {
         double var9 = param2[0][0];
         double var11 = param2[0][1];
         this.createParticle(this.x, this.y, this.z, var9 * param0, var11 * param0, 0.0, param3, param4, param5, param6);
         float var13 = this.random.nextFloat() * (float) Math.PI;
         double var14 = param7 ? 0.034 : 0.34;

         for (int var16 = 0; var16 < 3; var16++) {
            double var17 = var13 + var16 * (float) Math.PI * var14;
            double var19 = var9;
            double var21 = var11;

            for (int var23 = 1; var23 < param2.length; var23++) {
               double var24 = param2[var23][0];
               double var26 = param2[var23][1];

               for (double var28 = 0.25; var28 <= 1.0; var28 += 0.25) {
                  double var30 = Mth.lerp(var28, var19, var24) * param0;
                  double var32 = Mth.lerp(var28, var21, var26) * param0;
                  double var34 = var30 * Math.sin(var17);
                  var30 *= Math.cos(var17);

                  for (double var36 = -1.0; var36 <= 1.0; var36 += 2.0) {
                     this.createParticle(this.x, this.y, this.z, var30 * var36, var32, var34 * var36, param3, param4, param5, param6);
                  }
               }

               var19 = var24;
               var21 = var26;
            }
         }
      }

      private void createParticleBurst(int[] param0, int[] param1, boolean param2, boolean param3) {
         double var5 = this.random.nextGaussian() * 0.05;
         double var7 = this.random.nextGaussian() * 0.05;

         for (int var9 = 0; var9 < 70; var9++) {
            double var10 = this.xd * 0.5 + this.random.nextGaussian() * 0.15 + var5;
            double var12 = this.zd * 0.5 + this.random.nextGaussian() * 0.15 + var7;
            double var14 = this.yd * 0.5 + this.random.nextDouble() * 0.5;
            this.createParticle(this.x, this.y, this.z, var10, var14, var12, param0, param1, param2, param3);
         }
      }
   }
}
