package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
   String ROOT = "root";

   void startTick();

   void endTick();

   void push(String var1);

   void push(Supplier<String> var1);

   void pop();

   void popPush(String var1);

   void popPush(Supplier<String> var1);

   void markForCharting(MetricCategory var1);

   default void incrementCounter(String param0) {
      this.incrementCounter(param0, 1);
   }

   void incrementCounter(String var1, int var2);

   default void incrementCounter(Supplier<String> param0) {
      this.incrementCounter(param0, 1);
   }

   void incrementCounter(Supplier<String> var1, int var2);

   static ProfilerFiller tee(final ProfilerFiller param0, final ProfilerFiller param1) {
      if (param0 == InactiveProfiler.INSTANCE) {
         return param1;
      } else {
         return param1 == InactiveProfiler.INSTANCE ? param0 : new ProfilerFiller() {
            @Override
            public void startTick() {
               param0.startTick();
               param1.startTick();
            }

            @Override
            public void endTick() {
               param0.endTick();
               param1.endTick();
            }

            @Override
            public void push(String l_param0) {
               param0.push(l_param0);
               param1.push(l_param0);
            }

            @Override
            public void push(Supplier<String> l_param0) {
               param0.push(l_param0);
               param1.push(l_param0);
            }

            @Override
            public void markForCharting(MetricCategory l_param0) {
               param0.markForCharting(l_param0);
               param1.markForCharting(l_param0);
            }

            @Override
            public void pop() {
               param0.pop();
               param1.pop();
            }

            @Override
            public void popPush(String l_param0) {
               param0.popPush(l_param0);
               param1.popPush(l_param0);
            }

            @Override
            public void popPush(Supplier<String> l_param0) {
               param0.popPush(l_param0);
               param1.popPush(l_param0);
            }

            @Override
            public void incrementCounter(String l_param0, int l_param1) {
               param0.incrementCounter(l_param0, l_param1);
               param1.incrementCounter(l_param0, l_param1);
            }

            @Override
            public void incrementCounter(Supplier<String> l_param0, int l_param1) {
               param0.incrementCounter(l_param0, l_param1);
               param1.incrementCounter(l_param0, l_param1);
            }
         };
      }
   }
}
