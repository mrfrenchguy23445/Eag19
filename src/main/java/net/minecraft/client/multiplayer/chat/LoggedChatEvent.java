package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;

public interface LoggedChatEvent {
   Codec<LoggedChatEvent> CODEC = StringRepresentable.fromEnum(LoggedChatEvent.Type::values).dispatch(LoggedChatEvent::type, LoggedChatEvent.Type::codec);

   LoggedChatEvent.Type type();

   enum Type implements StringRepresentable {
      PLAYER("player", () -> LoggedChatMessage.Player.CODEC),
      SYSTEM("system", () -> LoggedChatMessage.System.CODEC);

      private final String serializedName;
      private final Supplier<Codec<? extends LoggedChatEvent>> codec;

      Type(String param2, Supplier<Codec<? extends LoggedChatEvent>> param3) {
         this.serializedName = param2;
         this.codec = param3;
      }

      private Codec<? extends LoggedChatEvent> codec() {
         return this.codec.get();
      }

      @Override
      public String getSerializedName() {
         return this.serializedName;
      }
   }
}
