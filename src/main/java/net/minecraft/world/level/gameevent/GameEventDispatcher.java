package net.minecraft.world.level.gameevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

public class GameEventDispatcher {
   private final ServerLevel level;

   public GameEventDispatcher(ServerLevel param0) {
      this.level = param0;
   }

   public void post(GameEvent param0, Vec3 param1, GameEvent.Context param2) {
      int var4 = param0.getNotificationRadius();
      BlockPos var5 = BlockPos.containing(param1);
      int var6 = SectionPos.blockToSectionCoord(var5.getX() - var4);
      int var7 = SectionPos.blockToSectionCoord(var5.getY() - var4);
      int var8 = SectionPos.blockToSectionCoord(var5.getZ() - var4);
      int var9 = SectionPos.blockToSectionCoord(var5.getX() + var4);
      int var10 = SectionPos.blockToSectionCoord(var5.getY() + var4);
      int var11 = SectionPos.blockToSectionCoord(var5.getZ() + var4);
      List<GameEvent.ListenerInfo> var12 = new ArrayList<>();
      GameEventListenerRegistry.ListenerVisitor var13 = (l_param4, l_param5) -> {
         if (l_param4.getDeliveryMode() == GameEventListener.DeliveryMode.BY_DISTANCE) {
            var12.add(new GameEvent.ListenerInfo(param0, param1, param2, l_param4, l_param5));
         } else {
            l_param4.handleGameEvent(this.level, param0, param2, param1);
         }
      };
      boolean var14 = false;

      for (int var15 = var6; var15 <= var9; var15++) {
         for (int var16 = var8; var16 <= var11; var16++) {
            ChunkAccess var17 = this.level.getChunkSource().getChunkNow(var15, var16);
            if (var17 != null) {
               for (int var18 = var7; var18 <= var10; var18++) {
                  var14 |= var17.getListenerRegistry(var18).visitInRangeListeners(param0, param1, param2, var13);
               }
            }
         }
      }

      if (!var12.isEmpty()) {
         this.handleGameEventMessagesInQueue(var12);
      }

      if (var14) {
         DebugPackets.sendGameEventInfo(this.level, param0, param1);
      }
   }

   private void handleGameEventMessagesInQueue(List<GameEvent.ListenerInfo> param0) {
      Collections.sort(param0);

      for (GameEvent.ListenerInfo var3 : param0) {
         GameEventListener var4 = var3.recipient();
         var4.handleGameEvent(this.level, var3.gameEvent(), var3.context(), var3.source());
      }
   }
}
