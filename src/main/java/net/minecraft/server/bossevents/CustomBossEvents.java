package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CustomBossEvents {
   private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

   @Nullable
   public CustomBossEvent get(ResourceLocation param0) {
      return this.events.get(param0);
   }

   public CustomBossEvent create(ResourceLocation param0, Component param1) {
      CustomBossEvent var3 = new CustomBossEvent(param0, param1);
      this.events.put(param0, var3);
      return var3;
   }

   public void remove(CustomBossEvent param0) {
      this.events.remove(param0.getTextId());
   }

   public Collection<ResourceLocation> getIds() {
      return this.events.keySet();
   }

   public Collection<CustomBossEvent> getEvents() {
      return this.events.values();
   }

   public CompoundTag save() {
      CompoundTag var1 = new CompoundTag();

      for (CustomBossEvent var3 : this.events.values()) {
         var1.put(var3.getTextId().toString(), var3.save());
      }

      return var1;
   }

   public void load(CompoundTag param0) {
      for (String var3 : param0.getAllKeys()) {
         ResourceLocation var4 = new ResourceLocation(var3);
         this.events.put(var4, CustomBossEvent.load(param0.getCompound(var3), var4));
      }
   }

   public void onPlayerConnect(ServerPlayer param0) {
      for (CustomBossEvent var3 : this.events.values()) {
         var3.onPlayerConnect(param0);
      }
   }

   public void onPlayerDisconnect(ServerPlayer param0) {
      for (CustomBossEvent var3 : this.events.values()) {
         var3.onPlayerDisconnect(param0);
      }
   }
}
