package net.minecraft.network.chat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Style {
   public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null);
   public static final Codec<Style> FORMATTING_CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            TextColor.CODEC.optionalFieldOf("color").forGetter(l_param0 -> Optional.ofNullable(l_param0.color)),
            Codec.BOOL.optionalFieldOf("bold").forGetter(l_param0 -> Optional.ofNullable(l_param0.bold)),
            Codec.BOOL.optionalFieldOf("italic").forGetter(l_param0 -> Optional.ofNullable(l_param0.italic)),
            Codec.BOOL.optionalFieldOf("underlined").forGetter(l_param0 -> Optional.ofNullable(l_param0.underlined)),
            Codec.BOOL.optionalFieldOf("strikethrough").forGetter(l_param0 -> Optional.ofNullable(l_param0.strikethrough)),
            Codec.BOOL.optionalFieldOf("obfuscated").forGetter(l_param0 -> Optional.ofNullable(l_param0.obfuscated)),
            Codec.STRING.optionalFieldOf("insertion").forGetter(l_param0 -> Optional.ofNullable(l_param0.insertion)),
            ResourceLocation.CODEC.optionalFieldOf("font").forGetter(l_param0 -> Optional.ofNullable(l_param0.font))
         )
         .apply(param0, Style::create)
   );
   public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
   @Nullable
   final TextColor color;
   @Nullable
   final Boolean bold;
   @Nullable
   final Boolean italic;
   @Nullable
   final Boolean underlined;
   @Nullable
   final Boolean strikethrough;
   @Nullable
   final Boolean obfuscated;
   @Nullable
   final ClickEvent clickEvent;
   @Nullable
   final HoverEvent hoverEvent;
   @Nullable
   final String insertion;
   @Nullable
   final ResourceLocation font;

   private static Style create(
      Optional<TextColor> param0,
      Optional<Boolean> param1,
      Optional<Boolean> param2,
      Optional<Boolean> param3,
      Optional<Boolean> param4,
      Optional<Boolean> param5,
      Optional<String> param6,
      Optional<ResourceLocation> param7
   ) {
      return new Style(
         param0.orElse(null),
         param1.orElse(null),
         param2.orElse(null),
         param3.orElse(null),
         param4.orElse(null),
         param5.orElse(null),
         null,
         null,
         param6.orElse(null),
         param7.orElse(null)
      );
   }

   Style(
      @Nullable TextColor param0,
      @Nullable Boolean param1,
      @Nullable Boolean param2,
      @Nullable Boolean param3,
      @Nullable Boolean param4,
      @Nullable Boolean param5,
      @Nullable ClickEvent param6,
      @Nullable HoverEvent param7,
      @Nullable String param8,
      @Nullable ResourceLocation param9
   ) {
      this.color = param0;
      this.bold = param1;
      this.italic = param2;
      this.underlined = param3;
      this.strikethrough = param4;
      this.obfuscated = param5;
      this.clickEvent = param6;
      this.hoverEvent = param7;
      this.insertion = param8;
      this.font = param9;
   }

   @Nullable
   public TextColor getColor() {
      return this.color;
   }

   public boolean isBold() {
      return this.bold == Boolean.TRUE;
   }

   public boolean isItalic() {
      return this.italic == Boolean.TRUE;
   }

   public boolean isStrikethrough() {
      return this.strikethrough == Boolean.TRUE;
   }

   public boolean isUnderlined() {
      return this.underlined == Boolean.TRUE;
   }

   public boolean isObfuscated() {
      return this.obfuscated == Boolean.TRUE;
   }

   public boolean isEmpty() {
      return this == EMPTY;
   }

   @Nullable
   public ClickEvent getClickEvent() {
      return this.clickEvent;
   }

   @Nullable
   public HoverEvent getHoverEvent() {
      return this.hoverEvent;
   }

   @Nullable
   public String getInsertion() {
      return this.insertion;
   }

   public ResourceLocation getFont() {
      return this.font != null ? this.font : DEFAULT_FONT;
   }

   public Style withColor(@Nullable TextColor param0) {
      return new Style(
         param0, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withColor(@Nullable ChatFormatting param0) {
      return this.withColor(param0 != null ? TextColor.fromLegacyFormat(param0) : null);
   }

   public Style withColor(int param0) {
      return this.withColor(TextColor.fromRgb(param0));
   }

   public Style withBold(@Nullable Boolean param0) {
      return new Style(
         this.color, param0, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withItalic(@Nullable Boolean param0) {
      return new Style(
         this.color, this.bold, param0, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withUnderlined(@Nullable Boolean param0) {
      return new Style(
         this.color, this.bold, this.italic, param0, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withStrikethrough(@Nullable Boolean param0) {
      return new Style(
         this.color, this.bold, this.italic, this.underlined, param0, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withObfuscated(@Nullable Boolean param0) {
      return new Style(
         this.color, this.bold, this.italic, this.underlined, this.strikethrough, param0, this.clickEvent, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withClickEvent(@Nullable ClickEvent param0) {
      return new Style(
         this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, param0, this.hoverEvent, this.insertion, this.font
      );
   }

   public Style withHoverEvent(@Nullable HoverEvent param0) {
      return new Style(
         this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, param0, this.insertion, this.font
      );
   }

   public Style withInsertion(@Nullable String param0) {
      return new Style(
         this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, param0, this.font
      );
   }

   public Style withFont(@Nullable ResourceLocation param0) {
      return new Style(
         this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, param0
      );
   }

   public Style applyFormat(ChatFormatting param0) {
      TextColor var2 = this.color;
      Boolean var3 = this.bold;
      Boolean var4 = this.italic;
      Boolean var5 = this.strikethrough;
      Boolean var6 = this.underlined;
      Boolean var7 = this.obfuscated;
      switch (param0) {
         case OBFUSCATED:
            var7 = true;
            break;
         case BOLD:
            var3 = true;
            break;
         case STRIKETHROUGH:
            var5 = true;
            break;
         case UNDERLINE:
            var6 = true;
            break;
         case ITALIC:
            var4 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            var2 = TextColor.fromLegacyFormat(param0);
      }

      return new Style(var2, var3, var4, var6, var5, var7, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyLegacyFormat(ChatFormatting param0) {
      TextColor var2 = this.color;
      Boolean var3 = this.bold;
      Boolean var4 = this.italic;
      Boolean var5 = this.strikethrough;
      Boolean var6 = this.underlined;
      Boolean var7 = this.obfuscated;
      switch (param0) {
         case OBFUSCATED:
            var7 = true;
            break;
         case BOLD:
            var3 = true;
            break;
         case STRIKETHROUGH:
            var5 = true;
            break;
         case UNDERLINE:
            var6 = true;
            break;
         case ITALIC:
            var4 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            var7 = false;
            var3 = false;
            var5 = false;
            var6 = false;
            var4 = false;
            var2 = TextColor.fromLegacyFormat(param0);
      }

      return new Style(var2, var3, var4, var6, var5, var7, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyFormats(ChatFormatting... param0) {
      TextColor var2 = this.color;
      Boolean var3 = this.bold;
      Boolean var4 = this.italic;
      Boolean var5 = this.strikethrough;
      Boolean var6 = this.underlined;
      Boolean var7 = this.obfuscated;

      for (ChatFormatting var11 : param0) {
         switch (var11) {
            case OBFUSCATED:
               var7 = true;
               break;
            case BOLD:
               var3 = true;
               break;
            case STRIKETHROUGH:
               var5 = true;
               break;
            case UNDERLINE:
               var6 = true;
               break;
            case ITALIC:
               var4 = true;
               break;
            case RESET:
               return EMPTY;
            default:
               var2 = TextColor.fromLegacyFormat(var11);
         }
      }

      return new Style(var2, var3, var4, var6, var5, var7, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyTo(Style param0) {
      if (this == EMPTY) {
         return param0;
      } else {
         return param0 == EMPTY
            ? this
            : new Style(
               this.color != null ? this.color : param0.color,
               this.bold != null ? this.bold : param0.bold,
               this.italic != null ? this.italic : param0.italic,
               this.underlined != null ? this.underlined : param0.underlined,
               this.strikethrough != null ? this.strikethrough : param0.strikethrough,
               this.obfuscated != null ? this.obfuscated : param0.obfuscated,
               this.clickEvent != null ? this.clickEvent : param0.clickEvent,
               this.hoverEvent != null ? this.hoverEvent : param0.hoverEvent,
               this.insertion != null ? this.insertion : param0.insertion,
               this.font != null ? this.font : param0.font
            );
      }
   }

   @Override
   public String toString() {
      final StringBuilder var1 = new StringBuilder("{");

      class Collector {
         private boolean isNotFirst;

         private void prependSeparator() {
            if (this.isNotFirst) {
               var1.append(',');
            }

            this.isNotFirst = true;
         }

         void addFlagString(String l_param0, @Nullable Boolean l_param1) {
            if (l_param1 != null) {
               this.prependSeparator();
               if (!l_param1) {
                  var1.append('!');
               }

               var1.append(l_param0);
            }
         }

         void addValueString(String l_param0, @Nullable Object l_param1) {
            if (l_param1 != null) {
               this.prependSeparator();
               var1.append(l_param0);
               var1.append('=');
               var1.append(l_param1);
            }
         }
      }

      Collector var2 = new Collector();
      var2.addValueString("color", this.color);
      var2.addFlagString("bold", this.bold);
      var2.addFlagString("italic", this.italic);
      var2.addFlagString("underlined", this.underlined);
      var2.addFlagString("strikethrough", this.strikethrough);
      var2.addFlagString("obfuscated", this.obfuscated);
      var2.addValueString("clickEvent", this.clickEvent);
      var2.addValueString("hoverEvent", this.hoverEvent);
      var2.addValueString("insertion", this.insertion);
      var2.addValueString("font", this.font);
      var1.append("}");
      return var1.toString();
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof Style var2)
            ? false
            : this.isBold() == var2.isBold()
               && Objects.equals(this.getColor(), var2.getColor())
               && this.isItalic() == var2.isItalic()
               && this.isObfuscated() == var2.isObfuscated()
               && this.isStrikethrough() == var2.isStrikethrough()
               && this.isUnderlined() == var2.isUnderlined()
               && Objects.equals(this.getClickEvent(), var2.getClickEvent())
               && Objects.equals(this.getHoverEvent(), var2.getHoverEvent())
               && Objects.equals(this.getInsertion(), var2.getInsertion())
               && Objects.equals(this.getFont(), var2.getFont());
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(
         this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion
      );
   }

   public static class Serializer implements JsonDeserializer<Style>, JsonSerializer<Style> {
      @Nullable
      public Style deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         if (param0.isJsonObject()) {
            JsonObject var4 = param0.getAsJsonObject();
            if (var4 == null) {
               return null;
            }

            Boolean var5 = getOptionalFlag(var4, "bold");
            Boolean var6 = getOptionalFlag(var4, "italic");
            Boolean var7 = getOptionalFlag(var4, "underlined");
            Boolean var8 = getOptionalFlag(var4, "strikethrough");
            Boolean var9 = getOptionalFlag(var4, "obfuscated");
            TextColor var10 = getTextColor(var4);
            String var11 = getInsertion(var4);
            ClickEvent var12 = getClickEvent(var4);
            HoverEvent var13 = getHoverEvent(var4);
            ResourceLocation var14 = getFont(var4);
            return new Style(var10, var5, var6, var7, var8, var9, var12, var13, var11, var14);
         } else {
            return null;
         }
      }

      @Nullable
      private static ResourceLocation getFont(JsonObject param0) {
         if (param0.has("font")) {
            String var1 = GsonHelper.getAsString(param0, "font");

            try {
               return new ResourceLocation(var1);
            } catch (ResourceLocationException var2) {
               throw new JsonSyntaxException("Invalid font name: " + var1);
            }
         } else {
            return null;
         }
      }

      @Nullable
      private static HoverEvent getHoverEvent(JsonObject param0) {
         if (param0.has("hoverEvent")) {
            JsonObject var1 = GsonHelper.getAsJsonObject(param0, "hoverEvent");
            HoverEvent var2 = HoverEvent.deserialize(var1);
            if (var2 != null && var2.getAction().isAllowedFromServer()) {
               return var2;
            }
         }

         return null;
      }

      @Nullable
      private static ClickEvent getClickEvent(JsonObject param0) {
         if (param0.has("clickEvent")) {
            JsonObject var1 = GsonHelper.getAsJsonObject(param0, "clickEvent");
            String var2 = GsonHelper.getAsString(var1, "action", null);
            ClickEvent.Action var3 = var2 == null ? null : ClickEvent.Action.getByName(var2);
            String var4 = GsonHelper.getAsString(var1, "value", null);
            if (var3 != null && var4 != null && var3.isAllowedFromServer()) {
               return new ClickEvent(var3, var4);
            }
         }

         return null;
      }

      @Nullable
      private static String getInsertion(JsonObject param0) {
         return GsonHelper.getAsString(param0, "insertion", null);
      }

      @Nullable
      private static TextColor getTextColor(JsonObject param0) {
         if (param0.has("color")) {
            String var1 = GsonHelper.getAsString(param0, "color");
            return TextColor.parseColor(var1);
         } else {
            return null;
         }
      }

      @Nullable
      private static Boolean getOptionalFlag(JsonObject param0, String param1) {
         return param0.has(param1) ? param0.get(param1).getAsBoolean() : null;
      }

      @Nullable
      public JsonElement serialize(Style param0, Type param1, JsonSerializationContext param2) {
         if (param0.isEmpty()) {
            return null;
         }

         JsonObject var4 = new JsonObject();
         if (param0.bold != null) {
            var4.addProperty("bold", param0.bold);
         }

         if (param0.italic != null) {
            var4.addProperty("italic", param0.italic);
         }

         if (param0.underlined != null) {
            var4.addProperty("underlined", param0.underlined);
         }

         if (param0.strikethrough != null) {
            var4.addProperty("strikethrough", param0.strikethrough);
         }

         if (param0.obfuscated != null) {
            var4.addProperty("obfuscated", param0.obfuscated);
         }

         if (param0.color != null) {
            var4.addProperty("color", param0.color.serialize());
         }

         if (param0.insertion != null) {
            var4.add("insertion", param2.serialize(param0.insertion));
         }

         if (param0.clickEvent != null) {
            JsonObject var5 = new JsonObject();
            var5.addProperty("action", param0.clickEvent.getAction().getName());
            var5.addProperty("value", param0.clickEvent.getValue());
            var4.add("clickEvent", var5);
         }

         if (param0.hoverEvent != null) {
            var4.add("hoverEvent", param0.hoverEvent.serialize());
         }

         if (param0.font != null) {
            var4.addProperty("font", param0.font.toString());
         }

         return var4;
      }
   }
}
