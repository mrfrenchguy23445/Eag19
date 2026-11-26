package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantScreen extends AbstractContainerScreen<MerchantMenu> {
   private static final ResourceLocation VILLAGER_LOCATION = new ResourceLocation("textures/gui/container/villager2.png");
   private static final int TEXTURE_WIDTH = 512;
   private static final int TEXTURE_HEIGHT = 256;
   private static final int MERCHANT_MENU_PART_X = 99;
   private static final int PROGRESS_BAR_X = 136;
   private static final int PROGRESS_BAR_Y = 16;
   private static final int SELL_ITEM_1_X = 5;
   private static final int SELL_ITEM_2_X = 35;
   private static final int BUY_ITEM_X = 68;
   private static final int LABEL_Y = 6;
   private static final int NUMBER_OF_OFFER_BUTTONS = 7;
   private static final int TRADE_BUTTON_X = 5;
   private static final int TRADE_BUTTON_HEIGHT = 20;
   private static final int TRADE_BUTTON_WIDTH = 88;
   private static final int SCROLLER_HEIGHT = 27;
   private static final int SCROLLER_WIDTH = 6;
   private static final int SCROLL_BAR_HEIGHT = 139;
   private static final int SCROLL_BAR_TOP_POS_Y = 18;
   private static final int SCROLL_BAR_START_X = 94;
   private static final Component TRADES_LABEL = Component.translatable("merchant.trades");
   private static final Component LEVEL_SEPARATOR = Component.literal(" - ");
   private static final Component DEPRECATED_TOOLTIP = Component.translatable("merchant.deprecated");
   private int shopItem;
   private final MerchantScreen.TradeOfferButton[] tradeOfferButtons = new MerchantScreen.TradeOfferButton[7];
   int scrollOff;
   private boolean isDragging;

   public MerchantScreen(MerchantMenu param0, Inventory param1, Component param2) {
      super(param0, param1, param2);
      this.imageWidth = 276;
      this.inventoryLabelX = 107;
   }

   private void postButtonClick() {
      this.menu.setSelectionHint(this.shopItem);
      this.menu.tryMoveItems(this.shopItem);
      this.minecraft.getConnection().send(new ServerboundSelectTradePacket(this.shopItem));
   }

   @Override
   protected void init() {
      super.init();
      int var1 = (this.width - this.imageWidth) / 2;
      int var2 = (this.height - this.imageHeight) / 2;
      int var3 = var2 + 16 + 2;

      for (int var4 = 0; var4 < 7; var4++) {
         this.tradeOfferButtons[var4] = this.addRenderableWidget(new MerchantScreen.TradeOfferButton(var1 + 5, var3, var4, l_param0 -> {
            if (l_param0 instanceof MerchantScreen.TradeOfferButton) {
               this.shopItem = ((MerchantScreen.TradeOfferButton)l_param0).getIndex() + this.scrollOff;
               this.postButtonClick();
            }
         }));
         var3 += 20;
      }
   }

   @Override
   protected void renderLabels(PoseStack param0, int param1, int param2) {
      int var4 = this.menu.getTraderLevel();
      if (var4 > 0 && var4 <= 5 && this.menu.showProgressBar()) {
         Component var5 = this.title.copy().append(LEVEL_SEPARATOR).append(Component.translatable("merchant.level." + var4));
         int var6 = this.font.width(var5);
         int var7 = 49 + this.imageWidth / 2 - var6 / 2;
         this.font.draw(param0, var5, var7, 6.0F, 4210752);
      } else {
         this.font.draw(param0, this.title, 49 + this.imageWidth / 2 - this.font.width(this.title) / 2, 6.0F, 4210752);
      }

      this.font.draw(param0, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752);
      int var5 = this.font.width(TRADES_LABEL);
      this.font.draw(param0, TRADES_LABEL, 5 - var5 / 2 + 48, 6.0F, 4210752);
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      RenderSystem.setShaderTexture(0, VILLAGER_LOCATION);
      int var5 = (this.width - this.imageWidth) / 2;
      int var6 = (this.height - this.imageHeight) / 2;
      blit(param0, var5, var6, 0, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 256);
      MerchantOffers var7 = this.menu.getOffers();
      if (!var7.isEmpty()) {
         int var8 = this.shopItem;
         if (var8 < 0 || var8 >= var7.size()) {
            return;
         }

         MerchantOffer var9 = var7.get(var8);
         if (var9.isOutOfStock()) {
            RenderSystem.setShaderTexture(0, VILLAGER_LOCATION);
            blit(param0, this.leftPos + 83 + 99, this.topPos + 35, 0, 311.0F, 0.0F, 28, 21, 512, 256);
         }
      }
   }

   private void renderProgressBar(PoseStack param0, int param1, int param2, MerchantOffer param3) {
      RenderSystem.setShaderTexture(0, VILLAGER_LOCATION);
      int var5 = this.menu.getTraderLevel();
      int var6 = this.menu.getTraderXp();
      if (var5 < 5) {
         blit(param0, param1 + 136, param2 + 16, 0, 0.0F, 186.0F, 102, 5, 512, 256);
         int var7 = VillagerData.getMinXpPerLevel(var5);
         if (var6 >= var7 && VillagerData.canLevelUp(var5)) {
            int var8 = 100;
            float var9 = 100.0F / (VillagerData.getMaxXpPerLevel(var5) - var7);
            int var10 = Math.min(Mth.floor(var9 * (var6 - var7)), 100);
            blit(param0, param1 + 136, param2 + 16, 0, 0.0F, 191.0F, var10 + 1, 5, 512, 256);
            int var11 = this.menu.getFutureTraderXp();
            if (var11 > 0) {
               int var12 = Math.min(Mth.floor(var11 * var9), 100 - var10);
               blit(param0, param1 + 136 + var10 + 1, param2 + 16 + 1, 0, 2.0F, 182.0F, var12, 3, 512, 256);
            }
         }
      }
   }

   private void renderScroller(PoseStack param0, int param1, int param2, MerchantOffers param3) {
      int var5 = param3.size() + 1 - 7;
      if (var5 > 1) {
         int var6 = 139 - (27 + (var5 - 1) * 139 / var5);
         int var7 = 1 + var6 / var5 + 139 / var5;
         int var8 = 113;
         int var9 = Math.min(113, this.scrollOff * var7);
         if (this.scrollOff == var5 - 1) {
            var9 = 113;
         }

         blit(param0, param1 + 94, param2 + 18 + var9, 0, 0.0F, 199.0F, 6, 27, 512, 256);
      } else {
         blit(param0, param1 + 94, param2 + 18, 0, 6.0F, 199.0F, 6, 27, 512, 256);
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      super.render(param0, param1, param2, param3);
      MerchantOffers var5 = this.menu.getOffers();
      if (!var5.isEmpty()) {
         int var6 = (this.width - this.imageWidth) / 2;
         int var7 = (this.height - this.imageHeight) / 2;
         int var8 = var7 + 16 + 1;
         int var9 = var6 + 5 + 5;
         RenderSystem.setShaderTexture(0, VILLAGER_LOCATION);
         this.renderScroller(param0, var6, var7, var5);
         int var10 = 0;

         for (MerchantOffer var12 : var5) {
            if (!this.canScroll(var5.size()) || var10 >= this.scrollOff && var10 < 7 + this.scrollOff) {
               ItemStack var13 = var12.getBaseCostA();
               ItemStack var14 = var12.getCostA();
               ItemStack var15 = var12.getCostB();
               ItemStack var16 = var12.getResult();
               param0.pushPose();
               param0.translate(0.0F, 0.0F, 100.0F);
               int var17 = var8 + 2;
               this.renderAndDecorateCostA(param0, var14, var13, var9, var17);
               if (!var15.isEmpty()) {
                  this.itemRenderer.renderAndDecorateFakeItem(param0, var15, var6 + 5 + 35, var17);
                  this.itemRenderer.renderGuiItemDecorations(param0, this.font, var15, var6 + 5 + 35, var17);
               }

               this.renderButtonArrows(param0, var12, var6, var17);
               this.itemRenderer.renderAndDecorateFakeItem(param0, var16, var6 + 5 + 68, var17);
               this.itemRenderer.renderGuiItemDecorations(param0, this.font, var16, var6 + 5 + 68, var17);
               param0.popPose();
               var8 += 20;
               var10++;
            } else {
               var10++;
            }
         }

         int var11 = this.shopItem;
         MerchantOffer var12 = var5.get(var11);
         if (this.menu.showProgressBar()) {
            this.renderProgressBar(param0, var6, var7, var12);
         }

         if (var12.isOutOfStock() && this.isHovering(186, 35, 22, 21, param1, param2) && this.menu.canRestock()) {
            this.renderTooltip(param0, DEPRECATED_TOOLTIP, param1, param2);
         }

         for (MerchantScreen.TradeOfferButton var16 : this.tradeOfferButtons) {
            if (var16.isHoveredOrFocused()) {
               var16.renderToolTip(param0, param1, param2);
            }

            var16.visible = var16.index < this.menu.getOffers().size();
         }

         RenderSystem.enableDepthTest();
      }

      this.renderTooltip(param0, param1, param2);
   }

   private void renderButtonArrows(PoseStack param0, MerchantOffer param1, int param2, int param3) {
      RenderSystem.enableBlend();
      RenderSystem.setShaderTexture(0, VILLAGER_LOCATION);
      if (param1.isOutOfStock()) {
         blit(param0, param2 + 5 + 35 + 20, param3 + 3, 0, 25.0F, 171.0F, 10, 9, 512, 256);
      } else {
         blit(param0, param2 + 5 + 35 + 20, param3 + 3, 0, 15.0F, 171.0F, 10, 9, 512, 256);
      }
   }

   private void renderAndDecorateCostA(PoseStack param0, ItemStack param1, ItemStack param2, int param3, int param4) {
      this.itemRenderer.renderAndDecorateFakeItem(param0, param1, param3, param4);
      if (param2.getCount() == param1.getCount()) {
         this.itemRenderer.renderGuiItemDecorations(param0, this.font, param1, param3, param4);
      } else {
         this.itemRenderer.renderGuiItemDecorations(param0, this.font, param2, param3, param4, param2.getCount() == 1 ? "1" : null);
         this.itemRenderer.renderGuiItemDecorations(param0, this.font, param1, param3 + 14, param4, param1.getCount() == 1 ? "1" : null);
         RenderSystem.setShaderTexture(0, VILLAGER_LOCATION);
         param0.pushPose();
         param0.translate(0.0F, 0.0F, 300.0F);
         blit(param0, param3 + 7, param4 + 12, 0, 0.0F, 176.0F, 9, 2, 512, 256);
         param0.popPose();
      }
   }

   private boolean canScroll(int param0) {
      return param0 > 7;
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      int var7 = this.menu.getOffers().size();
      if (this.canScroll(var7)) {
         int var8 = var7 - 7;
         this.scrollOff = Mth.clamp((int)(this.scrollOff - param4), 0, var8);
      }

      return true;
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      int var10 = this.menu.getOffers().size();
      if (this.isDragging) {
         int var11 = this.topPos + 18;
         int var12 = var11 + 139;
         int var13 = var10 - 7;
         float var14 = ((float)param2 - var11 - 13.5F) / (var12 - var11 - 27.0F);
         var14 = var14 * var13 + 0.5F;
         this.scrollOff = Mth.clamp((int)var14, 0, var13);
         return true;
      } else {
         return super.mouseDragged(param0, param2, param4, param5, param7);
      }
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      this.isDragging = false;
      int var6 = (this.width - this.imageWidth) / 2;
      int var7 = (this.height - this.imageHeight) / 2;
      if (this.canScroll(this.menu.getOffers().size()) && param0 > var6 + 94 && param0 < var6 + 94 + 6 && param2 > var7 + 18 && param2 <= var7 + 18 + 139 + 1) {
         this.isDragging = true;
      }

      return super.mouseClicked(param0, param2, param4);
   }

   class TradeOfferButton extends Button {
      final int index;

      public TradeOfferButton(int param1, int param2, int param3, Button.OnPress param4) {
         super(param1, param2, 88, 20, CommonComponents.EMPTY, param4, DEFAULT_NARRATION);
         this.index = param3;
         this.visible = false;
      }

      public int getIndex() {
         return this.index;
      }

      public void renderToolTip(PoseStack param0, int param1, int param2) {
         if (this.isHovered && MerchantScreen.this.menu.getOffers().size() > this.index + MerchantScreen.this.scrollOff) {
            if (param1 < this.getX() + 20) {
               ItemStack var4 = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostA();
               MerchantScreen.this.renderTooltip(param0, var4, param1, param2);
            } else if (param1 < this.getX() + 50 && param1 > this.getX() + 30) {
               ItemStack var4 = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostB();
               if (!var4.isEmpty()) {
                  MerchantScreen.this.renderTooltip(param0, var4, param1, param2);
               }
            } else if (param1 > this.getX() + 65) {
               ItemStack var4 = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getResult();
               MerchantScreen.this.renderTooltip(param0, var4, param1, param2);
            }
         }
      }
   }
}
