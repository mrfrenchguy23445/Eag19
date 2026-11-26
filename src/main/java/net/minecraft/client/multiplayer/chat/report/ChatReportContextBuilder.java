package net.minecraft.client.multiplayer.chat.report;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;

public class ChatReportContextBuilder {
   final int leadingCount;
   private final List<ChatReportContextBuilder.Collector> activeCollectors = new ArrayList<>();

   public ChatReportContextBuilder(int param0) {
      this.leadingCount = param0;
   }

   public void collectAllContext(ChatLog param0, IntCollection param1, ChatReportContextBuilder.Handler param2) {
      IntSortedSet var4 = new IntRBTreeSet(param1);

      for (int var5 = var4.lastInt(); var5 >= param0.start() && (this.isActive() || !var4.isEmpty()); var5--) {
         if (param0.lookup(var5) instanceof LoggedChatMessage.Player var6) {
            boolean var7 = this.acceptContext(var6.message());
            if (var4.remove(var5)) {
               this.trackContext(var6.message());
               param2.accept(var5, var6);
            } else if (var7) {
               param2.accept(var5, var6);
            }
         }
      }
   }

   public void trackContext(PlayerChatMessage param0) {
      this.activeCollectors.add(new ChatReportContextBuilder.Collector(param0));
   }

   public boolean acceptContext(PlayerChatMessage param0) {
      boolean var2 = false;
      Iterator<ChatReportContextBuilder.Collector> var3 = this.activeCollectors.iterator();

      while (var3.hasNext()) {
         ChatReportContextBuilder.Collector var4 = var3.next();
         if (var4.accept(param0)) {
            var2 = true;
            if (var4.isComplete()) {
               var3.remove();
            }
         }
      }

      return var2;
   }

   public boolean isActive() {
      return !this.activeCollectors.isEmpty();
   }

   class Collector {
      private final Set<MessageSignature> lastSeenSignatures;
      private PlayerChatMessage lastChainMessage;
      private boolean collectingChain = true;
      private int count;

      Collector(PlayerChatMessage param1) {
         this.lastSeenSignatures = new ObjectOpenHashSet<>(param1.signedBody().lastSeen().entries());
         this.lastChainMessage = param1;
      }

      boolean accept(PlayerChatMessage param0) {
         if (param0.equals(this.lastChainMessage)) {
            return false;
         }

         boolean var2 = this.lastSeenSignatures.remove(param0.signature());
         if (this.collectingChain && this.lastChainMessage.sender().equals(param0.sender())) {
            if (this.lastChainMessage.link().isDescendantOf(param0.link())) {
               var2 = true;
               this.lastChainMessage = param0;
            } else {
               this.collectingChain = false;
            }
         }

         if (var2) {
            this.count++;
         }

         return var2;
      }

      boolean isComplete() {
         return this.count >= ChatReportContextBuilder.this.leadingCount || !this.collectingChain && this.lastSeenSignatures.isEmpty();
      }
   }

   public interface Handler {
      void accept(int var1, LoggedChatMessage.Player var2);
   }
}
