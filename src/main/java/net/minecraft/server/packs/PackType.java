package net.minecraft.server.packs;

public enum PackType {
   CLIENT_RESOURCES("assets"),
   SERVER_DATA("data");

   private final String directory;

   PackType(String param2) {
      this.directory = param2;
   }

   public String getDirectory() {
      return this.directory;
   }
}
