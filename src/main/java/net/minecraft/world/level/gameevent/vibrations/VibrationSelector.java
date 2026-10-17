package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

public class VibrationSelector {
   public static final Codec<VibrationSelector> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            VibrationInfo.CODEC.optionalFieldOf("event").forGetter(l_param0 -> l_param0.currentVibrationData.map(Pair::getLeft)),
            Codec.LONG.fieldOf("tick").forGetter(l_param0 -> l_param0.currentVibrationData.map(Pair::getRight).orElse(-1L))
         )
         .apply(param0, VibrationSelector::new)
   );
   private Optional<Pair<VibrationInfo, Long>> currentVibrationData;

   public VibrationSelector(Optional<VibrationInfo> param0, long param1) {
      this.currentVibrationData = param0.map(l_param2 -> Pair.of(l_param2, param1));
   }

   public VibrationSelector() {
      this.currentVibrationData = Optional.empty();
   }

   public void addCandidate(VibrationInfo param0, long param1) {
      if (this.shouldReplaceVibration(param0, param1)) {
         this.currentVibrationData = Optional.of(Pair.of(param0, param1));
      }
   }

   private boolean shouldReplaceVibration(VibrationInfo param0, long param1) {
      if (this.currentVibrationData.isEmpty()) {
         return true;
      } else {
         Pair<VibrationInfo, Long> var4 = this.currentVibrationData.get();
         long var5 = var4.getRight();
         if (param1 != var5) {
            return false;
         } else {
            VibrationInfo var7 = var4.getLeft();
            if (param0.distance() < var7.distance()) {
               return true;
            } else {
               return param0.distance() > var7.distance()
                  ? false
                  : VibrationListener.getGameEventFrequency(param0.gameEvent()) > VibrationListener.getGameEventFrequency(var7.gameEvent());
            }
         }
      }
   }

   public Optional<VibrationInfo> chosenCandidate(long param0) {
      if (this.currentVibrationData.isEmpty()) {
         return Optional.empty();
      } else {
         return this.currentVibrationData.get().getRight() < param0 ? Optional.of(this.currentVibrationData.get().getLeft()) : Optional.empty();
      }
   }

   public void startOver() {
      this.currentVibrationData = Optional.empty();
   }
}
