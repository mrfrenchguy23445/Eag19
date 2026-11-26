package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;

public class EnchantTableRenderer implements BlockEntityRenderer<EnchantmentTableBlockEntity> {
   public static final Material BOOK_LOCATION = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/enchanting_table_book"));
   private final BookModel bookModel;

   public EnchantTableRenderer(BlockEntityRendererProvider.Context param0) {
      this.bookModel = new BookModel(param0.bakeLayer(ModelLayers.BOOK));
   }

   public void render(EnchantmentTableBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      param2.pushPose();
      param2.translate(0.5F, 0.75F, 0.5F);
      float var7 = param0.time + param1;
      param2.translate(0.0F, 0.1F + Mth.sin(var7 * 0.1F) * 0.01F, 0.0F);
      float var8 = param0.rot - param0.oRot;

      while (var8 >= (float) Math.PI) {
         var8 -= (float) (Math.PI * 2);
      }

      while (var8 < (float) -Math.PI) {
         var8 += (float) (Math.PI * 2);
      }

      float var9 = param0.oRot + var8 * param1;
      param2.mulPose(Axis.YP.rotation(-var9));
      param2.mulPose(Axis.ZP.rotationDegrees(80.0F));
      float var10 = Mth.lerp(param1, param0.oFlip, param0.flip);
      float var11 = Mth.frac(var10 + 0.25F) * 1.6F - 0.3F;
      float var12 = Mth.frac(var10 + 0.75F) * 1.6F - 0.3F;
      float var13 = Mth.lerp(param1, param0.oOpen, param0.open);
      this.bookModel.setupAnim(var7, Mth.clamp(var11, 0.0F, 1.0F), Mth.clamp(var12, 0.0F, 1.0F), var13);
      VertexConsumer var14 = BOOK_LOCATION.buffer(param3, RenderType::entitySolid);
      this.bookModel.render(param2, var14, param4, param5, 1.0F, 1.0F, 1.0F, 1.0F);
      param2.popPose();
   }
}
