package com.mojang.realmsclient.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.util.RealmsTextureManager;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RealmsWorldSlotButton extends Button {
   public static final ResourceLocation SLOT_FRAME_LOCATION = new ResourceLocation("realms", "textures/gui/realms/slot_frame.png");
   public static final ResourceLocation EMPTY_SLOT_LOCATION = new ResourceLocation("realms", "textures/gui/realms/empty_frame.png");
   public static final ResourceLocation CHECK_MARK_LOCATION = new ResourceLocation("minecraft", "textures/gui/checkmark.png");
   public static final ResourceLocation DEFAULT_WORLD_SLOT_1 = new ResourceLocation("minecraft", "textures/gui/title/background/panorama_0.png");
   public static final ResourceLocation DEFAULT_WORLD_SLOT_2 = new ResourceLocation("minecraft", "textures/gui/title/background/panorama_2.png");
   public static final ResourceLocation DEFAULT_WORLD_SLOT_3 = new ResourceLocation("minecraft", "textures/gui/title/background/panorama_3.png");
   private static final Component SLOT_ACTIVE_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.active");
   private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
   private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
   private final Supplier<RealmsServer> serverDataProvider;
   private final Consumer<Component> toolTipSetter;
   private final int slotIndex;
   @Nullable
   private RealmsWorldSlotButton.State state;

   public RealmsWorldSlotButton(
      int param0, int param1, int param2, int param3, Supplier<RealmsServer> param4, Consumer<Component> param5, int param6, Button.OnPress param7
   ) {
      super(param0, param1, param2, param3, CommonComponents.EMPTY, param7, DEFAULT_NARRATION);
      this.serverDataProvider = param4;
      this.slotIndex = param6;
      this.toolTipSetter = param5;
   }

   @Nullable
   public RealmsWorldSlotButton.State getState() {
      return this.state;
   }

   public void tick() {
      RealmsServer var1 = this.serverDataProvider.get();
      if (var1 != null) {
         RealmsWorldOptions var4 = var1.slots.get(this.slotIndex);
         boolean var9 = this.slotIndex == 4;
         boolean var2;
         String var3;
         long var5;
         String var7;
         boolean var8;
         if (var9) {
            var2 = var1.worldType == RealmsServer.WorldType.MINIGAME;
            var3 = "Minigame";
            var5 = var1.minigameId;
            var7 = var1.minigameImage;
            var8 = var1.minigameId == -1;
         } else {
            var2 = var1.activeSlot == this.slotIndex && var1.worldType != RealmsServer.WorldType.MINIGAME;
            var3 = var4.getSlotName(this.slotIndex);
            var5 = var4.templateId;
            var7 = var4.templateImage;
            var8 = var4.empty;
         }

         RealmsWorldSlotButton.Action var10 = getAction(var1, var2, var9);
         Pair<Component, Component> var11 = this.getTooltipAndNarration(var1, var3, var8, var9, var10);
         this.state = new RealmsWorldSlotButton.State(var2, var3, var5, var7, var8, var9, var10, var11.getFirst());
         this.setMessage(var11.getSecond());
      }
   }

   private static RealmsWorldSlotButton.Action getAction(RealmsServer param0, boolean param1, boolean param2) {
      if (param1) {
         if (!param0.expired && param0.state != RealmsServer.State.UNINITIALIZED) {
            return RealmsWorldSlotButton.Action.JOIN;
         }
      } else {
         if (!param2) {
            return RealmsWorldSlotButton.Action.SWITCH_SLOT;
         }

         if (!param0.expired) {
            return RealmsWorldSlotButton.Action.SWITCH_SLOT;
         }
      }

      return RealmsWorldSlotButton.Action.NOTHING;
   }

   private Pair<Component, Component> getTooltipAndNarration(
      RealmsServer param0, String param1, boolean param2, boolean param3, RealmsWorldSlotButton.Action param4
   ) {
      if (param4 == RealmsWorldSlotButton.Action.NOTHING) {
         return Pair.of(null, Component.literal(param1));
      }

      Component var6;
      if (param3) {
         if (param2) {
            var6 = CommonComponents.EMPTY;
         } else {
            var6 = CommonComponents.space().append(param1).append(CommonComponents.SPACE).append(param0.minigameName);
         }
      } else {
         var6 = CommonComponents.space().append(param1);
      }

      Component var7;
      if (param4 == RealmsWorldSlotButton.Action.JOIN) {
         var7 = SLOT_ACTIVE_TOOLTIP;
      } else {
         var7 = param3 ? SWITCH_TO_MINIGAME_SLOT_TOOLTIP : SWITCH_TO_WORLD_SLOT_TOOLTIP;
      }

      Component var8 = var7.copy().append(var6);
      return Pair.of(var7, var8);
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      if (this.state != null) {
         this.drawSlotFrame(
            param0,
            this.getX(),
            this.getY(),
            param1,
            param2,
            this.state.isCurrentlyActiveSlot,
            this.state.slotName,
            this.slotIndex,
            this.state.imageId,
            this.state.image,
            this.state.empty,
            this.state.minigame,
            this.state.action,
            this.state.actionPrompt
         );
      }
   }

   private void drawSlotFrame(
      PoseStack param0,
      int param1,
      int param2,
      int param3,
      int param4,
      boolean param5,
      String param6,
      int param7,
      long param8,
      @Nullable String param10,
      boolean param11,
      boolean param12,
      RealmsWorldSlotButton.Action param13,
      @Nullable Component param14
   ) {
      boolean var16 = this.isHoveredOrFocused();
      if (this.isMouseOver(param3, param4) && param14 != null) {
         this.toolTipSetter.accept(param14);
      }

      Minecraft var17 = Minecraft.getInstance();
      if (param12) {
         RenderSystem.setShaderTexture(0, RealmsTextureManager.worldTemplate(String.valueOf(param8), param10));
      } else if (param11) {
         RenderSystem.setShaderTexture(0, EMPTY_SLOT_LOCATION);
      } else if (param10 != null && param8 != -1L) {
         RenderSystem.setShaderTexture(0, RealmsTextureManager.worldTemplate(String.valueOf(param8), param10));
      } else if (param7 == 1) {
         RenderSystem.setShaderTexture(0, DEFAULT_WORLD_SLOT_1);
      } else if (param7 == 2) {
         RenderSystem.setShaderTexture(0, DEFAULT_WORLD_SLOT_2);
      } else if (param7 == 3) {
         RenderSystem.setShaderTexture(0, DEFAULT_WORLD_SLOT_3);
      }

      if (param5) {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      blit(param0, param1 + 3, param2 + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      RenderSystem.setShaderTexture(0, SLOT_FRAME_LOCATION);
      boolean var18 = var16 && param13 != RealmsWorldSlotButton.Action.NOTHING;
      if (var18) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else if (param5) {
         RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      blit(param0, param1, param2, 0.0F, 0.0F, 80, 80, 80, 80);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (param5) {
         this.renderCheckMark(param0, param1, param2);
      }

      drawCenteredString(param0, var17.font, param6, param1 + 40, param2 + 66, 16777215);
   }

   private void renderCheckMark(PoseStack param0, int param1, int param2) {
      RenderSystem.setShaderTexture(0, CHECK_MARK_LOCATION);
      RenderSystem.enableBlend();
      blit(param0, param1 + 67, param2 + 4, 0.0F, 0.0F, 9, 8, 9, 8);
      RenderSystem.disableBlend();
   }

   public enum Action {
      NOTHING,
      SWITCH_SLOT,
      JOIN;
   }

   public static class State {
      final boolean isCurrentlyActiveSlot;
      final String slotName;
      final long imageId;
      @Nullable
      final String image;
      public final boolean empty;
      public final boolean minigame;
      public final RealmsWorldSlotButton.Action action;
      @Nullable
      final Component actionPrompt;

      State(
         boolean param0,
         String param1,
         long param2,
         @Nullable String param4,
         boolean param5,
         boolean param6,
         RealmsWorldSlotButton.Action param7,
         @Nullable Component param8
      ) {
         this.isCurrentlyActiveSlot = param0;
         this.slotName = param1;
         this.imageId = param2;
         this.image = param4;
         this.empty = param5;
         this.minigame = param6;
         this.action = param7;
         this.actionPrompt = param8;
      }
   }
}
