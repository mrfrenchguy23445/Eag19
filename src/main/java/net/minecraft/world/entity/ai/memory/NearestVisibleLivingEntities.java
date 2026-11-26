package net.minecraft.world.entity.ai.memory;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.Sensor;

public class NearestVisibleLivingEntities {
   private static final NearestVisibleLivingEntities EMPTY = new NearestVisibleLivingEntities();
   private final List<LivingEntity> nearbyEntities;
   private final Predicate<LivingEntity> lineOfSightTest;

   private NearestVisibleLivingEntities() {
      this.nearbyEntities = List.of();
      this.lineOfSightTest = l_param0 -> false;
   }

   public NearestVisibleLivingEntities(LivingEntity param0, List<LivingEntity> param1) {
      this.nearbyEntities = param1;
      Object2BooleanOpenHashMap<LivingEntity> var3 = new Object2BooleanOpenHashMap<>(param1.size());
      Predicate<LivingEntity> var4 = l_param1 -> Sensor.isEntityTargetable(param0, l_param1);
      this.lineOfSightTest = l_param2 -> var3.computeIfAbsent(l_param2, var4);
   }

   public static NearestVisibleLivingEntities empty() {
      return EMPTY;
   }

   public Optional<LivingEntity> findClosest(Predicate<LivingEntity> param0) {
      for (LivingEntity var3 : this.nearbyEntities) {
         if (param0.test(var3) && this.lineOfSightTest.test(var3)) {
            return Optional.of(var3);
         }
      }

      return Optional.empty();
   }

   public Iterable<LivingEntity> findAll(Predicate<LivingEntity> param0) {
      return Iterables.filter(this.nearbyEntities, l_param1 -> param0.test(l_param1) && this.lineOfSightTest.test(l_param1));
   }

   public Stream<LivingEntity> find(Predicate<LivingEntity> param0) {
      return this.nearbyEntities.stream().filter(l_param1 -> param0.test(l_param1) && this.lineOfSightTest.test(l_param1));
   }

   public boolean contains(LivingEntity param0) {
      return this.nearbyEntities.contains(param0) && this.lineOfSightTest.test(param0);
   }

   public boolean contains(Predicate<LivingEntity> param0) {
      for (LivingEntity var3 : this.nearbyEntities) {
         if (param0.test(var3) && this.lineOfSightTest.test(var3)) {
            return true;
         }
      }

      return false;
   }
}
