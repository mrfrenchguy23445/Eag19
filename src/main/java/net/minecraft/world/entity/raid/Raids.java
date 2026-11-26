package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
   private static final String RAID_FILE_ID = "raids";
   private final Map<Integer, Raid> raidMap = Maps.newHashMap();
   private final ServerLevel level;
   private int nextAvailableID;
   private int tick;

   public Raids(ServerLevel param0) {
      this.level = param0;
      this.nextAvailableID = 1;
      this.setDirty();
   }

   public Raid get(int param0) {
      return this.raidMap.get(param0);
   }

   public void tick() {
      this.tick++;
      Iterator<Raid> var1 = this.raidMap.values().iterator();

      while (var1.hasNext()) {
         Raid var2 = var1.next();
         if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            var2.stop();
         }

         if (var2.isStopped()) {
            var1.remove();
            this.setDirty();
         } else {
            var2.tick();
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }

      DebugPackets.sendRaids(this.level, this.raidMap.values());
   }

   public static boolean canJoinRaid(Raider param0, Raid param1) {
      return param0 != null && param1 != null && param1.getLevel() != null
         ? param0.isAlive() && param0.canJoinRaid() && param0.getNoActionTime() <= 2400 && param0.level.dimensionType() == param1.getLevel().dimensionType()
         : false;
   }

   @Nullable
   public Raid createOrExtendRaid(ServerPlayer param0) {
      if (param0.isSpectator()) {
         return null;
      }

      if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
         return null;
      }

      DimensionType var2 = param0.level.dimensionType();
      if (!var2.hasRaids()) {
         return null;
      }

      BlockPos var3 = param0.blockPosition();
      List<PoiRecord> var5 = this.level
         .getPoiManager()
         .getInRange(l_param0 -> l_param0.is(PoiTypeTags.VILLAGE), var3, 64, PoiManager.Occupancy.IS_OCCUPIED)
         .toList();
      int var6 = 0;
      Vec3 var7 = Vec3.ZERO;

      for (PoiRecord var9 : var5) {
         BlockPos var10 = var9.getPos();
         var7 = var7.add(var10.getX(), var10.getY(), var10.getZ());
         var6++;
      }

      BlockPos var4;
      if (var6 > 0) {
         var7 = var7.scale(1.0 / var6);
         var4 = BlockPos.containing(var7);
      } else {
         var4 = var3;
      }

      Raid var8 = this.getOrCreateRaid(param0.getLevel(), var4);
      boolean var9 = false;
      if (!var8.isStarted()) {
         if (!this.raidMap.containsKey(var8.getId())) {
            this.raidMap.put(var8.getId(), var8);
         }

         var9 = true;
      } else if (var8.getBadOmenLevel() < var8.getMaxBadOmenLevel()) {
         var9 = true;
      } else {
         param0.removeEffect(MobEffects.BAD_OMEN);
         param0.connection.send(new ClientboundEntityEventPacket(param0, (byte)43));
      }

      if (var9) {
         var8.absorbBadOmen(param0);
         param0.connection.send(new ClientboundEntityEventPacket(param0, (byte)43));
         if (!var8.hasFirstWaveSpawned()) {
            param0.awardStat(Stats.RAID_TRIGGER);
            CriteriaTriggers.BAD_OMEN.trigger(param0);
         }
      }

      this.setDirty();
      return var8;
   }

   private Raid getOrCreateRaid(ServerLevel param0, BlockPos param1) {
      Raid var3 = param0.getRaidAt(param1);
      return var3 != null ? var3 : new Raid(this.getUniqueId(), param0, param1);
   }

   public static Raids load(ServerLevel param0, CompoundTag param1) {
      Raids var2 = new Raids(param0);
      var2.nextAvailableID = param1.getInt("NextAvailableID");
      var2.tick = param1.getInt("Tick");
      ListTag var3 = param1.getList("Raids", 10);

      for (int var4 = 0; var4 < var3.size(); var4++) {
         CompoundTag var5 = var3.getCompound(var4);
         Raid var6 = new Raid(param0, var5);
         var2.raidMap.put(var6.getId(), var6);
      }

      return var2;
   }

   @Override
   public CompoundTag save(CompoundTag param0) {
      param0.putInt("NextAvailableID", this.nextAvailableID);
      param0.putInt("Tick", this.tick);
      ListTag var2 = new ListTag();

      for (Raid var4 : this.raidMap.values()) {
         CompoundTag var5 = new CompoundTag();
         var4.save(var5);
         var2.add(var5);
      }

      param0.put("Raids", var2);
      return param0;
   }

   public static String getFileId(Holder<DimensionType> param0) {
      return param0.is(BuiltinDimensionTypes.END) ? "raids_end" : "raids";
   }

   private int getUniqueId() {
      return ++this.nextAvailableID;
   }

   @Nullable
   public Raid getNearbyRaid(BlockPos param0, int param1) {
      Raid var3 = null;
      double var4 = param1;

      for (Raid var7 : this.raidMap.values()) {
         double var8 = var7.getCenter().distSqr(param0);
         if (var7.isActive() && var8 < var4) {
            var3 = var7;
            var4 = var8;
         }
      }

      return var3;
   }
}
