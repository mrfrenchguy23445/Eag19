package net.minecraft.client.telemetry;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.serialization.Codec;

public record TelemetryEventInstance(TelemetryEventType type, TelemetryPropertyMap properties) {
   public static final Codec<TelemetryEventInstance> CODEC = TelemetryEventType.CODEC.dispatchStable(TelemetryEventInstance::type, TelemetryEventType::codec);

   public TelemetryEventInstance {
      properties.propertySet().forEach(l_param1 -> {
         if (!type.contains((TelemetryProperty<?>)l_param1)) {
            throw new IllegalArgumentException("Property '" + l_param1.id() + "' not expected for event: '" + type.id() + "'");
         }
      });
   }

   public TelemetryEvent export(TelemetrySession param0) {
      return this.type.export(param0, this.properties);
   }
}
