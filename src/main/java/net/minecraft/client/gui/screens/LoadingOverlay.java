package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class LoadingOverlay extends Overlay {
   private static long diagLastLog = -1L;
   static final ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION = new ResourceLocation("textures/gui/title/mojangstudios.png");
   private static final int LOGO_BACKGROUND_COLOR = FastColor.ARGB32.color(255, 239, 50, 61);
   private static final int LOGO_BACKGROUND_COLOR_DARK = FastColor.ARGB32.color(255, 0, 0, 0);
   private static final IntSupplier BRAND_BACKGROUND = () -> Minecraft.getInstance().options.darkMojangStudiosBackground().get()
      ? LOGO_BACKGROUND_COLOR_DARK
      : LOGO_BACKGROUND_COLOR;
   private static final int LOGO_SCALE = 240;
   private static final float LOGO_QUARTER_FLOAT = 60.0F;
   private static final int LOGO_QUARTER = 60;
   private static final int LOGO_HALF = 120;
   private static final float LOGO_OVERLAP = 0.0625F;
   private static final float SMOOTHING = 0.95F;
   public static final long FADE_OUT_TIME = 1000L;
   public static final long FADE_IN_TIME = 500L;
   private final Minecraft minecraft;
   private final ReloadInstance reload;
   private final Consumer<Optional<Throwable>> onFinish;
   private final boolean fadeIn;
   private float currentProgress;
   private long fadeOutStart = -1L;
   private long fadeInStart = -1L;

   public LoadingOverlay(Minecraft param0, ReloadInstance param1, Consumer<Optional<Throwable>> param2, boolean param3) {
      this.minecraft = param0;
      this.reload = param1;
      this.onFinish = param2;
      this.fadeIn = param3;
   }

   private static void diag(String msg) {
      System.out.println("[diag] LoadingOverlay: " + msg);
      try {
         net.lax1dude.eaglercraft.v1_8.log4j.LogManager.getLogger("Diag").info("{}", "LoadingOverlay: " + msg);
      } catch (Throwable var2) {
         System.out.println("[diag-eag-fail] LoadingOverlay: " + var2);
      }
   }

   public static void registerTextures(Minecraft param0) {
      param0.getTextureManager().register(MOJANG_STUDIOS_LOGO_LOCATION, new LoadingOverlay.LogoTexture());
   }

   private static int replaceAlpha(int param0, int param1) {
      return param0 & 16777215 | param1 << 24;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      long varDiag = Util.getMillis();
      if (varDiag - diagLastLog > 1500L) {
         diagLastLog = varDiag;
         diag("fadeIn=" + this.fadeIn + " isDone=" + this.reload.isDone() + " progress=" + this.reload.getActualProgress()
            + " fadeOutStart=" + this.fadeOutStart + " fadeInStart=" + this.fadeInStart
            + " screen=" + (this.minecraft.screen == null ? "null" : this.minecraft.screen.getClass().getSimpleName()));
      }
      int var5 = this.minecraft.getWindow().getGuiScaledWidth();
      int var6 = this.minecraft.getWindow().getGuiScaledHeight();
      long var7 = Util.getMillis();
      if (this.fadeIn && this.fadeInStart == -1L) {
         this.fadeInStart = var7;
      }

      float var9 = this.fadeOutStart > -1L ? (float)(var7 - this.fadeOutStart) / 1000.0F : -1.0F;
      float var10 = this.fadeInStart > -1L ? (float)(var7 - this.fadeInStart) / 500.0F : -1.0F;
      float var11;
      if (var9 >= 1.0F) {
         if (this.minecraft.screen != null) {
            diag("fading, rendering " + this.minecraft.screen.getClass().getSimpleName() + " behind");
            this.minecraft.screen.render(param0, 0, 0, param3);
         }

         int var12 = Mth.ceil((1.0F - Mth.clamp(var9 - 1.0F, 0.0F, 1.0F)) * 255.0F);
         fill(param0, 0, 0, var5, var6, replaceAlpha(BRAND_BACKGROUND.getAsInt(), var12));
         var11 = 1.0F - Mth.clamp(var9 - 1.0F, 0.0F, 1.0F);
      } else if (this.fadeIn) {
         if (this.minecraft.screen != null && var10 < 1.0F) {
            diag("fadeIn, rendering " + this.minecraft.screen.getClass().getSimpleName() + " behind");
            this.minecraft.screen.render(param0, param1, param2, param3);
         }

         int var12 = Mth.ceil(Mth.clamp(var10, 0.15, 1.0) * 255.0);
         fill(param0, 0, 0, var5, var6, replaceAlpha(BRAND_BACKGROUND.getAsInt(), var12));
         var11 = Mth.clamp(var10, 0.0F, 1.0F);
      } else {
         int var12 = BRAND_BACKGROUND.getAsInt();
         float var13 = (var12 >> 16 & 0xFF) / 255.0F;
         float var14 = (var12 >> 8 & 0xFF) / 255.0F;
         float var15 = (var12 & 0xFF) / 255.0F;
         GlStateManager._clearColor(var13, var14, var15, 1.0F);
         GlStateManager._clear(16384, Minecraft.ON_OSX);
         var11 = 1.0F;
      }

      int var12 = (int)(this.minecraft.getWindow().getGuiScaledWidth() * 0.5);
      int var13 = (int)(this.minecraft.getWindow().getGuiScaledHeight() * 0.5);
      double var14 = Math.min(this.minecraft.getWindow().getGuiScaledWidth() * 0.75, this.minecraft.getWindow().getGuiScaledHeight()) * 0.25;
      int var16 = (int)(var14 * 0.5);
      double var17 = var14 * 4.0;
      int var19 = (int)(var17 * 0.5);
      RenderSystem.setShaderTexture(0, MOJANG_STUDIOS_LOGO_LOCATION);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var11);
      blit(param0, var12 - var19, var13 - var16, var19, (int)var14, -0.0625F, 0.0F, 120, 60, 120, 120);
      blit(param0, var12, var13 - var16, var19, (int)var14, 0.0625F, 60.0F, 120, 60, 120, 120);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      int var20 = (int)(this.minecraft.getWindow().getGuiScaledHeight() * 0.8325);
      float var21 = this.reload.getActualProgress();
      this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + var21 * 0.050000012F, 0.0F, 1.0F);
      if (var9 < 1.0F) {
         this.drawProgressBar(param0, var5 / 2 - var19, var20 - 5, var5 / 2 + var19, var20 + 5, 1.0F - Mth.clamp(var9, 0.0F, 1.0F));
      }

      if (var9 >= 2.0F) {
         diag("overlay finished, removing overlay");
         this.minecraft.setOverlay(null);
      }

      if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || var10 >= 2.0F)) {
         diag("reload done, starting fadeOut; screen="
            + (this.minecraft.screen == null ? "null" : this.minecraft.screen.getClass().getSimpleName()));
         try {
            this.reload.checkExceptions();
            this.onFinish.accept(Optional.empty());
         } catch (Throwable var22) {
            this.onFinish.accept(Optional.of(var22));
         }

         this.fadeOutStart = Util.getMillis();
         if (this.minecraft.screen != null) {
            this.minecraft.screen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
         }
      }
   }

   private void drawProgressBar(PoseStack param0, int param1, int param2, int param3, int param4, float param5) {
      int var7 = Mth.ceil((param3 - param1 - 2) * this.currentProgress);
      int var8 = Math.round(param5 * 255.0F);
      int var9 = FastColor.ARGB32.color(var8, 255, 255, 255);
      fill(param0, param1 + 2, param2 + 2, param1 + var7, param4 - 2, var9);
      fill(param0, param1 + 1, param2, param3 - 1, param2 + 1, var9);
      fill(param0, param1 + 1, param4, param3 - 1, param4 - 1, var9);
      fill(param0, param1, param2, param1 + 1, param4, var9);
      fill(param0, param3, param2, param3 - 1, param4, var9);
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   static class LogoTexture extends SimpleTexture {
      public LogoTexture() {
         super(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);
      }

      @Override
      protected SimpleTexture.TextureImage getTextureImage(ResourceManager param0) {
         VanillaPackResources var2 = Minecraft.getInstance().getVanillaPackResources();
         IoSupplier<InputStream> var3 = var2.getResource(PackType.CLIENT_RESOURCES, LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);
         if (var3 == null) {
            return new SimpleTexture.TextureImage(new FileNotFoundException(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION.toString()));
         }

         try (InputStream var4 = var3.get()) {
            return new SimpleTexture.TextureImage(new TextureMetadataSection(true, true), NativeImage.read(var4));
         } catch (IOException var4) {
            return new SimpleTexture.TextureImage(var4);
         }
      }
   }
}
