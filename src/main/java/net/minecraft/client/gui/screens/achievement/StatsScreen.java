package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class StatsScreen extends Screen implements StatsUpdateListener {
   private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
   protected final Screen lastScreen;
   private StatsScreen.GeneralStatisticsList statsList;
   StatsScreen.ItemStatisticsList itemStatsList;
   private StatsScreen.MobsStatisticsList mobsStatsList;
   final StatsCounter stats;
   @Nullable
   private ObjectSelectionList<?> activeList;
   private boolean isLoading = true;
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private static final int SLOT_LEFT_INSERT = 40;
   private static final int SLOT_TEXT_OFFSET = 5;
   private static final int SORT_NONE = 0;
   private static final int SORT_DOWN = -1;
   private static final int SORT_UP = 1;

   public StatsScreen(Screen param0, StatsCounter param1) {
      super(Component.translatable("gui.stats"));
      this.lastScreen = param0;
      this.stats = param1;
   }

   @Override
   protected void init() {
      this.isLoading = true;
      this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
   }

   public void initLists() {
      this.statsList = new StatsScreen.GeneralStatisticsList(this.minecraft);
      this.itemStatsList = new StatsScreen.ItemStatisticsList(this.minecraft);
      this.mobsStatsList = new StatsScreen.MobsStatisticsList(this.minecraft);
   }

   public void initButtons() {
      this.addRenderableWidget(
         Button.builder(Component.translatable("stat.generalButton"), l_param0 -> this.setActiveList(this.statsList))
            .bounds(this.width / 2 - 120, this.height - 52, 80, 20)
            .build()
      );
      Button var1 = this.addRenderableWidget(
         Button.builder(Component.translatable("stat.itemsButton"), l_param0 -> this.setActiveList(this.itemStatsList))
            .bounds(this.width / 2 - 40, this.height - 52, 80, 20)
            .build()
      );
      Button var2 = this.addRenderableWidget(
         Button.builder(Component.translatable("stat.mobsButton"), l_param0 -> this.setActiveList(this.mobsStatsList))
            .bounds(this.width / 2 + 40, this.height - 52, 80, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
            .build()
      );
      if (this.itemStatsList.children().isEmpty()) {
         var1.active = false;
      }

      if (this.mobsStatsList.children().isEmpty()) {
         var2.active = false;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (this.isLoading) {
         this.renderBackground(param0);
         drawCenteredString(param0, this.font, PENDING_TEXT, this.width / 2, this.height / 2, 16777215);
         drawCenteredString(
            param0, this.font, LOADING_SYMBOLS[(int)(Util.getMillis() / 150L % LOADING_SYMBOLS.length)], this.width / 2, this.height / 2 + 9 * 2, 16777215
         );
      } else {
         this.getActiveList().render(param0, param1, param2, param3);
         drawCenteredString(param0, this.font, this.title, this.width / 2, 20, 16777215);
         super.render(param0, param1, param2, param3);
      }
   }

   @Override
   public void onStatsUpdated() {
      if (this.isLoading) {
         this.initLists();
         this.initButtons();
         this.setActiveList(this.statsList);
         this.isLoading = false;
      }
   }

   @Override
   public boolean isPauseScreen() {
      return !this.isLoading;
   }

   @Nullable
   public ObjectSelectionList<?> getActiveList() {
      return this.activeList;
   }

   public void setActiveList(@Nullable ObjectSelectionList<?> param0) {
      if (this.activeList != null) {
         this.removeWidget(this.activeList);
      }

      if (param0 != null) {
         this.addWidget(param0);
         this.activeList = param0;
      }
   }

   static String getTranslationKey(Stat<ResourceLocation> param0) {
      return "stat." + param0.getValue().toString().replace(':', '.');
   }

   int getColumnX(int param0) {
      return 115 + 40 * param0;
   }

   void blitSlot(PoseStack param0, int param1, int param2, Item param3) {
      this.blitSlotIcon(param0, param1 + 1, param2 + 1, 0, 0);
      this.itemRenderer.renderGuiItem(param0, param3.getDefaultInstance(), param1 + 2, param2 + 2);
   }

   void blitSlotIcon(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, STATS_ICON_LOCATION);
      blit(param0, param1, param2, 0, param3, param4, 18, 18, 128, 128);
   }

   class GeneralStatisticsList extends ObjectSelectionList<StatsScreen.GeneralStatisticsList.Entry> {
      public GeneralStatisticsList(Minecraft param1) {
         super(param1, StatsScreen.this.width, StatsScreen.this.height, 32, StatsScreen.this.height - 64, 10);
         ObjectArrayList<Stat<ResourceLocation>> var3 = new ObjectArrayList<>(Stats.CUSTOM.iterator());
         var3.sort(Comparator.comparing(l_param0 -> I18n.get(StatsScreen.getTranslationKey((Stat<ResourceLocation>)l_param0))));

         for (Stat<ResourceLocation> var5 : var3) {
            this.addEntry(new StatsScreen.GeneralStatisticsList.Entry(var5));
         }
      }

      @Override
      protected void renderBackground(PoseStack param0) {
         StatsScreen.this.renderBackground(param0);
      }

      class Entry extends ObjectSelectionList.Entry<StatsScreen.GeneralStatisticsList.Entry> {
         private final Stat<ResourceLocation> stat;
         private final Component statDisplay;

         Entry(Stat<ResourceLocation> param1) {
            this.stat = param1;
            this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(param1));
         }

         private String getValueText() {
            return this.stat.format(StatsScreen.this.stats.getValue(this.stat));
         }

         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            GuiComponent.drawString(param0, StatsScreen.this.font, this.statDisplay, param3 + 2, param2 + 1, param1 % 2 == 0 ? 16777215 : 9474192);
            String var11 = this.getValueText();
            GuiComponent.drawString(
               param0, StatsScreen.this.font, var11, param3 + 2 + 213 - StatsScreen.this.font.width(var11), param2 + 1, param1 % 2 == 0 ? 16777215 : 9474192
            );
         }

         @Override
         public Component getNarration() {
            return Component.translatable(
               "narrator.select", Component.empty().append(this.statDisplay).append(CommonComponents.SPACE).append(this.getValueText())
            );
         }
      }
   }

   class ItemStatisticsList extends ObjectSelectionList<StatsScreen.ItemStatisticsList.ItemRow> {
      protected final List<StatType<Block>> blockColumns;
      protected final List<StatType<Item>> itemColumns;
      private final int[] iconOffsets = new int[]{3, 4, 1, 2, 5, 6};
      protected int headerPressed = -1;
      protected final Comparator<StatsScreen.ItemStatisticsList.ItemRow> itemStatSorter = new StatsScreen.ItemStatisticsList.ItemRowComparator();
      @Nullable
      protected StatType<?> sortColumn;
      protected int sortOrder;

      public ItemStatisticsList(Minecraft param1) {
         super(param1, StatsScreen.this.width, StatsScreen.this.height, 32, StatsScreen.this.height - 64, 20);
         this.blockColumns = Lists.newArrayList();
         this.blockColumns.add(Stats.BLOCK_MINED);
         this.itemColumns = Lists.newArrayList(Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED);
         this.setRenderHeader(true, 20);
         Set<Item> var3 = Sets.newIdentityHashSet();

         for (Item var5 : BuiltInRegistries.ITEM) {
            boolean var6 = false;

            for (StatType<Item> var8 : this.itemColumns) {
               if (var8.contains(var5) && StatsScreen.this.stats.getValue(var8.get(var5)) > 0) {
                  var6 = true;
               }
            }

            if (var6) {
               var3.add(var5);
            }
         }

         for (Block var5 : BuiltInRegistries.BLOCK) {
            boolean var6 = false;

            for (StatType<Block> var8 : this.blockColumns) {
               if (var8.contains(var5) && StatsScreen.this.stats.getValue(var8.get(var5)) > 0) {
                  var6 = true;
               }
            }

            if (var6) {
               var3.add(var5.asItem());
            }
         }

         var3.remove(Items.AIR);

         for (Item var5 : var3) {
            this.addEntry(new StatsScreen.ItemStatisticsList.ItemRow(var5));
         }
      }

      @Override
      protected void renderHeader(PoseStack param0, int param1, int param2) {
         if (!this.minecraft.mouseHandler.isLeftPressed()) {
            this.headerPressed = -1;
         }

         for (int var4 = 0; var4 < this.iconOffsets.length; var4++) {
            StatsScreen.this.blitSlotIcon(param0, param1 + StatsScreen.this.getColumnX(var4) - 18, param2 + 1, 0, this.headerPressed == var4 ? 0 : 18);
         }

         if (this.sortColumn != null) {
            int var4 = StatsScreen.this.getColumnX(this.getColumnIndex(this.sortColumn)) - 36;
            int var5 = this.sortOrder == 1 ? 2 : 1;
            StatsScreen.this.blitSlotIcon(param0, param1 + var4, param2 + 1, 18 * var5, 0);
         }

         for (int var4 = 0; var4 < this.iconOffsets.length; var4++) {
            int var5 = this.headerPressed == var4 ? 1 : 0;
            StatsScreen.this.blitSlotIcon(param0, param1 + StatsScreen.this.getColumnX(var4) - 18 + var5, param2 + 1 + var5, 18 * this.iconOffsets[var4], 18);
         }
      }

      @Override
      public int getRowWidth() {
         return 375;
      }

      @Override
      protected int getScrollbarPosition() {
         return this.width / 2 + 140;
      }

      @Override
      protected void renderBackground(PoseStack param0) {
         StatsScreen.this.renderBackground(param0);
      }

      @Override
      protected void clickedHeader(int param0, int param1) {
         this.headerPressed = -1;

         for (int var3 = 0; var3 < this.iconOffsets.length; var3++) {
            int var4 = param0 - StatsScreen.this.getColumnX(var3);
            if (var4 >= -36 && var4 <= 0) {
               this.headerPressed = var3;
               break;
            }
         }

         if (this.headerPressed >= 0) {
            this.sortByColumn(this.getColumn(this.headerPressed));
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }
      }

      private StatType<?> getColumn(int param0) {
         return param0 < this.blockColumns.size() ? this.blockColumns.get(param0) : this.itemColumns.get(param0 - this.blockColumns.size());
      }

      private int getColumnIndex(StatType<?> param0) {
         int var2 = this.blockColumns.indexOf(param0);
         if (var2 >= 0) {
            return var2;
         }

         int var3 = this.itemColumns.indexOf(param0);
         return var3 >= 0 ? var3 + this.blockColumns.size() : -1;
      }

      @Override
      protected void renderDecorations(PoseStack param0, int param1, int param2) {
         if (param2 >= this.y0 && param2 <= this.y1) {
            StatsScreen.ItemStatisticsList.ItemRow var4 = this.getHovered();
            int var5 = (this.width - this.getRowWidth()) / 2;
            if (var4 != null) {
               if (param1 < var5 + 40 || param1 > var5 + 40 + 20) {
                  return;
               }

               Item var6 = var4.getItem();
               this.renderMousehoverTooltip(param0, this.getString(var6), param1, param2);
            } else {
               Component var6 = null;
               int var7 = param1 - var5;

               for (int var8 = 0; var8 < this.iconOffsets.length; var8++) {
                  int var9 = StatsScreen.this.getColumnX(var8);
                  if (var7 >= var9 - 18 && var7 <= var9) {
                     var6 = this.getColumn(var8).getDisplayName();
                     break;
                  }
               }

               this.renderMousehoverTooltip(param0, var6, param1, param2);
            }
         }
      }

      protected void renderMousehoverTooltip(PoseStack param0, @Nullable Component param1, int param2, int param3) {
         if (param1 != null) {
            int var5 = param2 + 12;
            int var6 = param3 - 12;
            int var7 = StatsScreen.this.font.width(param1);
            fillGradient(param0, var5 - 3, var6 - 3, var5 + var7 + 3, var6 + 8 + 3, -1073741824, -1073741824);
            param0.pushPose();
            param0.translate(0.0F, 0.0F, 400.0F);
            StatsScreen.this.font.drawShadow(param0, param1, var5, var6, -1);
            param0.popPose();
         }
      }

      protected Component getString(Item param0) {
         return param0.getDescription();
      }

      protected void sortByColumn(StatType<?> param0) {
         if (param0 != this.sortColumn) {
            this.sortColumn = param0;
            this.sortOrder = -1;
         } else if (this.sortOrder == -1) {
            this.sortOrder = 1;
         } else {
            this.sortColumn = null;
            this.sortOrder = 0;
         }

         this.children().sort(this.itemStatSorter);
      }

      class ItemRow extends ObjectSelectionList.Entry<StatsScreen.ItemStatisticsList.ItemRow> {
         private final Item item;

         ItemRow(Item param1) {
            this.item = param1;
         }

         public Item getItem() {
            return this.item;
         }

         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            StatsScreen.this.blitSlot(param0, param3 + 40, param2, this.item);

            for (int var11 = 0; var11 < StatsScreen.this.itemStatsList.blockColumns.size(); var11++) {
               Stat<Block> var12;
               if (this.item instanceof BlockItem) {
                  var12 = StatsScreen.this.itemStatsList.blockColumns.get(var11).get(((BlockItem)this.item).getBlock());
               } else {
                  var12 = null;
               }

               this.renderStat(param0, var12, param3 + StatsScreen.this.getColumnX(var11), param2, param1 % 2 == 0);
            }

            for (int var11 = 0; var11 < StatsScreen.this.itemStatsList.itemColumns.size(); var11++) {
               this.renderStat(
                  param0,
                  StatsScreen.this.itemStatsList.itemColumns.get(var11).get(this.item),
                  param3 + StatsScreen.this.getColumnX(var11 + StatsScreen.this.itemStatsList.blockColumns.size()),
                  param2,
                  param1 % 2 == 0
               );
            }
         }

         protected void renderStat(PoseStack param0, @Nullable Stat<?> param1, int param2, int param3, boolean param4) {
            String var6 = param1 == null ? "-" : param1.format(StatsScreen.this.stats.getValue(param1));
            GuiComponent.drawString(param0, StatsScreen.this.font, var6, param2 - StatsScreen.this.font.width(var6), param3 + 5, param4 ? 16777215 : 9474192);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.item.getDescription());
         }
      }

      class ItemRowComparator implements Comparator<StatsScreen.ItemStatisticsList.ItemRow> {
         public int compare(StatsScreen.ItemStatisticsList.ItemRow param0, StatsScreen.ItemStatisticsList.ItemRow param1) {
            Item var3 = param0.getItem();
            Item var4 = param1.getItem();
            int var5;
            int var6;
            if (ItemStatisticsList.this.sortColumn == null) {
               var5 = 0;
               var6 = 0;
            } else if (ItemStatisticsList.this.blockColumns.contains(ItemStatisticsList.this.sortColumn)) {
               StatType<Block> var7 = (StatType<Block>)ItemStatisticsList.this.sortColumn;
               var5 = var3 instanceof BlockItem ? StatsScreen.this.stats.getValue(var7, ((BlockItem)var3).getBlock()) : -1;
               var6 = var4 instanceof BlockItem ? StatsScreen.this.stats.getValue(var7, ((BlockItem)var4).getBlock()) : -1;
            } else {
               StatType<Item> var7 = (StatType<Item>)ItemStatisticsList.this.sortColumn;
               var5 = StatsScreen.this.stats.getValue(var7, var3);
               var6 = StatsScreen.this.stats.getValue(var7, var4);
            }

            return var5 == var6
               ? ItemStatisticsList.this.sortOrder * Integer.compare(Item.getId(var3), Item.getId(var4))
               : ItemStatisticsList.this.sortOrder * Integer.compare(var5, var6);
         }
      }
   }

   class MobsStatisticsList extends ObjectSelectionList<StatsScreen.MobsStatisticsList.MobRow> {
      public MobsStatisticsList(Minecraft param1) {
         super(param1, StatsScreen.this.width, StatsScreen.this.height, 32, StatsScreen.this.height - 64, 9 * 4);

         for (EntityType<?> var4 : BuiltInRegistries.ENTITY_TYPE) {
            if (StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(var4)) > 0 || StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(var4)) > 0) {
               this.addEntry(new StatsScreen.MobsStatisticsList.MobRow(var4));
            }
         }
      }

      @Override
      protected void renderBackground(PoseStack param0) {
         StatsScreen.this.renderBackground(param0);
      }

      class MobRow extends ObjectSelectionList.Entry<StatsScreen.MobsStatisticsList.MobRow> {
         private final Component mobName;
         private final Component kills;
         private final boolean hasKills;
         private final Component killedBy;
         private final boolean wasKilledBy;

         public MobRow(EntityType<?> param1) {
            this.mobName = param1.getDescription();
            int var3 = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(param1));
            if (var3 == 0) {
               this.kills = Component.translatable("stat_type.minecraft.killed.none", this.mobName);
               this.hasKills = false;
            } else {
               this.kills = Component.translatable("stat_type.minecraft.killed", var3, this.mobName);
               this.hasKills = true;
            }

            int var4 = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(param1));
            if (var4 == 0) {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", this.mobName);
               this.wasKilledBy = false;
            } else {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by", this.mobName, var4);
               this.wasKilledBy = true;
            }
         }

         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            GuiComponent.drawString(param0, StatsScreen.this.font, this.mobName, param3 + 2, param2 + 1, 16777215);
            GuiComponent.drawString(param0, StatsScreen.this.font, this.kills, param3 + 2 + 10, param2 + 1 + 9, this.hasKills ? 9474192 : 6316128);
            GuiComponent.drawString(param0, StatsScreen.this.font, this.killedBy, param3 + 2 + 10, param2 + 1 + 9 * 2, this.wasKilledBy ? 9474192 : 6316128);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.kills, this.killedBy));
         }
      }
   }
}
