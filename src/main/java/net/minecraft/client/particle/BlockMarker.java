package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMarker extends TextureSheetParticle {
   BlockMarker(ClientLevel param0, double param1, double param3, double param5, BlockState param7) {
      super(param0, param1, param3, param5);
      this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(param7));
      this.gravity = 0.0F;
      this.lifetime = 80;
      this.hasPhysics = false;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.TERRAIN_SHEET;
   }

   @Override
   public float getQuadSize(float param0) {
      return 0.5F;
   }

   public static class Provider implements ParticleProvider<BlockParticleOption> {
      public Particle createParticle(
         BlockParticleOption param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new BlockMarker(param1, param2, param4, param6, param0.getState());
      }
   }
}
