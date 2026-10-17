package net.minecraft.client.gui.screens;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class VideoSettingsScreen extends OptionsSubScreen {
   private static final Component FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
   private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", FABULOUS, FABULOUS);
   private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
   private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
   private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
   private OptionsList list;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final int oldMipmaps;

   private static OptionInstance<?>[] options(Options param0) {
      return new OptionInstance[]{
         param0.graphicsMode(),
         param0.renderDistance(),
         param0.prioritizeChunkUpdates(),
         param0.simulationDistance(),
         param0.ambientOcclusion(),
         param0.framerateLimit(),
         param0.enableVsync(),
         param0.bobView(),
         param0.guiScale(),
         param0.attackIndicator(),
         param0.gamma(),
         param0.cloudStatus(),
         param0.fullscreen(),
         param0.particles(),
         param0.mipmapLevels(),
         param0.entityShadows(),
         param0.screenEffectScale(),
         param0.entityDistanceScaling(),
         param0.fovEffectScale(),
         param0.showAutosaveIndicator(),
         param0.glintSpeed(),
         param0.glintStrength()
      };
   }

   public VideoSettingsScreen(Screen param0, Options param1) {
      super(param0, param1, Component.translatable("options.videoTitle"));
      this.gpuWarnlistManager = param0.minecraft.getGpuWarnlistManager();
      this.gpuWarnlistManager.resetWarnings();
      if (param1.graphicsMode().get() == GraphicsStatus.FABULOUS) {
         this.gpuWarnlistManager.dismissWarning();
      }

      this.oldMipmaps = param1.mipmapLevels().get();
   }

   @Override
   protected void init() {
      this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
      int var1 = -1;
      Window var2 = this.minecraft.getWindow();
      Monitor var3 = var2.findBestMonitor();
      int var4;
      if (var3 == null) {
         var4 = -1;
      } else {
         Optional<VideoMode> var5 = var2.getPreferredFullscreenVideoMode();
         var4 = var5.map(var3::getVideoModeIndex).orElse(-1);
      }

      OptionInstance<Integer> var5 = new OptionInstance<>(
         "options.fullscreen.resolution",
         OptionInstance.noTooltip(),
         (l_param1, l_param2) -> {
            if (var3 == null) {
               return Component.translatable("options.fullscreen.unavailable");
            } else {
               return l_param2 == -1
                  ? Options.genericValueLabel(l_param1, Component.translatable("options.fullscreen.current"))
                  : Options.genericValueLabel(l_param1, Component.literal(var3.getMode(l_param2).toString()));
            }
         },
         new OptionInstance.IntRange(-1, var3 != null ? var3.getModeCount() - 1 : -1),
         var4,
         l_param2 -> {
            if (var3 != null) {
               var2.setPreferredFullscreenVideoMode(l_param2 == -1 ? Optional.empty() : Optional.of(var3.getMode(l_param2)));
            }
         }
      );
      this.list.addBig(var5);
      this.list.addBig(this.options.biomeBlendRadius());
      this.list.addSmall(options(this.options));
      this.addWidget(this.list);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, l_param1 -> {
         this.minecraft.options.save();
         var2.changeFullscreenVideoMode();
         this.minecraft.setScreen(this.lastScreen);
      }).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
   }

   @Override
   public void removed() {
      if (this.options.mipmapLevels().get() != this.oldMipmaps) {
         this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
         this.minecraft.delayTextureReload();
      }

      super.removed();
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      int var6 = this.options.guiScale().get();
      if (super.mouseClicked(param0, param2, param4)) {
         if (this.options.guiScale().get() != var6) {
            this.minecraft.resizeDisplay();
         }

         if (this.gpuWarnlistManager.isShowingWarning()) {
            List<Component> var7 = Lists.newArrayList(WARNING_MESSAGE, CommonComponents.NEW_LINE);
            String var8 = this.gpuWarnlistManager.getRendererWarnings();
            if (var8 != null) {
               var7.add(CommonComponents.NEW_LINE);
               var7.add(Component.translatable("options.graphics.warning.renderer", var8).withStyle(ChatFormatting.GRAY));
            }

            String var9 = this.gpuWarnlistManager.getVendorWarnings();
            if (var9 != null) {
               var7.add(CommonComponents.NEW_LINE);
               var7.add(Component.translatable("options.graphics.warning.vendor", var9).withStyle(ChatFormatting.GRAY));
            }

            String var10 = this.gpuWarnlistManager.getVersionWarnings();
            if (var10 != null) {
               var7.add(CommonComponents.NEW_LINE);
               var7.add(Component.translatable("options.graphics.warning.version", var10).withStyle(ChatFormatting.GRAY));
            }

            this.minecraft.setScreen(new PopupScreen(WARNING_TITLE, var7, ImmutableList.of(new PopupScreen.ButtonOption(BUTTON_ACCEPT, l_param0 -> {
               this.options.graphicsMode().set(GraphicsStatus.FABULOUS);
               Minecraft.getInstance().levelRenderer.allChanged();
               this.gpuWarnlistManager.dismissWarning();
               this.minecraft.setScreen(this);
            }), new PopupScreen.ButtonOption(BUTTON_CANCEL, l_param0 -> {
               this.gpuWarnlistManager.dismissWarningAndSkipFabulous();
               this.minecraft.setScreen(this);
            }))));
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.basicListRender(param0, this.list, param1, param2, param3);
   }
}
