package net.minecraft.client.multiplayer;

import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.InsecurePublicKeyException.MissingException;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.slf4j.Logger;

public class AccountProfileKeyPairManager implements ProfileKeyPairManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Duration MINIMUM_PROFILE_KEY_REFRESH_INTERVAL = Duration.ofHours(1L);
   private static final Path PROFILE_KEY_PAIR_DIR = Path.of("profilekeys");
   private final UserApiService userApiService;
   private final Path profileKeyPairPath;
   private CompletableFuture<Optional<ProfileKeyPair>> keyPair;
   private Instant nextProfileKeyRefreshTime = Instant.EPOCH;

   public AccountProfileKeyPairManager(UserApiService param0, UUID param1, Path param2) {
      this.userApiService = param0;
      this.profileKeyPairPath = param2.resolve(PROFILE_KEY_PAIR_DIR).resolve(param1 + ".json");
      this.keyPair = CompletableFuture.<Optional<ProfileKeyPair>>supplyAsync(
            () -> this.readProfileKeyPair().filter(l2_param0 -> !l2_param0.publicKey().data().hasExpired()), Util.backgroundExecutor()
         )
         .thenCompose(this::readOrFetchProfileKeyPair);
   }

   @Override
   public CompletableFuture<Optional<ProfileKeyPair>> prepareKeyPair() {
      this.nextProfileKeyRefreshTime = Instant.now().plus(MINIMUM_PROFILE_KEY_REFRESH_INTERVAL);
      this.keyPair = this.keyPair.thenCompose(this::readOrFetchProfileKeyPair);
      return this.keyPair;
   }

   @Override
   public boolean shouldRefreshKeyPair() {
      return this.keyPair.isDone() && Instant.now().isAfter(this.nextProfileKeyRefreshTime)
         ? this.keyPair.join().map(ProfileKeyPair::dueRefresh).orElse(true)
         : false;
   }

   private CompletableFuture<Optional<ProfileKeyPair>> readOrFetchProfileKeyPair(Optional<ProfileKeyPair> param0) {
      return CompletableFuture.supplyAsync(() -> {
         if (param0.isPresent() && !param0.get().dueRefresh()) {
            if (!SharedConstants.IS_RUNNING_IN_IDE) {
               this.writeProfileKeyPair(null);
            }

            return param0;
         } else {
            try {
               ProfileKeyPair l_var2 = this.fetchProfileKeyPair(this.userApiService);
               this.writeProfileKeyPair(l_var2);
               return Optional.of(l_var2);
            } catch (IOException | CryptException | MinecraftClientException l_var2) {
               LOGGER.error("Failed to retrieve profile key pair", l_var2);
               this.writeProfileKeyPair(null);
               return param0;
            }
         }
      }, Util.backgroundExecutor());
   }

   private Optional<ProfileKeyPair> readProfileKeyPair() {
      if (Files.notExists(this.profileKeyPairPath)) {
         return Optional.empty();
      }

      try (BufferedReader var1 = Files.newBufferedReader(this.profileKeyPairPath)) {
         return ProfileKeyPair.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(var1)).result();
      } catch (Exception var1) {
         LOGGER.error("Failed to read profile key pair file {}", this.profileKeyPairPath, var1);
         return Optional.empty();
      }
   }

   private void writeProfileKeyPair(@Nullable ProfileKeyPair param0) {
      try {
         Files.deleteIfExists(this.profileKeyPairPath);
      } catch (IOException var2) {
         LOGGER.error("Failed to delete profile key pair file {}", this.profileKeyPairPath, var2);
      }

      if (param0 != null) {
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            ProfileKeyPair.CODEC.encodeStart(JsonOps.INSTANCE, param0).result().ifPresent(l_param0 -> {
               try {
                  Files.createDirectories(this.profileKeyPairPath.getParent());
                  Files.writeString(this.profileKeyPairPath, l_param0.toString());
               } catch (Exception l_var2) {
                  LOGGER.error("Failed to write profile key pair file {}", this.profileKeyPairPath, l_var2);
               }
            });
         }
      }
   }

   private ProfileKeyPair fetchProfileKeyPair(UserApiService param0) throws CryptException, IOException {
      KeyPairResponse var2 = param0.getKeyPair();
      if (var2 != null) {
         ProfilePublicKey.Data var3 = parsePublicKey(var2);
         return new ProfileKeyPair(Crypt.stringToPemRsaPrivateKey(var2.getPrivateKey()), new ProfilePublicKey(var3), Instant.parse(var2.getRefreshedAfter()));
      } else {
         throw new IOException("Could not retrieve profile key pair");
      }
   }

   private static ProfilePublicKey.Data parsePublicKey(KeyPairResponse param0) throws CryptException {
      if (!Strings.isNullOrEmpty(param0.getPublicKey()) && param0.getPublicKeySignature() != null && param0.getPublicKeySignature().array().length != 0) {
         try {
            Instant var1 = Instant.parse(param0.getExpiresAt());
            PublicKey var2 = Crypt.stringToRsaPublicKey(param0.getPublicKey());
            ByteBuffer var3 = param0.getPublicKeySignature();
            return new ProfilePublicKey.Data(var1, var2, var3.array());
         } catch (DateTimeException | IllegalArgumentException var1) {
            throw new CryptException(var1);
         }
      } else {
         throw new CryptException(new MissingException());
      }
   }
}
