package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;

public class WorldGenAttemptRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final List<BlockPos> toRender = Lists.newArrayList();
   private final List<Float> scales = Lists.newArrayList();
   private final List<Float> alphas = Lists.newArrayList();
   private final List<Float> reds = Lists.newArrayList();
   private final List<Float> greens = Lists.newArrayList();
   private final List<Float> blues = Lists.newArrayList();

   public void addPos(BlockPos param0, float param1, float param2, float param3, float param4, float param5) {
      this.toRender.add(param0);
      this.scales.add(param1);
      this.alphas.add(param5);
      this.reds.add(param2);
      this.greens.add(param3);
      this.blues.add(param4);
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      VertexConsumer var9 = param1.getBuffer(RenderType.debugFilledBox());

      for (int var10 = 0; var10 < this.toRender.size(); var10++) {
         BlockPos var11 = this.toRender.get(var10);
         Float var12 = this.scales.get(var10);
         float var13 = var12 / 2.0F;
         LevelRenderer.addChainedFilledBoxVertices(
            param0,
            var9,
            var11.getX() + 0.5F - var13 - param2,
            var11.getY() + 0.5F - var13 - param4,
            var11.getZ() + 0.5F - var13 - param6,
            var11.getX() + 0.5F + var13 - param2,
            var11.getY() + 0.5F + var13 - param4,
            var11.getZ() + 0.5F + var13 - param6,
            this.reds.get(var10),
            this.greens.get(var10),
            this.blues.get(var10),
            this.alphas.get(var10)
         );
      }
   }
}
