package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import javax.annotation.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
   public IpBanList(File param0) {
      super(param0);
   }

   @Override
   protected StoredUserEntry<String> createEntry(JsonObject param0) {
      return new IpBanListEntry(param0);
   }

   public boolean isBanned(SocketAddress param0) {
      String var2 = this.getIpFromAddress(param0);
      return this.contains(var2);
   }

   public boolean isBanned(String param0) {
      return this.contains(param0);
   }

   @Nullable
   public IpBanListEntry get(SocketAddress param0) {
      String var2 = this.getIpFromAddress(param0);
      return this.get(var2);
   }

   private String getIpFromAddress(SocketAddress param0) {
      String var2 = param0.toString();
      if (var2.contains("/")) {
         var2 = var2.substring(var2.indexOf(47) + 1);
      }

      if (var2.contains(":")) {
         var2 = var2.substring(0, var2.indexOf(58));
      }

      return var2;
   }
}
