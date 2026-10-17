package net.minecraft.locale;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringDecomposer;
import org.slf4j.Logger;

public abstract class Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();
   private static final Pattern UNSUPPORTED_FORMAT_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
   public static final String DEFAULT = "en_us";
   private static volatile Language instance = loadDefault();

   private static Language loadDefault() {
      Builder<String, String> var0 = ImmutableMap.builder();
      BiConsumer<String, String> var1 = var0::put;
      String var2 = "/assets/minecraft/lang/en_us.json";

      // PORT NOTE (Eag19 / TeaVM WASM-GC)
      //
      // Upstream loads en_us.json off the classpath with Class.getResourceAsStream.
      // That method exists on the WASM-GC classlib (TClass delegates to TClassLoader,
      // which asks TeaVM's ResourceSupplier), but this target registers no classpath
      // resources: VanillaPackResourcesBuilder already documents that vanilla assets
      // are served out of assets.epk by the resource-pack layer and the classpath holds
      // none. So the lookup returned null, and the classlib's TInputStreamReader does
      // not null-check its stream, so the first read called stream.available() on null
      // and threw NullPointerException - killing Language.<clinit> and with it the
      // whole game, because Language is first touched by CreativeModeTabs during the
      // Minecraft constructor. Read the identical resource through Eaglercraft's asset
      // API instead, which is backed by assets.epk; that EPK does contain
      // assets/minecraft/lang/en_us.json (it is one of the 7647 entries loaded at
      // startup). The null guard covers the (currently impossible) missing-entry case
      // so a future asset change degrades to the empty language rather than an NPE.
      try (InputStream var3 = EagRuntime.getResourceStream(var2)) {
         if (var3 != null) {
            loadFromJson(var3, var1);
         } else {
            LOGGER.error("Couldn't find language file {}", var2);
         }
      } catch (IOException | JsonParseException var3) {
         LOGGER.error("Couldn't read strings from {}", var2, var3);
      }

      final Map<String, String> var3 = var0.build();
      return new Language() {
         @Override
         public String getOrDefault(String l_param0, String l_param1) {
            return var3.getOrDefault(l_param0, l_param1);
         }

         @Override
         public boolean has(String l_param0) {
            return var3.containsKey(l_param0);
         }

         @Override
         public boolean isDefaultRightToLeft() {
            return false;
         }

         @Override
         public FormattedCharSequence getVisualOrder(FormattedText l_param0) {
            return l2_param1 -> l_param0.visit(
                  (l3_param1, l3_param2) -> StringDecomposer.iterateFormatted(l3_param2, l3_param1, l2_param1)
                     ? Optional.empty()
                     : FormattedText.STOP_ITERATION,
                  Style.EMPTY
               )
               .isPresent();
         }
      };
   }

   public static void loadFromJson(InputStream param0, BiConsumer<String, String> param1) {
      JsonObject var2 = GSON.fromJson(new InputStreamReader(param0, StandardCharsets.UTF_8), JsonObject.class);

      for (Entry<String, JsonElement> var4 : var2.entrySet()) {
         // PORT NOTE (Eag19 / TeaVM WASM-GC)
         //
         // Upstream is .replaceAll("%$1s"). The classlib's TMatcher.processReplacement
         // parses a replacement's "$n" and then does group(n).length(); for the common
         // no-index format specifiers (%d, %.2f, %05d) group 1 did not participate, so
         // group(1) is null and that dereference is an NPE. The JDK inserts an empty
         // string there; TeaVM wraps the NPE as IllegalArgumentException("") and kills
         // the call - which is what stopped Language.<clinit> from building its table.
         // Rebuild the identical replacement by hand so appendReplacement is never
         // reached: emit "%" + optional "<n>$" + "s" for every match, which is byte for
         // byte what the "%$1s" replacement produces on a correct JDK.
         String var5 = rewriteUnsupportedFormats(GsonHelper.convertToString(var4.getValue(), var4.getKey()));
         param1.accept(var4.getKey(), var5);
      }
   }

   private static String rewriteUnsupportedFormats(String param0) {
      Matcher var1 = UNSUPPORTED_FORMAT_PATTERN.matcher(param0);
      StringBuilder var2 = new StringBuilder();
      int var3 = 0;

      while (var1.find()) {
         var2.append(param0.substring(var3, var1.start()));
         var2.append('%');
         String var4 = var1.group(1);
         if (var4 != null) {
            var2.append(var4);
         }
         var2.append('s');
         var3 = var1.end();
      }

      var2.append(param0.substring(var3));
      return var2.toString();
   }

   public static Language getInstance() {
      return instance;
   }

   public static void inject(Language param0) {
      instance = param0;
   }

   public String getOrDefault(String param0) {
      return this.getOrDefault(param0, param0);
   }

   public abstract String getOrDefault(String var1, String var2);

   public abstract boolean has(String var1);

   public abstract boolean isDefaultRightToLeft();

   public abstract FormattedCharSequence getVisualOrder(FormattedText var1);

   public List<FormattedCharSequence> getVisualOrder(List<FormattedText> param0) {
      return param0.stream().map(this::getVisualOrder).collect(ImmutableList.toImmutableList());
   }
}
