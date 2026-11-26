package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public class CreativeModeInventoryScreen extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
   private static final ResourceLocation CREATIVE_TABS_LOCATION = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
   private static final String GUI_CREATIVE_TAB_PREFIX = "textures/gui/container/creative_inventory/tab_";
   private static final String CUSTOM_SLOT_LOCK = "CustomCreativeLock";
   private static final int NUM_ROWS = 5;
   private static final int NUM_COLS = 9;
   private static final int TAB_WIDTH = 26;
   private static final int TAB_HEIGHT = 32;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   static final SimpleContainer CONTAINER = new SimpleContainer(45);
   private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");
   private static final int TEXT_COLOR = 16777215;
   private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
   private float scrollOffs;
   private boolean scrolling;
   private EditBox searchBox;
   @Nullable
   private List<Slot> originalSlots;
   @Nullable
   private Slot destroyItemSlot;
   private CreativeInventoryListener listener;
   private boolean ignoreTextInput;
   private boolean hasClickedOutside;
   private final Set<TagKey<Item>> visibleTags = new HashSet<>();
   private final boolean displayOperatorCreativeTab;

   public CreativeModeInventoryScreen(Player param0, FeatureFlagSet param1, boolean param2) {
      super(new CreativeModeInventoryScreen.ItemPickerMenu(param0), param0.getInventory(), CommonComponents.EMPTY);
      param0.containerMenu = this.menu;
      this.passEvents = true;
      this.imageHeight = 136;
      this.imageWidth = 195;
      this.displayOperatorCreativeTab = param2;
      CreativeModeTabs.tryRebuildTabContents(param1, this.hasPermissions(param0), param0.level.registryAccess());
   }

   private boolean hasPermissions(Player param0) {
      return param0.canUseGameMasterBlocks() && this.displayOperatorCreativeTab;
   }

   private void tryRefreshInvalidatedTabs(FeatureFlagSet param0, boolean param1, HolderLookup.Provider param2) {
      if (CreativeModeTabs.tryRebuildTabContents(param0, param1, param2)) {
         for (CreativeModeTab var5 : CreativeModeTabs.allTabs()) {
            Collection<ItemStack> var6 = var5.getDisplayItems();
            if (var5 == selectedTab) {
               if (var5.getType() == CreativeModeTab.Type.CATEGORY && var6.isEmpty()) {
                  this.selectTab(CreativeModeTabs.getDefaultTab());
               } else {
                  this.refreshCurrentTabContents(var6);
               }
            }
         }
      }
   }

   private void refreshCurrentTabContents(Collection<ItemStack> param0) {
      int var2 = this.menu.getRowIndexForScroll(this.scrollOffs);
      this.menu.items.clear();
      if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
         this.refreshSearchResults();
      } else {
         this.menu.items.addAll(param0);
      }

      this.scrollOffs = this.menu.getScrollForRowIndex(var2);
      this.menu.scrollTo(this.scrollOffs);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      if (this.minecraft != null) {
         if (this.minecraft.player != null) {
            this.tryRefreshInvalidatedTabs(
               this.minecraft.player.connection.enabledFeatures(), this.hasPermissions(this.minecraft.player), this.minecraft.player.level.registryAccess()
            );
         }

         if (!this.minecraft.gameMode.hasInfiniteItems()) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
         } else {
            this.searchBox.tick();
         }
      }
   }

   @Override
   protected void slotClicked(@Nullable Slot param0, int param1, int param2, ClickType param3) {
      if (this.isCreativeSlot(param0)) {
         this.searchBox.moveCursorToEnd();
         this.searchBox.setHighlightPos(0);
      }

      boolean var5 = param3 == ClickType.QUICK_MOVE;
      param3 = param1 == -999 && param3 == ClickType.PICKUP ? ClickType.THROW : param3;
      if (param0 == null && selectedTab.getType() != CreativeModeTab.Type.INVENTORY && param3 != ClickType.QUICK_CRAFT) {
         if (!this.menu.getCarried().isEmpty() && this.hasClickedOutside) {
            if (param2 == 0) {
               this.minecraft.player.drop(this.menu.getCarried(), true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
               this.menu.setCarried(ItemStack.EMPTY);
            }

            if (param2 == 1) {
               ItemStack var6 = this.menu.getCarried().split(1);
               this.minecraft.player.drop(var6, true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(var6);
            }
         }
      } else {
         if (param0 != null && !param0.mayPickup(this.minecraft.player)) {
            return;
         }

         if (param0 == this.destroyItemSlot && var5) {
            for (int var6 = 0; var6 < this.minecraft.player.inventoryMenu.getItems().size(); var6++) {
               this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, var6);
            }
         } else if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            if (param0 == this.destroyItemSlot) {
               this.menu.setCarried(ItemStack.EMPTY);
            } else if (param3 == ClickType.THROW && param0 != null && param0.hasItem()) {
               ItemStack var6 = param0.remove(param2 == 0 ? 1 : param0.getItem().getMaxStackSize());
               ItemStack var7 = param0.getItem();
               this.minecraft.player.drop(var6, true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(var6);
               this.minecraft.gameMode.handleCreativeModeItemAdd(var7, ((CreativeModeInventoryScreen.SlotWrapper)param0).target.index);
            } else if (param3 == ClickType.THROW && !this.menu.getCarried().isEmpty()) {
               this.minecraft.player.drop(this.menu.getCarried(), true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
               this.menu.setCarried(ItemStack.EMPTY);
            } else {
               this.minecraft
                  .player
                  .inventoryMenu
                  .clicked(param0 == null ? param1 : ((CreativeModeInventoryScreen.SlotWrapper)param0).target.index, param2, param3, this.minecraft.player);
               this.minecraft.player.inventoryMenu.broadcastChanges();
            }
         } else if (param3 != ClickType.QUICK_CRAFT && param0.container == CONTAINER) {
            ItemStack var6 = this.menu.getCarried();
            ItemStack var7 = param0.getItem();
            if (param3 == ClickType.SWAP) {
               if (!var7.isEmpty()) {
                  ItemStack var8 = var7.copy();
                  var8.setCount(var8.getMaxStackSize());
                  this.minecraft.player.getInventory().setItem(param2, var8);
                  this.minecraft.player.inventoryMenu.broadcastChanges();
               }

               return;
            }

            if (param3 == ClickType.CLONE) {
               if (this.menu.getCarried().isEmpty() && param0.hasItem()) {
                  ItemStack var8 = param0.getItem().copy();
                  var8.setCount(var8.getMaxStackSize());
                  this.menu.setCarried(var8);
               }

               return;
            }

            if (param3 == ClickType.THROW) {
               if (!var7.isEmpty()) {
                  ItemStack var8 = var7.copy();
                  var8.setCount(param2 == 0 ? 1 : var8.getMaxStackSize());
                  this.minecraft.player.drop(var8, true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(var8);
               }

               return;
            }

            if (!var6.isEmpty() && !var7.isEmpty() && var6.sameItem(var7) && ItemStack.tagMatches(var6, var7)) {
               if (param2 == 0) {
                  if (var5) {
                     var6.setCount(var6.getMaxStackSize());
                  } else if (var6.getCount() < var6.getMaxStackSize()) {
                     var6.grow(1);
                  }
               } else {
                  var6.shrink(1);
               }
            } else if (!var7.isEmpty() && var6.isEmpty()) {
               this.menu.setCarried(var7.copy());
               var6 = this.menu.getCarried();
               if (var5) {
                  var6.setCount(var6.getMaxStackSize());
               }
            } else if (param2 == 0) {
               this.menu.setCarried(ItemStack.EMPTY);
            } else {
               this.menu.getCarried().shrink(1);
            }
         } else if (this.menu != null) {
            ItemStack var6 = param0 == null ? ItemStack.EMPTY : this.menu.getSlot(param0.index).getItem();
            this.menu.clicked(param0 == null ? param1 : param0.index, param2, param3, this.minecraft.player);
            if (AbstractContainerMenu.getQuickcraftHeader(param2) == 2) {
               for (int var7 = 0; var7 < 9; var7++) {
                  this.minecraft.gameMode.handleCreativeModeItemAdd(this.menu.getSlot(45 + var7).getItem(), 36 + var7);
               }
            } else if (param0 != null) {
               ItemStack var7 = this.menu.getSlot(param0.index).getItem();
               this.minecraft.gameMode.handleCreativeModeItemAdd(var7, param0.index - this.menu.slots.size() + 9 + 36);
               int var8 = 45 + param2;
               if (param3 == ClickType.SWAP) {
                  this.minecraft.gameMode.handleCreativeModeItemAdd(var6, var8 - this.menu.slots.size() + 9 + 36);
               } else if (param3 == ClickType.THROW && !var6.isEmpty()) {
                  ItemStack var9 = var6.copy();
                  var9.setCount(param2 == 0 ? 1 : var9.getMaxStackSize());
                  this.minecraft.player.drop(var9, true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(var9);
               }

               this.minecraft.player.inventoryMenu.broadcastChanges();
            }
         }
      }
   }

   private boolean isCreativeSlot(@Nullable Slot param0) {
      return param0 != null && param0.container == CONTAINER;
   }

   @Override
   protected void init() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         super.init();
         this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
         this.searchBox.setMaxLength(50);
         this.searchBox.setBordered(false);
         this.searchBox.setVisible(false);
         this.searchBox.setTextColor(16777215);
         this.addWidget(this.searchBox);
         CreativeModeTab var1 = selectedTab;
         selectedTab = CreativeModeTabs.getDefaultTab();
         this.selectTab(var1);
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
         this.listener = new CreativeInventoryListener(this.minecraft);
         this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
         if (!selectedTab.shouldDisplay()) {
            this.selectTab(CreativeModeTabs.getDefaultTab());
         }
      } else {
         this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
      }
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      int var4 = this.menu.getRowIndexForScroll(this.scrollOffs);
      String var5 = this.searchBox.getValue();
      this.init(param0, param1, param2);
      this.searchBox.setValue(var5);
      if (!this.searchBox.getValue().isEmpty()) {
         this.refreshSearchResults();
      }

      this.scrollOffs = this.menu.getScrollForRowIndex(var4);
      this.menu.scrollTo(this.scrollOffs);
   }

   @Override
   public void removed() {
      super.removed();
      if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
      }
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      if (this.ignoreTextInput) {
         return false;
      }

      if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
         return false;
      }

      String var3 = this.searchBox.getValue();
      if (this.searchBox.charTyped(param0, param1)) {
         if (!Objects.equals(var3, this.searchBox.getValue())) {
            this.refreshSearchResults();
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      this.ignoreTextInput = false;
      if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
         if (this.minecraft.options.keyChat.matches(param0, param1)) {
            this.ignoreTextInput = true;
            this.selectTab(CreativeModeTabs.searchTab());
            return true;
         } else {
            return super.keyPressed(param0, param1, param2);
         }
      } else {
         boolean var4 = !this.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
         boolean var5 = InputConstants.getKey(param0, param1).getNumericKeyValue().isPresent();
         if (var4 && var5 && this.checkHotbarKeyPressed(param0, param1)) {
            this.ignoreTextInput = true;
            return true;
         }

         String var6 = this.searchBox.getValue();
         if (this.searchBox.keyPressed(param0, param1, param2)) {
            if (!Objects.equals(var6, this.searchBox.getValue())) {
               this.refreshSearchResults();
            }

            return true;
         } else {
            return this.searchBox.isFocused() && this.searchBox.isVisible() && param0 != 256 ? true : super.keyPressed(param0, param1, param2);
         }
      }
   }

   @Override
   public boolean keyReleased(int param0, int param1, int param2) {
      this.ignoreTextInput = false;
      return super.keyReleased(param0, param1, param2);
   }

   private void refreshSearchResults() {
      this.menu.items.clear();
      this.visibleTags.clear();
      String var1 = this.searchBox.getValue();
      if (var1.isEmpty()) {
         this.menu.items.addAll(selectedTab.getDisplayItems());
      } else {
         SearchTree<ItemStack> var2;
         if (var1.startsWith("#")) {
            var1 = var1.substring(1);
            var2 = this.minecraft.getSearchTree(SearchRegistry.CREATIVE_TAGS);
            this.updateVisibleTags(var1);
         } else {
            var2 = this.minecraft.getSearchTree(SearchRegistry.CREATIVE_NAMES);
         }

         this.menu.items.addAll(var2.search(var1.toLowerCase(Locale.ROOT)));
      }

      this.scrollOffs = 0.0F;
      this.menu.scrollTo(0.0F);
   }

   private void updateVisibleTags(String param0) {
      int var2 = param0.indexOf(58);
      Predicate<ResourceLocation> var3;
      if (var2 == -1) {
         var3 = l_param1 -> l_param1.getPath().contains(param0);
      } else {
         String var4 = param0.substring(0, var2).trim();
         String var5 = param0.substring(var2 + 1).trim();
         var3 = l_param2 -> l_param2.getNamespace().contains(var4) && l_param2.getPath().contains(var5);
      }

      BuiltInRegistries.ITEM.getTagNames().filter(l_param1 -> var3.test(l_param1.location())).forEach(this.visibleTags::add);
   }

   @Override
   protected void renderLabels(PoseStack param0, int param1, int param2) {
      if (selectedTab.showTitle()) {
         this.font.draw(param0, selectedTab.getDisplayName(), 8.0F, 6.0F, 4210752);
      }
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (param4 == 0) {
         double var6 = param0 - this.leftPos;
         double var8 = param2 - this.topPos;

         for (CreativeModeTab var11 : CreativeModeTabs.tabs()) {
            if (this.checkTabClicked(var11, var6, var8)) {
               return true;
            }
         }

         if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY && this.insideScrollbar(param0, param2)) {
            this.scrolling = this.canScroll();
            return true;
         }
      }

      return super.mouseClicked(param0, param2, param4);
   }

   @Override
   public boolean mouseReleased(double param0, double param2, int param4) {
      if (param4 == 0) {
         double var6 = param0 - this.leftPos;
         double var8 = param2 - this.topPos;
         this.scrolling = false;

         for (CreativeModeTab var11 : CreativeModeTabs.tabs()) {
            if (this.checkTabClicked(var11, var6, var8)) {
               this.selectTab(var11);
               return true;
            }
         }
      }

      return super.mouseReleased(param0, param2, param4);
   }

   private boolean canScroll() {
      return selectedTab.canScroll() && this.menu.canScroll();
   }

   private void selectTab(CreativeModeTab param0) {
      CreativeModeTab var2 = selectedTab;
      selectedTab = param0;
      this.quickCraftSlots.clear();
      this.menu.items.clear();
      this.clearDraggingState();
      if (selectedTab.getType() == CreativeModeTab.Type.HOTBAR) {
         HotbarManager var3 = this.minecraft.getHotbarManager();

         for (int var4 = 0; var4 < 9; var4++) {
            Hotbar var5 = var3.get(var4);
            if (var5.isEmpty()) {
               for (int var6 = 0; var6 < 9; var6++) {
                  if (var6 == var4) {
                     ItemStack var7 = new ItemStack(Items.PAPER);
                     var7.getOrCreateTagElement("CustomCreativeLock");
                     Component var8 = this.minecraft.options.keyHotbarSlots[var4].getTranslatedKeyMessage();
                     Component var9 = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                     var7.setHoverName(Component.translatable("inventory.hotbarInfo", var9, var8));
                     this.menu.items.add(var7);
                  } else {
                     this.menu.items.add(ItemStack.EMPTY);
                  }
               }
            } else {
               this.menu.items.addAll(var5);
            }
         }
      } else if (selectedTab.getType() == CreativeModeTab.Type.CATEGORY) {
         this.menu.items.addAll(selectedTab.getDisplayItems());
      }

      if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
         AbstractContainerMenu var3 = this.minecraft.player.inventoryMenu;
         if (this.originalSlots == null) {
            this.originalSlots = ImmutableList.copyOf(this.menu.slots);
         }

         this.menu.slots.clear();

         for (int var4 = 0; var4 < var3.slots.size(); var4++) {
            int var5;
            int var6;
            if (var4 >= 5 && var4 < 9) {
               int var7 = var4 - 5;
               int var8 = var7 / 2;
               int var9 = var7 % 2;
               var5 = 54 + var8 * 54;
               var6 = 6 + var9 * 27;
            } else if (var4 >= 0 && var4 < 5) {
               var5 = -2000;
               var6 = -2000;
            } else if (var4 == 45) {
               var5 = 35;
               var6 = 20;
            } else {
               int var7 = var4 - 9;
               int var8 = var7 % 9;
               int var9 = var7 / 9;
               var5 = 9 + var8 * 18;
               if (var4 >= 36) {
                  var6 = 112;
               } else {
                  var6 = 54 + var9 * 18;
               }
            }

            Slot var7 = new CreativeModeInventoryScreen.SlotWrapper(var3.slots.get(var4), var4, var5, var6);
            this.menu.slots.add(var7);
         }

         this.destroyItemSlot = new Slot(CONTAINER, 0, 173, 112);
         this.menu.slots.add(this.destroyItemSlot);
      } else if (var2.getType() == CreativeModeTab.Type.INVENTORY) {
         this.menu.slots.clear();
         this.menu.slots.addAll(this.originalSlots);
         this.originalSlots = null;
      }

      if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
         this.searchBox.setVisible(true);
         this.searchBox.setCanLoseFocus(false);
         this.searchBox.setFocused(true);
         if (var2 != param0) {
            this.searchBox.setValue("");
         }

         this.refreshSearchResults();
      } else {
         this.searchBox.setVisible(false);
         this.searchBox.setCanLoseFocus(true);
         this.searchBox.setFocused(false);
         this.searchBox.setValue("");
      }

      this.scrollOffs = 0.0F;
      this.menu.scrollTo(0.0F);
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      if (!this.canScroll()) {
         return false;
      }

      this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs, param4);
      this.menu.scrollTo(this.scrollOffs);
      return true;
   }

   @Override
   protected boolean hasClickedOutside(double param0, double param2, int param4, int param5, int param6) {
      boolean var8 = param0 < param4 || param2 < param5 || param0 >= param4 + this.imageWidth || param2 >= param5 + this.imageHeight;
      this.hasClickedOutside = var8 && !this.checkTabClicked(selectedTab, param0, param2);
      return this.hasClickedOutside;
   }

   protected boolean insideScrollbar(double param0, double param2) {
      int var5 = this.leftPos;
      int var6 = this.topPos;
      int var7 = var5 + 175;
      int var8 = var6 + 18;
      int var9 = var7 + 14;
      int var10 = var8 + 112;
      return param0 >= var7 && param2 >= var8 && param0 < var9 && param2 < var10;
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      if (this.scrolling) {
         int var10 = this.topPos + 18;
         int var11 = var10 + 112;
         this.scrollOffs = ((float)param2 - var10 - 7.5F) / (var11 - var10 - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.menu.scrollTo(this.scrollOffs);
         return true;
      } else {
         return super.mouseDragged(param0, param2, param4, param5, param7);
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      super.render(param0, param1, param2, param3);

      for (CreativeModeTab var6 : CreativeModeTabs.tabs()) {
         if (this.checkTabHovering(param0, var6, param1, param2)) {
            break;
         }
      }

      if (this.destroyItemSlot != null
         && selectedTab.getType() == CreativeModeTab.Type.INVENTORY
         && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, param1, param2)) {
         this.renderTooltip(param0, TRASH_SLOT_TOOLTIP, param1, param2);
      }

      this.renderTooltip(param0, param1, param2);
   }

   @Override
   protected void renderTooltip(PoseStack param0, ItemStack param1, int param2, int param3) {
      boolean var5 = this.hoveredSlot != null && this.hoveredSlot instanceof CreativeModeInventoryScreen.CustomCreativeSlot;
      boolean var6 = selectedTab.getType() == CreativeModeTab.Type.CATEGORY;
      boolean var7 = selectedTab.getType() == CreativeModeTab.Type.SEARCH;
      TooltipFlag.Default var8 = this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
      TooltipFlag var9 = var5 ? var8.asCreative() : var8;
      List<Component> var10 = param1.getTooltipLines(this.minecraft.player, var9);
      List<Component> var11;
      if (var6 && var5) {
         var11 = var10;
      } else {
         var11 = Lists.newArrayList(var10);
         if (var7 && var5) {
            this.visibleTags.forEach(l_param2 -> {
               if (param1.is((TagKey<Item>)l_param2)) {
                  var11.add(1, Component.literal("#" + l_param2.location()).withStyle(ChatFormatting.DARK_PURPLE));
               }
            });
         }

         int var12 = 1;

         for (CreativeModeTab var14 : CreativeModeTabs.tabs()) {
            if (var14.getType() != CreativeModeTab.Type.SEARCH && var14.contains(param1)) {
               var11.add(var12++, var14.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
            }
         }
      }

      this.renderTooltip(param0, var11, param1.getTooltipImage(), param2, param3);
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      for (CreativeModeTab var6 : CreativeModeTabs.tabs()) {
         RenderSystem.setShaderTexture(0, CREATIVE_TABS_LOCATION);
         if (var6 != selectedTab) {
            this.renderTabButton(param0, var6);
         }
      }

      RenderSystem.setShaderTexture(0, new ResourceLocation("textures/gui/container/creative_inventory/tab_" + selectedTab.getBackgroundSuffix()));
      blit(param0, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      this.searchBox.render(param0, param2, param3, param1);
      int var5 = this.leftPos + 175;
      int var6 = this.topPos + 18;
      int var7 = var6 + 112;
      RenderSystem.setShaderTexture(0, CREATIVE_TABS_LOCATION);
      if (selectedTab.canScroll()) {
         blit(param0, var5, var6 + (int)((var7 - var6 - 17) * this.scrollOffs), 232 + (this.canScroll() ? 0 : 12), 0, 12, 15);
      }

      this.renderTabButton(param0, selectedTab);
      if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
         InventoryScreen.renderEntityInInventoryFollowsMouse(
            param0, this.leftPos + 88, this.topPos + 45, 20, this.leftPos + 88 - param2, this.topPos + 45 - 30 - param3, this.minecraft.player
         );
      }
   }

   private int getTabX(CreativeModeTab param0) {
      int var2 = param0.column();
      int var3 = 27;
      int var4 = 27 * var2;
      if (param0.isAlignedRight()) {
         var4 = this.imageWidth - 27 * (7 - var2) + 1;
      }

      return var4;
   }

   private int getTabY(CreativeModeTab param0) {
      int var2 = 0;
      if (param0.row() == CreativeModeTab.Row.TOP) {
         var2 -= 32;
      } else {
         var2 += this.imageHeight;
      }

      return var2;
   }

   protected boolean checkTabClicked(CreativeModeTab param0, double param1, double param3) {
      int var6 = this.getTabX(param0);
      int var7 = this.getTabY(param0);
      return param1 >= var6 && param1 <= var6 + 26 && param3 >= var7 && param3 <= var7 + 32;
   }

   protected boolean checkTabHovering(PoseStack param0, CreativeModeTab param1, int param2, int param3) {
      int var5 = this.getTabX(param1);
      int var6 = this.getTabY(param1);
      if (this.isHovering(var5 + 3, var6 + 3, 21, 27, param2, param3)) {
         this.renderTooltip(param0, param1.getDisplayName(), param2, param3);
         return true;
      } else {
         return false;
      }
   }

   protected void renderTabButton(PoseStack param0, CreativeModeTab param1) {
      boolean var3 = param1 == selectedTab;
      boolean var4 = param1.row() == CreativeModeTab.Row.TOP;
      int var5 = param1.column();
      int var6 = var5 * 26;
      int var7 = 0;
      int var8 = this.leftPos + this.getTabX(param1);
      int var9 = this.topPos;
      int var10 = 32;
      if (var3) {
         var7 += 32;
      }

      if (var4) {
         var9 -= 28;
      } else {
         var7 += 64;
         var9 += this.imageHeight - 4;
      }

      blit(param0, var8, var9, var6, var7, 26, 32);
      param0.pushPose();
      param0.translate(0.0F, 0.0F, 100.0F);
      var8 += 5;
      var9 += 8 + (var4 ? 1 : -1);
      ItemStack var11 = param1.getIconItem();
      this.itemRenderer.renderAndDecorateItem(param0, var11, var8, var9);
      this.itemRenderer.renderGuiItemDecorations(param0, this.font, var11, var8, var9);
      param0.popPose();
   }

   public boolean isInventoryOpen() {
      return selectedTab.getType() == CreativeModeTab.Type.INVENTORY;
   }

   public static void handleHotbarLoadOrSave(Minecraft param0, int param1, boolean param2, boolean param3) {
      LocalPlayer var4 = param0.player;
      HotbarManager var5 = param0.getHotbarManager();
      Hotbar var6 = var5.get(param1);
      if (param2) {
         for (int var7 = 0; var7 < Inventory.getSelectionSize(); var7++) {
            ItemStack var8 = var6.get(var7);
            ItemStack var9 = var8.isItemEnabled(var4.level.enabledFeatures()) ? var8.copy() : ItemStack.EMPTY;
            var4.getInventory().setItem(var7, var9);
            param0.gameMode.handleCreativeModeItemAdd(var9, 36 + var7);
         }

         var4.inventoryMenu.broadcastChanges();
      } else if (param3) {
         for (int var7 = 0; var7 < Inventory.getSelectionSize(); var7++) {
            var6.set(var7, var4.getInventory().getItem(var7).copy());
         }

         Component var7 = param0.options.keyHotbarSlots[param1].getTranslatedKeyMessage();
         Component var8 = param0.options.keyLoadHotbarActivator.getTranslatedKeyMessage();
         Component var9 = Component.translatable("inventory.hotbarSaved", var8, var7);
         param0.gui.setOverlayMessage(var9, false);
         param0.getNarrator().sayNow(var9);
         var5.save();
      }
   }

   static class CustomCreativeSlot extends Slot {
      public CustomCreativeSlot(Container param0, int param1, int param2, int param3) {
         super(param0, param1, param2, param3);
      }

      @Override
      public boolean mayPickup(Player param0) {
         ItemStack var2 = this.getItem();
         return super.mayPickup(param0) && !var2.isEmpty()
            ? var2.isItemEnabled(param0.level.enabledFeatures()) && var2.getTagElement("CustomCreativeLock") == null
            : var2.isEmpty();
      }
   }

   public static class ItemPickerMenu extends AbstractContainerMenu {
      public final NonNullList<ItemStack> items = NonNullList.create();
      private final AbstractContainerMenu inventoryMenu;

      public ItemPickerMenu(Player param0) {
         super(null, 0);
         this.inventoryMenu = param0.inventoryMenu;
         Inventory var2 = param0.getInventory();

         for (int var3 = 0; var3 < 5; var3++) {
            for (int var4 = 0; var4 < 9; var4++) {
               this.addSlot(
                  new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER, var3 * 9 + var4, 9 + var4 * 18, 18 + var3 * 18)
               );
            }
         }

         for (int var3 = 0; var3 < 9; var3++) {
            this.addSlot(new Slot(var2, var3, 9 + var3 * 18, 112));
         }

         this.scrollTo(0.0F);
      }

      @Override
      public boolean stillValid(Player param0) {
         return true;
      }

      protected int calculateRowCount() {
         return Mth.positiveCeilDiv(this.items.size(), 9) - 5;
      }

      protected int getRowIndexForScroll(float param0) {
         return Math.max((int)(param0 * this.calculateRowCount() + 0.5), 0);
      }

      protected float getScrollForRowIndex(int param0) {
         return Mth.clamp((float)param0 / this.calculateRowCount(), 0.0F, 1.0F);
      }

      protected float subtractInputFromScroll(float param0, double param1) {
         return Mth.clamp(param0 - (float)(param1 / this.calculateRowCount()), 0.0F, 1.0F);
      }

      public void scrollTo(float param0) {
         int var2 = this.getRowIndexForScroll(param0);

         for (int var3 = 0; var3 < 5; var3++) {
            for (int var4 = 0; var4 < 9; var4++) {
               int var5 = var4 + (var3 + var2) * 9;
               if (var5 >= 0 && var5 < this.items.size()) {
                  CreativeModeInventoryScreen.CONTAINER.setItem(var4 + var3 * 9, this.items.get(var5));
               } else {
                  CreativeModeInventoryScreen.CONTAINER.setItem(var4 + var3 * 9, ItemStack.EMPTY);
               }
            }
         }
      }

      public boolean canScroll() {
         return this.items.size() > 45;
      }

      @Override
      public ItemStack quickMoveStack(Player param0, int param1) {
         if (param1 >= this.slots.size() - 9 && param1 < this.slots.size()) {
            Slot var3 = this.slots.get(param1);
            if (var3 != null && var3.hasItem()) {
               var3.setByPlayer(ItemStack.EMPTY);
            }
         }

         return ItemStack.EMPTY;
      }

      @Override
      public boolean canTakeItemForPickAll(ItemStack param0, Slot param1) {
         return param1.container != CreativeModeInventoryScreen.CONTAINER;
      }

      @Override
      public boolean canDragTo(Slot param0) {
         return param0.container != CreativeModeInventoryScreen.CONTAINER;
      }

      @Override
      public ItemStack getCarried() {
         return this.inventoryMenu.getCarried();
      }

      @Override
      public void setCarried(ItemStack param0) {
         this.inventoryMenu.setCarried(param0);
      }
   }

   static class SlotWrapper extends Slot {
      final Slot target;

      public SlotWrapper(Slot param0, int param1, int param2, int param3) {
         super(param0.container, param1, param2, param3);
         this.target = param0;
      }

      @Override
      public void onTake(Player param0, ItemStack param1) {
         this.target.onTake(param0, param1);
      }

      @Override
      public boolean mayPlace(ItemStack param0) {
         return this.target.mayPlace(param0);
      }

      @Override
      public ItemStack getItem() {
         return this.target.getItem();
      }

      @Override
      public boolean hasItem() {
         return this.target.hasItem();
      }

      @Override
      public void setByPlayer(ItemStack param0) {
         this.target.setByPlayer(param0);
      }

      @Override
      public void set(ItemStack param0) {
         this.target.set(param0);
      }

      @Override
      public void setChanged() {
         this.target.setChanged();
      }

      @Override
      public int getMaxStackSize() {
         return this.target.getMaxStackSize();
      }

      @Override
      public int getMaxStackSize(ItemStack param0) {
         return this.target.getMaxStackSize(param0);
      }

      @Nullable
      @Override
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
         return this.target.getNoItemIcon();
      }

      @Override
      public ItemStack remove(int param0) {
         return this.target.remove(param0);
      }

      @Override
      public boolean isActive() {
         return this.target.isActive();
      }

      @Override
      public boolean mayPickup(Player param0) {
         return this.target.mayPickup(param0);
      }
   }
}
