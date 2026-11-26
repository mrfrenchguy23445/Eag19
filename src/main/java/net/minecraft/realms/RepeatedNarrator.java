package net.minecraft.realms;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.GameNarrator;
import net.minecraft.network.chat.Component;

public class RepeatedNarrator {
   private final float permitsPerSecond;
   private final AtomicReference<RepeatedNarrator.Params> params = new AtomicReference<>();

   public RepeatedNarrator(Duration param0) {
      this.permitsPerSecond = 1000.0F / (float)param0.toMillis();
   }

   public void narrate(GameNarrator param0, Component param1) {
      RepeatedNarrator.Params var3 = this.params
         .updateAndGet(
            l_param1 -> l_param1 != null && param1.equals(l_param1.narration)
               ? l_param1
               : new RepeatedNarrator.Params(param1, RateLimiter.create(this.permitsPerSecond))
         );
      if (var3.rateLimiter.tryAcquire(1)) {
         param0.sayNow(param1);
      }
   }

   static class Params {
      final Component narration;
      final RateLimiter rateLimiter;

      Params(Component param0, RateLimiter param1) {
         this.narration = param0;
         this.rateLimiter = param1;
      }
   }
}
