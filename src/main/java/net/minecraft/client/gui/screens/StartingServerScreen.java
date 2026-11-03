package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Cooperative-port loading screen for the integrated server. Replaces the vanilla
 * chunk-status pixel map: on this port every spawn chunk is generated inline on the
 * main thread, so the map lights up in one lump near the end and reads as a frozen
 * crash. This screen instead shows wall-clock elapsed time from the first frame:
 *
 *     STARTING INTEGRATED SERVER
 *                (13s)
 *
 * The chunk progress listener is still fed (it drives spawn-prep logging) but is
 * not rendered here.
 */
public class StartingServerScreen extends Screen {
   private static final long NARRATION_DELAY_MS = 2000L;
   private final long startMillis = Util.getMillis();
   private long lastNarration = -1L;
   private boolean done;

   public StartingServerScreen() {
      super(GameNarrator.NO_TITLE);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean shouldNarrateNavigation() {
      return false;
   }

   @Override
   public void removed() {
      this.done = true;
      this.triggerImmediateNarration(true);
   }

   private String elapsedText() {
      return "(" + Math.max(0, (Util.getMillis() - this.startMillis) / 1000L) + "s)";
   }

   @Override
   protected void updateNarratedWidget(NarrationElementOutput param0) {
      if (this.done) {
         param0.add(NarratedElementType.TITLE, Component.translatable("narrator.loading.done"));
      } else {
         param0.add(NarratedElementType.TITLE, "Starting integrated server " + this.elapsedText());
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      long var5 = Util.getMillis();
      if (var5 - this.lastNarration > NARRATION_DELAY_MS) {
         this.lastNarration = var5;
         this.triggerImmediateNarration(true);
      }

      int var7 = this.width / 2;
      int var8 = this.height / 2;
      drawCenteredString(param0, this.font, "STARTING INTEGRATED SERVER", var7, var8 - 20, 16777215);
      drawCenteredString(param0, this.font, this.elapsedText(), var7, var8 + 4, 14737632);
   }
}
