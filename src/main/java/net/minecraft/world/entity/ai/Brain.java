package net.minecraft.world.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class Brain<E extends LivingEntity> {
   static final Logger LOGGER = LogUtils.getLogger();
   private final Supplier<Codec<Brain<E>>> codec;
   private static final int SCHEDULE_UPDATE_DELAY = 20;
   private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = Maps.newHashMap();
   private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
   private final Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority = Maps.newTreeMap();
   private Schedule schedule = Schedule.EMPTY;
   private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements = Maps.newHashMap();
   private final Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped = Maps.newHashMap();
   private Set<Activity> coreActivities = Sets.newHashSet();
   private final Set<Activity> activeActivities = Sets.newHashSet();
   private Activity defaultActivity = Activity.IDLE;
   private long lastScheduleUpdate = -9999L;

   public static <E extends LivingEntity> Brain.Provider<E> provider(
      Collection<? extends MemoryModuleType<?>> param0, Collection<? extends SensorType<? extends Sensor<? super E>>> param1
   ) {
      return new Brain.Provider<>(param0, param1);
   }

   public static <E extends LivingEntity> Codec<Brain<E>> codec(
      final Collection<? extends MemoryModuleType<?>> param0, final Collection<? extends SensorType<? extends Sensor<? super E>>> param1
   ) {
      final MutableObject<Codec<Brain<E>>> var2 = new MutableObject<>();
      var2.setValue(
         (new MapCodec<Brain<E>>() {
               @Override
               public <T> Stream<T> keys(DynamicOps<T> l_param0) {
                  return param0.stream()
                     .flatMap(
                        l2_param0 -> l2_param0.getCodec()
                           .map(l3_param1 -> BuiltInRegistries.MEMORY_MODULE_TYPE.getKey((MemoryModuleType<?>)l2_param0))
                           .stream()
                     )
                     .map(l2_param1 -> l_param0.createString(l2_param1.toString()));
               }

               @Override
               public <T> DataResult<Brain<E>> decode(DynamicOps<T> l_param0, MapLike<T> l_param1) {
                  MutableObject<DataResult<Builder<Brain.MemoryValue<?>>>> l_var3 = new MutableObject<>(DataResult.success(ImmutableList.builder()));
                  l_param1.entries()
                     .forEach(
                        l2_param2 -> {
                           DataResult<MemoryModuleType<?>> l2_var4 = BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().parse(l_param0, l2_param2.getFirst());
                           DataResult<? extends Brain.MemoryValue<?>> l2_var5 = l2_var4.flatMap(
                              l3_param2 -> this.captureRead((MemoryModuleType<T>)l3_param2, l_param0, (T)l2_param2.getSecond())
                           );
                           l_var3.setValue(l_var3.getValue().apply2(Builder::add, l2_var5));
                        }
                     );
                  ImmutableList<Brain.MemoryValue<?>> l_var4 = l_var3.getValue()
                     .resultOrPartial(Brain.LOGGER::error)
                     .map(Builder::build)
                     .orElseGet(ImmutableList::of);
                  return DataResult.success(new Brain<>(param0, param1, l_var4, var2::getValue));
               }

               private <T, U> DataResult<Brain.MemoryValue<U>> captureRead(MemoryModuleType<U> l_param0, DynamicOps<T> l_param1, T l_param2) {
                  return l_param0.getCodec()
                     .map(DataResult::success)
                     .orElseGet(() -> DataResult.error(() -> "No codec for memory: " + l_param0))
                     .<ExpirableValue<U>>flatMap(l2_param2 -> l2_param2.parse(l_param1, l_param2))
                     .map(l2_param1 -> new Brain.MemoryValue<>(l_param0, Optional.of(l2_param1)));
               }

               public <T> RecordBuilder<T> encode(Brain<E> l_param0, DynamicOps<T> l_param1, RecordBuilder<T> l_param2) {
                  l_param0.memories().forEach(l2_param2 -> l2_param2.serialize(l_param1, l_param2));
                  return l_param2;
               }
            })
            .fieldOf("memories")
            .codec()
      );
      return var2.getValue();
   }

   public Brain(
      Collection<? extends MemoryModuleType<?>> param0,
      Collection<? extends SensorType<? extends Sensor<? super E>>> param1,
      ImmutableList<Brain.MemoryValue<?>> param2,
      Supplier<Codec<Brain<E>>> param3
   ) {
      this.codec = param3;

      for (MemoryModuleType<?> var6 : param0) {
         this.memories.put(var6, Optional.empty());
      }

      for (SensorType<? extends Sensor<? super E>> var6 : param1) {
         this.sensors.put(var6, (Sensor<? super E>)var6.create());
      }

      for (Sensor<? super E> var6 : this.sensors.values()) {
         for (MemoryModuleType<?> var8 : var6.requires()) {
            this.memories.put(var8, Optional.empty());
         }
      }

      for (Brain.MemoryValue<?> var6 : param2) {
         var6.setMemoryInternal(this);
      }
   }

   public <T> DataResult<T> serializeStart(DynamicOps<T> param0) {
      return this.codec.get().encodeStart(param0, this);
   }

   Stream<Brain.MemoryValue<?>> memories() {
      return this.memories.entrySet().stream().map(l_param0 -> Brain.MemoryValue.createUnchecked(l_param0.getKey(), l_param0.getValue()));
   }

   public boolean hasMemoryValue(MemoryModuleType<?> param0) {
      return this.checkMemory(param0, MemoryStatus.VALUE_PRESENT);
   }

   public void clearMemories() {
      this.memories.keySet().forEach(l_param0 -> this.memories.put((MemoryModuleType<?>)l_param0, Optional.empty()));
   }

   public <U> void eraseMemory(MemoryModuleType<U> param0) {
      this.setMemory(param0, Optional.empty());
   }

   public <U> void setMemory(MemoryModuleType<U> param0, @Nullable U param1) {
      this.setMemory(param0, Optional.ofNullable(param1));
   }

   public <U> void setMemoryWithExpiry(MemoryModuleType<U> param0, U param1, long param2) {
      this.setMemoryInternal(param0, Optional.of(ExpirableValue.of(param1, param2)));
   }

   public <U> void setMemory(MemoryModuleType<U> param0, Optional<? extends U> param1) {
      this.setMemoryInternal(param0, param1.map(ExpirableValue::of));
   }

   <U> void setMemoryInternal(MemoryModuleType<U> param0, Optional<? extends ExpirableValue<?>> param1) {
      if (this.memories.containsKey(param0)) {
         if (param1.isPresent() && this.isEmptyCollection(param1.get().getValue())) {
            this.eraseMemory(param0);
         } else {
            this.memories.put(param0, param1);
         }
      }
   }

   public <U> Optional<U> getMemory(MemoryModuleType<U> param0) {
      Optional<? extends ExpirableValue<?>> var2 = this.memories.get(param0);
      if (var2 == null) {
         throw new IllegalStateException("Unregistered memory fetched: " + param0);
      } else {
         return (Optional<U>)(Optional<?>)var2.map(ExpirableValue::getValue);
      }
   }

   @Nullable
   public <U> Optional<U> getMemoryInternal(MemoryModuleType<U> param0) {
      Optional<? extends ExpirableValue<?>> var2 = this.memories.get(param0);
      return var2 == null ? null : (Optional<U>)(Optional<?>)var2.map(ExpirableValue::getValue);
   }

   public <U> long getTimeUntilExpiry(MemoryModuleType<U> param0) {
      Optional<? extends ExpirableValue<?>> var2 = this.memories.get(param0);
      return var2.map(ExpirableValue::getTimeToLive).orElse(0L);
   }

   @Deprecated
   @VisibleForDebug
   public Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> getMemories() {
      return this.memories;
   }

   public <U> boolean isMemoryValue(MemoryModuleType<U> param0, U param1) {
      return !this.hasMemoryValue(param0) ? false : this.getMemory(param0).filter(l_param1 -> l_param1.equals(param1)).isPresent();
   }

   public boolean checkMemory(MemoryModuleType<?> param0, MemoryStatus param1) {
      Optional<? extends ExpirableValue<?>> var3 = this.memories.get(param0);
      return var3 == null
         ? false
         : param1 == MemoryStatus.REGISTERED
            || param1 == MemoryStatus.VALUE_PRESENT && var3.isPresent()
            || param1 == MemoryStatus.VALUE_ABSENT && !var3.isPresent();
   }

   public Schedule getSchedule() {
      return this.schedule;
   }

   public void setSchedule(Schedule param0) {
      this.schedule = param0;
   }

   public void setCoreActivities(Set<Activity> param0) {
      this.coreActivities = param0;
   }

   @Deprecated
   @VisibleForDebug
   public Set<Activity> getActiveActivities() {
      return this.activeActivities;
   }

   @Deprecated
   @VisibleForDebug
   public List<BehaviorControl<? super E>> getRunningBehaviors() {
      List<BehaviorControl<? super E>> var1 = new ObjectArrayList<>();

      for (Map<Activity, Set<BehaviorControl<? super E>>> var3 : this.availableBehaviorsByPriority.values()) {
         for (Set<BehaviorControl<? super E>> var5 : var3.values()) {
            for (BehaviorControl<? super E> var7 : var5) {
               if (var7.getStatus() == Behavior.Status.RUNNING) {
                  var1.add(var7);
               }
            }
         }
      }

      return var1;
   }

   public void useDefaultActivity() {
      this.setActiveActivity(this.defaultActivity);
   }

   public Optional<Activity> getActiveNonCoreActivity() {
      for (Activity var2 : this.activeActivities) {
         if (!this.coreActivities.contains(var2)) {
            return Optional.of(var2);
         }
      }

      return Optional.empty();
   }

   public void setActiveActivityIfPossible(Activity param0) {
      if (this.activityRequirementsAreMet(param0)) {
         this.setActiveActivity(param0);
      } else {
         this.useDefaultActivity();
      }
   }

   private void setActiveActivity(Activity param0) {
      if (!this.isActive(param0)) {
         this.eraseMemoriesForOtherActivitesThan(param0);
         this.activeActivities.clear();
         this.activeActivities.addAll(this.coreActivities);
         this.activeActivities.add(param0);
      }
   }

   private void eraseMemoriesForOtherActivitesThan(Activity param0) {
      for (Activity var3 : this.activeActivities) {
         if (var3 != param0) {
            Set<MemoryModuleType<?>> var4 = this.activityMemoriesToEraseWhenStopped.get(var3);
            if (var4 != null) {
               for (MemoryModuleType<?> var6 : var4) {
                  this.eraseMemory(var6);
               }
            }
         }
      }
   }

   public void updateActivityFromSchedule(long param0, long param2) {
      if (param2 - this.lastScheduleUpdate > 20L) {
         this.lastScheduleUpdate = param2;
         Activity var5 = this.getSchedule().getActivityAt((int)(param0 % 24000L));
         if (!this.activeActivities.contains(var5)) {
            this.setActiveActivityIfPossible(var5);
         }
      }
   }

   public void setActiveActivityToFirstValid(List<Activity> param0) {
      for (Activity var3 : param0) {
         if (this.activityRequirementsAreMet(var3)) {
            this.setActiveActivity(var3);
            break;
         }
      }
   }

   public void setDefaultActivity(Activity param0) {
      this.defaultActivity = param0;
   }

   public void addActivity(Activity param0, int param1, ImmutableList<? extends BehaviorControl<? super E>> param2) {
      this.addActivity(param0, this.createPriorityPairs(param1, param2));
   }

   public void addActivityAndRemoveMemoryWhenStopped(
      Activity param0, int param1, ImmutableList<? extends BehaviorControl<? super E>> param2, MemoryModuleType<?> param3
   ) {
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> var5 = ImmutableSet.of(Pair.of(param3, MemoryStatus.VALUE_PRESENT));
      Set<MemoryModuleType<?>> var6 = ImmutableSet.of(param3);
      this.addActivityAndRemoveMemoriesWhenStopped(param0, this.createPriorityPairs(param1, param2), var5, var6);
   }

   public void addActivity(Activity param0, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> param1) {
      this.addActivityAndRemoveMemoriesWhenStopped(param0, param1, ImmutableSet.of(), Sets.newHashSet());
   }

   public void addActivityWithConditions(
      Activity param0, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> param1, Set<Pair<MemoryModuleType<?>, MemoryStatus>> param2
   ) {
      this.addActivityAndRemoveMemoriesWhenStopped(param0, param1, param2, Sets.newHashSet());
   }

   public void addActivityAndRemoveMemoriesWhenStopped(
      Activity param0,
      ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> param1,
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> param2,
      Set<MemoryModuleType<?>> param3
   ) {
      this.activityRequirements.put(param0, param2);
      if (!param3.isEmpty()) {
         this.activityMemoriesToEraseWhenStopped.put(param0, param3);
      }

      for (Pair<Integer, ? extends BehaviorControl<? super E>> var6 : param1) {
         this.availableBehaviorsByPriority
            .computeIfAbsent(var6.getFirst(), l_param0 -> Maps.newHashMap())
            .computeIfAbsent(param0, l_param0 -> Sets.newLinkedHashSet())
            .add((BehaviorControl<? super E>)var6.getSecond());
      }
   }

   @VisibleForTesting
   public void removeAllBehaviors() {
      this.availableBehaviorsByPriority.clear();
   }

   public boolean isActive(Activity param0) {
      return this.activeActivities.contains(param0);
   }

   public Brain<E> copyWithoutBehaviors() {
      Brain<E> var1 = new Brain<>(this.memories.keySet(), this.sensors.keySet(), ImmutableList.of(), this.codec);

      for (Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> var3 : this.memories.entrySet()) {
         MemoryModuleType<?> var4 = var3.getKey();
         if (var3.getValue().isPresent()) {
            var1.memories.put(var4, var3.getValue());
         }
      }

      return var1;
   }

   public void tick(ServerLevel param0, E param1) {
      this.forgetOutdatedMemories();
      this.tickSensors(param0, param1);
      this.startEachNonRunningBehavior(param0, param1);
      this.tickEachRunningBehavior(param0, param1);
   }

   private void tickSensors(ServerLevel param0, E param1) {
      for (Sensor<? super E> var4 : this.sensors.values()) {
         var4.tick(param0, param1);
      }
   }

   private void forgetOutdatedMemories() {
      for (Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> var2 : this.memories.entrySet()) {
         if (var2.getValue().isPresent()) {
            ExpirableValue<?> var3 = (ExpirableValue<?>)var2.getValue().get();
            if (var3.hasExpired()) {
               this.eraseMemory(var2.getKey());
            }

            var3.tick();
         }
      }
   }

   public void stopAll(ServerLevel param0, E param1) {
      long var3 = param1.level.getGameTime();

      for (BehaviorControl<? super E> var6 : this.getRunningBehaviors()) {
         var6.doStop(param0, param1, var3);
      }
   }

   private void startEachNonRunningBehavior(ServerLevel param0, E param1) {
      long var3 = param0.getGameTime();

      for (Map<Activity, Set<BehaviorControl<? super E>>> var6 : this.availableBehaviorsByPriority.values()) {
         for (Entry<Activity, Set<BehaviorControl<? super E>>> var8 : var6.entrySet()) {
            Activity var9 = var8.getKey();
            if (this.activeActivities.contains(var9)) {
               for (BehaviorControl<? super E> var12 : var8.getValue()) {
                  if (var12.getStatus() == Behavior.Status.STOPPED) {
                     var12.tryStart(param0, param1, var3);
                  }
               }
            }
         }
      }
   }

   private void tickEachRunningBehavior(ServerLevel param0, E param1) {
      long var3 = param0.getGameTime();

      for (BehaviorControl<? super E> var6 : this.getRunningBehaviors()) {
         var6.tickOrStop(param0, param1, var3);
      }
   }

   private boolean activityRequirementsAreMet(Activity param0) {
      if (!this.activityRequirements.containsKey(param0)) {
         return false;
      }

      for (Pair<MemoryModuleType<?>, MemoryStatus> var3 : this.activityRequirements.get(param0)) {
         MemoryModuleType<?> var4 = var3.getFirst();
         MemoryStatus var5 = var3.getSecond();
         if (!this.checkMemory(var4, var5)) {
            return false;
         }
      }

      return true;
   }

   private boolean isEmptyCollection(Object param0) {
      return param0 instanceof Collection && ((Collection)param0).isEmpty();
   }

   ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> createPriorityPairs(
      int param0, ImmutableList<? extends BehaviorControl<? super E>> param1
   ) {
      int var3 = param0;
      Builder<Pair<Integer, ? extends BehaviorControl<? super E>>> var4 = ImmutableList.builder();

      for (BehaviorControl<? super E> var6 : param1) {
         var4.add(Pair.of(var3++, var6));
      }

      return var4.build();
   }

   static final class MemoryValue<U> {
      private final MemoryModuleType<U> type;
      private final Optional<? extends ExpirableValue<U>> value;

      static <U> Brain.MemoryValue<U> createUnchecked(MemoryModuleType<U> param0, Optional<? extends ExpirableValue<?>> param1) {
         return new Brain.MemoryValue<>(param0, (Optional<? extends ExpirableValue<U>>)param1);
      }

      MemoryValue(MemoryModuleType<U> param0, Optional<? extends ExpirableValue<U>> param1) {
         this.type = param0;
         this.value = param1;
      }

      void setMemoryInternal(Brain<?> param0) {
         param0.setMemoryInternal(this.type, this.value);
      }

      public <T> void serialize(DynamicOps<T> param0, RecordBuilder<T> param1) {
         this.type
            .getCodec()
            .ifPresent(
               l_param2 -> this.value
                  .ifPresent(
                     l2_param3 -> param1.add(
                        BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().encodeStart(param0, this.type),
                        l_param2.encodeStart(param0, (ExpirableValue<U>)l2_param3)
                     )
                  )
            );
      }
   }

   public static final class Provider<E extends LivingEntity> {
      private final Collection<? extends MemoryModuleType<?>> memoryTypes;
      private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes;
      private final Codec<Brain<E>> codec;

      Provider(Collection<? extends MemoryModuleType<?>> param0, Collection<? extends SensorType<? extends Sensor<? super E>>> param1) {
         this.memoryTypes = param0;
         this.sensorTypes = param1;
         this.codec = Brain.codec(param0, param1);
      }

      public Brain<E> makeBrain(Dynamic<?> param0) {
         return this.codec
            .parse(param0)
            .resultOrPartial(Brain.LOGGER::error)
            .orElseGet(() -> new Brain<>(this.memoryTypes, this.sensorTypes, ImmutableList.of(), () -> this.codec));
      }
   }
}
