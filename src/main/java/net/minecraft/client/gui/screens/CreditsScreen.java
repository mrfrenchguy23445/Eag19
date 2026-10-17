package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * Transparent credits / attribution screen shown from the title screen.
 *
 * <p>Unlike a normal screen this one does not paint an opaque backdrop: it re-renders the
 * previous screen (the title) behind itself, lays a light translucent veil over it and draws
 * the scrolling credits text on top, so the panorama keeps showing through. Scroll with the
 * mouse wheel; Escape or the Done button returns to the title.
 */
public class CreditsScreen extends Screen {
   private static final int LINE_HEIGHT = 12;
   private static final int MARGIN = 40;
   private final Screen lastScreen;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final List<FormattedCharSequence> lines = new ArrayList<>();
   private double scroll;

   public CreditsScreen(Screen param0) {
      super(Component.literal("Credits"));
      this.lastScreen = param0;
   }

   @Override
   protected void init() {
      this.layout.addToHeader(new StringWidget(Component.literal("EaglercraftX Credits"), this.font));
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onClose()).width(200).build());
      this.layout.arrangeElements();
      this.layout.visitWidgets(this::addRenderableWidget);
      this.lines.clear();
      int var1 = this.width - MARGIN * 2;
      for (int var2 = 0; var2 < CreditsContent.LINES.length; var2++) {
         String var3 = CreditsContent.LINES[var2];
         if (var3.isEmpty()) {
            this.lines.add(FormattedCharSequence.EMPTY);
         } else {
            this.lines.addAll(this.font.split(Component.literal(var3), var1));
         }
      }

      this.scroll = 0.0;
   }

   private int textTop() {
      return this.layout.getHeaderHeight() + 6;
   }

   private int textBottom() {
      return this.height - this.layout.getFooterHeight() - 6;
   }

   private double maxScroll() {
      int var1 = this.textBottom() - this.textTop();
      int var2 = this.lines.size() * LINE_HEIGHT;
      return Math.max(0, var2 - var1);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      this.scroll = Mth.clamp(this.scroll - param4 * 36.0, 0.0, this.maxScroll());
      return true;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      // Transparent: re-render the previous screen (the title) instead of an opaque backdrop.
      if (this.lastScreen != null) {
         this.lastScreen.render(param0, param1, param2, param3);
      }

      RenderSystem.enableBlend();
      // Light translucent veil (32% black) so the text stays readable while the title shows through.
      fill(param0, 0, 0, this.width, this.height, 0x52000000);
      int var5 = this.textTop();
      int var6 = this.textBottom();
      this.scroll = Mth.clamp(this.scroll, 0.0, this.maxScroll());
      int var7 = var5 - (int)this.scroll;
      for (int var8 = 0; var8 < this.lines.size(); var8++) {
         if (var7 > var6) {
            break;
         }

         if (var7 + LINE_HEIGHT > var5) {
            this.font.draw(param0, this.lines.get(var8), (float)MARGIN, (float)var7, 16777215);
         }

         var7 += LINE_HEIGHT;
      }

      super.render(param0, param1, param2, param3);
   }
}