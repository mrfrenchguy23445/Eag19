package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TerrainAdjustment implements StringRepresentable {
   NONE("none"),
   BURY("bury"),
   BEARD_THIN("beard_thin"),
   BEARD_BOX("beard_box");

   public static final Codec<TerrainAdjustment> CODEC = StringRepresentable.fromEnum(TerrainAdjustment::values);
   private final String id;

   TerrainAdjustment(String param2) {
      this.id = param2;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
