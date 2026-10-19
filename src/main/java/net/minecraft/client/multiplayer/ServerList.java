package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.lax1dude.eaglercraft.v1_8.internal.IClientConfigAdapter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class ServerList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ProcessorMailbox<Runnable> IO_MAILBOX = ProcessorMailbox.create(Util.backgroundExecutor(), "server-list-io");
   private static final int MAX_HIDDEN_SERVERS = 16;

   /**
    * Local-storage key for the serialised list. The web build has no filesystem,
    * so {@code servers.dat} would silently vanish on every reload; the same NBT
    * payload is instead kept in the browser's local storage, namespaced by the
    * platform layer the same way {@code EaglerProfile} is.
    */
   private static final String STORAGE_KEY = "servers";
   private final Minecraft minecraft;
   private final List<ServerData> serverList = Lists.newArrayList();
   private final List<ServerData> hiddenServerList = Lists.newArrayList();

   public ServerList(Minecraft param0) {
      this.minecraft = param0;
      this.load();
   }

   public void load() {
      try {
         this.serverList.clear();
         this.hiddenServerList.clear();
         byte[] stored = EagRuntime.getStorage(STORAGE_KEY);
         if (stored == null || stored.length == 0) {
            // Never written: this is a fresh install, so show what the host page
            // configured rather than an empty list. The distinction matters --
            // an empty list the player has actually saved must stay empty.
            this.seedConfiguredDefaults();
            return;
         }

         CompoundTag var1;
         try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(stored))) {
            var1 = NbtIo.read(in);
         }
         if (var1 == null) {
            return;
         }

         ListTag var2 = var1.getList("servers", 10);

         for (int var3 = 0; var3 < var2.size(); var3++) {
            CompoundTag var4 = var2.getCompound(var3);
            ServerData var5 = ServerData.read(var4);
            if (var4.getBoolean("hidden")) {
               this.hiddenServerList.add(var5);
            } else {
               this.serverList.add(var5);
            }
         }
      } catch (Exception var1) {
         LOGGER.error("Couldn't load server list", var1);
      }
   }

   /**
    * Populates the list from the host page's {@code servers} config. Failures are
    * non-fatal: a bad config entry must not stop the multiplayer screen opening.
    */
   private void seedConfiguredDefaults() {
      try {
         List<IClientConfigAdapter.DefaultServer> defaults = EagRuntime.getConfiguration().getDefaultServerList();
         if (defaults == null) {
            return;
         }

         for (IClientConfigAdapter.DefaultServer entry : defaults) {
            if (entry == null || entry.addr == null || entry.addr.isEmpty()) {
               continue;
            }
            String name = (entry.name == null || entry.name.isEmpty()) ? entry.addr : entry.name;
            // The third ServerData argument is "isLan", not the config's hideAddress --
            // this port's server entry has no address-hiding concept.
            this.serverList.add(new ServerData(name, entry.addr, false));
         }
      } catch (Throwable t) {
         LOGGER.error("Couldn't load configured default servers", t);
      }
   }

   public void save() {
      try {
         ListTag var1 = new ListTag();

         for (ServerData var3 : this.serverList) {
            CompoundTag var4 = var3.write();
            var4.putBoolean("hidden", false);
            var1.add(var4);
         }

         for (ServerData var3 : this.hiddenServerList) {
            CompoundTag var4 = var3.write();
            var4.putBoolean("hidden", true);
            var1.add(var4);
         }

         CompoundTag var2 = new CompoundTag();
         var2.put("servers", var1);

         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         try (DataOutputStream out = new DataOutputStream(bytes)) {
            NbtIo.write(var2, out);
         }
         EagRuntime.setStorage(STORAGE_KEY, bytes.toByteArray());
      } catch (Exception var1) {
         LOGGER.error("Couldn't save server list", var1);
      }
   }

   public ServerData get(int param0) {
      return this.serverList.get(param0);
   }

   @Nullable
   public ServerData get(String param0) {
      for (ServerData var3 : this.serverList) {
         if (var3.ip.equals(param0)) {
            return var3;
         }
      }

      for (ServerData var3 : this.hiddenServerList) {
         if (var3.ip.equals(param0)) {
            return var3;
         }
      }

      return null;
   }

   @Nullable
   public ServerData unhide(String param0) {
      for (int var2 = 0; var2 < this.hiddenServerList.size(); var2++) {
         ServerData var3 = this.hiddenServerList.get(var2);
         if (var3.ip.equals(param0)) {
            this.hiddenServerList.remove(var2);
            this.serverList.add(var3);
            return var3;
         }
      }

      return null;
   }

   public void remove(ServerData param0) {
      if (!this.serverList.remove(param0)) {
         this.hiddenServerList.remove(param0);
      }
   }

   public void add(ServerData param0, boolean param1) {
      if (param1) {
         this.hiddenServerList.add(0, param0);

         while (this.hiddenServerList.size() > MAX_HIDDEN_SERVERS) {
            this.hiddenServerList.remove(this.hiddenServerList.size() - 1);
         }
      } else {
         this.serverList.add(param0);
      }
   }

   public int size() {
      return this.serverList.size();
   }

   public void swap(int param0, int param1) {
      ServerData var3 = this.get(param0);
      this.serverList.set(param0, this.get(param1));
      this.serverList.set(param1, var3);
      this.save();
   }

   public void replace(int param0, ServerData param1) {
      this.serverList.set(param0, param1);
   }

   private static boolean set(ServerData param0, List<ServerData> param1) {
      for (int var2 = 0; var2 < param1.size(); var2++) {
         ServerData var3 = param1.get(var2);
         if (var3.name.equals(param0.name) && var3.ip.equals(param0.ip)) {
            param1.set(var2, param0);
            return true;
         }
      }

      return false;
   }

   public static void saveSingleServer(ServerData param0) {
      IO_MAILBOX.tell(() -> {
         ServerList l_var1 = new ServerList(Minecraft.getInstance());
         l_var1.load();
         if (!set(param0, l_var1.serverList)) {
            set(param0, l_var1.hiddenServerList);
         }

         l_var1.save();
      });
   }
}
