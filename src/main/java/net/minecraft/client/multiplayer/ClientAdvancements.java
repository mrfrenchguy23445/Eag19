package net.minecraft.client.multiplayer;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class ClientAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final AdvancementList advancements = new AdvancementList();
   private final Map<Advancement, AdvancementProgress> progress = Maps.newHashMap();
   @Nullable
   private ClientAdvancements.Listener listener;
   @Nullable
   private Advancement selectedTab;

   public ClientAdvancements(Minecraft param0) {
      this.minecraft = param0;
   }

   public void update(ClientboundUpdateAdvancementsPacket param0) {
      if (param0.shouldReset()) {
         this.advancements.clear();
         this.progress.clear();
      }

      this.advancements.remove(param0.getRemoved());
      this.advancements.add(param0.getAdded());

      for (Entry<ResourceLocation, AdvancementProgress> var3 : param0.getProgress().entrySet()) {
         Advancement var4 = this.advancements.get(var3.getKey());
         if (var4 != null) {
            AdvancementProgress var5 = var3.getValue();
            var5.update(var4.getCriteria(), var4.getRequirements());
            this.progress.put(var4, var5);
            if (this.listener != null) {
               this.listener.onUpdateAdvancementProgress(var4, var5);
            }

            if (!param0.shouldReset() && var5.isDone() && var4.getDisplay() != null && var4.getDisplay().shouldShowToast()) {
               this.minecraft.getToasts().addToast(new AdvancementToast(var4));
            }
         } else {
            LOGGER.warn("Server informed client about progress for unknown advancement {}", var3.getKey());
         }
      }
   }

   public AdvancementList getAdvancements() {
      return this.advancements;
   }

   public void setSelectedTab(@Nullable Advancement param0, boolean param1) {
      ClientPacketListener var3 = this.minecraft.getConnection();
      if (var3 != null && param0 != null && param1) {
         var3.send(ServerboundSeenAdvancementsPacket.openedTab(param0));
      }

      if (this.selectedTab != param0) {
         this.selectedTab = param0;
         if (this.listener != null) {
            this.listener.onSelectedTabChanged(param0);
         }
      }
   }

   public void setListener(@Nullable ClientAdvancements.Listener param0) {
      this.listener = param0;
      this.advancements.setListener(param0);
      if (param0 != null) {
         for (Entry<Advancement, AdvancementProgress> var3 : this.progress.entrySet()) {
            param0.onUpdateAdvancementProgress(var3.getKey(), var3.getValue());
         }

         param0.onSelectedTabChanged(this.selectedTab);
      }
   }

   public interface Listener extends AdvancementList.Listener {
      void onUpdateAdvancementProgress(Advancement var1, AdvancementProgress var2);

      void onSelectedTabChanged(@Nullable Advancement var1);
   }
}
