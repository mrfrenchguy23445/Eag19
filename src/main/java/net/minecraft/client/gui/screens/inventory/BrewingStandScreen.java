package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;

public class BrewingStandScreen extends AbstractContainerScreen<BrewingStandMenu> {
   private static final ResourceLocation BREWING_STAND_LOCATION = new ResourceLocation("textures/gui/container/brewing_stand.png");
   private static final int[] BUBBLELENGTHS = new int[]{29, 24, 20, 16, 11, 6, 0};

   public BrewingStandScreen(BrewingStandMenu param0, Inventory param1, Component param2) {
      super(param0, param1, param2);
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      super.render(param0, param1, param2, param3);
      this.renderTooltip(param0, param1, param2);
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      RenderSystem.setShaderTexture(0, BREWING_STAND_LOCATION);
      int var5 = (this.width - this.imageWidth) / 2;
      int var6 = (this.height - this.imageHeight) / 2;
      blit(param0, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
      int var7 = this.menu.getFuel();
      int var8 = Mth.clamp((18 * var7 + 20 - 1) / 20, 0, 18);
      if (var8 > 0) {
         blit(param0, var5 + 60, var6 + 44, 176, 29, var8, 4);
      }

      int var9 = this.menu.getBrewingTicks();
      if (var9 > 0) {
         int var10 = (int)(28.0F * (1.0F - var9 / 400.0F));
         if (var10 > 0) {
            blit(param0, var5 + 97, var6 + 16, 176, 0, 9, var10);
         }

         var10 = BUBBLELENGTHS[var9 / 2 % 7];
         if (var10 > 0) {
            blit(param0, var5 + 63, var6 + 14 + 29 - var10, 185, 29 - var10, 12, var10);
         }
      }
   }
}
