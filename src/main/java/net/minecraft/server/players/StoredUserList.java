package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public abstract class StoredUserList<K, V extends StoredUserEntry<K>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File file;
   private final Map<String, V> map = Maps.newHashMap();

   public StoredUserList(File param0) {
      this.file = param0;
   }

   public File getFile() {
      return this.file;
   }

   public void add(V param0) {
      this.map.put(this.getKeyForUser(param0.getUser()), param0);

      try {
         this.save();
      } catch (IOException var2) {
         LOGGER.warn("Could not save the list after adding a user.", var2);
      }
   }

   @Nullable
   public V get(K param0) {
      this.removeExpired();
      return this.map.get(this.getKeyForUser(param0));
   }

   public void remove(K param0) {
      this.map.remove(this.getKeyForUser(param0));

      try {
         this.save();
      } catch (IOException var2) {
         LOGGER.warn("Could not save the list after removing a user.", var2);
      }
   }

   public void remove(StoredUserEntry<K> param0) {
      this.remove(param0.getUser());
   }

   public String[] getUserList() {
      return this.map.keySet().toArray(new String[0]);
   }

   public boolean isEmpty() {
      return this.map.size() < 1;
   }

   protected String getKeyForUser(K param0) {
      return param0.toString();
   }

   protected boolean contains(K param0) {
      return this.map.containsKey(this.getKeyForUser(param0));
   }

   private void removeExpired() {
      List<K> var1 = Lists.newArrayList();

      for (V var3 : this.map.values()) {
         if (var3.hasExpired()) {
            var1.add(var3.getUser());
         }
      }

      for (K var3 : var1) {
         this.map.remove(this.getKeyForUser(var3));
      }
   }

   protected abstract StoredUserEntry<K> createEntry(JsonObject var1);

   public Collection<V> getEntries() {
      return this.map.values();
   }

   public void save() throws IOException {
      JsonArray var1 = new JsonArray();
      this.map.values().stream().map(l_param0 -> Util.make(new JsonObject(), l_param0::serialize)).forEach(var1::add);

      try (BufferedWriter var2 = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
         GSON.toJson(var1, var2);
      }
   }

   public void load() throws IOException {
      if (this.file.exists()) {
         try (BufferedReader var1 = Files.newReader(this.file, StandardCharsets.UTF_8)) {
            JsonArray var2 = GSON.fromJson(var1, JsonArray.class);
            this.map.clear();

            for (JsonElement var4 : var2) {
               JsonObject var5 = GsonHelper.convertToJsonObject(var4, "entry");
               StoredUserEntry<K> var6 = this.createEntry(var5);
               if (var6.getUser() != null) {
                  this.map.put(this.getKeyForUser(var6.getUser()), (V)var6);
               }
            }
         }
      }
   }
}
