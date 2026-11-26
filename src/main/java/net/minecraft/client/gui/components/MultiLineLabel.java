package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      @Override
      public int renderCentered(PoseStack param0, int param1, int param2) {
         return param2;
      }

      @Override
      public int renderCentered(PoseStack param0, int param1, int param2, int param3, int param4) {
         return param2;
      }

      @Override
      public int renderLeftAligned(PoseStack param0, int param1, int param2, int param3, int param4) {
         return param2;
      }

      @Override
      public int renderLeftAlignedNoShadow(PoseStack param0, int param1, int param2, int param3, int param4) {
         return param2;
      }

      @Override
      public void renderBackgroundCentered(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      }

      @Override
      public int getLineCount() {
         return 0;
      }

      @Override
      public int getWidth() {
         return 0;
      }
   };

   static MultiLineLabel create(Font param0, FormattedText param1, int param2) {
      return createFixed(
         param0,
         param0.split(param1, param2)
            .stream()
            .map(l_param1 -> new MultiLineLabel.TextWithWidth(l_param1, param0.width(l_param1)))
            .collect(ImmutableList.toImmutableList())
      );
   }

   static MultiLineLabel create(Font param0, FormattedText param1, int param2, int param3) {
      return createFixed(
         param0,
         param0.split(param1, param2)
            .stream()
            .limit(param3)
            .map(l_param1 -> new MultiLineLabel.TextWithWidth(l_param1, param0.width(l_param1)))
            .collect(ImmutableList.toImmutableList())
      );
   }

   static MultiLineLabel create(Font param0, Component... param1) {
      return createFixed(
         param0,
         Arrays.stream(param1)
            .map(Component::getVisualOrderText)
            .map(l_param1 -> new MultiLineLabel.TextWithWidth(l_param1, param0.width(l_param1)))
            .collect(ImmutableList.toImmutableList())
      );
   }

   static MultiLineLabel create(Font param0, List<Component> param1) {
      return createFixed(
         param0,
         param1.stream()
            .map(Component::getVisualOrderText)
            .map(l_param1 -> new MultiLineLabel.TextWithWidth(l_param1, param0.width(l_param1)))
            .collect(ImmutableList.toImmutableList())
      );
   }

   static MultiLineLabel createFixed(final Font param0, final List<MultiLineLabel.TextWithWidth> param1) {
      return param1.isEmpty()
         ? EMPTY
         : new MultiLineLabel() {
            private final int width = param1.stream().mapToInt(l2_param0 -> l2_param0.width).max().orElse(0);

            @Override
            public int renderCentered(PoseStack l_param0, int l_param1, int l_param2) {
               return this.renderCentered(l_param0, l_param1, l_param2, 9, 16777215);
            }

            @Override
            public int renderCentered(PoseStack l_param0, int l_param1, int l_param2, int l_param3, int l_param4) {
               int l_var6 = l_param2;

               for (MultiLineLabel.TextWithWidth l_var8 : param1) {
                  param0.drawShadow(l_param0, l_var8.text, l_param1 - l_var8.width / 2, l_var6, l_param4);
                  l_var6 += l_param3;
               }

               return l_var6;
            }

            @Override
            public int renderLeftAligned(PoseStack l_param0, int l_param1, int l_param2, int l_param3, int l_param4) {
               int l_var6 = l_param2;

               for (MultiLineLabel.TextWithWidth l_var8 : param1) {
                  param0.drawShadow(l_param0, l_var8.text, l_param1, l_var6, l_param4);
                  l_var6 += l_param3;
               }

               return l_var6;
            }

            @Override
            public int renderLeftAlignedNoShadow(PoseStack l_param0, int l_param1, int l_param2, int l_param3, int l_param4) {
               int l_var6 = l_param2;

               for (MultiLineLabel.TextWithWidth l_var8 : param1) {
                  param0.draw(l_param0, l_var8.text, l_param1, l_var6, l_param4);
                  l_var6 += l_param3;
               }

               return l_var6;
            }

            @Override
            public void renderBackgroundCentered(PoseStack l_param0, int l_param1, int l_param2, int l_param3, int l_param4, int l_param5) {
               int l_var7 = param1.stream().mapToInt(l2_param0 -> l2_param0.width).max().orElse(0);
               if (l_var7 > 0) {
                  GuiComponent.fill(
                     l_param0,
                     l_param1 - l_var7 / 2 - l_param4,
                     l_param2 - l_param4,
                     l_param1 + l_var7 / 2 + l_param4,
                     l_param2 + param1.size() * l_param3 + l_param4,
                     l_param5
                  );
               }
            }

            @Override
            public int getLineCount() {
               return param1.size();
            }

            @Override
            public int getWidth() {
               return this.width;
            }
         };
   }

   int renderCentered(PoseStack var1, int var2, int var3);

   int renderCentered(PoseStack var1, int var2, int var3, int var4, int var5);

   int renderLeftAligned(PoseStack var1, int var2, int var3, int var4, int var5);

   int renderLeftAlignedNoShadow(PoseStack var1, int var2, int var3, int var4, int var5);

   void renderBackgroundCentered(PoseStack var1, int var2, int var3, int var4, int var5, int var6);

   int getLineCount();

   int getWidth();

   class TextWithWidth {
      final FormattedCharSequence text;
      final int width;

      TextWithWidth(FormattedCharSequence param0, int param1) {
         this.text = param0;
         this.width = param1;
      }
   }
}
