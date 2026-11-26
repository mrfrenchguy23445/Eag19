package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SpellParticle extends TextureSheetParticle {
   private static final RandomSource RANDOM = RandomSource.create();
   private final SpriteSet sprites;

   SpellParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5, 0.5 - RANDOM.nextDouble(), param9, 0.5 - RANDOM.nextDouble());
      this.friction = 0.96F;
      this.gravity = -0.1F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = param13;
      this.yd *= 0.2F;
      if (param7 == 0.0 && param11 == 0.0) {
         this.xd *= 0.1F;
         this.zd *= 0.1F;
      }

      this.quadSize *= 0.75F;
      this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.setSpriteFromAge(param13);
      if (this.isCloseToScopingPlayer()) {
         this.setAlpha(0.0F);
      }
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      if (this.isCloseToScopingPlayer()) {
         this.setAlpha(0.0F);
      } else {
         this.setAlpha(Mth.lerp(0.05F, this.alpha, 1.0F));
      }
   }

   private boolean isCloseToScopingPlayer() {
      Minecraft var1 = Minecraft.getInstance();
      LocalPlayer var2 = var1.player;
      return var2 != null
         && var2.getEyePosition().distanceToSqr(this.x, this.y, this.z) <= 9.0
         && var1.options.getCameraType().isFirstPerson()
         && var2.isScoping();
   }

   public static class AmbientMobProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public AmbientMobProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         Particle var15 = new SpellParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
         var15.setAlpha(0.15F);
         var15.setColor((float)param8, (float)param10, (float)param12);
         return var15;
      }
   }

   public static class InstantProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public InstantProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new SpellParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
      }
   }

   public static class MobProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public MobProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         Particle var15 = new SpellParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
         var15.setColor((float)param8, (float)param10, (float)param12);
         return var15;
      }
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new SpellParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
      }
   }

   public static class WitchProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WitchProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         SpellParticle var15 = new SpellParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
         float var16 = param1.random.nextFloat() * 0.5F + 0.35F;
         var15.setColor(1.0F * var16, 0.0F * var16, 1.0F * var16);
         return var15;
      }
   }
}
