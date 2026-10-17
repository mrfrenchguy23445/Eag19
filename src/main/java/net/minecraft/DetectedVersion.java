package net.minecraft;

import java.util.Date;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.DataVersion;

/** Browser build: hardcoded 1.19.4 version, no version.json / Gson parsing. */
public class DetectedVersion implements WorldVersion {
   public static final WorldVersion BUILT_IN = new DetectedVersion();
   private final String id;
   private final String name;
   private final boolean stable;
   private final DataVersion worldVersion;
   private final int protocolVersion;
   private final int resourcePackVersion;
   private final int dataPackVersion;
   private final Date buildTime;

   private DetectedVersion() {
      this.id = "1.19.4";
      this.name = "1.19.4";
      this.stable = true;
      this.worldVersion = new DataVersion(3337, "main");
      this.protocolVersion = SharedConstants.getProtocolVersion();
      this.resourcePackVersion = 13;
      this.dataPackVersion = 12;
      this.buildTime = new Date();
   }

   public static WorldVersion tryDetectVersion() {
      return BUILT_IN;
   }

   @Override
   public String getId() {
      return this.id;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public DataVersion getDataVersion() {
      return this.worldVersion;
   }

   @Override
   public int getProtocolVersion() {
      return this.protocolVersion;
   }

   @Override
   public int getPackVersion(PackType param0) {
      return param0 == PackType.SERVER_DATA ? this.dataPackVersion : this.resourcePackVersion;
   }

   @Override
   public Date getBuildTime() {
      return this.buildTime;
   }

   @Override
   public boolean isStable() {
      return this.stable;
   }
}