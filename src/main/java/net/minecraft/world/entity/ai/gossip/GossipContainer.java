package net.minecraft.world.entity.ai.gossip;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import org.slf4j.Logger;

public class GossipContainer {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DISCARD_THRESHOLD = 2;
   private final Map<UUID, GossipContainer.EntityGossips> gossips = Maps.newHashMap();

   @VisibleForDebug
   public Map<UUID, Object2IntMap<GossipType>> getGossipEntries() {
      Map<UUID, Object2IntMap<GossipType>> var1 = Maps.newHashMap();
      this.gossips.keySet().forEach(l_param1 -> {
         GossipContainer.EntityGossips l_var3 = this.gossips.get(l_param1);
         var1.put(l_param1, l_var3.entries);
      });
      return var1;
   }

   public void decay() {
      Iterator<GossipContainer.EntityGossips> var1 = this.gossips.values().iterator();

      while (var1.hasNext()) {
         GossipContainer.EntityGossips var2 = var1.next();
         var2.decay();
         if (var2.isEmpty()) {
            var1.remove();
         }
      }
   }

   private Stream<GossipContainer.GossipEntry> unpack() {
      return this.gossips.entrySet().stream().flatMap(l_param0 -> l_param0.getValue().unpack(l_param0.getKey()));
   }

   private Collection<GossipContainer.GossipEntry> selectGossipsForTransfer(RandomSource param0, int param1) {
      List<GossipContainer.GossipEntry> var3 = this.unpack().toList();
      if (var3.isEmpty()) {
         return Collections.emptyList();
      }

      int[] var4 = new int[var3.size()];
      int var5 = 0;

      for (int var6 = 0; var6 < var3.size(); var6++) {
         GossipContainer.GossipEntry var7 = var3.get(var6);
         var5 += Math.abs(var7.weightedValue());
         var4[var6] = var5 - 1;
      }

      Set<GossipContainer.GossipEntry> var6 = Sets.newIdentityHashSet();

      for (int var7 = 0; var7 < param1; var7++) {
         int var8 = param0.nextInt(var5);
         int var9 = Arrays.binarySearch(var4, var8);
         var6.add(var3.get(var9 < 0 ? -var9 - 1 : var9));
      }

      return var6;
   }

   private GossipContainer.EntityGossips getOrCreate(UUID param0) {
      return this.gossips.computeIfAbsent(param0, l_param0 -> new GossipContainer.EntityGossips());
   }

   public void transferFrom(GossipContainer param0, RandomSource param1, int param2) {
      Collection<GossipContainer.GossipEntry> var4 = param0.selectGossipsForTransfer(param1, param2);
      var4.forEach(l_param0 -> {
         int l_var2 = l_param0.value - l_param0.type.decayPerTransfer;
         if (l_var2 >= 2) {
            this.getOrCreate(l_param0.target).entries.mergeInt(l_param0.type, l_var2, GossipContainer::mergeValuesForTransfer);
         }
      });
   }

   public int getReputation(UUID param0, Predicate<GossipType> param1) {
      GossipContainer.EntityGossips var3 = this.gossips.get(param0);
      return var3 != null ? var3.weightedValue(param1) : 0;
   }

   public long getCountForType(GossipType param0, DoublePredicate param1) {
      return this.gossips.values().stream().filter(l_param2 -> param1.test(l_param2.entries.getOrDefault(param0, 0) * param0.weight)).count();
   }

   public void add(UUID param0, GossipType param1, int param2) {
      GossipContainer.EntityGossips var4 = this.getOrCreate(param0);
      var4.entries.mergeInt(param1, param2, (l_param1, l_param2) -> this.mergeValuesForAddition(param1, l_param1, l_param2));
      var4.makeSureValueIsntTooLowOrTooHigh(param1);
      if (var4.isEmpty()) {
         this.gossips.remove(param0);
      }
   }

   public void remove(UUID param0, GossipType param1, int param2) {
      this.add(param0, param1, -param2);
   }

   public void remove(UUID param0, GossipType param1) {
      GossipContainer.EntityGossips var3 = this.gossips.get(param0);
      if (var3 != null) {
         var3.remove(param1);
         if (var3.isEmpty()) {
            this.gossips.remove(param0);
         }
      }
   }

   public void remove(GossipType param0) {
      Iterator<GossipContainer.EntityGossips> var2 = this.gossips.values().iterator();

      while (var2.hasNext()) {
         GossipContainer.EntityGossips var3 = var2.next();
         var3.remove(param0);
         if (var3.isEmpty()) {
            var2.remove();
         }
      }
   }

   public <T> T store(DynamicOps<T> param0) {
      return GossipContainer.GossipEntry.LIST_CODEC
         .encodeStart(param0, this.unpack().toList())
         .resultOrPartial(l_param0 -> LOGGER.warn("Failed to serialize gossips: {}", l_param0))
         .orElseGet(param0::emptyList);
   }

   public void update(Dynamic<?> param0) {
      GossipContainer.GossipEntry.LIST_CODEC
         .decode(param0)
         .resultOrPartial(l_param0 -> LOGGER.warn("Failed to deserialize gossips: {}", l_param0))
         .stream()
         .flatMap(l_param0 -> l_param0.getFirst().stream())
         .forEach(l_param0 -> this.getOrCreate(l_param0.target).entries.put(l_param0.type, l_param0.value));
   }

   private static int mergeValuesForTransfer(int param0, int param1) {
      return Math.max(param0, param1);
   }

   private int mergeValuesForAddition(GossipType param0, int param1, int param2) {
      int var4 = param1 + param2;
      return var4 > param0.max ? Math.max(param0.max, param1) : var4;
   }

   static class EntityGossips {
      final Object2IntMap<GossipType> entries = new Object2IntOpenHashMap<>();

      public int weightedValue(Predicate<GossipType> param0) {
         return this.entries
            .object2IntEntrySet()
            .stream()
            .filter(l_param1 -> param0.test(l_param1.getKey()))
            .mapToInt(l_param0 -> l_param0.getIntValue() * l_param0.getKey().weight)
            .sum();
      }

      public Stream<GossipContainer.GossipEntry> unpack(UUID param0) {
         return this.entries.object2IntEntrySet().stream().map(l_param1 -> new GossipContainer.GossipEntry(param0, l_param1.getKey(), l_param1.getIntValue()));
      }

      public void decay() {
         ObjectIterator<Entry<GossipType>> var1 = this.entries.object2IntEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<GossipType> var2 = var1.next();
            int var3 = var2.getIntValue() - var2.getKey().decayPerDay;
            if (var3 < 2) {
               var1.remove();
            } else {
               var2.setValue(var3);
            }
         }
      }

      public boolean isEmpty() {
         return this.entries.isEmpty();
      }

      public void makeSureValueIsntTooLowOrTooHigh(GossipType param0) {
         int var2 = this.entries.getInt(param0);
         if (var2 > param0.max) {
            this.entries.put(param0, param0.max);
         }

         if (var2 < 2) {
            this.remove(param0);
         }
      }

      public void remove(GossipType param0) {
         this.entries.removeInt(param0);
      }
   }

   record GossipEntry(UUID target, GossipType type, int value) {
      public static final Codec<GossipContainer.GossipEntry> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               UUIDUtil.CODEC.fieldOf("Target").forGetter(GossipContainer.GossipEntry::target),
               GossipType.CODEC.fieldOf("Type").forGetter(GossipContainer.GossipEntry::type),
               ExtraCodecs.POSITIVE_INT.fieldOf("Value").forGetter(GossipContainer.GossipEntry::value)
            )
            .apply(param0, GossipContainer.GossipEntry::new)
      );
      public static final Codec<List<GossipContainer.GossipEntry>> LIST_CODEC = CODEC.listOf();

      public int weightedValue() {
         return this.value * this.type.weight;
      }
   }
}
