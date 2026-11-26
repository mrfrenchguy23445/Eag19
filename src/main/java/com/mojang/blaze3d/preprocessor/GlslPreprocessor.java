package com.mojang.blaze3d.preprocessor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public abstract class GlslPreprocessor {
   private static final String C_COMMENT = "/\\*(?:[^*]|\\*+[^*/])*\\*+/";
   // PORT NOTE (Eag19 / TeaVM WASM-GC): TeaVM's Harmony-derived regex engine
   // (org.teavm.classlib.java.util.regex.TLexer) does not implement the JDK-only
   // \h (horizontal whitespace) and \v (vertical whitespace) escapes -- its escape
   // switch throws TPatternSyntaxException for them. Expanding them to the exact
   // character classes the JDK documents keeps these patterns semantically identical
   // while compiling under TeaVM. These Pattern.compile calls run in this class's
   // static initializer, so a rejection here aborts class init and, via ShaderInstance,
   // all of RenderSystem/GameRenderer startup.
   private static final String HORIZONTAL_WHITESPACE = "[ \\t\\xA0\\u1680\\u180e\\u2000-\\u200a\\u202f\\u205f\\u3000]";
   private static final String VERTICAL_WHITESPACE = "[\\n\\x0B\\f\\r\\x85\\u2028\\u2029]";
   private static final String LINE_COMMENT = "//" + VERTICAL_WHITESPACE + "*";
   private static final Pattern REGEX_MOJ_IMPORT = Pattern.compile(
      "(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|" + HORIZONTAL_WHITESPACE + ")*moj_import(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|" + HORIZONTAL_WHITESPACE + ")*(?:\"(.*)\"|<(.*)>))"
   );
   private static final Pattern REGEX_VERSION = Pattern.compile("(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|" + HORIZONTAL_WHITESPACE + ")*version(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|" + HORIZONTAL_WHITESPACE + ")*(\\d+))\\b");
   private static final Pattern REGEX_ENDS_WITH_WHITESPACE = Pattern.compile("(?:^|" + VERTICAL_WHITESPACE + ")(?:\\s|/\\*(?:[^*]|\\*+[^*/])*\\*+/|(//" + VERTICAL_WHITESPACE + "*))*\\z");

   public List<String> process(String param0) {
      GlslPreprocessor.Context var2 = new GlslPreprocessor.Context();
      List<String> var3 = this.processImports(param0, var2, "");
      var3.set(0, this.setVersion(var3.get(0), var2.glslVersion));
      return var3;
   }

   private List<String> processImports(String param0, GlslPreprocessor.Context param1, String param2) {
      int var4 = param1.sourceId;
      int var5 = 0;
      String var6 = "";
      List<String> var7 = Lists.newArrayList();
      Matcher var8 = REGEX_MOJ_IMPORT.matcher(param0);

      while (var8.find()) {
         if (!isDirectiveDisabled(param0, var8, var5)) {
            String var9 = var8.group(2);
            boolean var10 = var9 != null;
            if (!var10) {
               var9 = var8.group(3);
            }

            if (var9 != null) {
               String var11 = param0.substring(var5, var8.start(1));
               String var12 = param2 + var9;
               String var13 = this.applyImport(var10, var12);
               if (!Strings.isNullOrEmpty(var13)) {
                  if (!StringUtil.endsWithNewLine(var13)) {
                     var13 = var13 + System.lineSeparator();
                  }

                  param1.sourceId++;
                  int var14 = param1.sourceId;
                  List<String> var15 = this.processImports(var13, param1, var10 ? FileUtil.getFullResourcePath(var12) : "");
                  var15.set(0, String.format(Locale.ROOT, "#line %d %d\n%s", 0, var14, this.processVersions(var15.get(0), param1)));
                  if (!StringUtils.isBlank(var11)) {
                     var7.add(var11);
                  }

                  var7.addAll(var15);
               } else {
                  String var14 = var10 ? String.format(Locale.ROOT, "/*#moj_import \"%s\"*/", var9) : String.format(Locale.ROOT, "/*#moj_import <%s>*/", var9);
                  var7.add(var6 + var11 + var14);
               }

               int var14 = StringUtil.lineCount(param0.substring(0, var8.end(1)));
               var6 = String.format(Locale.ROOT, "#line %d %d", var14, var4);
               var5 = var8.end(1);
            }
         }
      }

      String var9 = param0.substring(var5);
      if (!StringUtils.isBlank(var9)) {
         var7.add(var6 + var9);
      }

      return var7;
   }

   private String processVersions(String param0, GlslPreprocessor.Context param1) {
      Matcher var3 = REGEX_VERSION.matcher(param0);
      if (var3.find() && isDirectiveEnabled(param0, var3)) {
         param1.glslVersion = Math.max(param1.glslVersion, Integer.parseInt(var3.group(2)));
         return param0.substring(0, var3.start(1)) + "/*" + param0.substring(var3.start(1), var3.end(1)) + "*/" + param0.substring(var3.end(1));
      } else {
         return param0;
      }
   }

   private String setVersion(String param0, int param1) {
      Matcher var3 = REGEX_VERSION.matcher(param0);
      return var3.find() && isDirectiveEnabled(param0, var3)
         ? param0.substring(0, var3.start(2)) + Math.max(param1, Integer.parseInt(var3.group(2))) + param0.substring(var3.end(2))
         : param0;
   }

   private static boolean isDirectiveEnabled(String param0, Matcher param1) {
      return !isDirectiveDisabled(param0, param1, 0);
   }

   private static boolean isDirectiveDisabled(String param0, Matcher param1, int param2) {
      int var3 = param1.start() - param2;
      if (var3 == 0) {
         return false;
      }

      Matcher var4 = REGEX_ENDS_WITH_WHITESPACE.matcher(param0.substring(param2, param1.start()));
      if (!var4.find()) {
         return true;
      }

      int var5 = var4.end(1);
      return var5 == param1.start();
   }

   @Nullable
   public abstract String applyImport(boolean var1, String var2);

   static final class Context {
      int glslVersion;
      int sourceId;
   }
}
