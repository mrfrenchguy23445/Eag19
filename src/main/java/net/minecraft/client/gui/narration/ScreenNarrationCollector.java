package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

public class ScreenNarrationCollector {
   int generation;
   final Map<ScreenNarrationCollector.EntryKey, ScreenNarrationCollector.NarrationEntry> entries = Maps.newTreeMap(
      Comparator.<ScreenNarrationCollector.EntryKey, NarratedElementType>comparing(l_param0 -> l_param0.type).thenComparing(l_param0 -> l_param0.depth)
   );

   public void update(Consumer<NarrationElementOutput> param0) {
      this.generation++;
      param0.accept(new ScreenNarrationCollector.Output(0));
   }

   public String collectNarrationText(boolean param0) {
      final StringBuilder var2 = new StringBuilder();
      Consumer<String> var3 = new Consumer<String>() {
         private boolean firstEntry = true;

         public void accept(String l_param0) {
            if (!this.firstEntry) {
               var2.append(". ");
            }

            this.firstEntry = false;
            var2.append(l_param0);
         }
      };
      this.entries.forEach((l_param2, l_param3) -> {
         if (l_param3.generation == this.generation && (param0 || !l_param3.alreadyNarrated)) {
            l_param3.contents.getText(var3);
            l_param3.alreadyNarrated = true;
         }
      });
      return var2.toString();
   }

   static class EntryKey {
      final NarratedElementType type;
      final int depth;

      EntryKey(NarratedElementType param0, int param1) {
         this.type = param0;
         this.depth = param1;
      }
   }

   static class NarrationEntry {
      NarrationThunk<?> contents = NarrationThunk.EMPTY;
      int generation = -1;
      boolean alreadyNarrated;

      public ScreenNarrationCollector.NarrationEntry update(int param0, NarrationThunk<?> param1) {
         if (!this.contents.equals(param1)) {
            this.contents = param1;
            this.alreadyNarrated = false;
         } else if (this.generation + 1 != param0) {
            this.alreadyNarrated = false;
         }

         this.generation = param0;
         return this;
      }
   }

   class Output implements NarrationElementOutput {
      private final int depth;

      Output(int param1) {
         this.depth = param1;
      }

      @Override
      public void add(NarratedElementType param0, NarrationThunk<?> param1) {
         ScreenNarrationCollector.this.entries
            .computeIfAbsent(new ScreenNarrationCollector.EntryKey(param0, this.depth), l_param0 -> new ScreenNarrationCollector.NarrationEntry())
            .update(ScreenNarrationCollector.this.generation, param1);
      }

      @Override
      public NarrationElementOutput nest() {
         return ScreenNarrationCollector.this.new Output(this.depth + 1);
      }
   }
}
