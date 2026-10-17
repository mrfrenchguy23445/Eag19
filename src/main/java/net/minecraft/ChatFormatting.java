package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.StringRepresentable;

public enum ChatFormatting implements StringRepresentable {
   BLACK("BLACK", '0', 0, 0),
   DARK_BLUE("DARK_BLUE", '1', 1, 170),
   DARK_GREEN("DARK_GREEN", '2', 2, 43520),
   DARK_AQUA("DARK_AQUA", '3', 3, 43690),
   DARK_RED("DARK_RED", '4', 4, 11141120),
   DARK_PURPLE("DARK_PURPLE", '5', 5, 11141290),
   GOLD("GOLD", '6', 6, 16755200),
   GRAY("GRAY", '7', 7, 11184810),
   DARK_GRAY("DARK_GRAY", '8', 8, 5592405),
   BLUE("BLUE", '9', 9, 5592575),
   GREEN("GREEN", 'a', 10, 5635925),
   AQUA("AQUA", 'b', 11, 5636095),
   RED("RED", 'c', 12, 16733525),
   LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13, 16733695),
   YELLOW("YELLOW", 'e', 14, 16777045),
   WHITE("WHITE", 'f', 15, 16777215),
   OBFUSCATED("OBFUSCATED", 'k', true),
   BOLD("BOLD", 'l', true),
   STRIKETHROUGH("STRIKETHROUGH", 'm', true),
   UNDERLINE("UNDERLINE", 'n', true),
   ITALIC("ITALIC", 'o', true),
   RESET("RESET", 'r', -1, null);

   public static final Codec<ChatFormatting> CODEC = StringRepresentable.fromEnum(ChatFormatting::values);
   public static final char PREFIX_CODE = '§';
   private static final Map<String, ChatFormatting> FORMATTING_BY_NAME = Arrays.stream(values())
      .collect(Collectors.toMap(param0 -> cleanName(param0.name), param0 -> (ChatFormatting)param0));
   private static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
   private final String name;
   private final char code;
   private final boolean isFormat;
   private final String toString;
   private final int id;
   @Nullable
   private final Integer color;

   private static String cleanName(String param0) {
      return param0.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
   }

   ChatFormatting(String param2, char param3, int param4, @Nullable Integer param5) {
      this(param2, param3, false, param4, param5);
   }

   ChatFormatting(String param2, char param3, boolean param4) {
      this(param2, param3, param4, -1, null);
   }

   ChatFormatting(String param2, char param3, boolean param4, int param5, @Nullable Integer param6) {
      this.name = param2;
      this.code = param3;
      this.isFormat = param4;
      this.id = param5;
      this.color = param6;
      this.toString = "§" + param3;
   }

   public char getChar() {
      return this.code;
   }

   public int getId() {
      return this.id;
   }

   public boolean isFormat() {
      return this.isFormat;
   }

   public boolean isColor() {
      return !this.isFormat && this != RESET;
   }

   @Nullable
   public Integer getColor() {
      return this.color;
   }

   public String getName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   @Override
   public String toString() {
      return this.toString;
   }

   @Nullable
   public static String stripFormatting(@Nullable String param0) {
      return param0 == null ? null : STRIP_FORMATTING_PATTERN.matcher(param0).replaceAll("");
   }

   @Nullable
   public static ChatFormatting getByName(@Nullable String param0) {
      return param0 == null ? null : FORMATTING_BY_NAME.get(cleanName(param0));
   }

   @Nullable
   public static ChatFormatting getById(int param0) {
      if (param0 < 0) {
         return RESET;
      }

      for (ChatFormatting var4 : values()) {
         if (var4.getId() == param0) {
            return var4;
         }
      }

      return null;
   }

   @Nullable
   public static ChatFormatting getByCode(char param0) {
      char var1 = Character.toString(param0).toLowerCase(Locale.ROOT).charAt(0);

      for (ChatFormatting var5 : values()) {
         if (var5.code == var1) {
            return var5;
         }
      }

      return null;
   }

   public static Collection<String> getNames(boolean param0, boolean param1) {
      List<String> var2 = Lists.newArrayList();

      for (ChatFormatting var6 : values()) {
         if ((!var6.isColor() || param0) && (!var6.isFormat() || param1)) {
            var2.add(var6.getName());
         }
      }

      return var2;
   }

   @Override
   public String getSerializedName() {
      return this.getName();
   }
}
