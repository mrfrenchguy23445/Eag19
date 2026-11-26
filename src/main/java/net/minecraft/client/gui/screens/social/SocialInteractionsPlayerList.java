package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.resources.ResourceLocation;

public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen param0, Minecraft param1, int param2, int param3, int param4, int param5, int param6) {
      super(param1, param2, param3, param4, param5, param6);
      this.socialInteractionsScreen = param0;
      this.setRenderBackground(false);
      this.setRenderTopAndBottom(false);
   }

   @Override
   protected void enableScissor() {
      enableScissor(this.x0, this.y0 + 4, this.x1, this.y1);
   }

   public void updatePlayerList(Collection<UUID> param0, double param1, boolean param3) {
      Map<UUID, PlayerEntry> var5 = new HashMap<>();
      this.addOnlinePlayers(param0, var5);
      this.updatePlayersFromChatLog(var5, param3);
      this.updateFiltersAndScroll(var5.values(), param1);
   }

   private void addOnlinePlayers(Collection<UUID> param0, Map<UUID, PlayerEntry> param1) {
      ClientPacketListener var3 = this.minecraft.player.connection;

      for (UUID var5 : param0) {
         PlayerInfo var6 = var3.getPlayerInfo(var5);
         if (var6 != null) {
            boolean var7 = var6.hasVerifiableChat();
            param1.put(var5, new PlayerEntry(this.minecraft, this.socialInteractionsScreen, var5, var6.getProfile().getName(), var6::getSkinLocation, var7));
         }
      }
   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> param0, boolean param1) {
      for (GameProfile var5 : collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog())) {
         PlayerEntry var6;
         if (param1) {
            var6 = param0.computeIfAbsent(
               var5.getId(),
               l_param1 -> {
                  PlayerEntry l_var3 = new PlayerEntry(
                     this.minecraft,
                     this.socialInteractionsScreen,
                     var5.getId(),
                     var5.getName(),
                     Suppliers.<ResourceLocation>memoize(() -> this.minecraft.getSkinManager().getInsecureSkinLocation(var5)),
                     true
                  );
                  l_var3.setRemoved(true);
                  return l_var3;
               }
            );
         } else {
            var6 = param0.get(var5.getId());
            if (var6 == null) {
               continue;
            }
         }

         var6.setHasRecentMessages(true);
      }
   }

   private static Collection<GameProfile> collectProfilesFromChatLog(ChatLog param0) {
      Set<GameProfile> var1 = new ObjectLinkedOpenHashSet<>();

      for (int var2 = param0.end(); var2 >= param0.start(); var2--) {
         if (param0.lookup(var2) instanceof LoggedChatMessage.Player var4 && var4.message().hasSignature()) {
            var1.add(var4.profile());
         }
      }

      return var1;
   }

   /**
    * The RFC 4122 version nibble of a UUID.
    *
    * <p>Equivalent to {@code java.util.UUID.version()}, which is absent from the WASM-GC TeaVM
    * classlib. Per RFC 4122 section 4.1.3 the version occupies bits 12-15 of the most significant
    * 64 bits, counted from the least significant bit of that long, so it is the nibble at shift 12.
    */
   private static int versionOf(java.util.UUID param0) {
      return (int)((param0.getMostSignificantBits() >> 12) & 15L);
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.<PlayerEntry, Integer>comparing(l_param0 -> {
         if (l_param0.getPlayerId().equals(this.minecraft.getUser().getProfileId())) {
            return 0;
         } else if (SocialInteractionsPlayerList.versionOf(l_param0.getPlayerId()) == 2) {
            return 4;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(l_param0.getPlayerId())) {
            return 1;
         } else {
            return l_param0.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing(l_param0 -> {
         if (!l_param0.getPlayerName().isBlank()) {
            int l_var1 = l_param0.getPlayerName().codePointAt(0);
            if (l_var1 == 95 || l_var1 >= 97 && l_var1 <= 122 || l_var1 >= 65 && l_var1 <= 90 || l_var1 >= 48 && l_var1 <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> param0, double param1) {
      this.players.clear();
      this.players.addAll(param0);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(param1);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf(l_param0 -> !l_param0.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
         this.replaceEntries(this.players);
      }
   }

   public void setFilter(String param0) {
      this.filter = param0;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo param0, SocialInteractionsScreen.Page param1) {
      UUID var3 = param0.getProfile().getId();

      for (PlayerEntry var5 : this.players) {
         if (var5.getPlayerId().equals(var3)) {
            var5.setRemoved(false);
            return;
         }
      }

      if ((param1 == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(var3))
         && (Strings.isNullOrEmpty(this.filter) || param0.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean var4 = param0.hasVerifiableChat();
         PlayerEntry var5 = new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, param0.getProfile().getId(), param0.getProfile().getName(), param0::getSkinLocation, var4
         );
         this.addEntry(var5);
         this.players.add(var5);
      }
   }

   public void removePlayer(UUID param0) {
      for (PlayerEntry var3 : this.players) {
         if (var3.getPlayerId().equals(param0)) {
            var3.setRemoved(true);
            return;
         }
      }
   }
}
