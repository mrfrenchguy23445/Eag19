package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class FallingDustParticle extends TextureSheetParticle {
   private final float rotSpeed;
   private final SpriteSet sprites;

   FallingDustParticle(ClientLevel param0, double param1, double param3, double param5, float param7, float param8, float param9, SpriteSet param10) {
      super(param0, param1, param3, param5);
      this.sprites = param10;
      this.rCol = param7;
      this.gCol = param8;
      this.bCol = param9;
      float var12 = 0.9F;
      this.quadSize *= 0.67499995F;
      int var13 = (int)(32.0 / (Math.random() * 0.8 + 0.2));
      this.lifetime = (int)Math.max(var13 * 0.9F, 1.0F);
      this.setSpriteFromAge(param10);
      this.rotSpeed = ((float)Math.random() - 0.5F) * 0.1F;
      this.roll = (float)Math.random() * (float) (Math.PI * 2);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public float getQuadSize(float param0) {
      return this.quadSize * Mth.clamp((this.age + param0) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
         this.oRoll = this.roll;
         this.roll = this.roll + (float) Math.PI * this.rotSpeed * 2.0F;
         if (this.onGround) {
            this.oRoll = this.roll = 0.0F;
         }

         this.move(this.xd, this.yd, this.zd);
         this.yd -= 0.003F;
         this.yd = Math.max(this.yd, -0.14F);
      }
   }

   public static class Provider implements ParticleProvider<BlockParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      @Nullable
      public Particle createParticle(
         BlockParticleOption param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         BlockState var15 = param0.getState();
         if (!var15.isAir() && var15.getRenderShape() == RenderShape.INVISIBLE) {
            return null;
         }

         BlockPos var16 = BlockPos.containing(param2, param4, param6);
         int var17 = Minecraft.getInstance().getBlockColors().getColor(var15, param1, var16);
         if (var15.getBlock() instanceof FallingBlock) {
            var17 = ((FallingBlock)var15.getBlock()).getDustColor(var15, param1, var16);
         }

         float var18 = (var17 >> 16 & 0xFF) / 255.0F;
         float var19 = (var17 >> 8 & 0xFF) / 255.0F;
         float var20 = (var17 & 0xFF) / 255.0F;
         return new FallingDustParticle(param1, param2, param4, param6, var18, var19, var20, this.sprite);
      }
   }
}
