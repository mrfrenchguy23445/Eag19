package net.minecraft.client.resources.metadata.texture;

import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public class TextureMetadataSectionSerializer implements MetadataSectionSerializer<TextureMetadataSection> {
   public TextureMetadataSection fromJson(JsonObject param0) {
      boolean var2 = GsonHelper.getAsBoolean(param0, "blur", false);
      boolean var3 = GsonHelper.getAsBoolean(param0, "clamp", false);
      return new TextureMetadataSection(var2, var3);
   }

   @Override
   public String getMetadataSectionName() {
      return "texture";
   }
}
