package net.minecraft.client.model;

import java.util.Arrays;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class BlazeModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart root;
   private final ModelPart[] upperBodyParts;
   private final ModelPart head;

   public BlazeModel(ModelPart param0) {
      this.root = param0;
      this.head = param0.getChild("head");
      this.upperBodyParts = new ModelPart[12];
      Arrays.setAll(this.upperBodyParts, l_param1 -> param0.getChild(getPartName(l_param1)));
   }

   private static String getPartName(int param0) {
      return "part" + param0;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      float var2 = 0.0F;
      CubeListBuilder var3 = CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);

      for (int var4 = 0; var4 < 4; var4++) {
         float var5 = Mth.cos(var2) * 9.0F;
         float var6 = -2.0F + Mth.cos(var4 * 2 * 0.25F);
         float var7 = Mth.sin(var2) * 9.0F;
         var1.addOrReplaceChild(getPartName(var4), var3, PartPose.offset(var5, var6, var7));
         var2 += (float) (Math.PI / 2);
      }

      var2 = (float) (Math.PI / 4);

      for (int var4 = 4; var4 < 8; var4++) {
         float var5 = Mth.cos(var2) * 7.0F;
         float var6 = 2.0F + Mth.cos(var4 * 2 * 0.25F);
         float var7 = Mth.sin(var2) * 7.0F;
         var1.addOrReplaceChild(getPartName(var4), var3, PartPose.offset(var5, var6, var7));
         var2 += (float) (Math.PI / 2);
      }

      var2 = 0.47123894F;

      for (int var4 = 8; var4 < 12; var4++) {
         float var5 = Mth.cos(var2) * 5.0F;
         float var6 = 11.0F + Mth.cos(var4 * 1.5F * 0.5F);
         float var7 = Mth.sin(var2) * 5.0F;
         var1.addOrReplaceChild(getPartName(var4), var3, PartPose.offset(var5, var6, var7));
         var2 += (float) (Math.PI / 2);
      }

      return LayerDefinition.create(var0, 64, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   @Override
   public void setupAnim(T param0, float param1, float param2, float param3, float param4, float param5) {
      float var7 = param3 * (float) Math.PI * -0.1F;

      for (int var8 = 0; var8 < 4; var8++) {
         this.upperBodyParts[var8].y = -2.0F + Mth.cos((var8 * 2 + param3) * 0.25F);
         this.upperBodyParts[var8].x = Mth.cos(var7) * 9.0F;
         this.upperBodyParts[var8].z = Mth.sin(var7) * 9.0F;
         var7 += (float) (Math.PI / 2);
      }

      var7 = (float) (Math.PI / 4) + param3 * (float) Math.PI * 0.03F;

      for (int var8 = 4; var8 < 8; var8++) {
         this.upperBodyParts[var8].y = 2.0F + Mth.cos((var8 * 2 + param3) * 0.25F);
         this.upperBodyParts[var8].x = Mth.cos(var7) * 7.0F;
         this.upperBodyParts[var8].z = Mth.sin(var7) * 7.0F;
         var7 += (float) (Math.PI / 2);
      }

      var7 = 0.47123894F + param3 * (float) Math.PI * -0.05F;

      for (int var8 = 8; var8 < 12; var8++) {
         this.upperBodyParts[var8].y = 11.0F + Mth.cos((var8 * 1.5F + param3) * 0.5F);
         this.upperBodyParts[var8].x = Mth.cos(var7) * 5.0F;
         this.upperBodyParts[var8].z = Mth.sin(var7) * 5.0F;
         var7 += (float) (Math.PI / 2);
      }

      this.head.yRot = param4 * (float) (Math.PI / 180.0);
      this.head.xRot = param5 * (float) (Math.PI / 180.0);
   }
}
