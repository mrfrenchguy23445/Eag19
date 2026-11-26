package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.slf4j.Logger;

public class SignedMessageChain {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private SignedMessageLink nextLink;

   public SignedMessageChain(UUID param0, UUID param1) {
      this.nextLink = SignedMessageLink.root(param0, param1);
   }

   public SignedMessageChain.Encoder encoder(Signer param0) {
      return l_param1 -> {
         SignedMessageLink l_var3 = this.advanceLink();
         return l_var3 == null ? null : new MessageSignature(param0.sign(l2_param2 -> PlayerChatMessage.updateSignature(l2_param2, l_var3, l_param1)));
      };
   }

   public SignedMessageChain.Decoder decoder(ProfilePublicKey param0) {
      SignatureValidator var2 = param0.createSignatureValidator();
      return (l_param2, l_param3) -> {
         SignedMessageLink l_var5 = this.advanceLink();
         if (l_var5 == null) {
            throw new SignedMessageChain.DecodeException(Component.translatable("chat.disabled.chain_broken"), false);
         }

         if (param0.data().hasExpired()) {
            throw new SignedMessageChain.DecodeException(Component.translatable("chat.disabled.expiredProfileKey"), false);
         }

         PlayerChatMessage l_var6 = new PlayerChatMessage(l_var5, l_param2, l_param3, null, FilterMask.PASS_THROUGH);
         if (!l_var6.verify(var2)) {
            throw new SignedMessageChain.DecodeException(Component.translatable("multiplayer.disconnect.unsigned_chat"), true);
         }

         if (l_var6.hasExpiredServer(Instant.now())) {
            LOGGER.warn("Received expired chat: '{}'. Is the client/server system time unsynchronized?", l_param3.content());
         }

         return l_var6;
      };
   }

   @Nullable
   private SignedMessageLink advanceLink() {
      SignedMessageLink var1 = this.nextLink;
      if (var1 != null) {
         this.nextLink = var1.advance();
      }

      return var1;
   }

   public static class DecodeException extends ThrowingComponent {
      private final boolean shouldDisconnect;

      public DecodeException(Component param0, boolean param1) {
         super(param0);
         this.shouldDisconnect = param1;
      }

      public boolean shouldDisconnect() {
         return this.shouldDisconnect;
      }
   }

   @FunctionalInterface
   public interface Decoder {
      SignedMessageChain.Decoder REJECT_ALL = (param0, param1) -> {
         throw new SignedMessageChain.DecodeException(Component.translatable("chat.disabled.missingProfileKey"), false);
      };

      static SignedMessageChain.Decoder unsigned(UUID param0) {
         return (l_param1, l_param2) -> PlayerChatMessage.unsigned(param0, l_param2.content());
      }

      PlayerChatMessage unpack(@Nullable MessageSignature var1, SignedMessageBody var2) throws SignedMessageChain.DecodeException;
   }

   @FunctionalInterface
   public interface Encoder {
      SignedMessageChain.Encoder UNSIGNED = param0 -> null;

      @Nullable
      MessageSignature pack(SignedMessageBody var1);
   }
}
