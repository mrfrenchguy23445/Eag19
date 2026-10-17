package net.minecraft.world.entity.monster.warden;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class AngerManagement {
   @VisibleForTesting
   protected static final int CONVERSION_DELAY = 2;
   @VisibleForTesting
   protected static final int MAX_ANGER = 150;
   private static final int DEFAULT_ANGER_DECREASE = 1;
   private int conversionDelay = Mth.randomBetweenInclusive(RandomSource.create(), 0, 2);
   int highestAnger;
   private static final Codec<Pair<UUID, Integer>> SUSPECT_ANGER_PAIR = RecordCodecBuilder.create(
      param0 -> param0.group(UUIDUtil.CODEC.fieldOf("uuid").forGetter(Pair::getFirst), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anger").forGetter(Pair::getSecond))
         .apply(param0, Pair::of)
   );
   private final Predicate<Entity> filter;
   @VisibleForTesting
   protected final ArrayList<Entity> suspects;
   private final AngerManagement.Sorter suspectSorter;
   @VisibleForTesting
   protected final Object2IntMap<Entity> angerBySuspect;
   @VisibleForTesting
   protected final Object2IntMap<UUID> angerByUuid;

   public static Codec<AngerManagement> codec(Predicate<Entity> param0) {
      return RecordCodecBuilder.create(
         l_param1 -> l_param1.group(
               SUSPECT_ANGER_PAIR.listOf().fieldOf("suspects").orElse(Collections.emptyList()).forGetter(AngerManagement::createUuidAngerPairs)
            )
            .apply(l_param1, l2_param1 -> new AngerManagement(param0, l2_param1))
      );
   }

   public AngerManagement(Predicate<Entity> param0, List<Pair<UUID, Integer>> param1) {
      this.filter = param0;
      this.suspects = new ArrayList<>();
      this.suspectSorter = new AngerManagement.Sorter(this);
      this.angerBySuspect = new Object2IntOpenHashMap<>();
      this.angerByUuid = new Object2IntOpenHashMap<>(param1.size());
      param1.forEach(l_param0 -> this.angerByUuid.put(l_param0.getFirst(), l_param0.getSecond()));
   }

   private List<Pair<UUID, Integer>> createUuidAngerPairs() {
      return Streams.<Pair<UUID, Integer>>concat(
            this.suspects.stream().map(l_param0 -> Pair.of(l_param0.getUUID(), this.angerBySuspect.getInt(l_param0))),
            this.angerByUuid.object2IntEntrySet().stream().map(l_param0 -> Pair.of(l_param0.getKey(), l_param0.getIntValue()))
         )
         .collect(Collectors.toList());
   }

   public void tick(ServerLevel param0, Predicate<Entity> param1) {
      this.conversionDelay--;
      if (this.conversionDelay <= 0) {
         this.convertFromUuids(param0);
         this.conversionDelay = 2;
      }

      ObjectIterator<Entry<UUID>> var3 = this.angerByUuid.object2IntEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<UUID> var4 = var3.next();
         int var5 = var4.getIntValue();
         if (var5 <= 1) {
            var3.remove();
         } else {
            var4.setValue(var5 - 1);
         }
      }

      ObjectIterator<Entry<Entity>> var4 = this.angerBySuspect.object2IntEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<Entity> var5 = var4.next();
         int var6 = var5.getIntValue();
         Entity var7 = var5.getKey();
         Entity.RemovalReason var8 = var7.getRemovalReason();
         if (var6 > 1 && param1.test(var7) && var8 == null) {
            var5.setValue(var6 - 1);
         } else {
            this.suspects.remove(var7);
            var4.remove();
            if (var6 > 1 && var8 != null) {
               switch (var8) {
                  case CHANGED_DIMENSION:
                  case UNLOADED_TO_CHUNK:
                  case UNLOADED_WITH_PLAYER:
                     this.angerByUuid.put(var7.getUUID(), var6 - 1);
               }
            }
         }
      }

      this.sortAndUpdateHighestAnger();
   }

   private void sortAndUpdateHighestAnger() {
      this.highestAnger = 0;
      this.suspects.sort(this.suspectSorter);
      if (this.suspects.size() == 1) {
         this.highestAnger = this.angerBySuspect.getInt(this.suspects.get(0));
      }
   }

   private void convertFromUuids(ServerLevel param0) {
      ObjectIterator<Entry<UUID>> var2 = this.angerByUuid.object2IntEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<UUID> var3 = var2.next();
         int var4 = var3.getIntValue();
         Entity var5 = param0.getEntity(var3.getKey());
         if (var5 != null) {
            this.angerBySuspect.put(var5, var4);
            this.suspects.add(var5);
            var2.remove();
         }
      }
   }

   public int increaseAnger(Entity param0, int param1) {
      boolean var3 = !this.angerBySuspect.containsKey(param0);
      int var4 = this.angerBySuspect.computeInt(param0, (l_param1, l_param2) -> Math.min(150, (l_param2 == null ? 0 : l_param2) + param1));
      if (var3) {
         int var5 = this.angerByUuid.removeInt(param0.getUUID());
         var4 += var5;
         this.angerBySuspect.put(param0, var4);
         this.suspects.add(param0);
      }

      this.sortAndUpdateHighestAnger();
      return var4;
   }

   public void clearAnger(Entity param0) {
      this.angerBySuspect.removeInt(param0);
      this.suspects.remove(param0);
      this.sortAndUpdateHighestAnger();
   }

   @Nullable
   private Entity getTopSuspect() {
      return this.suspects.stream().filter(this.filter).findFirst().orElse(null);
   }

   public int getActiveAnger(@Nullable Entity param0) {
      return param0 == null ? this.highestAnger : this.angerBySuspect.getInt(param0);
   }

   public Optional<LivingEntity> getActiveEntity() {
      return Optional.ofNullable(this.getTopSuspect()).filter(l_param0 -> l_param0 instanceof LivingEntity).map(l_param0 -> (LivingEntity)l_param0);
   }

   @VisibleForTesting
   protected record Sorter(AngerManagement angerManagement) implements Comparator<Entity> {
      public int compare(Entity param0, Entity param1) {
         if (param0.equals(param1)) {
            return 0;
         } else {
            int var3 = this.angerManagement.angerBySuspect.getOrDefault(param0, 0);
            int var4 = this.angerManagement.angerBySuspect.getOrDefault(param1, 0);
            this.angerManagement.highestAnger = Math.max(this.angerManagement.highestAnger, Math.max(var3, var4));
            boolean var5 = AngerLevel.byAnger(var3).isAngry();
            boolean var6 = AngerLevel.byAnger(var4).isAngry();
            if (var5 != var6) {
               return var5 ? -1 : 1;
            } else {
               boolean var7 = param0 instanceof Player;
               boolean var8 = param1 instanceof Player;
               if (var7 != var8) {
                  return var7 ? -1 : 1;
               } else {
                  return Integer.compare(var4, var3);
               }
            }
         }
      }
   }
}
