package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int LISTENER_RENDER_DIST = 32;
   private static final float BOX_HEIGHT = 1.0F;
   private final List<GameEventListenerRenderer.TrackedGameEvent> trackedGameEvents = Lists.newArrayList();
   private final List<GameEventListenerRenderer.TrackedListener> trackedListeners = Lists.newArrayList();

   public GameEventListenerRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      Level var9 = this.minecraft.level;
      if (var9 == null) {
         this.trackedGameEvents.clear();
         this.trackedListeners.clear();
      } else {
         Vec3 var10 = new Vec3(param2, 0.0, param6);
         this.trackedGameEvents.removeIf(GameEventListenerRenderer.TrackedGameEvent::isExpired);
         this.trackedListeners.removeIf(l_param2 -> l_param2.isExpired(var9, var10));
         VertexConsumer var11 = param1.getBuffer(RenderType.lines());

         for (GameEventListenerRenderer.TrackedListener var13 : this.trackedListeners) {
            var13.getPosition(var9)
               .ifPresent(
                  l_param9 -> {
                     double l_var10 = l_param9.x() - var13.getListenerRadius();
                     double l_var12 = l_param9.y() - var13.getListenerRadius();
                     double l_var14 = l_param9.z() - var13.getListenerRadius();
                     double l_var16 = l_param9.x() + var13.getListenerRadius();
                     double l_var18 = l_param9.y() + var13.getListenerRadius();
                     double l_var20 = l_param9.z() + var13.getListenerRadius();
                     LevelRenderer.renderVoxelShape(
                        param0,
                        var11,
                        Shapes.create(new AABB(l_var10, l_var12, l_var14, l_var16, l_var18, l_var20)),
                        -param2,
                        -param4,
                        -param6,
                        1.0F,
                        1.0F,
                        0.0F,
                        0.35F
                     );
                  }
               );
         }

         VertexConsumer var12 = param1.getBuffer(RenderType.debugFilledBox());

         for (GameEventListenerRenderer.TrackedListener var14 : this.trackedListeners) {
            var14.getPosition(var9)
               .ifPresent(
                  l_param8 -> LevelRenderer.addChainedFilledBoxVertices(
                     param0,
                     var12,
                     l_param8.x() - 0.25 - param2,
                     l_param8.y() - param4,
                     l_param8.z() - 0.25 - param6,
                     l_param8.x() + 0.25 - param2,
                     l_param8.y() - param4 + 1.0,
                     l_param8.z() + 0.25 - param6,
                     1.0F,
                     1.0F,
                     0.0F,
                     0.35F
                  )
               );
         }

         for (GameEventListenerRenderer.TrackedListener var14 : this.trackedListeners) {
            var14.getPosition(var9)
               .ifPresent(
                  l_param2 -> {
                     DebugRenderer.renderFloatingText(param0, param1, "Listener Origin", l_param2.x(), l_param2.y() + 1.8F, l_param2.z(), -1, 0.025F);
                     DebugRenderer.renderFloatingText(
                        param0, param1, BlockPos.containing(l_param2).toString(), l_param2.x(), l_param2.y() + 1.5, l_param2.z(), -6959665, 0.025F
                     );
                  }
               );
         }

         for (GameEventListenerRenderer.TrackedGameEvent var14 : this.trackedGameEvents) {
            Vec3 var15 = var14.position;
            double var16 = 0.2F;
            double var18 = var15.x - 0.2F;
            double var20 = var15.y - 0.2F;
            double var22 = var15.z - 0.2F;
            double var24 = var15.x + 0.2F;
            double var26 = var15.y + 0.2F + 0.5;
            double var28 = var15.z + 0.2F;
            renderFilledBox(param0, param1, new AABB(var18, var20, var22, var24, var26, var28), 1.0F, 1.0F, 1.0F, 0.2F);
            DebugRenderer.renderFloatingText(param0, param1, var14.gameEvent.getName(), var15.x, var15.y + 0.85F, var15.z, -7564911, 0.0075F);
         }
      }
   }

   private static void renderFilledBox(PoseStack param0, MultiBufferSource param1, AABB param2, float param3, float param4, float param5, float param6) {
      Camera var7 = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (var7.isInitialized()) {
         Vec3 var8 = var7.getPosition().reverse();
         DebugRenderer.renderFilledBox(param0, param1, param2.move(var8), param3, param4, param5, param6);
      }
   }

   public void trackGameEvent(GameEvent param0, Vec3 param1) {
      this.trackedGameEvents.add(new GameEventListenerRenderer.TrackedGameEvent(Util.getMillis(), param0, param1));
   }

   public void trackListener(PositionSource param0, int param1) {
      this.trackedListeners.add(new GameEventListenerRenderer.TrackedListener(param0, param1));
   }

   record TrackedGameEvent(long timeStamp, GameEvent gameEvent, Vec3 position) {

      public boolean isExpired() {
         return Util.getMillis() - this.timeStamp > 3000L;
      }
   }

   static class TrackedListener implements GameEventListener {
      public final PositionSource listenerSource;
      public final int listenerRange;

      public TrackedListener(PositionSource param0, int param1) {
         this.listenerSource = param0;
         this.listenerRange = param1;
      }

      public boolean isExpired(Level param0, Vec3 param1) {
         return this.listenerSource.getPosition(param0).filter(l_param1 -> l_param1.distanceToSqr(param1) <= 1024.0).isPresent();
      }

      public Optional<Vec3> getPosition(Level param0) {
         return this.listenerSource.getPosition(param0);
      }

      @Override
      public PositionSource getListenerSource() {
         return this.listenerSource;
      }

      @Override
      public int getListenerRadius() {
         return this.listenerRange;
      }

      @Override
      public boolean handleGameEvent(ServerLevel param0, GameEvent param1, GameEvent.Context param2, Vec3 param3) {
         return false;
      }
   }
}
