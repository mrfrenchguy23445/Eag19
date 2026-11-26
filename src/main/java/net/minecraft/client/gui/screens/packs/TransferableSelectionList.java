package net.minecraft.client.gui.screens.packs;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.util.FormattedCharSequence;

public class TransferableSelectionList extends ObjectSelectionList<TransferableSelectionList.PackEntry> {
   static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/resource_packs.png");
   static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
   static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
   private final Component title;
   final PackSelectionScreen screen;

   public TransferableSelectionList(Minecraft param0, PackSelectionScreen param1, int param2, int param3, Component param4) {
      super(param0, param2, param3, 32, param3 - 55 + 4, 36);
      this.screen = param1;
      this.title = param4;
      this.centerListVertically = false;
      this.setRenderHeader(true, (int)(9.0F * 1.5F));
   }

   @Override
   protected void renderHeader(PoseStack param0, int param1, int param2) {
      Component var4 = Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
      this.minecraft.font.draw(param0, var4, param1 + this.width / 2 - this.minecraft.font.width(var4) / 2, Math.min(this.y0 + 3, param2), 16777215);
   }

   @Override
   public int getRowWidth() {
      return this.width;
   }

   @Override
   protected int getScrollbarPosition() {
      return this.x1 - 6;
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (this.getSelected() != null) {
         switch (param0) {
            case 32:
            case 257:
               this.getSelected().keyboardSelection();
               return true;
            default:
               if (Screen.hasShiftDown()) {
                  switch (param0) {
                     case 264:
                        this.getSelected().keyboardMoveDown();
                        return true;
                     case 265:
                        this.getSelected().keyboardMoveUp();
                        return true;
                  }
               }
         }
      }

      return super.keyPressed(param0, param1, param2);
   }

   public static class PackEntry extends ObjectSelectionList.Entry<TransferableSelectionList.PackEntry> {
      private static final int ICON_OVERLAY_X_MOVE_RIGHT = 0;
      private static final int ICON_OVERLAY_X_MOVE_LEFT = 32;
      private static final int ICON_OVERLAY_X_MOVE_DOWN = 64;
      private static final int ICON_OVERLAY_X_MOVE_UP = 96;
      private static final int ICON_OVERLAY_Y_UNSELECTED = 0;
      private static final int ICON_OVERLAY_Y_SELECTED = 32;
      private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;
      private static final int MAX_NAME_WIDTH_PIXELS = 157;
      private static final String TOO_LONG_NAME_SUFFIX = "...";
      private final TransferableSelectionList parent;
      protected final Minecraft minecraft;
      private final PackSelectionModel.Entry pack;
      private final FormattedCharSequence nameDisplayCache;
      private final MultiLineLabel descriptionDisplayCache;
      private final FormattedCharSequence incompatibleNameDisplayCache;
      private final MultiLineLabel incompatibleDescriptionDisplayCache;

      public PackEntry(Minecraft param0, TransferableSelectionList param1, PackSelectionModel.Entry param2) {
         this.minecraft = param0;
         this.pack = param2;
         this.parent = param1;
         this.nameDisplayCache = cacheName(param0, param2.getTitle());
         this.descriptionDisplayCache = cacheDescription(param0, param2.getExtendedDescription());
         this.incompatibleNameDisplayCache = cacheName(param0, TransferableSelectionList.INCOMPATIBLE_TITLE);
         this.incompatibleDescriptionDisplayCache = cacheDescription(param0, param2.getCompatibility().getDescription());
      }

      private static FormattedCharSequence cacheName(Minecraft param0, Component param1) {
         int var2 = param0.font.width(param1);
         if (var2 > 157) {
            FormattedText var3 = FormattedText.composite(param0.font.substrByWidth(param1, 157 - param0.font.width("...")), FormattedText.of("..."));
            return Language.getInstance().getVisualOrder(var3);
         } else {
            return param1.getVisualOrderText();
         }
      }

      private static MultiLineLabel cacheDescription(Minecraft param0, Component param1) {
         return MultiLineLabel.create(param0.font, param1, 157, 2);
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.pack.getTitle());
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         PackCompatibility var11 = this.pack.getCompatibility();
         if (!var11.isCompatible()) {
            GuiComponent.fill(param0, param3 - 1, param2 - 1, param3 + param4 - 9, param2 + param5 + 1, -8978432);
         }

         RenderSystem.setShaderTexture(0, this.pack.getIconTexture());
         GuiComponent.blit(param0, param3, param2, 0.0F, 0.0F, 32, 32, 32, 32);
         FormattedCharSequence var12 = this.nameDisplayCache;
         MultiLineLabel var13 = this.descriptionDisplayCache;
         if (this.showHoverOverlay() && (this.minecraft.options.touchscreen().get() || param8 || this.parent.getSelected() == this && this.parent.isFocused())) {
            RenderSystem.setShaderTexture(0, TransferableSelectionList.ICON_OVERLAY_LOCATION);
            GuiComponent.fill(param0, param3, param2, param3 + 32, param2 + 32, -1601138544);
            int var14 = param6 - param3;
            int var15 = param7 - param2;
            if (!this.pack.getCompatibility().isCompatible()) {
               var12 = this.incompatibleNameDisplayCache;
               var13 = this.incompatibleDescriptionDisplayCache;
            }

            if (this.pack.canSelect()) {
               if (var14 < 32) {
                  GuiComponent.blit(param0, param3, param2, 0.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  GuiComponent.blit(param0, param3, param2, 0.0F, 0.0F, 32, 32, 256, 256);
               }
            } else {
               if (this.pack.canUnselect()) {
                  if (var14 < 16) {
                     GuiComponent.blit(param0, param3, param2, 32.0F, 32.0F, 32, 32, 256, 256);
                  } else {
                     GuiComponent.blit(param0, param3, param2, 32.0F, 0.0F, 32, 32, 256, 256);
                  }
               }

               if (this.pack.canMoveUp()) {
                  if (var14 < 32 && var14 > 16 && var15 < 16) {
                     GuiComponent.blit(param0, param3, param2, 96.0F, 32.0F, 32, 32, 256, 256);
                  } else {
                     GuiComponent.blit(param0, param3, param2, 96.0F, 0.0F, 32, 32, 256, 256);
                  }
               }

               if (this.pack.canMoveDown()) {
                  if (var14 < 32 && var14 > 16 && var15 > 16) {
                     GuiComponent.blit(param0, param3, param2, 64.0F, 32.0F, 32, 32, 256, 256);
                  } else {
                     GuiComponent.blit(param0, param3, param2, 64.0F, 0.0F, 32, 32, 256, 256);
                  }
               }
            }
         }

         this.minecraft.font.drawShadow(param0, var12, param3 + 32 + 2, param2 + 1, 16777215);
         var13.renderLeftAligned(param0, param3 + 32 + 2, param2 + 12, 10, 8421504);
      }

      public String getPackId() {
         return this.pack.getId();
      }

      private boolean showHoverOverlay() {
         return !this.pack.isFixedPosition() || !this.pack.isRequired();
      }

      public void keyboardSelection() {
         if (this.pack.canSelect() && this.handlePackSelection()) {
            this.parent.screen.updateFocus(this.parent);
         } else if (this.pack.canUnselect()) {
            this.pack.unselect();
            this.parent.screen.updateFocus(this.parent);
         }
      }

      void keyboardMoveUp() {
         if (this.pack.canMoveUp()) {
            this.pack.moveUp();
         }
      }

      void keyboardMoveDown() {
         if (this.pack.canMoveDown()) {
            this.pack.moveDown();
         }
      }

      private boolean handlePackSelection() {
         if (this.pack.getCompatibility().isCompatible()) {
            this.pack.select();
            return true;
         } else {
            Component var1 = this.pack.getCompatibility().getConfirmation();
            this.minecraft.setScreen(new ConfirmScreen(l_param0 -> {
               this.minecraft.setScreen(this.parent.screen);
               if (l_param0) {
                  this.pack.select();
               }
            }, TransferableSelectionList.INCOMPATIBLE_CONFIRM_TITLE, var1));
            return false;
         }
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         if (param4 != 0) {
            return false;
         }

         double var6 = param0 - this.parent.getRowLeft();
         double var8 = param2 - this.parent.getRowTop(this.parent.children().indexOf(this));
         if (this.showHoverOverlay() && var6 <= 32.0) {
            this.parent.screen.clearSelected();
            if (this.pack.canSelect()) {
               this.handlePackSelection();
               return true;
            }

            if (var6 < 16.0 && this.pack.canUnselect()) {
               this.pack.unselect();
               return true;
            }

            if (var6 > 16.0 && var8 < 16.0 && this.pack.canMoveUp()) {
               this.pack.moveUp();
               return true;
            }

            if (var6 > 16.0 && var8 > 16.0 && this.pack.canMoveDown()) {
               this.pack.moveDown();
               return true;
            }
         }

         return false;
      }
   }
}
