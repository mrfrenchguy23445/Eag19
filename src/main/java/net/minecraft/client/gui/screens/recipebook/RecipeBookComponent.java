package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeBookComponent extends GuiComponent implements PlaceRecipe<Ingredient>, Renderable, GuiEventListener, NarratableEntry, RecipeShownListener {
   protected static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
   private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint")
      .withStyle(ChatFormatting.ITALIC)
      .withStyle(ChatFormatting.GRAY);
   public static final int IMAGE_WIDTH = 147;
   public static final int IMAGE_HEIGHT = 166;
   private static final int OFFSET_X_POSITION = 86;
   private static final Component ONLY_CRAFTABLES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.craftable");
   private static final Component ALL_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.all");
   private int xOffset;
   private int width;
   private int height;
   protected final GhostRecipe ghostRecipe = new GhostRecipe();
   private final List<RecipeBookTabButton> tabButtons = Lists.newArrayList();
   @Nullable
   private RecipeBookTabButton selectedTab;
   protected StateSwitchingButton filterButton;
   protected RecipeBookMenu<?> menu;
   protected Minecraft minecraft;
   @Nullable
   private EditBox searchBox;
   private String lastSearch = "";
   private ClientRecipeBook book;
   private final RecipeBookPage recipeBookPage = new RecipeBookPage();
   private final StackedContents stackedContents = new StackedContents();
   private int timesInventoryChanged;
   private boolean ignoreTextInput;
   private boolean visible;
   private boolean widthTooNarrow;

   public void init(int param0, int param1, Minecraft param2, boolean param3, RecipeBookMenu<?> param4) {
      this.minecraft = param2;
      this.width = param0;
      this.height = param1;
      this.menu = param4;
      this.widthTooNarrow = param3;
      param2.player.containerMenu = param4;
      this.book = param2.player.getRecipeBook();
      this.timesInventoryChanged = param2.player.getInventory().getTimesChanged();
      this.visible = this.isVisibleAccordingToBookData();
      if (this.visible) {
         this.initVisuals();
      }
   }

   public void initVisuals() {
      this.xOffset = this.widthTooNarrow ? 0 : 86;
      int var1 = (this.width - 147) / 2 - this.xOffset;
      int var2 = (this.height - 166) / 2;
      this.stackedContents.clear();
      this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
      this.menu.fillCraftSlotsStackedContents(this.stackedContents);
      String var3 = this.searchBox != null ? this.searchBox.getValue() : "";
      this.searchBox = new EditBox(this.minecraft.font, var1 + 26, var2 + 14, 79, 9 + 3, Component.translatable("itemGroup.search"));
      this.searchBox.setMaxLength(50);
      this.searchBox.setVisible(true);
      this.searchBox.setTextColor(16777215);
      this.searchBox.setValue(var3);
      this.searchBox.setHint(SEARCH_HINT);
      this.recipeBookPage.init(this.minecraft, var1, var2);
      this.recipeBookPage.addListener(this);
      this.filterButton = new StateSwitchingButton(var1 + 110, var2 + 12, 26, 16, this.book.isFiltering(this.menu));
      this.updateFilterButtonTooltip();
      this.initFilterButtonTextures();
      this.tabButtons.clear();

      for (RecipeBookCategories var5 : RecipeBookCategories.getCategories(this.menu.getRecipeBookType())) {
         this.tabButtons.add(new RecipeBookTabButton(var5));
      }

      if (this.selectedTab != null) {
         this.selectedTab = this.tabButtons.stream().filter(l_param0 -> l_param0.getCategory().equals(this.selectedTab.getCategory())).findFirst().orElse(null);
      }

      if (this.selectedTab == null) {
         this.selectedTab = this.tabButtons.get(0);
      }

      this.selectedTab.setStateTriggered(true);
      this.updateCollections(false);
      this.updateTabs();
   }

   private void updateFilterButtonTooltip() {
      this.filterButton.setTooltip(this.filterButton.isStateTriggered() ? Tooltip.create(this.getRecipeFilterName()) : Tooltip.create(ALL_RECIPES_TOOLTIP));
   }

   protected void initFilterButtonTextures() {
      this.filterButton.initTextureValues(152, 41, 28, 18, RECIPE_BOOK_LOCATION);
   }

   public int updateScreenPosition(int param0, int param1) {
      int var3;
      if (this.isVisible() && !this.widthTooNarrow) {
         var3 = 177 + (param0 - param1 - 200) / 2;
      } else {
         var3 = (param0 - param1) / 2;
      }

      return var3;
   }

   public void toggleVisibility() {
      this.setVisible(!this.isVisible());
   }

   public boolean isVisible() {
      return this.visible;
   }

   private boolean isVisibleAccordingToBookData() {
      return this.book.isOpen(this.menu.getRecipeBookType());
   }

   protected void setVisible(boolean param0) {
      if (param0) {
         this.initVisuals();
      }

      this.visible = param0;
      this.book.setOpen(this.menu.getRecipeBookType(), param0);
      if (!param0) {
         this.recipeBookPage.setInvisible();
      }

      this.sendUpdateSettings();
   }

   public void slotClicked(@Nullable Slot param0) {
      if (param0 != null && param0.index < this.menu.getSize()) {
         this.ghostRecipe.clear();
         if (this.isVisible()) {
            this.updateStackedContents();
         }
      }
   }

   private void updateCollections(boolean param0) {
      List<RecipeCollection> var2 = this.book.getCollection(this.selectedTab.getCategory());
      var2.forEach(l_param0 -> l_param0.canCraft(this.stackedContents, this.menu.getGridWidth(), this.menu.getGridHeight(), this.book));
      List<RecipeCollection> var3 = Lists.newArrayList(var2);
      var3.removeIf(l_param0 -> !l_param0.hasKnownRecipes());
      var3.removeIf(l_param0 -> !l_param0.hasFitting());
      String var4 = this.searchBox.getValue();
      if (!var4.isEmpty()) {
         ObjectSet<RecipeCollection> var5 = new ObjectLinkedOpenHashSet<>(
            this.minecraft.getSearchTree(SearchRegistry.RECIPE_COLLECTIONS).search(var4.toLowerCase(Locale.ROOT))
         );
         var3.removeIf(l_param1 -> !var5.contains(l_param1));
      }

      if (this.book.isFiltering(this.menu)) {
         var3.removeIf(l_param0 -> !l_param0.hasCraftable());
      }

      this.recipeBookPage.updateCollections(var3, param0);
   }

   private void updateTabs() {
      int var1 = (this.width - 147) / 2 - this.xOffset - 30;
      int var2 = (this.height - 166) / 2 + 3;
      int var3 = 27;
      int var4 = 0;

      for (RecipeBookTabButton var6 : this.tabButtons) {
         RecipeBookCategories var7 = var6.getCategory();
         if (var7 == RecipeBookCategories.CRAFTING_SEARCH || var7 == RecipeBookCategories.FURNACE_SEARCH) {
            var6.visible = true;
            var6.setPosition(var1, var2 + 27 * var4++);
         } else if (var6.updateVisibility(this.book)) {
            var6.setPosition(var1, var2 + 27 * var4++);
            var6.startAnimation(this.minecraft);
         }
      }
   }

   public void tick() {
      boolean var1 = this.isVisibleAccordingToBookData();
      if (this.isVisible() != var1) {
         this.setVisible(var1);
      }

      if (this.isVisible()) {
         if (this.timesInventoryChanged != this.minecraft.player.getInventory().getTimesChanged()) {
            this.updateStackedContents();
            this.timesInventoryChanged = this.minecraft.player.getInventory().getTimesChanged();
         }

         this.searchBox.tick();
      }
   }

   private void updateStackedContents() {
      this.stackedContents.clear();
      this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
      this.menu.fillCraftSlotsStackedContents(this.stackedContents);
      this.updateCollections(false);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (this.isVisible()) {
         param0.pushPose();
         param0.translate(0.0F, 0.0F, 100.0F);
         RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
         int var5 = (this.width - 147) / 2 - this.xOffset;
         int var6 = (this.height - 166) / 2;
         blit(param0, var5, var6, 1, 1, 147, 166);
         this.searchBox.render(param0, param1, param2, param3);

         for (RecipeBookTabButton var8 : this.tabButtons) {
            var8.render(param0, param1, param2, param3);
         }

         this.filterButton.render(param0, param1, param2, param3);
         this.recipeBookPage.render(param0, var5, var6, param1, param2, param3);
         param0.popPose();
      }
   }

   public void renderTooltip(PoseStack param0, int param1, int param2, int param3, int param4) {
      if (this.isVisible()) {
         this.recipeBookPage.renderTooltip(param0, param3, param4);
         this.renderGhostRecipeTooltip(param0, param1, param2, param3, param4);
      }
   }

   protected Component getRecipeFilterName() {
      return ONLY_CRAFTABLES_TOOLTIP;
   }

   private void renderGhostRecipeTooltip(PoseStack param0, int param1, int param2, int param3, int param4) {
      ItemStack var6 = null;

      for (int var7 = 0; var7 < this.ghostRecipe.size(); var7++) {
         GhostRecipe.GhostIngredient var8 = this.ghostRecipe.get(var7);
         int var9 = var8.getX() + param1;
         int var10 = var8.getY() + param2;
         if (param3 >= var9 && param4 >= var10 && param3 < var9 + 16 && param4 < var10 + 16) {
            var6 = var8.getItem();
         }
      }

      if (var6 != null && this.minecraft.screen != null) {
         this.minecraft.screen.renderComponentTooltip(param0, this.minecraft.screen.getTooltipFromItem(var6), param3, param4);
      }
   }

   public void renderGhostRecipe(PoseStack param0, int param1, int param2, boolean param3, float param4) {
      this.ghostRecipe.render(param0, this.minecraft, param1, param2, param3, param4);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.isVisible() && !this.minecraft.player.isSpectator()) {
         if (this.recipeBookPage.mouseClicked(param0, param2, param4, (this.width - 147) / 2 - this.xOffset, (this.height - 166) / 2, 147, 166)) {
            Recipe<?> var6 = this.recipeBookPage.getLastClickedRecipe();
            RecipeCollection var7 = this.recipeBookPage.getLastClickedRecipeCollection();
            if (var6 != null && var7 != null) {
               if (!var7.isCraftable(var6) && this.ghostRecipe.getRecipe() == var6) {
                  return false;
               }

               this.ghostRecipe.clear();
               this.minecraft.gameMode.handlePlaceRecipe(this.minecraft.player.containerMenu.containerId, var6, Screen.hasShiftDown());
               if (!this.isOffsetNextToMainGUI()) {
                  this.setVisible(false);
               }
            }

            return true;
         } else {
            if (this.searchBox.mouseClicked(param0, param2, param4)) {
               return true;
            }

            if (this.filterButton.mouseClicked(param0, param2, param4)) {
               boolean var6 = this.toggleFiltering();
               this.filterButton.setStateTriggered(var6);
               this.updateFilterButtonTooltip();
               this.sendUpdateSettings();
               this.updateCollections(false);
               return true;
            }

            for (RecipeBookTabButton var7 : this.tabButtons) {
               if (var7.mouseClicked(param0, param2, param4)) {
                  if (this.selectedTab != var7) {
                     if (this.selectedTab != null) {
                        this.selectedTab.setStateTriggered(false);
                     }

                     this.selectedTab = var7;
                     this.selectedTab.setStateTriggered(true);
                     this.updateCollections(true);
                  }

                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean toggleFiltering() {
      RecipeBookType var1 = this.menu.getRecipeBookType();
      boolean var2 = !this.book.isFiltering(var1);
      this.book.setFiltering(var1, var2);
      return var2;
   }

   public boolean hasClickedOutside(double param0, double param2, int param4, int param5, int param6, int param7, int param8) {
      if (!this.isVisible()) {
         return true;
      }

      boolean var10 = param0 < param4 || param2 < param5 || param0 >= param4 + param6 || param2 >= param5 + param7;
      boolean var11 = param4 - 147 < param0 && param0 < param4 && param5 < param2 && param2 < param5 + param7;
      return var10 && !var11 && !this.selectedTab.isHoveredOrFocused();
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      this.ignoreTextInput = false;
      if (!this.isVisible() || this.minecraft.player.isSpectator()) {
         return false;
      } else if (param0 == 256 && !this.isOffsetNextToMainGUI()) {
         this.setVisible(false);
         return true;
      } else if (this.searchBox.keyPressed(param0, param1, param2)) {
         this.checkSearchStringUpdate();
         return true;
      } else if (this.searchBox.isFocused() && this.searchBox.isVisible() && param0 != 256) {
         return true;
      } else if (this.minecraft.options.keyChat.matches(param0, param1) && !this.searchBox.isFocused()) {
         this.ignoreTextInput = true;
         this.searchBox.setFocused(true);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyReleased(int param0, int param1, int param2) {
      this.ignoreTextInput = false;
      return GuiEventListener.super.keyReleased(param0, param1, param2);
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      if (this.ignoreTextInput) {
         return false;
      } else if (!this.isVisible() || this.minecraft.player.isSpectator()) {
         return false;
      } else if (this.searchBox.charTyped(param0, param1)) {
         this.checkSearchStringUpdate();
         return true;
      } else {
         return GuiEventListener.super.charTyped(param0, param1);
      }
   }

   @Override
   public boolean isMouseOver(double param0, double param2) {
      return false;
   }

   @Override
   public void setFocused(boolean param0) {
   }

   @Override
   public boolean isFocused() {
      return false;
   }

   private void checkSearchStringUpdate() {
      String var1 = this.searchBox.getValue().toLowerCase(Locale.ROOT);
      this.pirateSpeechForThePeople(var1);
      if (!var1.equals(this.lastSearch)) {
         this.updateCollections(false);
         this.lastSearch = var1;
      }
   }

   private void pirateSpeechForThePeople(String param0) {
      if ("excitedze".equals(param0)) {
         LanguageManager var2 = this.minecraft.getLanguageManager();
         String var3 = "en_pt";
         LanguageInfo var4 = var2.getLanguage("en_pt");
         if (var4 == null || var2.getSelected().equals("en_pt")) {
            return;
         }

         var2.setSelected("en_pt");
         this.minecraft.options.languageCode = "en_pt";
         this.minecraft.reloadResourcePacks();
         this.minecraft.options.save();
      }
   }

   private boolean isOffsetNextToMainGUI() {
      return this.xOffset == 86;
   }

   public void recipesUpdated() {
      this.updateTabs();
      if (this.isVisible()) {
         this.updateCollections(false);
      }
   }

   @Override
   public void recipesShown(List<Recipe<?>> param0) {
      for (Recipe<?> var3 : param0) {
         this.minecraft.player.removeRecipeHighlight(var3);
      }
   }

   public void setupGhostRecipe(Recipe<?> param0, List<Slot> param1) {
      ItemStack var3 = param0.getResultItem(this.minecraft.level.registryAccess());
      this.ghostRecipe.setRecipe(param0);
      this.ghostRecipe.addIngredient(Ingredient.of(var3), param1.get(0).x, param1.get(0).y);
      this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(), this.menu.getResultSlotIndex(), param0, param0.getIngredients().iterator(), 0);
   }

   @Override
   public void addItemToSlot(Iterator<Ingredient> param0, int param1, int param2, int param3, int param4) {
      Ingredient var6 = param0.next();
      if (!var6.isEmpty()) {
         Slot var7 = this.menu.slots.get(param1);
         this.ghostRecipe.addIngredient(var6, var7.x, var7.y);
      }
   }

   protected void sendUpdateSettings() {
      if (this.minecraft.getConnection() != null) {
         RecipeBookType var1 = this.menu.getRecipeBookType();
         boolean var2 = this.book.getBookSettings().isOpen(var1);
         boolean var3 = this.book.getBookSettings().isFiltering(var1);
         this.minecraft.getConnection().send(new ServerboundRecipeBookChangeSettingsPacket(var1, var2, var3));
      }
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.visible ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
   }

   @Override
   public void updateNarration(NarrationElementOutput param0) {
      List<NarratableEntry> var2 = Lists.newArrayList();
      this.recipeBookPage.listButtons(l_param1 -> {
         if (l_param1.isActive()) {
            var2.add(l_param1);
         }
      });
      var2.add(this.searchBox);
      var2.add(this.filterButton);
      var2.addAll(this.tabButtons);
      Screen.NarratableSearchResult var3 = Screen.findNarratableWidget(var2, null);
      if (var3 != null) {
         var3.entry.updateNarration(param0.nest());
      }
   }
}
