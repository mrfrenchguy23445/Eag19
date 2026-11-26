package net.minecraft.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class PeriodicNotificationManager
   extends SimplePreparableReloadListener<Map<String, List<PeriodicNotificationManager.Notification>>>
   implements AutoCloseable {
   private static final Codec<Map<String, List<PeriodicNotificationManager.Notification>>> CODEC = Codec.unboundedMap(
      Codec.STRING,
      RecordCodecBuilder.<PeriodicNotificationManager.Notification>create(
            param0 -> param0.group(
                  Codec.LONG.optionalFieldOf("delay", 0L).forGetter(PeriodicNotificationManager.Notification::delay),
                  Codec.LONG.fieldOf("period").forGetter(PeriodicNotificationManager.Notification::period),
                  Codec.STRING.fieldOf("title").forGetter(PeriodicNotificationManager.Notification::title),
                  Codec.STRING.fieldOf("message").forGetter(PeriodicNotificationManager.Notification::message)
               )
               .apply(param0, PeriodicNotificationManager.Notification::new)
         )
         .listOf()
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation notifications;
   private final Object2BooleanFunction<String> selector;
   @Nullable
   private java.util.Timer timer;
   @Nullable
   private PeriodicNotificationManager.NotificationTask notificationTask;

   public PeriodicNotificationManager(ResourceLocation param0, Object2BooleanFunction<String> param1) {
      this.notifications = param0;
      this.selector = param1;
   }

   protected Map<String, List<PeriodicNotificationManager.Notification>> prepare(ResourceManager param0, ProfilerFiller param1) {
      try (Reader var3 = param0.openAsReader(this.notifications)) {
         return CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(var3)).result().orElseThrow();
      } catch (Exception var3) {
         LOGGER.warn("Failed to load {}", this.notifications, var3);
         return ImmutableMap.of();
      }
   }

   protected void apply(Map<String, List<PeriodicNotificationManager.Notification>> param0, ResourceManager param1, ProfilerFiller param2) {
      List<PeriodicNotificationManager.Notification> var4 = param0.entrySet()
         .stream()
         .filter(l_param0 -> this.selector.apply(l_param0.getKey()))
         .map(Entry::getValue)
         .flatMap(Collection::stream)
         .collect(Collectors.toList());
      if (var4.isEmpty()) {
         this.stopTimer();
      } else if (var4.stream().anyMatch(l_param0 -> l_param0.period == 0L)) {
         Util.logAndPauseIfInIde("A periodic notification in " + this.notifications + " has a period of zero minutes");
         this.stopTimer();
      } else {
         long var5 = this.calculateInitialDelay(var4);
         long var7 = this.calculateOptimalPeriod(var4, var5);
         if (this.timer == null) {
            this.timer = new java.util.Timer();
         }

         if (this.notificationTask == null) {
            this.notificationTask = new PeriodicNotificationManager.NotificationTask(var4, var5, var7);
         } else {
            this.notificationTask = this.notificationTask.reset(var4, var7);
         }

         this.timer.scheduleAtFixedRate(this.notificationTask, TimeUnit.MINUTES.toMillis(var5), TimeUnit.MINUTES.toMillis(var7));
      }
   }

   @Override
   public void close() {
      this.stopTimer();
   }

   private void stopTimer() {
      if (this.timer != null) {
         this.timer.cancel();
      }
   }

   private long calculateOptimalPeriod(List<PeriodicNotificationManager.Notification> param0, long param1) {
      return param0.stream().mapToLong(l_param2 -> {
         long l_var3 = l_param2.delay - param1;
         return LongMath.gcd(l_var3, l_param2.period);
      }).reduce(LongMath::gcd).orElseThrow(() -> new IllegalStateException("Empty notifications from: " + this.notifications));
   }

   private long calculateInitialDelay(List<PeriodicNotificationManager.Notification> param0) {
      return param0.stream().mapToLong(l_param0 -> l_param0.delay).min().orElse(0L);
   }

   public record Notification(long delay, long period, String title, String message) {
   }

   static class NotificationTask extends TimerTask {
      private final Minecraft minecraft = Minecraft.getInstance();
      private final List<PeriodicNotificationManager.Notification> notifications;
      private final long period;
      private final AtomicLong elapsed;

      public NotificationTask(List<PeriodicNotificationManager.Notification> param0, long param1, long param3) {
         this.notifications = param0;
         this.period = param3;
         this.elapsed = new AtomicLong(param1);
      }

      public PeriodicNotificationManager.NotificationTask reset(List<PeriodicNotificationManager.Notification> param0, long param1) {
         this.cancel();
         return new PeriodicNotificationManager.NotificationTask(param0, this.elapsed.get(), param1);
      }

      @Override
      public void run() {
         long var1 = this.elapsed.getAndAdd(this.period);
         long var3 = this.elapsed.get();

         for (PeriodicNotificationManager.Notification var6 : this.notifications) {
            if (var1 >= var6.delay) {
               long var7 = var1 / var6.period;
               long var9 = var3 / var6.period;
               if (var7 != var9) {
                  this.minecraft
                     .execute(
                        () -> SystemToast.add(
                           Minecraft.getInstance().getToasts(),
                           SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                           Component.translatable(var6.title, var7),
                           Component.translatable(var6.message, var7)
                        )
                     );
                  return;
               }
            }
         }
      }
   }
}
