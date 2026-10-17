package net.minecraft.client.telemetry.events;

import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;

public class WorldLoadTimesEvent {
   private final boolean newWorld;
   @Nullable
   private final Duration worldLoadDuration;

   public WorldLoadTimesEvent(boolean param0, @Nullable Duration param1) {
      this.worldLoadDuration = param1;
      this.newWorld = param0;
   }

   public void send(TelemetryEventSender param0) {
      if (this.worldLoadDuration != null) {
         param0.send(TelemetryEventType.WORLD_LOAD_TIMES, l_param0 -> {
            l_param0.put(TelemetryProperty.WORLD_LOAD_TIME_MS, (int)this.worldLoadDuration.toMillis());
            l_param0.put(TelemetryProperty.NEW_WORLD, this.newWorld);
         });
      }
   }
}
