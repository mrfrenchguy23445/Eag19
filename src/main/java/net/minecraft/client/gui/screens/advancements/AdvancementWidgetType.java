package net.minecraft.client.gui.screens.advancements;

public enum AdvancementWidgetType {
   OBTAINED(0),
   UNOBTAINED(1);

   private final int y;

   AdvancementWidgetType(int param2) {
      this.y = param2;
   }

   public int getIndex() {
      return this.y;
   }
}
