package net.minecraft.client.gui.components;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;

public class BossHealthOverlay extends GuiComponent {
   private static final ResourceLocation GUI_BARS_LOCATION = new ResourceLocation("textures/gui/bars.png");
   private static final int BAR_WIDTH = 182;
   private static final int BAR_HEIGHT = 5;
   private static final int OVERLAY_OFFSET = 80;
   private final Minecraft minecraft;
   final Map<UUID, LerpingBossEvent> events = Maps.newLinkedHashMap();

   public BossHealthOverlay(Minecraft param0) {
      this.minecraft = param0;
   }

   public void render(PoseStack param0) {
      if (!this.events.isEmpty()) {
         int var2 = this.minecraft.getWindow().getGuiScaledWidth();
         int var3 = 12;

         for (LerpingBossEvent var5 : this.events.values()) {
            int var6 = var2 / 2 - 91;
            int var7 = var3;
            RenderSystem.setShaderTexture(0, GUI_BARS_LOCATION);
            this.drawBar(param0, var6, var7, var5);
            Component var8 = var5.getName();
            int var9 = this.minecraft.font.width(var8);
            int var10 = var2 / 2 - var9 / 2;
            int var11 = var7 - 9;
            this.minecraft.font.drawShadow(param0, var8, var10, var11, 16777215);
            var3 += 10 + 9;
            if (var3 >= this.minecraft.getWindow().getGuiScaledHeight() / 3) {
               break;
            }
         }
      }
   }

   private void drawBar(PoseStack param0, int param1, int param2, BossEvent param3) {
      this.drawBar(param0, param1, param2, param3, 182, 0);
      int var5 = (int)(param3.getProgress() * 183.0F);
      if (var5 > 0) {
         this.drawBar(param0, param1, param2, param3, var5, 5);
      }
   }

   private void drawBar(PoseStack param0, int param1, int param2, BossEvent param3, int param4, int param5) {
      blit(param0, param1, param2, 0, param3.getColor().ordinal() * 5 * 2 + param5, param4, 5);
      if (param3.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
         RenderSystem.enableBlend();
         blit(param0, param1, param2, 0, 80 + (param3.getOverlay().ordinal() - 1) * 5 * 2 + param5, param4, 5);
         RenderSystem.disableBlend();
      }
   }

   public void update(ClientboundBossEventPacket param0) {
      param0.dispatch(
         new ClientboundBossEventPacket.Handler() {
            @Override
            public void add(
               UUID l_param0,
               Component l_param1,
               float l_param2,
               BossEvent.BossBarColor l_param3,
               BossEvent.BossBarOverlay l_param4,
               boolean l_param5,
               boolean l_param6,
               boolean l_param7
            ) {
               BossHealthOverlay.this.events
                  .put(l_param0, new LerpingBossEvent(l_param0, l_param1, l_param2, l_param3, l_param4, l_param5, l_param6, l_param7));
            }

            @Override
            public void remove(UUID l_param0) {
               BossHealthOverlay.this.events.remove(l_param0);
            }

            @Override
            public void updateProgress(UUID l_param0, float l_param1) {
               BossHealthOverlay.this.events.get(l_param0).setProgress(l_param1);
            }

            @Override
            public void updateName(UUID l_param0, Component l_param1) {
               BossHealthOverlay.this.events.get(l_param0).setName(l_param1);
            }

            @Override
            public void updateStyle(UUID l_param0, BossEvent.BossBarColor l_param1, BossEvent.BossBarOverlay l_param2) {
               LerpingBossEvent l_var4 = BossHealthOverlay.this.events.get(l_param0);
               l_var4.setColor(l_param1);
               l_var4.setOverlay(l_param2);
            }

            @Override
            public void updateProperties(UUID l_param0, boolean l_param1, boolean l_param2, boolean l_param3) {
               LerpingBossEvent l_var5 = BossHealthOverlay.this.events.get(l_param0);
               l_var5.setDarkenScreen(l_param1);
               l_var5.setPlayBossMusic(l_param2);
               l_var5.setCreateWorldFog(l_param3);
            }
         }
      );
   }

   public void reset() {
      this.events.clear();
   }

   public boolean shouldPlayMusic() {
      if (!this.events.isEmpty()) {
         for (BossEvent var2 : this.events.values()) {
            if (var2.shouldPlayBossMusic()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldDarkenScreen() {
      if (!this.events.isEmpty()) {
         for (BossEvent var2 : this.events.values()) {
            if (var2.shouldDarkenScreen()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldCreateWorldFog() {
      if (!this.events.isEmpty()) {
         for (BossEvent var2 : this.events.values()) {
            if (var2.shouldCreateWorldFog()) {
               return true;
            }
         }
      }

      return false;
   }
}
