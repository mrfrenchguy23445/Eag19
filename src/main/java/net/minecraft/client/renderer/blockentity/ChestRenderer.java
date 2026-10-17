package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Calendar;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class ChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T> {
   private static final String BOTTOM = "bottom";
   private static final String LID = "lid";
   private static final String LOCK = "lock";
   private final ModelPart lid;
   private final ModelPart bottom;
   private final ModelPart lock;
   private final ModelPart doubleLeftLid;
   private final ModelPart doubleLeftBottom;
   private final ModelPart doubleLeftLock;
   private final ModelPart doubleRightLid;
   private final ModelPart doubleRightBottom;
   private final ModelPart doubleRightLock;
   private boolean xmasTextures;

   public ChestRenderer(BlockEntityRendererProvider.Context param0) {
      Calendar var2 = Calendar.getInstance();
      if (var2.get(2) + 1 == 12 && var2.get(5) >= 24 && var2.get(5) <= 26) {
         this.xmasTextures = true;
      }

      ModelPart var3 = param0.bakeLayer(ModelLayers.CHEST);
      this.bottom = var3.getChild("bottom");
      this.lid = var3.getChild("lid");
      this.lock = var3.getChild("lock");
      ModelPart var4 = param0.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
      this.doubleLeftBottom = var4.getChild("bottom");
      this.doubleLeftLid = var4.getChild("lid");
      this.doubleLeftLock = var4.getChild("lock");
      ModelPart var5 = param0.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
      this.doubleRightBottom = var5.getChild("bottom");
      this.doubleRightLid = var5.getChild("lid");
      this.doubleRightLock = var5.getChild("lock");
   }

   public static LayerDefinition createSingleBodyLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
      var1.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      var1.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      return LayerDefinition.create(var0, 64, 64);
   }

   public static LayerDefinition createDoubleBodyRightLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
      var1.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      var1.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(15.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      return LayerDefinition.create(var0, 64, 64);
   }

   public static LayerDefinition createDoubleBodyLeftLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
      var1.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      var1.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      return LayerDefinition.create(var0, 64, 64);
   }

   @Override
   public void render(T param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      Level var7 = param0.getLevel();
      boolean var8 = var7 != null;
      BlockState var9 = var8 ? param0.getBlockState() : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
      ChestType var10 = var9.hasProperty(ChestBlock.TYPE) ? var9.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
      if (var9.getBlock() instanceof AbstractChestBlock<?> var12) {
         boolean var13 = var10 != ChestType.SINGLE;
         param2.pushPose();
         float var14 = var9.getValue(ChestBlock.FACING).toYRot();
         param2.translate(0.5F, 0.5F, 0.5F);
         param2.mulPose(Axis.YP.rotationDegrees(-var14));
         param2.translate(-0.5F, -0.5F, -0.5F);
         DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> var15;
         if (var8) {
            var15 = var12.combine(var9, var7, param0.getBlockPos(), true);
         } else {
            var15 = DoubleBlockCombiner.Combiner::acceptNone;
         }

         float var16 = var15.apply(ChestBlock.opennessCombiner(param0)).get(param1);
         var16 = 1.0F - var16;
         var16 = 1.0F - var16 * var16 * var16;
         int var17 = var15.apply(new BrightnessCombiner<>()).applyAsInt(param4);
         Material var18 = Sheets.chooseMaterial(param0, var10, this.xmasTextures);
         VertexConsumer var19 = var18.buffer(param3, RenderType::entityCutout);
         if (var13) {
            if (var10 == ChestType.LEFT) {
               this.render(param2, var19, this.doubleLeftLid, this.doubleLeftLock, this.doubleLeftBottom, var16, var17, param5);
            } else {
               this.render(param2, var19, this.doubleRightLid, this.doubleRightLock, this.doubleRightBottom, var16, var17, param5);
            }
         } else {
            this.render(param2, var19, this.lid, this.lock, this.bottom, var16, var17, param5);
         }

         param2.popPose();
      }
   }

   private void render(PoseStack param0, VertexConsumer param1, ModelPart param2, ModelPart param3, ModelPart param4, float param5, int param6, int param7) {
      param2.xRot = -(param5 * (float) (Math.PI / 2));
      param3.xRot = param2.xRot;
      param2.render(param0, param1, param6, param7);
      param3.render(param0, param1, param6, param7);
      param4.render(param0, param1, param6, param7);
   }
}
