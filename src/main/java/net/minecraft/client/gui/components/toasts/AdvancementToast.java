package net.minecraft.client.gui.components.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class AdvancementToast implements Toast {
   public static final int DISPLAY_TIME = 5000;
   private final Advancement advancement;
   private boolean playedSound;

   public AdvancementToast(Advancement param0) {
      this.advancement = param0;
   }

   @Override
   public Toast.Visibility render(PoseStack param0, ToastComponent param1, long param2) {
      RenderSystem.setShaderTexture(0, TEXTURE);
      DisplayInfo var5 = this.advancement.getDisplay();
      GuiComponent.blit(param0, 0, 0, 0, 0, this.width(), this.height());
      if (var5 != null) {
         List<FormattedCharSequence> var6 = param1.getMinecraft().font.split(var5.getTitle(), 125);
         int var7 = var5.getFrame() == FrameType.CHALLENGE ? 16746751 : 16776960;
         if (var6.size() == 1) {
            param1.getMinecraft().font.draw(param0, var5.getFrame().getDisplayName(), 30.0F, 7.0F, var7 | 0xFF000000);
            param1.getMinecraft().font.draw(param0, var6.get(0), 30.0F, 18.0F, -1);
         } else {
            int var8 = 1500;
            float var9 = 300.0F;
            if (param2 < 1500L) {
               int var10 = Mth.floor(Mth.clamp((float)(1500L - param2) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
               param1.getMinecraft().font.draw(param0, var5.getFrame().getDisplayName(), 30.0F, 11.0F, var7 | var10);
            } else {
               int var10 = Mth.floor(Mth.clamp((float)(param2 - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
               int var11 = this.height() / 2 - var6.size() * 9 / 2;

               for (FormattedCharSequence var13 : var6) {
                  param1.getMinecraft().font.draw(param0, var13, 30.0F, var11, 16777215 | var10);
                  var11 += 9;
               }
            }
         }

         if (!this.playedSound && param2 > 0L) {
            this.playedSound = true;
            if (var5.getFrame() == FrameType.CHALLENGE) {
               param1.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
            }
         }

         param1.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(param0, var5.getIcon(), 8, 8);
         return param2 >= 5000.0 * param1.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
      } else {
         return Toast.Visibility.HIDE;
      }
   }
}
