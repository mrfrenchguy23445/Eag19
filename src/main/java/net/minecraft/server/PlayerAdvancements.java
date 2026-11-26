package net.minecraft.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class PlayerAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(AdvancementProgress.class, new AdvancementProgress.Serializer())
      .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
      .setPrettyPrinting()
      .create();
   // Port note: upstream writes new TypeToken<Map<ResourceLocation, AdvancementProgress>>() {},
   // which recovers its type argument from Class.getGenericSuperclass(). That method is absent
   // from this target's TeaVM classlib, so the anonymous form cannot work; the explicit factory
   // carries the same information. See com.google.gson.reflect.TypeToken.
   private static final TypeToken<Map<ResourceLocation, AdvancementProgress>> TYPE_TOKEN = TypeToken.getParameterized(Map.class, ResourceLocation.class, AdvancementProgress.class);
   private final DataFixer dataFixer;
   private final PlayerList playerList;
   private final Path playerSavePath;
   private final Map<Advancement, AdvancementProgress> progress = new LinkedHashMap<>();
   private final Set<Advancement> visible = new HashSet<>();
   private final Set<Advancement> progressChanged = new HashSet<>();
   private final Set<Advancement> rootsToUpdate = new HashSet<>();
   private ServerPlayer player;
   @Nullable
   private Advancement lastSelectedTab;
   private boolean isFirstPacket = true;

   public PlayerAdvancements(DataFixer param0, PlayerList param1, ServerAdvancementManager param2, Path param3, ServerPlayer param4) {
      this.dataFixer = param0;
      this.playerList = param1;
      this.playerSavePath = param3;
      this.player = param4;
      this.load(param2);
   }

   public void setPlayer(ServerPlayer param0) {
      this.player = param0;
   }

   public void stopListening() {
      for (CriterionTrigger<?> var2 : CriteriaTriggers.all()) {
         var2.removePlayerListeners(this);
      }
   }

   public void reload(ServerAdvancementManager param0) {
      this.stopListening();
      this.progress.clear();
      this.visible.clear();
      this.rootsToUpdate.clear();
      this.progressChanged.clear();
      this.isFirstPacket = true;
      this.lastSelectedTab = null;
      this.load(param0);
   }

   private void registerListeners(ServerAdvancementManager param0) {
      for (Advancement var3 : param0.getAllAdvancements()) {
         this.registerListeners(var3);
      }
   }

   private void checkForAutomaticTriggers(ServerAdvancementManager param0) {
      for (Advancement var3 : param0.getAllAdvancements()) {
         if (var3.getCriteria().isEmpty()) {
            this.award(var3, "");
            var3.getRewards().grant(this.player);
         }
      }
   }

   private void load(ServerAdvancementManager param0) {
      if (Files.isRegularFile(this.playerSavePath)) {
         try (JsonReader var2 = new JsonReader(Files.newBufferedReader(this.playerSavePath, StandardCharsets.UTF_8))) {
            var2.setLenient(false);
            Dynamic<JsonElement> var3 = new Dynamic<>(JsonOps.INSTANCE, Streams.parse(var2));
            int var4 = var3.get("DataVersion").asInt(1343);
            var3 = var3.remove("DataVersion");
            var3 = DataFixTypes.ADVANCEMENTS.updateToCurrentVersion(this.dataFixer, var3, var4);
            Map<ResourceLocation, AdvancementProgress> var5 = GSON.getAdapter(TYPE_TOKEN).fromJsonTree(var3.getValue());
            if (var5 == null) {
               throw new JsonParseException("Found null for advancements");
            }

            var5.entrySet().stream().sorted(Entry.comparingByValue()).forEach(l_param1 -> {
               Advancement l_var3 = param0.getAdvancement(l_param1.getKey());
               if (l_var3 == null) {
                  LOGGER.warn("Ignored advancement '{}' in progress file {} - it doesn't exist anymore?", l_param1.getKey(), this.playerSavePath);
               } else {
                  this.startProgress(l_var3, l_param1.getValue());
                  this.progressChanged.add(l_var3);
                  this.markForVisibilityUpdate(l_var3);
               }
            });
         } catch (JsonParseException var2) {
            LOGGER.error("Couldn't parse player advancements in {}", this.playerSavePath, var2);
         } catch (IOException var2) {
            LOGGER.error("Couldn't access player advancements in {}", this.playerSavePath, var2);
         }
      }

      this.checkForAutomaticTriggers(param0);
      this.registerListeners(param0);
   }

   public void save() {
      Map<ResourceLocation, AdvancementProgress> var1 = new LinkedHashMap<>();

      for (Entry<Advancement, AdvancementProgress> var3 : this.progress.entrySet()) {
         AdvancementProgress var4 = var3.getValue();
         if (var4.hasProgress()) {
            var1.put(var3.getKey().getId(), var4);
         }
      }

      JsonElement var2 = GSON.toJsonTree(var1);
      var2.getAsJsonObject().addProperty("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

      try {
         FileUtil.createDirectoriesSafe(this.playerSavePath.getParent());

         try (Writer var3 = Files.newBufferedWriter(this.playerSavePath, StandardCharsets.UTF_8)) {
            GSON.toJson(var2, var3);
         }
      } catch (IOException var3) {
         LOGGER.error("Couldn't save player advancements to {}", this.playerSavePath, var3);
      }
   }

   public boolean award(Advancement param0, String param1) {
      boolean var3 = false;
      AdvancementProgress var4 = this.getOrStartProgress(param0);
      boolean var5 = var4.isDone();
      if (var4.grantProgress(param1)) {
         this.unregisterListeners(param0);
         this.progressChanged.add(param0);
         var3 = true;
         if (!var5 && var4.isDone()) {
            param0.getRewards().grant(this.player);
            if (param0.getDisplay() != null
               && param0.getDisplay().shouldAnnounceChat()
               && this.player.level.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
               this.playerList
                  .broadcastSystemMessage(
                     Component.translatable(
                        "chat.type.advancement." + param0.getDisplay().getFrame().getName(), this.player.getDisplayName(), param0.getChatComponent()
                     ),
                     false
                  );
            }
         }
      }

      if (!var5 && var4.isDone()) {
         this.markForVisibilityUpdate(param0);
      }

      return var3;
   }

   public boolean revoke(Advancement param0, String param1) {
      boolean var3 = false;
      AdvancementProgress var4 = this.getOrStartProgress(param0);
      boolean var5 = var4.isDone();
      if (var4.revokeProgress(param1)) {
         this.registerListeners(param0);
         this.progressChanged.add(param0);
         var3 = true;
      }

      if (var5 && !var4.isDone()) {
         this.markForVisibilityUpdate(param0);
      }

      return var3;
   }

   private void markForVisibilityUpdate(Advancement param0) {
      this.rootsToUpdate.add(param0.getRoot());
   }

   private void registerListeners(Advancement param0) {
      AdvancementProgress var2 = this.getOrStartProgress(param0);
      if (!var2.isDone()) {
         for (Entry<String, Criterion> var4 : param0.getCriteria().entrySet()) {
            CriterionProgress var5 = var2.getCriterion(var4.getKey());
            if (var5 != null && !var5.isDone()) {
               CriterionTriggerInstance var6 = var4.getValue().getTrigger();
               if (var6 != null) {
                  CriterionTrigger<CriterionTriggerInstance> var7 = CriteriaTriggers.getCriterion(var6.getCriterion());
                  if (var7 != null) {
                     var7.addPlayerListener(this, new CriterionTrigger.Listener<>(var6, param0, var4.getKey()));
                  }
               }
            }
         }
      }
   }

   private void unregisterListeners(Advancement param0) {
      AdvancementProgress var2 = this.getOrStartProgress(param0);

      for (Entry<String, Criterion> var4 : param0.getCriteria().entrySet()) {
         CriterionProgress var5 = var2.getCriterion(var4.getKey());
         if (var5 != null && (var5.isDone() || var2.isDone())) {
            CriterionTriggerInstance var6 = var4.getValue().getTrigger();
            if (var6 != null) {
               CriterionTrigger<CriterionTriggerInstance> var7 = CriteriaTriggers.getCriterion(var6.getCriterion());
               if (var7 != null) {
                  var7.removePlayerListener(this, new CriterionTrigger.Listener<>(var6, param0, var4.getKey()));
               }
            }
         }
      }
   }

   public void flushDirty(ServerPlayer param0) {
      if (this.isFirstPacket || !this.rootsToUpdate.isEmpty() || !this.progressChanged.isEmpty()) {
         Map<ResourceLocation, AdvancementProgress> var2 = new HashMap<>();
         Set<Advancement> var3 = new HashSet<>();
         Set<ResourceLocation> var4 = new HashSet<>();

         for (Advancement var6 : this.rootsToUpdate) {
            this.updateTreeVisibility(var6, var3, var4);
         }

         this.rootsToUpdate.clear();

         for (Advancement var6 : this.progressChanged) {
            if (this.visible.contains(var6)) {
               var2.put(var6.getId(), this.progress.get(var6));
            }
         }

         this.progressChanged.clear();
         if (!var2.isEmpty() || !var3.isEmpty() || !var4.isEmpty()) {
            param0.connection.send(new ClientboundUpdateAdvancementsPacket(this.isFirstPacket, var3, var4, var2));
         }
      }

      this.isFirstPacket = false;
   }

   public void setSelectedTab(@Nullable Advancement param0) {
      Advancement var2 = this.lastSelectedTab;
      if (param0 != null && param0.getParent() == null && param0.getDisplay() != null) {
         this.lastSelectedTab = param0;
      } else {
         this.lastSelectedTab = null;
      }

      if (var2 != this.lastSelectedTab) {
         this.player.connection.send(new ClientboundSelectAdvancementsTabPacket(this.lastSelectedTab == null ? null : this.lastSelectedTab.getId()));
      }
   }

   public AdvancementProgress getOrStartProgress(Advancement param0) {
      AdvancementProgress var2 = this.progress.get(param0);
      if (var2 == null) {
         var2 = new AdvancementProgress();
         this.startProgress(param0, var2);
      }

      return var2;
   }

   private void startProgress(Advancement param0, AdvancementProgress param1) {
      param1.update(param0.getCriteria(), param0.getRequirements());
      this.progress.put(param0, param1);
   }

   private void updateTreeVisibility(Advancement param0, Set<Advancement> param1, Set<ResourceLocation> param2) {
      AdvancementVisibilityEvaluator.evaluateVisibility(param0, l_param0 -> this.getOrStartProgress(l_param0).isDone(), (l_param2, l_param3) -> {
         if (l_param3) {
            if (this.visible.add(l_param2)) {
               param1.add(l_param2);
               if (this.progress.containsKey(l_param2)) {
                  this.progressChanged.add(l_param2);
               }
            }
         } else if (this.visible.remove(l_param2)) {
            param2.add(l_param2.getId());
         }
      });
   }
}
