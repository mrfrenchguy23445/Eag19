package net.minecraft.world.level.timers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

public class TimerQueue<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String CALLBACK_DATA_TAG = "Callback";
   private static final String TIMER_NAME_TAG = "Name";
   private static final String TIMER_TRIGGER_TIME_TAG = "TriggerTime";
   private final TimerCallbacks<T> callbacksRegistry;
   private final Queue<TimerQueue.Event<T>> queue = new PriorityQueue<>(createComparator());
   private UnsignedLong sequentialId = UnsignedLong.ZERO;
   private final Table<String, Long, TimerQueue.Event<T>> events = HashBasedTable.create();

   private static <T> Comparator<TimerQueue.Event<T>> createComparator() {
      return Comparator.<TimerQueue.Event<T>>comparingLong(l_param0 -> l_param0.triggerTime).thenComparing(l_param0 -> l_param0.sequentialId);
   }

   public TimerQueue(TimerCallbacks<T> param0, Stream<? extends Dynamic<?>> param1) {
      this(param0);
      this.queue.clear();
      this.events.clear();
      this.sequentialId = UnsignedLong.ZERO;
      param1.forEach(l_param0 -> {
         Tag l_var2 = l_param0.convert(NbtOps.INSTANCE).getValue();
         if (l_var2 instanceof CompoundTag l_var3) {
            this.loadEvent(l_var3);
         } else {
            LOGGER.warn("Invalid format of events: {}", l_var2);
         }
      });
   }

   public TimerQueue(TimerCallbacks<T> param0) {
      this.callbacksRegistry = param0;
   }

   public void tick(T param0, long param1) {
      while (true) {
         TimerQueue.Event<T> var4 = this.queue.peek();
         if (var4 == null || var4.triggerTime > param1) {
            return;
         }

         this.queue.remove();
         this.events.remove(var4.id, param1);
         var4.callback.handle(param0, this, param1);
      }
   }

   public void schedule(String param0, long param1, TimerCallback<T> param3) {
      if (!this.events.contains(param0, param1)) {
         this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
         TimerQueue.Event<T> var5 = new TimerQueue.Event<>(param1, this.sequentialId, param0, param3);
         this.events.put(param0, param1, var5);
         this.queue.add(var5);
      }
   }

   public int remove(String param0) {
      Collection<TimerQueue.Event<T>> var2 = this.events.row(param0).values();
      var2.forEach(this.queue::remove);
      int var3 = var2.size();
      var2.clear();
      return var3;
   }

   public Set<String> getEventsIds() {
      return Collections.unmodifiableSet(this.events.rowKeySet());
   }

   private void loadEvent(CompoundTag param0) {
      CompoundTag var2 = param0.getCompound("Callback");
      TimerCallback<T> var3 = this.callbacksRegistry.deserialize(var2);
      if (var3 != null) {
         String var4 = param0.getString("Name");
         long var5 = param0.getLong("TriggerTime");
         this.schedule(var4, var5, var3);
      }
   }

   private CompoundTag storeEvent(TimerQueue.Event<T> param0) {
      CompoundTag var2 = new CompoundTag();
      var2.putString("Name", param0.id);
      var2.putLong("TriggerTime", param0.triggerTime);
      var2.put("Callback", this.callbacksRegistry.serialize(param0.callback));
      return var2;
   }

   public ListTag store() {
      ListTag var1 = new ListTag();
      this.queue.stream().sorted(createComparator()).map(this::storeEvent).forEach(var1::add);
      return var1;
   }

   public static class Event<T> {
      public final long triggerTime;
      public final UnsignedLong sequentialId;
      public final String id;
      public final TimerCallback<T> callback;

      Event(long param0, UnsignedLong param2, String param3, TimerCallback<T> param4) {
         this.triggerTime = param0;
         this.sequentialId = param2;
         this.id = param3;
         this.callback = param4;
      }
   }
}
