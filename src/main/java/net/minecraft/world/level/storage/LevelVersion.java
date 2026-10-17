package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SharedConstants;

public class LevelVersion {
   private final int levelDataVersion;
   private final long lastPlayed;
   private final String minecraftVersionName;
   private final DataVersion minecraftVersion;
   private final boolean snapshot;

   private LevelVersion(int param0, long param1, String param3, int param4, String param5, boolean param6) {
      this.levelDataVersion = param0;
      this.lastPlayed = param1;
      this.minecraftVersionName = param3;
      this.minecraftVersion = new DataVersion(param4, param5);
      this.snapshot = param6;
   }

   public static LevelVersion parse(Dynamic<?> param0) {
      int var1 = param0.get("version").asInt(0);
      long var2 = param0.get("LastPlayed").asLong(0L);
      OptionalDynamic<?> var4 = param0.get("Version");
      return var4.result().isPresent()
         ? new LevelVersion(
            var1,
            var2,
            var4.get("Name").asString(SharedConstants.getCurrentVersion().getName()),
            var4.get("Id").asInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion()),
            var4.get("Series").asString(DataVersion.MAIN_SERIES),
            var4.get("Snapshot").asBoolean(!SharedConstants.getCurrentVersion().isStable())
         )
         : new LevelVersion(var1, var2, "", 0, DataVersion.MAIN_SERIES, false);
   }

   public int levelDataVersion() {
      return this.levelDataVersion;
   }

   public long lastPlayed() {
      return this.lastPlayed;
   }

   public String minecraftVersionName() {
      return this.minecraftVersionName;
   }

   public DataVersion minecraftVersion() {
      return this.minecraftVersion;
   }

   public boolean snapshot() {
      return this.snapshot;
   }
}
