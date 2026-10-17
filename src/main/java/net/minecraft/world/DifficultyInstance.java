package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.util.Mth;

@Immutable
public class DifficultyInstance {
   private static final float DIFFICULTY_TIME_GLOBAL_OFFSET = -72000.0F;
   private static final float MAX_DIFFICULTY_TIME_GLOBAL = 1440000.0F;
   private static final float MAX_DIFFICULTY_TIME_LOCAL = 3600000.0F;
   private final Difficulty base;
   private final float effectiveDifficulty;

   public DifficultyInstance(Difficulty param0, long param1, long param3, float param5) {
      this.base = param0;
      this.effectiveDifficulty = this.calculateDifficulty(param0, param1, param3, param5);
   }

   public Difficulty getDifficulty() {
      return this.base;
   }

   public float getEffectiveDifficulty() {
      return this.effectiveDifficulty;
   }

   public boolean isHard() {
      return this.effectiveDifficulty >= Difficulty.HARD.ordinal();
   }

   public boolean isHarderThan(float param0) {
      return this.effectiveDifficulty > param0;
   }

   public float getSpecialMultiplier() {
      if (this.effectiveDifficulty < 2.0F) {
         return 0.0F;
      } else {
         return this.effectiveDifficulty > 4.0F ? 1.0F : (this.effectiveDifficulty - 2.0F) / 2.0F;
      }
   }

   private float calculateDifficulty(Difficulty param0, long param1, long param3, float param5) {
      if (param0 == Difficulty.PEACEFUL) {
         return 0.0F;
      }

      boolean var7 = param0 == Difficulty.HARD;
      float var8 = 0.75F;
      float var9 = Mth.clamp(((float)param1 + -72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
      var8 += var9;
      float var10 = 0.0F;
      var10 += Mth.clamp((float)param3 / 3600000.0F, 0.0F, 1.0F) * (var7 ? 1.0F : 0.75F);
      var10 += Mth.clamp(param5 * 0.25F, 0.0F, var9);
      if (param0 == Difficulty.EASY) {
         var10 *= 0.5F;
      }

      var8 += var10;
      return param0.getId() * var8;
   }
}
