package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;

public class BlockElementFace {
   public static final int NO_TINT = -1;
   public final Direction cullForDirection;
   public final int tintIndex;
   public final String texture;
   public final BlockFaceUV uv;

   public BlockElementFace(@Nullable Direction param0, int param1, String param2, BlockFaceUV param3) {
      this.cullForDirection = param0;
      this.tintIndex = param1;
      this.texture = param2;
      this.uv = param3;
   }

   protected static class Deserializer implements JsonDeserializer<BlockElementFace> {
      private static final int DEFAULT_TINT_INDEX = -1;

      public BlockElementFace deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         Direction var5 = this.getCullFacing(var4);
         int var6 = this.getTintIndex(var4);
         String var7 = this.getTexture(var4);
         BlockFaceUV var8 = param2.deserialize(var4, BlockFaceUV.class);
         return new BlockElementFace(var5, var6, var7, var8);
      }

      protected int getTintIndex(JsonObject param0) {
         return GsonHelper.getAsInt(param0, "tintindex", -1);
      }

      private String getTexture(JsonObject param0) {
         return GsonHelper.getAsString(param0, "texture");
      }

      @Nullable
      private Direction getCullFacing(JsonObject param0) {
         String var2 = GsonHelper.getAsString(param0, "cullface", "");
         return Direction.byName(var2);
      }
   }
}
