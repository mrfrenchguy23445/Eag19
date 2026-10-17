package net.minecraft.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.properties.Property;
import java.io.File;
import java.util.Collections;
import java.net.InetAddress;
import java.util.Map;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.SignatureValidator;

public record Services(
   MinecraftSessionService sessionService, SignatureValidator serviceSignatureValidator, GameProfileRepository profileRepository, GameProfileCache profileCache
) {
   private static final String USERID_CACHE_FILE = "usercache.json";

   public static Services create(YggdrasilAuthenticationService param0, File param1) {
      MinecraftSessionService var2 = param0.createMinecraftSessionService();
      GameProfileRepository var3 = param0.createProfileRepository();
      GameProfileCache var4 = new GameProfileCache(var3, new File(param1, "usercache.json"));
      SignatureValidator var5 = SignatureValidator.from(param0.getServicesKey());
      return new Services(var2, var5, var3, var4);
   }

   public static Services createOffline(File param0) {
      GameProfileRepository var1 = (param0x, param1, param2) -> {
         for (String var3 : param0x) {
            param2.onProfileLookupFailed(new GameProfile(null, var3), new IllegalStateException("Offline mode"));
         }
      };

      MinecraftSessionService var2 = new MinecraftSessionService() {
         @Override
         public void joinServer(GameProfile param0, String param1, String param2) {
         }

         @Override
         public GameProfile hasJoinedServer(GameProfile param0, String param1, InetAddress param2) {
         return null;
         }

         @Override
         public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile param0, boolean param1) {
            return Collections.emptyMap();
         }

         @Override
         public GameProfile fillProfileProperties(GameProfile param0, boolean param1) {
            return param0;
         }

         @Override
         public String getSecurePropertyValue(Property param0) {
            return param0.getValue();
         }
      };

      GameProfileCache var3 = new GameProfileCache(var1, new File(param0, USERID_CACHE_FILE));

      SignatureValidator var4 = (param0x, param1) -> false;

      return new Services(var2, var4, var1, var3);
   }
}