package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private final Map<DimensionType, Map<String, BoundingBox>> postMainBoxes = Maps.newIdentityHashMap();
   private final Map<DimensionType, Map<String, BoundingBox>> postPiecesBoxes = Maps.newIdentityHashMap();
   private final Map<DimensionType, Map<String, Boolean>> startPiecesMap = Maps.newIdentityHashMap();
   private static final int MAX_RENDER_DIST = 500;

   public StructureRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      Camera var9 = this.minecraft.gameRenderer.getMainCamera();
      LevelAccessor var10 = this.minecraft.level;
      DimensionType var11 = var10.dimensionType();
      BlockPos var12 = BlockPos.containing(var9.getPosition().x, 0.0, var9.getPosition().z);
      VertexConsumer var13 = param1.getBuffer(RenderType.lines());
      if (this.postMainBoxes.containsKey(var11)) {
         for (BoundingBox var15 : this.postMainBoxes.get(var11).values()) {
            if (var12.closerThan(var15.getCenter(), 500.0)) {
               LevelRenderer.renderLineBox(
                  param0,
                  var13,
                  var15.minX() - param2,
                  var15.minY() - param4,
                  var15.minZ() - param6,
                  var15.maxX() + 1 - param2,
                  var15.maxY() + 1 - param4,
                  var15.maxZ() + 1 - param6,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F
               );
            }
         }
      }

      if (this.postPiecesBoxes.containsKey(var11)) {
         for (Entry<String, BoundingBox> var15 : this.postPiecesBoxes.get(var11).entrySet()) {
            String var16 = var15.getKey();
            BoundingBox var17 = var15.getValue();
            Boolean var18 = this.startPiecesMap.get(var11).get(var16);
            if (var12.closerThan(var17.getCenter(), 500.0)) {
               if (var18) {
                  LevelRenderer.renderLineBox(
                     param0,
                     var13,
                     var17.minX() - param2,
                     var17.minY() - param4,
                     var17.minZ() - param6,
                     var17.maxX() + 1 - param2,
                     var17.maxY() + 1 - param4,
                     var17.maxZ() + 1 - param6,
                     0.0F,
                     1.0F,
                     0.0F,
                     1.0F,
                     0.0F,
                     1.0F,
                     0.0F
                  );
               } else {
                  LevelRenderer.renderLineBox(
                     param0,
                     var13,
                     var17.minX() - param2,
                     var17.minY() - param4,
                     var17.minZ() - param6,
                     var17.maxX() + 1 - param2,
                     var17.maxY() + 1 - param4,
                     var17.maxZ() + 1 - param6,
                     0.0F,
                     0.0F,
                     1.0F,
                     1.0F,
                     0.0F,
                     0.0F,
                     1.0F
                  );
               }
            }
         }
      }
   }

   public void addBoundingBox(BoundingBox param0, List<BoundingBox> param1, List<Boolean> param2, DimensionType param3) {
      if (!this.postMainBoxes.containsKey(param3)) {
         this.postMainBoxes.put(param3, Maps.newHashMap());
      }

      if (!this.postPiecesBoxes.containsKey(param3)) {
         this.postPiecesBoxes.put(param3, Maps.newHashMap());
         this.startPiecesMap.put(param3, Maps.newHashMap());
      }

      this.postMainBoxes.get(param3).put(param0.toString(), param0);

      for (int var5 = 0; var5 < param1.size(); var5++) {
         BoundingBox var6 = param1.get(var5);
         Boolean var7 = param2.get(var5);
         this.postPiecesBoxes.get(param3).put(var6.toString(), var6);
         this.startPiecesMap.get(param3).put(var6.toString(), var7);
      }
   }

   @Override
   public void clear() {
      this.postMainBoxes.clear();
      this.postPiecesBoxes.clear();
      this.startPiecesMap.clear();
   }
}
