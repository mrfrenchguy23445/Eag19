package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.mojang.serialization.Codec;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureUpdater;

public record LastSeenMessages(List<MessageSignature> entries) {
   public static final Codec<LastSeenMessages> CODEC = MessageSignature.CODEC.listOf().xmap(LastSeenMessages::new, LastSeenMessages::entries);
   public static LastSeenMessages EMPTY = new LastSeenMessages(List.of());
   public static final int LAST_SEEN_MESSAGES_MAX_LENGTH = 20;

   public void updateSignature(SignatureUpdater.Output param0) throws SignatureException {
      param0.update(Ints.toByteArray(this.entries.size()));

      for (MessageSignature var3 : this.entries) {
         param0.update(var3.bytes());
      }
   }

   public LastSeenMessages.Packed pack(MessageSignatureCache param0) {
      return new LastSeenMessages.Packed(this.entries.stream().map(l_param1 -> l_param1.pack(param0)).toList());
   }

   public record Packed(List<MessageSignature.Packed> entries) {
      public static final LastSeenMessages.Packed EMPTY = new LastSeenMessages.Packed(List.of());

      public Packed(FriendlyByteBuf param0) {
         this(readEntries(param0));
      }

      private static List<MessageSignature.Packed> readEntries(FriendlyByteBuf buf) {
         return buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), MessageSignature.Packed::read);
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeCollection(this.entries, MessageSignature.Packed::write);
      }

      public Optional<LastSeenMessages> unpack(MessageSignatureCache param0) {
         List<MessageSignature> var2 = new ArrayList<>(this.entries.size());

         for (MessageSignature.Packed var4 : this.entries) {
            Optional<MessageSignature> var5 = var4.unpack(param0);
            if (var5.isEmpty()) {
               return Optional.empty();
            }

            var2.add(var5.get());
         }

         return Optional.of(new LastSeenMessages(var2));
      }
   }

   public record Update(int offset, BitSet acknowledged) {
      public Update(FriendlyByteBuf param0) {
         this(param0.readVarInt(), param0.readFixedBitSet(20));
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeVarInt(this.offset);
         param0.writeFixedBitSet(this.acknowledged, 20);
      }
   }
}
