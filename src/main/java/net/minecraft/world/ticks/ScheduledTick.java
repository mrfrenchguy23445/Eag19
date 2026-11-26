package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

public record ScheduledTick<T>(T type, BlockPos pos, long triggerTick, TickPriority priority, long subTickOrder) {
   public static final Comparator<ScheduledTick<?>> DRAIN_ORDER = (param0, param1) -> {
      int var2 = Long.compare(param0.triggerTick, param1.triggerTick);
      if (var2 != 0) {
         return var2;
      }

      var2 = param0.priority.compareTo(param1.priority);
      return var2 != 0 ? var2 : Long.compare(param0.subTickOrder, param1.subTickOrder);
   };
   public static final Comparator<ScheduledTick<?>> INTRA_TICK_DRAIN_ORDER = (param0, param1) -> {
      int var2 = param0.priority.compareTo(param1.priority);
      return var2 != 0 ? var2 : Long.compare(param0.subTickOrder, param1.subTickOrder);
   };
   public static final Strategy<ScheduledTick<?>> UNIQUE_TICK_HASH = new Strategy<ScheduledTick<?>>() {
      public int hashCode(ScheduledTick<?> param0) {
         return 31 * param0.pos().hashCode() + param0.type().hashCode();
      }

      public boolean equals(@Nullable ScheduledTick<?> param0, @Nullable ScheduledTick<?> param1) {
         if (param0 == param1) {
            return true;
         } else {
            return param0 != null && param1 != null ? param0.type() == param1.type() && param0.pos().equals(param1.pos()) : false;
         }
      }
   };

   public ScheduledTick(T param0, BlockPos param1, long param2, long param4) {
      this(param0, param1, param2, TickPriority.NORMAL, param4);
   }

   public ScheduledTick {
      pos = pos.immutable();
   }

   public static <T> ScheduledTick<T> probe(T param0, BlockPos param1) {
      return new ScheduledTick<>(param0, param1, 0L, TickPriority.NORMAL, 0L);
   }
}
