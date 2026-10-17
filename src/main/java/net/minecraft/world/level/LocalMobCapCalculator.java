package net.minecraft.world.level;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;

public class LocalMobCapCalculator {
   private final Long2ObjectMap<List<ServerPlayer>> playersNearChunk = new Long2ObjectOpenHashMap<>();
   private final Map<ServerPlayer, LocalMobCapCalculator.MobCounts> playerMobCounts = Maps.newHashMap();
   private final ChunkMap chunkMap;

   public LocalMobCapCalculator(ChunkMap param0) {
      this.chunkMap = param0;
   }

   private List<ServerPlayer> getPlayersNear(ChunkPos param0) {
      return this.playersNearChunk.computeIfAbsent(param0.toLong(), l_param1 -> this.chunkMap.getPlayersCloseForSpawning(param0));
   }

   public void addMob(ChunkPos param0, MobCategory param1) {
      for (ServerPlayer var4 : this.getPlayersNear(param0)) {
         this.playerMobCounts.computeIfAbsent(var4, l_param0 -> new LocalMobCapCalculator.MobCounts()).add(param1);
      }
   }

   public boolean canSpawn(MobCategory param0, ChunkPos param1) {
      for (ServerPlayer var4 : this.getPlayersNear(param1)) {
         LocalMobCapCalculator.MobCounts var5 = this.playerMobCounts.get(var4);
         if (var5 == null || var5.canSpawn(param0)) {
            return true;
         }
      }

      return false;
   }

   static class MobCounts {
      private final Object2IntMap<MobCategory> counts = new Object2IntOpenHashMap<>(MobCategory.values().length);

      public void add(MobCategory param0) {
         this.counts.computeInt(param0, (l_param0, l_param1) -> l_param1 == null ? 1 : l_param1 + 1);
      }

      public boolean canSpawn(MobCategory param0) {
         return this.counts.getOrDefault(param0, 0) < param0.getMaxInstancesPerChunk();
      }
   }
}
