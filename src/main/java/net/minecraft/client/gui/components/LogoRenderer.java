package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class LogoRenderer extends GuiComponent {
   public static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
   public static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");
   public static final int LOGO_WIDTH = 274;
   public static final int LOGO_HEIGHT = 44;
   public static final int DEFAULT_HEIGHT_OFFSET = 30;
   private final boolean showEasterEgg = RandomSource.create().nextFloat() < 1.0E-4;
   private final boolean keepLogoThroughFade;

   public LogoRenderer(boolean param0) {
      this.keepLogoThroughFade = param0;
   }

   public void renderLogo(PoseStack param0, int param1, float param2) {
      this.renderLogo(param0, param1, param2, 30);
   }

   public void renderLogo(PoseStack param0, int param1, float param2, int param3) {
      RenderSystem.setShaderTexture(0, MINECRAFT_LOGO);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.keepLogoThroughFade ? 1.0F : param2);
      int var5 = param1 / 2 - 137;
      if (this.showEasterEgg) {
         blitOutlineBlack(var5, param3, (l_param1, l_param2) -> {
            blit(param0, l_param1, l_param2, 0, 0, 99, 44);
            blit(param0, l_param1 + 99, l_param2, 129, 0, 27, 44);
            blit(param0, l_param1 + 99 + 26, l_param2, 126, 0, 3, 44);
            blit(param0, l_param1 + 99 + 26 + 3, l_param2, 99, 0, 26, 44);
            blit(param0, l_param1 + 155, l_param2, 0, 45, 155, 44);
         });
      } else {
         blitOutlineBlack(var5, param3, (l_param1, l_param2) -> {
            blit(param0, l_param1, l_param2, 0, 0, 155, 44);
            blit(param0, l_param1 + 155, l_param2, 0, 45, 155, 44);
         });
      }

      RenderSystem.setShaderTexture(0, MINECRAFT_EDITION);
      blit(param0, var5 + 88, param3 + 37, 0.0F, 0.0F, 98, 14, 128, 16);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
