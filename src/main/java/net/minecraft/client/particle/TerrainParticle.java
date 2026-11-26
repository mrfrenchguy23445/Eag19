package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TerrainParticle extends TextureSheetParticle {
   private final BlockPos pos;
   private final float uo;
   private final float vo;

   public TerrainParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, BlockState param13) {
      this(param0, param1, param3, param5, param7, param9, param11, param13, BlockPos.containing(param1, param3, param5));
   }

   public TerrainParticle(
      ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, BlockState param13, BlockPos param14
   ) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.pos = param14;
      this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(param13));
      this.gravity = 1.0F;
      this.rCol = 0.6F;
      this.gCol = 0.6F;
      this.bCol = 0.6F;
      if (!param13.is(Blocks.GRASS_BLOCK)) {
         int var16 = Minecraft.getInstance().getBlockColors().getColor(param13, param0, param14, 0);
         this.rCol *= (var16 >> 16 & 0xFF) / 255.0F;
         this.gCol *= (var16 >> 8 & 0xFF) / 255.0F;
         this.bCol *= (var16 & 0xFF) / 255.0F;
      }

      this.quadSize /= 2.0F;
      this.uo = this.random.nextFloat() * 3.0F;
      this.vo = this.random.nextFloat() * 3.0F;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.TERRAIN_SHEET;
   }

   @Override
   protected float getU0() {
      return this.sprite.getU((this.uo + 1.0F) / 4.0F * 16.0F);
   }

   @Override
   protected float getU1() {
      return this.sprite.getU(this.uo / 4.0F * 16.0F);
   }

   @Override
   protected float getV0() {
      return this.sprite.getV(this.vo / 4.0F * 16.0F);
   }

   @Override
   protected float getV1() {
      return this.sprite.getV((this.vo + 1.0F) / 4.0F * 16.0F);
   }

   @Override
   public int getLightColor(float param0) {
      int var2 = super.getLightColor(param0);
      return var2 == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : var2;
   }

   public static class Provider implements ParticleProvider<BlockParticleOption> {
      public Particle createParticle(
         BlockParticleOption param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         BlockState var15 = param0.getState();
         return !var15.isAir() && !var15.is(Blocks.MOVING_PISTON) ? new TerrainParticle(param1, param2, param4, param6, param8, param10, param12, var15) : null;
      }
   }
}
