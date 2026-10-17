package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class ComponentRenderUtils {
   private static final FormattedCharSequence INDENT = FormattedCharSequence.codepoint(32, Style.EMPTY);

   private static String stripColor(String param0) {
      return Minecraft.getInstance().options.chatColors().get() ? param0 : ChatFormatting.stripFormatting(param0);
   }

   public static List<FormattedCharSequence> wrapComponents(FormattedText param0, int param1, Font param2) {
      ComponentCollector var3 = new ComponentCollector();
      param0.visit((l_param1, l_param2) -> {
         var3.append(FormattedText.of(stripColor(l_param2), l_param1));
         return Optional.empty();
      }, Style.EMPTY);
      List<FormattedCharSequence> var4 = Lists.newArrayList();
      param2.getSplitter().splitLines(var3.getResultOrEmpty(), param1, Style.EMPTY, (l_param1, l_param2) -> {
         FormattedCharSequence l_var3 = Language.getInstance().getVisualOrder(l_param1);
         var4.add(l_param2 ? FormattedCharSequence.composite(INDENT, l_var3) : l_var3);
      });
      return var4.isEmpty() ? Lists.newArrayList(FormattedCharSequence.EMPTY) : var4;
   }
}
