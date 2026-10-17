package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class DripParticle extends TextureSheetParticle {
   private final Fluid type;
   protected boolean isGlowing;

   DripParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7) {
      super(param0, param1, param3, param5);
      this.setSize(0.01F, 0.01F);
      this.gravity = 0.06F;
      this.type = param7;
   }

   protected Fluid getType() {
      return this.type;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public int getLightColor(float param0) {
      return this.isGlowing ? 240 : super.getLightColor(param0);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.preMoveUpdate();
      if (!this.removed) {
         this.yd = this.yd - this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.postMoveUpdate();
         if (!this.removed) {
            this.xd *= 0.98F;
            this.yd *= 0.98F;
            this.zd *= 0.98F;
            if (this.type != Fluids.EMPTY) {
               BlockPos var1 = BlockPos.containing(this.x, this.y, this.z);
               FluidState var2 = this.level.getFluidState(var1);
               if (var2.getType() == this.type && this.y < var1.getY() + var2.getHeight(this.level, var1)) {
                  this.remove();
               }
            }
         }
      }
   }

   protected void preMoveUpdate() {
      if (this.lifetime-- <= 0) {
         this.remove();
      }
   }

   protected void postMoveUpdate() {
   }

   public static TextureSheetParticle createWaterHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripHangParticle(param1, param2, param4, param6, Fluids.WATER, ParticleTypes.FALLING_WATER);
      var14.setColor(0.2F, 0.3F, 1.0F);
      return var14;
   }

   public static TextureSheetParticle createWaterFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.FallAndLandParticle(param1, param2, param4, param6, Fluids.WATER, ParticleTypes.SPLASH);
      var14.setColor(0.2F, 0.3F, 1.0F);
      return var14;
   }

   public static TextureSheetParticle createLavaHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      return new DripParticle.CoolingDripHangParticle(param1, param2, param4, param6, Fluids.LAVA, ParticleTypes.FALLING_LAVA);
   }

   public static TextureSheetParticle createLavaFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.FallAndLandParticle(param1, param2, param4, param6, Fluids.LAVA, ParticleTypes.LANDING_LAVA);
      var14.setColor(1.0F, 0.2857143F, 0.083333336F);
      return var14;
   }

   public static TextureSheetParticle createLavaLandParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripLandParticle(param1, param2, param4, param6, Fluids.LAVA);
      var14.setColor(1.0F, 0.2857143F, 0.083333336F);
      return var14;
   }

   private static DripParticle setCherryColor(DripParticle param0) {
      param0.setColor(0.937F, 0.655F, 0.804F);
      return param0;
   }

   public static TextureSheetParticle createCherryLeavesHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      return setCherryColor(new DripParticle.DripHangParticle(param1, param2, param4, param6, Fluids.EMPTY, ParticleTypes.FALLING_CHERRY_LEAVES));
   }

   public static TextureSheetParticle createCherryLeavesFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle.FallAndLandParticle var14 = new DripParticle.FallAndLandParticle(
         param1, param2, param4, param6, Fluids.EMPTY, ParticleTypes.LANDING_CHERRY_LEAVES
      );
      var14.gravity = 0.005F;
      return setCherryColor(var14);
   }

   public static TextureSheetParticle createCherryLeavesLandParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      return setCherryColor(new DripParticle.DripLandParticle(param1, param2, param4, param6, Fluids.EMPTY));
   }

   public static TextureSheetParticle createHoneyHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle.DripHangParticle var14 = new DripParticle.DripHangParticle(param1, param2, param4, param6, Fluids.EMPTY, ParticleTypes.FALLING_HONEY);
      var14.gravity *= 0.01F;
      var14.lifetime = 100;
      var14.setColor(0.622F, 0.508F, 0.082F);
      return var14;
   }

   public static TextureSheetParticle createHoneyFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.HoneyFallAndLandParticle(param1, param2, param4, param6, Fluids.EMPTY, ParticleTypes.LANDING_HONEY);
      var14.gravity = 0.01F;
      var14.setColor(0.582F, 0.448F, 0.082F);
      return var14;
   }

   public static TextureSheetParticle createHoneyLandParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripLandParticle(param1, param2, param4, param6, Fluids.EMPTY);
      var14.lifetime = (int)(128.0 / (Math.random() * 0.8 + 0.2));
      var14.setColor(0.522F, 0.408F, 0.082F);
      return var14;
   }

   public static TextureSheetParticle createDripstoneWaterHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripHangParticle(param1, param2, param4, param6, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER);
      var14.setColor(0.2F, 0.3F, 1.0F);
      return var14;
   }

   public static TextureSheetParticle createDripstoneWaterFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripstoneFallAndLandParticle(param1, param2, param4, param6, Fluids.WATER, ParticleTypes.SPLASH);
      var14.setColor(0.2F, 0.3F, 1.0F);
      return var14;
   }

   public static TextureSheetParticle createDripstoneLavaHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      return new DripParticle.CoolingDripHangParticle(param1, param2, param4, param6, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA);
   }

   public static TextureSheetParticle createDripstoneLavaFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripstoneFallAndLandParticle(param1, param2, param4, param6, Fluids.LAVA, ParticleTypes.LANDING_LAVA);
      var14.setColor(1.0F, 0.2857143F, 0.083333336F);
      return var14;
   }

   public static TextureSheetParticle createNectarFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.FallingParticle(param1, param2, param4, param6, Fluids.EMPTY);
      var14.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      var14.gravity = 0.007F;
      var14.setColor(0.92F, 0.782F, 0.72F);
      return var14;
   }

   public static TextureSheetParticle createSporeBlossomFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      int var14 = (int)(64.0F / Mth.randomBetween(param1.getRandom(), 0.1F, 0.9F));
      DripParticle var15 = new DripParticle.FallingParticle(param1, param2, param4, param6, Fluids.EMPTY, var14);
      var15.gravity = 0.005F;
      var15.setColor(0.32F, 0.5F, 0.22F);
      return var15;
   }

   public static TextureSheetParticle createObsidianTearHangParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle.DripHangParticle var14 = new DripParticle.DripHangParticle(param1, param2, param4, param6, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR);
      var14.isGlowing = true;
      var14.gravity *= 0.01F;
      var14.lifetime = 100;
      var14.setColor(0.51171875F, 0.03125F, 0.890625F);
      return var14;
   }

   public static TextureSheetParticle createObsidianTearFallParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.FallAndLandParticle(param1, param2, param4, param6, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR);
      var14.isGlowing = true;
      var14.gravity = 0.01F;
      var14.setColor(0.51171875F, 0.03125F, 0.890625F);
      return var14;
   }

   public static TextureSheetParticle createObsidianTearLandParticle(
      SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      DripParticle var14 = new DripParticle.DripLandParticle(param1, param2, param4, param6, Fluids.EMPTY);
      var14.isGlowing = true;
      var14.lifetime = (int)(28.0 / (Math.random() * 0.8 + 0.2));
      var14.setColor(0.51171875F, 0.03125F, 0.890625F);
      return var14;
   }

   static class CoolingDripHangParticle extends DripParticle.DripHangParticle {
      CoolingDripHangParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7, ParticleOptions param8) {
         super(param0, param1, param3, param5, param7, param8);
      }

      @Override
      protected void preMoveUpdate() {
         this.rCol = 1.0F;
         this.gCol = 16.0F / (40 - this.lifetime + 16);
         this.bCol = 4.0F / (40 - this.lifetime + 8);
         super.preMoveUpdate();
      }
   }

   static class DripHangParticle extends DripParticle {
      private final ParticleOptions fallingParticle;

      DripHangParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7, ParticleOptions param8) {
         super(param0, param1, param3, param5, param7);
         this.fallingParticle = param8;
         this.gravity *= 0.02F;
         this.lifetime = 40;
      }

      @Override
      protected void preMoveUpdate() {
         if (this.lifetime-- <= 0) {
            this.remove();
            this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }
      }

      @Override
      protected void postMoveUpdate() {
         this.xd *= 0.02;
         this.yd *= 0.02;
         this.zd *= 0.02;
      }
   }

   static class DripLandParticle extends DripParticle {
      DripLandParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7) {
         super(param0, param1, param3, param5, param7);
         this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      }
   }

   static class DripstoneFallAndLandParticle extends DripParticle.FallAndLandParticle {
      DripstoneFallAndLandParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7, ParticleOptions param8) {
         super(param0, param1, param3, param5, param7, param8);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            SoundEvent var1 = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
            float var2 = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, var1, SoundSource.BLOCKS, var2, 1.0F, false);
         }
      }
   }

   static class FallAndLandParticle extends DripParticle.FallingParticle {
      protected final ParticleOptions landParticle;

      FallAndLandParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7, ParticleOptions param8) {
         super(param0, param1, param3, param5, param7);
         this.landParticle = param8;
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
         }
      }
   }

   static class FallingParticle extends DripParticle {
      FallingParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7) {
         this(param0, param1, param3, param5, param7, (int)(64.0 / (Math.random() * 0.8 + 0.2)));
      }

      FallingParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7, int param8) {
         super(param0, param1, param3, param5, param7);
         this.lifetime = param8;
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
         }
      }
   }

   static class HoneyFallAndLandParticle extends DripParticle.FallAndLandParticle {
      HoneyFallAndLandParticle(ClientLevel param0, double param1, double param3, double param5, Fluid param7, ParticleOptions param8) {
         super(param0, param1, param3, param5, param7, param8);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            float var1 = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, var1, 1.0F, false);
         }
      }
   }
}
