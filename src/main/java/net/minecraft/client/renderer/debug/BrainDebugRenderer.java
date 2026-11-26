package net.minecraft.client.renderer.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;

public class BrainDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final boolean SHOW_NAME_FOR_ALL = true;
   private static final boolean SHOW_PROFESSION_FOR_ALL = false;
   private static final boolean SHOW_BEHAVIORS_FOR_ALL = false;
   private static final boolean SHOW_ACTIVITIES_FOR_ALL = false;
   private static final boolean SHOW_INVENTORY_FOR_ALL = false;
   private static final boolean SHOW_GOSSIPS_FOR_ALL = false;
   private static final boolean SHOW_PATH_FOR_ALL = false;
   private static final boolean SHOW_HEALTH_FOR_ALL = false;
   private static final boolean SHOW_WANTS_GOLEM_FOR_ALL = true;
   private static final boolean SHOW_ANGER_LEVEL_FOR_ALL = false;
   private static final boolean SHOW_NAME_FOR_SELECTED = true;
   private static final boolean SHOW_PROFESSION_FOR_SELECTED = true;
   private static final boolean SHOW_BEHAVIORS_FOR_SELECTED = true;
   private static final boolean SHOW_ACTIVITIES_FOR_SELECTED = true;
   private static final boolean SHOW_MEMORIES_FOR_SELECTED = true;
   private static final boolean SHOW_INVENTORY_FOR_SELECTED = true;
   private static final boolean SHOW_GOSSIPS_FOR_SELECTED = true;
   private static final boolean SHOW_PATH_FOR_SELECTED = true;
   private static final boolean SHOW_HEALTH_FOR_SELECTED = true;
   private static final boolean SHOW_WANTS_GOLEM_FOR_SELECTED = true;
   private static final boolean SHOW_ANGER_LEVEL_FOR_SELECTED = true;
   private static final boolean SHOW_POI_INFO = true;
   private static final int MAX_RENDER_DIST_FOR_BRAIN_INFO = 30;
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final float TEXT_SCALE = 0.02F;
   private static final int WHITE = -1;
   private static final int YELLOW = -256;
   private static final int CYAN = -16711681;
   private static final int GREEN = -16711936;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private static final int RED = -65536;
   private static final int ORANGE = -23296;
   private final Minecraft minecraft;
   private final Map<BlockPos, BrainDebugRenderer.PoiInfo> pois = Maps.newHashMap();
   private final Map<UUID, BrainDebugRenderer.BrainDump> brainDumpsPerEntity = Maps.newHashMap();
   @Nullable
   private UUID lastLookedAtUuid;

   public BrainDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void clear() {
      this.pois.clear();
      this.brainDumpsPerEntity.clear();
      this.lastLookedAtUuid = null;
   }

   public void addPoi(BrainDebugRenderer.PoiInfo param0) {
      this.pois.put(param0.pos, param0);
   }

   public void removePoi(BlockPos param0) {
      this.pois.remove(param0);
   }

   public void setFreeTicketCount(BlockPos param0, int param1) {
      BrainDebugRenderer.PoiInfo var3 = this.pois.get(param0);
      if (var3 == null) {
         LOGGER.warn("Strange, setFreeTicketCount was called for an unknown POI: {}", param0);
      } else {
         var3.freeTicketCount = param1;
      }
   }

   public void addOrUpdateBrainDump(BrainDebugRenderer.BrainDump param0) {
      this.brainDumpsPerEntity.put(param0.uuid, param0);
   }

   public void removeBrainDump(int param0) {
      this.brainDumpsPerEntity.values().removeIf(l_param1 -> l_param1.id == param0);
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      this.clearRemovedEntities();
      this.doRender(param0, param1, param2, param4, param6);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void clearRemovedEntities() {
      this.brainDumpsPerEntity.entrySet().removeIf(l_param0 -> {
         Entity l_var2 = this.minecraft.level.getEntity(l_param0.getValue().id);
         return l_var2 == null || l_var2.isRemoved();
      });
   }

   private void doRender(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      BlockPos var9 = BlockPos.containing(param2, param4, param6);
      this.brainDumpsPerEntity.values().forEach(l_param8 -> {
         if (this.isPlayerCloseEnoughToMob(l_param8)) {
            this.renderBrainInfo(param0, param1, l_param8, param2, param4, param6);
         }
      });

      for (BlockPos var11 : this.pois.keySet()) {
         if (var9.closerThan(var11, 30.0)) {
            highlightPoi(param0, param1, var11);
         }
      }

      this.pois.values().forEach(l_param3 -> {
         if (var9.closerThan(l_param3.pos, 30.0)) {
            this.renderPoiInfo(param0, param1, l_param3);
         }
      });
      this.getGhostPois().forEach((l_param3, l_param4) -> {
         if (var9.closerThan(l_param3, 30.0)) {
            this.renderGhostPoi(param0, param1, l_param3, (List<String>)l_param4);
         }
      });
   }

   private static void highlightPoi(PoseStack param0, MultiBufferSource param1, BlockPos param2) {
      float var3 = 0.05F;
      DebugRenderer.renderFilledBox(param0, param1, param2, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
   }

   private void renderGhostPoi(PoseStack param0, MultiBufferSource param1, BlockPos param2, List<String> param3) {
      float var5 = 0.05F;
      DebugRenderer.renderFilledBox(param0, param1, param2, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
      renderTextOverPos(param0, param1, param3 + "", param2, 0, -256);
      renderTextOverPos(param0, param1, "Ghost POI", param2, 1, -65536);
   }

   private void renderPoiInfo(PoseStack param0, MultiBufferSource param1, BrainDebugRenderer.PoiInfo param2) {
      int var4 = 0;
      Set<String> var5 = this.getTicketHolderNames(param2);
      if (var5.size() < 4) {
         renderTextOverPoi(param0, param1, "Owners: " + var5, param2, var4, -256);
      } else {
         renderTextOverPoi(param0, param1, var5.size() + " ticket holders", param2, var4, -256);
      }

      var4++;
      Set<String> var6 = this.getPotentialTicketHolderNames(param2);
      if (var6.size() < 4) {
         renderTextOverPoi(param0, param1, "Candidates: " + var6, param2, var4, -23296);
      } else {
         renderTextOverPoi(param0, param1, var6.size() + " potential owners", param2, var4, -23296);
      }

      renderTextOverPoi(param0, param1, "Free tickets: " + param2.freeTicketCount, param2, ++var4, -256);
      renderTextOverPoi(param0, param1, param2.type, param2, ++var4, -1);
   }

   private void renderPath(PoseStack param0, MultiBufferSource param1, BrainDebugRenderer.BrainDump param2, double param3, double param5, double param7) {
      if (param2.path != null) {
         PathfindingRenderer.renderPath(param0, param1, param2.path, 0.5F, false, false, param3, param5, param7);
      }
   }

   private void renderBrainInfo(PoseStack param0, MultiBufferSource param1, BrainDebugRenderer.BrainDump param2, double param3, double param5, double param7) {
      boolean var10 = this.isMobSelected(param2);
      int var11 = 0;
      renderTextOverMob(param0, param1, param2.pos, var11, param2.name, -1, 0.03F);
      var11++;
      if (var10) {
         renderTextOverMob(param0, param1, param2.pos, var11, param2.profession + " " + param2.xp + " xp", -1, 0.02F);
         var11++;
      }

      if (var10) {
         int var12 = param2.health < param2.maxHealth ? -23296 : -1;
         renderTextOverMob(
            param0,
            param1,
            param2.pos,
            var11,
            "health: " + String.format(Locale.ROOT, "%.1f", param2.health) + " / " + String.format(Locale.ROOT, "%.1f", param2.maxHealth),
            var12,
            0.02F
         );
         var11++;
      }

      if (var10 && !param2.inventory.equals("")) {
         renderTextOverMob(param0, param1, param2.pos, var11, param2.inventory, -98404, 0.02F);
         var11++;
      }

      if (var10) {
         for (String var13 : param2.behaviors) {
            renderTextOverMob(param0, param1, param2.pos, var11, var13, -16711681, 0.02F);
            var11++;
         }
      }

      if (var10) {
         for (String var13 : param2.activities) {
            renderTextOverMob(param0, param1, param2.pos, var11, var13, -16711936, 0.02F);
            var11++;
         }
      }

      if (param2.wantsGolem) {
         renderTextOverMob(param0, param1, param2.pos, var11, "Wants Golem", -23296, 0.02F);
         var11++;
      }

      if (var10 && param2.angerLevel != -1) {
         renderTextOverMob(param0, param1, param2.pos, var11, "Anger Level: " + param2.angerLevel, -98404, 0.02F);
         var11++;
      }

      if (var10) {
         for (String var13 : param2.gossips) {
            if (var13.startsWith(param2.name)) {
               renderTextOverMob(param0, param1, param2.pos, var11, var13, -1, 0.02F);
            } else {
               renderTextOverMob(param0, param1, param2.pos, var11, var13, -23296, 0.02F);
            }

            var11++;
         }
      }

      if (var10) {
         for (String var13 : Lists.reverse(param2.memories)) {
            renderTextOverMob(param0, param1, param2.pos, var11, var13, -3355444, 0.02F);
            var11++;
         }
      }

      if (var10) {
         this.renderPath(param0, param1, param2, param3, param5, param7);
      }
   }

   private static void renderTextOverPoi(PoseStack param0, MultiBufferSource param1, String param2, BrainDebugRenderer.PoiInfo param3, int param4, int param5) {
      renderTextOverPos(param0, param1, param2, param3.pos, param4, param5);
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

   private Set<String> getTicketHolderNames(BrainDebugRenderer.PoiInfo param0) {
      return this.getTicketHolders(param0.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private Set<String> getPotentialTicketHolderNames(BrainDebugRenderer.PoiInfo param0) {
      return this.getPotentialTicketHolders(param0.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private boolean isMobSelected(BrainDebugRenderer.BrainDump param0) {
      return Objects.equals(this.lastLookedAtUuid, param0.uuid);
   }

   private boolean isPlayerCloseEnoughToMob(BrainDebugRenderer.BrainDump param0) {
      Player var2 = this.minecraft.player;
      BlockPos var3 = BlockPos.containing(var2.getX(), param0.pos.y(), var2.getZ());
      BlockPos var4 = BlockPos.containing(param0.pos);
      return var3.closerThan(var4, 30.0);
   }

   private Collection<UUID> getTicketHolders(BlockPos param0) {
      return this.brainDumpsPerEntity
         .values()
         .stream()
         .filter(l_param1 -> l_param1.hasPoi(param0))
         .map(BrainDebugRenderer.BrainDump::getUuid)
         .collect(Collectors.toSet());
   }

   private Collection<UUID> getPotentialTicketHolders(BlockPos param0) {
      return this.brainDumpsPerEntity
         .values()
         .stream()
         .filter(l_param1 -> l_param1.hasPotentialPoi(param0))
         .map(BrainDebugRenderer.BrainDump::getUuid)
         .collect(Collectors.toSet());
   }

   private Map<BlockPos, List<String>> getGhostPois() {
      Map<BlockPos, List<String>> var1 = Maps.newHashMap();

      for (BrainDebugRenderer.BrainDump var3 : this.brainDumpsPerEntity.values()) {
         for (BlockPos var5 : Iterables.concat(var3.pois, var3.potentialPois)) {
            if (!this.pois.containsKey(var5)) {
               var1.computeIfAbsent(var5, l_param0 -> Lists.newArrayList()).add(var3.name);
            }
         }
      }

      return var1;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(l_param0 -> this.lastLookedAtUuid = l_param0.getUUID());
   }

   public static class BrainDump {
      public final UUID uuid;
      public final int id;
      public final String name;
      public final String profession;
      public final int xp;
      public final float health;
      public final float maxHealth;
      public final Position pos;
      public final String inventory;
      public final Path path;
      public final boolean wantsGolem;
      public final int angerLevel;
      public final List<String> activities = Lists.newArrayList();
      public final List<String> behaviors = Lists.newArrayList();
      public final List<String> memories = Lists.newArrayList();
      public final List<String> gossips = Lists.newArrayList();
      public final Set<BlockPos> pois = Sets.newHashSet();
      public final Set<BlockPos> potentialPois = Sets.newHashSet();

      public BrainDump(
         UUID param0,
         int param1,
         String param2,
         String param3,
         int param4,
         float param5,
         float param6,
         Position param7,
         String param8,
         @Nullable Path param9,
         boolean param10,
         int param11
      ) {
         this.uuid = param0;
         this.id = param1;
         this.name = param2;
         this.profession = param3;
         this.xp = param4;
         this.health = param5;
         this.maxHealth = param6;
         this.pos = param7;
         this.inventory = param8;
         this.path = param9;
         this.wantsGolem = param10;
         this.angerLevel = param11;
      }

      boolean hasPoi(BlockPos param0) {
         return this.pois.stream().anyMatch(param0::equals);
      }

      boolean hasPotentialPoi(BlockPos param0) {
         return this.potentialPois.contains(param0);
      }

      public UUID getUuid() {
         return this.uuid;
      }
   }

   public static class PoiInfo {
      public final BlockPos pos;
      public String type;
      public int freeTicketCount;

      public PoiInfo(BlockPos param0, String param1, int param2) {
         this.pos = param0;
         this.type = param1;
         this.freeTicketCount = param2;
      }
   }
}
