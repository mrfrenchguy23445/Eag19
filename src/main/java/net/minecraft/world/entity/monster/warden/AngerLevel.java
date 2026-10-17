package net.minecraft.world.entity.monster.warden;

import java.util.Arrays;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public enum AngerLevel {
   CALM(0, SoundEvents.WARDEN_AMBIENT, SoundEvents.WARDEN_LISTENING),
   AGITATED(40, SoundEvents.WARDEN_AGITATED, SoundEvents.WARDEN_LISTENING_ANGRY),
   ANGRY(80, SoundEvents.WARDEN_ANGRY, SoundEvents.WARDEN_LISTENING_ANGRY);

   private static final AngerLevel[] SORTED_LEVELS = Util.make(
      values(), param0 -> Arrays.sort(param0, (l_param0, l_param1) -> Integer.compare(l_param1.minimumAnger, l_param0.minimumAnger))
   );
   private final int minimumAnger;
   private final SoundEvent ambientSound;
   private final SoundEvent listeningSound;

   AngerLevel(int param2, SoundEvent param3, SoundEvent param4) {
      this.minimumAnger = param2;
      this.ambientSound = param3;
      this.listeningSound = param4;
   }

   public int getMinimumAnger() {
      return this.minimumAnger;
   }

   public SoundEvent getAmbientSound() {
      return this.ambientSound;
   }

   public SoundEvent getListeningSound() {
      return this.listeningSound;
   }

   public static AngerLevel byAnger(int param0) {
      for (AngerLevel var4 : SORTED_LEVELS) {
         if (param0 >= var4.minimumAnger) {
            return var4;
         }
      }

      return CALM;
   }

   public boolean isAngry() {
      return this == ANGRY;
   }
}
