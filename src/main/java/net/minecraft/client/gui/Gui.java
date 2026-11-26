package net.minecraft.client.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class Gui extends GuiComponent {
   private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   private static final ResourceLocation PUMPKIN_BLUR_LOCATION = new ResourceLocation("textures/misc/pumpkinblur.png");
   private static final ResourceLocation SPYGLASS_SCOPE_LOCATION = new ResourceLocation("textures/misc/spyglass_scope.png");
   private static final ResourceLocation POWDER_SNOW_OUTLINE_LOCATION = new ResourceLocation("textures/misc/powder_snow_outline.png");
   private static final Component DEMO_EXPIRED_TEXT = Component.translatable("demo.demoExpired");
   private static final Component SAVING_TEXT = Component.translatable("menu.savingLevel");
   private static final int COLOR_WHITE = 16777215;
   private static final float MIN_CROSSHAIR_ATTACK_SPEED = 5.0F;
   private static final int NUM_HEARTS_PER_ROW = 10;
   private static final int LINE_HEIGHT = 10;
   private static final String SPACER = ": ";
   private static final float PORTAL_OVERLAY_ALPHA_MIN = 0.2F;
   private static final int HEART_SIZE = 9;
   private static final int HEART_SEPARATION = 8;
   private static final float AUTOSAVE_FADE_SPEED_FACTOR = 0.2F;
   private final RandomSource random = RandomSource.create();
   private final Minecraft minecraft;
   private final ItemRenderer itemRenderer;
   private final ChatComponent chat;
   private int tickCount;
   @Nullable
   private Component overlayMessageString;
   private int overlayMessageTime;
   private boolean animateOverlayMessageColor;
   private boolean chatDisabledByPlayerShown;
   public float vignetteBrightness = 1.0F;
   private int toolHighlightTimer;
   private ItemStack lastToolHighlight = ItemStack.EMPTY;
   private final DebugScreenOverlay debugScreen;
   private final SubtitleOverlay subtitleOverlay;
   private final SpectatorGui spectatorGui;
   private final PlayerTabOverlay tabList;
   private final BossHealthOverlay bossOverlay;
   private int titleTime;
   @Nullable
   private Component title;
   @Nullable
   private Component subtitle;
   private int titleFadeInTime;
   private int titleStayTime;
   private int titleFadeOutTime;
   private int lastHealth;
   private int displayHealth;
   private long lastHealthTime;
   private long healthBlinkTime;
   private int screenWidth;
   private int screenHeight;
   private float autosaveIndicatorValue;
   private float lastAutosaveIndicatorValue;
   private float scopeScale;

   public Gui(Minecraft param0, ItemRenderer param1) {
      this.minecraft = param0;
      this.itemRenderer = param1;
      this.debugScreen = new DebugScreenOverlay(param0);
      this.spectatorGui = new SpectatorGui(param0);
      this.chat = new ChatComponent(param0);
      this.tabList = new PlayerTabOverlay(param0, this);
      this.bossOverlay = new BossHealthOverlay(param0);
      this.subtitleOverlay = new SubtitleOverlay(param0);
      this.resetTitleTimes();
   }

   public void resetTitleTimes() {
      this.titleFadeInTime = 10;
      this.titleStayTime = 70;
      this.titleFadeOutTime = 20;
   }

   public void render(PoseStack param0, float param1) {
      Window var3 = this.minecraft.getWindow();
      this.screenWidth = var3.getGuiScaledWidth();
      this.screenHeight = var3.getGuiScaledHeight();
      Font var4 = this.getFont();
      RenderSystem.enableBlend();
      if (Minecraft.useFancyGraphics()) {
         this.renderVignette(param0, this.minecraft.getCameraEntity());
      } else {
         RenderSystem.enableDepthTest();
      }

      float var5 = this.minecraft.getDeltaFrameTime();
      this.scopeScale = Mth.lerp(0.5F * var5, this.scopeScale, 1.125F);
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         if (this.minecraft.player.isScoping()) {
            this.renderSpyglassOverlay(param0, this.scopeScale);
         } else {
            this.scopeScale = 0.5F;
            ItemStack var6 = this.minecraft.player.getInventory().getArmor(3);
            if (var6.is(Blocks.CARVED_PUMPKIN.asItem())) {
               this.renderTextureOverlay(param0, PUMPKIN_BLUR_LOCATION, 1.0F);
            }
         }
      }

      if (this.minecraft.player.getTicksFrozen() > 0) {
         this.renderTextureOverlay(param0, POWDER_SNOW_OUTLINE_LOCATION, this.minecraft.player.getPercentFrozen());
      }

      float var6 = Mth.lerp(param1, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
      if (var6 > 0.0F && !this.minecraft.player.hasEffect(MobEffects.CONFUSION)) {
         this.renderPortalOverlay(param0, var6);
      }

      if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         this.spectatorGui.renderHotbar(param0);
      } else if (!this.minecraft.options.hideGui) {
         this.renderHotbar(param1, param0);
      }

      if (!this.minecraft.options.hideGui) {
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         RenderSystem.enableBlend();
         this.renderCrosshair(param0);
         this.minecraft.getProfiler().push("bossHealth");
         this.bossOverlay.render(param0);
         this.minecraft.getProfiler().pop();
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         if (this.minecraft.gameMode.canHurtPlayer()) {
            this.renderPlayerHealth(param0);
         }

         this.renderVehicleHealth(param0);
         RenderSystem.disableBlend();
         int var7 = this.screenWidth / 2 - 91;
         PlayerRideableJumping var8 = this.minecraft.player.jumpableVehicle();
         if (var8 != null) {
            this.renderJumpMeter(var8, param0, var7);
         } else if (this.minecraft.gameMode.hasExperience()) {
            this.renderExperienceBar(param0, var7);
         }

         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.renderSelectedItemName(param0);
         } else if (this.minecraft.player.isSpectator()) {
            this.spectatorGui.renderTooltip(param0);
         }
      }

      if (this.minecraft.player.getSleepTimer() > 0) {
         this.minecraft.getProfiler().push("sleep");
         RenderSystem.disableDepthTest();
         float var7 = this.minecraft.player.getSleepTimer();
         float var8 = var7 / 100.0F;
         if (var8 > 1.0F) {
            var8 = 1.0F - (var7 - 100.0F) / 10.0F;
         }

         int var9 = (int)(220.0F * var8) << 24 | 1052704;
         fill(param0, 0, 0, this.screenWidth, this.screenHeight, var9);
         RenderSystem.enableDepthTest();
         this.minecraft.getProfiler().pop();
      }

      if (this.minecraft.isDemo()) {
         this.renderDemoOverlay(param0);
      }

      this.renderEffects(param0);
      if (this.minecraft.options.renderDebug) {
         this.debugScreen.render(param0);
      }

      if (!this.minecraft.options.hideGui) {
         if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
            this.minecraft.getProfiler().push("overlayMessage");
            float var7 = this.overlayMessageTime - param1;
            int var8 = (int)(var7 * 255.0F / 20.0F);
            if (var8 > 255) {
               var8 = 255;
            }

            if (var8 > 8) {
               param0.pushPose();
               param0.translate(this.screenWidth / 2, this.screenHeight - 68, 0.0F);
               int var9 = 16777215;
               if (this.animateOverlayMessageColor) {
                  var9 = Mth.hsvToRgb(var7 / 50.0F, 0.7F, 0.6F) & 16777215;
               }

               int var10 = var8 << 24 & 0xFF000000;
               int var11 = var4.width(this.overlayMessageString);
               this.drawBackdrop(param0, var4, -4, var11, 16777215 | var10);
               var4.drawShadow(param0, this.overlayMessageString, -var11 / 2, -4.0F, var9 | var10);
               param0.popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         if (this.title != null && this.titleTime > 0) {
            this.minecraft.getProfiler().push("titleAndSubtitle");
            float var7 = this.titleTime - param1;
            int var8 = 255;
            if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
               float var9 = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime - var7;
               var8 = (int)(var9 * 255.0F / this.titleFadeInTime);
            }

            if (this.titleTime <= this.titleFadeOutTime) {
               var8 = (int)(var7 * 255.0F / this.titleFadeOutTime);
            }

            var8 = Mth.clamp(var8, 0, 255);
            if (var8 > 8) {
               param0.pushPose();
               param0.translate(this.screenWidth / 2, this.screenHeight / 2, 0.0F);
               RenderSystem.enableBlend();
               param0.pushPose();
               param0.scale(4.0F, 4.0F, 4.0F);
               int var9 = var8 << 24 & 0xFF000000;
               int var10 = var4.width(this.title);
               this.drawBackdrop(param0, var4, -10, var10, 16777215 | var9);
               var4.drawShadow(param0, this.title, -var10 / 2, -10.0F, 16777215 | var9);
               param0.popPose();
               if (this.subtitle != null) {
                  param0.pushPose();
                  param0.scale(2.0F, 2.0F, 2.0F);
                  int var11 = var4.width(this.subtitle);
                  this.drawBackdrop(param0, var4, 5, var11, 16777215 | var9);
                  var4.drawShadow(param0, this.subtitle, -var11 / 2, 5.0F, 16777215 | var9);
                  param0.popPose();
               }

               RenderSystem.disableBlend();
               param0.popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         this.subtitleOverlay.render(param0);
         Scoreboard var7 = this.minecraft.level.getScoreboard();
         Objective var8 = null;
         PlayerTeam var9 = var7.getPlayersTeam(this.minecraft.player.getScoreboardName());
         if (var9 != null) {
            int var10 = var9.getColor().getId();
            if (var10 >= 0) {
               var8 = var7.getDisplayObjective(3 + var10);
            }
         }

         Objective var10 = var8 != null ? var8 : var7.getDisplayObjective(1);
         if (var10 != null) {
            this.displayScoreboardSidebar(param0, var10);
         }

         RenderSystem.enableBlend();
         int var11 = Mth.floor(this.minecraft.mouseHandler.xpos() * var3.getGuiScaledWidth() / var3.getScreenWidth());
         int var12 = Mth.floor(this.minecraft.mouseHandler.ypos() * var3.getGuiScaledHeight() / var3.getScreenHeight());
         this.minecraft.getProfiler().push("chat");
         this.chat.render(param0, this.tickCount, var11, var12);
         this.minecraft.getProfiler().pop();
         var10 = var7.getDisplayObjective(0);
         if (!this.minecraft.options.keyPlayerList.isDown()
            || this.minecraft.isLocalServer() && this.minecraft.player.connection.getListedOnlinePlayers().size() <= 1 && var10 == null) {
            this.tabList.setVisible(false);
         } else {
            this.tabList.setVisible(true);
            this.tabList.render(param0, this.screenWidth, var7, var10);
         }

         this.renderSavingIndicator(param0);
      }
   }

   private void drawBackdrop(PoseStack param0, Font param1, int param2, int param3, int param4) {
      int var6 = this.minecraft.options.getBackgroundColor(0.0F);
      if (var6 != 0) {
         int var7 = -param3 / 2;
         fill(param0, var7 - 2, param2 - 2, var7 + param3 + 2, param2 + 9 + 2, FastColor.ARGB32.multiply(var6, param4));
      }
   }

   private void renderCrosshair(PoseStack param0) {
      Options var2 = this.minecraft.options;
      if (var2.getCameraType().isFirstPerson()) {
         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
            if (var2.renderDebug && !var2.hideGui && !this.minecraft.player.isReducedDebugInfo() && !var2.reducedDebugInfo().get()) {
               Camera var3 = this.minecraft.gameRenderer.getMainCamera();
               PoseStack var4 = RenderSystem.getModelViewStack();
               var4.pushPose();
               var4.mulPoseMatrix(param0.last().pose());
               var4.translate(this.screenWidth / 2, this.screenHeight / 2, 0.0F);
               var4.mulPose(Axis.XN.rotationDegrees(var3.getXRot()));
               var4.mulPose(Axis.YP.rotationDegrees(var3.getYRot()));
               var4.scale(-1.0F, -1.0F, -1.0F);
               RenderSystem.applyModelViewMatrix();
               RenderSystem.renderCrosshair(10);
               var4.popPose();
               RenderSystem.applyModelViewMatrix();
            } else {
               RenderSystem.blendFuncSeparate(
                  GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               int var3 = 15;
               blit(param0, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / 2, 0, 0, 15, 15);
               if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                  float var4 = this.minecraft.player.getAttackStrengthScale(0.0F);
                  boolean var5 = false;
                  if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && var4 >= 1.0F) {
                     var5 = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                     var5 &= this.minecraft.crosshairPickEntity.isAlive();
                  }

                  int var6 = this.screenHeight / 2 - 7 + 16;
                  int var7 = this.screenWidth / 2 - 8;
                  if (var5) {
                     blit(param0, var7, var6, 68, 94, 16, 16);
                  } else if (var4 < 1.0F) {
                     int var8 = (int)(var4 * 17.0F);
                     blit(param0, var7, var6, 36, 94, 16, 4);
                     blit(param0, var7, var6, 52, 94, var8, 4);
                  }
               }

               RenderSystem.defaultBlendFunc();
            }
         }
      }
   }

   private boolean canRenderCrosshairForSpectator(HitResult param0) {
      if (param0 == null) {
         return false;
      } else if (param0.getType() == HitResult.Type.ENTITY) {
         return ((EntityHitResult)param0).getEntity() instanceof MenuProvider;
      } else if (param0.getType() == HitResult.Type.BLOCK) {
         BlockPos var2 = ((BlockHitResult)param0).getBlockPos();
         Level var3 = this.minecraft.level;
         return var3.getBlockState(var2).getMenuProvider(var3, var2) != null;
      } else {
         return false;
      }
   }

   protected void renderEffects(PoseStack param0) {
      Collection<MobEffectInstance> var2 = this.minecraft.player.getActiveEffects();
      if (!var2.isEmpty() && !(this.minecraft.screen instanceof EffectRenderingInventoryScreen var3 && var3.canSeeEffects())) {
         RenderSystem.enableBlend();
         int var3 = 0;
         int var4 = 0;
         MobEffectTextureManager var5 = this.minecraft.getMobEffectTextures();
         List<Runnable> var6 = Lists.newArrayListWithExpectedSize(var2.size());
         RenderSystem.setShaderTexture(0, AbstractContainerScreen.INVENTORY_LOCATION);

         for (MobEffectInstance var8 : Ordering.natural().reverse().sortedCopy(var2)) {
            MobEffect var9 = var8.getEffect();
            if (var8.showIcon()) {
               int var10 = this.screenWidth;
               int var11 = 1;
               if (this.minecraft.isDemo()) {
                  var11 += 15;
               }

               if (var9.isBeneficial()) {
                  var3++;
                  var10 -= 25 * var3;
               } else {
                  var4++;
                  var10 -= 25 * var4;
                  var11 += 26;
               }

               float var12 = 1.0F;
               if (var8.isAmbient()) {
                  blit(param0, var10, var11, 165, 166, 24, 24);
               } else {
                  blit(param0, var10, var11, 141, 166, 24, 24);
                  if (var8.endsWithin(200)) {
                     int var13 = var8.getDuration();
                     int var14 = 10 - var13 / 20;
                     var12 = Mth.clamp(var13 / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                        + Mth.cos(var13 * (float) Math.PI / 5.0F) * Mth.clamp(var14 / 10.0F * 0.25F, 0.0F, 0.25F);
                  }
               }

               TextureAtlasSprite var13 = var5.get(var9);
               int var14 = var10;
               int var15 = var11;
               float var16 = var12;
               var6.add(() -> {
                  RenderSystem.setShaderTexture(0, var13.atlasLocation());
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var16);
                  blit(param0, var14 + 3, var15 + 3, 0, 18, 18, var13);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               });
            }
         }

         var6.forEach(Runnable::run);
      }
   }

   private void renderHotbar(float param0, PoseStack param1) {
      Player var3 = this.getCameraPlayer();
      if (var3 != null) {
         RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
         ItemStack var4 = var3.getOffhandItem();
         HumanoidArm var5 = var3.getMainArm().getOpposite();
         int var6 = this.screenWidth / 2;
         int var7 = 182;
         int var8 = 91;
         param1.pushPose();
         param1.translate(0.0F, 0.0F, -90.0F);
         blit(param1, var6 - 91, this.screenHeight - 22, 0, 0, 182, 22);
         blit(param1, var6 - 91 - 1 + var3.getInventory().selected * 20, this.screenHeight - 22 - 1, 0, 22, 24, 22);
         if (!var4.isEmpty()) {
            if (var5 == HumanoidArm.LEFT) {
               blit(param1, var6 - 91 - 29, this.screenHeight - 23, 24, 22, 29, 24);
            } else {
               blit(param1, var6 + 91, this.screenHeight - 23, 53, 22, 29, 24);
            }
         }

         param1.popPose();
         int var9 = 1;

         for (int var10 = 0; var10 < 9; var10++) {
            int var11 = var6 - 90 + var10 * 20 + 2;
            int var12 = this.screenHeight - 16 - 3;
            this.renderSlot(param1, var11, var12, param0, var3, var3.getInventory().items.get(var10), var9++);
         }

         if (!var4.isEmpty()) {
            int var10 = this.screenHeight - 16 - 3;
            if (var5 == HumanoidArm.LEFT) {
               this.renderSlot(param1, var6 - 91 - 26, var10, param0, var3, var4, var9++);
            } else {
               this.renderSlot(param1, var6 + 91 + 10, var10, param0, var3, var4, var9++);
            }
         }

         RenderSystem.enableBlend();
         if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float var10 = this.minecraft.player.getAttackStrengthScale(0.0F);
            if (var10 < 1.0F) {
               int var11 = this.screenHeight - 20;
               int var12 = var6 + 91 + 6;
               if (var5 == HumanoidArm.RIGHT) {
                  var12 = var6 - 91 - 22;
               }

               RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
               int var13 = (int)(var10 * 19.0F);
               blit(param1, var12, var11, 0, 94, 18, 18);
               blit(param1, var12, var11 + 18 - var13, 18, 112 - var13, 18, var13);
            }
         }

         RenderSystem.disableBlend();
      }
   }

   public void renderJumpMeter(PlayerRideableJumping param0, PoseStack param1, int param2) {
      this.minecraft.getProfiler().push("jumpBar");
      RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
      float var4 = this.minecraft.player.getJumpRidingScale();
      int var5 = 182;
      int var6 = (int)(var4 * 183.0F);
      int var7 = this.screenHeight - 32 + 3;
      blit(param1, param2, var7, 0, 84, 182, 5);
      if (param0.getJumpCooldown() > 0) {
         blit(param1, param2, var7, 0, 74, 182, 5);
      } else if (var6 > 0) {
         blit(param1, param2, var7, 0, 89, var6, 5);
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderExperienceBar(PoseStack param0, int param1) {
      this.minecraft.getProfiler().push("expBar");
      RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
      int var3 = this.minecraft.player.getXpNeededForNextLevel();
      if (var3 > 0) {
         int var4 = 182;
         int var5 = (int)(this.minecraft.player.experienceProgress * 183.0F);
         int var6 = this.screenHeight - 32 + 3;
         blit(param0, param1, var6, 0, 64, 182, 5);
         if (var5 > 0) {
            blit(param0, param1, var6, 0, 69, var5, 5);
         }
      }

      this.minecraft.getProfiler().pop();
      if (this.minecraft.player.experienceLevel > 0) {
         this.minecraft.getProfiler().push("expLevel");
         String var4 = this.minecraft.player.experienceLevel + "";
         int var5 = (this.screenWidth - this.getFont().width(var4)) / 2;
         int var6 = this.screenHeight - 31 - 4;
         this.getFont().draw(param0, var4, var5 + 1, var6, 0);
         this.getFont().draw(param0, var4, var5 - 1, var6, 0);
         this.getFont().draw(param0, var4, var5, var6 + 1, 0);
         this.getFont().draw(param0, var4, var5, var6 - 1, 0);
         this.getFont().draw(param0, var4, var5, var6, 8453920);
         this.minecraft.getProfiler().pop();
      }
   }

   public void renderSelectedItemName(PoseStack param0) {
      this.minecraft.getProfiler().push("selectedItemName");
      if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
         MutableComponent var2 = Component.empty().append(this.lastToolHighlight.getHoverName()).withStyle(this.lastToolHighlight.getRarity().color);
         if (this.lastToolHighlight.hasCustomHoverName()) {
            var2.withStyle(ChatFormatting.ITALIC);
         }

         int var3 = this.getFont().width(var2);
         int var4 = (this.screenWidth - var3) / 2;
         int var5 = this.screenHeight - 59;
         if (!this.minecraft.gameMode.canHurtPlayer()) {
            var5 += 14;
         }

         int var6 = (int)(this.toolHighlightTimer * 256.0F / 10.0F);
         if (var6 > 255) {
            var6 = 255;
         }

         if (var6 > 0) {
            fill(param0, var4 - 2, var5 - 2, var4 + var3 + 2, var5 + 9 + 2, this.minecraft.options.getBackgroundColor(0));
            this.getFont().drawShadow(param0, var2, var4, var5, 16777215 + (var6 << 24));
         }
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderDemoOverlay(PoseStack param0) {
      this.minecraft.getProfiler().push("demo");
      Component var2;
      if (this.minecraft.level.getGameTime() >= 120500L) {
         var2 = DEMO_EXPIRED_TEXT;
      } else {
         var2 = Component.translatable("demo.remainingTime", StringUtil.formatTickDuration((int)(120500L - this.minecraft.level.getGameTime())));
      }

      int var3 = this.getFont().width(var2);
      this.getFont().drawShadow(param0, var2, this.screenWidth - var3 - 10, 5.0F, 16777215);
      this.minecraft.getProfiler().pop();
   }

   private void displayScoreboardSidebar(PoseStack param0, Objective param1) {
      Scoreboard var3 = param1.getScoreboard();
      Collection<Score> var4 = var3.getPlayerScores(param1);
      List<Score> var5 = var4.stream().filter(l_param0 -> l_param0.getOwner() != null && !l_param0.getOwner().startsWith("#")).collect(Collectors.toList());
      if (var5.size() > 15) {
         var4 = Lists.newArrayList(Iterables.skip(var5, var4.size() - 15));
      } else {
         var4 = var5;
      }

      List<Pair<Score, Component>> var6 = Lists.newArrayListWithCapacity(var4.size());
      Component var7 = param1.getDisplayName();
      int var8 = this.getFont().width(var7);
      int var9 = var8;
      int var10 = this.getFont().width(": ");

      for (Score var12 : var4) {
         PlayerTeam var13 = var3.getPlayersTeam(var12.getOwner());
         Component var14 = PlayerTeam.formatNameForTeam(var13, Component.literal(var12.getOwner()));
         var6.add(Pair.of(var12, var14));
         var9 = Math.max(var9, this.getFont().width(var14) + var10 + this.getFont().width(Integer.toString(var12.getScore())));
      }

      int var11 = var4.size() * 9;
      int var12 = this.screenHeight / 2 + var11 / 3;
      int var13 = 3;
      int var14 = this.screenWidth - var9 - 3;
      int var15 = 0;
      int var16 = this.minecraft.options.getBackgroundColor(0.3F);
      int var17 = this.minecraft.options.getBackgroundColor(0.4F);

      for (Pair<Score, Component> var19 : var6) {
         var15++;
         Score var20 = var19.getFirst();
         Component var21 = var19.getSecond();
         String var22 = "" + ChatFormatting.RED + var20.getScore();
         int var23 = var14;
         int var24 = var12 - var15 * 9;
         int var25 = this.screenWidth - 3 + 2;
         fill(param0, var23 - 2, var24, var25, var24 + 9, var16);
         this.getFont().draw(param0, var21, var23, var24, -1);
         this.getFont().draw(param0, var22, var25 - this.getFont().width(var22), var24, -1);
         if (var15 == var4.size()) {
            fill(param0, var23 - 2, var24 - 9 - 1, var25, var24 - 1, var17);
            fill(param0, var23 - 2, var24 - 1, var25, var24, var16);
            this.getFont().draw(param0, var7, var23 + var9 / 2 - var8 / 2, var24 - 9, -1);
         }
      }
   }

   private Player getCameraPlayer() {
      return !(this.minecraft.getCameraEntity() instanceof Player) ? null : (Player)this.minecraft.getCameraEntity();
   }

   private LivingEntity getPlayerVehicleWithHealth() {
      Player var1 = this.getCameraPlayer();
      if (var1 != null) {
         Entity var2 = var1.getVehicle();
         if (var2 == null) {
            return null;
         }

         if (var2 instanceof LivingEntity) {
            return (LivingEntity)var2;
         }
      }

      return null;
   }

   private int getVehicleMaxHearts(LivingEntity param0) {
      if (param0 != null && param0.showVehicleHealth()) {
         float var2 = param0.getMaxHealth();
         int var3 = (int)(var2 + 0.5F) / 2;
         if (var3 > 30) {
            var3 = 30;
         }

         return var3;
      } else {
         return 0;
      }
   }

   private int getVisibleVehicleHeartRows(int param0) {
      return (int)Math.ceil(param0 / 10.0);
   }

   private void renderPlayerHealth(PoseStack param0) {
      Player var2 = this.getCameraPlayer();
      if (var2 != null) {
         int var3 = Mth.ceil(var2.getHealth());
         boolean var4 = this.healthBlinkTime > this.tickCount && (this.healthBlinkTime - this.tickCount) / 3L % 2L == 1L;
         long var5 = Util.getMillis();
         if (var3 < this.lastHealth && var2.invulnerableTime > 0) {
            this.lastHealthTime = var5;
            this.healthBlinkTime = this.tickCount + 20;
         } else if (var3 > this.lastHealth && var2.invulnerableTime > 0) {
            this.lastHealthTime = var5;
            this.healthBlinkTime = this.tickCount + 10;
         }

         if (var5 - this.lastHealthTime > 1000L) {
            this.lastHealth = var3;
            this.displayHealth = var3;
            this.lastHealthTime = var5;
         }

         this.lastHealth = var3;
         int var7 = this.displayHealth;
         this.random.setSeed(this.tickCount * 312871);
         FoodData var8 = var2.getFoodData();
         int var9 = var8.getFoodLevel();
         int var10 = this.screenWidth / 2 - 91;
         int var11 = this.screenWidth / 2 + 91;
         int var12 = this.screenHeight - 39;
         float var13 = Math.max((float)var2.getAttributeValue(Attributes.MAX_HEALTH), Math.max(var7, var3));
         int var14 = Mth.ceil(var2.getAbsorptionAmount());
         int var15 = Mth.ceil((var13 + var14) / 2.0F / 10.0F);
         int var16 = Math.max(10 - (var15 - 2), 3);
         int var17 = var12 - (var15 - 1) * var16 - 10;
         int var18 = var12 - 10;
         int var19 = var2.getArmorValue();
         int var20 = -1;
         if (var2.hasEffect(MobEffects.REGENERATION)) {
            var20 = this.tickCount % Mth.ceil(var13 + 5.0F);
         }

         this.minecraft.getProfiler().push("armor");

         for (int var21 = 0; var21 < 10; var21++) {
            if (var19 > 0) {
               int var22 = var10 + var21 * 8;
               if (var21 * 2 + 1 < var19) {
                  blit(param0, var22, var17, 34, 9, 9, 9);
               }

               if (var21 * 2 + 1 == var19) {
                  blit(param0, var22, var17, 25, 9, 9, 9);
               }

               if (var21 * 2 + 1 > var19) {
                  blit(param0, var22, var17, 16, 9, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().popPush("health");
         this.renderHearts(param0, var2, var10, var12, var16, var20, var13, var3, var7, var14, var4);
         LivingEntity var21 = this.getPlayerVehicleWithHealth();
         int var22 = this.getVehicleMaxHearts(var21);
         if (var22 == 0) {
            this.minecraft.getProfiler().popPush("food");

            for (int var23 = 0; var23 < 10; var23++) {
               int var24 = var12;
               int var25 = 16;
               int var26 = 0;
               if (var2.hasEffect(MobEffects.HUNGER)) {
                  var25 += 36;
                  var26 = 13;
               }

               if (var2.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (var9 * 3 + 1) == 0) {
                  var24 += this.random.nextInt(3) - 1;
               }

               int var27 = var11 - var23 * 8 - 9;
               blit(param0, var27, var24, 16 + var26 * 9, 27, 9, 9);
               if (var23 * 2 + 1 < var9) {
                  blit(param0, var27, var24, var25 + 36, 27, 9, 9);
               }

               if (var23 * 2 + 1 == var9) {
                  blit(param0, var27, var24, var25 + 45, 27, 9, 9);
               }
            }

            var18 -= 10;
         }

         this.minecraft.getProfiler().popPush("air");
         int var23 = var2.getMaxAirSupply();
         int var24 = Math.min(var2.getAirSupply(), var23);
         if (var2.isEyeInFluid(FluidTags.WATER) || var24 < var23) {
            int var25 = this.getVisibleVehicleHeartRows(var22) - 1;
            var18 -= var25 * 10;
            int var26 = Mth.ceil((var24 - 2) * 10.0 / var23);
            int var27 = Mth.ceil(var24 * 10.0 / var23) - var26;

            for (int var28 = 0; var28 < var26 + var27; var28++) {
               if (var28 < var26) {
                  blit(param0, var11 - var28 * 8 - 9, var18, 16, 18, 9, 9);
               } else {
                  blit(param0, var11 - var28 * 8 - 9, var18, 25, 18, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }
   }

   private void renderHearts(
      PoseStack param0, Player param1, int param2, int param3, int param4, int param5, float param6, int param7, int param8, int param9, boolean param10
   ) {
      Gui.HeartType var12 = Gui.HeartType.forPlayer(param1);
      int var13 = 9 * (param1.level.getLevelData().isHardcore() ? 5 : 0);
      int var14 = Mth.ceil(param6 / 2.0);
      int var15 = Mth.ceil(param9 / 2.0);
      int var16 = var14 * 2;

      for (int var17 = var14 + var15 - 1; var17 >= 0; var17--) {
         int var18 = var17 / 10;
         int var19 = var17 % 10;
         int var20 = param2 + var19 * 8;
         int var21 = param3 - var18 * param4;
         if (param7 + param9 <= 4) {
            var21 += this.random.nextInt(2);
         }

         if (var17 < var14 && var17 == param5) {
            var21 -= 2;
         }

         this.renderHeart(param0, Gui.HeartType.CONTAINER, var20, var21, var13, param10, false);
         int var22 = var17 * 2;
         boolean var23 = var17 >= var14;
         if (var23) {
            int var24 = var22 - var16;
            if (var24 < param9) {
               boolean var25 = var24 + 1 == param9;
               this.renderHeart(param0, var12 == Gui.HeartType.WITHERED ? var12 : Gui.HeartType.ABSORBING, var20, var21, var13, false, var25);
            }
         }

         if (param10 && var22 < param8) {
            boolean var24 = var22 + 1 == param8;
            this.renderHeart(param0, var12, var20, var21, var13, true, var24);
         }

         if (var22 < param7) {
            boolean var24 = var22 + 1 == param7;
            this.renderHeart(param0, var12, var20, var21, var13, false, var24);
         }
      }
   }

   private void renderHeart(PoseStack param0, Gui.HeartType param1, int param2, int param3, int param4, boolean param5, boolean param6) {
      blit(param0, param2, param3, param1.getX(param6, param5), param4, 9, 9);
   }

   private void renderVehicleHealth(PoseStack param0) {
      LivingEntity var2 = this.getPlayerVehicleWithHealth();
      if (var2 != null) {
         int var3 = this.getVehicleMaxHearts(var2);
         if (var3 != 0) {
            int var4 = (int)Math.ceil(var2.getHealth());
            this.minecraft.getProfiler().popPush("mountHealth");
            int var5 = this.screenHeight - 39;
            int var6 = this.screenWidth / 2 + 91;
            int var7 = var5;
            int var8 = 0;
            boolean var9 = false;

            while (var3 > 0) {
               int var10 = Math.min(var3, 10);
               var3 -= var10;

               for (int var11 = 0; var11 < var10; var11++) {
                  int var12 = 52;
                  int var13 = 0;
                  int var14 = var6 - var11 * 8 - 9;
                  blit(param0, var14, var7, 52 + var13 * 9, 9, 9, 9);
                  if (var11 * 2 + 1 + var8 < var4) {
                     blit(param0, var14, var7, 88, 9, 9, 9);
                  }

                  if (var11 * 2 + 1 + var8 == var4) {
                     blit(param0, var14, var7, 97, 9, 9, 9);
                  }
               }

               var7 -= 10;
               var8 += 20;
            }
         }
      }
   }

   private void renderTextureOverlay(PoseStack param0, ResourceLocation param1, float param2) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, param2);
      RenderSystem.setShaderTexture(0, param1);
      blit(param0, 0, 0, -90, 0.0F, 0.0F, this.screenWidth, this.screenHeight, this.screenWidth, this.screenHeight);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSpyglassOverlay(PoseStack param0, float param1) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      float var3 = Math.min(this.screenWidth, this.screenHeight);
      float var4 = var3;
      float var5 = Math.min(this.screenWidth / var3, this.screenHeight / var4) * param1;
      int var6 = Mth.floor(var3 * var5);
      int var7 = Mth.floor(var4 * var5);
      int var8 = (this.screenWidth - var6) / 2;
      int var9 = (this.screenHeight - var7) / 2;
      int var10 = var8 + var6;
      int var11 = var9 + var7;
      RenderSystem.setShaderTexture(0, SPYGLASS_SCOPE_LOCATION);
      blit(param0, var8, var9, -90, 0.0F, 0.0F, var6, var7, var6, var7);
      fill(param0, 0, var11, this.screenWidth, this.screenHeight, -90, -16777216);
      fill(param0, 0, 0, this.screenWidth, var9, -90, -16777216);
      fill(param0, 0, var9, var8, var11, -90, -16777216);
      fill(param0, var10, var9, this.screenWidth, var11, -90, -16777216);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   private void updateVignetteBrightness(Entity param0) {
      if (param0 != null) {
         BlockPos var2 = BlockPos.containing(param0.getX(), param0.getEyeY(), param0.getZ());
         float var3 = LightTexture.getBrightness(param0.level.dimensionType(), param0.level.getMaxLocalRawBrightness(var2));
         float var4 = Mth.clamp(1.0F - var3, 0.0F, 1.0F);
         this.vignetteBrightness = this.vignetteBrightness + (var4 - this.vignetteBrightness) * 0.01F;
      }
   }

   private void renderVignette(PoseStack param0, Entity param1) {
      WorldBorder var3 = this.minecraft.level.getWorldBorder();
      float var4 = (float)var3.getDistanceToBorder(param1);
      double var5 = Math.min(var3.getLerpSpeed() * var3.getWarningTime() * 1000.0, Math.abs(var3.getLerpTarget() - var3.getSize()));
      double var7 = Math.max(var3.getWarningBlocks(), var5);
      if (var4 < var7) {
         var4 = 1.0F - (float)(var4 / var7);
      } else {
         var4 = 0.0F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.blendFuncSeparate(
         GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      if (var4 > 0.0F) {
         var4 = Mth.clamp(var4, 0.0F, 1.0F);
         RenderSystem.setShaderColor(0.0F, var4, var4, 1.0F);
      } else {
         float var9 = this.vignetteBrightness;
         var9 = Mth.clamp(var9, 0.0F, 1.0F);
         RenderSystem.setShaderColor(var9, var9, var9, 1.0F);
      }

      RenderSystem.setShaderTexture(0, VIGNETTE_LOCATION);
      blit(param0, 0, 0, -90, 0.0F, 0.0F, this.screenWidth, this.screenHeight, this.screenWidth, this.screenHeight);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
   }

   private void renderPortalOverlay(PoseStack param0, float param1) {
      if (param1 < 1.0F) {
         param1 *= param1;
         param1 *= param1;
         param1 = param1 * 0.8F + 0.2F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, param1);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
      TextureAtlasSprite var3 = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
      blit(param0, 0, 0, -90, this.screenWidth, this.screenHeight, var3);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSlot(PoseStack param0, int param1, int param2, float param3, Player param4, ItemStack param5, int param6) {
      if (!param5.isEmpty()) {
         float var8 = param5.getPopTime() - param3;
         if (var8 > 0.0F) {
            float var9 = 1.0F + var8 / 5.0F;
            param0.pushPose();
            param0.translate(param1 + 8, param2 + 12, 0.0F);
            param0.scale(1.0F / var9, (var9 + 1.0F) / 2.0F, 1.0F);
            param0.translate(-(param1 + 8), -(param2 + 12), 0.0F);
         }

         this.itemRenderer.renderAndDecorateItem(param0, param4, param5, param1, param2, param6);
         if (var8 > 0.0F) {
            param0.popPose();
         }

         this.itemRenderer.renderGuiItemDecorations(param0, this.minecraft.font, param5, param1, param2);
      }
   }

   public void tick(boolean param0) {
      this.tickAutosaveIndicator();
      if (!param0) {
         this.tick();
      }
   }

   private void tick() {
      if (this.overlayMessageTime > 0) {
         this.overlayMessageTime--;
      }

      if (this.titleTime > 0) {
         this.titleTime--;
         if (this.titleTime <= 0) {
            this.title = null;
            this.subtitle = null;
         }
      }

      this.tickCount++;
      Entity var1 = this.minecraft.getCameraEntity();
      if (var1 != null) {
         this.updateVignetteBrightness(var1);
      }

      if (this.minecraft.player != null) {
         ItemStack var2 = this.minecraft.player.getInventory().getSelected();
         if (var2.isEmpty()) {
            this.toolHighlightTimer = 0;
         } else if (this.lastToolHighlight.isEmpty()
            || !var2.is(this.lastToolHighlight.getItem())
            || !var2.getHoverName().equals(this.lastToolHighlight.getHoverName())) {
            this.toolHighlightTimer = (int)(40.0 * this.minecraft.options.notificationDisplayTime().get());
         } else if (this.toolHighlightTimer > 0) {
            this.toolHighlightTimer--;
         }

         this.lastToolHighlight = var2;
      }

      this.chat.tick();
   }

   private void tickAutosaveIndicator() {
      MinecraftServer var1 = this.minecraft.getSingleplayerServer();
      boolean var2 = var1 != null && var1.isCurrentlySaving();
      this.lastAutosaveIndicatorValue = this.autosaveIndicatorValue;
      this.autosaveIndicatorValue = Mth.lerp(0.2F, this.autosaveIndicatorValue, var2 ? 1.0F : 0.0F);
   }

   public void setNowPlaying(Component param0) {
      Component var2 = Component.translatable("record.nowPlaying", param0);
      this.setOverlayMessage(var2, true);
      this.minecraft.getNarrator().sayNow(var2);
   }

   public void setOverlayMessage(Component param0, boolean param1) {
      this.setChatDisabledByPlayerShown(false);
      this.overlayMessageString = param0;
      this.overlayMessageTime = 60;
      this.animateOverlayMessageColor = param1;
   }

   public void setChatDisabledByPlayerShown(boolean param0) {
      this.chatDisabledByPlayerShown = param0;
   }

   public boolean isShowingChatDisabledByPlayer() {
      return this.chatDisabledByPlayerShown && this.overlayMessageTime > 0;
   }

   public void setTimes(int param0, int param1, int param2) {
      if (param0 >= 0) {
         this.titleFadeInTime = param0;
      }

      if (param1 >= 0) {
         this.titleStayTime = param1;
      }

      if (param2 >= 0) {
         this.titleFadeOutTime = param2;
      }

      if (this.titleTime > 0) {
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
      }
   }

   public void setSubtitle(Component param0) {
      this.subtitle = param0;
   }

   public void setTitle(Component param0) {
      this.title = param0;
      this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
   }

   public void clear() {
      this.title = null;
      this.subtitle = null;
      this.titleTime = 0;
   }

   public ChatComponent getChat() {
      return this.chat;
   }

   public int getGuiTicks() {
      return this.tickCount;
   }

   public Font getFont() {
      return this.minecraft.font;
   }

   public SpectatorGui getSpectatorGui() {
      return this.spectatorGui;
   }

   public PlayerTabOverlay getTabList() {
      return this.tabList;
   }

   public void onDisconnected() {
      this.tabList.reset();
      this.bossOverlay.reset();
      this.minecraft.getToasts().clear();
      this.minecraft.options.renderDebug = false;
      this.chat.clearMessages(true);
   }

   public BossHealthOverlay getBossOverlay() {
      return this.bossOverlay;
   }

   public void clearCache() {
      this.debugScreen.clearChunkCache();
   }

   private void renderSavingIndicator(PoseStack param0) {
      if (this.minecraft.options.showAutosaveIndicator().get() && (this.autosaveIndicatorValue > 0.0F || this.lastAutosaveIndicatorValue > 0.0F)) {
         int var2 = Mth.floor(
            255.0F * Mth.clamp(Mth.lerp(this.minecraft.getFrameTime(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0F, 1.0F)
         );
         if (var2 > 8) {
            Font var3 = this.getFont();
            int var4 = var3.width(SAVING_TEXT);
            int var5 = 16777215 | var2 << 24 & 0xFF000000;
            var3.drawShadow(param0, SAVING_TEXT, this.screenWidth - var4 - 10, this.screenHeight - 15, var5);
         }
      }
   }

   enum HeartType {
      CONTAINER(0, false),
      NORMAL(2, true),
      POISIONED(4, true),
      WITHERED(6, true),
      ABSORBING(8, false),
      FROZEN(9, false);

      private final int index;
      private final boolean canBlink;

      HeartType(int param2, boolean param3) {
         this.index = param2;
         this.canBlink = param3;
      }

      public int getX(boolean param0, boolean param1) {
         int var3;
         if (this == CONTAINER) {
            var3 = param1 ? 1 : 0;
         } else {
            int var4 = param0 ? 1 : 0;
            int var5 = this.canBlink && param1 ? 2 : 0;
            var3 = var4 + var5;
         }

         return 16 + (this.index * 2 + var3) * 9;
      }

      static Gui.HeartType forPlayer(Player param0) {
         Gui.HeartType var1;
         if (param0.hasEffect(MobEffects.POISON)) {
            var1 = POISIONED;
         } else if (param0.hasEffect(MobEffects.WITHER)) {
            var1 = WITHERED;
         } else if (param0.isFullyFrozen()) {
            var1 = FROZEN;
         } else {
            var1 = NORMAL;
         }

         return var1;
      }
   }
}
