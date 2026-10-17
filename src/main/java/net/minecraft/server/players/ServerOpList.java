package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class ServerOpList extends StoredUserList<GameProfile, ServerOpListEntry> {
   public ServerOpList(File param0) {
      super(param0);
   }

   @Override
   protected StoredUserEntry<GameProfile> createEntry(JsonObject param0) {
      return new ServerOpListEntry(param0);
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(GameProfile::getName).toArray(String[]::new);
   }

   public boolean canBypassPlayerLimit(GameProfile param0) {
      ServerOpListEntry var2 = this.get(param0);
      return var2 != null ? var2.getBypassesPlayerLimit() : false;
   }

   protected String getKeyForUser(GameProfile param0) {
      return param0.getId().toString();
   }
}
