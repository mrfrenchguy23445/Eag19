package net.minecraft.advancements;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum FrameType {
   TASK("task", 0, ChatFormatting.GREEN),
   CHALLENGE("challenge", 26, ChatFormatting.DARK_PURPLE),
   GOAL("goal", 52, ChatFormatting.GREEN);

   private final String name;
   private final int texture;
   private final ChatFormatting chatColor;
   private final Component displayName;

   FrameType(String param2, int param3, ChatFormatting param4) {
      this.name = param2;
      this.texture = param3;
      this.chatColor = param4;
      this.displayName = Component.translatable("advancements.toast." + param2);
   }

   public String getName() {
      return this.name;
   }

   public int getTexture() {
      return this.texture;
   }

   public static FrameType byName(String param0) {
      for (FrameType var4 : values()) {
         if (var4.name.equals(param0)) {
            return var4;
         }
      }

      throw new IllegalArgumentException("Unknown frame type '" + param0 + "'");
   }

   public ChatFormatting getChatColor() {
      return this.chatColor;
   }

   public Component getDisplayName() {
      return this.displayName;
   }
}
