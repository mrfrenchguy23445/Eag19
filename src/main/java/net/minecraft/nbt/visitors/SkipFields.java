package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class SkipFields extends CollectToTag {
   private final Deque<FieldTree> stack = new ArrayDeque<>();

   public SkipFields(FieldSelector... param0) {
      FieldTree var2 = FieldTree.createRoot();

      for (FieldSelector var6 : param0) {
         var2.addEntry(var6);
      }

      this.stack.push(var2);
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> param0, String param1) {
      FieldTree var3 = this.stack.element();
      if (var3.isSelected(param0, param1)) {
         return StreamTagVisitor.EntryResult.SKIP;
      }

      if (param0 == CompoundTag.TYPE) {
         FieldTree var4 = var3.fieldsToRecurse().get(param1);
         if (var4 != null) {
            this.stack.push(var4);
         }
      }

      return super.visitEntry(param0, param1);
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      if (this.depth() == this.stack.element().depth()) {
         this.stack.pop();
      }

      return super.visitContainerEnd();
   }
}
