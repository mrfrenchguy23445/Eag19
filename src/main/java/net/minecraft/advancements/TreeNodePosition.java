package net.minecraft.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;

public class TreeNodePosition {
   private final Advancement advancement;
   @Nullable
   private final TreeNodePosition parent;
   @Nullable
   private final TreeNodePosition previousSibling;
   private final int childIndex;
   private final List<TreeNodePosition> children = Lists.newArrayList();
   private TreeNodePosition ancestor;
   @Nullable
   private TreeNodePosition thread;
   private int x;
   private float y;
   private float mod;
   private float change;
   private float shift;

   public TreeNodePosition(Advancement param0, @Nullable TreeNodePosition param1, @Nullable TreeNodePosition param2, int param3, int param4) {
      if (param0.getDisplay() == null) {
         throw new IllegalArgumentException("Can't position an invisible advancement!");
      }

      this.advancement = param0;
      this.parent = param1;
      this.previousSibling = param2;
      this.childIndex = param3;
      this.ancestor = this;
      this.x = param4;
      this.y = -1.0F;
      TreeNodePosition var6 = null;

      for (Advancement var8 : param0.getChildren()) {
         var6 = this.addChild(var8, var6);
      }
   }

   @Nullable
   private TreeNodePosition addChild(Advancement param0, @Nullable TreeNodePosition param1) {
      if (param0.getDisplay() != null) {
         param1 = new TreeNodePosition(param0, this, param1, this.children.size() + 1, this.x + 1);
         this.children.add(param1);
      } else {
         for (Advancement var4 : param0.getChildren()) {
            param1 = this.addChild(var4, param1);
         }
      }

      return param1;
   }

   private void firstWalk() {
      if (this.children.isEmpty()) {
         if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0F;
         } else {
            this.y = 0.0F;
         }
      } else {
         TreeNodePosition var1 = null;

         for (TreeNodePosition var3 : this.children) {
            var3.firstWalk();
            var1 = var3.apportion(var1 == null ? var3 : var1);
         }

         this.executeShifts();
         float var2 = (this.children.get(0).y + this.children.get(this.children.size() - 1).y) / 2.0F;
         if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0F;
            this.mod = this.y - var2;
         } else {
            this.y = var2;
         }
      }
   }

   private float secondWalk(float param0, int param1, float param2) {
      this.y += param0;
      this.x = param1;
      if (this.y < param2) {
         param2 = this.y;
      }

      for (TreeNodePosition var5 : this.children) {
         param2 = var5.secondWalk(param0 + this.mod, param1 + 1, param2);
      }

      return param2;
   }

   private void thirdWalk(float param0) {
      this.y += param0;

      for (TreeNodePosition var3 : this.children) {
         var3.thirdWalk(param0);
      }
   }

   private void executeShifts() {
      float var1 = 0.0F;
      float var2 = 0.0F;

      for (int var3 = this.children.size() - 1; var3 >= 0; var3--) {
         TreeNodePosition var4 = this.children.get(var3);
         var4.y += var1;
         var4.mod += var1;
         var2 += var4.change;
         var1 += var4.shift + var2;
      }
   }

   @Nullable
   private TreeNodePosition previousOrThread() {
      if (this.thread != null) {
         return this.thread;
      } else {
         return !this.children.isEmpty() ? this.children.get(0) : null;
      }
   }

   @Nullable
   private TreeNodePosition nextOrThread() {
      if (this.thread != null) {
         return this.thread;
      } else {
         return !this.children.isEmpty() ? this.children.get(this.children.size() - 1) : null;
      }
   }

   private TreeNodePosition apportion(TreeNodePosition param0) {
      if (this.previousSibling == null) {
         return param0;
      }

      TreeNodePosition var2 = this;
      TreeNodePosition var3 = this;
      TreeNodePosition var4 = this.previousSibling;
      TreeNodePosition var5 = this.parent.children.get(0);
      float var6 = this.mod;
      float var7 = this.mod;
      float var8 = var4.mod;
      float var9 = var5.mod;

      while (var4.nextOrThread() != null && var2.previousOrThread() != null) {
         var4 = var4.nextOrThread();
         var2 = var2.previousOrThread();
         var5 = var5.previousOrThread();
         var3 = var3.nextOrThread();
         var3.ancestor = this;
         float var10 = var4.y + var8 - (var2.y + var6) + 1.0F;
         if (var10 > 0.0F) {
            var4.getAncestor(this, param0).moveSubtree(this, var10);
            var6 += var10;
            var7 += var10;
         }

         var8 += var4.mod;
         var6 += var2.mod;
         var9 += var5.mod;
         var7 += var3.mod;
      }

      if (var4.nextOrThread() != null && var3.nextOrThread() == null) {
         var3.thread = var4.nextOrThread();
         var3.mod += var8 - var7;
      } else {
         if (var2.previousOrThread() != null && var5.previousOrThread() == null) {
            var5.thread = var2.previousOrThread();
            var5.mod += var6 - var9;
         }

         param0 = this;
      }

      return param0;
   }

   private void moveSubtree(TreeNodePosition param0, float param1) {
      float var3 = param0.childIndex - this.childIndex;
      if (var3 != 0.0F) {
         param0.change -= param1 / var3;
         this.change += param1 / var3;
      }

      param0.shift += param1;
      param0.y += param1;
      param0.mod += param1;
   }

   private TreeNodePosition getAncestor(TreeNodePosition param0, TreeNodePosition param1) {
      return this.ancestor != null && param0.parent.children.contains(this.ancestor) ? this.ancestor : param1;
   }

   private void finalizePosition() {
      if (this.advancement.getDisplay() != null) {
         this.advancement.getDisplay().setLocation(this.x, this.y);
      }

      if (!this.children.isEmpty()) {
         for (TreeNodePosition var2 : this.children) {
            var2.finalizePosition();
         }
      }
   }

   public static void run(Advancement param0) {
      if (param0.getDisplay() == null) {
         throw new IllegalArgumentException("Can't position children of an invisible root!");
      }

      TreeNodePosition var1 = new TreeNodePosition(param0, null, null, 1, 0);
      var1.firstWalk();
      float var2 = var1.secondWalk(0.0F, 0, var1.y);
      if (var2 < 0.0F) {
         var1.thirdWalk(-var2);
      }

      var1.finalizePosition();
   }
}
