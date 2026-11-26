package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class DimensionDataStorage {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, SavedData> cache = Maps.newHashMap();
   private final DataFixer fixerUpper;
   private final File dataFolder;

   public DimensionDataStorage(File param0, DataFixer param1) {
      this.fixerUpper = param1;
      this.dataFolder = param0;
   }

   private File getDataFile(String param0) {
      return new File(this.dataFolder, param0 + ".dat");
   }

   public <T extends SavedData> T computeIfAbsent(Function<CompoundTag, T> param0, Supplier<T> param1, String param2) {
      T var4 = this.get(param0, param2);
      if (var4 != null) {
         return var4;
      }

      T var5 = (T)param1.get();
      this.set(param2, var5);
      return var5;
   }

   @Nullable
   public <T extends SavedData> T get(Function<CompoundTag, T> param0, String param1) {
      SavedData var3 = this.cache.get(param1);
      if (var3 == null && !this.cache.containsKey(param1)) {
         var3 = this.readSavedData(param0, param1);
         this.cache.put(param1, var3);
      }

      return (T)var3;
   }

   @Nullable
   private <T extends SavedData> T readSavedData(Function<CompoundTag, T> param0, String param1) {
      try {
         File var3 = this.getDataFile(param1);
         if (var3.exists()) {
            CompoundTag var4 = this.readTagFromDisk(param1, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            return param0.apply(var4.getCompound("data"));
         }
      } catch (Exception var3) {
         LOGGER.error("Error loading saved data: {}", param1, var3);
      }

      return null;
   }

   public void set(String param0, SavedData param1) {
      this.cache.put(param0, param1);
   }

   public CompoundTag readTagFromDisk(String param0, int param1) throws IOException {
      File var3 = this.getDataFile(param0);

      try (
         FileInputStream var4 = new FileInputStream(var3);
         PushbackInputStream var5 = new PushbackInputStream(var4, 2);
      ) {
         CompoundTag var6;
         if (this.isGzip(var5)) {
            var6 = NbtIo.readCompressed(var5);
         } else {
            try (DataInputStream var7 = new DataInputStream(var5)) {
               var6 = NbtIo.read(var7);
            }
         }

         int var7 = NbtUtils.getDataVersion(var6, 1343);
         return DataFixTypes.SAVED_DATA.update(this.fixerUpper, var6, var7, param1);
      }
   }

   private boolean isGzip(PushbackInputStream param0) throws IOException {
      byte[] var2 = new byte[2];
      boolean var3 = false;
      int var4 = param0.read(var2, 0, 2);
      if (var4 == 2) {
         int var5 = (var2[1] & 255) << 8 | var2[0] & 255;
         if (var5 == 35615) {
            var3 = true;
         }
      }

      if (var4 != 0) {
         param0.unread(var2, 0, var4);
      }

      return var3;
   }

   public void save() {
      this.cache.forEach((l_param0, l_param1) -> {
         if (l_param1 != null) {
            l_param1.save(this.getDataFile(l_param0));
         }
      });
   }
}
