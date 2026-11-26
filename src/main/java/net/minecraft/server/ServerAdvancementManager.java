package net.minecraft.server;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.PredicateManager;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private AdvancementList advancements = new AdvancementList();
   private final PredicateManager predicateManager;

   public ServerAdvancementManager(PredicateManager param0) {
      super(GSON, "advancements");
      this.predicateManager = param0;
   }

   protected void apply(Map<ResourceLocation, JsonElement> param0, ResourceManager param1, ProfilerFiller param2) {
      Map<ResourceLocation, Advancement.Builder> var4 = Maps.newHashMap();
      param0.forEach((l_param1, l_param2) -> {
         try {
            JsonObject l_var4 = GsonHelper.convertToJsonObject(l_param2, "advancement");
            Advancement.Builder l_var5 = Advancement.Builder.fromJson(l_var4, new DeserializationContext(l_param1, this.predicateManager));
            var4.put(l_param1, l_var5);
         } catch (Exception l_var4) {
            LOGGER.error("Parsing error loading custom advancement {}: {}", l_param1, l_var4.getMessage());
         }
      });
      AdvancementList var5 = new AdvancementList();
      var5.add(var4);

      for (Advancement var7 : var5.getRoots()) {
         if (var7.getDisplay() != null) {
            TreeNodePosition.run(var7);
         }
      }

      this.advancements = var5;
   }

   @Nullable
   public Advancement getAdvancement(ResourceLocation param0) {
      return this.advancements.get(param0);
   }

   public Collection<Advancement> getAllAdvancements() {
      return this.advancements.getAllAdvancements();
   }
}
