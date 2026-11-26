package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private final Minecraft minecraft;
   private final Map<Integer, List<GoalSelectorDebugRenderer.DebugGoal>> goalSelectors = Maps.newHashMap();

   @Override
   public void clear() {
      this.goalSelectors.clear();
   }

   public void addGoalSelector(int param0, List<GoalSelectorDebugRenderer.DebugGoal> param1) {
      this.goalSelectors.put(param0, param1);
   }

   public void removeGoalSelector(int param0) {
      this.goalSelectors.remove(param0);
   }

   public GoalSelectorDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      Camera var9 = this.minecraft.gameRenderer.getMainCamera();
      BlockPos var10 = BlockPos.containing(var9.getPosition().x, 0.0, var9.getPosition().z);
      this.goalSelectors.forEach((l_param3, l_param4) -> {
         for (int l_var5 = 0; l_var5 < l_param4.size(); l_var5++) {
            GoalSelectorDebugRenderer.DebugGoal l_var6 = l_param4.get(l_var5);
            if (var10.closerThan(l_var6.pos, 160.0)) {
               double l_var7 = l_var6.pos.getX() + 0.5;
               double l_var9 = l_var6.pos.getY() + 2.0 + l_var5 * 0.25;
               double l_var11 = l_var6.pos.getZ() + 0.5;
               int l_var13 = l_var6.isRunning ? -16711936 : -3355444;
               DebugRenderer.renderFloatingText(param0, param1, l_var6.name, l_var7, l_var9, l_var11, l_var13);
            }
         }
      });
   }

   public static class DebugGoal {
      public final BlockPos pos;
      public final int priority;
      public final String name;
      public final boolean isRunning;

      public DebugGoal(BlockPos param0, int param1, String param2, boolean param3) {
         this.pos = param0;
         this.priority = param1;
         this.name = param2;
         this.isRunning = param3;
      }
   }
}
