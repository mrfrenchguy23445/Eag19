package com.mojang.realmsclient.util;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.slf4j.Logger;

public class RealmsPersistence {
   private static final String FILE_NAME = "realms_persistence.json";
   private static final GuardedSerializer GSON = new GuardedSerializer();
   private static final Logger LOGGER = LogUtils.getLogger();

   public RealmsPersistence.RealmsPersistenceData read() {
      return readFile();
   }

   public void save(RealmsPersistence.RealmsPersistenceData param0) {
      writeFile(param0);
   }

   public static RealmsPersistence.RealmsPersistenceData readFile() {
      File var0 = getPathToData();

      try {
         String var1 = FileUtils.readFileToString(var0, StandardCharsets.UTF_8);
         RealmsPersistence.RealmsPersistenceData var2 = GSON.fromJson(var1, RealmsPersistence.RealmsPersistenceData.class);
         if (var2 != null) {
            return var2;
         }
      } catch (FileNotFoundException var3) {
      } catch (Exception var1) {
         LOGGER.warn("Failed to read Realms storage {}", var0, var1);
      }

      return new RealmsPersistence.RealmsPersistenceData();
   }

   public static void writeFile(RealmsPersistence.RealmsPersistenceData param0) {
      File var1 = getPathToData();

      try {
         FileUtils.writeStringToFile(var1, GSON.toJson(param0), StandardCharsets.UTF_8);
      } catch (IOException var3) {
      }
   }

   private static File getPathToData() {
      return new File(Minecraft.getInstance().gameDirectory, "realms_persistence.json");
   }

   public static class RealmsPersistenceData implements ReflectionBasedSerialization {
      @SerializedName("newsLink")
      public String newsLink;
      @SerializedName("hasUnreadNews")
      public boolean hasUnreadNews;
   }
}
