package net.minecraft.client.telemetry;

import java.util.function.Consumer;

@FunctionalInterface
public interface TelemetryEventSender {
   TelemetryEventSender DISABLED = (param0, param1) -> {};

   default TelemetryEventSender decorate(Consumer<TelemetryPropertyMap.Builder> param0) {
      return (l_param1, l_param2) -> this.send(l_param1, l2_param2 -> {
         l_param2.accept(l2_param2);
         param0.accept(l2_param2);
      });
   }

   void send(TelemetryEventType var1, Consumer<TelemetryPropertyMap.Builder> var2);
}
