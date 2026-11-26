package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
   private final ResourceLocation id;
   private final Set<UUID> players = Sets.newHashSet();
   private int value;
   private int max = 100;

   public CustomBossEvent(ResourceLocation param0, Component param1) {
      super(param1, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
      this.id = param0;
      this.setProgress(0.0F);
   }

   public ResourceLocation getTextId() {
      return this.id;
   }

   @Override
   public void addPlayer(ServerPlayer param0) {
      super.addPlayer(param0);
      this.players.add(param0.getUUID());
   }

   public void addOfflinePlayer(UUID param0) {
      this.players.add(param0);
   }

   @Override
   public void removePlayer(ServerPlayer param0) {
      super.removePlayer(param0);
      this.players.remove(param0.getUUID());
   }

   @Override
   public void removeAllPlayers() {
      super.removeAllPlayers();
      this.players.clear();
   }

   public int getValue() {
      return this.value;
   }

   public int getMax() {
      return this.max;
   }

   public void setValue(int param0) {
      this.value = param0;
      this.setProgress(Mth.clamp((float)param0 / this.max, 0.0F, 1.0F));
   }

   public void setMax(int param0) {
      this.max = param0;
      this.setProgress(Mth.clamp((float)this.value / param0, 0.0F, 1.0F));
   }

   public final Component getDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.getName())
         .withStyle(
            l_param0 -> l_param0.withColor(this.getColor().getFormatting())
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getTextId().toString())))
               .withInsertion(this.getTextId().toString())
         );
   }

   public boolean setPlayers(Collection<ServerPlayer> param0) {
      Set<UUID> var2 = Sets.newHashSet();
      Set<ServerPlayer> var3 = Sets.newHashSet();

      for (UUID var5 : this.players) {
         boolean var6 = false;

         for (ServerPlayer var8 : param0) {
            if (var8.getUUID().equals(var5)) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            var2.add(var5);
         }
      }

      for (ServerPlayer var5 : param0) {
         boolean var6 = false;

         for (UUID var8 : this.players) {
            if (var5.getUUID().equals(var8)) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            var3.add(var5);
         }
      }

      for (UUID var5 : var2) {
         for (ServerPlayer var7 : this.getPlayers()) {
            if (var7.getUUID().equals(var5)) {
               this.removePlayer(var7);
               break;
            }
         }

         this.players.remove(var5);
      }

      for (ServerPlayer var5 : var3) {
         this.addPlayer(var5);
      }

      return !var2.isEmpty() || !var3.isEmpty();
   }

   public CompoundTag save() {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", Component.Serializer.toJson(this.name));
      var1.putBoolean("Visible", this.isVisible());
      var1.putInt("Value", this.value);
      var1.putInt("Max", this.max);
      var1.putString("Color", this.getColor().getName());
      var1.putString("Overlay", this.getOverlay().getName());
      var1.putBoolean("DarkenScreen", this.shouldDarkenScreen());
      var1.putBoolean("PlayBossMusic", this.shouldPlayBossMusic());
      var1.putBoolean("CreateWorldFog", this.shouldCreateWorldFog());
      ListTag var2 = new ListTag();

      for (UUID var4 : this.players) {
         var2.add(NbtUtils.createUUID(var4));
      }

      var1.put("Players", var2);
      return var1;
   }

   public static CustomBossEvent load(CompoundTag param0, ResourceLocation param1) {
      CustomBossEvent var2 = new CustomBossEvent(param1, Component.Serializer.fromJson(param0.getString("Name")));
      var2.setVisible(param0.getBoolean("Visible"));
      var2.setValue(param0.getInt("Value"));
      var2.setMax(param0.getInt("Max"));
      var2.setColor(BossEvent.BossBarColor.byName(param0.getString("Color")));
      var2.setOverlay(BossEvent.BossBarOverlay.byName(param0.getString("Overlay")));
      var2.setDarkenScreen(param0.getBoolean("DarkenScreen"));
      var2.setPlayBossMusic(param0.getBoolean("PlayBossMusic"));
      var2.setCreateWorldFog(param0.getBoolean("CreateWorldFog"));
      ListTag var3 = param0.getList("Players", 11);

      for (int var4 = 0; var4 < var3.size(); var4++) {
         var2.addOfflinePlayer(NbtUtils.loadUUID(var3.get(var4)));
      }

      return var2;
   }

   public void onPlayerConnect(ServerPlayer param0) {
      if (this.players.contains(param0.getUUID())) {
         this.addPlayer(param0);
      }
   }

   public void onPlayerDisconnect(ServerPlayer param0) {
      super.removePlayer(param0);
   }
}
