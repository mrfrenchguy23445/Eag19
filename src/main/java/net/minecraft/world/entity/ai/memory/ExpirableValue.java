package net.minecraft.world.entity.ai.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.VisibleForDebug;

public class ExpirableValue<T> {
   private final T value;
   private long timeToLive;

   public ExpirableValue(T param0, long param1) {
      this.value = param0;
      this.timeToLive = param1;
   }

   public void tick() {
      if (this.canExpire()) {
         this.timeToLive--;
      }
   }

   public static <T> ExpirableValue<T> of(T param0) {
      return new ExpirableValue<>(param0, Long.MAX_VALUE);
   }

   public static <T> ExpirableValue<T> of(T param0, long param1) {
      return new ExpirableValue<>(param0, param1);
   }

   public long getTimeToLive() {
      return this.timeToLive;
   }

   public T getValue() {
      return this.value;
   }

   public boolean hasExpired() {
      return this.timeToLive <= 0L;
   }

   @Override
   public String toString() {
      return this.value + (this.canExpire() ? " (ttl: " + this.timeToLive + ")" : "");
   }

   @VisibleForDebug
   public boolean canExpire() {
      return this.timeToLive != Long.MAX_VALUE;
   }

   public static <T> Codec<ExpirableValue<T>> codec(Codec<T> param0) {
      return RecordCodecBuilder.create(
         l_param1 -> l_param1.group(
               param0.fieldOf("value").forGetter(l2_param0 -> l2_param0.value),
               Codec.LONG.optionalFieldOf("ttl").forGetter(l2_param0 -> l2_param0.canExpire() ? Optional.of(l2_param0.timeToLive) : Optional.empty())
            )
            .apply(l_param1, (l2_param0, l2_param1) -> new ExpirableValue<>(l2_param0, l2_param1.orElse(Long.MAX_VALUE)))
      );
   }
}
