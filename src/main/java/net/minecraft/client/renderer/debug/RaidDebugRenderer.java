package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private static final float TEXT_SCALE = 0.04F;
   private final Minecraft minecraft;
   private Collection<BlockPos> raidCenters = Lists.newArrayList();

   public RaidDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   public void setRaidCenters(Collection<BlockPos> param0) {
      this.raidCenters = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      BlockPos var9 = this.getCamera().getBlockPosition();

      for (BlockPos var11 : this.raidCenters) {
         if (var9.closerThan(var11, 160.0)) {
            highlightRaidCenter(param0, param1, var11);
         }
      }
   }

   private static void highlightRaidCenter(PoseStack param0, MultiBufferSource param1, BlockPos param2) {
      DebugRenderer.renderFilledBox(param0, param1, param2.offset(-1, -1, -1), param2.offset(1, 1, 1), 1.0F, 0.0F, 0.0F, 0.15F);
      int var3 = -65536;
      renderTextOverBlock(param0, param1, "Raid center", param2, -65536);
   }

   private static void renderTextOverBlock(PoseStack param0, MultiBufferSource param1, String param2, BlockPos param3, int param4) {
      double var5 = param3.getX() + 0.5;
      double var7 = param3.getY() + 1.3;
      double var9 = param3.getZ() + 0.5;
      DebugRenderer.renderFloatingText(param0, param1, param2, var5, var7, var9, param4, 0.04F, true, 0.0F, true);
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }
}
