package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DebugPackets {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void sendGameTestAddMarker(ServerLevel param0, BlockPos param1, String param2, int param3, int param4) {
      FriendlyByteBuf var5 = new FriendlyByteBuf(Unpooled.buffer());
      var5.writeBlockPos(param1);
      var5.writeInt(param3);
      var5.writeUtf(param2);
      var5.writeInt(param4);
      sendPacketToAllPlayers(param0, var5, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER);
   }

   public static void sendGameTestClearPacket(ServerLevel param0) {
      FriendlyByteBuf var1 = new FriendlyByteBuf(Unpooled.buffer());
      sendPacketToAllPlayers(param0, var1, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR);
   }

   public static void sendPoiPacketsForChunk(ServerLevel param0, ChunkPos param1) {
   }

   public static void sendPoiAddedPacket(ServerLevel param0, BlockPos param1) {
      sendVillageSectionsPacket(param0, param1);
   }

   public static void sendPoiRemovedPacket(ServerLevel param0, BlockPos param1) {
      sendVillageSectionsPacket(param0, param1);
   }

   public static void sendPoiTicketCountPacket(ServerLevel param0, BlockPos param1) {
      sendVillageSectionsPacket(param0, param1);
   }

   private static void sendVillageSectionsPacket(ServerLevel param0, BlockPos param1) {
   }

   public static void sendPathFindingPacket(Level param0, Mob param1, @Nullable Path param2, float param3) {
   }

   public static void sendNeighborsUpdatePacket(Level param0, BlockPos param1) {
   }

   public static void sendStructurePacket(WorldGenLevel param0, StructureStart param1) {
   }

   public static void sendGoalSelector(Level param0, Mob param1, GoalSelector param2) {
      if (param0 instanceof ServerLevel) {
         ;
      }
   }

   public static void sendRaids(ServerLevel param0, Collection<Raid> param1) {
   }

   public static void sendEntityBrain(LivingEntity param0) {
   }

   public static void sendBeeInfo(Bee param0) {
   }

   public static void sendGameEventInfo(Level param0, GameEvent param1, Vec3 param2) {
   }

   public static void sendGameEventListenerInfo(Level param0, GameEventListener param1) {
   }

   public static void sendHiveInfo(Level param0, BlockPos param1, BlockState param2, BeehiveBlockEntity param3) {
   }

   private static void writeBrain(LivingEntity param0, FriendlyByteBuf param1) {
      Brain<?> var2 = param0.getBrain();
      long var3 = param0.level.getGameTime();
      if (param0 instanceof InventoryCarrier) {
         Container var5 = ((InventoryCarrier)param0).getInventory();
         param1.writeUtf(var5.isEmpty() ? "" : var5.toString());
      } else {
         param1.writeUtf("");
      }

      param1.writeOptional(
         var2.hasMemoryValue(MemoryModuleType.PATH) ? var2.getMemory(MemoryModuleType.PATH) : Optional.empty(),
         (l_param0, l_param1) -> l_param1.writeToStream(l_param0)
      );
      if (param0 instanceof Villager var5) {
         boolean var6 = var5.wantsToSpawnGolem(var3);
         param1.writeBoolean(var6);
      } else {
         param1.writeBoolean(false);
      }

      if (param0.getType() == EntityType.WARDEN) {
         Warden var5 = (Warden)param0;
         param1.writeInt(var5.getClientAngerLevel());
      } else {
         param1.writeInt(-1);
      }

      param1.writeCollection(var2.getActiveActivities(), (l_param0, l_param1) -> l_param0.writeUtf(l_param1.getName()));
      Set<String> var5 = var2.getRunningBehaviors().stream().map(BehaviorControl::debugString).collect(Collectors.toSet());
      param1.writeCollection(var5, FriendlyByteBuf::writeUtf);
      param1.writeCollection(getMemoryDescriptions(param0, var3), (l_param0, l_param1) -> {
         String l_var2 = StringUtil.truncateStringIfNecessary(l_param1, 255, true);
         l_param0.writeUtf(l_var2);
      });
      if (param0 instanceof Villager) {
         Set<BlockPos> var6 = Stream.of(MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT)
            .map(var2::getMemory)
            .flatMap(Optional::stream)
            .map(GlobalPos::pos)
            .collect(Collectors.toSet());
         param1.writeCollection(var6, FriendlyByteBuf::writeBlockPos);
      } else {
         param1.writeVarInt(0);
      }

      if (param0 instanceof Villager) {
         Set<BlockPos> var6 = Stream.of(MemoryModuleType.POTENTIAL_JOB_SITE)
            .map(var2::getMemory)
            .flatMap(Optional::stream)
            .map(GlobalPos::pos)
            .collect(Collectors.toSet());
         param1.writeCollection(var6, FriendlyByteBuf::writeBlockPos);
      } else {
         param1.writeVarInt(0);
      }

      if (param0 instanceof Villager) {
         Map<UUID, Object2IntMap<GossipType>> var6 = ((Villager)param0).getGossips().getGossipEntries();
         List<String> var7 = Lists.newArrayList();
         var6.forEach((l_param1, l_param2) -> {
            String l_var3 = DebugEntityNameGenerator.getEntityName(l_param1);
            l_param2.forEach((l2_param2, l2_param3) -> var7.add(l_var3 + ": " + l2_param2 + ": " + l2_param3));
         });
         param1.writeCollection(var7, FriendlyByteBuf::writeUtf);
      } else {
         param1.writeVarInt(0);
      }
   }

   private static List<String> getMemoryDescriptions(LivingEntity param0, long param1) {
      Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> var3 = param0.getBrain().getMemories();
      List<String> var4 = Lists.newArrayList();

      for (Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> var6 : var3.entrySet()) {
         MemoryModuleType<?> var7 = var6.getKey();
         Optional<? extends ExpirableValue<?>> var8 = var6.getValue();
         String var9;
         if (var8.isPresent()) {
            ExpirableValue<?> var10 = (ExpirableValue<?>)var8.get();
            Object var11 = var10.getValue();
            if (var7 == MemoryModuleType.HEARD_BELL_TIME) {
               long var12 = param1 - (Long)var11;
               var9 = var12 + " ticks ago";
            } else if (var10.canExpire()) {
               var9 = getShortDescription((ServerLevel)param0.level, var11) + " (ttl: " + var10.getTimeToLive() + ")";
            } else {
               var9 = getShortDescription((ServerLevel)param0.level, var11);
            }
         } else {
            var9 = "-";
         }

         var4.add(BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(var7).getPath() + ": " + var9);
      }

      var4.sort(String::compareTo);
      return var4;
   }

   private static String getShortDescription(ServerLevel param0, @Nullable Object param1) {
      if (param1 == null) {
         return "-";
      }

      if (param1 instanceof UUID) {
         return getShortDescription(param0, param0.getEntity((UUID)param1));
      }

      if (param1 instanceof LivingEntity) {
         Entity var2 = (Entity)param1;
         return DebugEntityNameGenerator.getEntityName(var2);
      }

      if (param1 instanceof Nameable) {
         return ((Nameable)param1).getName().getString();
      }

      if (param1 instanceof WalkTarget) {
         return getShortDescription(param0, ((WalkTarget)param1).getTarget());
      }

      if (param1 instanceof EntityTracker) {
         return getShortDescription(param0, ((EntityTracker)param1).getEntity());
      }

      if (param1 instanceof GlobalPos) {
         return getShortDescription(param0, ((GlobalPos)param1).pos());
      }

      if (param1 instanceof BlockPosTracker) {
         return getShortDescription(param0, ((BlockPosTracker)param1).currentBlockPosition());
      }

      if (param1 instanceof DamageSource) {
         Entity var2 = ((DamageSource)param1).getEntity();
         return var2 == null ? param1.toString() : getShortDescription(param0, var2);
      }

      if (!(param1 instanceof Collection)) {
         return param1.toString();
      }

      List<String> var2 = Lists.newArrayList();

      for (Object var4 : (Iterable)param1) {
         var2.add(getShortDescription(param0, var4));
      }

      return var2.toString();
   }

   private static void sendPacketToAllPlayers(ServerLevel param0, FriendlyByteBuf param1, ResourceLocation param2) {
      Packet<?> var3 = new ClientboundCustomPayloadPacket(param2, param1);

      for (Player var5 : param0.players()) {
         ((ServerPlayer)var5).connection.send(var3);
      }
   }
}
