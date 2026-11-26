package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public record ChatTypeDecoration(String translationKey, List<ChatTypeDecoration.Parameter> parameters, Style style) {
   public static final Codec<ChatTypeDecoration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.STRING.fieldOf("translation_key").forGetter(ChatTypeDecoration::translationKey),
            ChatTypeDecoration.Parameter.CODEC.listOf().fieldOf("parameters").forGetter(ChatTypeDecoration::parameters),
            Style.FORMATTING_CODEC.optionalFieldOf("style", Style.EMPTY).forGetter(ChatTypeDecoration::style)
         )
         .apply(param0, ChatTypeDecoration::new)
   );

   public static ChatTypeDecoration withSender(String param0) {
      return new ChatTypeDecoration(param0, List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY);
   }

   public static ChatTypeDecoration incomingDirectMessage(String param0) {
      Style var1 = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
      return new ChatTypeDecoration(param0, List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), var1);
   }

   public static ChatTypeDecoration outgoingDirectMessage(String param0) {
      Style var1 = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
      return new ChatTypeDecoration(param0, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT), var1);
   }

   public static ChatTypeDecoration teamMessage(String param0) {
      return new ChatTypeDecoration(
         param0, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY
      );
   }

   public Component decorate(Component param0, ChatType.Bound param1) {
      Object[] var3 = this.resolveParameters(param0, param1);
      return Component.translatable(this.translationKey, var3).withStyle(this.style);
   }

   private Component[] resolveParameters(Component param0, ChatType.Bound param1) {
      Component[] var3 = new Component[this.parameters.size()];

      for (int var4 = 0; var4 < var3.length; var4++) {
         ChatTypeDecoration.Parameter var5 = this.parameters.get(var4);
         var3[var4] = var5.select(param0, param1);
      }

      return var3;
   }

   public enum Parameter implements StringRepresentable {
      SENDER("sender", (param0, param1) -> param1.name()),
      TARGET("target", (param0, param1) -> param1.targetName()),
      CONTENT("content", (param0, param1) -> param0);

      public static final Codec<ChatTypeDecoration.Parameter> CODEC = StringRepresentable.fromEnum(ChatTypeDecoration.Parameter::values);
      private final String name;
      private final ChatTypeDecoration.Parameter.Selector selector;

      Parameter(String param2, ChatTypeDecoration.Parameter.Selector param3) {
         this.name = param2;
         this.selector = param3;
      }

      public Component select(Component param0, ChatType.Bound param1) {
         Component var3 = this.selector.select(param0, param1);
         return Objects.requireNonNullElse(var3, CommonComponents.EMPTY);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public interface Selector {
         @Nullable
         Component select(Component var1, ChatType.Bound var2);
      }
   }
}
