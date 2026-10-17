package net.minecraft.server.packs.metadata.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.GsonHelper;

public class PackMetadataSectionSerializer implements MetadataSectionType<PackMetadataSection> {
   public PackMetadataSection fromJson(JsonObject param0) {
      Component var2 = Component.Serializer.fromJson(param0.get("description"));
      if (var2 == null) {
         throw new JsonParseException("Invalid/missing description!");
      }

      int var3 = GsonHelper.getAsInt(param0, "pack_format");
      return new PackMetadataSection(var2, var3);
   }

   public JsonObject toJson(PackMetadataSection param0) {
      JsonObject var2 = new JsonObject();
      var2.add("description", Component.Serializer.toJsonTree(param0.getDescription()));
      var2.addProperty("pack_format", param0.getPackFormat());
      return var2;
   }

   @Override
   public String getMetadataSectionName() {
      return "pack";
   }
}
