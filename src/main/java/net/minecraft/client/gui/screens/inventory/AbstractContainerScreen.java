package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
   public static final ResourceLocation INVENTORY_LOCATION = new ResourceLocation("textures/gui/container/inventory.png");
   private static final float SNAPBACK_SPEED = 100.0F;
   private static final int QUICKDROP_DELAY = 500;
   public static final int SLOT_ITEM_BLIT_OFFSET = 100;
   private static final int HOVER_ITEM_BLIT_OFFSET = 200;
   protected int imageWidth = 176;
   protected int imageHeight = 166;
   protected int titleLabelX;
   protected int titleLabelY;
   protected int inventoryLabelX;
   protected int inventoryLabelY;
   protected final T menu;
   protected final Component playerInventoryTitle;
   @Nullable
   protected Slot hoveredSlot;
   @Nullable
   private Slot clickedSlot;
   @Nullable
   private Slot snapbackEnd;
   @Nullable
   private Slot quickdropSlot;
   @Nullable
   private Slot lastClickSlot;
   protected int leftPos;
   protected int topPos;
   private boolean isSplittingStack;
   private ItemStack draggingItem = ItemStack.EMPTY;
   private int snapbackStartX;
   private int snapbackStartY;
   private long snapbackTime;
   private ItemStack snapbackItem = ItemStack.EMPTY;
   private long quickdropTime;
   protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
   protected boolean isQuickCrafting;
   private int quickCraftingType;
   private int quickCraftingButton;
   private boolean skipNextRelease;
   private int quickCraftingRemainder;
   private long lastClickTime;
   private int lastClickButton;
   private boolean doubleclick;
   private ItemStack lastQuickMoved = ItemStack.EMPTY;

   public AbstractContainerScreen(T param0, Inventory param1, Component param2) {
      super(param2);
      this.menu = param0;
      this.playerInventoryTitle = param1.getDisplayName();
      this.skipNextRelease = true;
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelX = 8;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   @Override
   protected void init() {
      this.leftPos = (this.width - this.imageWidth) / 2;
      this.topPos = (this.height - this.imageHeight) / 2;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      int var5 = this.leftPos;
      int var6 = this.topPos;
      this.renderBg(param0, param3, param1, param2);
      RenderSystem.disableDepthTest();
      super.render(param0, param1, param2, param3);
      param0.pushPose();
      param0.translate(var5, var6, 0.0F);
      this.hoveredSlot = null;

      for (int var7 = 0; var7 < this.menu.slots.size(); var7++) {
         Slot var8 = this.menu.slots.get(var7);
         if (var8.isActive()) {
            this.renderSlot(param0, var8);
         }

         if (this.isHovering(var8, param1, param2) && var8.isActive()) {
            this.hoveredSlot = var8;
            int var9 = var8.x;
            int var10 = var8.y;
            renderSlotHighlight(param0, var9, var10, 0);
         }
      }

      this.renderLabels(param0, param1, param2);
      ItemStack var7 = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
      if (!var7.isEmpty()) {
         int var8 = 8;
         int var9 = this.draggingItem.isEmpty() ? 8 : 16;
         String var10 = null;
         if (!this.draggingItem.isEmpty() && this.isSplittingStack) {
            var7 = var7.copy();
            var7.setCount(Mth.ceil(var7.getCount() / 2.0F));
         } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
            var7 = var7.copy();
            var7.setCount(this.quickCraftingRemainder);
            if (var7.isEmpty()) {
               var10 = ChatFormatting.YELLOW + "0";
            }
         }

         this.renderFloatingItem(param0, var7, param1 - var5 - 8, param2 - var6 - var9, var10);
      }

      if (!this.snapbackItem.isEmpty()) {
         float var8 = (float)(Util.getMillis() - this.snapbackTime) / 100.0F;
         if (var8 >= 1.0F) {
            var8 = 1.0F;
            this.snapbackItem = ItemStack.EMPTY;
         }

         int var9 = this.snapbackEnd.x - this.snapbackStartX;
         int var10 = this.snapbackEnd.y - this.snapbackStartY;
         int var11 = this.snapbackStartX + (int)(var9 * var8);
         int var12 = this.snapbackStartY + (int)(var10 * var8);
         this.renderFloatingItem(param0, this.snapbackItem, var11, var12, null);
      }

      param0.popPose();
      RenderSystem.enableDepthTest();
   }

   public static void renderSlotHighlight(PoseStack param0, int param1, int param2, int param3) {
      RenderSystem.disableDepthTest();
      RenderSystem.colorMask(true, true, true, false);
      fillGradient(param0, param1, param2, param1 + 16, param2 + 16, -2130706433, -2130706433, param3);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableDepthTest();
   }

   protected void renderTooltip(PoseStack param0, int param1, int param2) {
      if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
         this.renderTooltip(param0, this.hoveredSlot.getItem(), param1, param2);
      }
   }

   private void renderFloatingItem(PoseStack param0, ItemStack param1, int param2, int param3, String param4) {
      param0.pushPose();
      param0.translate(0.0F, 0.0F, 232.0F);
      this.itemRenderer.renderAndDecorateItem(param0, param1, param2, param3);
      this.itemRenderer.renderGuiItemDecorations(param0, this.font, param1, param2, param3 - (this.draggingItem.isEmpty() ? 0 : 8), param4);
      param0.popPose();
   }

   protected void renderLabels(PoseStack param0, int param1, int param2) {
      this.font.draw(param0, this.title, this.titleLabelX, this.titleLabelY, 4210752);
      this.font.draw(param0, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752);
   }

   protected abstract void renderBg(PoseStack var1, float var2, int var3, int var4);

   private void renderSlot(PoseStack param0, Slot param1) {
      int var3 = param1.x;
      int var4 = param1.y;
      ItemStack var5 = param1.getItem();
      boolean var6 = false;
      boolean var7 = param1 == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
      ItemStack var8 = this.menu.getCarried();
      String var9 = null;
      if (param1 == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !var5.isEmpty()) {
         var5 = var5.copy();
         var5.setCount(var5.getCount() / 2);
      } else if (this.isQuickCrafting && this.quickCraftSlots.contains(param1) && !var8.isEmpty()) {
         if (this.quickCraftSlots.size() == 1) {
            return;
         }

         if (AbstractContainerMenu.canItemQuickReplace(param1, var8, true) && this.menu.canDragTo(param1)) {
            var5 = var8.copy();
            var6 = true;
            AbstractContainerMenu.getQuickCraftSlotCount(
               this.quickCraftSlots, this.quickCraftingType, var5, param1.getItem().isEmpty() ? 0 : param1.getItem().getCount()
            );
            int var10 = Math.min(var5.getMaxStackSize(), param1.getMaxStackSize(var5));
            if (var5.getCount() > var10) {
               var9 = ChatFormatting.YELLOW.toString() + var10;
               var5.setCount(var10);
            }
         } else {
            this.quickCraftSlots.remove(param1);
            this.recalculateQuickCraftRemaining();
         }
      }

      param0.pushPose();
      param0.translate(0.0F, 0.0F, 100.0F);
      if (var5.isEmpty() && param1.isActive()) {
         Pair<ResourceLocation, ResourceLocation> var10 = param1.getNoItemIcon();
         if (var10 != null) {
            TextureAtlasSprite var11 = this.minecraft.getTextureAtlas(var10.getFirst()).apply(var10.getSecond());
            RenderSystem.setShaderTexture(0, var11.atlasLocation());
            blit(param0, var3, var4, 0, 16, 16, var11);
            var7 = true;
         }
      }

      if (!var7) {
         if (var6) {
            fill(param0, var3, var4, var3 + 16, var4 + 16, -2130706433);
         }

         this.itemRenderer.renderAndDecorateItem(param0, this.minecraft.player, var5, var3, var4, param1.x + param1.y * this.imageWidth);
         this.itemRenderer.renderGuiItemDecorations(param0, this.font, var5, var3, var4, var9);
      }

      param0.popPose();
   }

   private void recalculateQuickCraftRemaining() {
      ItemStack var1 = this.menu.getCarried();
      if (!var1.isEmpty() && this.isQuickCrafting) {
         if (this.quickCraftingType == 2) {
            this.quickCraftingRemainder = var1.getMaxStackSize();
         } else {
            this.quickCraftingRemainder = var1.getCount();

            for (Slot var3 : this.quickCraftSlots) {
               ItemStack var4 = var1.copy();
               ItemStack var5 = var3.getItem();
               int var6 = var5.isEmpty() ? 0 : var5.getCount();
               AbstractContainerMenu.getQuickCraftSlotCount(this.quickCraftSlots, this.quickCraftingType, var4, var6);
               int var7 = Math.min(var4.getMaxStackSize(), var3.getMaxStackSize(var4));
               if (var4.getCount() > var7) {
                  var4.setCount(var7);
               }

               this.quickCraftingRemainder = this.quickCraftingRemainder - (var4.getCount() - var6);
            }
         }
      }
   }

   @Nullable
   private Slot findSlot(double param0, double param2) {
      for (int var5 = 0; var5 < this.menu.slots.size(); var5++) {
         Slot var6 = this.menu.slots.get(var5);
         if (this.isHovering(var6, param0, param2) && var6.isActive()) {
            return var6;
         }
      }

      return null;
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (super.mouseClicked(param0, param2, param4)) {
         return true;
      }

      boolean var6 = this.minecraft.options.keyPickItem.matchesMouse(param4) && this.minecraft.gameMode.hasInfiniteItems();
      Slot var7 = this.findSlot(param0, param2);
      long var8 = Util.getMillis();
      this.doubleclick = this.lastClickSlot == var7 && var8 - this.lastClickTime < 250L && this.lastClickButton == param4;
      this.skipNextRelease = false;
      if (param4 != 0 && param4 != 1 && !var6) {
         this.checkHotbarMouseClicked(param4);
      } else {
         int var10 = this.leftPos;
         int var11 = this.topPos;
         boolean var12 = this.hasClickedOutside(param0, param2, var10, var11, param4);
         int var13 = -1;
         if (var7 != null) {
            var13 = var7.index;
         }

         if (var12) {
            var13 = -999;
         }

         if (this.minecraft.options.touchscreen().get() && var12 && this.menu.getCarried().isEmpty()) {
            this.onClose();
            return true;
         }

         if (var13 != -1) {
            if (this.minecraft.options.touchscreen().get()) {
               if (var7 != null && var7.hasItem()) {
                  this.clickedSlot = var7;
                  this.draggingItem = ItemStack.EMPTY;
                  this.isSplittingStack = param4 == 1;
               } else {
                  this.clickedSlot = null;
               }
            } else if (!this.isQuickCrafting) {
               if (this.menu.getCarried().isEmpty()) {
                  if (var6) {
                     this.slotClicked(var7, var13, param4, ClickType.CLONE);
                  } else {
                     boolean var14 = var13 != -999
                        && (
                           InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
                              || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344)
                        );
                     ClickType var15 = ClickType.PICKUP;
                     if (var14) {
                        this.lastQuickMoved = var7 != null && var7.hasItem() ? var7.getItem().copy() : ItemStack.EMPTY;
                        var15 = ClickType.QUICK_MOVE;
                     } else if (var13 == -999) {
                        var15 = ClickType.THROW;
                     }

                     this.slotClicked(var7, var13, param4, var15);
                  }

                  this.skipNextRelease = true;
               } else {
                  this.isQuickCrafting = true;
                  this.quickCraftingButton = param4;
                  this.quickCraftSlots.clear();
                  if (param4 == 0) {
                     this.quickCraftingType = 0;
                  } else if (param4 == 1) {
                     this.quickCraftingType = 1;
                  } else if (var6) {
                     this.quickCraftingType = 2;
                  }
               }
            }
         }
      }

      this.lastClickSlot = var7;
      this.lastClickTime = var8;
      this.lastClickButton = param4;
      return true;
   }

   private void checkHotbarMouseClicked(int param0) {
      if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
         if (this.minecraft.options.keySwapOffhand.matchesMouse(param0)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
            return;
         }

         for (int var2 = 0; var2 < 9; var2++) {
            if (this.minecraft.options.keyHotbarSlots[var2].matchesMouse(param0)) {
               this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, var2, ClickType.SWAP);
            }
         }
      }
   }

   protected boolean hasClickedOutside(double param0, double param2, int param4, int param5, int param6) {
      return param0 < param4 || param2 < param5 || param0 >= param4 + this.imageWidth || param2 >= param5 + this.imageHeight;
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      Slot var10 = this.findSlot(param0, param2);
      ItemStack var11 = this.menu.getCarried();
      if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
         if (param4 == 0 || param4 == 1) {
            if (this.draggingItem.isEmpty()) {
               if (var10 != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                  this.draggingItem = this.clickedSlot.getItem().copy();
               }
            } else if (this.draggingItem.getCount() > 1 && var10 != null && AbstractContainerMenu.canItemQuickReplace(var10, this.draggingItem, false)) {
               long var12 = Util.getMillis();
               if (this.quickdropSlot == var10) {
                  if (var12 - this.quickdropTime > 500L) {
                     this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                     this.slotClicked(var10, var10.index, 1, ClickType.PICKUP);
                     this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                     this.quickdropTime = var12 + 750L;
                     this.draggingItem.shrink(1);
                  }
               } else {
                  this.quickdropSlot = var10;
                  this.quickdropTime = var12;
               }
            }
         }
      } else if (this.isQuickCrafting
         && var10 != null
         && !var11.isEmpty()
         && (var11.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
         && AbstractContainerMenu.canItemQuickReplace(var10, var11, true)
         && var10.mayPlace(var11)
         && this.menu.canDragTo(var10)) {
         this.quickCraftSlots.add(var10);
         this.recalculateQuickCraftRemaining();
      }

      return true;
   }

   @Override
   public boolean mouseReleased(double param0, double param2, int param4) {
      Slot var6 = this.findSlot(param0, param2);
      int var7 = this.leftPos;
      int var8 = this.topPos;
      boolean var9 = this.hasClickedOutside(param0, param2, var7, var8, param4);
      int var10 = -1;
      if (var6 != null) {
         var10 = var6.index;
      }

      if (var9) {
         var10 = -999;
      }

      if (this.doubleclick && var6 != null && param4 == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, var6)) {
         if (hasShiftDown()) {
            if (!this.lastQuickMoved.isEmpty()) {
               for (Slot var12 : this.menu.slots) {
                  if (var12 != null
                     && var12.mayPickup(this.minecraft.player)
                     && var12.hasItem()
                     && var12.container == var6.container
                     && AbstractContainerMenu.canItemQuickReplace(var12, this.lastQuickMoved, true)) {
                     this.slotClicked(var12, var12.index, param4, ClickType.QUICK_MOVE);
                  }
               }
            }
         } else {
            this.slotClicked(var6, var10, param4, ClickType.PICKUP_ALL);
         }

         this.doubleclick = false;
         this.lastClickTime = 0L;
      } else {
         if (this.isQuickCrafting && this.quickCraftingButton != param4) {
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            this.skipNextRelease = true;
            return true;
         }

         if (this.skipNextRelease) {
            this.skipNextRelease = false;
            return true;
         }

         if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
            if (param4 == 0 || param4 == 1) {
               if (this.draggingItem.isEmpty() && var6 != this.clickedSlot) {
                  this.draggingItem = this.clickedSlot.getItem();
               }

               boolean var11 = AbstractContainerMenu.canItemQuickReplace(var6, this.draggingItem, false);
               if (var10 != -1 && !this.draggingItem.isEmpty() && var11) {
                  this.slotClicked(this.clickedSlot, this.clickedSlot.index, param4, ClickType.PICKUP);
                  this.slotClicked(var6, var10, 0, ClickType.PICKUP);
                  if (this.menu.getCarried().isEmpty()) {
                     this.snapbackItem = ItemStack.EMPTY;
                  } else {
                     this.slotClicked(this.clickedSlot, this.clickedSlot.index, param4, ClickType.PICKUP);
                     this.snapbackStartX = Mth.floor(param0 - var7);
                     this.snapbackStartY = Mth.floor(param2 - var8);
                     this.snapbackEnd = this.clickedSlot;
                     this.snapbackItem = this.draggingItem;
                     this.snapbackTime = Util.getMillis();
                  }
               } else if (!this.draggingItem.isEmpty()) {
                  this.snapbackStartX = Mth.floor(param0 - var7);
                  this.snapbackStartY = Mth.floor(param2 - var8);
                  this.snapbackEnd = this.clickedSlot;
                  this.snapbackItem = this.draggingItem;
                  this.snapbackTime = Util.getMillis();
               }

               this.clearDraggingState();
            }
         } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
            this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ClickType.QUICK_CRAFT);

            for (Slot var12 : this.quickCraftSlots) {
               this.slotClicked(var12, var12.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ClickType.QUICK_CRAFT);
            }

            this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ClickType.QUICK_CRAFT);
         } else if (!this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keyPickItem.matchesMouse(param4)) {
               this.slotClicked(var6, var10, param4, ClickType.CLONE);
            } else {
               boolean var11 = var10 != -999
                  && (
                     InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
                        || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344)
                  );
               if (var11) {
                  this.lastQuickMoved = var6 != null && var6.hasItem() ? var6.getItem().copy() : ItemStack.EMPTY;
               }

               this.slotClicked(var6, var10, param4, var11 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }
         }
      }

      if (this.menu.getCarried().isEmpty()) {
         this.lastClickTime = 0L;
      }

      this.isQuickCrafting = false;
      return true;
   }

   public void clearDraggingState() {
      this.draggingItem = ItemStack.EMPTY;
      this.clickedSlot = null;
   }

   private boolean isHovering(Slot param0, double param1, double param3) {
      return this.isHovering(param0.x, param0.y, 16, 16, param1, param3);
   }

   protected boolean isHovering(int param0, int param1, int param2, int param3, double param4, double param6) {
      int var9 = this.leftPos;
      int var10 = this.topPos;
      param4 -= var9;
      param6 -= var10;
      return param4 >= param0 - 1 && param4 < param0 + param2 + 1 && param6 >= param1 - 1 && param6 < param1 + param3 + 1;
   }

   protected void slotClicked(Slot param0, int param1, int param2, ClickType param3) {
      if (param0 != null) {
         param1 = param0.index;
      }

      this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, param1, param2, param3, this.minecraft.player);
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (this.minecraft.options.keyInventory.matches(param0, param1)) {
         this.onClose();
         return true;
      }

      this.checkHotbarKeyPressed(param0, param1);
      if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
         if (this.minecraft.options.keyPickItem.matches(param0, param1)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ClickType.CLONE);
         } else if (this.minecraft.options.keyDrop.matches(param0, param1)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, hasControlDown() ? 1 : 0, ClickType.THROW);
         }
      }

      return true;
   }

   protected boolean checkHotbarKeyPressed(int param0, int param1) {
      if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
         if (this.minecraft.options.keySwapOffhand.matches(param0, param1)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
            return true;
         }

         for (int var3 = 0; var3 < 9; var3++) {
            if (this.minecraft.options.keyHotbarSlots[var3].matches(param0, param1)) {
               this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, var3, ClickType.SWAP);
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void removed() {
      if (this.minecraft.player != null) {
         this.menu.removed(this.minecraft.player);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public final void tick() {
      super.tick();
      if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
         this.containerTick();
      } else {
         this.minecraft.player.closeContainer();
      }
   }

   protected void containerTick() {
   }

   @Override
   public T getMenu() {
      return this.menu;
   }

   @Override
   public void onClose() {
      this.minecraft.player.closeContainer();
      super.onClose();
   }
}
