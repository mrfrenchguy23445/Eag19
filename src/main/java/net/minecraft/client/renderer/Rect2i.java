package net.minecraft.client.renderer;

public class Rect2i {
   private int xPos;
   private int yPos;
   private int width;
   private int height;

   public Rect2i(int param0, int param1, int param2, int param3) {
      this.xPos = param0;
      this.yPos = param1;
      this.width = param2;
      this.height = param3;
   }

   public Rect2i intersect(Rect2i param0) {
      int var2 = this.xPos;
      int var3 = this.yPos;
      int var4 = this.xPos + this.width;
      int var5 = this.yPos + this.height;
      int var6 = param0.getX();
      int var7 = param0.getY();
      int var8 = var6 + param0.getWidth();
      int var9 = var7 + param0.getHeight();
      this.xPos = Math.max(var2, var6);
      this.yPos = Math.max(var3, var7);
      this.width = Math.max(0, Math.min(var4, var8) - this.xPos);
      this.height = Math.max(0, Math.min(var5, var9) - this.yPos);
      return this;
   }

   public int getX() {
      return this.xPos;
   }

   public int getY() {
      return this.yPos;
   }

   public void setX(int param0) {
      this.xPos = param0;
   }

   public void setY(int param0) {
      this.yPos = param0;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setWidth(int param0) {
      this.width = param0;
   }

   public void setHeight(int param0) {
      this.height = param0;
   }

   public void setPosition(int param0, int param1) {
      this.xPos = param0;
      this.yPos = param1;
   }

   public boolean contains(int param0, int param1) {
      return param0 >= this.xPos && param0 <= this.xPos + this.width && param1 >= this.yPos && param1 <= this.yPos + this.height;
   }
}
