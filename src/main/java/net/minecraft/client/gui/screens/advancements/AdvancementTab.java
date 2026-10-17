package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class AdvancementTab extends GuiComponent {
   private final Minecraft minecraft;
   private final AdvancementsScreen screen;
   private final AdvancementTabType type;
   private final int index;
   private final Advancement advancement;
   private final DisplayInfo display;
   private final ItemStack icon;
   private final Component title;
   private final AdvancementWidget root;
   private final Map<Advancement, AdvancementWidget> widgets = Maps.newLinkedHashMap();
   private double scrollX;
   private double scrollY;
   private int minX = Integer.MAX_VALUE;
   private int minY = Integer.MAX_VALUE;
   private int maxX = Integer.MIN_VALUE;
   private int maxY = Integer.MIN_VALUE;
   private float fade;
   private boolean centered;

   public AdvancementTab(Minecraft param0, AdvancementsScreen param1, AdvancementTabType param2, int param3, Advancement param4, DisplayInfo param5) {
      this.minecraft = param0;
      this.screen = param1;
      this.type = param2;
      this.index = param3;
      this.advancement = param4;
      this.display = param5;
      this.icon = param5.getIcon();
      this.title = param5.getTitle();
      this.root = new AdvancementWidget(this, param0, param4, param5);
      this.addWidget(this.root, param4);
   }

   public AdvancementTabType getType() {
      return this.type;
   }

   public int getIndex() {
      return this.index;
   }

   public Advancement getAdvancement() {
      return this.advancement;
   }

   public Component getTitle() {
      return this.title;
   }

   public DisplayInfo getDisplay() {
      return this.display;
   }

   public void drawTab(PoseStack param0, int param1, int param2, boolean param3) {
      this.type.draw(param0, param1, param2, param3, this.index);
   }

   public void drawIcon(PoseStack param0, int param1, int param2, ItemRenderer param3) {
      this.type.drawIcon(param0, param1, param2, this.index, param3, this.icon);
   }

   public void drawContents(PoseStack param0, int param1, int param2) {
      if (!this.centered) {
         this.scrollX = 117 - (this.maxX + this.minX) / 2;
         this.scrollY = 56 - (this.maxY + this.minY) / 2;
         this.centered = true;
      }

      enableScissor(param1, param2, param1 + 234, param2 + 113);
      param0.pushPose();
      param0.translate(param1, param2, 0.0F);
      ResourceLocation var4 = this.display.getBackground();
      if (var4 != null) {
         RenderSystem.setShaderTexture(0, var4);
      } else {
         RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
      }

      int var5 = Mth.floor(this.scrollX);
      int var6 = Mth.floor(this.scrollY);
      int var7 = var5 % 16;
      int var8 = var6 % 16;

      for (int var9 = -1; var9 <= 15; var9++) {
         for (int var10 = -1; var10 <= 8; var10++) {
            blit(param0, var7 + 16 * var9, var8 + 16 * var10, 0.0F, 0.0F, 16, 16, 16, 16);
         }
      }

      this.root.drawConnectivity(param0, var5, var6, true);
      this.root.drawConnectivity(param0, var5, var6, false);
      this.root.draw(param0, var5, var6);
      param0.popPose();
      disableScissor();
   }

   public void drawTooltips(PoseStack param0, int param1, int param2, int param3, int param4) {
      param0.pushPose();
      param0.translate(0.0F, 0.0F, -200.0F);
      fill(param0, 0, 0, 234, 113, Mth.floor(this.fade * 255.0F) << 24);
      boolean var6 = false;
      int var7 = Mth.floor(this.scrollX);
      int var8 = Mth.floor(this.scrollY);
      if (param1 > 0 && param1 < 234 && param2 > 0 && param2 < 113) {
         for (AdvancementWidget var10 : this.widgets.values()) {
            if (var10.isMouseOver(var7, var8, param1, param2)) {
               var6 = true;
               var10.drawHover(param0, var7, var8, this.fade, param3, param4);
               break;
            }
         }
      }

      param0.popPose();
      if (var6) {
         this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
      } else {
         this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
      }
   }

   public boolean isMouseOver(int param0, int param1, double param2, double param4) {
      return this.type.isMouseOver(param0, param1, this.index, param2, param4);
   }

   @Nullable
   public static AdvancementTab create(Minecraft param0, AdvancementsScreen param1, int param2, Advancement param3) {
      if (param3.getDisplay() == null) {
         return null;
      }

      for (AdvancementTabType var7 : AdvancementTabType.values()) {
         if (param2 < var7.getMax()) {
            return new AdvancementTab(param0, param1, var7, param2, param3, param3.getDisplay());
         }

         param2 -= var7.getMax();
      }

      return null;
   }

   public void scroll(double param0, double param2) {
      if (this.maxX - this.minX > 234) {
         this.scrollX = Mth.clamp(this.scrollX + param0, -(this.maxX - 234), 0.0);
      }

      if (this.maxY - this.minY > 113) {
         this.scrollY = Mth.clamp(this.scrollY + param2, -(this.maxY - 113), 0.0);
      }
   }

   public void addAdvancement(Advancement param0) {
      if (param0.getDisplay() != null) {
         AdvancementWidget var2 = new AdvancementWidget(this, this.minecraft, param0, param0.getDisplay());
         this.addWidget(var2, param0);
      }
   }

   private void addWidget(AdvancementWidget param0, Advancement param1) {
      this.widgets.put(param1, param0);
      int var3 = param0.getX();
      int var4 = var3 + 28;
      int var5 = param0.getY();
      int var6 = var5 + 27;
      this.minX = Math.min(this.minX, var3);
      this.maxX = Math.max(this.maxX, var4);
      this.minY = Math.min(this.minY, var5);
      this.maxY = Math.max(this.maxY, var6);

      for (AdvancementWidget var8 : this.widgets.values()) {
         var8.attachToParent();
      }
   }

   @Nullable
   public AdvancementWidget getWidget(Advancement param0) {
      return this.widgets.get(param0);
   }

   public AdvancementsScreen getScreen() {
      return this.screen;
   }
}
