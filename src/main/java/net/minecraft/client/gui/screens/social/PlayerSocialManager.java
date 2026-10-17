package net.minecraft.client.gui.screens.social;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PlayerSocialManager {
   private final Minecraft minecraft;
   private final Set<UUID> hiddenPlayers = Sets.newHashSet();
   private final UserApiService service;
   private final Map<String, UUID> discoveredNamesToUUID = Maps.newHashMap();
   private boolean onlineMode;
   private CompletableFuture<?> pendingBlockListRefresh = CompletableFuture.completedFuture(null);

   public PlayerSocialManager(Minecraft param0, UserApiService param1) {
      this.minecraft = param0;
      this.service = param1;
   }

   public void hidePlayer(UUID param0) {
      this.hiddenPlayers.add(param0);
   }

   public void showPlayer(UUID param0) {
      this.hiddenPlayers.remove(param0);
   }

   public boolean shouldHideMessageFrom(UUID param0) {
      return this.isHidden(param0) || this.isBlocked(param0);
   }

   public boolean isHidden(UUID param0) {
      return this.hiddenPlayers.contains(param0);
   }

   public void startOnlineMode() {
      this.onlineMode = true;
      this.pendingBlockListRefresh = this.pendingBlockListRefresh.thenRunAsync(this.service::refreshBlockList, Util.ioPool());
   }

   public void stopOnlineMode() {
      this.onlineMode = false;
   }

   public boolean isBlocked(UUID param0) {
      if (!this.onlineMode) {
         return false;
      }

      this.pendingBlockListRefresh.join();
      return this.service.isBlockedPlayer(param0);
   }

   public Set<UUID> getHiddenPlayers() {
      return this.hiddenPlayers;
   }

   public UUID getDiscoveredUUID(String param0) {
      return this.discoveredNamesToUUID.getOrDefault(param0, Util.NIL_UUID);
   }

   public void addPlayer(PlayerInfo param0) {
      GameProfile var2 = param0.getProfile();
      if (var2.isComplete()) {
         this.discoveredNamesToUUID.put(var2.getName(), var2.getId());
      }

      if (this.minecraft.screen instanceof SocialInteractionsScreen var4) {
         var4.onAddPlayer(param0);
      }
   }

   public void removePlayer(UUID param0) {
      if (this.minecraft.screen instanceof SocialInteractionsScreen var3) {
         var3.onRemovePlayer(param0);
      }
   }
}
