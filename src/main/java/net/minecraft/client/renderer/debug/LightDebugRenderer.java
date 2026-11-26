package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class LightDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int MAX_RENDER_DIST = 10;

   public LightDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      Level var9 = this.minecraft.level;
      BlockPos var10 = BlockPos.containing(param2, param4, param6);
      LongSet var11 = new LongOpenHashSet();

      for (BlockPos var13 : BlockPos.betweenClosed(var10.offset(-10, -10, -10), var10.offset(10, 10, 10))) {
         int var14 = var9.getBrightness(LightLayer.SKY, var13);
         float var15 = (15 - var14) / 15.0F * 0.5F + 0.16F;
         int var16 = Mth.hsvToRgb(var15, 0.9F, 0.9F);
         long var17 = SectionPos.blockToSection(var13.asLong());
         if (var11.add(var17)) {
            DebugRenderer.renderFloatingText(
               param0,
               param1,
               var9.getChunkSource().getLightEngine().getDebugData(LightLayer.SKY, SectionPos.of(var17)),
               SectionPos.sectionToBlockCoord(SectionPos.x(var17), 8),
               SectionPos.sectionToBlockCoord(SectionPos.y(var17), 8),
               SectionPos.sectionToBlockCoord(SectionPos.z(var17), 8),
               16711680,
               0.3F
            );
         }

         if (var14 != 15) {
            DebugRenderer.renderFloatingText(param0, param1, String.valueOf(var14), var13.getX() + 0.5, var13.getY() + 0.25, var13.getZ() + 0.5, var16);
         }
      }
   }
}
