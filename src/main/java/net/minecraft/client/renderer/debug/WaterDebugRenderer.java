package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

public class WaterDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public WaterDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      BlockPos var9 = this.minecraft.player.blockPosition();
      LevelReader var10 = this.minecraft.player.level;

      for (BlockPos var12 : BlockPos.betweenClosed(var9.offset(-10, -10, -10), var9.offset(10, 10, 10))) {
         FluidState var13 = var10.getFluidState(var12);
         if (var13.is(FluidTags.WATER)) {
            double var14 = var12.getY() + var13.getHeight(var10, var12);
            DebugRenderer.renderFilledBox(
               param0,
               param1,
               new AABB(var12.getX() + 0.01F, var12.getY() + 0.01F, var12.getZ() + 0.01F, var12.getX() + 0.99F, var14, var12.getZ() + 0.99F)
                  .move(-param2, -param4, -param6),
               0.0F,
               1.0F,
               0.0F,
               0.15F
            );
         }
      }

      for (BlockPos var12 : BlockPos.betweenClosed(var9.offset(-10, -10, -10), var9.offset(10, 10, 10))) {
         FluidState var13 = var10.getFluidState(var12);
         if (var13.is(FluidTags.WATER)) {
            DebugRenderer.renderFloatingText(
               param0,
               param1,
               String.valueOf(var13.getAmount()),
               var12.getX() + 0.5,
               var12.getY() + var13.getHeight(var10, var12),
               var12.getZ() + 0.5,
               -16777216
            );
         }
      }
   }
}
