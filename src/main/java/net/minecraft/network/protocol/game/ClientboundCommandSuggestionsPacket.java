package net.minecraft.network.protocol.game;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.Packet;

public class ClientboundCommandSuggestionsPacket implements Packet<ClientGamePacketListener> {
   private final int id;
   private final Suggestions suggestions;

   public ClientboundCommandSuggestionsPacket(int param0, Suggestions param1) {
      this.id = param0;
      this.suggestions = param1;
   }

   public ClientboundCommandSuggestionsPacket(FriendlyByteBuf param0) {
      this.id = param0.readVarInt();
      int var2 = param0.readVarInt();
      int var3 = param0.readVarInt();
      StringRange var4 = StringRange.between(var2, var2 + var3);
      List<Suggestion> var5 = param0.readList(l_param1 -> {
         String l_var2 = l_param1.readUtf();
         Component l_var3 = l_param1.readNullable(FriendlyByteBuf::readComponent);
         return new Suggestion(var4, l_var2, l_var3);
      });
      this.suggestions = new Suggestions(var4, var5);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.id);
      param0.writeVarInt(this.suggestions.getRange().getStart());
      param0.writeVarInt(this.suggestions.getRange().getLength());
      param0.writeCollection(this.suggestions.getList(), (l_param0, l_param1) -> {
         l_param0.writeUtf(l_param1.getText());
         l_param0.writeNullable(l_param1.getTooltip(), (l2_param0, l2_param1) -> l2_param0.writeComponent(ComponentUtils.fromMessage(l2_param1)));
      });
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleCommandSuggestions(this);
   }

   public int getId() {
      return this.id;
   }

   public Suggestions getSuggestions() {
      return this.suggestions;
   }
}
