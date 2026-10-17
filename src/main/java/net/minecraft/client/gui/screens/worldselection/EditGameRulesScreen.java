package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;

public class EditGameRulesScreen extends Screen {
   private final Consumer<Optional<GameRules>> exitCallback;
   private EditGameRulesScreen.RuleList rules;
   private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.newHashSet();
   private Button doneButton;
   @Nullable
   private List<FormattedCharSequence> tooltip;
   private final GameRules gameRules;

   public EditGameRulesScreen(GameRules param0, Consumer<Optional<GameRules>> param1) {
      super(Component.translatable("editGamerule.title"));
      this.gameRules = param0;
      this.exitCallback = param1;
   }

   @Override
   protected void init() {
      this.rules = new EditGameRulesScreen.RuleList(this.gameRules);
      this.addWidget(this.rules);
      GridLayout.RowHelper var1 = new GridLayout().columnSpacing(10).createRowHelper(2);
      this.doneButton = var1.addChild(Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.exitCallback.accept(Optional.of(this.gameRules))).build());
      var1.addChild(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.exitCallback.accept(Optional.empty())).build());
      var1.getGrid().visitWidgets(l_param1 -> {
         AbstractWidget var10000 = this.addRenderableWidget(l_param1);
      });
      var1.getGrid().setPosition(this.width / 2 - 155, this.height - 28);
      var1.getGrid().arrangeElements();
   }

   @Override
   public void onClose() {
      this.exitCallback.accept(Optional.empty());
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.tooltip = null;
      this.rules.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 20, 16777215);
      super.render(param0, param1, param2, param3);
   }

   private void updateDoneButton() {
      this.doneButton.active = this.invalidEntries.isEmpty();
   }

   void markInvalid(EditGameRulesScreen.RuleEntry param0) {
      this.invalidEntries.add(param0);
      this.updateDoneButton();
   }

   void clearInvalid(EditGameRulesScreen.RuleEntry param0) {
      this.invalidEntries.remove(param0);
      this.updateDoneButton();
   }

   public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final CycleButton<Boolean> checkbox;

      public BooleanRuleEntry(Component param1, List<FormattedCharSequence> param2, String param3, GameRules.BooleanValue param4) {
         super(param2, param1);
         this.checkbox = CycleButton.onOffBuilder(param4.get())
            .displayOnlyValue()
            .withCustomNarration(l_param1 -> l_param1.createDefaultNarrationMessage().append("\n").append(param3))
            .create(10, 5, 44, 20, param1, (l_param1, l_param2) -> param4.set(l_param2, null));
         this.children.add(this.checkbox);
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderLabel(param0, param2, param3);
         this.checkbox.setX(param3 + param4 - 45);
         this.checkbox.setY(param2);
         this.checkbox.render(param0, param6, param7, param9);
      }
   }

   public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
      final Component label;

      public CategoryRuleEntry(Component param1) {
         super(null);
         this.label = param1;
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         GuiComponent.drawCenteredString(param0, EditGameRulesScreen.this.minecraft.font, this.label, param3 + param4 / 2, param2 + 5, 16777215);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of();
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            @Override
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(NarrationElementOutput l_param0) {
               l_param0.add(NarratedElementType.TITLE, CategoryRuleEntry.this.label);
            }
         });
      }
   }

   @FunctionalInterface
   interface EntryFactory<T extends GameRules.Value<T>> {
      EditGameRulesScreen.RuleEntry create(Component var1, List<FormattedCharSequence> var2, String var3, T var4);
   }

   public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
      private final List<FormattedCharSequence> label;
      protected final List<AbstractWidget> children = Lists.newArrayList();

      public GameRuleEntry(@Nullable List<FormattedCharSequence> param1, Component param2) {
         super(param1);
         this.label = EditGameRulesScreen.this.minecraft.font.split(param2, 175);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      protected void renderLabel(PoseStack param0, int param1, int param2) {
         if (this.label.size() == 1) {
            EditGameRulesScreen.this.minecraft.font.draw(param0, this.label.get(0), param2, param1 + 5, 16777215);
         } else if (this.label.size() >= 2) {
            EditGameRulesScreen.this.minecraft.font.draw(param0, this.label.get(0), param2, param1, 16777215);
            EditGameRulesScreen.this.minecraft.font.draw(param0, this.label.get(1), param2, param1 + 10, 16777215);
         }
      }
   }

   public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final EditBox input;

      public IntegerRuleEntry(Component param1, List<FormattedCharSequence> param2, String param3, GameRules.IntegerValue param4) {
         super(param2, param1);
         this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 42, 20, param1.copy().append("\n").append(param3).append("\n"));
         this.input.setValue(Integer.toString(param4.get()));
         this.input.setResponder(l_param1 -> {
            if (param4.tryDeserialize(l_param1)) {
               this.input.setTextColor(14737632);
               EditGameRulesScreen.this.clearInvalid(this);
            } else {
               this.input.setTextColor(16711680);
               EditGameRulesScreen.this.markInvalid(this);
            }
         });
         this.children.add(this.input);
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderLabel(param0, param2, param3);
         this.input.setX(param3 + param4 - 44);
         this.input.setY(param2);
         this.input.render(param0, param6, param7, param9);
      }
   }

   public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
      @Nullable
      final List<FormattedCharSequence> tooltip;

      public RuleEntry(@Nullable List<FormattedCharSequence> param0) {
         this.tooltip = param0;
      }
   }

   public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
      public RuleList(final GameRules param1) {
         super(
            EditGameRulesScreen.this.minecraft, EditGameRulesScreen.this.width, EditGameRulesScreen.this.height, 43, EditGameRulesScreen.this.height - 32, 24
         );
         final Map<GameRules.Category, Map<GameRules.Key<?>, EditGameRulesScreen.RuleEntry>> var3 = Maps.newHashMap();
         GameRules.visitGameRuleTypes(
            new GameRules.GameRuleTypeVisitor() {
               @Override
               public void visitBoolean(GameRules.Key<GameRules.BooleanValue> l_param0, GameRules.Type<GameRules.BooleanValue> l_param1) {
                  this.addEntry(
                     l_param0,
                     (l2_param0, l2_param1, l2_param2, l2_param3) -> EditGameRulesScreen.this.new BooleanRuleEntry(l2_param0, l2_param1, l2_param2, l2_param3)
                  );
               }

               @Override
               public void visitInteger(GameRules.Key<GameRules.IntegerValue> l_param0, GameRules.Type<GameRules.IntegerValue> l_param1) {
                  this.addEntry(
                     l_param0,
                     (l2_param0, l2_param1, l2_param2, l2_param3) -> EditGameRulesScreen.this.new IntegerRuleEntry(l2_param0, l2_param1, l2_param2, l2_param3)
                  );
               }

               private <T extends GameRules.Value<T>> void addEntry(GameRules.Key<T> l_param0, EditGameRulesScreen.EntryFactory<T> l_param1) {
                  Component l_var3 = Component.translatable(l_param0.getDescriptionId());
                  Component l_var4 = Component.literal(l_param0.getId()).withStyle(ChatFormatting.YELLOW);
                  T l_var5 = param1.getRule(l_param0);
                  String l_var6 = l_var5.serialize();
                  Component l_var7 = Component.translatable("editGamerule.default", Component.literal(l_var6)).withStyle(ChatFormatting.GRAY);
                  String l_var8 = l_param0.getDescriptionId() + ".description";
                  List<FormattedCharSequence> l_var9;
                  String l_var10;
                  if (I18n.exists(l_var8)) {
                     Builder<FormattedCharSequence> l_var11 = ImmutableList.<FormattedCharSequence>builder().add(l_var4.getVisualOrderText());
                     Component l_var12 = Component.translatable(l_var8);
                     EditGameRulesScreen.this.font.split(l_var12, 150).forEach(l_var11::add);
                     l_var9 = l_var11.add(l_var7.getVisualOrderText()).build();
                     l_var10 = l_var12.getString() + "\n" + l_var7.getString();
                  } else {
                     l_var9 = ImmutableList.of(l_var4.getVisualOrderText(), l_var7.getVisualOrderText());
                     l_var10 = l_var7.getString();
                  }

                  var3.computeIfAbsent(l_param0.getCategory(), l2_param0 -> Maps.newHashMap()).put(l_param0, l_param1.create(l_var3, l_var9, l_var10, l_var5));
               }
            }
         );
         var3.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(
               l_param0 -> {
                  this.addEntry(
                     EditGameRulesScreen.this.new CategoryRuleEntry(
                        Component.translatable(l_param0.getKey().getDescriptionId()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)
                     )
                  );
                  l_param0.getValue()
                     .entrySet()
                     .stream()
                     .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRules.Key::getId)))
                     .forEach(l2_param0 -> this.addEntry(l2_param0.getValue()));
               }
            );
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, float param3) {
         super.render(param0, param1, param2, param3);
         EditGameRulesScreen.RuleEntry var5 = this.getHovered();
         if (var5 != null && var5.tooltip != null) {
            EditGameRulesScreen.this.setTooltipForNextRenderPass(var5.tooltip);
         }
      }
   }
}
