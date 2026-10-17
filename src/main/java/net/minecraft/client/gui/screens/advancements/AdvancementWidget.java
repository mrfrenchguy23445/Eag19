package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class AdvancementWidget extends GuiComponent {
   private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/advancements/widgets.png");
   private static final int HEIGHT = 26;
   private static final int BOX_X = 0;
   private static final int BOX_WIDTH = 200;
   private static final int FRAME_WIDTH = 26;
   private static final int ICON_X = 8;
   private static final int ICON_Y = 5;
   private static final int ICON_WIDTH = 26;
   private static final int TITLE_PADDING_LEFT = 3;
   private static final int TITLE_PADDING_RIGHT = 5;
   private static final int TITLE_X = 32;
   private static final int TITLE_Y = 9;
   private static final int TITLE_MAX_WIDTH = 163;
   private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
   private final AdvancementTab tab;
   private final Advancement advancement;
   private final DisplayInfo display;
   private final FormattedCharSequence title;
   private final int width;
   private final List<FormattedCharSequence> description;
   private final Minecraft minecraft;
   @Nullable
   private AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   @Nullable
   private AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(AdvancementTab param0, Minecraft param1, Advancement param2, DisplayInfo param3) {
      this.tab = param0;
      this.advancement = param2;
      this.display = param3;
      this.minecraft = param1;
      this.title = Language.getInstance().getVisualOrder(param1.font.substrByWidth(param3.getTitle(), 163));
      this.x = Mth.floor(param3.getX() * 28.0F);
      this.y = Mth.floor(param3.getY() * 27.0F);
      int var5 = param2.getMaxCriteraRequired();
      int var6 = String.valueOf(var5).length();
      int var7 = var5 > 1 ? param1.font.width("  ") + param1.font.width("0") * var6 * 2 + param1.font.width("/") : 0;
      int var8 = 29 + param1.font.width(this.title) + var7;
      this.description = Language.getInstance()
         .getVisualOrder(
            this.findOptimalLines(ComponentUtils.mergeStyles(param3.getDescription().copy(), Style.EMPTY.withColor(param3.getFrame().getChatColor())), var8)
         );

      for (FormattedCharSequence var10 : this.description) {
         var8 = Math.max(var8, param1.font.width(var10));
      }

      this.width = var8 + 3 + 5;
   }

   private static float getMaxWidth(StringSplitter param0, List<FormattedText> param1) {
      return (float)param1.stream().mapToDouble(param0::stringWidth).max().orElse(0.0);
   }

   private List<FormattedText> findOptimalLines(Component param0, int param1) {
      StringSplitter var3 = this.minecraft.font.getSplitter();
      List<FormattedText> var4 = null;
      float var5 = Float.MAX_VALUE;

      for (int var9 : TEST_SPLIT_OFFSETS) {
         List<FormattedText> var10 = var3.splitLines(param0, param1 - var9, Style.EMPTY);
         float var11 = Math.abs(getMaxWidth(var3, var10) - param1);
         if (var11 <= 10.0F) {
            return var10;
         }

         if (var11 < var5) {
            var5 = var11;
            var4 = var10;
         }
      }

      return var4;
   }

   @Nullable
   private AdvancementWidget getFirstVisibleParent(Advancement param0) {
      do {
         param0 = param0.getParent();
      } while (param0 != null && param0.getDisplay() == null);

      return param0 != null && param0.getDisplay() != null ? this.tab.getWidget(param0) : null;
   }

   public void drawConnectivity(PoseStack param0, int param1, int param2, boolean param3) {
      if (this.parent != null) {
         int var5 = param1 + this.parent.x + 13;
         int var6 = param1 + this.parent.x + 26 + 4;
         int var7 = param2 + this.parent.y + 13;
         int var8 = param1 + this.x + 13;
         int var9 = param2 + this.y + 13;
         int var10 = param3 ? -16777216 : -1;
         if (param3) {
            hLine(param0, var6, var5, var7 - 1, var10);
            hLine(param0, var6 + 1, var5, var7, var10);
            hLine(param0, var6, var5, var7 + 1, var10);
            hLine(param0, var8, var6 - 1, var9 - 1, var10);
            hLine(param0, var8, var6 - 1, var9, var10);
            hLine(param0, var8, var6 - 1, var9 + 1, var10);
            vLine(param0, var6 - 1, var9, var7, var10);
            vLine(param0, var6 + 1, var9, var7, var10);
         } else {
            hLine(param0, var6, var5, var7, var10);
            hLine(param0, var8, var6, var9, var10);
            vLine(param0, var6, var9, var7, var10);
         }
      }

      for (AdvancementWidget var6 : this.children) {
         var6.drawConnectivity(param0, param1, param2, param3);
      }
   }

   public void draw(PoseStack param0, int param1, int param2) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float var4 = this.progress == null ? 0.0F : this.progress.getPercent();
         AdvancementWidgetType var5;
         if (var4 >= 1.0F) {
            var5 = AdvancementWidgetType.OBTAINED;
         } else {
            var5 = AdvancementWidgetType.UNOBTAINED;
         }

         RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
         blit(param0, param1 + this.x + 3, param2 + this.y, this.display.getFrame().getTexture(), 128 + var5.getIndex() * 26, 26, 26);
         this.minecraft.getItemRenderer().renderAndDecorateFakeItem(param0, this.display.getIcon(), param1 + this.x + 8, param2 + this.y + 5);
      }

      for (AdvancementWidget var5 : this.children) {
         var5.draw(param0, param1, param2);
      }
   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(AdvancementProgress param0) {
      this.progress = param0;
   }

   public void addChild(AdvancementWidget param0) {
      this.children.add(param0);
   }

   public void drawHover(PoseStack param0, int param1, int param2, float param3, int param4, int param5) {
      boolean var7 = param4 + param1 + this.x + this.width + 26 >= this.tab.getScreen().width;
      String var8 = this.progress == null ? null : this.progress.getProgressText();
      int var9 = var8 == null ? 0 : this.minecraft.font.width(var8);
      boolean var10 = 113 - param2 - this.y - 26 <= 6 + this.description.size() * 9;
      float var11 = this.progress == null ? 0.0F : this.progress.getPercent();
      int var15 = Mth.floor(var11 * this.width);
      AdvancementWidgetType var12;
      AdvancementWidgetType var13;
      AdvancementWidgetType var14;
      if (var11 >= 1.0F) {
         var15 = this.width / 2;
         var12 = AdvancementWidgetType.OBTAINED;
         var13 = AdvancementWidgetType.OBTAINED;
         var14 = AdvancementWidgetType.OBTAINED;
      } else if (var15 < 2) {
         var15 = this.width / 2;
         var12 = AdvancementWidgetType.UNOBTAINED;
         var13 = AdvancementWidgetType.UNOBTAINED;
         var14 = AdvancementWidgetType.UNOBTAINED;
      } else if (var15 > this.width - 2) {
         var15 = this.width / 2;
         var12 = AdvancementWidgetType.OBTAINED;
         var13 = AdvancementWidgetType.OBTAINED;
         var14 = AdvancementWidgetType.UNOBTAINED;
      } else {
         var12 = AdvancementWidgetType.OBTAINED;
         var13 = AdvancementWidgetType.UNOBTAINED;
         var14 = AdvancementWidgetType.UNOBTAINED;
      }

      int var16 = this.width - var15;
      RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
      RenderSystem.enableBlend();
      int var17 = param2 + this.y;
      int var18;
      if (var7) {
         var18 = param1 + this.x - this.width + 26 + 6;
      } else {
         var18 = param1 + this.x;
      }

      int var19 = 32 + this.description.size() * 9;
      if (!this.description.isEmpty()) {
         if (var10) {
            blitNineSliced(param0, var18, var17 + 26 - var19, this.width, var19, 10, 200, 26, 0, 52);
         } else {
            blitNineSliced(param0, var18, var17, this.width, var19, 10, 200, 26, 0, 52);
         }
      }

      blit(param0, var18, var17, 0, var12.getIndex() * 26, var15, 26);
      blit(param0, var18 + var15, var17, 200 - var16, var13.getIndex() * 26, var16, 26);
      blit(param0, param1 + this.x + 3, param2 + this.y, this.display.getFrame().getTexture(), 128 + var14.getIndex() * 26, 26, 26);
      if (var7) {
         this.minecraft.font.drawShadow(param0, this.title, var18 + 5, param2 + this.y + 9, -1);
         if (var8 != null) {
            this.minecraft.font.drawShadow(param0, var8, param1 + this.x - var9, param2 + this.y + 9, -1);
         }
      } else {
         this.minecraft.font.drawShadow(param0, this.title, param1 + this.x + 32, param2 + this.y + 9, -1);
         if (var8 != null) {
            this.minecraft.font.drawShadow(param0, var8, param1 + this.x + this.width - var9 - 5, param2 + this.y + 9, -1);
         }
      }

      if (var10) {
         for (int var20 = 0; var20 < this.description.size(); var20++) {
            this.minecraft.font.draw(param0, this.description.get(var20), var18 + 5, var17 + 26 - var19 + 7 + var20 * 9, -5592406);
         }
      } else {
         for (int var20 = 0; var20 < this.description.size(); var20++) {
            this.minecraft.font.draw(param0, this.description.get(var20), var18 + 5, param2 + this.y + 9 + 17 + var20 * 9, -5592406);
         }
      }

      this.minecraft.getItemRenderer().renderAndDecorateFakeItem(param0, this.display.getIcon(), param1 + this.x + 8, param2 + this.y + 5);
   }

   public boolean isMouseOver(int param0, int param1, int param2, int param3) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int var5 = param0 + this.x;
         int var6 = var5 + 26;
         int var7 = param1 + this.y;
         int var8 = var7 + 26;
         return param2 >= var5 && param2 <= var6 && param3 >= var7 && param3 <= var8;
      } else {
         return false;
      }
   }

   public void attachToParent() {
      if (this.parent == null && this.advancement.getParent() != null) {
         this.parent = this.getFirstVisibleParent(this.advancement);
         if (this.parent != null) {
            this.parent.addChild(this);
         }
      }
   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
   }
}
