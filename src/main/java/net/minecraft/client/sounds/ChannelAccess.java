package net.minecraft.client.sounds;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class ChannelAccess {
   private final Set<ChannelAccess.ChannelHandle> channels = Sets.newIdentityHashSet();
   final Library library;
   final Executor executor;

   public ChannelAccess(Library param0, Executor param1) {
      this.library = param0;
      this.executor = param1;
   }

   public CompletableFuture<ChannelAccess.ChannelHandle> createHandle(Library.Pool param0) {
      CompletableFuture<ChannelAccess.ChannelHandle> var2 = new CompletableFuture<>();
      this.executor.execute(() -> {
         Channel l_var3 = this.library.acquireChannel(param0);
         if (l_var3 != null) {
            ChannelAccess.ChannelHandle l_var4 = new ChannelAccess.ChannelHandle(l_var3);
            this.channels.add(l_var4);
            var2.complete(l_var4);
         } else {
            var2.complete(null);
         }
      });
      return var2;
   }

   public void executeOnChannels(Consumer<Stream<Channel>> param0) {
      this.executor.execute(() -> param0.accept(this.channels.stream().map(l2_param0 -> l2_param0.channel).filter(Objects::nonNull)));
   }

   public void scheduleTick() {
      this.executor.execute(() -> {
         Iterator<ChannelAccess.ChannelHandle> l_var1 = this.channels.iterator();

         while (l_var1.hasNext()) {
            ChannelAccess.ChannelHandle l_var2 = l_var1.next();
            l_var2.channel.updateStream();
            if (l_var2.channel.stopped()) {
               l_var2.release();
               l_var1.remove();
            }
         }
      });
   }

   public void clear() {
      this.channels.forEach(ChannelAccess.ChannelHandle::release);
      this.channels.clear();
   }

   public class ChannelHandle {
      @Nullable
      Channel channel;
      private boolean stopped;

      public boolean isStopped() {
         return this.stopped;
      }

      public ChannelHandle(Channel param1) {
         this.channel = param1;
      }

      public void execute(Consumer<Channel> param0) {
         ChannelAccess.this.executor.execute(() -> {
            if (this.channel != null) {
               param0.accept(this.channel);
            }
         });
      }

      public void release() {
         this.stopped = true;
         ChannelAccess.this.library.releaseChannel(this.channel);
         this.channel = null;
      }
   }
}
