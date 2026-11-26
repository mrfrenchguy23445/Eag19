package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class MessageSignatureCache {
   public static final int NOT_FOUND = -1;
   private static final int DEFAULT_CAPACITY = 128;
   private final MessageSignature[] entries;

   public MessageSignatureCache(int param0) {
      this.entries = new MessageSignature[param0];
   }

   public static MessageSignatureCache createDefault() {
      return new MessageSignatureCache(128);
   }

   public int pack(MessageSignature param0) {
      for (int var2 = 0; var2 < this.entries.length; var2++) {
         if (param0.equals(this.entries[var2])) {
            return var2;
         }
      }

      return -1;
   }

   @Nullable
   public MessageSignature unpack(int param0) {
      return this.entries[param0];
   }

   public void push(PlayerChatMessage param0) {
      List<MessageSignature> var2 = param0.signedBody().lastSeen().entries();
      ArrayDeque<MessageSignature> var3 = new ArrayDeque<>(var2.size() + 1);
      var3.addAll(var2);
      MessageSignature var4 = param0.signature();
      if (var4 != null) {
         var3.add(var4);
      }

      this.push(var3);
   }

   @VisibleForTesting
   void push(List<MessageSignature> param0) {
      this.push(new ArrayDeque<>(param0));
   }

   private void push(ArrayDeque<MessageSignature> param0) {
      Set<MessageSignature> var2 = new ObjectOpenHashSet<>(param0);

      for (int var3 = 0; !param0.isEmpty() && var3 < this.entries.length; var3++) {
         MessageSignature var4 = this.entries[var3];
         this.entries[var3] = param0.removeLast();
         if (var4 != null && !var2.contains(var4)) {
            param0.addFirst(var4);
         }
      }
   }
}
