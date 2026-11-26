package net.minecraft.world.effect;

import net.minecraft.ChatFormatting;

public enum MobEffectCategory {
   BENEFICIAL(ChatFormatting.BLUE),
   HARMFUL(ChatFormatting.RED),
   NEUTRAL(ChatFormatting.BLUE);

   private final ChatFormatting tooltipFormatting;

   MobEffectCategory(ChatFormatting param2) {
      this.tooltipFormatting = param2;
   }

   public ChatFormatting getTooltipFormatting() {
      return this.tooltipFormatting;
   }
}
