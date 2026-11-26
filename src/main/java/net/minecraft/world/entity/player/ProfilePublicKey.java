package net.minecraft.world.entity.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.Crypt;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureValidator;

public record ProfilePublicKey(ProfilePublicKey.Data data) {
   public static final Component EXPIRED_PROFILE_PUBLIC_KEY = Component.translatable("multiplayer.disconnect.expired_public_key");
   private static final Component INVALID_SIGNATURE = Component.translatable("multiplayer.disconnect.invalid_public_key_signature");
   public static final Duration EXPIRY_GRACE_PERIOD = Duration.ofHours(8L);
   public static final Codec<ProfilePublicKey> TRUSTED_CODEC = ProfilePublicKey.Data.CODEC.xmap(ProfilePublicKey::new, ProfilePublicKey::data);

   public static ProfilePublicKey createValidated(SignatureValidator param0, UUID param1, ProfilePublicKey.Data param2, Duration param3) throws ProfilePublicKey.ValidationException {
      if (param2.hasExpired(param3)) {
         throw new ProfilePublicKey.ValidationException(EXPIRED_PROFILE_PUBLIC_KEY);
      } else if (!param2.validateSignature(param0, param1)) {
         throw new ProfilePublicKey.ValidationException(INVALID_SIGNATURE);
      } else {
         return new ProfilePublicKey(param2);
      }
   }

   public SignatureValidator createSignatureValidator() {
      return SignatureValidator.from(this.data.key, "SHA256withRSA");
   }

   public record Data(Instant expiresAt, PublicKey key, byte[] keySignature) {
      private static final int MAX_KEY_SIGNATURE_SIZE = 4096;
      public static final Codec<ProfilePublicKey.Data> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               ExtraCodecs.INSTANT_ISO8601.fieldOf("expires_at").forGetter(ProfilePublicKey.Data::expiresAt),
               Crypt.PUBLIC_KEY_CODEC.fieldOf("key").forGetter(ProfilePublicKey.Data::key),
               ExtraCodecs.BASE64_STRING.fieldOf("signature_v2").forGetter(ProfilePublicKey.Data::keySignature)
            )
            .apply(param0, ProfilePublicKey.Data::new)
      );

      public Data(FriendlyByteBuf param0) {
         this(param0.readInstant(), param0.readPublicKey(), param0.readByteArray(4096));
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeInstant(this.expiresAt);
         param0.writePublicKey(this.key);
         param0.writeByteArray(this.keySignature);
      }

      boolean validateSignature(SignatureValidator param0, UUID param1) {
         return param0.validate(this.signedPayload(param1), this.keySignature);
      }

      private byte[] signedPayload(UUID param0) {
         byte[] var2 = this.key.getEncoded();
         byte[] var3 = new byte[24 + var2.length];
         ByteBuffer var4 = ByteBuffer.wrap(var3).order(ByteOrder.BIG_ENDIAN);
         var4.putLong(param0.getMostSignificantBits()).putLong(param0.getLeastSignificantBits()).putLong(this.expiresAt.toEpochMilli()).put(var2);
         return var3;
      }

      public boolean hasExpired() {
         return this.expiresAt.isBefore(Instant.now());
      }

      public boolean hasExpired(Duration param0) {
         return this.expiresAt.plus(param0).isBefore(Instant.now());
      }

      @Override
      public boolean equals(Object param0) {
         return !(param0 instanceof ProfilePublicKey.Data var2)
            ? false
            : this.expiresAt.equals(var2.expiresAt) && this.key.equals(var2.key) && Arrays.equals(this.keySignature, var2.keySignature);
      }
   }

   public static class ValidationException extends ThrowingComponent {
      public ValidationException(Component param0) {
         super(param0);
      }
   }
}
