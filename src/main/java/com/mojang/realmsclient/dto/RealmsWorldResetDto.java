package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;

public class RealmsWorldResetDto extends ValueObject implements ReflectionBasedSerialization {
   @SerializedName("seed")
   private final String seed;
   @SerializedName("worldTemplateId")
   private final long worldTemplateId;
   @SerializedName("levelType")
   private final int levelType;
   @SerializedName("generateStructures")
   private final boolean generateStructures;

   public RealmsWorldResetDto(String param0, long param1, int param3, boolean param4) {
      this.seed = param0;
      this.worldTemplateId = param1;
      this.levelType = param3;
      this.generateStructures = param4;
   }
}
