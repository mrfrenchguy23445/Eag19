package net.minecraft.world.level.entity;

import net.minecraft.server.level.ChunkHolder;

public enum Visibility {
   HIDDEN(false, false),
   TRACKED(true, false),
   TICKING(true, true);

   private final boolean accessible;
   private final boolean ticking;

   Visibility(boolean param2, boolean param3) {
      this.accessible = param2;
      this.ticking = param3;
   }

   public boolean isTicking() {
      return this.ticking;
   }

   public boolean isAccessible() {
      return this.accessible;
   }

   public static Visibility fromFullChunkStatus(ChunkHolder.FullChunkStatus param0) {
      if (param0.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING)) {
         return TICKING;
      } else {
         return param0.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) ? TRACKED : HIDDEN;
      }
   }
}
