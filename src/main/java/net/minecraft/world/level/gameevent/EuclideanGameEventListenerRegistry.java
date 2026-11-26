package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class EuclideanGameEventListenerRegistry implements GameEventListenerRegistry {
   private final List<GameEventListener> listeners = Lists.newArrayList();
   private final Set<GameEventListener> listenersToRemove = Sets.newHashSet();
   private final List<GameEventListener> listenersToAdd = Lists.newArrayList();
   private boolean processing;
   private final ServerLevel level;

   public EuclideanGameEventListenerRegistry(ServerLevel param0) {
      this.level = param0;
   }

   @Override
   public boolean isEmpty() {
      return this.listeners.isEmpty();
   }

   @Override
   public void register(GameEventListener param0) {
      if (this.processing) {
         this.listenersToAdd.add(param0);
      } else {
         this.listeners.add(param0);
      }

      DebugPackets.sendGameEventListenerInfo(this.level, param0);
   }

   @Override
   public void unregister(GameEventListener param0) {
      if (this.processing) {
         this.listenersToRemove.add(param0);
      } else {
         this.listeners.remove(param0);
      }
   }

   @Override
   public boolean visitInRangeListeners(GameEvent param0, Vec3 param1, GameEvent.Context param2, GameEventListenerRegistry.ListenerVisitor param3) {
      this.processing = true;
      boolean var5 = false;

      try {
         Iterator<GameEventListener> var6 = this.listeners.iterator();

         while (var6.hasNext()) {
            GameEventListener var7 = var6.next();
            if (this.listenersToRemove.remove(var7)) {
               var6.remove();
            } else {
               Optional<Vec3> var8 = getPostableListenerPosition(this.level, param1, var7);
               if (var8.isPresent()) {
                  param3.visit(var7, var8.get());
                  var5 = true;
               }
            }
         }
      } finally {
         this.processing = false;
      }

      if (!this.listenersToAdd.isEmpty()) {
         this.listeners.addAll(this.listenersToAdd);
         this.listenersToAdd.clear();
      }

      if (!this.listenersToRemove.isEmpty()) {
         this.listeners.removeAll(this.listenersToRemove);
         this.listenersToRemove.clear();
      }

      return var5;
   }

   private static Optional<Vec3> getPostableListenerPosition(ServerLevel param0, Vec3 param1, GameEventListener param2) {
      Optional<Vec3> var3 = param2.getListenerSource().getPosition(param0);
      if (var3.isEmpty()) {
         return Optional.empty();
      }

      double var4 = var3.get().distanceToSqr(param1);
      int var6 = param2.getListenerRadius() * param2.getListenerRadius();
      return var4 > var6 ? Optional.empty() : var3;
   }
}
