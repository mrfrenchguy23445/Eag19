package net.minecraft.client.renderer.chunk;

import java.util.BitSet;
import java.util.Set;
import net.minecraft.core.Direction;

public class VisibilitySet {
   private static final int FACINGS = Direction.values().length;
   private final BitSet data = new BitSet(FACINGS * FACINGS);

   public void add(Set<Direction> param0) {
      for (Direction var3 : param0) {
         for (Direction var5 : param0) {
            this.set(var3, var5, true);
         }
      }
   }

   public void set(Direction param0, Direction param1, boolean param2) {
      this.data.set(param0.ordinal() + param1.ordinal() * FACINGS, param2);
      this.data.set(param1.ordinal() + param0.ordinal() * FACINGS, param2);
   }

   public void setAll(boolean param0) {
      this.data.set(0, this.data.size(), param0);
   }

   public boolean visibilityBetween(Direction param0, Direction param1) {
      return this.data.get(param0.ordinal() + param1.ordinal() * FACINGS);
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(' ');

      for (Direction var5 : Direction.values()) {
         var1.append(' ').append(var5.toString().toUpperCase().charAt(0));
      }

      var1.append('\n');

      for (Direction var5 : Direction.values()) {
         var1.append(var5.toString().toUpperCase().charAt(0));

         for (Direction var9 : Direction.values()) {
            if (var5 == var9) {
               var1.append("  ");
            } else {
               boolean var10 = this.visibilityBetween(var5, var9);
               var1.append(' ').append((char)(var10 ? 'Y' : 'n'));
            }
         }

         var1.append('\n');
      }

      return var1.toString();
   }
}
