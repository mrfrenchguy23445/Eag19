package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
   private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
   private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
   private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
   private static final boolean SHOW_PATH_FOR_ALL_BEES = false;
   private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_PATH_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_MEMBERS = true;
   private static final boolean SHOW_BLACKLISTS = true;
   private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
   private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final int HIVE_TIMEOUT = 20;
   private static final float TEXT_SCALE = 0.02F;
   private static final int WHITE = -1;
   private static final int YELLOW = -256;
   private static final int ORANGE = -23296;
   private static final int GREEN = -16711936;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private static final int RED = -65536;
   private final Minecraft minecraft;
   private final Map<BlockPos, BeeDebugRenderer.HiveInfo> hives = Maps.newHashMap();
   private final Map<UUID, BeeDebugRenderer.BeeInfo> beeInfosPerEntity = Maps.newHashMap();
   private UUID lastLookedAtUuid;

   public BeeDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void clear() {
      this.hives.clear();
      this.beeInfosPerEntity.clear();
      this.lastLookedAtUuid = null;
   }

   public void addOrUpdateHiveInfo(BeeDebugRenderer.HiveInfo param0) {
      this.hives.put(param0.pos, param0);
   }

   public void addOrUpdateBeeInfo(BeeDebugRenderer.BeeInfo param0) {
      this.beeInfosPerEntity.put(param0.uuid, param0);
   }

   public void removeBeeInfo(int param0) {
      this.beeInfosPerEntity.values().removeIf(l_param1 -> l_param1.id == param0);
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      this.clearRemovedHives();
      this.clearRemovedBees();
      this.doRender(param0, param1);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void clearRemovedBees() {
      this.beeInfosPerEntity.entrySet().removeIf(l_param0 -> this.minecraft.level.getEntity(l_param0.getValue().id) == null);
   }

   private void clearRemovedHives() {
      long var1 = this.minecraft.level.getGameTime() - 20L;
      this.hives.entrySet().removeIf(l_param2 -> l_param2.getValue().lastSeen < var1);
   }

   private void doRender(PoseStack param0, MultiBufferSource param1) {
      BlockPos var3 = this.getCamera().getBlockPosition();
      this.beeInfosPerEntity.values().forEach(l_param2 -> {
         if (this.isPlayerCloseEnoughToMob(l_param2)) {
            this.renderBeeInfo(param0, param1, l_param2);
         }
      });
      this.renderFlowerInfos(param0, param1);

      for (BlockPos var5 : this.hives.keySet()) {
         if (var3.closerThan(var5, 30.0)) {
            highlightHive(param0, param1, var5);
         }
      }

      Map<BlockPos, Set<UUID>> var4 = this.createHiveBlacklistMap();
      this.hives.values().forEach(l_param4 -> {
         if (var3.closerThan(l_param4.pos, 30.0)) {
            Set<UUID> l_var6 = var4.get(l_param4.pos);
            this.renderHiveInfo(param0, param1, l_param4, l_var6 == null ? Sets.newHashSet() : l_var6);
         }
      });
      this.getGhostHives().forEach((l_param3, l_param4) -> {
         if (var3.closerThan(l_param3, 30.0)) {
            this.renderGhostHive(param0, param1, l_param3, (List<String>)l_param4);
         }
      });
   }

   private Map<BlockPos, Set<UUID>> createHiveBlacklistMap() {
      Map<BlockPos, Set<UUID>> var1 = Maps.newHashMap();
      this.beeInfosPerEntity
         .values()
         .forEach(
            l_param1 -> l_param1.blacklistedHives.forEach(l2_param2 -> var1.computeIfAbsent(l2_param2, l3_param0 -> Sets.newHashSet()).add(l_param1.getUuid()))
         );
      return var1;
   }

   private void renderFlowerInfos(PoseStack param0, MultiBufferSource param1) {
      Map<BlockPos, Set<UUID>> var3 = Maps.newHashMap();
      this.beeInfosPerEntity
         .values()
         .stream()
         .filter(BeeDebugRenderer.BeeInfo::hasFlower)
         .forEach(l_param1 -> var3.computeIfAbsent(l_param1.flowerPos, l2_param0 -> Sets.newHashSet()).add(l_param1.getUuid()));
      var3.entrySet().forEach(l_param2 -> {
         BlockPos l_var3 = l_param2.getKey();
         Set<UUID> l_var4 = l_param2.getValue();
         Set<String> l_var5 = l_var4.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
         int l_var6 = 1;
         renderTextOverPos(param0, param1, l_var5.toString(), l_var3, l_var6++, -256);
         renderTextOverPos(param0, param1, "Flower", l_var3, l_var6++, -1);
         float l_var7 = 0.05F;
         DebugRenderer.renderFilledBox(param0, param1, l_var3, 0.05F, 0.8F, 0.8F, 0.0F, 0.3F);
      });
   }

   private static String getBeeUuidsAsString(Collection<UUID> param0) {
      if (param0.isEmpty()) {
         return "-";
      } else {
         return param0.size() > 3
            ? param0.size() + " bees"
            : param0.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
      }
   }

   private static void highlightHive(PoseStack param0, MultiBufferSource param1, BlockPos param2) {
      float var3 = 0.05F;
      DebugRenderer.renderFilledBox(param0, param1, param2, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
   }

   private void renderGhostHive(PoseStack param0, MultiBufferSource param1, BlockPos param2, List<String> param3) {
      float var5 = 0.05F;
      DebugRenderer.renderFilledBox(param0, param1, param2, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
      renderTextOverPos(param0, param1, param3 + "", param2, 0, -256);
      renderTextOverPos(param0, param1, "Ghost Hive", param2, 1, -65536);
   }

   private void renderHiveInfo(PoseStack param0, MultiBufferSource param1, BeeDebugRenderer.HiveInfo param2, Collection<UUID> param3) {
      int var5 = 0;
      if (!param3.isEmpty()) {
         renderTextOverHive(param0, param1, "Blacklisted by " + getBeeUuidsAsString(param3), param2, var5++, -65536);
      }

      renderTextOverHive(param0, param1, "Out: " + getBeeUuidsAsString(this.getHiveMembers(param2.pos)), param2, var5++, -3355444);
      if (param2.occupantCount == 0) {
         renderTextOverHive(param0, param1, "In: -", param2, var5++, -256);
      } else if (param2.occupantCount == 1) {
         renderTextOverHive(param0, param1, "In: 1 bee", param2, var5++, -256);
      } else {
         renderTextOverHive(param0, param1, "In: " + param2.occupantCount + " bees", param2, var5++, -256);
      }

      renderTextOverHive(param0, param1, "Honey: " + param2.honeyLevel, param2, var5++, -23296);
      renderTextOverHive(param0, param1, param2.hiveType + (param2.sedated ? " (sedated)" : ""), param2, var5++, -1);
   }

   private void renderPath(PoseStack param0, MultiBufferSource param1, BeeDebugRenderer.BeeInfo param2) {
      if (param2.path != null) {
         PathfindingRenderer.renderPath(
            param0,
            param1,
            param2.path,
            0.5F,
            false,
            false,
            this.getCamera().getPosition().x(),
            this.getCamera().getPosition().y(),
            this.getCamera().getPosition().z()
         );
      }
   }

   private void renderBeeInfo(PoseStack param0, MultiBufferSource param1, BeeDebugRenderer.BeeInfo param2) {
      boolean var4 = this.isBeeSelected(param2);
      int var5 = 0;
      renderTextOverMob(param0, param1, param2.pos, var5++, param2.toString(), -1, 0.03F);
      if (param2.hivePos == null) {
         renderTextOverMob(param0, param1, param2.pos, var5++, "No hive", -98404, 0.02F);
      } else {
         renderTextOverMob(param0, param1, param2.pos, var5++, "Hive: " + this.getPosDescription(param2, param2.hivePos), -256, 0.02F);
      }

      if (param2.flowerPos == null) {
         renderTextOverMob(param0, param1, param2.pos, var5++, "No flower", -98404, 0.02F);
      } else {
         renderTextOverMob(param0, param1, param2.pos, var5++, "Flower: " + this.getPosDescription(param2, param2.flowerPos), -256, 0.02F);
      }

      for (String var7 : param2.goals) {
         renderTextOverMob(param0, param1, param2.pos, var5++, var7, -16711936, 0.02F);
      }

      if (var4) {
         this.renderPath(param0, param1, param2);
      }

      if (param2.travelTicks > 0) {
         int var6 = param2.travelTicks < 600 ? -3355444 : -23296;
         renderTextOverMob(param0, param1, param2.pos, var5++, "Travelling: " + param2.travelTicks + " ticks", var6, 0.02F);
      }
   }

   private static void renderTextOverHive(PoseStack param0, MultiBufferSource param1, String param2, BeeDebugRenderer.HiveInfo param3, int param4, int param5) {
      BlockPos var6 = param3.pos;
      renderTextOverPos(param0, param1, param2, var6, param4, param5);
   }

   private static void renderTextOverPos(PoseStack param0, MultiBufferSource param1, String param2, BlockPos param3, int param4, int param5) {
      double var6 = 1.3;
      double var8 = 0.2;
      double var10 = param3.getX() + 0.5;
      double var12 = param3.getY() + 1.3 + param4 * 0.2;
      double var14 = param3.getZ() + 0.5;
      DebugRenderer.renderFloatingText(param0, param1, param2, var10, var12, var14, param5, 0.02F, true, 0.0F, true);
   }

   private static void renderTextOverMob(PoseStack param0, MultiBufferSource param1, Position param2, int param3, String param4, int param5, float param6) {
      double var7 = 2.4;
      double var9 = 0.25;
      BlockPos var11 = BlockPos.containing(param2);
      double var12 = var11.getX() + 0.5;
      double var14 = param2.y() + 2.4 + param3 * 0.25;
      double var16 = var11.getZ() + 0.5;
      float var18 = 0.5F;
      DebugRenderer.renderFloatingText(param0, param1, param4, var12, var14, var16, param5, param6, false, 0.5F, true);
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }

   private Set<String> getHiveMemberNames(BeeDebugRenderer.HiveInfo param0) {
      return this.getHiveMembers(param0.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private String getPosDescription(BeeDebugRenderer.BeeInfo param0, BlockPos param1) {
      double var3 = Math.sqrt(param1.distToCenterSqr(param0.pos));
      double var5 = Math.round(var3 * 10.0) / 10.0;
      return param1.toShortString() + " (dist " + var5 + ")";
   }

   private boolean isBeeSelected(BeeDebugRenderer.BeeInfo param0) {
      return Objects.equals(this.lastLookedAtUuid, param0.uuid);
   }

   private boolean isPlayerCloseEnoughToMob(BeeDebugRenderer.BeeInfo param0) {
      Player var2 = this.minecraft.player;
      BlockPos var3 = BlockPos.containing(var2.getX(), param0.pos.y(), var2.getZ());
      BlockPos var4 = BlockPos.containing(param0.pos);
      return var3.closerThan(var4, 30.0);
   }

   private Collection<UUID> getHiveMembers(BlockPos param0) {
      return this.beeInfosPerEntity
         .values()
         .stream()
         .filter(l_param1 -> l_param1.hasHive(param0))
         .map(BeeDebugRenderer.BeeInfo::getUuid)
         .collect(Collectors.toSet());
   }

   private Map<BlockPos, List<String>> getGhostHives() {
      Map<BlockPos, List<String>> var1 = Maps.newHashMap();

      for (BeeDebugRenderer.BeeInfo var3 : this.beeInfosPerEntity.values()) {
         if (var3.hivePos != null && !this.hives.containsKey(var3.hivePos)) {
            var1.computeIfAbsent(var3.hivePos, l_param0 -> Lists.newArrayList()).add(var3.getName());
         }
      }

      return var1;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(l_param0 -> this.lastLookedAtUuid = l_param0.getUUID());
   }

   public static class BeeInfo {
      public final UUID uuid;
      public final int id;
      public final Position pos;
      @Nullable
      public final Path path;
      @Nullable
      public final BlockPos hivePos;
      @Nullable
      public final BlockPos flowerPos;
      public final int travelTicks;
      public final List<String> goals = Lists.newArrayList();
      public final Set<BlockPos> blacklistedHives = Sets.newHashSet();

      public BeeInfo(UUID param0, int param1, Position param2, @Nullable Path param3, @Nullable BlockPos param4, @Nullable BlockPos param5, int param6) {
         this.uuid = param0;
         this.id = param1;
         this.pos = param2;
         this.path = param3;
         this.hivePos = param4;
         this.flowerPos = param5;
         this.travelTicks = param6;
      }

      public boolean hasHive(BlockPos param0) {
         return this.hivePos != null && this.hivePos.equals(param0);
      }

      public UUID getUuid() {
         return this.uuid;
      }

      public String getName() {
         return DebugEntityNameGenerator.getEntityName(this.uuid);
      }

      @Override
      public String toString() {
         return this.getName();
      }

      public boolean hasFlower() {
         return this.flowerPos != null;
      }
   }

   public static class HiveInfo {
      public final BlockPos pos;
      public final String hiveType;
      public final int occupantCount;
      public final int honeyLevel;
      public final boolean sedated;
      public final long lastSeen;

      public HiveInfo(BlockPos param0, String param1, int param2, int param3, boolean param4, long param5) {
         this.pos = param0;
         this.hiveType = param1;
         this.occupantCount = param2;
         this.honeyLevel = param3;
         this.sedated = param4;
         this.lastSeen = param5;
      }
   }
}
