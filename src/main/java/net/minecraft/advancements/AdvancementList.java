package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AdvancementList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<ResourceLocation, Advancement> advancements = Maps.newHashMap();
   private final Set<Advancement> roots = Sets.newLinkedHashSet();
   private final Set<Advancement> tasks = Sets.newLinkedHashSet();
   @Nullable
   private AdvancementList.Listener listener;

   private void remove(Advancement param0) {
      for (Advancement var3 : param0.getChildren()) {
         this.remove(var3);
      }

      LOGGER.info("Forgot about advancement {}", param0.getId());
      this.advancements.remove(param0.getId());
      if (param0.getParent() == null) {
         this.roots.remove(param0);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementRoot(param0);
         }
      } else {
         this.tasks.remove(param0);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementTask(param0);
         }
      }
   }

   public void remove(Set<ResourceLocation> param0) {
      for (ResourceLocation var3 : param0) {
         Advancement var4 = this.advancements.get(var3);
         if (var4 == null) {
            LOGGER.warn("Told to remove advancement {} but I don't know what that is", var3);
         } else {
            this.remove(var4);
         }
      }
   }

   public void add(Map<ResourceLocation, Advancement.Builder> param0) {
      Map<ResourceLocation, Advancement.Builder> var2 = Maps.newHashMap(param0);

      while (!var2.isEmpty()) {
         boolean var3 = false;
         Iterator<Entry<ResourceLocation, Advancement.Builder>> var4 = var2.entrySet().iterator();

         while (var4.hasNext()) {
            Entry<ResourceLocation, Advancement.Builder> var5 = var4.next();
            ResourceLocation var6 = var5.getKey();
            Advancement.Builder var7 = var5.getValue();
            if (var7.canBuild(this.advancements::get)) {
               Advancement var8 = var7.build(var6);
               this.advancements.put(var6, var8);
               var3 = true;
               var4.remove();
               if (var8.getParent() == null) {
                  this.roots.add(var8);
                  if (this.listener != null) {
                     this.listener.onAddAdvancementRoot(var8);
                  }
               } else {
                  this.tasks.add(var8);
                  if (this.listener != null) {
                     this.listener.onAddAdvancementTask(var8);
                  }
               }
            }
         }

         if (!var3) {
            for (Entry<ResourceLocation, Advancement.Builder> var5 : var2.entrySet()) {
               LOGGER.error("Couldn't load advancement {}: {}", var5.getKey(), var5.getValue());
            }
            break;
         }
      }

      LOGGER.info("Loaded {} advancements", this.advancements.size());
   }

   public void clear() {
      this.advancements.clear();
      this.roots.clear();
      this.tasks.clear();
      if (this.listener != null) {
         this.listener.onAdvancementsCleared();
      }
   }

   public Iterable<Advancement> getRoots() {
      return this.roots;
   }

   public Collection<Advancement> getAllAdvancements() {
      return this.advancements.values();
   }

   @Nullable
   public Advancement get(ResourceLocation param0) {
      return this.advancements.get(param0);
   }

   public void setListener(@Nullable AdvancementList.Listener param0) {
      this.listener = param0;
      if (param0 != null) {
         for (Advancement var3 : this.roots) {
            param0.onAddAdvancementRoot(var3);
         }

         for (Advancement var3 : this.tasks) {
            param0.onAddAdvancementTask(var3);
         }
      }
   }

   public interface Listener {
      void onAddAdvancementRoot(Advancement var1);

      void onRemoveAdvancementRoot(Advancement var1);

      void onAddAdvancementTask(Advancement var1);

      void onRemoveAdvancementTask(Advancement var1);

      void onAdvancementsCleared();
   }
}
