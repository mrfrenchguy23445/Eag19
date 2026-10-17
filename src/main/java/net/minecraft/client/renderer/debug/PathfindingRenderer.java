package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Map<Integer, Path> pathMap = Maps.newHashMap();
   private final Map<Integer, Float> pathMaxDist = Maps.newHashMap();
   private final Map<Integer, Long> creationMap = Maps.newHashMap();
   private static final long TIMEOUT = 5000L;
   private static final float MAX_RENDER_DIST = 80.0F;
   private static final boolean SHOW_OPEN_CLOSED = true;
   private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
   private static final boolean SHOW_GROUND_LABELS = true;
   private static final float TEXT_SCALE = 0.02F;

   public void addPath(int param0, Path param1, float param2) {
      this.pathMap.put(param0, param1);
      this.creationMap.put(param0, Util.getMillis());
      this.pathMaxDist.put(param0, param2);
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      if (!this.pathMap.isEmpty()) {
         long var9 = Util.getMillis();

         for (Integer var12 : this.pathMap.keySet()) {
            Path var13 = this.pathMap.get(var12);
            float var14 = this.pathMaxDist.get(var12);
            renderPath(param0, param1, var13, var14, true, true, param2, param4, param6);
         }

         for (Integer var14 : this.creationMap.keySet().toArray(new Integer[0])) {
            if (var9 - this.creationMap.get(var14) > 5000L) {
               this.pathMap.remove(var14);
               this.creationMap.remove(var14);
            }
         }
      }
   }

   public static void renderPath(
      PoseStack param0, MultiBufferSource param1, Path param2, float param3, boolean param4, boolean param5, double param6, double param8, double param10
   ) {
      renderPathLine(param0, param1.getBuffer(RenderType.debugLineStrip(6.0)), param2, param6, param8, param10);
      BlockPos var12 = param2.getTarget();
      if (distanceToCamera(var12, param6, param8, param10) <= 80.0F) {
         DebugRenderer.renderFilledBox(
            param0,
            param1,
            new AABB(var12.getX() + 0.25F, var12.getY() + 0.25F, var12.getZ() + 0.25, var12.getX() + 0.75F, var12.getY() + 0.75F, var12.getZ() + 0.75F)
               .move(-param6, -param8, -param10),
            0.0F,
            1.0F,
            0.0F,
            0.5F
         );

         for (int var13 = 0; var13 < param2.getNodeCount(); var13++) {
            Node var14 = param2.getNode(var13);
            if (distanceToCamera(var14.asBlockPos(), param6, param8, param10) <= 80.0F) {
               float var15 = var13 == param2.getNextNodeIndex() ? 1.0F : 0.0F;
               float var16 = var13 == param2.getNextNodeIndex() ? 0.0F : 1.0F;
               DebugRenderer.renderFilledBox(
                  param0,
                  param1,
                  new AABB(
                        var14.x + 0.5F - param3,
                        var14.y + 0.01F * var13,
                        var14.z + 0.5F - param3,
                        var14.x + 0.5F + param3,
                        var14.y + 0.25F + 0.01F * var13,
                        var14.z + 0.5F + param3
                     )
                     .move(-param6, -param8, -param10),
                  var15,
                  0.0F,
                  var16,
                  0.5F
               );
            }
         }
      }

      if (param4) {
         for (Node var16 : param2.getClosedSet()) {
            if (distanceToCamera(var16.asBlockPos(), param6, param8, param10) <= 80.0F) {
               DebugRenderer.renderFilledBox(
                  param0,
                  param1,
                  new AABB(
                        var16.x + 0.5F - param3 / 2.0F,
                        var16.y + 0.01F,
                        var16.z + 0.5F - param3 / 2.0F,
                        var16.x + 0.5F + param3 / 2.0F,
                        var16.y + 0.1,
                        var16.z + 0.5F + param3 / 2.0F
                     )
                     .move(-param6, -param8, -param10),
                  1.0F,
                  0.8F,
                  0.8F,
                  0.5F
               );
            }
         }

         for (Node var16 : param2.getOpenSet()) {
            if (distanceToCamera(var16.asBlockPos(), param6, param8, param10) <= 80.0F) {
               DebugRenderer.renderFilledBox(
                  param0,
                  param1,
                  new AABB(
                        var16.x + 0.5F - param3 / 2.0F,
                        var16.y + 0.01F,
                        var16.z + 0.5F - param3 / 2.0F,
                        var16.x + 0.5F + param3 / 2.0F,
                        var16.y + 0.1,
                        var16.z + 0.5F + param3 / 2.0F
                     )
                     .move(-param6, -param8, -param10),
                  0.8F,
                  1.0F,
                  1.0F,
                  0.5F
               );
            }
         }
      }

      if (param5) {
         for (int var13 = 0; var13 < param2.getNodeCount(); var13++) {
            Node var14 = param2.getNode(var13);
            if (distanceToCamera(var14.asBlockPos(), param6, param8, param10) <= 80.0F) {
               DebugRenderer.renderFloatingText(
                  param0, param1, String.valueOf(var14.type), var14.x + 0.5, var14.y + 0.75, var14.z + 0.5, -1, 0.02F, true, 0.0F, true
               );
               DebugRenderer.renderFloatingText(
                  param0,
                  param1,
                  String.format(Locale.ROOT, "%.2f", var14.costMalus),
                  var14.x + 0.5,
                  var14.y + 0.25,
                  var14.z + 0.5,
                  -1,
                  0.02F,
                  true,
                  0.0F,
                  true
               );
            }
         }
      }
   }

   public static void renderPathLine(PoseStack param0, VertexConsumer param1, Path param2, double param3, double param5, double param7) {
      for (int var9 = 0; var9 < param2.getNodeCount(); var9++) {
         Node var10 = param2.getNode(var9);
         if (!(distanceToCamera(var10.asBlockPos(), param3, param5, param7) > 80.0F)) {
            float var11 = (float)var9 / param2.getNodeCount() * 0.33F;
            int var12 = var9 == 0 ? 0 : Mth.hsvToRgb(var11, 0.9F, 0.9F);
            int var13 = var12 >> 16 & 0xFF;
            int var14 = var12 >> 8 & 0xFF;
            int var15 = var12 & 0xFF;
            param1.vertex(param0.last().pose(), (float)(var10.x - param3 + 0.5), (float)(var10.y - param5 + 0.5), (float)(var10.z - param7 + 0.5))
               .color(var13, var14, var15, 255)
               .endVertex();
         }
      }
   }

   private static float distanceToCamera(BlockPos param0, double param1, double param3, double param5) {
      return (float)(Math.abs(param0.getX() - param1) + Math.abs(param0.getY() - param3) + Math.abs(param0.getZ() - param5));
   }
}
