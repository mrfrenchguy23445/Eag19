package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.BitSet;
import java.util.Objects;
import javax.annotation.Nullable;

public class LastSeenMessagesTracker {
   private final LastSeenTrackedEntry[] trackedMessages;
   private int tail;
   private int offset;
   @Nullable
   private MessageSignature lastTrackedMessage;

   public LastSeenMessagesTracker(int param0) {
      this.trackedMessages = new LastSeenTrackedEntry[param0];
   }

   public boolean addPending(MessageSignature param0, boolean param1) {
      if (Objects.equals(param0, this.lastTrackedMessage)) {
         return false;
      }

      this.lastTrackedMessage = param0;
      this.addEntry(param1 ? new LastSeenTrackedEntry(param0, true) : null);
      return true;
   }

   private void addEntry(@Nullable LastSeenTrackedEntry param0) {
      int var2 = this.tail;
      this.tail = (var2 + 1) % this.trackedMessages.length;
      this.offset++;
      this.trackedMessages[var2] = param0;
   }

   public void ignorePending(MessageSignature param0) {
      for (int var2 = 0; var2 < this.trackedMessages.length; var2++) {
         LastSeenTrackedEntry var3 = this.trackedMessages[var2];
         if (var3 != null && var3.pending() && param0.equals(var3.signature())) {
            this.trackedMessages[var2] = null;
            break;
         }
      }
   }

   public int getAndClearOffset() {
      int var1 = this.offset;
      this.offset = 0;
      return var1;
   }

   public LastSeenMessagesTracker.Update generateAndApplyUpdate() {
      int var1 = this.getAndClearOffset();
      BitSet var2 = new BitSet(this.trackedMessages.length);
      ObjectList<MessageSignature> var3 = new ObjectArrayList<>(this.trackedMessages.length);

      for (int var4 = 0; var4 < this.trackedMessages.length; var4++) {
         int var5 = (this.tail + var4) % this.trackedMessages.length;
         LastSeenTrackedEntry var6 = this.trackedMessages[var5];
         if (var6 != null) {
            var2.set(var4, true);
            var3.add(var6.signature());
            this.trackedMessages[var5] = var6.acknowledge();
         }
      }

      LastSeenMessages var4 = new LastSeenMessages(var3);
      LastSeenMessages.Update var5 = new LastSeenMessages.Update(var1, var2);
      return new LastSeenMessagesTracker.Update(var4, var5);
   }

   public int offset() {
      return this.offset;
   }

   public record Update(LastSeenMessages lastSeen, LastSeenMessages.Update update) {
   }
}
