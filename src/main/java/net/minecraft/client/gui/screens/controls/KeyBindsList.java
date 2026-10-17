package net.minecraft.client.gui.screens.controls;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.ArrayUtils;

public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
   final KeyBindsScreen keyBindsScreen;
   int maxNameWidth;

   public KeyBindsList(KeyBindsScreen param0, Minecraft param1) {
      super(param1, param0.width + 45, param0.height, 20, param0.height - 32, 20);
      this.keyBindsScreen = param0;
      KeyMapping[] var3 = ArrayUtils.clone(param1.options.keyMappings);
      Arrays.sort(var3);
      String var4 = null;

      for (KeyMapping var8 : var3) {
         String var9 = var8.getCategory();
         if (!var9.equals(var4)) {
            var4 = var9;
            this.addEntry(new KeyBindsList.CategoryEntry(Component.translatable(var9)));
         }

         Component var10 = Component.translatable(var8.getName());
         int var11 = param1.font.width(var10);
         if (var11 > this.maxNameWidth) {
            this.maxNameWidth = var11;
         }

         this.addEntry(new KeyBindsList.KeyEntry(var8, var10));
      }
   }

   public void resetMappingAndUpdateButtons() {
      KeyMapping.resetMapping();
      this.refreshEntries();
   }

   public void refreshEntries() {
      this.children().forEach(KeyBindsList.Entry::refreshEntry);
   }

   @Override
   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 15;
   }

   @Override
   public int getRowWidth() {
      return super.getRowWidth() + 32;
   }

   public class CategoryEntry extends KeyBindsList.Entry {
      final Component name;
      private final int width;

      public CategoryEntry(Component param1) {
         this.name = param1;
         this.width = KeyBindsList.this.minecraft.font.width(this.name);
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         KeyBindsList.this.minecraft
            .font
            .draw(param0, this.name, KeyBindsList.this.minecraft.screen.width / 2 - this.width / 2, param2 + param5 - 9 - 1, 16777215);
      }

      @Nullable
      @Override
      public ComponentPath nextFocusPath(FocusNavigationEvent param0) {
         return null;
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return Collections.emptyList();
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
               l_param0.add(NarratedElementType.TITLE, CategoryEntry.this.name);
            }
         });
      }

      @Override
      protected void refreshEntry() {
      }
   }

   public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
      abstract void refreshEntry();
   }

   public class KeyEntry extends KeyBindsList.Entry {
      private final KeyMapping key;
      private final Component name;
      private final Button changeButton;
      private final Button resetButton;
      private boolean hasCollision = false;

      KeyEntry(KeyMapping param1, Component param2) {
         this.key = param1;
         this.name = param2;
         this.changeButton = Button.builder(param2, l_param1 -> {
               KeyBindsList.this.keyBindsScreen.selectedKey = param1;
               KeyBindsList.this.resetMappingAndUpdateButtons();
            })
            .bounds(0, 0, 75, 20)
            .createNarration(
               l_param2 -> param1.isUnbound()
                  ? Component.translatable("narrator.controls.unbound", param2)
                  : Component.translatable("narrator.controls.bound", param2, l_param2.get())
            )
            .build();
         this.resetButton = Button.builder(Component.translatable("controls.reset"), l_param1 -> {
            KeyBindsList.this.minecraft.options.setKey(param1, param1.getDefaultKey());
            KeyBindsList.this.resetMappingAndUpdateButtons();
         }).bounds(0, 0, 50, 20).createNarration(l_param1 -> Component.translatable("narrator.controls.reset", param2)).build();
         this.refreshEntry();
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         float var10003 = param3 + 90 - KeyBindsList.this.maxNameWidth;
         KeyBindsList.this.minecraft.font.draw(param0, this.name, var10003, param2 + param5 / 2 - 9 / 2, 16777215);
         this.resetButton.setX(param3 + 190);
         this.resetButton.setY(param2);
         this.resetButton.render(param0, param6, param7, param9);
         this.changeButton.setX(param3 + 105);
         this.changeButton.setY(param2);
         if (this.hasCollision) {
            int var11 = 3;
            int var12 = this.changeButton.getX() - 6;
            GuiComponent.fill(param0, var12, param2 + 2, var12 + 3, param2 + param5 + 2, ChatFormatting.RED.getColor() | 0xFF000000);
         }

         this.changeButton.render(param0, param6, param7, param9);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      @Override
      protected void refreshEntry() {
         this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
         this.resetButton.active = !this.key.isDefault();
         this.hasCollision = false;
         MutableComponent var1 = Component.empty();
         if (!this.key.isUnbound()) {
            for (KeyMapping var5 : KeyBindsList.this.minecraft.options.keyMappings) {
               if (var5 != this.key && this.key.same(var5)) {
                  if (this.hasCollision) {
                     var1.append(", ");
                  }

                  this.hasCollision = true;
                  var1.append(Component.translatable(var5.getName()));
               }
            }
         }

         if (this.hasCollision) {
            this.changeButton
               .setMessage(
                  Component.literal("[ ")
                     .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                     .append(" ]")
                     .withStyle(ChatFormatting.RED)
               );
            this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", var1)));
         } else {
            this.changeButton.setTooltip(null);
         }

         if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
            this.changeButton
               .setMessage(
                  Component.literal("> ")
                     .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                     .append(" <")
                     .withStyle(ChatFormatting.YELLOW)
               );
         }
      }
   }
}
