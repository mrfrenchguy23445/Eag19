package net.minecraft.world.level.gameevent;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class DynamicGameEventListener<T extends GameEventListener> {
   private T listener;
   @Nullable
   private SectionPos lastSection;

   public DynamicGameEventListener(T param0) {
      this.listener = param0;
   }

   public void add(ServerLevel param0) {
      this.move(param0);
   }

   public void updateListener(T param0, @Nullable Level param1) {
      T var3 = this.listener;
      if (var3 != param0) {
         if (param1 instanceof ServerLevel var4) {
            ifChunkExists(var4, this.lastSection, l_param1 -> l_param1.unregister(var3));
            ifChunkExists(var4, this.lastSection, l_param1 -> l_param1.register(param0));
         }

         this.listener = param0;
      }
   }

   public T getListener() {
      return this.listener;
   }

   public void remove(ServerLevel param0) {
      ifChunkExists(param0, this.lastSection, l_param0 -> l_param0.unregister(this.listener));
   }

   public void move(ServerLevel param0) {
      this.listener.getListenerSource().getPosition(param0).map(SectionPos::of).ifPresent(l_param1 -> {
         if (this.lastSection == null || !this.lastSection.equals(l_param1)) {
            ifChunkExists(param0, this.lastSection, l2_param0 -> l2_param0.unregister(this.listener));
            this.lastSection = l_param1;
            ifChunkExists(param0, this.lastSection, l2_param0 -> l2_param0.register(this.listener));
         }
      });
   }

   private static void ifChunkExists(LevelReader param0, @Nullable SectionPos param1, Consumer<GameEventListenerRegistry> param2) {
      if (param1 != null) {
         ChunkAccess var3 = param0.getChunk(param1.x(), param1.z(), ChunkStatus.FULL, false);
         if (var3 != null) {
            param2.accept(var3.getListenerRegistry(param1.y()));
         }
      }
   }
}
