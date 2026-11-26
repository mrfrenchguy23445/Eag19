package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {
   private final BookModel bookModel;

   public LecternRenderer(BlockEntityRendererProvider.Context param0) {
      this.bookModel = new BookModel(param0.bakeLayer(ModelLayers.BOOK));
   }

   public void render(LecternBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      BlockState var7 = param0.getBlockState();
      if (var7.getValue(LecternBlock.HAS_BOOK)) {
         param2.pushPose();
         param2.translate(0.5F, 1.0625F, 0.5F);
         float var8 = var7.getValue(LecternBlock.FACING).getClockWise().toYRot();
         param2.mulPose(Axis.YP.rotationDegrees(-var8));
         param2.mulPose(Axis.ZP.rotationDegrees(67.5F));
         param2.translate(0.0F, -0.125F, 0.0F);
         this.bookModel.setupAnim(0.0F, 0.1F, 0.9F, 1.2F);
         VertexConsumer var9 = EnchantTableRenderer.BOOK_LOCATION.buffer(param3, RenderType::entitySolid);
         this.bookModel.render(param2, var9, param4, param5, 1.0F, 1.0F, 1.0F, 1.0F);
         param2.popPose();
      }
   }
}
