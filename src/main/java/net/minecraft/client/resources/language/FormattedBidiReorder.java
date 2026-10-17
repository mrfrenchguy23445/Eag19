package net.minecraft.client.resources.language;

import com.google.common.collect.Lists;
import jdk_internal.bidi.Bidi;
import jdk_internal.icu.lang.UCharacter;
import java.util.List;
import net.lax1dude.eaglercraft.v1_8.EaglerBidiReorder;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.util.FormattedCharSequence;

public class FormattedBidiReorder {
   public static FormattedCharSequence reorder(FormattedText param0, boolean param1) {
      // The third argument SubStringSource takes is a per-substring text transform, and for
      // 1.19.4 that was Arabic shaping. It is gone, and there is no two-argument create to
      // fall back to -- the signature is create(FormattedText) with no mirror function at
      // all, which would also drop the mirroring -- so the transform is spelled out as the
      // identity. The mirror function is the part that still matters: SubStringSource applies
      // it to every right-to-left run it is asked to reverse.
      SubStringSource var2 = SubStringSource.create(param0, UCharacter::getMirror, l_param0 -> l_param0);
      Bidi var3 = new Bidi(var2.getPlainText(), param1 ? 127 : 126);
      List<FormattedCharSequence> var4 = Lists.newArrayList();

      // Upstream walked ICU4J's getVisualRun, which hands back runs already in visual order.
      // The JDK's Bidi reports them in logical order, so EaglerBidiReorder.visualRuns does the
      // reordering step that ICU4J would have done. Entries are {start, limit, level}, and the
      // level is the embedding level of the run now at that visual position.
      for (int[] var5 : EaglerBidiReorder.visualRuns(var3)) {
         var4.addAll(var2.substring(var5[0], var5[1] - var5[0], (var5[2] & 1) != 0));
      }

      return FormattedCharSequence.composite(var4);
   }
}
