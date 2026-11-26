package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FileToIdConverter {
   private final String prefix;
   private final String extension;

   public FileToIdConverter(String param0, String param1) {
      this.prefix = param0;
      this.extension = param1;
   }

   public static FileToIdConverter json(String param0) {
      return new FileToIdConverter(param0, ".json");
   }

   public ResourceLocation idToFile(ResourceLocation param0) {
      return param0.withPath(this.prefix + "/" + param0.getPath() + this.extension);
   }

   public ResourceLocation fileToId(ResourceLocation param0) {
      String var2 = param0.getPath();
      return param0.withPath(var2.substring(this.prefix.length() + 1, var2.length() - this.extension.length()));
   }

   public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager param0) {
      return param0.listResources(this.prefix, l_param0 -> l_param0.getPath().endsWith(this.extension));
   }

   public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager param0) {
      return param0.listResourceStacks(this.prefix, l_param0 -> l_param0.getPath().endsWith(this.extension));
   }
}
