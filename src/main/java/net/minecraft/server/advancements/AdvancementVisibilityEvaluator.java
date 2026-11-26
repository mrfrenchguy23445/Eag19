package net.minecraft.server.advancements;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;

public class AdvancementVisibilityEvaluator {
   private static final int VISIBILITY_DEPTH = 2;

   private static AdvancementVisibilityEvaluator.VisibilityRule evaluateVisibilityRule(Advancement param0, boolean param1) {
      DisplayInfo var2 = param0.getDisplay();
      if (var2 == null) {
         return AdvancementVisibilityEvaluator.VisibilityRule.HIDE;
      } else if (param1) {
         return AdvancementVisibilityEvaluator.VisibilityRule.SHOW;
      } else {
         return var2.isHidden() ? AdvancementVisibilityEvaluator.VisibilityRule.HIDE : AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE;
      }
   }

   private static boolean evaluateVisiblityForUnfinishedNode(Stack<AdvancementVisibilityEvaluator.VisibilityRule> param0) {
      for (int var1 = 0; var1 <= 2; var1++) {
         AdvancementVisibilityEvaluator.VisibilityRule var2 = param0.peek(var1);
         if (var2 == AdvancementVisibilityEvaluator.VisibilityRule.SHOW) {
            return true;
         }

         if (var2 == AdvancementVisibilityEvaluator.VisibilityRule.HIDE) {
            return false;
         }
      }

      return false;
   }

   private static boolean evaluateVisibility(
      Advancement param0,
      Stack<AdvancementVisibilityEvaluator.VisibilityRule> param1,
      Predicate<Advancement> param2,
      AdvancementVisibilityEvaluator.Output param3
   ) {
      boolean var4 = param2.test(param0);
      AdvancementVisibilityEvaluator.VisibilityRule var5 = evaluateVisibilityRule(param0, var4);
      boolean var6 = var4;
      param1.push(var5);

      for (Advancement var8 : param0.getChildren()) {
         var6 |= evaluateVisibility(var8, param1, param2, param3);
      }

      boolean var7 = var6 || evaluateVisiblityForUnfinishedNode(param1);
      param1.pop();
      param3.accept(param0, var7);
      return var6;
   }

   public static void evaluateVisibility(Advancement param0, Predicate<Advancement> param1, AdvancementVisibilityEvaluator.Output param2) {
      Advancement var3 = param0.getRoot();
      Stack<AdvancementVisibilityEvaluator.VisibilityRule> var4 = new ObjectArrayList<>();

      for (int var5 = 0; var5 <= 2; var5++) {
         var4.push(AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE);
      }

      evaluateVisibility(var3, var4, param1, param2);
   }

   @FunctionalInterface
   public interface Output {
      void accept(Advancement var1, boolean var2);
   }

   enum VisibilityRule {
      SHOW,
      HIDE,
      NO_CHANGE;
   }
}
