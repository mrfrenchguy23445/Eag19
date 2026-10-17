package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import org.joml.Matrix4f;

public class LightningBoltRenderer extends EntityRenderer<LightningBolt> {
   public LightningBoltRenderer(EntityRendererProvider.Context param0) {
      super(param0);
   }

   public void render(LightningBolt param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      float[] var7 = new float[8];
      float[] var8 = new float[8];
      float var9 = 0.0F;
      float var10 = 0.0F;
      RandomSource var11 = RandomSource.create(param0.seed);

      for (int var12 = 7; var12 >= 0; var12--) {
         var7[var12] = var9;
         var8[var12] = var10;
         var9 += var11.nextInt(11) - 5;
         var10 += var11.nextInt(11) - 5;
      }

      VertexConsumer var11x = param4.getBuffer(RenderType.lightning());
      Matrix4f var12 = param3.last().pose();

      for (int var13 = 0; var13 < 4; var13++) {
         RandomSource var14 = RandomSource.create(param0.seed);

         for (int var15 = 0; var15 < 3; var15++) {
            int var16 = 7;
            int var17 = 0;
            if (var15 > 0) {
               var16 = 7 - var15;
            }

            if (var15 > 0) {
               var17 = var16 - 2;
            }

            float var18 = var7[var16] - var9;
            float var19 = var8[var16] - var10;

            for (int var20 = var16; var20 >= var17; var20--) {
               float var21 = var18;
               float var22 = var19;
               if (var15 == 0) {
                  var18 += var14.nextInt(11) - 5;
                  var19 += var14.nextInt(11) - 5;
               } else {
                  var18 += var14.nextInt(31) - 15;
                  var19 += var14.nextInt(31) - 15;
               }

               float var23 = 0.5F;
               float var24 = 0.45F;
               float var25 = 0.45F;
               float var26 = 0.5F;
               float var27 = 0.1F + var13 * 0.2F;
               if (var15 == 0) {
                  var27 *= var20 * 0.1F + 1.0F;
               }

               float var28 = 0.1F + var13 * 0.2F;
               if (var15 == 0) {
                  var28 *= (var20 - 1.0F) * 0.1F + 1.0F;
               }

               quad(var12, var11x, var18, var19, var20, var21, var22, 0.45F, 0.45F, 0.5F, var27, var28, false, false, true, false);
               quad(var12, var11x, var18, var19, var20, var21, var22, 0.45F, 0.45F, 0.5F, var27, var28, true, false, true, true);
               quad(var12, var11x, var18, var19, var20, var21, var22, 0.45F, 0.45F, 0.5F, var27, var28, true, true, false, true);
               quad(var12, var11x, var18, var19, var20, var21, var22, 0.45F, 0.45F, 0.5F, var27, var28, false, true, false, false);
            }
         }
      }
   }

   private static void quad(
      Matrix4f param0,
      VertexConsumer param1,
      float param2,
      float param3,
      int param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9,
      float param10,
      float param11,
      boolean param12,
      boolean param13,
      boolean param14,
      boolean param15
   ) {
      param1.vertex(param0, param2 + (param12 ? param11 : -param11), param4 * 16, param3 + (param13 ? param11 : -param11))
         .color(param7, param8, param9, 0.3F)
         .endVertex();
      param1.vertex(param0, param5 + (param12 ? param10 : -param10), (param4 + 1) * 16, param6 + (param13 ? param10 : -param10))
         .color(param7, param8, param9, 0.3F)
         .endVertex();
      param1.vertex(param0, param5 + (param14 ? param10 : -param10), (param4 + 1) * 16, param6 + (param15 ? param10 : -param10))
         .color(param7, param8, param9, 0.3F)
         .endVertex();
      param1.vertex(param0, param2 + (param14 ? param11 : -param11), param4 * 16, param3 + (param15 ? param11 : -param11))
         .color(param7, param8, param9, 0.3F)
         .endVertex();
   }

   public ResourceLocation getTextureLocation(LightningBolt param0) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
