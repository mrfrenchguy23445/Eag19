package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class EndermiteModel<T extends Entity> extends HierarchicalModel<T> {
   private static final int BODY_COUNT = 4;
   private static final int[][] BODY_SIZES = new int[][]{{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
   private static final int[][] BODY_TEXS = new int[][]{{0, 0}, {0, 5}, {0, 14}, {0, 18}};
   private final ModelPart root;
   private final ModelPart[] bodyParts;

   public EndermiteModel(ModelPart param0) {
      this.root = param0;
      this.bodyParts = new ModelPart[4];

      for (int var2 = 0; var2 < 4; var2++) {
         this.bodyParts[var2] = param0.getChild(createSegmentName(var2));
      }
   }

   private static String createSegmentName(int param0) {
      return "segment" + param0;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      float var2 = -3.5F;

      for (int var3 = 0; var3 < 4; var3++) {
         var1.addOrReplaceChild(
            createSegmentName(var3),
            CubeListBuilder.create()
               .texOffs(BODY_TEXS[var3][0], BODY_TEXS[var3][1])
               .addBox(BODY_SIZES[var3][0] * -0.5F, 0.0F, BODY_SIZES[var3][2] * -0.5F, BODY_SIZES[var3][0], BODY_SIZES[var3][1], BODY_SIZES[var3][2]),
            PartPose.offset(0.0F, 24 - BODY_SIZES[var3][1], var2)
         );
         if (var3 < 3) {
            var2 += (BODY_SIZES[var3][2] + BODY_SIZES[var3 + 1][2]) * 0.5F;
         }
      }

      return LayerDefinition.create(var0, 64, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   @Override
   public void setupAnim(T param0, float param1, float param2, float param3, float param4, float param5) {
      for (int var7 = 0; var7 < this.bodyParts.length; var7++) {
         this.bodyParts[var7].yRot = Mth.cos(param3 * 0.9F + var7 * 0.15F * (float) Math.PI) * (float) Math.PI * 0.01F * (1 + Math.abs(var7 - 2));
         this.bodyParts[var7].x = Mth.sin(param3 * 0.9F + var7 * 0.15F * (float) Math.PI) * (float) Math.PI * 0.1F * Math.abs(var7 - 2);
      }
   }
}
