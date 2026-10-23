package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.profile.EditProfileScreen;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;

public class TitleScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEMO_LEVEL_ID = "Demo_World";
   public static final Component COPYRIGHT_TEXT = Component.literal("Copyright Mojang AB. Do not distribute!");
   public static final CubeMap CUBE_MAP = new CubeMap(new ResourceLocation("textures/gui/title/background/panorama"));
   private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
   @Nullable
   private String splash;
   private Button resetDemoButton;
   private final PanoramaRenderer panorama = new PanoramaRenderer(CUBE_MAP);
   private final boolean fading;
   private long fadeInStart;
   @Nullable
   private TitleScreen.WarningLabel warningLabel;
   private final LogoRenderer logoRenderer;
   @Nullable
   private Button singleplayerButton;

   public TitleScreen() {
      this(false);
   }

   public TitleScreen(boolean param0) {
      this(param0, null);
   }

   public TitleScreen(boolean param0, @Nullable LogoRenderer param1) {
      super(Component.translatable("narrator.screen.title"));
      this.fading = param0;
      this.logoRenderer = Objects.requireNonNullElseGet(param1, () -> new LogoRenderer(false));
   }

   @Override
   public void tick() {
      this.minecraft.getRealms32BitWarningStatus().showRealms32BitWarningIfNeeded(this);
   }

   public static CompletableFuture<Void> preloadResources(TextureManager param0, Executor param1) {
      return CompletableFuture.allOf(
         param0.preload(LogoRenderer.MINECRAFT_LOGO, param1),
         param0.preload(LogoRenderer.MINECRAFT_EDITION, param1),
         param0.preload(PANORAMA_OVERLAY, param1),
         CUBE_MAP.preload(param0, param1)
      );
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected void init() {
      if (this.splash == null) {
         this.splash = this.minecraft.getSplashManager().getSplash();
      }

      int var1 = this.font.width(COPYRIGHT_TEXT);
      int var2 = this.width - var1 - 2;
      int var3 = 24;
      int var4 = this.height / 4 + 48;
      if (this.minecraft.isDemo()) {
         this.createDemoMenuOptions(var4, 24);
      } else {
         this.createNormalMenuOptions(var4, 24);
      }

      this.addRenderableWidget(
         new ImageButton(
            this.width / 2 - 124,
            var4 + 72 + 12,
            20,
            20,
            0,
            106,
            20,
            Button.WIDGETS_LOCATION,
            256,
            256,
            l_param0 -> this.minecraft.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())),
            Component.translatable("narrator.button.language")
         )
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("menu.options"), l_param0 -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)))
            .bounds(this.width / 2 - 100, var4 + 72 + 12, 98, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("menu.editprofile"), l_param0 -> this.minecraft.setScreen(new EditProfileScreen(this)))
            .bounds(this.width / 2 + 2, var4 + 72 + 12, 98, 20)
            .build()
      );
      this.addRenderableWidget(
         new ImageButton(
            this.width / 2 + 104,
            var4 + 72 + 12,
            20,
            20,
            0,
            0,
            20,
            Button.ACCESSIBILITY_TEXTURE,
            32,
            64,
            l_param0 -> this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)),
            Component.translatable("narrator.button.accessibility")
         )
      );
      this.addRenderableWidget(
         new PlainTextButton(
            var2, this.height - 10, var1, 10, COPYRIGHT_TEXT, l_param0 -> this.minecraft.setScreen(new CreditsAndAttributionScreen(this)), this.font
         )
      );
      this.minecraft.setConnectedToRealms(false);
      if (!this.minecraft.is64Bit()) {
         this.warningLabel = new TitleScreen.WarningLabel(
            this.font, MultiLineLabel.create(this.font, Component.translatable("title.32bit.deprecation"), 350, 2), this.width / 2, var4 - 24
         );
      }
   }

   private void createNormalMenuOptions(int param0, int param1) {
      this.singleplayerButton = this.addRenderableWidget(
         Button.builder(Component.translatable("menu.singleplayer"), l_param0 -> this.minecraft.setScreen(new SelectWorldScreen(this)))
            .bounds(this.width / 2 - 100, param0, 200, 20)
            .build()
      );
      this.singleplayerButton.active = true;
      Component var3 = this.getMultiplayerDisabledReason();
      boolean var4 = var3 == null;
      Tooltip var5 = var3 != null ? Tooltip.create(var3) : null;
      this.addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), l_param0 -> {
         Screen l_var2 = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this);
         this.minecraft.setScreen(l_var2);
      }).bounds(this.width / 2 - 100, param0 + param1 * 1, 200, 20).tooltip(var5).build()).active = var4;
      this.addRenderableWidget(
         Button.builder(Component.literal("Credits"), l_param0 -> this.minecraft.setScreen(new CreditsScreen(this)))
            .bounds(this.width / 2 - 100, param0 + param1 * 2, 200, 20)
            .build()
      );
   }

   @Nullable
   private Component getMultiplayerDisabledReason() {
      if (this.minecraft.allowsMultiplayer()) {
         return null;
      } else {
         BanDetails var1 = this.minecraft.multiplayerBan();
         if (var1 != null) {
            return var1.expires() != null
               ? Component.translatable("title.multiplayer.disabled.banned.temporary")
               : Component.translatable("title.multiplayer.disabled.banned.permanent");
         } else {
            return Component.translatable("title.multiplayer.disabled");
         }
      }
   }

   private void createDemoMenuOptions(int param0, int param1) {
      boolean var3 = this.checkDemoWorldPresence();
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("menu.playdemo"),
               l_param1 -> {
                  if (var3) {
                     this.minecraft.createWorldOpenFlows().loadLevel(this, "Demo_World");
                  } else {
                     this.minecraft
                        .createWorldOpenFlows()
                        .createFreshLevel("Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions);
                  }
               }
            )
            .bounds(this.width / 2 - 100, param0, 200, 20)
            .build()
      );
      this.resetDemoButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("menu.resetdemo"),
               l_param0 -> {
                  LevelStorageSource l_var2 = this.minecraft.getLevelSource();

                  try (LevelStorageSource.LevelStorageAccess l_var3 = l_var2.createAccess("Demo_World")) {
                     LevelSummary l_var4 = l_var3.getSummary();
                     if (l_var4 != null) {
                        this.minecraft
                           .setScreen(
                              new ConfirmScreen(
                                 this::confirmDemo,
                                 Component.translatable("selectWorld.deleteQuestion"),
                                 Component.translatable("selectWorld.deleteWarning", l_var4.getLevelName()),
                                 Component.translatable("selectWorld.deleteButton"),
                                 CommonComponents.GUI_CANCEL
                              )
                           );
                     }
                  } catch (IOException l_var3) {
                     SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
                     LOGGER.warn("Failed to access demo world", l_var3);
                  }
               }
            )
            .bounds(this.width / 2 - 100, param0 + param1 * 1, 200, 20)
            .build()
      );
      this.resetDemoButton.active = var3;
   }

   private boolean checkDemoWorldPresence() {
      try (LevelStorageSource.LevelStorageAccess var1 = this.minecraft.getLevelSource().createAccess("Demo_World")) {
         return var1.getSummary() != null;
      } catch (IOException var1) {
         SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
         LOGGER.warn("Failed to read demo world data", var1);
         return false;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (this.fadeInStart == 0L && this.fading) {
         this.fadeInStart = Util.getMillis();
      }

      float var5 = this.fading ? (float)(Util.getMillis() - this.fadeInStart) / 1000.0F : 1.0F;
      this.panorama.render(param3, Mth.clamp(var5, 0.0F, 1.0F));
      RenderSystem.setShaderTexture(0, PANORAMA_OVERLAY);
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.fading ? Mth.ceil(Mth.clamp(var5, 0.0F, 1.0F)) : 1.0F);
      blit(param0, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float var6 = this.fading ? Mth.clamp(var5 - 1.0F, 0.0F, 1.0F) : 1.0F;
      this.logoRenderer.renderLogo(param0, this.width, var6);
      int var7 = Mth.ceil(var6 * 255.0F) << 24;
      if ((var7 & -67108864) != 0) {
         if (this.warningLabel != null) {
            this.warningLabel.render(param0, var7);
         }

         if (this.splash != null) {
            param0.pushPose();
            param0.translate(this.width / 2 + 90, 70.0F, 0.0F);
            param0.mulPose(Axis.ZP.rotationDegrees(-20.0F));
            float var8 = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
            var8 = var8 * 100.0F / (this.font.width(this.splash) + 32);
            param0.scale(var8, var8, var8);
            drawCenteredString(param0, this.font, this.splash, 0, -8, 16776960 | var7);
            param0.popPose();
         }

         String var8 = "Eag19 - u0(BETA)" + SharedConstants.getCurrentVersion().getName();
         drawString(param0, this.font, var8, 2, this.height - 10, 16777215 | var7);

         for (GuiEventListener var10 : this.children()) {
            if (var10 instanceof AbstractWidget) {
               ((AbstractWidget)var10).setAlpha(var6);
            }
         }

         super.render(param0, param1, param2, param3);

         if (this.singleplayerButton != null) {
            int alpha = Mth.clamp((int)(var6 * 176.0F), 0, 255);
            fill(
               param0,
               this.singleplayerButton.getX(),
               this.singleplayerButton.getY(),
               this.singleplayerButton.getX() + this.singleplayerButton.getWidth(),
               this.singleplayerButton.getY() + this.singleplayerButton.getHeight(),
               alpha << 24
            );
         }
      }
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      return super.mouseClicked(param0, param2, param4);
   }

   @Override
   public void added() {
      super.added();
   }

   private void confirmDemo(boolean param0) {
      if (param0) {
         try (LevelStorageSource.LevelStorageAccess var2 = this.minecraft.getLevelSource().createAccess("Demo_World")) {
            var2.deleteLevel();
         } catch (IOException var2) {
            SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to delete demo world", var2);
         }
      }

      this.minecraft.setScreen(this);
   }

   record WarningLabel(Font font, MultiLineLabel label, int x, int y) {
      public void render(PoseStack param0, int param1) {
         this.label.renderBackgroundCentered(param0, this.x, this.y, 9, 2, 2097152 | Math.min(param1, 1426063360));
         this.label.renderCentered(param0, this.x, this.y, 9, 16777215 | param1);
      }
   }
}
