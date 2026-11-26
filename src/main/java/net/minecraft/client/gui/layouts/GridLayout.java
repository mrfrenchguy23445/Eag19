package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.Mth;

public class GridLayout extends AbstractLayout {
   private final List<LayoutElement> children = new ArrayList<>();
   private final List<GridLayout.CellInhabitant> cellInhabitants = new ArrayList<>();
   private final LayoutSettings defaultCellSettings = LayoutSettings.defaults();
   private int rowSpacing = 0;
   private int columnSpacing = 0;

   public GridLayout() {
      this(0, 0);
   }

   public GridLayout(int param0, int param1) {
      super(param0, param1, 0, 0);
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int var1 = 0;
      int var2 = 0;

      for (GridLayout.CellInhabitant var4 : this.cellInhabitants) {
         var1 = Math.max(var4.getLastOccupiedRow(), var1);
         var2 = Math.max(var4.getLastOccupiedColumn(), var2);
      }

      int[] var3 = new int[var2 + 1];
      int[] var4 = new int[var1 + 1];

      for (GridLayout.CellInhabitant var6 : this.cellInhabitants) {
         int var7 = var6.getHeight() - (var6.occupiedRows - 1) * this.rowSpacing;
         Divisor var8 = new Divisor(var7, var6.occupiedRows);

         for (int var9 = var6.row; var9 <= var6.getLastOccupiedRow(); var9++) {
            var4[var9] = Math.max(var4[var9], var8.nextInt());
         }

         int var9 = var6.getWidth() - (var6.occupiedColumns - 1) * this.columnSpacing;
         Divisor var10 = new Divisor(var9, var6.occupiedColumns);

         for (int var11 = var6.column; var11 <= var6.getLastOccupiedColumn(); var11++) {
            var3[var11] = Math.max(var3[var11], var10.nextInt());
         }
      }

      int[] var5 = new int[var2 + 1];
      int[] var6 = new int[var1 + 1];
      var5[0] = 0;

      for (int var7 = 1; var7 <= var2; var7++) {
         var5[var7] = var5[var7 - 1] + var3[var7 - 1] + this.columnSpacing;
      }

      var6[0] = 0;

      for (int var7 = 1; var7 <= var1; var7++) {
         var6[var7] = var6[var7 - 1] + var4[var7 - 1] + this.rowSpacing;
      }

      for (GridLayout.CellInhabitant var8 : this.cellInhabitants) {
         int var9 = 0;

         for (int var10 = var8.column; var10 <= var8.getLastOccupiedColumn(); var10++) {
            var9 += var3[var10];
         }

         var9 += this.columnSpacing * (var8.occupiedColumns - 1);
         var8.setX(this.getX() + var5[var8.column], var9);
         int var10 = 0;

         for (int var11 = var8.row; var11 <= var8.getLastOccupiedRow(); var11++) {
            var10 += var4[var11];
         }

         var10 += this.rowSpacing * (var8.occupiedRows - 1);
         var8.setY(this.getY() + var6[var8.row], var10);
      }

      this.width = var5[var2] + var3[var2];
      this.height = var6[var1] + var4[var1];
   }

   public <T extends LayoutElement> T addChild(T param0, int param1, int param2) {
      return this.addChild(param0, param1, param2, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T param0, int param1, int param2, LayoutSettings param3) {
      return this.addChild(param0, param1, param2, 1, 1, param3);
   }

   public <T extends LayoutElement> T addChild(T param0, int param1, int param2, int param3, int param4) {
      return this.addChild(param0, param1, param2, param3, param4, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T param0, int param1, int param2, int param3, int param4, LayoutSettings param5) {
      if (param3 < 1) {
         throw new IllegalArgumentException("Occupied rows must be at least 1");
      }

      if (param4 < 1) {
         throw new IllegalArgumentException("Occupied columns must be at least 1");
      }

      this.cellInhabitants.add(new GridLayout.CellInhabitant(param0, param1, param2, param3, param4, param5));
      this.children.add(param0);
      return param0;
   }

   public GridLayout columnSpacing(int param0) {
      this.columnSpacing = param0;
      return this;
   }

   public GridLayout rowSpacing(int param0) {
      this.rowSpacing = param0;
      return this;
   }

   public GridLayout spacing(int param0) {
      return this.columnSpacing(param0).rowSpacing(param0);
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> param0) {
      this.children.forEach(param0);
   }

   public LayoutSettings newCellSettings() {
      return this.defaultCellSettings.copy();
   }

   public LayoutSettings defaultCellSetting() {
      return this.defaultCellSettings;
   }

   public GridLayout.RowHelper createRowHelper(int param0) {
      return new GridLayout.RowHelper(param0);
   }

   static class CellInhabitant extends AbstractLayout.AbstractChildWrapper {
      final int row;
      final int column;
      final int occupiedRows;
      final int occupiedColumns;

      CellInhabitant(LayoutElement param0, int param1, int param2, int param3, int param4, LayoutSettings param5) {
         super(param0, param5.getExposed());
         this.row = param1;
         this.column = param2;
         this.occupiedRows = param3;
         this.occupiedColumns = param4;
      }

      public int getLastOccupiedRow() {
         return this.row + this.occupiedRows - 1;
      }

      public int getLastOccupiedColumn() {
         return this.column + this.occupiedColumns - 1;
      }
   }

   public final class RowHelper {
      private final int columns;
      private int index;

      RowHelper(int param1) {
         this.columns = param1;
      }

      public <T extends LayoutElement> T addChild(T param0) {
         return this.addChild(param0, 1);
      }

      public <T extends LayoutElement> T addChild(T param0, int param1) {
         return this.addChild(param0, param1, this.defaultCellSetting());
      }

      public <T extends LayoutElement> T addChild(T param0, LayoutSettings param1) {
         return this.addChild(param0, 1, param1);
      }

      public <T extends LayoutElement> T addChild(T param0, int param1, LayoutSettings param2) {
         int var4 = this.index / this.columns;
         int var5 = this.index % this.columns;
         if (var5 + param1 > this.columns) {
            var4++;
            var5 = 0;
            this.index = Mth.roundToward(this.index, this.columns);
         }

         this.index += param1;
         return GridLayout.this.addChild(param0, var4, var5, 1, param1, param2);
      }

      public GridLayout getGrid() {
         return GridLayout.this;
      }

      public LayoutSettings newCellSettings() {
         return GridLayout.this.newCellSettings();
      }

      public LayoutSettings defaultCellSetting() {
         return GridLayout.this.defaultCellSetting();
      }
   }
}
