package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAP_SIZE = 128;
   private static final int HALF_MAP_SIZE = 64;
   public static final int MAX_SCALE = 4;
   public static final int TRACKED_DECORATION_LIMIT = 256;
   public final int centerX;
   public final int centerZ;
   public final ResourceKey<Level> dimension;
   private final boolean trackingPosition;
   private final boolean unlimitedTracking;
   public final byte scale;
   public byte[] colors = new byte[16384];
   public final boolean locked;
   private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
   private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
   private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
   final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
   private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
   private int trackedDecorationCount;

   private MapItemSavedData(int param0, int param1, byte param2, boolean param3, boolean param4, boolean param5, ResourceKey<Level> param6) {
      this.scale = param2;
      this.centerX = param0;
      this.centerZ = param1;
      this.dimension = param6;
      this.trackingPosition = param3;
      this.unlimitedTracking = param4;
      this.locked = param5;
      this.setDirty();
   }

   public static MapItemSavedData createFresh(double param0, double param2, byte param4, boolean param5, boolean param6, ResourceKey<Level> param7) {
      int var8 = 128 * (1 << param4);
      int var9 = Mth.floor((param0 + 64.0) / var8);
      int var10 = Mth.floor((param2 + 64.0) / var8);
      int var11 = var9 * var8 + var8 / 2 - 64;
      int var12 = var10 * var8 + var8 / 2 - 64;
      return new MapItemSavedData(var11, var12, param4, param5, param6, false, param7);
   }

   public static MapItemSavedData createForClient(byte param0, boolean param1, ResourceKey<Level> param2) {
      return new MapItemSavedData(0, 0, param0, false, false, param1, param2);
   }

   public static MapItemSavedData load(CompoundTag param0) {
      ResourceKey<Level> var1 = DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, param0.get("dimension")))
         .resultOrPartial(LOGGER::error)
         .orElseThrow(() -> new IllegalArgumentException("Invalid map dimension: " + param0.get("dimension")));
      int var2 = param0.getInt("xCenter");
      int var3 = param0.getInt("zCenter");
      byte var4 = (byte)Mth.clamp(param0.getByte("scale"), 0, 4);
      boolean var5 = !param0.contains("trackingPosition", 1) || param0.getBoolean("trackingPosition");
      boolean var6 = param0.getBoolean("unlimitedTracking");
      boolean var7 = param0.getBoolean("locked");
      MapItemSavedData var8 = new MapItemSavedData(var2, var3, var4, var5, var6, var7, var1);
      byte[] var9 = param0.getByteArray("colors");
      if (var9.length == 16384) {
         var8.colors = var9;
      }

      ListTag var10 = param0.getList("banners", 10);

      for (int var11 = 0; var11 < var10.size(); var11++) {
         MapBanner var12 = MapBanner.load(var10.getCompound(var11));
         var8.bannerMarkers.put(var12.getId(), var12);
         var8.addDecoration(var12.getDecoration(), null, var12.getId(), var12.getPos().getX(), var12.getPos().getZ(), 180.0, var12.getName());
      }

      ListTag var11 = param0.getList("frames", 10);

      for (int var12 = 0; var12 < var11.size(); var12++) {
         MapFrame var13 = MapFrame.load(var11.getCompound(var12));
         var8.frameMarkers.put(var13.getId(), var13);
         var8.addDecoration(
            MapDecoration.Type.FRAME, null, "frame-" + var13.getEntityId(), var13.getPos().getX(), var13.getPos().getZ(), var13.getRotation(), null
         );
      }

      return var8;
   }

   @Override
   public CompoundTag save(CompoundTag param0) {
      ResourceLocation.CODEC
         .encodeStart(NbtOps.INSTANCE, this.dimension.location())
         .resultOrPartial(LOGGER::error)
         .ifPresent(l_param1 -> param0.put("dimension", l_param1));
      param0.putInt("xCenter", this.centerX);
      param0.putInt("zCenter", this.centerZ);
      param0.putByte("scale", this.scale);
      param0.putByteArray("colors", this.colors);
      param0.putBoolean("trackingPosition", this.trackingPosition);
      param0.putBoolean("unlimitedTracking", this.unlimitedTracking);
      param0.putBoolean("locked", this.locked);
      ListTag var2 = new ListTag();

      for (MapBanner var4 : this.bannerMarkers.values()) {
         var2.add(var4.save());
      }

      param0.put("banners", var2);
      ListTag var3 = new ListTag();

      for (MapFrame var5 : this.frameMarkers.values()) {
         var3.add(var5.save());
      }

      param0.put("frames", var3);
      return param0;
   }

   public MapItemSavedData locked() {
      MapItemSavedData var1 = new MapItemSavedData(this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);
      var1.bannerMarkers.putAll(this.bannerMarkers);
      var1.decorations.putAll(this.decorations);
      var1.trackedDecorationCount = this.trackedDecorationCount;
      System.arraycopy(this.colors, 0, var1.colors, 0, this.colors.length);
      var1.setDirty();
      return var1;
   }

   public MapItemSavedData scaled(int param0) {
      return createFresh(this.centerX, this.centerZ, (byte)Mth.clamp(this.scale + param0, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
   }

   public void tickCarriedBy(Player param0, ItemStack param1) {
      if (!this.carriedByPlayers.containsKey(param0)) {
         MapItemSavedData.HoldingPlayer var3 = new MapItemSavedData.HoldingPlayer(param0);
         this.carriedByPlayers.put(param0, var3);
         this.carriedBy.add(var3);
      }

      if (!param0.getInventory().contains(param1)) {
         this.removeDecoration(param0.getName().getString());
      }

      for (int var3 = 0; var3 < this.carriedBy.size(); var3++) {
         MapItemSavedData.HoldingPlayer var4 = this.carriedBy.get(var3);
         String var5 = var4.player.getName().getString();
         if (!var4.player.isRemoved() && (var4.player.getInventory().contains(param1) || param1.isFramed())) {
            if (!param1.isFramed() && var4.player.level.dimension() == this.dimension && this.trackingPosition) {
               this.addDecoration(MapDecoration.Type.PLAYER, var4.player.level, var5, var4.player.getX(), var4.player.getZ(), var4.player.getYRot(), null);
            }
         } else {
            this.carriedByPlayers.remove(var4.player);
            this.carriedBy.remove(var4);
            this.removeDecoration(var5);
         }
      }

      if (param1.isFramed() && this.trackingPosition) {
         ItemFrame var3 = param1.getFrame();
         BlockPos var4 = var3.getPos();
         MapFrame var5 = this.frameMarkers.get(MapFrame.frameId(var4));
         if (var5 != null && var3.getId() != var5.getEntityId() && this.frameMarkers.containsKey(var5.getId())) {
            this.removeDecoration("frame-" + var5.getEntityId());
         }

         MapFrame var6 = new MapFrame(var4, var3.getDirection().get2DDataValue() * 90, var3.getId());
         this.addDecoration(
            MapDecoration.Type.FRAME, param0.level, "frame-" + var3.getId(), var4.getX(), var4.getZ(), var3.getDirection().get2DDataValue() * 90, null
         );
         this.frameMarkers.put(var6.getId(), var6);
      }

      CompoundTag var3 = param1.getTag();
      if (var3 != null && var3.contains("Decorations", 9)) {
         ListTag var4 = var3.getList("Decorations", 10);

         for (int var5 = 0; var5 < var4.size(); var5++) {
            CompoundTag var6 = var4.getCompound(var5);
            if (!this.decorations.containsKey(var6.getString("id"))) {
               this.addDecoration(
                  MapDecoration.Type.byIcon(var6.getByte("type")),
                  param0.level,
                  var6.getString("id"),
                  var6.getDouble("x"),
                  var6.getDouble("z"),
                  var6.getDouble("rot"),
                  null
               );
            }
         }
      }
   }

   private void removeDecoration(String param0) {
      MapDecoration var2 = this.decorations.remove(param0);
      if (var2 != null && var2.getType().shouldTrackCount()) {
         this.trackedDecorationCount--;
      }

      this.setDecorationsDirty();
   }

   public static void addTargetDecoration(ItemStack param0, BlockPos param1, String param2, MapDecoration.Type param3) {
      ListTag var4;
      if (param0.hasTag() && param0.getTag().contains("Decorations", 9)) {
         var4 = param0.getTag().getList("Decorations", 10);
      } else {
         var4 = new ListTag();
         param0.addTagElement("Decorations", var4);
      }

      CompoundTag var5 = new CompoundTag();
      var5.putByte("type", param3.getIcon());
      var5.putString("id", param2);
      var5.putDouble("x", param1.getX());
      var5.putDouble("z", param1.getZ());
      var5.putDouble("rot", 180.0);
      var4.add(var5);
      if (param3.hasMapColor()) {
         CompoundTag var6 = param0.getOrCreateTagElement("display");
         var6.putInt("MapColor", param3.getMapColor());
      }
   }

   private void addDecoration(
      MapDecoration.Type param0, @Nullable LevelAccessor param1, String param2, double param3, double param5, double param7, @Nullable Component param9
   ) {
      int var11 = 1 << this.scale;
      float var12 = (float)(param3 - this.centerX) / var11;
      float var13 = (float)(param5 - this.centerZ) / var11;
      byte var14 = (byte)(var12 * 2.0F + 0.5);
      byte var15 = (byte)(var13 * 2.0F + 0.5);
      int var17 = 63;
      byte var16;
      if (var12 >= -63.0F && var13 >= -63.0F && var12 <= 63.0F && var13 <= 63.0F) {
         param7 += param7 < 0.0 ? -8.0 : 8.0;
         var16 = (byte)(param7 * 16.0 / 360.0);
         if (this.dimension == Level.NETHER && param1 != null) {
            int var18 = (int)(param1.getLevelData().getDayTime() / 10L);
            var16 = (byte)(var18 * var18 * 34187121 + var18 * 121 >> 15 & 15);
         }
      } else {
         if (param0 != MapDecoration.Type.PLAYER) {
            this.removeDecoration(param2);
            return;
         }

         int var18 = 320;
         if (Math.abs(var12) < 320.0F && Math.abs(var13) < 320.0F) {
            param0 = MapDecoration.Type.PLAYER_OFF_MAP;
         } else {
            if (!this.unlimitedTracking) {
               this.removeDecoration(param2);
               return;
            }

            param0 = MapDecoration.Type.PLAYER_OFF_LIMITS;
         }

         var16 = 0;
         if (var12 <= -63.0F) {
            var14 = -128;
         }

         if (var13 <= -63.0F) {
            var15 = -128;
         }

         if (var12 >= 63.0F) {
            var14 = 127;
         }

         if (var13 >= 63.0F) {
            var15 = 127;
         }
      }

      MapDecoration var18 = new MapDecoration(param0, var14, var15, var16, param9);
      MapDecoration var19 = this.decorations.put(param2, var18);
      if (!var18.equals(var19)) {
         if (var19 != null && var19.getType().shouldTrackCount()) {
            this.trackedDecorationCount--;
         }

         if (param0.shouldTrackCount()) {
            this.trackedDecorationCount++;
         }

         this.setDecorationsDirty();
      }
   }

   @Nullable
   public Packet<?> getUpdatePacket(int param0, Player param1) {
      MapItemSavedData.HoldingPlayer var3 = this.carriedByPlayers.get(param1);
      return var3 == null ? null : var3.nextUpdatePacket(param0);
   }

   private void setColorsDirty(int param0, int param1) {
      this.setDirty();

      for (MapItemSavedData.HoldingPlayer var4 : this.carriedBy) {
         var4.markColorsDirty(param0, param1);
      }
   }

   private void setDecorationsDirty() {
      this.setDirty();
      this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
   }

   public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player param0) {
      MapItemSavedData.HoldingPlayer var2 = this.carriedByPlayers.get(param0);
      if (var2 == null) {
         var2 = new MapItemSavedData.HoldingPlayer(param0);
         this.carriedByPlayers.put(param0, var2);
         this.carriedBy.add(var2);
      }

      return var2;
   }

   public boolean toggleBanner(LevelAccessor param0, BlockPos param1) {
      double var3 = param1.getX() + 0.5;
      double var5 = param1.getZ() + 0.5;
      int var7 = 1 << this.scale;
      double var8 = (var3 - this.centerX) / var7;
      double var10 = (var5 - this.centerZ) / var7;
      int var12 = 63;
      if (var8 >= -63.0 && var10 >= -63.0 && var8 <= 63.0 && var10 <= 63.0) {
         MapBanner var13 = MapBanner.fromWorld(param0, param1);
         if (var13 == null) {
            return false;
         }

         if (this.bannerMarkers.remove(var13.getId(), var13)) {
            this.removeDecoration(var13.getId());
            return true;
         }

         if (!this.isTrackedCountOverLimit(256)) {
            this.bannerMarkers.put(var13.getId(), var13);
            this.addDecoration(var13.getDecoration(), param0, var13.getId(), var3, var5, 180.0, var13.getName());
            return true;
         }
      }

      return false;
   }

   public void checkBanners(BlockGetter param0, int param1, int param2) {
      Iterator<MapBanner> var4 = this.bannerMarkers.values().iterator();

      while (var4.hasNext()) {
         MapBanner var5 = var4.next();
         if (var5.getPos().getX() == param1 && var5.getPos().getZ() == param2) {
            MapBanner var6 = MapBanner.fromWorld(param0, var5.getPos());
            if (!var5.equals(var6)) {
               var4.remove();
               this.removeDecoration(var5.getId());
            }
         }
      }
   }

   public Collection<MapBanner> getBanners() {
      return this.bannerMarkers.values();
   }

   public void removedFromFrame(BlockPos param0, int param1) {
      this.removeDecoration("frame-" + param1);
      this.frameMarkers.remove(MapFrame.frameId(param0));
   }

   public boolean updateColor(int param0, int param1, byte param2) {
      byte var4 = this.colors[param0 + param1 * 128];
      if (var4 != param2) {
         this.setColor(param0, param1, param2);
         return true;
      } else {
         return false;
      }
   }

   public void setColor(int param0, int param1, byte param2) {
      this.colors[param0 + param1 * 128] = param2;
      this.setColorsDirty(param0, param1);
   }

   public boolean isExplorationMap() {
      for (MapDecoration var2 : this.decorations.values()) {
         if (var2.getType() == MapDecoration.Type.MANSION || var2.getType() == MapDecoration.Type.MONUMENT) {
            return true;
         }
      }

      return false;
   }

   public void addClientSideDecorations(List<MapDecoration> param0) {
      this.decorations.clear();
      this.trackedDecorationCount = 0;

      for (int var2 = 0; var2 < param0.size(); var2++) {
         MapDecoration var3 = param0.get(var2);
         this.decorations.put("icon-" + var2, var3);
         if (var3.getType().shouldTrackCount()) {
            this.trackedDecorationCount++;
         }
      }
   }

   public Iterable<MapDecoration> getDecorations() {
      return this.decorations.values();
   }

   public boolean isTrackedCountOverLimit(int param0) {
      return this.trackedDecorationCount >= param0;
   }

   public class HoldingPlayer {
      public final Player player;
      private boolean dirtyData = true;
      private int minDirtyX;
      private int minDirtyY;
      private int maxDirtyX = 127;
      private int maxDirtyY = 127;
      private boolean dirtyDecorations = true;
      private int tick;
      public int step;

      HoldingPlayer(Player param1) {
         this.player = param1;
      }

      private MapItemSavedData.MapPatch createPatch() {
         int var1 = this.minDirtyX;
         int var2 = this.minDirtyY;
         int var3 = this.maxDirtyX + 1 - this.minDirtyX;
         int var4 = this.maxDirtyY + 1 - this.minDirtyY;
         byte[] var5 = new byte[var3 * var4];

         for (int var6 = 0; var6 < var3; var6++) {
            for (int var7 = 0; var7 < var4; var7++) {
               var5[var6 + var7 * var3] = MapItemSavedData.this.colors[var1 + var6 + (var2 + var7) * 128];
            }
         }

         return new MapItemSavedData.MapPatch(var1, var2, var3, var4, var5);
      }

      @Nullable
      Packet<?> nextUpdatePacket(int param0) {
         MapItemSavedData.MapPatch var2;
         if (this.dirtyData) {
            this.dirtyData = false;
            var2 = this.createPatch();
         } else {
            var2 = null;
         }

         Collection<MapDecoration> var3;
         if (this.dirtyDecorations && this.tick++ % 5 == 0) {
            this.dirtyDecorations = false;
            var3 = MapItemSavedData.this.decorations.values();
         } else {
            var3 = null;
         }

         return var3 == null && var2 == null
            ? null
            : new ClientboundMapItemDataPacket(param0, MapItemSavedData.this.scale, MapItemSavedData.this.locked, var3, var2);
      }

      void markColorsDirty(int param0, int param1) {
         if (this.dirtyData) {
            this.minDirtyX = Math.min(this.minDirtyX, param0);
            this.minDirtyY = Math.min(this.minDirtyY, param1);
            this.maxDirtyX = Math.max(this.maxDirtyX, param0);
            this.maxDirtyY = Math.max(this.maxDirtyY, param1);
         } else {
            this.dirtyData = true;
            this.minDirtyX = param0;
            this.minDirtyY = param1;
            this.maxDirtyX = param0;
            this.maxDirtyY = param1;
         }
      }

      private void markDecorationsDirty() {
         this.dirtyDecorations = true;
      }
   }

   public static class MapPatch {
      public final int startX;
      public final int startY;
      public final int width;
      public final int height;
      public final byte[] mapColors;

      public MapPatch(int param0, int param1, int param2, int param3, byte[] param4) {
         this.startX = param0;
         this.startY = param1;
         this.width = param2;
         this.height = param3;
         this.mapColors = param4;
      }

      public void applyToMap(MapItemSavedData param0) {
         for (int var2 = 0; var2 < this.width; var2++) {
            for (int var3 = 0; var3 < this.height; var3++) {
               param0.setColor(this.startX + var2, this.startY + var3, this.mapColors[var2 + var3 * this.width]);
            }
         }
      }
   }
}
