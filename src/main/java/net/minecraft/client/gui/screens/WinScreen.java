package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

public class WinScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   private static final Component SECTION_HEADING = Component.literal("============").withStyle(ChatFormatting.WHITE);
   private static final String NAME_PREFIX = "           ";
   private static final String OBFUSCATE_TOKEN = "" + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + ChatFormatting.GREEN + ChatFormatting.AQUA;
   private static final float SPEEDUP_FACTOR = 5.0F;
   private static final float SPEEDUP_FACTOR_FAST = 15.0F;
   private final boolean poem;
   private final Runnable onFinished;
   private float scroll;
   private List<FormattedCharSequence> lines;
   private IntSet centeredLines;
   private int totalScrollLength;
   private boolean speedupActive;
   private final IntSet speedupModifiers = new IntOpenHashSet();
   private float scrollSpeed;
   private final float unmodifiedScrollSpeed;
   private final LogoRenderer logoRenderer = new LogoRenderer(false);

   public WinScreen(boolean param0, Runnable param1) {
      super(GameNarrator.NO_TITLE);
      this.poem = param0;
      this.onFinished = param1;
      if (!param0) {
         this.unmodifiedScrollSpeed = 0.75F;
      } else {
         this.unmodifiedScrollSpeed = 0.5F;
      }

      this.scrollSpeed = this.unmodifiedScrollSpeed;
   }

   private float calculateScrollSpeed() {
      return this.speedupActive ? this.unmodifiedScrollSpeed * (5.0F + this.speedupModifiers.size() * 15.0F) : this.unmodifiedScrollSpeed;
   }

   @Override
   public void tick() {
      this.minecraft.getMusicManager().tick();
      this.minecraft.getSoundManager().tick(false);
      float var1 = this.totalScrollLength + this.height + this.height + 24;
      if (this.scroll > var1) {
         this.respawn();
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 341 || param0 == 345) {
         this.speedupModifiers.add(param0);
      } else if (param0 == 32) {
         this.speedupActive = true;
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyPressed(param0, param1, param2);
   }

   @Override
   public boolean keyReleased(int param0, int param1, int param2) {
      if (param0 == 32) {
         this.speedupActive = false;
      } else if (param0 == 341 || param0 == 345) {
         this.speedupModifiers.remove(param0);
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyReleased(param0, param1, param2);
   }

   @Override
   public void onClose() {
      this.respawn();
   }

   private void respawn() {
      this.onFinished.run();
   }

   @Override
   protected void init() {
      if (this.lines == null) {
         this.lines = Lists.newArrayList();
         this.centeredLines = new IntOpenHashSet();
         if (this.poem) {
            this.wrapCreditsIO("texts/end.txt", this::addPoemFile);
         }

         this.wrapCreditsIO("texts/credits.json", this::addCreditsFile);
         if (this.poem) {
            this.wrapCreditsIO("texts/postcredits.txt", this::addPoemFile);
         }

         this.totalScrollLength = this.lines.size() * 12;
      }
   }

   private void wrapCreditsIO(String param0, WinScreen.CreditsReader param1) {
      try (Reader var3 = this.minecraft.getResourceManager().openAsReader(new ResourceLocation(param0))) {
         param1.read(var3);
      } catch (Exception var3) {
         LOGGER.error("Couldn't load credits", var3);
      }
   }

   private void addPoemFile(Reader param0) throws IOException {
      BufferedReader var2 = new BufferedReader(param0);
      RandomSource var3 = RandomSource.create(8124371L);

      String var4;
      while ((var4 = var2.readLine()) != null) {
         var4 = var4.replaceAll("PLAYERNAME", this.minecraft.getUser().getName());

         int var5;
         while ((var5 = var4.indexOf(OBFUSCATE_TOKEN)) != -1) {
            String var6 = var4.substring(0, var5);
            String var7 = var4.substring(var5 + OBFUSCATE_TOKEN.length());
            var4 = var6 + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, var3.nextInt(4) + 3) + var7;
         }

         this.addPoemLines(var4);
         this.addEmptyLine();
      }

      for (int var5 = 0; var5 < 8; var5++) {
         this.addEmptyLine();
      }
   }

   private void addCreditsFile(Reader param0) {
      for (JsonElement var4 : GsonHelper.parseArray(param0)) {
         JsonObject var5 = var4.getAsJsonObject();
         String var6 = var5.get("section").getAsString();
         this.addCreditsLine(SECTION_HEADING, true);
         this.addCreditsLine(Component.literal(var6).withStyle(ChatFormatting.YELLOW), true);
         this.addCreditsLine(SECTION_HEADING, true);
         this.addEmptyLine();
         this.addEmptyLine();

         for (JsonElement var9 : var5.getAsJsonArray("titles")) {
            JsonObject var10 = var9.getAsJsonObject();
            String var11 = var10.get("title").getAsString();
            JsonArray var12 = var10.getAsJsonArray("names");
            this.addCreditsLine(Component.literal(var11).withStyle(ChatFormatting.GRAY), false);

            for (JsonElement var14 : var12) {
               String var15 = var14.getAsString();
               this.addCreditsLine(Component.literal("           ").append(var15).withStyle(ChatFormatting.WHITE), false);
            }

            this.addEmptyLine();
            this.addEmptyLine();
         }
      }
   }

   private void addEmptyLine() {
      this.lines.add(FormattedCharSequence.EMPTY);
   }

   private void addPoemLines(String param0) {
      this.lines.addAll(this.minecraft.font.split(Component.literal(param0), 274));
   }

   private void addCreditsLine(Component param0, boolean param1) {
      if (param1) {
         this.centeredLines.add(this.lines.size());
      }

      this.lines.add(param0.getVisualOrderText());
   }

   private void renderBg(PoseStack param0) {
      RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
      int var2 = this.width;
      float var3 = this.scroll * 0.5F;
      int var4 = 64;
      float var5 = this.scroll / this.unmodifiedScrollSpeed;
      float var6 = var5 * 0.02F;
      float var7 = (this.totalScrollLength + this.height + this.height + 24) / this.unmodifiedScrollSpeed;
      float var8 = (var7 - 20.0F - var5) * 0.005F;
      if (var8 < var6) {
         var6 = var8;
      }

      if (var6 > 1.0F) {
         var6 = 1.0F;
      }

      var6 *= var6;
      var6 = var6 * 96.0F / 255.0F;
      RenderSystem.setShaderColor(var6, var6, var6, 1.0F);
      blit(param0, 0, 0, 0, 0.0F, var3, var2, this.height, 64, 64);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.scroll = this.scroll + param3 * this.scrollSpeed;
      this.renderBg(param0);
      int var5 = this.width / 2 - 137;
      int var6 = this.height + 50;
      float var7 = -this.scroll;
      param0.pushPose();
      param0.translate(0.0F, var7, 0.0F);
      this.logoRenderer.renderLogo(param0, this.width, 1.0F, var6);
      int var8 = var6 + 100;

      for (int var9 = 0; var9 < this.lines.size(); var9++) {
         if (var9 == this.lines.size() - 1) {
            float var10 = var8 + var7 - (this.height / 2 - 6);
            if (var10 < 0.0F) {
               param0.translate(0.0F, -var10, 0.0F);
            }
         }

         if (var8 + var7 + 12.0F + 8.0F > 0.0F && var8 + var7 < this.height) {
            FormattedCharSequence var10 = this.lines.get(var9);
            if (this.centeredLines.contains(var9)) {
               this.font.drawShadow(param0, var10, var5 + (274 - this.font.width(var10)) / 2, var8, 16777215);
            } else {
               this.font.drawShadow(param0, var10, var5, var8, 16777215);
            }
         }

         var8 += 12;
      }

      param0.popPose();
      RenderSystem.setShaderTexture(0, VIGNETTE_LOCATION);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
      blit(param0, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      super.render(param0, param1, param2, param3);
   }

   @FunctionalInterface
   interface CreditsReader {
      void read(Reader var1) throws IOException;
   }
}
