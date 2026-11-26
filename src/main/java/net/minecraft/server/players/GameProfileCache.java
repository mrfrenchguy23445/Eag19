package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import org.slf4j.Logger;

public class GameProfileCache {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int GAMEPROFILES_MRU_LIMIT = 1000;
   private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
   private static boolean usesAuthentication;
   private final Map<String, GameProfileCache.GameProfileInfo> profilesByName = Maps.newConcurrentMap();
   private final Map<UUID, GameProfileCache.GameProfileInfo> profilesByUUID = Maps.newConcurrentMap();
   private final Map<String, CompletableFuture<Optional<GameProfile>>> requests = Maps.newConcurrentMap();
   private final GameProfileRepository profileRepository;
   private final Gson gson = new GsonBuilder().create();
   private final File file;
   private final AtomicLong operationCount = new AtomicLong();
   @Nullable
   private Executor executor;

   public GameProfileCache(GameProfileRepository param0, File param1) {
      this.profileRepository = param0;
      this.file = param1;
      Lists.reverse(this.load()).forEach(this::safeAdd);
   }

   private void safeAdd(GameProfileCache.GameProfileInfo param0) {
      GameProfile var2 = param0.getProfile();
      param0.setLastAccess(this.getNextOperation());
      String var3 = var2.getName();
      if (var3 != null) {
         this.profilesByName.put(var3.toLowerCase(Locale.ROOT), param0);
      }

      UUID var4 = var2.getId();
      if (var4 != null) {
         this.profilesByUUID.put(var4, param0);
      }
   }

   private static Optional<GameProfile> lookupGameProfile(GameProfileRepository param0, String param1) {
      final AtomicReference<GameProfile> var2 = new AtomicReference<>();
      ProfileLookupCallback var3 = new ProfileLookupCallback() {
         @Override
         public void onProfileLookupSucceeded(GameProfile l_param0) {
            var2.set(l_param0);
         }

         @Override
         public void onProfileLookupFailed(GameProfile l_param0, Exception l_param1) {
            var2.set(null);
         }
      };
      param0.findProfilesByNames(new String[]{param1}, Agent.MINECRAFT, var3);
      GameProfile var4 = var2.get();
      if (!usesAuthentication() && var4 == null) {
         UUID var5 = UUIDUtil.getOrCreatePlayerUUID(new GameProfile(null, param1));
         return Optional.of(new GameProfile(var5, param1));
      } else {
         return Optional.ofNullable(var4);
      }
   }

   public static void setUsesAuthentication(boolean param0) {
      usesAuthentication = param0;
   }

   private static boolean usesAuthentication() {
      return usesAuthentication;
   }

   public void add(GameProfile param0) {
      Calendar var2 = Calendar.getInstance();
      var2.setTime(new Date());
      var2.add(2, 1);
      Date var3 = var2.getTime();
      GameProfileCache.GameProfileInfo var4 = new GameProfileCache.GameProfileInfo(param0, var3);
      this.safeAdd(var4);
      this.save();
   }

   private long getNextOperation() {
      return this.operationCount.incrementAndGet();
   }

   public Optional<GameProfile> get(String param0) {
      String var2 = param0.toLowerCase(Locale.ROOT);
      GameProfileCache.GameProfileInfo var3 = this.profilesByName.get(var2);
      boolean var4 = false;
      if (var3 != null && new Date().getTime() >= var3.expirationDate.getTime()) {
         this.profilesByUUID.remove(var3.getProfile().getId());
         this.profilesByName.remove(var3.getProfile().getName().toLowerCase(Locale.ROOT));
         var4 = true;
         var3 = null;
      }

      Optional<GameProfile> var5;
      if (var3 != null) {
         var3.setLastAccess(this.getNextOperation());
         var5 = Optional.of(var3.getProfile());
      } else {
         var5 = lookupGameProfile(this.profileRepository, var2);
         if (var5.isPresent()) {
            this.add(var5.get());
            var4 = false;
         }
      }

      if (var4) {
         this.save();
      }

      return var5;
   }

   public void getAsync(String param0, Consumer<Optional<GameProfile>> param1) {
      if (this.executor == null) {
         throw new IllegalStateException("No executor");
      }

      CompletableFuture<Optional<GameProfile>> var3 = this.requests.get(param0);
      if (var3 != null) {
         this.requests.put(param0, var3.whenCompleteAsync((l_param1, l_param2) -> param1.accept((Optional<GameProfile>)l_param1), this.executor));
      } else {
         this.requests
            .put(
               param0,
               CompletableFuture.<Optional<GameProfile>>supplyAsync(() -> this.get(param0), Util.backgroundExecutor())
                  .whenCompleteAsync((l_param1, l_param2) -> this.requests.remove(param0), this.executor)
                  .whenCompleteAsync((l_param1, l_param2) -> param1.accept((Optional<GameProfile>)l_param1), this.executor)
            );
      }
   }

   public Optional<GameProfile> get(UUID param0) {
      GameProfileCache.GameProfileInfo var2 = this.profilesByUUID.get(param0);
      if (var2 == null) {
         return Optional.empty();
      }

      var2.setLastAccess(this.getNextOperation());
      return Optional.of(var2.getProfile());
   }

   public void setExecutor(Executor param0) {
      this.executor = param0;
   }

   public void clearExecutor() {
      this.executor = null;
   }

   private static DateFormat createDateFormat() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   }

   public List<GameProfileCache.GameProfileInfo> load() {
      List<GameProfileCache.GameProfileInfo> var1 = Lists.newArrayList();

      try (Reader var2 = Files.newReader(this.file, StandardCharsets.UTF_8)) {
         JsonArray var3 = this.gson.fromJson(var2, JsonArray.class);
         if (var3 == null) {
            return var1;
         }

         DateFormat var4 = createDateFormat();
         var3.forEach(l_param2 -> readGameProfile(l_param2, var4).ifPresent(var1::add));
      } catch (FileNotFoundException var7) {
      } catch (IOException | JsonParseException var2) {
         LOGGER.warn("Failed to load profile cache {}", this.file, var2);
      }

      return var1;
   }

   public void save() {
      JsonArray var1 = new JsonArray();
      DateFormat var2 = createDateFormat();
      this.getTopMRUProfiles(1000).forEach(l_param2 -> var1.add(writeGameProfile(l_param2, var2)));
      String var3 = this.gson.toJson(var1);

      try (Writer var4 = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
         var4.write(var3);
      } catch (IOException var9) {
      }
   }

   private Stream<GameProfileCache.GameProfileInfo> getTopMRUProfiles(int param0) {
      return ImmutableList.copyOf(this.profilesByUUID.values())
         .stream()
         .sorted(Comparator.comparing(GameProfileCache.GameProfileInfo::getLastAccess).reversed())
         .limit(param0);
   }

   private static JsonElement writeGameProfile(GameProfileCache.GameProfileInfo param0, DateFormat param1) {
      JsonObject var2 = new JsonObject();
      var2.addProperty("name", param0.getProfile().getName());
      UUID var3 = param0.getProfile().getId();
      var2.addProperty("uuid", var3 == null ? "" : var3.toString());
      var2.addProperty("expiresOn", param1.format(param0.getExpirationDate()));
      return var2;
   }

   private static Optional<GameProfileCache.GameProfileInfo> readGameProfile(JsonElement param0, DateFormat param1) {
      if (param0.isJsonObject()) {
         JsonObject var2 = param0.getAsJsonObject();
         JsonElement var3 = var2.get("name");
         JsonElement var4 = var2.get("uuid");
         JsonElement var5 = var2.get("expiresOn");
         if (var3 != null && var4 != null) {
            String var6 = var4.getAsString();
            String var7 = var3.getAsString();
            Date var8 = null;
            if (var5 != null) {
               try {
                  var8 = param1.parse(var5.getAsString());
               } catch (ParseException var12) {
               }
            }

            if (var7 != null && var6 != null && var8 != null) {
               UUID var9;
               try {
                  var9 = UUID.fromString(var6);
               } catch (Throwable var10) {
                  return Optional.empty();
               }

               return Optional.of(new GameProfileCache.GameProfileInfo(new GameProfile(var9, var7), var8));
            } else {
               return Optional.empty();
            }
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   static class GameProfileInfo {
      private final GameProfile profile;
      final Date expirationDate;
      private volatile long lastAccess;

      GameProfileInfo(GameProfile param0, Date param1) {
         this.profile = param0;
         this.expirationDate = param1;
      }

      public GameProfile getProfile() {
         return this.profile;
      }

      public Date getExpirationDate() {
         return this.expirationDate;
      }

      public void setLastAccess(long param0) {
         this.lastAccess = param0;
      }

      public long getLastAccess() {
         return this.lastAccess;
      }
   }
}
