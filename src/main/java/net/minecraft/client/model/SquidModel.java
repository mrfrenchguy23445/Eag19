package net.minecraft.client.model;

import java.util.Arrays;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class SquidModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart[] tentacles = new ModelPart[8];
   private final ModelPart root;

   public SquidModel(ModelPart param0) {
      this.root = param0;
      Arrays.setAll(this.tentacles, l_param1 -> param0.getChild(createTentacleName(l_param1)));
   }

   private static String createTentacleName(int param0) {
      return "tentacle" + param0;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      int var2 = -16;
      var1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
      int var3 = 8;
      CubeListBuilder var4 = CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F);

      for (int var5 = 0; var5 < 8; var5++) {
         double var6 = var5 * Math.PI * 2.0 / 8.0;
         float var8 = (float)Math.cos(var6) * 5.0F;
         float var9 = 15.0F;
         float var10 = (float)Math.sin(var6) * 5.0F;
         var6 = var5 * Math.PI * -2.0 / 8.0 + (Math.PI / 2);
         float var11 = (float)var6;
         var1.addOrReplaceChild(createTentacleName(var5), var4, PartPose.offsetAndRotation(var8, 15.0F, var10, 0.0F, var11, 0.0F));
      }

      return LayerDefinition.create(var0, 64, 32);
   }

   @Override
   public void setupAnim(T param0, float param1, float param2, float param3, float param4, float param5) {
      for (ModelPart var10 : this.tentacles) {
         var10.xRot = param3;
      }
   }

   @Override
   public ModelPart root() {
      return this.root;
   }
}
