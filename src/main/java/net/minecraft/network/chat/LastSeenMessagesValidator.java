package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Optional;
import javax.annotation.Nullable;

public class LastSeenMessagesValidator {
   private final int lastSeenCount;
   private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList<>();
   @Nullable
   private MessageSignature lastPendingMessage;

   public LastSeenMessagesValidator(int param0) {
      this.lastSeenCount = param0;

      for (int var2 = 0; var2 < param0; var2++) {
         this.trackedMessages.add(null);
      }
   }

   public void addPending(MessageSignature param0) {
      if (!param0.equals(this.lastPendingMessage)) {
         this.trackedMessages.add(new LastSeenTrackedEntry(param0, true));
         this.lastPendingMessage = param0;
      }
   }

   public int trackedMessagesCount() {
      return this.trackedMessages.size();
   }

   public boolean applyOffset(int param0) {
      int var2 = this.trackedMessages.size() - this.lastSeenCount;
      if (param0 >= 0 && param0 <= var2) {
         this.trackedMessages.removeElements(0, param0);
         return true;
      } else {
         return false;
      }
   }

   public Optional<LastSeenMessages> applyUpdate(LastSeenMessages.Update param0) {
      if (!this.applyOffset(param0.offset())) {
         return Optional.empty();
      }

      ObjectList<MessageSignature> var2 = new ObjectArrayList<>(param0.acknowledged().cardinality());
      if (param0.acknowledged().length() > this.lastSeenCount) {
         return Optional.empty();
      }

      for (int var3 = 0; var3 < this.lastSeenCount; var3++) {
         boolean var4 = param0.acknowledged().get(var3);
         LastSeenTrackedEntry var5 = this.trackedMessages.get(var3);
         if (var4) {
            if (var5 == null) {
               return Optional.empty();
            }

            this.trackedMessages.set(var3, var5.acknowledge());
            var2.add(var5.signature());
         } else {
            if (var5 != null && !var5.pending()) {
               return Optional.empty();
            }

            this.trackedMessages.set(var3, null);
         }
      }

      return Optional.of(new LastSeenMessages(var2));
   }
}
