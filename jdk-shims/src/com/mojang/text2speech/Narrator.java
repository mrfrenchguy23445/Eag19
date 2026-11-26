package com.mojang.text2speech;
public interface Narrator {
   Narrator EMPTY = new Narrator() {
      @Override
      public void say(String param0, boolean param1) {
      }

      @Override
      public void clear() {
      }

      @Override
      public boolean active() {
         return false;
      }

      @Override
      public void destroy() {
      }
   };

   void say(String param0, boolean param1);

   void clear();

   default boolean active() {
      return false;
   }

   void destroy();
   static Narrator getNarrator() {
      return EMPTY;
   }
}
